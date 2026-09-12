package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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

			CabinRecord exteriorCabin = CabinProtection.findExteriorEntrance(serverLevel, hit.getBlockPos())
				.orElse(null);
			if (exteriorCabin != null) {
				return enter(serverPlayer, exteriorCabin);
			}

			CabinRecord interiorCabin = CabinProtection.findInteriorExit(serverLevel, hit.getBlockPos())
				.orElse(null);
			if (interiorCabin != null) {
				return leave(serverPlayer, interiorCabin);
			}

			return InteractionResult.PASS;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			server.execute(() -> recoverOfflineOccupant(player));
		});
	}

	private static InteractionResult enter(ServerPlayer player, CabinRecord cabin) {
		ServerLevel exteriorLevel = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(exteriorLevel.getServer());
		CabinRecord current = registry.find(cabin.uuid()).orElse(null);
		if (current == null || current.lifecycle() != CabinLifecycle.DEPLOYED
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
		var destination = PocketDimension.interiorEntrance(current.cellIndex());
		player.teleportTo(
			pocket,
			destination.getX() + 0.5,
			destination.getY(),
			destination.getZ() + 0.5,
			Set.of(),
			net.minecraft.core.Direction.SOUTH.toYRot(),
			0.0F,
			false
		);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult leave(ServerPlayer player, CabinRecord cabin) {
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYED || cabin.exterior().isEmpty()) {
			player.sendSystemMessage(Component.literal("That cabin has no active exterior entrance."));
			return InteractionResult.FAIL;
		}

		var destination = SafeDestinationResolver.resolveForCabin(player, cabin);
		if (destination.isEmpty()) {
			player.sendSystemMessage(Component.literal("No safe cabin exit destination is currently available."));
			return InteractionResult.FAIL;
		}
		return destination.get().teleport(player, cabin.exterior().get().facing().toYRot())
			? InteractionResult.SUCCESS_SERVER
			: InteractionResult.FAIL;
	}

	private static void recoverOfflineOccupant(ServerPlayer player) {
		if (!player.level().dimension().equals(PocketDimension.LEVEL_KEY)) {
			return;
		}
		ServerLevel pocket = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(pocket.getServer());
		long cellIndex = PocketDimension.cellIndexAt(player.blockPosition()).orElse(-1L);
		CabinRecord cabin = registry.findByCell(cellIndex).orElse(null);
		if (cabin == null || !requiresLoginEvacuation(player.blockPosition(), cabin)) {
			return;
		}

		var destination = SafeDestinationResolver.resolveForCabin(player, cabin);
		CabinRecord rechecked = registry.find(cabin.uuid()).orElse(null);
		if (rechecked == null || rechecked.lifecycle() == CabinLifecycle.DEPLOYED) {
			return;
		}
		if (destination.isPresent() && destination.get().teleport(player, 0.0F)) {
			player.sendSystemMessage(Component.literal(
				"Your cabin moved while you were offline, so you were returned to a safe location."
			));
		} else {
			player.sendSystemMessage(Component.literal(
				"No safe destination was available outside your inactive cabin. Ask an operator for help."
			));
		}
	}

	static boolean requiresLoginEvacuation(BlockPos position, CabinRecord cabin) {
		return cabin.lifecycle() != CabinLifecycle.DEPLOYED
			&& PocketDimension.cellIndexAt(position).orElse(-1L) == cabin.cellIndex();
	}
}
