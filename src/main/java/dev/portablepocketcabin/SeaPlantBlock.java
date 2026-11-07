package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A seaweed foraged underwater, such as wakame, laver, or sea lettuce. It stands in water on a solid floor like
 * seagrass, and breaking it gives the seaweed, which plants it again in water. It doesn't grow tall with bone meal.
 */
final class SeaPlantBlock extends SeagrassBlock {
	static final MapCodec<SeaPlantBlock> CODEC = simpleCodec(SeaPlantBlock::new);

	SeaPlantBlock(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public MapCodec<SeagrassBlock> codec() {
		return (MapCodec) CODEC;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return false;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return false;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
	}
}
