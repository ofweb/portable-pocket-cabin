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
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.OptionalLong;

public final class PocketDimension {
	public static final int CELL_SPACING = 512;
	public static final int CELLS_PER_ROW = 1024;
	public static final long MAX_CELL_INDEX = 58_000L * CELLS_PER_ROW - 1;
	public static final int CELL_ORIGIN_X = 1024;
	public static final int CELL_ORIGIN_Z = 1024;
	public static final int CELL_FLOOR_Y = 63;
	public static final int INTERIOR_USABLE_RADIUS = 10;
	public static final int INTERIOR_SHELL_RADIUS = INTERIOR_USABLE_RADIUS + 1;
	public static final int INTERIOR_CEILING_Y = CELL_FLOOR_Y + 7;
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
		BlockPos center = cellCenter(cellIndex);

		for (int x = -INTERIOR_USABLE_RADIUS; x <= INTERIOR_USABLE_RADIUS; x++) {
			for (int z = -INTERIOR_USABLE_RADIUS; z <= INTERIOR_USABLE_RADIUS; z++) {
				for (int y = CELL_FLOOR_Y + 1; y < INTERIOR_CEILING_Y; y++) {
					level.setBlockAndUpdate(new BlockPos(center.getX() + x, y, center.getZ() + z),
						Blocks.AIR.defaultBlockState());
				}
			}
		}

		for (int x = -INTERIOR_SHELL_RADIUS; x <= INTERIOR_SHELL_RADIUS; x++) {
			for (int z = -INTERIOR_SHELL_RADIUS; z <= INTERIOR_SHELL_RADIUS; z++) {
				BlockPos floor = new BlockPos(center.getX() + x, CELL_FLOOR_Y, center.getZ() + z);
				BlockPos ceiling = new BlockPos(center.getX() + x, INTERIOR_CEILING_Y, center.getZ() + z);
				level.setBlockAndUpdate(floor, Blocks.POLISHED_ANDESITE.defaultBlockState());
				level.setBlockAndUpdate(ceiling, Blocks.STONE_BRICKS.defaultBlockState());
			}
		}

		for (int y = CELL_FLOOR_Y + 1; y < INTERIOR_CEILING_Y; y++) {
			for (int offset = -INTERIOR_SHELL_RADIUS; offset <= INTERIOR_SHELL_RADIUS; offset++) {
				level.setBlockAndUpdate(center.offset(-INTERIOR_SHELL_RADIUS, y - CELL_FLOOR_Y, offset),
					Blocks.STONE_BRICKS.defaultBlockState());
				level.setBlockAndUpdate(center.offset(INTERIOR_SHELL_RADIUS, y - CELL_FLOOR_Y, offset),
					Blocks.STONE_BRICKS.defaultBlockState());
				level.setBlockAndUpdate(center.offset(offset, y - CELL_FLOOR_Y, -INTERIOR_SHELL_RADIUS),
					Blocks.STONE_BRICKS.defaultBlockState());
				level.setBlockAndUpdate(center.offset(offset, y - CELL_FLOOR_Y, INTERIOR_SHELL_RADIUS),
					Blocks.STONE_BRICKS.defaultBlockState());
			}
		}

		for (int x : new int[] {-7, 7}) {
			for (int z : new int[] {-7, 7}) {
				level.setBlockAndUpdate(center.offset(x, INTERIOR_CEILING_Y - CELL_FLOOR_Y, z),
					Blocks.SEA_LANTERN.defaultBlockState());
			}
		}

		BlockState lowerDoor = Blocks.IRON_DOOR.defaultBlockState()
			.setValue(DoorBlock.FACING, net.minecraft.core.Direction.SOUTH)
			.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
			.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
		level.setBlockAndUpdate(interiorExitDoorLower(cellIndex), lowerDoor);
		level.setBlockAndUpdate(interiorExitDoorUpper(cellIndex),
			lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	public static BlockPos interiorEntrance(long cellIndex) {
		return cellCenter(cellIndex).offset(0, 1, INTERIOR_USABLE_RADIUS - 1);
	}

	public static BlockPos interiorExitDoorLower(long cellIndex) {
		return cellCenter(cellIndex).offset(0, 1, INTERIOR_SHELL_RADIUS);
	}

	public static BlockPos interiorExitDoorUpper(long cellIndex) {
		return interiorExitDoorLower(cellIndex).above();
	}

	public static boolean isInteriorExit(long cellIndex, BlockPos pos) {
		return pos.equals(interiorExitDoorLower(cellIndex)) || pos.equals(interiorExitDoorUpper(cellIndex));
	}

	public static boolean isInteriorShell(long cellIndex, BlockPos pos) {
		BlockPos center = cellCenter(cellIndex);
		int dx = Math.abs(pos.getX() - center.getX());
		int dz = Math.abs(pos.getZ() - center.getZ());
		if (dx > INTERIOR_SHELL_RADIUS || dz > INTERIOR_SHELL_RADIUS) {
			return false;
		}
		if (pos.getY() == CELL_FLOOR_Y || pos.getY() == INTERIOR_CEILING_Y) {
			return true;
		}
		return pos.getY() > CELL_FLOOR_Y && pos.getY() < INTERIOR_CEILING_Y
			&& (dx == INTERIOR_SHELL_RADIUS || dz == INTERIOR_SHELL_RADIUS);
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
