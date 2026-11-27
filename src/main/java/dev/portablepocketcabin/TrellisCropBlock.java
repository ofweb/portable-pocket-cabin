package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * A climbing crop on a trellis: cucumber, grapes, hops, tomatoes, and more. It is planted only by using its seeds on
 * a trellis. Picking a ripe vine gives its fruit and sets it back two stages, so it fruits again without replanting.
 * A vine grown from cuttings gives a cutting to shears once grown, and is set back two stages too.
 *
 * <p>A tall vine, such as tomato or grape, climbs a second trellis stacked on its own once it reaches its tall stage,
 * and is then {@link #TALL} and two blocks high; on a single trellis it stays one block high and fruits all the same.
 */
final class TrellisCropBlock extends FieldCropBlock {
	static final int PICKED_BACK = 2;
	/** Whether the vine has climbed the trellis above, which then holds its top. */
	static final BooleanProperty TALL = BooleanProperty.create("tall");
	private final Supplier<Item> produce;
	/** Whether shears take a cutting from the grown vine, which is how it is planted. */
	private final boolean cuttings;
	private final MapCodec<TrellisCropBlock> codec;

	TrellisCropBlock(Properties properties, Supplier<? extends ItemLike> seeds, Supplier<Item> produce, boolean cuttings) {
		super(properties, seeds);
		this.produce = produce;
		this.cuttings = cuttings;
		this.codec = simpleCodec(p -> new TrellisCropBlock(p, seeds, produce, cuttings));
		registerDefaultState(defaultBlockState().setValue(TALL, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(TALL);
	}

	/** A vine never waits for room: with no trellis above it grows on at one block high. */
	@Override
	boolean canGrowTo(LevelReader level, BlockPos pos, int age) {
		return true;
	}

	@Override
	boolean fillsAbove(BlockState state) {
		return state.getValue(TALL);
	}

	/** Grows or cuts the vine to {@code age}, climbing a trellis stacked above at its tall stages and leaving it below them. */
	@Override
	boolean grow(Level level, BlockPos pos, BlockState state, int age) {
		BlockPos above = pos.above();
		BlockState over = level.getBlockState(above);
		boolean hasTop = over.is(CookingPlants.PLANT_TOP);
		boolean tall = tallFrom() >= 0 && age >= tallFrom() && (hasTop || over.is(CookingPlants.TRELLIS));
		level.setBlock(pos, state.setValue(AGE, age).setValue(TALL, tall), Block.UPDATE_CLIENTS);
		if (tall && !hasTop) level.setBlock(above, CookingPlants.PLANT_TOP.defaultBlockState().setValue(PlantTopBlock.TRELLIS, true), Block.UPDATE_ALL);
		else if (!tall && hasTop) level.setBlock(above, CookingPlants.TRELLIS.defaultBlockState(), Block.UPDATE_ALL);
		return true;
	}

	/**
	 * A vine whose climbed trellis is broken stays one block high. A grown vine looks again for a trellis stacked on it
	 * a tick later, since a ripe vine no longer grows and so would not find it.
	 */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
		BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.UP) {
			if (state.getValue(TALL) && !neighborState.is(CookingPlants.PLANT_TOP)) state = state.setValue(TALL, false);
			else if (!state.getValue(TALL) && neighborState.is(CookingPlants.TRELLIS) && tallFrom() >= 0 && getAge(state) >= tallFrom()) {
				ticks.scheduleTick(pos, this, 1);
			}
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(TALL) && level.getBlockState(pos.above()).is(CookingPlants.TRELLIS)) grow(level, pos, state, getAge(state));
	}

	/** Shears on a grown vine grown from cuttings give one cutting, set it back two stages, and wear the shears. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		if (!cuttings || !stack.is(net.minecraft.world.item.Items.SHEARS)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
		return Cuttings.take(state, level, pos, player, stack, hand, AGE, getMaxAge() - 1);
	}

	@Override
	public MapCodec<? extends CropBlock> codec() {
		return codec;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return TrellisBlock.SHAPE;
	}

	/** A vine is never placed directly: its seeds are used on a trellis instead. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!isMaxAge(state)) return InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			int count = 1 + server.getRandom().nextInt(2);
			popResource(level, pos, new ItemStack(produce.get(), count));
			level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
			grow(level, pos, state, getMaxAge() - PICKED_BACK);
			RichSoil.spend(level, pos.below());
		}
		return InteractionResult.SUCCESS;
	}
}
