package dev.portablepocketcabin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

final class CabinReconciliation {
	enum Action {
		NONE,
		ENSURE_PACKED,
		FINISH_DEPLOYMENT,
		ROLL_BACK_DEPLOYMENT,
		ABORT_PACKING,
		ORPHAN
	}

	private CabinReconciliation() {
	}

	static void reconcileAll(MinecraftServer server) {
		CabinRegistry registry = CabinRegistry.get(server);
		for (CabinRecord cabin : registry.cabins()) {
			String result = reconcile(server, cabin.uuid());
			if (!"unchanged".equals(result)) {
				PortablePocketCabin.LOGGER.info("Reconciled cabin {}: {}", cabin.uuid(), result);
			}
		}
	}

	static String reconcile(MinecraftServer server, java.util.UUID cabinId) {
		CabinRegistry registry = CabinRegistry.get(server);
		CabinRecord cabin = registry.find(cabinId)
			.orElseThrow(() -> new IllegalStateException("No cabin record exists for " + cabinId));
		boolean projectionValid = hasValidProjection(server, cabin);
		Action action = plan(cabin, projectionValid);

		return switch (action) {
			case NONE -> "unchanged";
			case ENSURE_PACKED -> {
				cabin.lastExterior().ifPresent(exterior -> removeProjection(server, exterior));
				registry.markExteriorCleanupComplete(cabin.uuid());
				yield "packed projection cleaned";
			}
			case FINISH_DEPLOYMENT -> {
				registry.finishDeployment(cabin.uuid());
				yield "interrupted deployment committed";
			}
			case ROLL_BACK_DEPLOYMENT -> {
				cabin.exterior().ifPresent(exterior -> removeProjection(server, exterior));
				registry.rollbackDeployment(cabin.uuid());
				yield "interrupted deployment rolled back to PACKED";
			}
			case ABORT_PACKING -> {
				registry.abortPacking(cabin.uuid());
				yield "interrupted packing restored to DEPLOYED";
			}
			case ORPHAN -> {
				evacuateOnlineOccupants(server, cabin);
				cabin.exterior().ifPresent(exterior -> removeProjection(server, exterior));
				registry.markOrphaned(cabin.uuid());
				yield "missing exterior marked ORPHANED";
			}
		};
	}

	static Action plan(CabinRecord cabin, boolean projectionValid) {
		return switch (cabin.lifecycle()) {
			case PACKED -> cabin.exteriorCleanupPending() ? Action.ENSURE_PACKED : Action.NONE;
			case DEPLOYING -> projectionValid && cabin.interiorGenerated()
				? Action.FINISH_DEPLOYMENT : Action.ROLL_BACK_DEPLOYMENT;
			case DEPLOYED -> projectionValid ? Action.NONE : Action.ORPHAN;
			case PACKING -> projectionValid ? Action.ABORT_PACKING : Action.ORPHAN;
			case ORPHANED -> Action.NONE;
		};
	}

	static boolean hasValidProjection(MinecraftServer server, CabinRecord cabin) {
		if (cabin.exterior().isEmpty()) {
			return false;
		}
		CabinExterior exterior = cabin.exterior().get();
		ServerLevel level = server.getLevel(exterior.dimension());
		return level != null && ExteriorCabin.projectionValid(level, exterior);
	}

	private static void removeProjection(MinecraftServer server, CabinExterior exterior) {
		ServerLevel level = server.getLevel(exterior.dimension());
		if (level != null) {
			ExteriorCabin.removeProjection(level, exterior);
		}
	}

	private static void evacuateOnlineOccupants(MinecraftServer server, CabinRecord cabin) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isOccupant(player, cabin)) {
				continue;
			}
			SafeDestinationResolver.resolveForCabin(player, cabin)
				.ifPresent(destination -> destination.teleport(player, 0.0F));
		}
	}

	static boolean isOccupant(ServerPlayer player, CabinRecord cabin) {
		if (!player.level().dimension().equals(PocketDimension.LEVEL_KEY)) {
			return false;
		}
		return PocketDimension.cellIndexAt(player.blockPosition()).orElse(-1L) == cabin.cellIndex();
	}
}
