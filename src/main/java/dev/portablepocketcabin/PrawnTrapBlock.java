package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A prawn trap. Placed in any water, it catches prawns, up to eight: about one every Minecraft hour where prawns live
 * wild, and half as fast elsewhere. It catches up from saved game time while its chunk is unloaded. An empty hand takes the prawns, and hoppers below
 * take them too.
 */
final class PrawnTrapBlock extends CookingStationBlock implements SimpleWaterloggedBlock {
	static final MapCodec<PrawnTrapBlock> CODEC = simpleCodec(PrawnTrapBlock::new);
	/** The biomes where a trap catches prawns: warm and lukewarm oceans and mangrove swamps. */
	static final TagKey<Biome> PRAWN_WATERS = TagKey.create(Registries.BIOME, PortablePocketCabin.id("prawn_waters"));
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

	PrawnTrapBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean water = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
		return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, water);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
		BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(BlockStateProperties.WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return state;
	}

	/** Whether a trap here can catch: under water. Prawn waters only make it faster. */
	static boolean catches(Level level, BlockPos pos, BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof PrawnTrapBlockEntity trap) || !CookingStationBlockEntity.mayUse(trap, player)) {
			return InteractionResult.FAIL;
		}
		ItemStack prawns = trap.removeItemNoUpdate(0);
		if (prawns.isEmpty()) return InteractionResult.PASS;
		trap.setChanged();
		if (!player.getInventory().add(prawns)) player.drop(prawns, false);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PrawnTrapBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != Butchery.PRAWN_TRAP_ENTITY) return null;
		return (world, pos, blockState, entity) -> ((PrawnTrapBlockEntity) entity).tick((ServerLevel) world, blockState);
	}
}
