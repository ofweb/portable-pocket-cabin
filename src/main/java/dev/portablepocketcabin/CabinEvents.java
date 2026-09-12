package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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
	}

	private static InteractionResult enter(ServerPlayer player, CabinRecord cabin) {
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYED) {
			player.sendSystemMessage(Component.literal("That cabin entrance is not active."));
			return InteractionResult.FAIL;
		}
		if (!cabin.owner().equals(player.getUUID())) {
			player.sendSystemMessage(Component.literal("Only the owner may enter during Delivery 3."));
			return InteractionResult.FAIL;
		}

		ServerLevel pocket = ((ServerLevel) player.level()).getServer().getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			player.sendSystemMessage(Component.literal("Pocket dimension is unavailable."));
			return InteractionResult.FAIL;
		}

		var destination = PocketDimension.interiorEntrance(cabin.cellIndex());
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

		CabinExterior exterior = cabin.exterior().get();
		ServerLevel exteriorLevel = ((ServerLevel) player.level()).getServer().getLevel(exterior.dimension());
		if (exteriorLevel == null) {
			player.sendSystemMessage(Component.literal("The cabin exterior dimension is unavailable."));
			return InteractionResult.FAIL;
		}

		var destination = ExteriorCabin.outsideDestination(exterior);
		player.teleportTo(
			exteriorLevel,
			destination.getX() + 0.5,
			destination.getY(),
			destination.getZ() + 0.5,
			Set.of(),
			exterior.facing().toYRot(),
			0.0F,
			false
		);
		return InteractionResult.SUCCESS_SERVER;
	}
}
