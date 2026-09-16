package dev.portablepocketcabin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative, recoverable downgrade and removal of purchased cabin windows. */
final class CabinWindowReversalService {
	interface Effect {
		CabinUpgradeService.Outcome validate(CabinRecord cabin, CabinWindowState resultingState);

		void apply(CabinRecord cabin, CabinWindowState resultingState);

		void refresh(CabinRecord cabin);
	}

	interface Ejector {
		boolean eject(CabinRecord cabin, CabinUpgradeState.WindowReversal reversal);
	}

	record Preview(
		boolean available,
		String message,
		CabinWindowState resultingState,
		List<net.minecraft.world.item.ItemStack> receiptRefund,
		List<CabinUpgradeState.Fund> invalidatedFunds
	) {
		Preview {
			receiptRefund = copyStacks(receiptRefund);
			invalidatedFunds = List.copyOf(invalidatedFunds);
		}

		@Override
		public List<net.minecraft.world.item.ItemStack> receiptRefund() {
			return copyStacks(receiptRefund);
		}

		static Preview unavailable(String message, CabinWindowState current) {
			return new Preview(false, message, current, List.of(), List.of());
		}
	}

	private CabinWindowReversalService() {
	}

	static Preview preview(
		CabinRecord cabin,
		CabinUpgradeState.Target target,
		CabinUpgradeState.ReversalAction action,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions,
		Effect effect
	) {
		if (!target.isWindow()) {
			return Preview.unavailable("Only cabin windows can be reversed.", cabin.upgrades().windows());
		}
		CabinWindowState.Identity identity = target.windowIdentity();
		int currentTier = cabin.upgrades().windows().tier(identity);
		if (currentTier == 0) {
			return Preview.unavailable("That cabin window is not installed.", cabin.upgrades().windows());
		}
		CabinWindowState.ReversalResult result;
		try {
			result = action == CabinUpgradeState.ReversalAction.DOWNGRADE
				? cabin.upgrades().windows().downgrade(identity, currentTier)
				: cabin.upgrades().windows().remove(identity, currentTier);
		} catch (IllegalStateException exception) {
			return Preview.unavailable(exception.getMessage(), cabin.upgrades().windows());
		}
		CabinUpgradeService.Outcome validation = effect.validate(cabin, result.state());
		if (!validation.success()) {
			return Preview.unavailable(validation.message(), cabin.upgrades().windows());
		}
		CabinRecord tentative = withWindows(cabin, result.state());
		List<CabinUpgradeState.Fund> invalidated = new ArrayList<>();
		for (CabinUpgradeState.Fund fund : cabin.upgrades().funds()) {
			Optional<CabinUpgradeCatalog.Offer> offer = CabinUpgradeCatalog.offer(
				tentative, fund.target(), attunement, definitions
			);
			if (offer.isEmpty() || offer.get().complete() || offer.get().locked()
				|| !offer.get().requirements().equals(fund.requirements())) {
				invalidated.add(fund);
			}
		}
		return new Preview(true, validation.message(), result.state(), result.refundStacks(), invalidated);
	}

	static CabinUpgradeService.Outcome reverse(
		CabinRegistry registry,
		UUID cabinId,
		UUID actor,
		CabinUpgradeState.Target target,
		CabinUpgradeState.ReversalAction action,
		long expectedFundRevision,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions,
		Effect effect,
		Ejector ejector,
		Runnable flush
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (!active(cabin)) {
				return CabinUpgradeService.Outcome.failure("That cabin controller is not active.");
			}
			if (!cabin.owner().equals(actor)) {
				return CabinUpgradeService.Outcome.failure("Only the cabin owner may reverse a window.");
			}
			if (cabin.upgrades().operationInProgress()) {
				return CabinUpgradeService.Outcome.failure("A cabin upgrade operation is already in progress.");
			}
			if (cabin.upgrades().fundRevision() != expectedFundRevision) {
				return CabinUpgradeService.Outcome.failure("The upgrade funds changed; confirm the action again.");
			}
			Preview preview = preview(cabin, target, action, attunement, definitions, effect);
			if (!preview.available()) {
				return CabinUpgradeService.Outcome.failure(preview.message());
			}
			int expectedTier = cabin.upgrades().windows().tier(target.windowIdentity());
			int targetTier = preview.resultingState().tier(target.windowIdentity());
			CabinUpgradeState.WindowReversal reversal = new CabinUpgradeState.WindowReversal(
				UUID.randomUUID(), action, target, expectedTier, targetTier,
				preview.receiptRefund(), preview.invalidatedFunds(), CabinUpgradeState.ReversalPhase.WORLD_PENDING
			);
			registry.updateUpgradeState(cabinId, cabin.upgrades().withReversal(reversal));
			flush.run();
			return resume(registry, cabinId, effect, ejector, flush);
		}
	}

	static CabinUpgradeService.Outcome resume(
		CabinRegistry registry, UUID cabinId, Effect effect, Ejector ejector, Runnable flush
	) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (cabin == null || cabin.upgrades().reversal().isEmpty()) {
				return CabinUpgradeService.Outcome.failure("No window reversal is in progress.");
			}
			CabinUpgradeState.WindowReversal reversal = cabin.upgrades().reversal().orElseThrow();
			if (reversal.phase() == CabinUpgradeState.ReversalPhase.WORLD_PENDING) {
				CabinWindowState.ReversalResult result = result(cabin.upgrades().windows(), reversal);
				try {
					effect.apply(cabin, result.state());
				} catch (RuntimeException exception) {
					return CabinUpgradeService.Outcome.failure(
						"Window reversal was interrupted and will resume safely: " + exception.getMessage()
					);
				}
				registry.updateUpgradeState(cabinId, cabin.upgrades().commitReversal(result.state()));
				flush.run();
				cabin = registry.find(cabinId).orElseThrow();
				reversal = cabin.upgrades().reversal().orElseThrow();
			}
			boolean ejected;
			try {
				ejected = ejector.eject(cabin, reversal);
			} catch (RuntimeException exception) {
				return CabinUpgradeService.Outcome.failure(
					"Window state changed, but refund recovery failed and will remain pending: "
						+ exception.getMessage()
				);
			}
			if (!ejected) {
				return CabinUpgradeService.Outcome.failure(
					"Window state changed, but its refund remains pending and will retry safely."
				);
			}
			registry.updateUpgradeState(cabinId, cabin.upgrades().completeReversal());
			flush.run();
			CabinRecord completed = registry.find(cabinId).orElseThrow();
			effect.refresh(completed);
			return CabinUpgradeService.Outcome.success(
				reversal.action() == CabinUpgradeState.ReversalAction.DOWNGRADE
					? "Cabin window downgraded to tier " + reversal.targetTier() + "."
					: "Cabin window removed."
			);
		}
	}

	static void reconcileAll(MinecraftServer server) {
		CabinRegistry registry = CabinRegistry.get(server);
		if (registry.cabins().stream().noneMatch(cabin -> cabin.upgrades().reversal().isPresent())) {
			return;
		}
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			throw new IllegalStateException("Pocket dimension is unavailable during window reversal recovery");
		}
		Effect effect = new CabinWindowReversalEffect(server, pocket);
		Ejector ejector = (cabin, reversal) -> CabinFundEjection.ejectMarked(
			pocket, cabin, reversal.operationId(), reversal.payloadStacks()
		);
		for (CabinRecord cabin : registry.cabins()) {
			if (cabin.upgrades().reversal().isEmpty()) {
				continue;
			}
			try {
				CabinUpgradeService.Outcome outcome = resume(
					registry, cabin.uuid(), effect, ejector, () -> CabinRegistry.flush(server)
				);
				if (!outcome.success()) {
					PortablePocketCabin.LOGGER.error(
						"Could not reconcile window reversal for cabin {}: {}", cabin.uuid(), outcome.message()
					);
				}
			} catch (RuntimeException exception) {
				PortablePocketCabin.LOGGER.error(
					"Could not reconcile window reversal for cabin {}", cabin.uuid(), exception
				);
			}
		}
	}

	private static CabinWindowState.ReversalResult result(
		CabinWindowState state, CabinUpgradeState.WindowReversal reversal
	) {
		return reversal.action() == CabinUpgradeState.ReversalAction.DOWNGRADE
			? state.downgrade(reversal.target().windowIdentity(), reversal.expectedTier())
			: state.remove(reversal.target().windowIdentity(), reversal.expectedTier());
	}

	private static CabinRecord withWindows(CabinRecord cabin, CabinWindowState windows) {
		return new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(),
			cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(),
			cabin.exteriorCleanupPending(), cabin.palette(), cabin.lastDeploymentItemId(),
			cabin.deploymentItemDeliveryPending(), cabin.entryPermission(), cabin.trustedPlayers(),
			cabin.progression(), cabin.upgrades().withWindows(windows)
		);
	}

	private static boolean active(CabinRecord cabin) {
		return cabin != null && cabin.lifecycle() == CabinLifecycle.DEPLOYED && cabin.interiorGenerated();
	}

	private static List<net.minecraft.world.item.ItemStack> copyStacks(
		List<net.minecraft.world.item.ItemStack> stacks
	) {
		return stacks.stream().map(net.minecraft.world.item.ItemStack::copy).toList();
	}
}
