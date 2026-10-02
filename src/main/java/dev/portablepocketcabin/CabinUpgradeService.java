package dev.portablepocketcabin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-side authority for target-keyed upgrade fund transactions and installation. */
final class CabinUpgradeService {
	interface UpgradeEffect {
		Outcome validate(CabinRecord cabin, CabinUpgradeCatalog.Offer offer);

		void apply(CabinRecord cabin, CabinUpgradeState.Installation installation);

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

	record Withdrawal(boolean success, ItemStack stack, String message) {
		static Withdrawal success(ItemStack stack) {
			return new Withdrawal(true, stack, "Withdrew " + stack.getCount() + " upgrade materials.");
		}

		static Withdrawal failure(String message) {
			return new Withdrawal(false, ItemStack.EMPTY, message);
		}
	}

	private CabinUpgradeService() {
	}

	/** Deposits from one explicitly offered stack into one explicitly selected requirement icon. */
	static Contribution deposit(
		CabinRegistry registry,
		UUID cabinId,
		UUID actor,
		CabinUpgradeState.Target target,
		ItemStack offered,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions,
		UpgradeEffect effect
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (!active(cabin)) {
				return Contribution.failure("That cabin controller is not active.");
			}
			if (!mayUseFund(cabin, actor)) {
				return Contribution.failure("You may not contribute to this cabin.");
			}
			if (cabin.upgrades().operationInProgress()) {
				return Contribution.failure("A cabin upgrade operation is in progress.");
			}
			CabinUpgradeCatalog.Offer offer = CabinUpgradeCatalog.offer(cabin, target, attunement, definitions)
				.orElse(null);
			if (offer == null || offer.complete() || offer.locked()) {
				return Contribution.failure("That upgrade is not currently available.");
			}
			if (!effect.validate(cabin, offer).success()) {
				return Contribution.failure("That upgrade is currently obstructed.");
			}
			CabinUpgradeState.Fund existing = cabin.upgrades().fund(target).orElse(null);
			if (existing != null && CabinUpgradeCatalog.isStale(existing, cabin, attunement, definitions)) {
				return Contribution.failure("The funded upgrade requirements no longer match the loaded definition.");
			}
			if (offered.isEmpty()) {
				return Contribution.failure("Offer a required item to contribute.");
			}

			List<CabinUpgradeState.Requirement> requirements = existing == null
				? offer.requirements() : existing.requirements();
			Identifier itemId = BuiltInRegistries.ITEM.getKey(offered.getItem());
			CabinUpgradeState.Requirement requirement = requirements.stream()
				.filter(value -> value.itemId().equals(itemId))
				.findFirst()
				.orElse(null);
			if (requirement == null) {
				return Contribution.failure("That item is not required by this upgrade.");
			}
			int funded = existing == null ? 0 : existing.fundedCount(itemId);
			int missing = requirement.count() - funded;
			if (missing <= 0) {
				return Contribution.failure("That requirement is already fully funded.");
			}
			int accepted = Math.min(missing, offered.getCount());
			List<ItemStack> stacks = new ArrayList<>(existing == null ? List.of() : existing.stacks());
			stacks.add(offered.copyWithCount(accepted));
			CabinUpgradeState.Fund updated = new CabinUpgradeState.Fund(target, requirements, stacks);
			registry.updateUpgradeState(cabinId, cabin.upgrades().withFund(updated));
			offered.shrink(accepted);
			return Contribution.success(accepted);
		}
	}

	/** Withdraws from the oldest matching contribution while preserving its exact components. */
	static Withdrawal withdraw(
		CabinRegistry registry,
		UUID cabinId,
		UUID actor,
		CabinUpgradeState.Target target,
		Identifier itemId,
		int requested
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (!active(cabin)) {
				return Withdrawal.failure("That cabin controller is not active.");
			}
			if (!mayUseFund(cabin, actor)) {
				return Withdrawal.failure("You may not withdraw from this cabin.");
			}
			if (cabin.upgrades().operationInProgress()) {
				return Withdrawal.failure("A cabin upgrade operation is in progress.");
			}
			CabinUpgradeState.Fund fund = cabin.upgrades().fund(target).orElse(null);
			if (fund == null || requested <= 0) {
				return Withdrawal.failure("That upgrade fund has no matching materials.");
			}
			List<ItemStack> stacks = new ArrayList<>(fund.stacks());
			for (int index = 0; index < stacks.size(); index++) {
				ItemStack stored = stacks.get(index);
				if (!BuiltInRegistries.ITEM.getKey(stored.getItem()).equals(itemId)) {
					continue;
				}
				int amount = Math.min(requested, stored.getCount());
				ItemStack result = stored.copyWithCount(amount);
				if (amount == stored.getCount()) {
					stacks.remove(index);
				} else {
					ItemStack remainder = stored.copy();
					remainder.shrink(amount);
					stacks.set(index, remainder);
				}
				CabinUpgradeState updated = stacks.isEmpty()
					? cabin.upgrades().withoutFund(target)
					: cabin.upgrades().withFund(fund.withStacks(stacks));
				registry.updateUpgradeState(cabinId, updated);
				return Withdrawal.success(result);
			}
			return Withdrawal.failure("That upgrade fund has no matching materials.");
		}
	}

	static Outcome install(
		CabinRegistry registry,
		UUID cabinId,
		UUID actor,
		CabinUpgradeState.Target target,
		long expectedFundRevision,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions,
		UpgradeEffect effect,
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
			if (cabin.upgrades().fundRevision() != expectedFundRevision) {
				return Outcome.failure("The upgrade fund changed; confirm the installation again.");
			}
			CabinUpgradeState.Fund fund = cabin.upgrades().fund(target).orElse(null);
			if (fund == null) {
				return Outcome.failure("That upgrade has no funded materials.");
			}
			if (cabin.upgrades().operationInProgress()) {
				return Outcome.failure("A cabin upgrade operation is already in progress.");
			}
			if (CabinUpgradeCatalog.isStale(fund, cabin, attunement, definitions)) {
				return Outcome.failure("That upgrade is no longer available.");
			}
			if (!fund.isComplete()) {
				return Outcome.failure("That upgrade is not fully funded.");
			}
			CabinUpgradeCatalog.Offer offer = CabinUpgradeCatalog.offer(cabin, target, attunement, definitions)
				.orElse(null);
			if (offer == null || offer.complete() || offer.locked()) {
				return Outcome.failure("That upgrade is no longer available.");
			}
			Outcome validation = effect.validate(cabin, offer);
			if (!validation.success()) {
				return validation;
			}

			CabinUpgradeState.Installation installation = target.isGeneralSpace()
				? CabinUpgradeState.Installation.generalSpace(
					UUID.randomUUID(), target, cabin.progression().generalSize()
				)
				: target.isStorage() ? new CabinUpgradeState.Installation(
					UUID.randomUUID(), target, cabin.upgrades().storage().level(), offer.targetSize())
				: CabinUpgradeState.Installation.window(
					UUID.randomUUID(), target, cabin.upgrades().windows().tier(target.windowIdentity())
				);
			registry.updateUpgradeState(cabinId, cabin.upgrades().withInstallation(installation));
			flush.run();
			try {
				effect.apply(cabin, installation);
			} catch (RuntimeException exception) {
				return Outcome.failure("Installation was interrupted and will resume safely: "
					+ exception.getMessage());
			}
			CabinRecord installed = registry.completeUpgradeInstallation(cabinId, installation.operationId());
			flush.run();
			effect.refresh(installed);
			return target.isGeneralSpace()
				? Outcome.success("Cabin general space expanded to " + installation.targetState()
					+ "x" + installation.targetState() + ".")
				: target.isStorage() ? Outcome.success("Central storage capacity increased.")
				: Outcome.success("Cabin window installed at tier " + installation.targetState() + ".");
		}
	}

	static void reconcileInstallation(
		CabinRegistry registry, UUID cabinId, UpgradeEffect effect, Runnable flush
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (cabin == null || cabin.upgrades().installation().isEmpty()) {
				return;
			}
			CabinUpgradeState.Installation installation = cabin.upgrades().installation().orElseThrow();
			effect.apply(cabin, installation);
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
		UpgradeEffect effect = new CabinUpgradeEffect(server, pocket);
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

	private static boolean mayUseFund(CabinRecord cabin, UUID actor) {
		return cabin.owner().equals(actor)
			|| (cabin.trustedPlayers().contains(actor) && cabin.canEnter(actor));
	}

	private static boolean active(CabinRecord cabin) {
		return cabin != null && cabin.lifecycle() == CabinLifecycle.DEPLOYED && cabin.interiorGenerated();
	}
}
