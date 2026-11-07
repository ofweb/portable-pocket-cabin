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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The hanging hook, where carcasses are bled and dressed. It hangs from the underside of a block, or from a wall, and
 * needs that block to stay up, like a lantern.
 */
final class HangingHookBlock extends CookingStationBlock {
	static final MapCodec<HangingHookBlock> CODEC = simpleCodec(HangingHookBlock::new);
	private static final VoxelShape HANGING = Block.box(6, 8, 6, 10, 16, 10);
	private static final VoxelShape WALL = Block.box(6, 8, 6, 10, 14, 10);

	HangingHookBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HANGING, true)
			.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HANGING, HorizontalDirectionalBlock.FACING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(BlockStateProperties.HANGING) ? HANGING : WALL;
	}

	/** Placed against the underside of a block it hangs; placed against a wall it hangs from that wall. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (face == Direction.UP) return null;
		BlockState state = face == Direction.DOWN
			? defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite())
			: defaultBlockState().setValue(BlockStateProperties.HANGING, false).setValue(HorizontalDirectionalBlock.FACING, face);
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(BlockStateProperties.HANGING)) return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
		Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
		BlockPos wall = pos.relative(facing.getOpposite());
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
		BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof HangingHookBlockEntity hook) || !CookingStationBlockEntity.mayUse(hook, player)) {
			return InteractionResult.FAIL;
		}
		ItemStack held = player.getAbilities().instabuild ? stack.copy() : stack;
		return hook.useItem(server, player, held, hand) ? InteractionResult.SUCCESS_SERVER : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof HangingHookBlockEntity hook) || !CookingStationBlockEntity.mayUse(hook, player)) {
			return InteractionResult.FAIL;
		}
		return hook.useHand(server, player) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HangingHookBlockEntity(pos, state);
	}
}
