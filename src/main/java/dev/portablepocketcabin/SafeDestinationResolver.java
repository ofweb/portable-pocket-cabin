package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class SafeDestinationResolver {
	private static final int SEARCH_RADIUS = 8;
	private static final int TICKET_RADIUS = 1;
	private static final TicketType SAFE_SEARCH_TICKET = new TicketType(40L, TicketType.FLAG_LOADING);
	private static final EntityDimensions STANDING_PLAYER = EntityDimensions.fixed(0.6F, 1.8F);
	private static final int[] Y_OFFSETS = {0, 1, -1, 2, -2, 3, -3};

	record Destination(ServerLevel level, BlockPos feet) {
		boolean teleport(ServerPlayer player, float yaw) {
			return player.teleportTo(
				level,
				feet.getX() + 0.5,
				feet.getY(),
				feet.getZ() + 0.5,
				Set.of(),
				yaw,
				0.0F,
				false
			);
		}
	}

	private SafeDestinationResolver() {
	}

	static Optional<Destination> resolveForCabin(ServerPlayer player, CabinRecord cabin) {
		MinecraftServer server = ((ServerLevel) player.level()).getServer();
		List<CabinExterior> campsites = new ArrayList<>();
		cabin.exterior().ifPresent(campsites::add);
		cabin.lastExterior().ifPresent(last -> {
			if (!campsites.contains(last)) {
				campsites.add(last);
			}
		});

		for (CabinExterior campsite : campsites) {
			ServerLevel level = server.getLevel(campsite.dimension());
			if (level == null) {
				continue;
			}
			Optional<Destination> destination = search(
				level, ExteriorCabin.outsideDestination(campsite), player
			);
			if (destination.isPresent()) {
				return destination;
			}
		}

		ServerLevel overworld = server.overworld();
		return search(overworld, overworld.getRespawnData().pos().above(), player);
	}

	static Optional<Destination> search(ServerLevel level, BlockPos preferredFeet, ServerPlayer player) {
		return search(level, preferredFeet, player, SEARCH_RADIUS);
	}

	static Optional<Destination> search(
		ServerLevel level, BlockPos preferredFeet, ServerPlayer player, int radius
	) {
		if (radius < 0 || radius > SEARCH_RADIUS) {
			throw new IllegalArgumentException("Safe destination radius must be between 0 and " + SEARCH_RADIUS);
		}
		return searchWithinRadius(level, preferredFeet, player, radius);
	}

	static Optional<Destination> resolveExact(ServerLevel level, BlockPos preferredFeet, ServerPlayer player) {
		return searchWithinRadius(level, preferredFeet, player, 0);
	}

	private static Optional<Destination> searchWithinRadius(
		ServerLevel level, BlockPos preferredFeet, ServerPlayer player, int radius
	) {
		Set<ChunkPos> ticketedChunks = new LinkedHashSet<>();
		try {
			for (BlockPos candidate : candidates(preferredFeet, radius)) {
				if (!level.isInWorldBounds(candidate)
					|| !level.getWorldBorder().isWithinBounds(candidate)) {
					continue;
				}
				ChunkPos chunk = ChunkPos.containing(candidate);
				if (ticketedChunks.add(chunk)) {
					level.getChunkSource().addTicketWithRadius(SAFE_SEARCH_TICKET, chunk, TICKET_RADIUS);
					level.getChunk(chunk.x(), chunk.z());
				}
				if (isSafe(level, candidate, player)) {
					return Optional.of(new Destination(level, candidate.immutable()));
				}
			}
			return Optional.empty();
		} finally {
			for (ChunkPos chunk : ticketedChunks) {
				level.getChunkSource().removeTicketWithRadius(SAFE_SEARCH_TICKET, chunk, TICKET_RADIUS);
			}
		}
	}

	static boolean isSafe(ServerLevel level, BlockPos feet, ServerPlayer player) {
		BlockPos floor = feet.below();
		BlockPos head = feet.above();
		if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(head)
			|| !level.getWorldBorder().isWithinBounds(feet)
			|| !level.getWorldBorder().isWithinBounds(head)) {
			return false;
		}

		BlockState floorState = level.getBlockState(floor);
		if (!floorState.isFaceSturdy(level, floor, Direction.UP)) {
			return false;
		}
		if (isHazard(level, floor) || isHazard(level, feet) || isHazard(level, head)) {
			return false;
		}

		AABB bounds = STANDING_PLAYER.makeBoundingBox(
			feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5
		);
		return level.getWorldBorder().isWithinBounds(bounds) && level.noCollision(player, bounds);
	}

	private static boolean isHazard(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (level.getFluidState(pos).is(FluidTags.LAVA)) {
			return true;
		}
		return state.is(BlockTags.FIRE)
			|| state.is(BlockTags.CAMPFIRES)
			|| state.is(Blocks.MAGMA_BLOCK)
			|| state.is(Blocks.CACTUS)
			|| state.is(Blocks.POWDER_SNOW)
			|| state.is(Blocks.SWEET_BERRY_BUSH)
			|| state.is(Blocks.WITHER_ROSE);
	}

	private static List<BlockPos> candidates(BlockPos preferred, int maximumRadius) {
		List<BlockPos> positions = new ArrayList<>();
		for (int radius = 0; radius <= maximumRadius; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					for (int dy : Y_OFFSETS) {
						positions.add(preferred.offset(dx, dy, dz));
					}
				}
			}
		}
		return positions;
	}
}
