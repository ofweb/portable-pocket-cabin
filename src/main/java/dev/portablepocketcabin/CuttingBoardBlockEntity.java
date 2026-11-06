package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/** A cutting board holds one stack. The knife cuts one item from it, or the whole stack when sneaking. */
final class CuttingBoardBlockEntity extends DisplayStationBlockEntity {
	CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
		super(CookingBlocks.CUTTING_BOARD_ENTITY, pos, state, 1);
	}

	private static Optional<CookingRecipe> recipe(ServerLevel level, ItemStack stack) {
		if (stack.isEmpty()) return Optional.empty();
		return CookingStation.CUTTING.find(level, CookingInput.of(List.of(stack.copyWithCount(1)))).map(holder -> holder.value());
	}

	/** Puts the held stack on the board, or adds it to the same item already there. Returns whether anything moved. */
	boolean place(ServerLevel level, ItemStack held) {
		ItemStack board = items.get(0);
		if (recipe(level, held).isEmpty()) return false;
		if (board.isEmpty()) {
			items.set(0, held.split(held.getCount()));
		} else if (ItemStack.isSameItemSameComponents(board, held) && board.getCount() < board.getMaxStackSize()) {
			int moved = Math.min(held.getCount(), board.getMaxStackSize() - board.getCount());
			board.grow(moved);
			held.shrink(moved);
		} else {
			return false;
		}
		changedForClients();
		return true;
	}

	/** Cuts one item, or the whole stack, giving the results to the player and dropping what does not fit. */
	boolean cut(ServerLevel level, Player player, boolean wholeStack) {
		ItemStack board = items.get(0);
		CookingRecipe recipe = recipe(level, board).orElse(null);
		if (recipe == null) return false;
		int count = wholeStack ? board.getCount() : 1;
		for (int i = 0; i < count; i++) {
			give(level, player, recipe.result());
			for (ItemStackTemplate remainder : recipe.remainders()) give(level, player, remainder);
		}
		board.shrink(count);
		level.playSound(null, worldPosition, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.8F, 1.4F);
		changedForClients();
		return true;
	}

	private void give(ServerLevel level, Player player, ItemStackTemplate output) {
		ItemStack made = output.create();
		if (!player.getInventory().add(made) && !made.isEmpty()) Block.popResource(level, worldPosition, made);
	}

	/** Takes the stack back off the board. */
	ItemStack take() {
		ItemStack taken = items.get(0);
		if (taken.isEmpty()) return ItemStack.EMPTY;
		items.set(0, ItemStack.EMPTY);
		changedForClients();
		return taken;
	}
}
