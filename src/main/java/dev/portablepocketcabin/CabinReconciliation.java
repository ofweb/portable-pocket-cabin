package dev.portablepocketcabin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

	static void reconcileOwnerInventory(ServerPlayer owner) {
		MinecraftServer server = owner.level().getServer();
		CabinRegistry registry = CabinRegistry.get(server);
		CabinRecord cabin = registry.findByOwner(owner.getUUID()).orElse(null);
		if (cabin == null) {
			return;
		}

		reconcile(server, cabin.uuid());
		cabin = registry.find(cabin.uuid()).orElseThrow();
		Inventory inventory = owner.getInventory();
		if (cabin.lifecycle() == CabinLifecycle.DEPLOYED) {
			cabin.lastDeploymentItemId().ifPresent(itemId -> CabinItems.removeByInstance(inventory, itemId));
			removeCabinItems(inventory, cabin, false);
			return;
		}
		if (cabin.lifecycle() != CabinLifecycle.PACKED) {
			return;
		}

		int validSlot = firstCurrentBoundSlot(inventory, cabin);
		if (cabin.deploymentItemDeliveryPending()) {
			UUID itemId = cabin.lastDeploymentItemId().orElseGet(UUID::randomUUID);
			int sourceSlot = CabinItems.findByInstance(inventory, itemId);
			if (sourceSlot >= 0) {
				inventory.setItem(sourceSlot,
					CabinItems.createBound(cabin, cabin.packedItemGeneration(), false, itemId));
				validSlot = sourceSlot;
			} else if (validSlot < 0) {
				int freeSlot = inventory.getFreeSlot();
				if (freeSlot < 0) {
					owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
						"Your Packed Cabin is waiting for delivery. Free one inventory slot and reconnect."
					));
					return;
				}
				inventory.setItem(freeSlot,
					CabinItems.createBound(cabin, cabin.packedItemGeneration(), false, itemId));
				validSlot = freeSlot;
			}
			registry.resolveDeploymentItemDelivery(cabin.uuid());
			CabinRegistry.flush(server);
			owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				"Your interrupted cabin deployment was recovered as a Packed Cabin."
			));
		}
		removeDuplicateCurrentItems(inventory, cabin, validSlot);
	}

	private static int firstCurrentBoundSlot(Inventory inventory, CabinRecord cabin) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			CabinItems.Binding binding = CabinItems.binding(inventory.getItem(slot)).orElse(null);
			if (binding != null && !binding.pending() && binding.cabinId().equals(cabin.uuid())
				&& binding.generation() == cabin.packedItemGeneration()
				&& binding.palette().equals(cabin.palette())) {
				return slot;
			}
		}
		return Inventory.NOT_FOUND_INDEX;
	}

	private static void removeDuplicateCurrentItems(Inventory inventory, CabinRecord cabin, int keepSlot) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (slot == keepSlot) {
				continue;
			}
			CabinItems.Binding binding = CabinItems.binding(inventory.getItem(slot)).orElse(null);
			if (binding != null && binding.cabinId().equals(cabin.uuid())
				&& binding.generation() == cabin.packedItemGeneration()) {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	private static void removeCabinItems(Inventory inventory, CabinRecord cabin, boolean pendingOnly) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			CabinItems.Binding binding = CabinItems.binding(inventory.getItem(slot)).orElse(null);
			if (binding != null && binding.cabinId().equals(cabin.uuid())
				&& (!pendingOnly || binding.pending())) {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	static void onChunkLoaded(ServerLevel level, ChunkPos chunk) {
		List<UUID> cabinIds = new ArrayList<>();
		for (CabinRecord cabin : CabinRegistry.get(level.getServer()).cabins()) {
			if (projectionForReconciliation(cabin)
				.filter(exterior -> exterior.dimension().equals(level.dimension())
					&& ExteriorCabin.touchesChunk(exterior, chunk))
				.isPresent()) {
				cabinIds.add(cabin.uuid());
			}
		}
		if (cabinIds.isEmpty()) {
			return;
		}

		MinecraftServer server = level.getServer();
		server.schedule(new TickTask(server.getTickCount() + 1, () -> {
			CabinRegistry registry = CabinRegistry.get(level.getServer());
			for (UUID cabinId : cabinIds) {
				CabinRecord cabin = registry.find(cabinId).orElse(null);
				if (cabin == null || projectionForReconciliation(cabin)
					.filter(exterior -> exterior.dimension().equals(level.dimension())
						&& ExteriorCabin.touchesChunk(exterior, chunk))
					.isEmpty()) {
					continue;
				}
				String result = reconcile(level.getServer(), cabinId);
				if (!"unchanged".equals(result)) {
					PortablePocketCabin.LOGGER.info("Reconciled cabin {} after chunk load: {}", cabinId, result);
				}
			}
		}));
	}

	private static Optional<CabinExterior> projectionForReconciliation(CabinRecord cabin) {
		if (cabin.lifecycle() == CabinLifecycle.PACKED && cabin.exteriorCleanupPending()) {
			return cabin.lastExterior();
		}
		return switch (cabin.lifecycle()) {
			case DEPLOYING, DEPLOYED, PACKING -> cabin.exterior();
			case PACKED, ORPHANED -> Optional.empty();
		};
	}

	static String reconcile(MinecraftServer server, java.util.UUID cabinId) {
		CabinRegistry registry = CabinRegistry.get(server);
		CabinRecord cabin = registry.find(cabinId)
			.orElseThrow(() -> new IllegalStateException("No cabin record exists for " + cabinId));
		upgradeLegacyCornerFrames(server, cabin);
		boolean projectionValid = hasValidProjection(server, cabin);
		Action action = plan(cabin, projectionValid);

		return switch (action) {
			case NONE -> "unchanged";
			case ENSURE_PACKED -> {
				cabin.lastExterior().ifPresent(exterior -> removeProjection(server, exterior));
				CabinRecord packed = registry.markExteriorCleanupComplete(cabin.uuid());
				CabinWindows.update(server, packed);
				yield "packed projection cleaned";
			}
			case FINISH_DEPLOYMENT -> {
				CabinRecord deployed = registry.finishDeployment(cabin.uuid());
				CabinWindows.update(server, deployed);
				yield "interrupted deployment committed";
			}
			case ROLL_BACK_DEPLOYMENT -> {
				cabin.exterior().ifPresent(exterior -> removeProjection(server, exterior));
				CabinRecord packed = registry.rollbackDeployment(cabin.uuid());
				CabinWindows.update(server, packed);
				yield "interrupted deployment rolled back to PACKED";
			}
			case ABORT_PACKING -> {
				registry.abortPacking(cabin.uuid());
				yield "interrupted packing restored to DEPLOYED";
			}
			case ORPHAN -> {
				evacuateOnlineOccupants(server, cabin);
				cabin.exterior().ifPresent(exterior -> removeProjection(server, exterior));
				CabinRecord orphaned = registry.markOrphaned(cabin.uuid());
				CabinWindows.update(server, orphaned);
				yield "missing exterior marked ORPHANED";
			}
		};
	}

	private static void upgradeLegacyCornerFrames(MinecraftServer server, CabinRecord cabin) {
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket != null) {
			PocketDimension.upgradeLegacyCornerFrames(pocket, cabin);
		}
		cabin.exterior().ifPresent(exterior -> {
			ServerLevel exteriorLevel = server.getLevel(exterior.dimension());
			if (exteriorLevel != null) {
				ExteriorCabin.upgradeLegacyCornerFrames(exteriorLevel, exterior, cabin.palette());
			}
		});
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
		return level != null && ExteriorCabin.projectionValid(level, exterior, cabin.palette());
	}

	private static void removeProjection(MinecraftServer server, CabinExterior exterior) {
		ServerLevel level = server.getLevel(exterior.dimension());
		if (level != null) {
			CabinRecord cabin = CabinRegistry.get(server).cabins().stream()
				.filter(candidate -> candidate.exterior().filter(exterior::equals).isPresent()
					|| candidate.lastExterior().filter(exterior::equals).isPresent())
				.findFirst().orElse(null);
			ExteriorCabin.removeProjection(level, exterior,
				cabin == null ? CabinPalette.DEFAULT : cabin.palette());
		}
	}

	private static void evacuateOnlineOccupants(MinecraftServer server, CabinRecord cabin) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isOccupant(player, cabin)) {
				continue;
			}
			SafeDestinationResolver.resolveForCabin(player, cabin).ifPresent(destination -> {
				if (destination.teleport(player, 0.0F)) {
					CabinOccupancyData.get(server).clear(player.getUUID());
				}
			});
		}
	}

	static boolean isOccupant(ServerPlayer player, CabinRecord cabin) {
		if (!player.level().dimension().equals(PocketDimension.LEVEL_KEY)) {
			return false;
		}
		return PocketDimension.cellIndexAt(player.blockPosition()).orElse(-1L) == cabin.cellIndex();
	}
}
