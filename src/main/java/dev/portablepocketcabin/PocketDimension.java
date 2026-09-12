package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.DimensionType;

public final class PocketDimension {
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
}
