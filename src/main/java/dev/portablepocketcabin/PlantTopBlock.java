package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The upper block of a tall plant: corn, dill, and courgette once grown tall, and a tall vine that has climbed a second
 * trellis. The plant below draws both blocks; this block gives the upper one its outline and keeps anything else out
 * of it. Using it, bone meal on it, and breaking it all act on the plant below. As a climbed trellis it shows the
 * trellis, and breaking it gives the trellis back and leaves the vine one block high.
 */
final class PlantTopBlock extends Block implements BonemealableBlock {
	static final MapCodec<PlantTopBlock> CODEC = simpleCodec(PlantTopBlock::new);
	/** Whether this top is a trellis stacked on a vine's own, which the vine has climbed. */
	static final BooleanProperty TRELLIS = BooleanProperty.create("trellis");
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

	PlantTopBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TRELLIS, false));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TRELLIS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(TRELLIS) ? TrellisBlock.SHAPE : SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(TRELLIS) ? TrellisBlock.SHAPE : Shapes.empty();
	}

	/** Stands only on a tall plant that fills this block. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.getBlock() instanceof FieldCropBlock plant && plant.fillsAbove(below);
	}

	/** Goes when the plant below is gone or cut back; a climbed trellis stays as a trellis while it still stands on one. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
		BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction != Direction.DOWN || state.canSurvive(level, pos)) return state;
		BlockState trellis = CookingPlants.TRELLIS.defaultBlockState();
		return state.getValue(TRELLIS) && trellis.canSurvive(level, pos) ? trellis : Blocks.AIR.defaultBlockState();
	}

	/** Breaking the top of a field crop breaks the whole plant, giving its harvest, or nothing in creative. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockPos below = pos.below();
		if (!level.isClientSide() && !state.getValue(TRELLIS) && level.getBlockState(below).getBlock() instanceof FieldCropBlock) {
			if (player.preventsBlockDrops()) level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			else level.destroyBlock(below, true, player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos below = pos.below();
		return level.getBlockState(below).useWithoutItem(level, player, hit.withPosition(below));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		BlockPos below = pos.below();
		return level.getBlockState(below).useItemOn(stack, level, player, hand, hit.withPosition(below));
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		BlockState below = level.getBlockState(pos.below());
		return below.getBlock() instanceof BonemealableBlock plant && plant.isValidBonemealTarget(level, pos.below(), below);
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		BlockState below = level.getBlockState(pos.below());
		if (below.getBlock() instanceof BonemealableBlock plant) plant.performBonemeal(level, random, pos.below(), below);
	}
}
