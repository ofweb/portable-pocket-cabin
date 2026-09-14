package dev.portablepocketcabin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-side authority for tracking and funding cabin upgrades. */
final class CabinUpgradeService {
	@FunctionalInterface
	interface FundEjector {
		boolean eject(CabinRecord cabin, List<ItemStack> stacks);
	}

	interface ExpansionEffect {
		Outcome validate(CabinRecord cabin, int targetSize);

		void apply(CabinRecord cabin, int targetSize);

		void refresh(CabinRecord cabin);
	}

	record Outcome(boolean success, String message) {
		static Outcome success(String message) {
			return new Outcome(true, message);
		}

		static Outcome failure(String message) {
			return new Outcome(false, message);
		}
	}

	record Contribution(boolean success, int accepted, String message) {
		static Contribution success(int accepted) {
			return new Contribution(true, accepted, "Accepted " + accepted + " upgrade materials.");
		}

		static Contribution failure(String message) {
			return new Contribution(false, 0, message);
		}
	}

	private CabinUpgradeService() {
	}

	static Outcome track(
		CabinRegistry registry,
		UUID cabinId,
		UUID actor,
		CabinUpgradeState.Target requested,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (!active(cabin)) {
				return Outcome.failure("That cabin controller is not active.");
			}
			if (!cabin.owner().equals(actor)) {
				return Outcome.failure("Only the cabin owner may track an upgrade.");
			}
			if (cabin.upgrades().tracked().isPresent()) {
				return Outcome.failure("Stop tracking the current upgrade before selecting another.");
			}
			CabinUpgradeCatalog.Offer offer = CabinUpgradeCatalog.next(cabin, attunement, definitions)
				.orElse(null);
			if (offer == null) {
				return Outcome.failure("This cabin has reached its general-space limit.");
			}
			if (!offer.target().equals(requested)) {
				return Outcome.failure("That upgrade is no longer available.");
			}
			CabinUpgradeState.TrackedUpgrade tracked = new CabinUpgradeState.TrackedUpgrade(
				offer.target(), offer.requirements(), List.of()
			);
			registry.updateUpgradeState(cabinId, cabin.upgrades().withTracked(tracked));
			return Outcome.success("Upgrade tracking started.");
		}
	}

	/**
	 * Moves at most the currently missing count from the deliberately offered stack into the fund.
	 * The input stack is shrunk only after the durable cabin state has accepted the corresponding copy.
	 */
	static Contribution contribute(
		CabinRegistry registry,
		UUID cabinId,
		UUID actor,
		ItemStack offered,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (!active(cabin)) {
				return Contribution.failure("That cabin controller is not active.");
			}
			boolean trustedContributor = cabin.trustedPlayers().contains(actor) && cabin.canEnter(actor);
			if (!cabin.owner().equals(actor) && !trustedContributor) {
				return Contribution.failure("You may not contribute to this cabin.");
			}
			CabinUpgradeState.TrackedUpgrade tracked = cabin.upgrades().tracked().orElse(null);
			if (tracked == null) {
				return Contribution.failure("No upgrade is currently tracked.");
			}
			if (cabin.upgrades().installation().isPresent()) {
				return Contribution.failure("That upgrade is currently being installed.");
			}
			if (CabinUpgradeCatalog.isStale(tracked, cabin, attunement, definitions)) {
				return Contribution.failure("The tracked upgrade changed after a datapack reload.");
			}
			if (offered.isEmpty()) {
				return Contribution.failure("Offer a required item to contribute.");
			}

			var itemId = BuiltInRegistries.ITEM.getKey(offered.getItem());
			CabinUpgradeState.Requirement requirement = tracked.requirements().stream()
				.filter(value -> value.itemId().equals(itemId))
				.findFirst()
				.orElse(null);
			if (requirement == null) {
				return Contribution.failure("That item is not required by the tracked upgrade.");
			}
			int missing = requirement.count() - tracked.fundedCount(itemId);
			if (missing <= 0) {
				return Contribution.failure("That requirement is already fully funded.");
			}
			int accepted = Math.min(missing, offered.getCount());
			List<ItemStack> fund = new ArrayList<>(tracked.fund());
			fund.add(offered.copyWithCount(accepted));
			CabinUpgradeState.TrackedUpgrade updated = tracked.withFund(fund);
			registry.updateUpgradeState(cabinId, new CabinUpgradeState(
				java.util.Optional.of(updated), cabin.upgrades().installation()
			));
			offered.shrink(accepted);
			return Contribution.success(accepted);
		}
	}

	static Outcome stopTracking(
		CabinRegistry registry, UUID cabinId, UUID actor, FundEjector ejector
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (!active(cabin)) {
				return Outcome.failure("That cabin controller is not active.");
			}
			if (!cabin.owner().equals(actor)) {
				return Outcome.failure("Only the cabin owner may stop tracking an upgrade.");
			}
			CabinUpgradeState.TrackedUpgrade tracked = cabin.upgrades().tracked().orElse(null);
			if (tracked == null) {
				return Outcome.failure("No upgrade is currently tracked.");
			}
			if (cabin.upgrades().installation().isPresent()) {
				return Outcome.failure("An installation already started and must be recovered.");
			}
			if (!ejector.eject(cabin, tracked.fund())) {
				return Outcome.failure("The upgrade fund could not be ejected; tracking was not changed.");
			}
			registry.updateUpgradeState(cabinId, CabinUpgradeState.EMPTY);
			return Outcome.success("Stopped tracking and ejected the upgrade fund.");
		}
	}

	static Outcome install(
		CabinRegistry registry,
		UUID cabinId,
		UUID actor,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions,
		ExpansionEffect effect,
		Runnable flush
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (!active(cabin)) {
				return Outcome.failure("That cabin controller is not active.");
			}
			if (!cabin.owner().equals(actor)) {
				return Outcome.failure("Only the cabin owner may install an upgrade.");
			}
			CabinUpgradeState.TrackedUpgrade tracked = cabin.upgrades().tracked().orElse(null);
			if (tracked == null) {
				return Outcome.failure("No upgrade is currently tracked.");
			}
			if (cabin.upgrades().installation().isPresent()) {
				return Outcome.failure("That upgrade installation is already in progress.");
			}
			if (CabinUpgradeCatalog.isStale(tracked, cabin, attunement, definitions)) {
				return Outcome.failure("The tracked upgrade changed after a datapack reload.");
			}
			if (!tracked.isComplete()) {
				return Outcome.failure("The tracked upgrade is not fully funded.");
			}
			if (!tracked.target().isGeneralSpace()) {
				return Outcome.failure("That upgrade type cannot be installed by this service.");
			}
			int targetSize = tracked.target().generalSpaceSize();
			Outcome validation = effect.validate(cabin, targetSize);
			if (!validation.success()) {
				return validation;
			}

			CabinUpgradeState.Installation installation = new CabinUpgradeState.Installation(
				UUID.randomUUID(), tracked.target(), cabin.progression().generalSize()
			);
			registry.updateUpgradeState(cabinId, cabin.upgrades().withInstallation(installation));
			flush.run();
			try {
				effect.apply(cabin, targetSize);
			} catch (RuntimeException exception) {
				return Outcome.failure("Installation was interrupted and will resume safely: "
					+ exception.getMessage());
			}
			CabinRecord installed = registry.completeUpgradeInstallation(cabinId, installation.operationId());
			flush.run();
			effect.refresh(installed);
			return Outcome.success("Cabin general space expanded to " + targetSize + "x" + targetSize + ".");
		}
	}

	static void reconcileInstallation(
		CabinRegistry registry, UUID cabinId, ExpansionEffect effect, Runnable flush
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (cabin == null || cabin.upgrades().installation().isEmpty()) {
				return;
			}
			CabinUpgradeState.Installation installation = cabin.upgrades().installation().orElseThrow();
			int targetSize = installation.target().generalSpaceSize();
			effect.apply(cabin, targetSize);
			CabinRecord installed = registry.completeUpgradeInstallation(cabinId, installation.operationId());
			flush.run();
			effect.refresh(installed);
		}
	}

	static void reconcileAll(MinecraftServer server) {
		CabinRegistry registry = CabinRegistry.get(server);
		if (registry.cabins().stream().noneMatch(cabin -> cabin.upgrades().installation().isPresent())) {
			return;
		}
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			throw new IllegalStateException("Pocket dimension is unavailable during upgrade recovery");
		}
		ExpansionEffect effect = new CabinGeneralSpaceEffect(server, pocket);
		for (CabinRecord cabin : registry.cabins()) {
			if (cabin.upgrades().installation().isEmpty()) {
				continue;
			}
			try {
				reconcileInstallation(registry, cabin.uuid(), effect, () -> CabinRegistry.flush(server));
			} catch (RuntimeException exception) {
				PortablePocketCabin.LOGGER.error(
					"Could not reconcile upgrade installation for cabin {}", cabin.uuid(), exception
				);
			}
		}
	}

	private static boolean active(CabinRecord cabin) {
		return cabin != null && cabin.lifecycle() == CabinLifecycle.DEPLOYED && cabin.interiorGenerated();
	}
}
