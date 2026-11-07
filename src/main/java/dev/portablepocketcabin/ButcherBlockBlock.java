package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The butcher block, where dressed carcasses and fish are broken down into parts. */
final class ButcherBlockBlock extends CookingStationBlock {
	static final MapCodec<ButcherBlockBlock> CODEC = simpleCodec(ButcherBlockBlock::new);

	ButcherBlockBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof ButcherBlockBlockEntity block) || !CookingStationBlockEntity.mayUse(block, player)) {
			return InteractionResult.FAIL;
		}
		if (block.isTool(stack)) {
			return block.cut(server, player, stack, hand, player.isShiftKeyDown()) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
		}
		ItemStack held = player.getAbilities().instabuild ? stack.copy() : stack;
		return block.place(server, player, held) ? InteractionResult.SUCCESS_SERVER : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof ButcherBlockBlockEntity block) || !CookingStationBlockEntity.mayUse(block, player)) {
			return InteractionResult.FAIL;
		}
		ItemStack taken = block.take();
		if (taken.isEmpty()) return InteractionResult.PASS;
		if (!player.getInventory().add(taken)) player.drop(taken, false);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ButcherBlockBlockEntity(pos, state);
	}
}
