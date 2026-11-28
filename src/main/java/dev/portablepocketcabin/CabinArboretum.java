package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.*;

/**
 * The arboretum grove (B-0023 part 1). Each cabin's grove lies in its own region of the arboretum dimension, reached
 * through a door in the west corridor. Grove positions are relative to the region's anchor: the inside of the exit
 * door, on the grass floor. x runs east, z runs south, and y 0 is the grass.
 */
public final class CabinArboretum {
	static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, PortablePocketCabin.id("arboretum"));
	static final int[] SIZES = {0, 7, 19, 29};
	static final int[] HEIGHTS = {0, 9, 12, 15};
	static final int REGION_SPACING = 32_768;
	static final int REGION_COLUMNS = 1_800;
	static final int REGION_ORIGIN = -(REGION_COLUMNS - 1) * (REGION_SPACING / 2);
	static final int FLOOR_Y = 64;
	/** The grid spots' x and z offsets; a size uses those inside its floor with a block to spare before the walls. */
	private static final int[] GRID_X = {-10, -6, -2, 2, 6, 10};
	private static final int[] GRID_Z = {1, 5, 9, 13, 17, 21, 25};
	static final BlockPos EXIT = new BlockPos(0, 1, -1);
	static final BlockPos ARRIVAL = new BlockPos(0, 1, 0);
	static final BlockPos BOARD = new BlockPos(1, 1, 0);
	private CabinArboretum() { }

	// Regions

	static boolean hasRegion(CabinRecord cabin) {
		return cabin.cellIndex() < (long) REGION_COLUMNS * REGION_COLUMNS;
	}

	static BlockPos anchor(long cellIndex) {
		if (cellIndex < 0 || cellIndex >= (long) REGION_COLUMNS * REGION_COLUMNS)
			throw new IllegalArgumentException("Cabin has no arboretum region: " + cellIndex);
		int column = (int) (cellIndex % REGION_COLUMNS), row = (int) (cellIndex / REGION_COLUMNS);
		return new BlockPos(REGION_ORIGIN + column * REGION_SPACING, FLOOR_Y, REGION_ORIGIN + row * REGION_SPACING);
	}

	/** The cell index whose region holds this position, if any. */
	static OptionalLong regionAt(BlockPos pos) {
		long column = Math.floorDiv((long) pos.getX() - REGION_ORIGIN + REGION_SPACING / 2, REGION_SPACING);
		long row = Math.floorDiv((long) pos.getZ() - REGION_ORIGIN + REGION_SPACING / 2, REGION_SPACING);
		if (column < 0 || column >= REGION_COLUMNS || row < 0 || row >= REGION_COLUMNS) return OptionalLong.empty();
		return OptionalLong.of(row * REGION_COLUMNS + column);
	}

	static Optional<CabinRecord> cabinAt(MinecraftServer server, BlockPos pos) {
		var cell = regionAt(pos);
		return cell.isEmpty() ? Optional.empty() : CabinRegistry.get(server).findByCell(cell.getAsLong())
			.filter(cabin -> cabin.upgrades().arboretum().level() > 0);
	}

	static ServerLevel level(MinecraftServer server) {
		ServerLevel level = server.getLevel(LEVEL_KEY);
		if (level == null) throw new IllegalStateException("The arboretum dimension is unavailable");
		return level;
	}

	public static boolean isArboretum(Level level) { return level.dimension().equals(LEVEL_KEY); }

	// Grove geometry

	static int half(int level) { return (SIZES[level] - 1) / 2; }

	/** Grove-relative position of a world position in the cabin's region. */
	static BlockPos relative(CabinRecord cabin, BlockPos pos) { return pos.subtract(anchor(cabin.cellIndex())); }

	static BlockPos world(CabinRecord cabin, BlockPos relative) { return anchor(cabin.cellIndex()).offset(relative); }

	/** Inside the walls, roof and foundation: the floor, its dirt, and the open space above. */
	static boolean inside(int level, BlockPos r) {
		int half = half(level);
		return Math.abs(r.getX()) <= half && r.getZ() >= 0 && r.getZ() < SIZES[level] && r.getY() >= -2 && r.getY() <= HEIGHTS[level];
	}

	static boolean inVolume(int level, BlockPos r) {
		int half = half(level);
		return Math.abs(r.getX()) <= half + 1 && r.getZ() >= -1 && r.getZ() <= SIZES[level] && r.getY() >= -3 && r.getY() <= HEIGHTS[level] + 1;
	}

	static boolean isShell(int level, BlockPos r) { return inVolume(level, r) && !inside(level, r); }

	static List<BlockPos> grid(int level) {
		var result = new ArrayList<BlockPos>();
		int half = half(level);
		for (int z : GRID_Z) for (int x : GRID_X)
			if (Math.abs(x) <= half - 1 && z <= SIZES[level] - 2) result.add(new BlockPos(x, 1, z));
		return List.copyOf(result);
	}

	/** Grid spots in world positions for the cabin's current grove. */
	static List<BlockPos> spots(CabinRecord cabin) {
		int level = cabin.upgrades().arboretum().level();
		return level == 0 ? List.of() : grid(level).stream().map(r -> world(cabin, r)).toList();
	}

	static boolean isSpot(CabinRecord cabin, BlockPos pos) { return spots(cabin).contains(pos); }

	/** The wall torches lighting the grove's edges at night, keyed by position, each facing away from its wall. */
	static Map<BlockPos, Direction> torches(int level) {
		var result = new LinkedHashMap<BlockPos, Direction>();
		int half = half(level), size = SIZES[level];
		for (int z = 3; z < size; z += 4) {
			result.put(new BlockPos(-half, 3, z), Direction.EAST);
			result.put(new BlockPos(half, 3, z), Direction.WEST);
		}
		for (int x = 4; x <= half; x += 4) {
			for (int side : new int[]{-x, x}) {
				result.put(new BlockPos(side, 3, size - 1), Direction.NORTH);
				result.put(new BlockPos(side, 3, 0), Direction.SOUTH);
			}
		}
		result.put(new BlockPos(0, 3, size - 1), Direction.NORTH);
		return result;
	}

	/** The blocks a new grove of this size is built from. */
	static Map<BlockPos, BlockState> defaults(CabinRecord cabin, int level) {
		var result = new LinkedHashMap<BlockPos, BlockState>();
		int half = half(level), size = SIZES[level], height = HEIGHTS[level];
		var palette = cabin.palette();
		for (int x = -half - 1; x <= half + 1; x++) for (int z = -1; z <= size; z++) for (int y = -3; y <= height + 1; y++) {
			var r = new BlockPos(x, y, z);
			boolean side = Math.abs(x) == half + 1, end = z == -1 || z == size;
			BlockState state;
			if (side && end) state = palette.walls().structuralWoodBlock().defaultBlockState();
			else if (side || end) state = palette.walls().planksBlock().defaultBlockState();
			else if (y == -3) state = palette.floor().planksBlock().defaultBlockState();
			else if (y == height + 1) state = Blocks.GLASS.defaultBlockState();
			else if (y == 0) state = Blocks.GRASS_BLOCK.defaultBlockState();
			else if (y < 0) state = Blocks.DIRT.defaultBlockState();
			else state = Blocks.AIR.defaultBlockState();
			if (state.hasProperty(RotatedPillarBlock.AXIS)) state = state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
			result.put(r, state);
		}
		torches(level).forEach((r, facing) -> result.put(r, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing)));
		var door = ArboretumBlocks.GROVE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH)
			.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
		result.put(EXIT, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		result.put(EXIT.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		result.put(BOARD, ArboretumBlocks.PLANTING_BOARD.defaultBlockState());
		return result;
	}

	// The cabin's arboretum door

	/** The lower half of the arboretum door, in the west corridor's south wall. */
	static BlockPos door(CabinRecord cabin) {
		return PocketDimension.cellCenter(cabin.cellIndex()).offset(-cabin.progression().generalSize() / 2 - 29, 1, 2);
	}

	static Map<BlockPos, BlockState> doorBlocks(CabinRecord cabin) {
		var door = ArboretumBlocks.ARBORETUM_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
			.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
		return Map.of(door(cabin), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER),
			door(cabin).above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	// Purchases

	static CabinUpgradeService.Outcome validate(ServerLevel pocket, CabinRecord cabin, int target) {
		int current = cabin.upgrades().arboretum().level();
		if (target != current + 1) return CabinUpgradeService.Outcome.failure("The arboretum requires the previous size.");
		if (!hasRegion(cabin)) return CabinUpgradeService.Outcome.failure("This world has no arboretum space left.");
		if (CabinGreenhouseGrowth.hasGrovePending(pocket.getServer(), cabin.uuid()))
			return CabinUpgradeService.Outcome.failure("Arboretum catch-up is in progress.");
		if (current == 0) {
			var corridor = CabinCorridors.validateInstall(pocket, cabin, CabinCorridor.WEST);
			if (!corridor.success()) return corridor;
			var shell = CabinCorridorLayout.blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), CabinCorridor.WEST);
			for (var pos : doorBlocks(cabin).keySet()) {
				var state = pocket.getBlockState(pos);
				if (!state.isAir() && !state.equals(shell.get(pos)))
					return CabinUpgradeService.Outcome.failure("The arboretum door is obstructed at " + pos.toShortString());
			}
		}
		var grove = level(pocket.getServer());
		for (var entry : defaults(cabin, target).entrySet()) {
			var r = entry.getKey();
			if (current > 0 && inVolume(current, r)) continue;
			var pos = world(cabin, r);
			grove.getChunk(pos);
			if (!grove.getBlockState(pos).isAir())
				return CabinUpgradeService.Outcome.failure("Arboretum growth is obstructed at " + pos.toShortString());
		}
		return CabinUpgradeService.Outcome.success("");
	}

	static void install(ServerLevel pocket, CabinRecord cabin, CabinUpgradeState.Installation installation) {
		var grove = level(pocket.getServer());
		int from = installation.expectedState(), to = installation.targetState();
		CabinCorridors.applyWorldEffect(() -> {
			if (from == 0) {
				if (!cabin.progression().corridors().contains(CabinCorridor.WEST))
					CabinCorridorLayout.blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), CabinCorridor.WEST)
						.forEach((pos, state) -> pocket.setBlock(pos, state, CabinCorridorSnapshot.FLAGS));
				doorBlocks(cabin).forEach((pos, state) -> pocket.setBlock(pos, state, CabinCorridorSnapshot.FLAGS));
			}
			// Old torches hang on walls that are about to open up; take them down first.
			if (from > 0) torches(from).keySet().forEach(r -> {
				var pos = world(cabin, r);
				if (grove.getBlockState(pos).is(Blocks.WALL_TORCH)) grove.setBlock(pos, Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
			});
			for (var entry : defaults(cabin, to).entrySet()) {
				var r = entry.getKey();
				// Everything inside the old grove stays as the household left it.
				if (from > 0 && inside(from, r) && !torches(to).containsKey(r)) continue;
				if (from > 0 && inside(from, r) && !grove.getBlockState(world(cabin, r)).isAir()) continue;
				grove.setBlock(world(cabin, r), entry.getValue(), CabinCorridorSnapshot.FLAGS);
			}
		});
		pocket.getServer().saveEverything(false, true, false);
	}

	// Walking through

	static boolean inGrove(ServerPlayer player, CabinRecord cabin) {
		return isArboretum(player.level()) && regionAt(player.blockPosition()).orElse(-1L) == cabin.cellIndex();
	}

	static InteractionResult enter(ServerPlayer player, BlockPos door) {
		var server = player.level().getServer();
		CabinRecord cabin = CabinRegistry.get(server).findByCell(PocketDimension.cellIndexAt(door).orElse(-1L)).orElse(null);
		if (cabin == null || cabin.upgrades().arboretum().level() == 0 || !doorBlocks(cabin).containsKey(door)) return InteractionResult.FAIL;
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYED || cabin.upgrades().operationInProgress()
			|| CabinCorridors.hasPending(server, cabin.uuid()) || CabinGreenhouseGrowth.hasGrovePending(server, cabin.uuid())) {
			player.sendSystemMessage(Component.literal("The arboretum is closed while the cabin changes."));
			return InteractionResult.FAIL;
		}
		var grove = level(server);
		BlockPos arrival = world(cabin, ARRIVAL);
		boolean moved = player.teleportTo(grove, arrival.getX() + .5, arrival.getY(), arrival.getZ() + .5, Set.of(),
			Direction.SOUTH.toYRot(), 0, false);
		return moved ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
	}

	static InteractionResult leave(ServerPlayer player, BlockPos exit) {
		var server = player.level().getServer();
		CabinRecord cabin = cabinAt(server, exit).orElse(null);
		if (cabin == null || !relative(cabin, exit).equals(EXIT) && !relative(cabin, exit).equals(EXIT.above())) return InteractionResult.FAIL;
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYED || pocket == null || CabinCorridors.hasPending(server, cabin.uuid())) {
			// The cabin is packed or moving: leave outside instead, as packing would.
			var outside = SafeDestinationResolver.resolveForCabin(player, cabin);
			if (outside.isPresent() && outside.get().teleport(player, 0)) {
				CabinOccupancyData.get(server).clear(player.getUUID());
				return InteractionResult.SUCCESS_SERVER;
			}
			player.sendSystemMessage(Component.literal("No safe way out of the arboretum is available right now."));
			return InteractionResult.FAIL;
		}
		BlockPos front = door(cabin).north();
		var destination = SafeDestinationResolver.search(pocket, front, player, 2)
			.or(() -> SafeDestinationResolver.search(pocket, PocketDimension.interiorEntrance(cabin.cellIndex(), cabin.progression().generalSize()), player));
		if (destination.isEmpty() || !destination.get().teleport(player, Direction.NORTH.toYRot())) {
			player.sendSystemMessage(Component.literal("The way back into the cabin is blocked."));
			return InteractionResult.FAIL;
		}
		CabinOccupancyData.get(server).enter(player.getUUID(), cabin);
		return InteractionResult.SUCCESS_SERVER;
	}

	// Simulation

	/** The chunks holding the grove, which stay simulated while the cabin is deployed. */
	static Set<ChunkPos> chunks(CabinRecord cabin) {
		int level = cabin.upgrades().arboretum().level();
		if (level == 0) return Set.of();
		var result = new LinkedHashSet<ChunkPos>();
		int half = half(level);
		BlockPos min = world(cabin, new BlockPos(-half - 1, 0, -1)), max = world(cabin, new BlockPos(half + 1, 0, SIZES[level]));
		for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++)
			for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) result.add(new ChunkPos(cx, cz));
		return Set.copyOf(result);
	}

	/** Every block of the cabin's current grove, inside and shell. */
	static Set<BlockPos> volume(CabinRecord cabin) {
		int level = cabin.upgrades().arboretum().level();
		if (level == 0) return Set.of();
		var result = new LinkedHashSet<BlockPos>();
		int half = half(level);
		for (var r : BlockPos.betweenClosed(new BlockPos(-half - 1, -3, -1), new BlockPos(half + 1, HEIGHTS[level] + 1, SIZES[level])))
			result.add(world(cabin, r));
		return Set.copyOf(result);
	}

	/** The open space and soil inside the grove, where trees grow. */
	static Set<BlockPos> growingSpace(CabinRecord cabin) {
		int level = cabin.upgrades().arboretum().level();
		var result = new LinkedHashSet<BlockPos>();
		for (var pos : volume(cabin)) if (inside(level, relative(cabin, pos))) result.add(pos);
		return Set.copyOf(result);
	}

	/** Whether this arboretum position is inside a grove: its soil and the open space above, not its shell. */
	public static boolean insideGrove(MinecraftServer server, BlockPos pos) {
		var cabin = cabinAt(server, pos).orElse(null);
		return cabin != null && inside(cabin.upgrades().arboretum().level(), relative(cabin, pos));
	}

	// Protection

	/** Whether this arboretum position belongs to a grove's walls, roof, foundation, doors or planting board. */
	static boolean isProtected(MinecraftServer server, BlockPos pos) {
		var cabin = cabinAt(server, pos).orElse(null);
		if (cabin == null) return false;
		var r = relative(cabin, pos);
		int level = cabin.upgrades().arboretum().level();
		return isShell(level, r) || r.equals(EXIT) || r.equals(EXIT.above()) || r.equals(BOARD);
	}

	/** Whether this player may change blocks or use items at this arboretum position. */
	static boolean mayChange(ServerLevel level, UUID actor, BlockPos pos) {
		var cabin = cabinAt(level.getServer(), pos).orElse(null);
		if (cabin == null) return true;
		return cabin.isHousehold(actor) && !CabinGreenhouseGrowth.hasGrovePending(level.getServer(), cabin.uuid());
	}
}
