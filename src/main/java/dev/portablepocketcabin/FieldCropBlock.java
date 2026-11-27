package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * A field crop such as barley, onion, or cilantro: planted from its seeds, it grows through eight stages at the rate its
 * growing needs give, and only on its soil. Wild crops generated on grass survive there but do not grow.
 *
 * <p>Tall crops such as corn grow into the block above from a stage on, like a pitcher plant: the block above holds
 * their {@link PlantTopBlock top}, and they wait at the stage before until it is free. Their model draws both blocks.
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
		if (random.nextFloat() < needs().growthChance(level, pos)) grow(level, pos, state, getAge(state) + 1);
	}

	/** Bone meal grows the plant as far as it can: a tall crop with no room above stops at the stage before. */
	@Override
	public void growCrops(Level level, BlockPos pos, BlockState state) {
		int age = getAge(state);
		for (int target = Math.min(getMaxAge(), age + getBonemealAgeIncrease(level)); target > age; target--) {
			if (grow(level, pos, state, target)) return;
		}
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return super.isValidBonemealTarget(level, pos, state) && canGrowTo(level, pos, getAge(state) + 1);
	}

	/** The stage from which this plant also fills the block above, or -1 when it stays within one block. */
	final int tallFrom() {
		return CookingPlants.tallFrom(this);
	}

	/**
	 * Whether the plant can grow to {@code age} where it stands: a tall stage needs empty air above, or its own top. It
	 * never grows into another block, not even grass or snow, so a greenhouse's glass roof stops it below.
	 */
	boolean canGrowTo(LevelReader level, BlockPos pos, int age) {
		if (tallFrom() < 0 || age < tallFrom()) return true;
		BlockState above = level.getBlockState(pos.above());
		return above.is(CookingPlants.PLANT_TOP) || above.isAir();
	}

	/** Whether the plant, as it is, also fills the block above, which then holds its top. */
	boolean fillsAbove(BlockState state) {
		return tallFrom() >= 0 && getAge(state) >= tallFrom();
	}

	/**
	 * Grows or cuts the plant to {@code age}, taking the block above for its top when it grows tall and giving it back
	 * when it is cut below that. Returns whether it could.
	 */
	boolean grow(Level level, BlockPos pos, BlockState state, int age) {
		if (!canGrowTo(level, pos, age)) return false;
		BlockState grown = state.setValue(AGE, age);
		level.setBlock(pos, grown, Block.UPDATE_CLIENTS);
		BlockPos above = pos.above();
		boolean hasTop = level.getBlockState(above).is(CookingPlants.PLANT_TOP);
		if (fillsAbove(grown) && !hasTop) level.setBlock(above, CookingPlants.PLANT_TOP.defaultBlockState(), Block.UPDATE_ALL);
		else if (!fillsAbove(grown) && hasTop) level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		return true;
	}

	/** A tall crop whose top is gone, broken, burnt, or pushed away, goes with it, as a two-block flower does. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
		BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.UP && fillsAbove(state) && !neighborState.is(CookingPlants.PLANT_TOP)) return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}
}
