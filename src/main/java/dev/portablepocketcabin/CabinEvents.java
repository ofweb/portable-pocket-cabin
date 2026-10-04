package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

import java.util.Set;

final class CabinEvents {
	private CabinEvents() {
	}

	static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel && !CabinCorridors.mayChange(serverLevel, player.getUUID(), pos)) return false;
			if (level instanceof ServerLevel serverLevel && CabinProtection.isProtected(serverLevel, pos)) {
				if (player instanceof ServerPlayer serverPlayer) {
					serverPlayer.sendSystemMessage(Component.literal("That block is part of a protected cabin."), true);
				}
				return false;
			}
			return true;
		});

		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}
			if (!CabinCorridors.mayChange(serverLevel, player.getUUID(), hit.getBlockPos())
				|| !CabinCorridors.mayChange(serverLevel, player.getUUID(), hit.getBlockPos().relative(hit.getDirection()))) {
				return InteractionResult.FAIL;
			}

			CabinRecord exteriorCabin = CabinProtection.findExteriorEntrance(serverLevel, hit.getBlockPos())
				.orElse(null);
			if (exteriorCabin != null) {
				if (player.isShiftKeyDown() && exteriorCabin.exterior().isPresent()
					&& ExteriorCabin.isController(exteriorCabin.exterior().get(), hit.getBlockPos())) {
					return CabinPacking.useController(serverPlayer, exteriorCabin, hit.getBlockPos());
				}
				return enter(serverPlayer, exteriorCabin);
			}

			CabinRecord interiorCabin = CabinProtection.findInteriorExit(serverLevel, hit.getBlockPos())
				.orElse(null);
			if (interiorCabin != null) {
				return leave(serverPlayer, interiorCabin);
			}

			CabinRecord controlledCabin = CabinProtection.findInteriorController(serverLevel, hit.getBlockPos())
				.orElse(null);
			if (controlledCabin != null) {
				return CabinUpgrades.useController(serverPlayer, controlledCabin);
			}

			if (serverLevel.dimension().equals(PocketDimension.LEVEL_KEY)) {
				for (CabinRecord cabin : CabinRegistry.get(serverLevel.getServer()).cabins()) {
					if (cabin.upgrades().enchanting().level() > 0) {
						var supplied = CabinEnchanting.stations(cabin, cabin.upgrades().enchanting().level()).get(hit.getBlockPos());
						if (supplied != null && !supplied.is(net.minecraft.world.level.block.Blocks.BOOKSHELF)) {
							if (!CabinStorage.mayUse(cabin, player.getUUID())) return InteractionResult.FAIL;
							if (supplied.is(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE)) return CabinEnchantingMenu.open(serverPlayer, cabin);
						}
					}
					if (CabinCrafting.stations(cabin, cabin.upgrades().crafting().level()).containsKey(hit.getBlockPos()))
						return CabinStation.open(serverPlayer, cabin, hit.getBlockPos());
					if (cabin.upgrades().storage().level() > 0 && hit.getBlockPos().equals(CabinStorage.control(cabin)))
						return CabinStorage.open(serverPlayer, cabin);
				}
			}
			return InteractionResult.PASS;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			server.execute(() -> {
				CabinStorage.recover(player);
				CabinReconciliation.reconcileOwnerInventory(player);
				recoverOfflineOccupant(player);
			});
		});
		net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
			level instanceof ServerLevel serverLevel && !CabinCorridors.mayChange(serverLevel, player.getUUID(), entity.blockPosition())
				? InteractionResult.FAIL : InteractionResult.PASS);
		net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
			level instanceof ServerLevel serverLevel && !CabinCorridors.mayChange(serverLevel, player.getUUID(), entity.blockPosition())
				? InteractionResult.FAIL : InteractionResult.PASS);
		net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) ->
			level instanceof ServerLevel serverLevel && !CabinCorridors.mayChange(serverLevel, player.getUUID(), player.blockPosition())
				? InteractionResult.FAIL : InteractionResult.PASS);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			var player = handler.player;
			if (!player.level().dimension().equals(PocketDimension.LEVEL_KEY)) return;
			CabinRegistry.get(server).findByCell(PocketDimension.cellIndexAt(player.blockPosition()).orElse(-1L))
				.ifPresent(cabin -> CabinOccupancyData.get(server).enter(player.getUUID(), cabin));
		});

		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) ->
			CabinReconciliation.onChunkLoaded(level, chunk.getPos()));
	}

	private static InteractionResult enter(ServerPlayer player, CabinRecord cabin) {
		ServerLevel exteriorLevel = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(exteriorLevel.getServer());
		CabinRecord current = registry.find(cabin.uuid()).orElse(null);
		if (current == null || current.lifecycle() != CabinLifecycle.DEPLOYED
			|| CabinCorridors.hasPending(exteriorLevel.getServer(), current.uuid())
			|| current.exterior().isEmpty() || !current.exterior().equals(cabin.exterior())) {
			player.sendSystemMessage(Component.literal("That cabin entrance is not active."));
			return InteractionResult.FAIL;
		}
		if (!current.canEnter(player.getUUID())) {
			player.sendSystemMessage(Component.literal("You do not have permission to enter that cabin."));
			return InteractionResult.FAIL;
		}
		if (!CabinReconciliation.hasValidProjection(exteriorLevel.getServer(), current)) {
			CabinReconciliation.reconcile(exteriorLevel.getServer(), current.uuid());
			player.sendSystemMessage(Component.literal("That cabin entrance is incomplete and has been disabled."));
			return InteractionResult.FAIL;
		}

		ServerLevel pocket = exteriorLevel.getServer().getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			player.sendSystemMessage(Component.literal("Pocket dimension is unavailable."));
			return InteractionResult.FAIL;
		}

		current = registry.find(cabin.uuid()).orElse(null);
		if (current == null || current.lifecycle() != CabinLifecycle.DEPLOYED
			|| !current.canEnter(player.getUUID())) {
			player.sendSystemMessage(Component.literal("Cabin access changed before entry completed."));
			return InteractionResult.FAIL;
		}
		var destination = PocketDimension.interiorEntrance(current.cellIndex(), current.progression().generalSize());
		boolean teleported = player.teleportTo(
			pocket,
			destination.getX() + 0.5,
			destination.getY(),
			destination.getZ() + 0.5,
			Set.of(),
			net.minecraft.core.Direction.SOUTH.toYRot(),
			0.0F,
			false
		);
		if (teleported) {
			CabinOccupancyData.get(exteriorLevel.getServer()).enter(player.getUUID(), current);
		}
		return teleported ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
	}

	private static InteractionResult leave(ServerPlayer player, CabinRecord cabin) {
		ServerLevel pocket = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(pocket.getServer());
		CabinRecord current = registry.find(cabin.uuid()).orElse(null);
		if (current == null || current.lifecycle() != CabinLifecycle.DEPLOYED || current.exterior().isEmpty()) {
			player.sendSystemMessage(Component.literal("That cabin has no active exterior entrance."));
			return InteractionResult.FAIL;
		}
		if (!CabinReconciliation.hasValidProjection(pocket.getServer(), current)) {
			CabinReconciliation.reconcile(pocket.getServer(), current.uuid());
			if (!player.level().dimension().equals(PocketDimension.LEVEL_KEY)) {
				CabinOccupancyData.get(pocket.getServer()).clear(player.getUUID());
				return InteractionResult.SUCCESS_SERVER;
			}
			current = registry.find(current.uuid()).orElse(null);
			if (current == null) {
				player.sendSystemMessage(Component.literal("That cabin could not be recovered."));
				return InteractionResult.FAIL;
			}
		}

		var destination = SafeDestinationResolver.resolveForCabin(player, current);
		if (destination.isEmpty()) {
			player.sendSystemMessage(Component.literal("No safe cabin exit destination is currently available."));
			return InteractionResult.FAIL;
		}
		var yawExterior = current.exterior().isPresent() ? current.exterior() : current.lastExterior();
		float yaw = yawExterior
			.map(exterior -> exterior.facing().toYRot())
			.orElse(0.0F);
		if (!destination.get().teleport(player, yaw)) {
			return InteractionResult.FAIL;
		}
		CabinOccupancyData.get(pocket.getServer()).clear(player.getUUID());
		return InteractionResult.SUCCESS_SERVER;
	}

	private static void recoverOfflineOccupant(ServerPlayer player) {
		if (!player.level().dimension().equals(PocketDimension.LEVEL_KEY)) {
			CabinOccupancyData.get(player.level().getServer()).clear(player.getUUID());
			return;
		}
		ServerLevel pocket = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(pocket.getServer());
		long cellIndex = PocketDimension.cellIndexAt(player.blockPosition()).orElse(-1L);
		CabinRecord cabin = registry.findByCell(cellIndex).orElse(null);
		CabinOccupancyData occupancy = CabinOccupancyData.get(pocket.getServer());
		CabinOccupancyData.Stay stay = occupancy.find(player.getUUID()).orElse(null);
		if (cabin != null && requiresMainRoomReturn(player.blockPosition(), cabin, stay,
			CabinCorridors.hasPending(pocket.getServer(), cabin.uuid()))) {
			var safeMain = SafeDestinationResolver.search(pocket, PocketDimension.cellCenter(cabin.cellIndex()).above(), player,
				Math.min(8, cabin.progression().generalSize() / 2))
				.filter(destination -> PocketDimension.isWithinUsable(cabin.cellIndex(), cabin.progression().generalSize(), destination.feet()));
			if (safeMain.isPresent() && safeMain.get().teleport(player, player.getYRot())) {
				occupancy.enter(player.getUUID(), cabin);
				return;
			}
			SafeDestinationResolver.resolveForCabin(player, cabin).ifPresent(destination -> {
				if (destination.teleport(player, 0)) occupancy.clear(player.getUUID());
			});
			return;
		}
		if (cabin == null || !requiresLoginEvacuation(player.blockPosition(), cabin, stay)) {
			return;
		}

		var destination = SafeDestinationResolver.resolveForCabin(player, cabin);
		CabinRecord rechecked = registry.find(cabin.uuid()).orElse(null);
		CabinOccupancyData.Stay recheckedStay = occupancy.find(player.getUUID()).orElse(null);
		if (rechecked == null
			|| !requiresLoginEvacuation(player.blockPosition(), rechecked, recheckedStay)) {
			return;
		}
		if (destination.isPresent() && destination.get().teleport(player, 0.0F)) {
			occupancy.clear(player.getUUID());
			player.sendSystemMessage(Component.literal(
				"Your cabin moved while you were offline, so you were returned to a safe location."
			));
		} else {
			player.sendSystemMessage(Component.literal(
				"No safe destination was available outside your inactive cabin. Ask an operator for help."
			));
		}
	}

	static boolean requiresMainRoomReturn(BlockPos position, CabinRecord cabin, CabinOccupancyData.Stay stay, boolean pendingMove) {
		return cabin.lifecycle() == CabinLifecycle.DEPLOYED && stay != null
			&& stay.cabinId().equals(cabin.uuid()) && stay.packedItemGeneration() == cabin.packedItemGeneration()
			&& (stay.generalSize() != cabin.progression().generalSize() || pendingMove)
			&& !PocketDimension.isWithinUsable(cabin.cellIndex(), cabin.progression().generalSize(), position);
	}

	static boolean requiresLoginEvacuation(
		BlockPos position, CabinRecord cabin, CabinOccupancyData.Stay stay
	) {
		if (PocketDimension.cellIndexAt(position).orElse(-1L) != cabin.cellIndex()) {
			return false;
		}
		return cabin.lifecycle() != CabinLifecycle.DEPLOYED
			|| stay == null
			|| !stay.cabinId().equals(cabin.uuid())
			|| stay.packedItemGeneration() != cabin.packedItemGeneration();
	}
}
