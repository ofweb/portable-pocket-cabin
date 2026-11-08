package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * A field crop such as barley, onion, or cilantro: planted from its seeds, it grows through eight stages at the rate its
 * growing needs give, and only on its soil. Wild crops generated on grass survive there but do not grow.
 */
class FieldCropBlock extends CropBlock implements CookingPlant {
	private final Supplier<? extends ItemLike> seeds;
	private final MapCodec<FieldCropBlock> codec;

	FieldCropBlock(Properties properties, Supplier<? extends ItemLike> seeds) {
		super(properties);
		this.seeds = seeds;
		this.codec = simpleCodec(p -> new FieldCropBlock(p, seeds));
	}

	@Override
	public MapCodec<? extends CropBlock> codec() {
		return codec;
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return seeds.get();
	}

	/** Planted on its soil, or found wild on ground that supports vegetation, such as grass and dirt. */
	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return needs().soilMatches(state) || state.is(CookingPlants.WILD_GROUND);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (isMaxAge(state) || !needs().canGrow(level, pos)) return;
		if (random.nextFloat() < needs().growthChance(level, pos)) level.setBlock(pos, getStateForAge(getAge(state) + 1), Block.UPDATE_CLIENTS);
	}
}
