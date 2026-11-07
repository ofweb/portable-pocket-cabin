package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Supplier;

/**
 * A paddy crop such as rice or water chestnut. It stands in a water source one block deep, over its soil, and always
 * holds that water, like seagrass, so breaking it leaves the water behind and a bucket can't drain it.
 */
final class PaddyCropBlock extends FieldCropBlock implements LiquidBlockContainer {
	private final MapCodec<PaddyCropBlock> paddyCodec;

	PaddyCropBlock(Properties properties, Supplier<? extends ItemLike> seeds) {
		super(properties, seeds);
		this.paddyCodec = simpleCodec(p -> new PaddyCropBlock(p, seeds));
	}

	@Override
	public MapCodec<? extends CropBlock> codec() {
		return paddyCodec;
	}

	/** Planted only into a water source with open air above it, so the water is one block deep. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
		if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) return null;
		return super.getStateForPlacement(context);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return super.canSurvive(state, level, pos) && !level.getFluidState(pos.above()).is(FluidTags.WATER);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
		BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		if (!updated.isAir()) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return updated;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return Fluids.WATER.getSource(false);
	}

	@Override
	public boolean canPlaceLiquid(LivingEntity entity, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
		return false;
	}

	@Override
	public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
		return false;
	}
}
