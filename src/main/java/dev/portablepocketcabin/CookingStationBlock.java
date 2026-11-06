package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Blocks for the cooking stations. Each kind keeps its interaction here and its state in its block entity. */
abstract class CookingStationBlock extends BaseEntityBlock {
	CookingStationBlock(Properties properties) {
		super(properties);
	}

	static InteractionResult openMenu(Level level, BlockPos pos, Player player, CookingMenu.Kind kind) {
		if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof CookingStationBlockEntity station) || !CookingStationBlockEntity.mayUse(station, player)) {
			return InteractionResult.FAIL;
		}
		serverPlayer.openMenu(new ExtendedMenuProvider<BlockPos>() {
			@Override
			public BlockPos getScreenOpeningData(ServerPlayer opening) {
				return pos;
			}

			@Override
			public Component getDisplayName() {
				return station.getBlockState().getBlock().getName();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
				return CookingMenu.server(kind, containerId, inventory, station, pos);
			}
		});
		return InteractionResult.SUCCESS_SERVER;
	}

	static final class Stove extends CookingStationBlock {
		static final MapCodec<Stove> CODEC = simpleCodec(Stove::new);

		Stove(Properties properties) {
			super(properties);
			registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
				.setValue(BlockStateProperties.LIT, false));
		}

		@Override
		protected MapCodec<? extends BaseEntityBlock> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(HorizontalDirectionalBlock.FACING, BlockStateProperties.LIT);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context) {
			return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
		}

		@Override
		protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
			if (!stack.is(ItemTags.SHOVELS) || !state.getValue(BlockStateProperties.LIT)) return InteractionResult.TRY_WITH_EMPTY_HAND;
			if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof StoveBlockEntity stove
				&& CookingStationBlockEntity.mayUse(stove, player) && stove.extinguish()) {
				level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
				if (player instanceof ServerPlayer serverPlayer) stack.hurtAndBreak(1, serverLevel, serverPlayer, item -> { });
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			return openMenu(level, pos, player, CookingMenu.Kind.STOVE);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new StoveBlockEntity(pos, state);
		}

		@Override
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
			return level instanceof ServerLevel
				? createTickerHelper(type, CookingBlocks.STOVE_ENTITY, (world, pos, blockState, stove) ->
					StoveBlockEntity.serverTick((ServerLevel) world, pos, blockState, stove))
				: null;
		}
	}

	static final class Pot extends CookingStationBlock {
		static final MapCodec<Pot> CODEC = simpleCodec(Pot::new);
		private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

		Pot(Properties properties) {
			super(properties);
		}

		@Override
		protected MapCodec<? extends BaseEntityBlock> codec() {
			return CODEC;
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return SHAPE;
		}

		@Override
		protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
			if (!stack.is(Items.WATER_BUCKET)) return InteractionResult.TRY_WITH_EMPTY_HAND;
			if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot
				&& CookingStationBlockEntity.mayUse(pot, player) && pot.fillWater()) {
				level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
				if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			return openMenu(level, pos, player, CookingMenu.Kind.POT);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new CookingPotBlockEntity(pos, state);
		}

		@Override
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
			return level instanceof ServerLevel
				? createTickerHelper(type, CookingBlocks.COOKING_POT_ENTITY, (world, pos, blockState, pot) ->
					CookingPotBlockEntity.serverTick((ServerLevel) world, pos, blockState, pot))
				: null;
		}
	}

	static final class Skillet extends CookingStationBlock {
		static final MapCodec<Skillet> CODEC = simpleCodec(Skillet::new);
		private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 2, 14);

		Skillet(Properties properties) {
			super(properties);
		}

		@Override
		protected MapCodec<? extends BaseEntityBlock> codec() {
			return CODEC;
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return SHAPE;
		}

		@Override
		protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
			if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
			if (!(level.getBlockEntity(pos) instanceof SkilletBlockEntity skillet) || !CookingStationBlockEntity.mayUse(skillet, player)) {
				return InteractionResult.FAIL;
			}
			ItemStack held = player.getAbilities().instabuild ? stack.copy() : stack;
			return skillet.add(serverLevel, held) ? InteractionResult.SUCCESS_SERVER : InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
			if (!(level.getBlockEntity(pos) instanceof SkilletBlockEntity skillet) || !CookingStationBlockEntity.mayUse(skillet, player)) {
				return InteractionResult.FAIL;
			}
			ItemStack taken = skillet.take(serverLevel);
			if (taken.isEmpty()) return InteractionResult.PASS;
			if (!player.getInventory().add(taken)) player.drop(taken, false);
			return InteractionResult.SUCCESS_SERVER;
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new SkilletBlockEntity(pos, state);
		}

		@Override
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
			return level instanceof ServerLevel
				? createTickerHelper(type, CookingBlocks.SKILLET_ENTITY, (world, pos, blockState, skillet) ->
					SkilletBlockEntity.serverTick((ServerLevel) world, pos, blockState, skillet))
				: null;
		}
	}
}
