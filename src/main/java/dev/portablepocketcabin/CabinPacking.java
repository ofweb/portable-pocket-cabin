package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CabinPacking {
	private static final int COUNTDOWN_TICKS = 5 * 20;
	private static final double MAX_PACK_DISTANCE_SQUARED = 10.0 * 10.0;
	private static final Map<UUID, PackingTask> TASKS = new LinkedHashMap<>();

	private static final class PackingTask {
		private final UUID cabinId;
		private final UUID ownerId;
		private final long pendingGeneration;
		private final int finishTick;
		private int lastAnnouncedSecond = Integer.MAX_VALUE;

		private PackingTask(UUID cabinId, UUID ownerId, long pendingGeneration, int finishTick) {
			this.cabinId = cabinId;
			this.ownerId = ownerId;
			this.pendingGeneration = pendingGeneration;
			this.finishTick = finishTick;
		}
	}

	private CabinPacking() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(CabinPacking::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			abortForOwner(server, handler.player));
	}

	static int request(ServerPlayer owner) {
		ServerLevel ownerLevel = (ServerLevel) owner.level();
		MinecraftServer server = ownerLevel.getServer();
		CabinRegistry registry = CabinRegistry.get(server);
		CabinRecord cabin = registry.findByOwner(owner.getUUID()).orElse(null);
		if (cabin == null) {
			owner.sendSystemMessage(Component.literal("You do not own a cabin."));
			return 0;
		}
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYED || cabin.exterior().isEmpty()) {
			owner.sendSystemMessage(Component.literal("Your cabin must be DEPLOYED before it can be packed."));
			return 0;
		}
		if (TASKS.containsKey(cabin.uuid())) {
			owner.sendSystemMessage(Component.literal("That cabin is already being packed."));
			return 0;
		}
		if (!CabinReconciliation.hasValidProjection(server, cabin)) {
			CabinReconciliation.reconcile(server, cabin.uuid());
			owner.sendSystemMessage(Component.literal("Packing aborted: the exterior is incomplete and was marked for recovery."));
			return 0;
		}

		CabinExterior exterior = cabin.exterior().get();
		if (!ownerLevel.dimension().equals(exterior.dimension())) {
			owner.sendSystemMessage(Component.literal("Packing must be requested from outside the cabin."));
			return 0;
		}
		var controller = ExteriorCabin.controller(exterior);
		if (owner.distanceToSqr(controller.getX() + 0.5, controller.getY() + 0.5, controller.getZ() + 0.5)
			> MAX_PACK_DISTANCE_SQUARED) {
			owner.sendSystemMessage(Component.literal("Move within 10 blocks of the cabin controller to pack it."));
			return 0;
		}

		int freeSlot = owner.getInventory().getFreeSlot();
		if (freeSlot < 0) {
			owner.sendSystemMessage(Component.literal("Make one inventory slot available for the packed cabin."));
			return 0;
		}

		List<ServerPlayer> occupants = occupants(server, cabin);
		for (ServerPlayer occupant : occupants) {
			if (SafeDestinationResolver.resolveForCabin(occupant, cabin).isEmpty()) {
				owner.sendSystemMessage(Component.literal("Packing aborted: no safe evacuation destination exists for "
					+ occupant.getGameProfile().name() + "."));
				return 0;
			}
		}

		CabinRecord packing;
		try {
			packing = registry.beginPacking(cabin.uuid(), owner.getUUID());
			CabinRegistry.flush(server);
		} catch (IllegalStateException exception) {
			owner.sendSystemMessage(Component.literal(exception.getMessage()));
			return 0;
		}

		long pendingGeneration = packing.packedItemGeneration() + 1;
		owner.getInventory().setItem(freeSlot, CabinItems.createBound(packing, pendingGeneration, true));
		PackingTask task = new PackingTask(
			packing.uuid(), owner.getUUID(), pendingGeneration, server.getTickCount() + COUNTDOWN_TICKS
		);
		task.lastAnnouncedSecond = 5;
		TASKS.put(packing.uuid(), task);
		announce(occupants, 5);
		owner.sendSystemMessage(Component.literal("Packing started. The cabin entrance is locked for 5 seconds."));
		return 1;
	}

	private static void tick(MinecraftServer server) {
		for (PackingTask task : new ArrayList<>(TASKS.values())) {
			CabinRecord cabin = CabinRegistry.get(server).find(task.cabinId).orElse(null);
			if (cabin == null) {
				TASKS.remove(task.cabinId);
				continue;
			}
			if (cabin.lifecycle() != CabinLifecycle.PACKING) {
				ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId);
				if (owner != null) {
					CabinItems.removePending(owner.getInventory(), task.cabinId, task.pendingGeneration);
				}
				TASKS.remove(task.cabinId);
				continue;
			}

			ServerPlayer owner = server.getPlayerList().getPlayer(task.ownerId);
			if (owner == null) {
				abort(server, task, null, "owner disconnected");
				continue;
			}

			int remainingTicks = task.finishTick - server.getTickCount();
			if (remainingTicks <= 0) {
				finish(server, task, owner, cabin);
				continue;
			}

			int seconds = (remainingTicks + 19) / 20;
			if (seconds != task.lastAnnouncedSecond) {
				task.lastAnnouncedSecond = seconds;
				announce(occupants(server, cabin), seconds);
			}
		}
	}

	private static void finish(MinecraftServer server, PackingTask task, ServerPlayer owner, CabinRecord cabin) {
		int pendingSlot = CabinItems.findPending(owner.getInventory(), cabin, task.pendingGeneration);
		if (pendingSlot < 0) {
			abort(server, task, owner, "reserved inventory item is no longer available");
			return;
		}

		List<ServerPlayer> occupants = occupants(server, cabin);
		List<SafeDestinationResolver.Destination> destinations = new ArrayList<>();
		for (ServerPlayer occupant : occupants) {
			var destination = SafeDestinationResolver.resolveForCabin(occupant, cabin);
			if (destination.isEmpty()) {
				abort(server, task, owner, "an occupant no longer has a safe evacuation destination");
				return;
			}
			destinations.add(destination.get());
		}

		for (int index = 0; index < occupants.size(); index++) {
			if (!destinations.get(index).teleport(occupants.get(index), 0.0F)) {
				abort(server, task, owner, "an occupant could not be evacuated");
				return;
			}
		}

		CabinExterior exterior = cabin.exterior().orElseThrow();
		CabinRecord packed = CabinRegistry.get(server).finishPacking(cabin.uuid());
		CabinRegistry.flush(server);
		ServerLevel exteriorLevel = server.getLevel(exterior.dimension());
		if (exteriorLevel != null) {
			ExteriorCabin.removeProjection(exteriorLevel, exterior);
		}
		CabinRegistry.get(server).markExteriorCleanupComplete(cabin.uuid());
		ItemStack reservedItem = owner.getInventory().getItem(pendingSlot);
		CabinItems.activatePending(reservedItem);
		TASKS.remove(task.cabinId);
		owner.sendSystemMessage(Component.literal("Cabin packed. Its interior remains unchanged."));
		PortablePocketCabin.LOGGER.info("Packed cabin {} at item generation {}", packed.uuid(), packed.packedItemGeneration());
	}

	private static void abortForOwner(MinecraftServer server, ServerPlayer owner) {
		for (PackingTask task : new ArrayList<>(TASKS.values())) {
			if (task.ownerId.equals(owner.getUUID())) {
				abort(server, task, owner, "owner disconnected");
			}
		}
	}

	private static void abort(
		MinecraftServer server, PackingTask task, ServerPlayer owner, String reason
	) {
		CabinRegistry registry = CabinRegistry.get(server);
		CabinRecord cabin = registry.find(task.cabinId).orElse(null);
		if (cabin != null && cabin.lifecycle() == CabinLifecycle.PACKING) {
			registry.abortPacking(cabin.uuid());
		}
		if (owner != null) {
			CabinItems.removePending(owner.getInventory(), task.cabinId, task.pendingGeneration);
			owner.sendSystemMessage(Component.literal("Packing aborted: " + reason + "."));
		}
		TASKS.remove(task.cabinId);
		PortablePocketCabin.LOGGER.info("Aborted packing cabin {}: {}", task.cabinId, reason);
	}

	private static List<ServerPlayer> occupants(MinecraftServer server, CabinRecord cabin) {
		return server.getPlayerList().getPlayers().stream()
			.filter(player -> CabinReconciliation.isOccupant(player, cabin))
			.toList();
	}

	private static void announce(List<ServerPlayer> occupants, int seconds) {
		for (ServerPlayer occupant : occupants) {
			occupant.sendSystemMessage(Component.literal("Cabin is being packed in " + seconds + " seconds…"));
		}
	}
}
