package dev.portablepocketcabin;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class CabinRespawning {
	static final int DEFAULT_MIN_DISTANCE = 128;
	static final int DEFAULT_MAX_DISTANCE = 256;
	static final int MAX_CANDIDATE_REGIONS = 16;
	private static final int REGION_SEARCH_RADIUS = 4;
	private static final long SEARCH_BUDGET_NANOS = 50_000_000L;

	static final GameRule<Integer> MIN_DISTANCE = GameRuleBuilder.forInteger(DEFAULT_MIN_DISTANCE)
		.category(GameRuleCategory.PLAYER)
		.range(1, 30_000_000)
		.buildAndRegister(PortablePocketCabin.id("respawn_min_distance"));
	static final GameRule<Integer> MAX_DISTANCE = GameRuleBuilder.forInteger(DEFAULT_MAX_DISTANCE)
		.category(GameRuleCategory.PLAYER)
		.range(1, 30_000_000)
		.buildAndRegister(PortablePocketCabin.id("respawn_max_distance"));

	private CabinRespawning() {
	}

	static void register() {
		EntitySleepEvents.START_SLEEPING.register((entity, position) -> {
			if (entity instanceof ServerPlayer player) {
				bindAfterSleeping(player, position);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register(CabinRespawning::afterRespawn);
	}

	private static void bindAfterSleeping(ServerPlayer player, BlockPos position) {
		ServerLevel level = player.level();
		if (!level.dimension().equals(PocketDimension.LEVEL_KEY)) {
			return;
		}

		BlockPos bedPosition = normalizedBedHead(level, position).orElse(null);
		if (bedPosition == null) {
			return;
		}
		long cellIndex = PocketDimension.cellIndexAt(bedPosition).orElse(-1L);
		CabinRecord cabin = CabinRegistry.get(level.getServer()).findByCell(cellIndex).orElse(null);
		if (cabin == null) {
			return;
		}

		CabinRespawnData data = CabinRespawnData.get(level.getServer());
		if (bindOwner(data, player.getUUID(), cabin, bedPosition)) {
			player.sendSystemMessage(Component.literal("This cabin bed is now your cabin home."), true);
		}
	}

	static boolean bindOwner(
		CabinRespawnData data, UUID playerId, CabinRecord cabin, BlockPos bedPosition
	) {
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYED
			|| !cabin.owner().equals(playerId)
			|| PocketDimension.cellIndexAt(bedPosition).orElse(-1L) != cabin.cellIndex()) {
			return false;
		}
		data.bind(playerId, cabin.uuid(), bedPosition);
		return true;
	}

	private static void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		if (alive) {
			return;
		}
		MinecraftServer server = newPlayer.level().getServer();
		CabinHomeBinding binding = CabinRespawnData.get(server).find(newPlayer.getUUID()).orElse(null);
		if (binding == null) {
			return;
		}
		CabinRecord cabin = CabinRegistry.get(server).find(binding.cabinId()).orElse(null);
		if (cabin == null || !cabin.owner().equals(newPlayer.getUUID())) {
			return;
		}

		ServerLevel deathLevel = oldPlayer.level();
		BlockPos deathPosition = oldPlayer.blockPosition().immutable();
		if (tryCabinRespawn(newPlayer, deathLevel, deathPosition, binding, cabin)) {
			CabinOccupancyData occupancy = CabinOccupancyData.get(server);
			CabinRecord current = CabinRegistry.get(server).find(binding.cabinId()).orElse(null);
			if (newPlayer.level().dimension().equals(PocketDimension.LEVEL_KEY) && current != null) {
				occupancy.enter(newPlayer.getUUID(), current);
			} else {
				occupancy.clear(newPlayer.getUUID());
			}
			newPlayer.sendSystemMessage(Component.literal("You returned to your cabin home."));
		}
	}

	private static boolean tryCabinRespawn(
		ServerPlayer player,
		ServerLevel deathLevel,
		BlockPos deathPosition,
		CabinHomeBinding binding,
		CabinRecord initialCabin
	) {
		MinecraftServer server = player.level().getServer();
		CabinRegistry registry = CabinRegistry.get(server);
		CabinRecord cabin = initialCabin;

		if (canUseInterior(binding, cabin)) {
			Optional<SafeDestinationResolver.Destination> bedside = findBedside(player, cabin, binding);
			if (bedside.isPresent() && interiorStillValid(player, binding, bedside.get())) {
				return bedside.get().teleport(player, 0.0F);
			}
		}

		cabin = registry.find(binding.cabinId()).orElse(null);
		if (canUseInterior(binding, cabin) && cabin.exterior().isPresent()) {
			CabinExterior exterior = cabin.exterior().get();
			ServerLevel exteriorLevel = server.getLevel(exterior.dimension());
			if (exteriorLevel != null) {
				Optional<SafeDestinationResolver.Destination> outside = SafeDestinationResolver.search(
					exteriorLevel, ExteriorCabin.outsideDestination(exterior), player
				);
				if (outside.isPresent() && exteriorStillValid(player, binding, exterior, outside.get())) {
					return outside.get().teleport(player, exterior.facing().toYRot());
				}
			}
		}

		cabin = registry.find(binding.cabinId()).orElse(null);
		if (cabin == null) {
			return teleportToWorldSpawn(player);
		}
		if (shouldSearchNearDeath(cabin)) {
			Optional<SafeDestinationResolver.Destination> nearby = findNearDeath(
				player, deathLevel, deathPosition
			);
			if (nearby.isPresent() && nearDeathStillValid(player, binding, nearby.get())) {
				return nearby.get().teleport(player, player.getYRot());
			}
		}

		cabin = registry.find(binding.cabinId()).orElse(null);
		if (cabin != null && cabin.lastExterior().isPresent()) {
			CabinExterior lastExterior = cabin.lastExterior().get();
			ServerLevel lastLevel = server.getLevel(lastExterior.dimension());
			if (lastLevel != null) {
				Optional<SafeDestinationResolver.Destination> last = SafeDestinationResolver.search(
					lastLevel, ExteriorCabin.outsideDestination(lastExterior), player
				);
				if (last.isPresent() && fallbackStillValid(player, binding, lastExterior, last.get())) {
					return last.get().teleport(player, lastExterior.facing().toYRot());
				}
			}
		}

		return teleportToWorldSpawn(player);
	}

	static boolean canUseInterior(CabinHomeBinding binding, CabinRecord cabin) {
		return cabin != null
			&& cabin.uuid().equals(binding.cabinId())
			&& cabin.owner().equals(binding.playerId())
			&& cabin.lifecycle() == CabinLifecycle.DEPLOYED;
	}

	static boolean shouldSearchNearDeath(CabinRecord cabin) {
		return cabin != null && cabin.lifecycle() != CabinLifecycle.DEPLOYED;
	}

	private static Optional<SafeDestinationResolver.Destination> findBedside(
		ServerPlayer player, CabinRecord cabin, CabinHomeBinding binding
	) {
		ServerLevel pocket = player.level().getServer().getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null || !validBed(pocket, cabin, binding.bedPosition())) {
			return Optional.empty();
		}

		BlockState headState = pocket.getBlockState(binding.bedPosition());
		Direction facing = headState.getValue(HorizontalDirectionalBlock.FACING);
		BlockPos foot = binding.bedPosition().relative(facing.getOpposite());
		List<BlockPos> candidates = List.of(
			binding.bedPosition().relative(facing.getClockWise()),
			binding.bedPosition().relative(facing.getCounterClockWise()),
			foot.relative(facing.getClockWise()),
			foot.relative(facing.getCounterClockWise()),
			binding.bedPosition().relative(facing),
			foot.relative(facing.getOpposite())
		);
		for (BlockPos candidate : candidates) {
			Optional<SafeDestinationResolver.Destination> destination =
				SafeDestinationResolver.resolveExact(pocket, candidate, player);
			if (destination.isPresent()) {
				return destination;
			}
		}
		return Optional.empty();
	}

	private static boolean validBed(ServerLevel pocket, CabinRecord cabin, BlockPos head) {
		if (PocketDimension.cellIndexAt(head).orElse(-1L) != cabin.cellIndex()) {
			return false;
		}
		BlockState headState = pocket.getBlockState(head);
		if (!(headState.getBlock() instanceof BedBlock) || headState.getValue(BedBlock.PART) != BedPart.HEAD) {
			return false;
		}
		Direction facing = headState.getValue(HorizontalDirectionalBlock.FACING);
		BlockState footState = pocket.getBlockState(head.relative(facing.getOpposite()));
		return footState.getBlock() instanceof BedBlock
			&& footState.getValue(BedBlock.PART) == BedPart.FOOT
			&& footState.getValue(HorizontalDirectionalBlock.FACING) == facing;
	}

	private static Optional<BlockPos> normalizedBedHead(ServerLevel level, BlockPos position) {
		BlockState state = level.getBlockState(position);
		if (!(state.getBlock() instanceof BedBlock)) {
			return Optional.empty();
		}
		if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
			return Optional.of(position.immutable());
		}
		return Optional.of(position.relative(state.getValue(HorizontalDirectionalBlock.FACING)).immutable());
	}

	private static Optional<SafeDestinationResolver.Destination> findNearDeath(
		ServerPlayer player, ServerLevel deathLevel, BlockPos deathPosition
	) {
		if (!allowsNearDeathSearch(deathLevel.dimension())) {
			return Optional.empty();
		}
		MinecraftServer server = deathLevel.getServer();
		int minimum = server.getGameRules().get(MIN_DISTANCE);
		int maximum = Math.max(minimum, server.getGameRules().get(MAX_DISTANCE));
		long deadline = System.nanoTime() + SEARCH_BUDGET_NANOS;

		for (BlockPos offset : candidateOffsets(player.getUUID(), minimum, maximum)) {
			if (System.nanoTime() >= deadline) {
				break;
			}
			int x = deathPosition.getX() + offset.getX();
			int z = deathPosition.getZ() + offset.getZ();
			Optional<SafeDestinationResolver.Destination> destination;
			if (deathLevel.dimension().equals(Level.NETHER)) {
				int y = Math.clamp(deathPosition.getY(), deathLevel.getMinY() + 1, deathLevel.getMaxY() - 2);
				destination = SafeDestinationResolver.searchUntil(
					deathLevel, new BlockPos(x, y, z), player, REGION_SEARCH_RADIUS, deadline
				);
			} else {
				destination = SafeDestinationResolver.searchSurfaceUntil(
					deathLevel, x, z, player, REGION_SEARCH_RADIUS, deadline
				);
			}
			if (destination.isPresent()
				&& withinHorizontalBounds(deathPosition, destination.get().feet(), minimum, maximum)) {
				return destination;
			}
		}
		return Optional.empty();
	}

	static boolean allowsNearDeathSearch(ResourceKey<Level> dimension) {
		return dimension.equals(Level.OVERWORLD)
			|| dimension.equals(Level.NETHER)
			|| dimension.equals(Level.END);
	}

	static List<BlockPos> candidateOffsets(UUID playerId, int minimum, int maximum) {
		if (minimum < 1 || maximum < minimum) {
			throw new IllegalArgumentException("Respawn search bounds must satisfy 1 <= minimum <= maximum");
		}
		List<BlockPos> candidates = new ArrayList<>(MAX_CANDIDATE_REGIONS);
		long mixed = playerId.getMostSignificantBits() ^ Long.rotateLeft(playerId.getLeastSignificantBits(), 23);
		double phase = Math.floorMod(mixed, 65_536L) * (Math.PI * 2.0 / 65_536.0);
		double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
		for (int index = 0; index < MAX_CANDIDATE_REGIONS; index++) {
			double fraction = index / (double) (MAX_CANDIDATE_REGIONS - 1);
			double radius = minimum + (maximum - minimum) * fraction;
			double angle = phase + goldenAngle * index;
			int dx = (int) Math.round(Math.cos(angle) * radius);
			int dz = (int) Math.round(Math.sin(angle) * radius);
			double actualDistance = Math.hypot(dx, dz);
			if (actualDistance < minimum || actualDistance > maximum) {
				int cardinalDistance = minimum + (int) Math.floor((maximum - minimum) * fraction);
				dx = switch (index & 3) {
					case 0 -> cardinalDistance;
					case 2 -> -cardinalDistance;
					default -> 0;
				};
				dz = switch (index & 3) {
					case 1 -> cardinalDistance;
					case 3 -> -cardinalDistance;
					default -> 0;
				};
			}
			candidates.add(new BlockPos(dx, 0, dz));
		}
		return List.copyOf(candidates);
	}

	static boolean withinHorizontalBounds(BlockPos origin, BlockPos candidate, int minimum, int maximum) {
		double distance = Math.hypot(candidate.getX() - origin.getX(), candidate.getZ() - origin.getZ());
		return distance >= minimum && distance <= maximum;
	}

	private static boolean interiorStillValid(
		ServerPlayer player, CabinHomeBinding expectedBinding, SafeDestinationResolver.Destination destination
	) {
		MinecraftServer server = player.level().getServer();
		CabinHomeBinding binding = CabinRespawnData.get(server).find(player.getUUID()).orElse(null);
		CabinRecord cabin = CabinRegistry.get(server).find(expectedBinding.cabinId()).orElse(null);
		return expectedBinding.equals(binding)
			&& canUseInterior(expectedBinding, cabin)
			&& destination.level().dimension().equals(PocketDimension.LEVEL_KEY)
			&& validBed(destination.level(), cabin, expectedBinding.bedPosition())
			&& SafeDestinationResolver.isSafe(destination.level(), destination.feet(), player);
	}

	private static boolean exteriorStillValid(
		ServerPlayer player,
		CabinHomeBinding expectedBinding,
		CabinExterior expectedExterior,
		SafeDestinationResolver.Destination destination
	) {
		MinecraftServer server = player.level().getServer();
		CabinHomeBinding binding = CabinRespawnData.get(server).find(player.getUUID()).orElse(null);
		CabinRecord cabin = CabinRegistry.get(server).find(expectedBinding.cabinId()).orElse(null);
		return expectedBinding.equals(binding)
			&& canUseInterior(expectedBinding, cabin)
			&& cabin.exterior().filter(expectedExterior::equals).isPresent()
			&& destination.level().dimension().equals(expectedExterior.dimension())
			&& SafeDestinationResolver.isSafe(destination.level(), destination.feet(), player);
	}

	private static boolean nearDeathStillValid(
		ServerPlayer player, CabinHomeBinding expectedBinding, SafeDestinationResolver.Destination destination
	) {
		MinecraftServer server = player.level().getServer();
		CabinHomeBinding binding = CabinRespawnData.get(server).find(player.getUUID()).orElse(null);
		CabinRecord cabin = CabinRegistry.get(server).find(expectedBinding.cabinId()).orElse(null);
		return expectedBinding.equals(binding)
			&& cabin != null
			&& cabin.lifecycle() != CabinLifecycle.DEPLOYED
			&& allowsNearDeathSearch(destination.level().dimension())
			&& SafeDestinationResolver.isSafe(destination.level(), destination.feet(), player);
	}

	private static boolean fallbackStillValid(
		ServerPlayer player,
		CabinHomeBinding expectedBinding,
		CabinExterior expectedExterior,
		SafeDestinationResolver.Destination destination
	) {
		MinecraftServer server = player.level().getServer();
		CabinHomeBinding binding = CabinRespawnData.get(server).find(player.getUUID()).orElse(null);
		CabinRecord cabin = CabinRegistry.get(server).find(expectedBinding.cabinId()).orElse(null);
		return expectedBinding.equals(binding)
			&& cabin != null
			&& cabin.lastExterior().filter(expectedExterior::equals).isPresent()
			&& destination.level().dimension().equals(expectedExterior.dimension())
			&& SafeDestinationResolver.isSafe(destination.level(), destination.feet(), player);
	}

	private static boolean teleportToWorldSpawn(ServerPlayer player) {
		ServerLevel overworld = player.level().getServer().overworld();
		Optional<SafeDestinationResolver.Destination> spawn = SafeDestinationResolver.search(
			overworld, overworld.getRespawnData().pos().above(), player
		);
		return spawn.isPresent() && spawn.get().teleport(player, 0.0F);
	}
}
