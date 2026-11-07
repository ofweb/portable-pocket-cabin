package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * A free-standing container station with a screen, such as the mill, butter churn, cheese press, or a time station.
 * Worked stations advance with their tool, or with an empty hand for the churn; sneaking, or having nothing to work,
 * opens the screen instead.
 */
final class ProcessStationBlock extends CookingStationBlock {
	/** What a station block needs: its screen, block entity, shape, and the tool or hand that works it. */
	record Config(CookingMenu.Kind kind, Supplier<BlockEntityType<ProcessStationBlockEntity>> type, VoxelShape shape,
		Supplier<Item> tool, boolean handWork, SoundEvent workSound) {
		static Config of(CookingMenu.Kind kind, Supplier<BlockEntityType<ProcessStationBlockEntity>> type) {
			return new Config(kind, type, Shapes.block(), null, false, null);
		}

		Config shape(VoxelShape shape) {
			return new Config(kind, type, shape, tool, handWork, workSound);
		}

		Config tool(Supplier<Item> tool, SoundEvent sound) {
			return new Config(kind, type, shape, tool, false, sound);
		}

		Config handWork(SoundEvent sound) {
			return new Config(kind, type, shape, null, true, sound);
		}
	}

	private final Config config;
	private final MapCodec<ProcessStationBlock> codec;

	ProcessStationBlock(Properties properties, Config config) {
		super(properties);
		this.config = config;
		this.codec = simpleCodec(p -> new ProcessStationBlock(p, config));
		registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HorizontalDirectionalBlock.FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return config.shape();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hit) {
		return useTool(config, stack, level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return useHand(config, level, pos, player);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ProcessStationBlockEntity(config.type().get(), pos, state, config.kind());
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return ticker(config, level, type);
	}

	/** Using the station's tool on it works the current recipe once. Other items fall through to the empty-hand use. */
	static InteractionResult useTool(Config config, ItemStack stack, Level level, BlockPos pos, Player player) {
		if (config.tool() == null || !stack.is(config.tool().get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
		work(config, level, pos, player);
		return InteractionResult.SUCCESS;
	}

	/** A hand-worked station works once unless the player sneaks or there is nothing to work; otherwise it opens. */
	static InteractionResult useHand(Config config, Level level, BlockPos pos, Player player) {
		if (config.handWork() && !player.isShiftKeyDown() && work(config, level, pos, player)) return InteractionResult.SUCCESS;
		return openMenu(level, pos, player, config.kind());
	}

	private static boolean work(Config config, Level level, BlockPos pos, Player player) {
		if (!(level instanceof ServerLevel server)) return true;
		if (!(level.getBlockEntity(pos) instanceof ProcessStationBlockEntity station) || !CookingStationBlockEntity.mayUse(station, player)) {
			return false;
		}
		if (!station.work(server)) return false;
		if (config.workSound() != null) level.playSound(null, pos, config.workSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
		return true;
	}

	@SuppressWarnings("unchecked")
	static <T extends BlockEntity> BlockEntityTicker<T> ticker(Config config, Level level, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || config.kind().station.worked() || type != config.type().get()) return null;
		return (world, pos, state, entity) -> ProcessStationBlockEntity.serverTick((ServerLevel) world, pos, state,
			(ProcessStationBlockEntity) entity);
	}

	/** A station that stands on a prep counter, such as the mixing bowl and the mortar and pestle. */
	static final class CounterTopStation extends CounterTop {
		private final Config config;
		private final MapCodec<CounterTopStation> codec;

		CounterTopStation(Properties properties, Config config) {
			super(properties);
			this.config = config;
			this.codec = simpleCodec(p -> new CounterTopStation(p, config));
		}

		@Override
		protected MapCodec<? extends BaseEntityBlock> codec() {
			return codec;
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return config.shape();
		}

		@Override
		protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
			return useTool(config, stack, level, pos, player);
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			return useHand(config, level, pos, player);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new ProcessStationBlockEntity(config.type().get(), pos, state, config.kind());
		}

		@Override
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
			return ticker(config, level, type);
		}
	}
}
