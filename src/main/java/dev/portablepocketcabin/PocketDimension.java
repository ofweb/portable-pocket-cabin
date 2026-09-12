package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.DimensionType;

public final class PocketDimension {
	public static final int CELL_SPACING = 512;
	public static final int CELLS_PER_ROW = 1024;
	public static final long MAX_CELL_INDEX = 58_000L * CELLS_PER_ROW - 1;
	public static final int CELL_ORIGIN_X = 1024;
	public static final int CELL_ORIGIN_Z = 1024;
	public static final int CELL_FLOOR_Y = 63;
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
}
