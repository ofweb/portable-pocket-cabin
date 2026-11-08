package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The butcher block holds one dressed carcass or fish. Each use of the cleaver, or the knife for fish, takes the next
 * step of its data: a fish's scales and guts first, then each part in order. Sneaking takes every remaining step. The
 * carcass records how far it has gone, so it can be taken off and put back. Slot 1 only stages parts on their way to
 * kitchen storage.
 */
final class ButcherBlockBlockEntity extends DisplayStationBlockEntity {
	ButcherBlockBlockEntity(BlockPos pos, BlockState state) {
		super(Butchery.BUTCHER_BLOCK_ENTITY, pos, state, 2);
	}

	ItemStack carcass() {
		return items.get(0);
	}

	/** Puts a dressed carcass or a fish on the block. An undressed carcass is refused with a message. */
	boolean place(ServerLevel level, Player player, ItemStack held) {
		if (!carcass().isEmpty()) return false;
		if (CarcassData.forBlock(held).isEmpty()) {
			if (CarcassData.forCarcass(held).isPresent()) tell(player, "not_dressed");
			return CarcassData.forCarcass(held).isPresent();
		}
		items.set(0, held.split(1));
		level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
		changedForClients();
		return true;
	}

	/** The steps a carcass or fish gives on the block, in order. */
	static List<List<ItemStackTemplate>> steps(CarcassData data) {
		List<List<ItemStackTemplate>> steps = new ArrayList<>();
		if (data.dressing() == CarcassData.Dressing.NONE && !data.dressingGives().isEmpty()) steps.add(data.dressingGives());
		for (ItemStackTemplate part : data.parts()) steps.add(List.of(part));
		return steps;
	}

	/** Whether this tool works what is on the block: the cleaver for carcasses, the knife for fish. */
	boolean isTool(ItemStack tool) {
		CarcassData data = CarcassData.forBlock(carcass()).orElse(null);
		if (data == null) return false;
		return tool.is(data.dressing() == CarcassData.Dressing.NONE ? CookingItems.KITCHEN_KNIFE : Butchery.CLEAVER);
	}

	/** Takes the next step, or every remaining step, giving the parts to the player. Returns whether anything was taken. */
	boolean cut(ServerLevel level, Player player, ItemStack tool, InteractionHand hand, boolean all) {
		ItemStack carcass = carcass();
		CarcassData data = CarcassData.forBlock(carcass).orElse(null);
		if (data == null) return false;
		List<List<ItemStackTemplate>> steps = steps(data);
		int done = carcass.getOrDefault(Butchery.BUTCHERED, 0);
		int until = all ? steps.size() : Math.min(steps.size(), done + 1);
		for (int step = done; step < until; step++) {
			for (ItemStackTemplate part : steps.get(step)) give(level, player, part.create());
			tool.hurtAndBreak(1, player, hand);
		}
		if (until >= steps.size()) {
			items.set(0, ItemStack.EMPTY);
		} else {
			carcass.set(Butchery.BUTCHERED, until);
		}
		level.playSound(null, worldPosition, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.9F, 0.7F);
		changedForClients();
		return true;
	}

	/** Takes the carcass back off the block, keeping how far it was butchered. */
	ItemStack take() {
		ItemStack taken = carcass();
		items.set(0, ItemStack.EMPTY);
		changedForClients();
		return taken;
	}

	/** Gives a part to the player. What does not fit goes to kitchen storage, then drops beside the block. */
	private void give(ServerLevel level, Player player, ItemStack part) {
		if (player.getInventory().add(part) || part.isEmpty()) return;
		if (player instanceof ServerPlayer serverPlayer) {
			items.set(1, part);
			CabinKitchenStorage.overflow(serverPlayer, this, 1);
			part = items.get(1);
			items.set(1, ItemStack.EMPTY);
		}
		if (!part.isEmpty()) Block.popResource(level, worldPosition.above(), part);
	}

	/** Says which tool works what is on the block: the knife for fish, the cleaver for carcasses. */
	void wrongTool(Player player) {
		CarcassData data = CarcassData.forBlock(carcass()).orElse(null);
		if (data != null) tell(player, data.dressing() == CarcassData.Dressing.NONE ? "use_knife" : "use_cleaver");
	}

	private static void tell(Player player, String message) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Component.translatable("message.portable_pocket_cabin.butcher_block." + message), true);
		}
	}

}
