package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

public final class PocketDimension {
	public static final int CELL_SPACING = 512;
	public static final int CELLS_PER_ROW = 1024;
	public static final long MAX_CELL_INDEX = 58_000L * CELLS_PER_ROW - 1;
	public static final int CELL_ORIGIN_X = 1024;
	public static final int CELL_ORIGIN_Z = 1024;
	public static final int CELL_FLOOR_Y = 63;
	private static final int INITIAL_INTERIOR_CLEAR_HEIGHT = 2;
	private static final int MAXIMUM_INTERIOR_CLEAR_HEIGHT = 10;
	public static final int INTERIOR_FRONT_USABLE_Z = 2;
	public static final int INTERIOR_FRONT_WALL_Z = INTERIOR_FRONT_USABLE_Z + 1;
	public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(
		Registries.DIMENSION,
		PortablePocketCabin.id("pocket_home")
	);
	public static final ResourceKey<LevelStem> STEM_KEY = ResourceKey.create(
		Registries.LEVEL_STEM,
		PortablePocketCabin.id("pocket_home")
	);
	public static final ResourceKey<DimensionType> TYPE_KEY = ResourceKey.create(
		Registries.DIMENSION_TYPE,
		PortablePocketCabin.id("pocket_home")
	);
	public static final BlockPos TEST_PLATFORM_CENTER = new BlockPos(0, 64, 0);
	public static final int TEST_PLATFORM_RADIUS = 3;

	private PocketDimension() {
	}

	public static void ensureTestPlatform(net.minecraft.server.level.ServerLevel level) {
		for (int x = -TEST_PLATFORM_RADIUS; x <= TEST_PLATFORM_RADIUS; x++) {
			for (int z = -TEST_PLATFORM_RADIUS; z <= TEST_PLATFORM_RADIUS; z++) {
				level.setBlockAndUpdate(TEST_PLATFORM_CENTER.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
				for (int y = 0; y <= 2; y++) {
					level.setBlockAndUpdate(TEST_PLATFORM_CENTER.offset(x, y, z), Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	public static BlockPos cellCenter(long cellIndex) {
		if (cellIndex < 0 || cellIndex > MAX_CELL_INDEX) {
			throw new IllegalArgumentException("Cabin cell index is outside the supported grid: " + cellIndex);
		}
		int column = (int) (cellIndex % CELLS_PER_ROW);
		int row = (int) (cellIndex / CELLS_PER_ROW);
		return new BlockPos(
			CELL_ORIGIN_X + column * CELL_SPACING,
			CELL_FLOOR_Y,
			CELL_ORIGIN_Z + row * CELL_SPACING
		);
	}

	public static void ensureDebugMarker(net.minecraft.server.level.ServerLevel level, long cellIndex) {
		BlockPos center = cellCenter(cellIndex);
		for (int x = -TEST_PLATFORM_RADIUS; x <= TEST_PLATFORM_RADIUS; x++) {
			for (int z = -TEST_PLATFORM_RADIUS; z <= TEST_PLATFORM_RADIUS; z++) {
				level.setBlockAndUpdate(center.offset(x, 0, z), Blocks.SMOOTH_STONE.defaultBlockState());
				for (int y = 1; y <= 3; y++) {
					level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
				}
			}
		}
		level.setBlockAndUpdate(center, Blocks.GOLD_BLOCK.defaultBlockState());
		level.setBlockAndUpdate(center.above(), Blocks.LODESTONE.defaultBlockState());
		level.setBlockAndUpdate(center.offset(2, 1, 2), Blocks.SEA_LANTERN.defaultBlockState());
		level.setBlockAndUpdate(center.offset(2, 1, -2), Blocks.SEA_LANTERN.defaultBlockState());
		level.setBlockAndUpdate(center.offset(-2, 1, 2), Blocks.SEA_LANTERN.defaultBlockState());
		level.setBlockAndUpdate(center.offset(-2, 1, -2), Blocks.SEA_LANTERN.defaultBlockState());
	}

	public static void ensureCabinInterior(net.minecraft.server.level.ServerLevel level, long cellIndex) {
		ensureCabinInterior(level, cellIndex, CabinPalette.DEFAULT);
	}

	public static void ensureCabinInterior(
		net.minecraft.server.level.ServerLevel level, long cellIndex, CabinPalette palette
	) {
		ensureCabinInterior(level, cellIndex, palette, CabinProgression.INITIAL_GENERAL_SIZE);
	}

	public static void ensureCabinInterior(
		net.minecraft.server.level.ServerLevel level, long cellIndex, CabinPalette palette, int generalSize
	) {
		BlockPos center = cellCenter(cellIndex);
		InteriorBounds bounds = bounds(generalSize);
		int ceilingY = interiorCeilingY(generalSize);
		for (int x = bounds.minimumX(); x <= bounds.maximumX(); x++) {
			for (int z = bounds.minimumZ(); z <= bounds.maximumZ(); z++) {
				for (int y = CELL_FLOOR_Y + 1; y < ceilingY; y++) {
					level.setBlockAndUpdate(new BlockPos(center.getX() + x, y, center.getZ() + z),
						Blocks.AIR.defaultBlockState());
				}
			}
		}
		placeShell(level, cellIndex, palette, generalSize);
		CabinWindows.initializeInactive(level, cellIndex, generalSize);
	}

	static ExpansionCheck validateExpansion(
		net.minecraft.server.level.ServerLevel level, long cellIndex, int currentSize, int targetSize
	) {
		return validateExpansion(
			cellIndex, currentSize, targetSize, pos -> !level.getBlockState(pos).isAir()
		);
	}

	static ExpansionCheck validateExpansion(
		long cellIndex, int currentSize, int targetSize, java.util.function.Predicate<BlockPos> occupied
	) {
		if (targetSize != currentSize + 1) {
			return new ExpansionCheck(false, "General space expands exactly one block at a time");
		}
		Map<BlockPos, BlockState> currentShell = shellBlocks(cellIndex, currentSize, CabinPalette.DEFAULT);
		Map<BlockPos, BlockState> targetShell = shellBlocks(cellIndex, targetSize, CabinPalette.DEFAULT);
		int targetCeilingY = interiorCeilingY(targetSize);
		for (BlockPos pos : targetShell.keySet()) {
			if (!currentShell.containsKey(pos) && occupied.test(pos)) {
				return obstructed(pos);
			}
		}
		InteriorBounds target = bounds(targetSize);
		BlockPos center = cellCenter(cellIndex);
		for (int x = target.minimumX(); x <= target.maximumX(); x++) {
			for (int z = target.minimumZ(); z <= target.maximumZ(); z++) {
				for (int y = CELL_FLOOR_Y + 1; y < targetCeilingY; y++) {
					BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
					if (!isWithinUsable(cellIndex, currentSize, pos)
						&& !currentShell.containsKey(pos) && occupied.test(pos)) {
						return obstructed(pos);
					}
				}
			}
		}
		return new ExpansionCheck(true, "Expansion volume is clear");
	}

	private static ExpansionCheck obstructed(BlockPos pos) {
		return new ExpansionCheck(false, "Expansion is obstructed at "
			+ pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
	}

	static void expandGeneralSpace(
		net.minecraft.server.level.ServerLevel level, CabinRecord cabin, int targetSize
	) {
		int currentSize = cabin.progression().generalSize();
		ExpansionCheck check = validateExpansion(level, cabin.cellIndex(), currentSize, targetSize);
		if (!check.valid()) {
			throw new IllegalStateException(check.message());
		}
		applyGeneralSpaceExpansion(level, cabin, targetSize);
	}

	/** Applies a previously validated expansion deterministically; safe to replay during reconciliation. */
	static void applyGeneralSpaceExpansion(
		net.minecraft.server.level.ServerLevel level, CabinRecord cabin, int targetSize
	) {
		int currentSize = cabin.progression().generalSize();
		if (targetSize != currentSize + 1) {
			throw new IllegalStateException("General space expands exactly one block at a time");
		}
		Map<BlockPos, BlockState> current = shellBlocks(cabin.cellIndex(), currentSize, cabin.palette());
		Map<BlockPos, BlockState> target = shellBlocks(cabin.cellIndex(), targetSize, cabin.palette());
		for (BlockPos position : current.keySet()) {
			if (!target.containsKey(position)) {
				level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
			}
		}
		for (Map.Entry<BlockPos, BlockState> entry : target.entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}

		InteriorBounds targetBounds = bounds(targetSize);
		BlockPos center = cellCenter(cabin.cellIndex());
		int targetCeilingY = interiorCeilingY(targetSize);
		for (int x = targetBounds.minimumX(); x <= targetBounds.maximumX(); x++) {
			for (int z = targetBounds.minimumZ(); z <= targetBounds.maximumZ(); z++) {
				for (int y = CELL_FLOOR_Y + 1; y < targetCeilingY; y++) {
					BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
					if (!isWithinUsable(cabin.cellIndex(), currentSize, pos)) {
						level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
	}

	static Map<BlockPos, BlockState> shellBlocks(long cellIndex, int generalSize, CabinPalette palette) {
		InteriorBounds bounds = bounds(generalSize);
		BlockPos center = cellCenter(cellIndex);
		Map<BlockPos, BlockState> result = new LinkedHashMap<>();
		BlockState floorState = palette.floor().planksBlock().defaultBlockState();
		BlockState ceilingState = palette.roof().planksBlock().defaultBlockState();
		BlockState wallState = palette.walls().planksBlock().defaultBlockState();
		BlockState frameState = palette.walls().structuralWoodBlock().defaultBlockState();
		int ceilingOffset = interiorCeilingOffset(generalSize);

		for (int x = bounds.shellMinimumX(); x <= bounds.shellMaximumX(); x++) {
			for (int z = bounds.shellMinimumZ(); z <= bounds.shellMaximumZ(); z++) {
				result.put(center.offset(x, 0, z), floorState);
				result.put(center.offset(x, ceilingOffset, z), ceilingState);
			}
		}
		for (int y = 1; y < ceilingOffset; y++) {
			for (int x = bounds.shellMinimumX(); x <= bounds.shellMaximumX(); x++) {
				result.put(center.offset(x, y, bounds.shellMinimumZ()),
					frameOrWall(x, bounds, frameState, wallState));
				result.put(center.offset(x, y, bounds.shellMaximumZ()),
					frameOrWall(x, bounds, frameState, wallState));
			}
			for (int z = bounds.shellMinimumZ(); z <= bounds.shellMaximumZ(); z++) {
				result.put(center.offset(bounds.shellMinimumX(), y, z),
					frameOrWall(z, bounds, frameState, wallState));
				result.put(center.offset(bounds.shellMaximumX(), y, z),
					frameOrWall(z, bounds, frameState, wallState));
			}
		}

		BlockState lowerDoor = palette.door().doorBlock().defaultBlockState()
			.setValue(DoorBlock.FACING, net.minecraft.core.Direction.SOUTH)
			.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
			.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
		result.put(interiorExitDoorLower(cellIndex), lowerDoor);
		result.put(interiorExitDoorUpper(cellIndex), lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		result.put(interiorController(cellIndex), Blocks.LODESTONE.defaultBlockState());
		result.put(center.offset(bounds.minimumX(), ceilingOffset, bounds.minimumZ()),
			Blocks.SEA_LANTERN.defaultBlockState());
		result.put(center.offset(bounds.maximumX(), ceilingOffset, bounds.minimumZ()),
			Blocks.SEA_LANTERN.defaultBlockState());
		return Map.copyOf(result);
	}

	private static void placeShell(
		net.minecraft.server.level.ServerLevel level, long cellIndex, CabinPalette palette, int generalSize
	) {
		for (Map.Entry<BlockPos, BlockState> entry : shellBlocks(cellIndex, generalSize, palette).entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
	}

	private static BlockState frameOrWall(
		int offset, InteriorBounds bounds, BlockState frame, BlockState wall
	) {
		return offset == bounds.shellMinimumX() || offset == bounds.shellMaximumX()
			|| offset == bounds.shellMinimumZ() || offset == bounds.shellMaximumZ() ? frame : wall;
	}

	public static BlockPos interiorEntrance(long cellIndex) {
		return cellCenter(cellIndex).offset(0, 1, INTERIOR_FRONT_USABLE_Z);
	}

	public static BlockPos interiorExitDoorLower(long cellIndex) {
		return cellCenter(cellIndex).offset(0, 1, INTERIOR_FRONT_WALL_Z);
	}

	public static BlockPos interiorExitDoorUpper(long cellIndex) {
		return interiorExitDoorLower(cellIndex).above();
	}

	public static boolean isInteriorExit(long cellIndex, BlockPos pos) {
		return pos.equals(interiorExitDoorLower(cellIndex)) || pos.equals(interiorExitDoorUpper(cellIndex));
	}

	public static BlockPos interiorController(long cellIndex) {
		return cellCenter(cellIndex).offset(-1, 1, INTERIOR_FRONT_WALL_Z);
	}

	public static boolean isInteriorController(long cellIndex, BlockPos pos) {
		return pos.equals(interiorController(cellIndex));
	}

	public static boolean isInteriorShell(long cellIndex, BlockPos pos) {
		return isInteriorShell(cellIndex, CabinProgression.INITIAL_GENERAL_SIZE, pos);
	}

	public static boolean isInteriorShell(long cellIndex, int generalSize, BlockPos pos) {
		BlockPos center = cellCenter(cellIndex);
		InteriorBounds bounds = bounds(generalSize);
		int ceilingY = interiorCeilingY(generalSize);
		int x = pos.getX() - center.getX();
		int z = pos.getZ() - center.getZ();
		if (x < bounds.shellMinimumX() || x > bounds.shellMaximumX()
			|| z < bounds.shellMinimumZ() || z > bounds.shellMaximumZ()) {
			return false;
		}
		if (pos.getY() == CELL_FLOOR_Y || pos.getY() == ceilingY) {
			return true;
		}
		return pos.getY() > CELL_FLOOR_Y && pos.getY() < ceilingY
			&& (x == bounds.shellMinimumX() || x == bounds.shellMaximumX()
				|| z == bounds.shellMinimumZ() || z == bounds.shellMaximumZ());
	}

	static boolean isWithinUsable(long cellIndex, int generalSize, BlockPos pos) {
		BlockPos center = cellCenter(cellIndex);
		InteriorBounds bounds = bounds(generalSize);
		int ceilingY = interiorCeilingY(generalSize);
		int x = pos.getX() - center.getX();
		int z = pos.getZ() - center.getZ();
		return x >= bounds.minimumX() && x <= bounds.maximumX()
			&& z >= bounds.minimumZ() && z <= bounds.maximumZ()
			&& pos.getY() > CELL_FLOOR_Y && pos.getY() < ceilingY;
	}

	static int clearInteriorHeight(int generalSize) {
		requireSupportedGeneralSize(generalSize);
		return Math.min(
			MAXIMUM_INTERIOR_CLEAR_HEIGHT,
			INITIAL_INTERIOR_CLEAR_HEIGHT
				+ (generalSize - CabinProgression.INITIAL_GENERAL_SIZE) / 2
		);
	}

	private static int interiorCeilingY(int generalSize) {
		return CELL_FLOOR_Y + interiorCeilingOffset(generalSize);
	}

	private static int interiorCeilingOffset(int generalSize) {
		return clearInteriorHeight(generalSize) + 1;
	}

	static InteriorBounds bounds(int generalSize) {
		requireSupportedGeneralSize(generalSize);
		int minimumX = -((generalSize - 1) / 2);
		int maximumX = generalSize / 2;
		int minimumZ = INTERIOR_FRONT_USABLE_Z - generalSize + 1;
		return new InteriorBounds(minimumX, maximumX, minimumZ, INTERIOR_FRONT_USABLE_Z);
	}

	private static void requireSupportedGeneralSize(int generalSize) {
		if (generalSize < CabinProgression.INITIAL_GENERAL_SIZE
			|| generalSize > CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE) {
			throw new IllegalArgumentException("Unsupported general cabin size " + generalSize);
		}
	}

	record InteriorBounds(int minimumX, int maximumX, int minimumZ, int maximumZ) {
		int shellMinimumX() { return minimumX - 1; }
		int shellMaximumX() { return maximumX + 1; }
		int shellMinimumZ() { return minimumZ - 1; }
		int shellMaximumZ() { return maximumZ + 1; }
	}

	record ExpansionCheck(boolean valid, String message) {
	}

	public static OptionalLong cellIndexAt(BlockPos pos) {
		long relativeX = (long) pos.getX() - CELL_ORIGIN_X;
		long relativeZ = (long) pos.getZ() - CELL_ORIGIN_Z;
		long column = Math.floorDiv(relativeX + CELL_SPACING / 2L, CELL_SPACING);
		long row = Math.floorDiv(relativeZ + CELL_SPACING / 2L, CELL_SPACING);
		if (column < 0 || column >= CELLS_PER_ROW || row < 0 || row >= 58_000L) {
			return OptionalLong.empty();
		}
		long cellIndex = row * CELLS_PER_ROW + column;
		return cellIndex <= MAX_CELL_INDEX ? OptionalLong.of(cellIndex) : OptionalLong.empty();
	}
}
