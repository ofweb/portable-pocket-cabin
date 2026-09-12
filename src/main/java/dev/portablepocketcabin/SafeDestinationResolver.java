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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class SafeDestinationResolver {
	private static final int SEARCH_RADIUS = 8;
	private static final int TICKET_RADIUS = 1;
	private static final long DEFAULT_SEARCH_BUDGET_NANOS = 250_000_000L;
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
		long deadline = deadlineAfter(DEFAULT_SEARCH_BUDGET_NANOS);
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
			Optional<Destination> destination = searchUntil(
				level, ExteriorCabin.outsideDestination(campsite), player, SEARCH_RADIUS, deadline
			);
			if (destination.isPresent()) {
				return destination;
			}
		}

		ServerLevel overworld = server.overworld();
		return searchUntil(overworld, overworld.getRespawnData().pos().above(), player, SEARCH_RADIUS, deadline);
	}

	static Optional<Destination> search(ServerLevel level, BlockPos preferredFeet, ServerPlayer player) {
		return searchUntil(
			level, preferredFeet, player, SEARCH_RADIUS, deadlineAfter(DEFAULT_SEARCH_BUDGET_NANOS)
		);
	}

	static Optional<Destination> search(
		ServerLevel level, BlockPos preferredFeet, ServerPlayer player, int radius
	) {
		if (radius < 0 || radius > SEARCH_RADIUS) {
			throw new IllegalArgumentException("Safe destination radius must be between 0 and " + SEARCH_RADIUS);
		}
		return searchUntil(level, preferredFeet, player, radius, deadlineAfter(DEFAULT_SEARCH_BUDGET_NANOS));
	}

	static Optional<Destination> resolveExact(ServerLevel level, BlockPos preferredFeet, ServerPlayer player) {
		return searchUntil(level, preferredFeet, player, 0, deadlineAfter(DEFAULT_SEARCH_BUDGET_NANOS));
	}

	static Optional<Destination> searchUntil(
		ServerLevel level, BlockPos preferredFeet, ServerPlayer player, int radius, long deadline
	) {
		if (radius < 0 || radius > SEARCH_RADIUS) {
			throw new IllegalArgumentException("Safe destination radius must be between 0 and " + SEARCH_RADIUS);
		}
		Set<ChunkPos> ticketedChunks = new LinkedHashSet<>();
		try {
			boolean firstCandidate = true;
			for (BlockPos candidate : candidates(preferredFeet, radius)) {
				if (!firstCandidate && deadlineReached(deadline)) {
					return Optional.empty();
				}
				firstCandidate = false;
				if (!level.isInWorldBounds(candidate)
					|| !level.getWorldBorder().isWithinBounds(candidate)) {
					continue;
				}
				ChunkPos chunk = ChunkPos.containing(candidate);
				if (!ensureChunkLoaded(level, chunk, ticketedChunks, deadline)) {
					return Optional.empty();
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

	static Optional<Destination> searchSurfaceUntil(
		ServerLevel level, int preferredX, int preferredZ, ServerPlayer player, int radius, long deadline
	) {
		if (radius < 0 || radius > SEARCH_RADIUS) {
			throw new IllegalArgumentException("Safe destination radius must be between 0 and " + SEARCH_RADIUS);
		}
		Set<ChunkPos> ticketedChunks = new LinkedHashSet<>();
		try {
			boolean firstCandidate = true;
			for (int ring = 0; ring <= radius; ring++) {
				for (int dx = -ring; dx <= ring; dx++) {
					for (int dz = -ring; dz <= ring; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
							continue;
						}
						if (!firstCandidate && deadlineReached(deadline)) {
							return Optional.empty();
						}
						firstCandidate = false;
						int x = preferredX + dx;
						int z = preferredZ + dz;
						BlockPos horizontal = new BlockPos(x, level.getMinY(), z);
						if (!level.getWorldBorder().isWithinBounds(horizontal)) {
							continue;
						}
						ChunkPos chunk = ChunkPos.containing(horizontal);
						if (!ensureChunkLoaded(level, chunk, ticketedChunks, deadline)) {
							return Optional.empty();
						}
						int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
						for (int dy : Y_OFFSETS) {
							BlockPos candidate = new BlockPos(x, surfaceY + dy, z);
							if (isSafe(level, candidate, player)) {
								return Optional.of(new Destination(level, candidate));
							}
						}
					}
				}
			}
			return Optional.empty();
		} finally {
			releaseTickets(level, ticketedChunks);
		}
	}

	private static boolean ensureChunkLoaded(
		ServerLevel level, ChunkPos chunk, Set<ChunkPos> ticketedChunks, long deadline
	) {
		if (ticketedChunks.contains(chunk)) {
			return true;
		}
		if (level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) != null) {
			level.getChunkSource().addTicketWithRadius(SAFE_SEARCH_TICKET, chunk, TICKET_RADIUS);
			ticketedChunks.add(chunk);
			return true;
		}
		if (deadlineReached(deadline)) {
			return false;
		}

		ticketedChunks.add(chunk);
		CompletableFuture<?> loading = level.getChunkSource().addTicketAndLoadWithRadius(
			SAFE_SEARCH_TICKET, chunk, TICKET_RADIUS
		);
		level.getServer().managedBlock(() -> loading.isDone() || deadlineReached(deadline));
		return loading.isDone() && level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) != null;
	}

	private static void releaseTickets(ServerLevel level, Set<ChunkPos> ticketedChunks) {
		for (ChunkPos chunk : ticketedChunks) {
			level.getChunkSource().removeTicketWithRadius(SAFE_SEARCH_TICKET, chunk, TICKET_RADIUS);
		}
	}

	private static long deadlineAfter(long budgetNanos) {
		return System.nanoTime() + budgetNanos;
	}

	private static boolean deadlineReached(long deadline) {
		return System.nanoTime() - deadline >= 0;
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
