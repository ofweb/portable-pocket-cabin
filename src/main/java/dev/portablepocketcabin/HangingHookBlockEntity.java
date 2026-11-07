package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * A hanging hook holds one carcass. A glass bottle collects blood from a fresh carcass. The knife skins or scrapes, and
 * an empty hand plucks, following the carcass data. Sneaking with an empty hand takes the carcass down.
 */
final class HangingHookBlockEntity extends DisplayStationBlockEntity {
	HangingHookBlockEntity(BlockPos pos, BlockState state) {
		super(Butchery.HANGING_HOOK_ENTITY, pos, state, 1);
	}

	ItemStack hung() {
		return items.get(0);
	}

	/** Hangs a carcass, bleeds it, or dresses it with the knife. Returns whether anything happened. */
	boolean useItem(ServerLevel level, Player player, ItemStack held, InteractionHand hand) {
		ItemStack hung = hung();
		if (hung.isEmpty()) {
			if (CarcassData.forCarcass(held).filter(data -> data.dressing() != CarcassData.Dressing.NONE).isEmpty()) return false;
			items.set(0, held.split(1));
			level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
			changedForClients();
			return true;
		}
		CarcassData data = CarcassData.forCarcass(hung).orElse(null);
		if (data == null) return false;
		if (held.is(Items.GLASS_BOTTLE)) {
			if (!Butchery.state(hung).fresh()) {
				tell(player, "no_blood");
				return true;
			}
			held.consume(1, player);
			give(level, player, new ItemStack(Butchery.BLOOD));
			hung.set(Butchery.CARCASS, Butchery.state(hung).withBled());
			level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
			changedForClients();
			return true;
		}
		if (held.is(CookingItems.KITCHEN_KNIFE)) {
			if (data.dressing() == CarcassData.Dressing.PLUCK) {
				tell(player, "pluck_by_hand");
				return true;
			}
			if (data.dressing() == CarcassData.Dressing.SKIN || data.dressing() == CarcassData.Dressing.SCRAPE) dress(level, player, data);
			return true;
		}
		return false;
	}

	/** Plucks a bird by hand, or with sneaking takes the carcass down. */
	boolean useHand(ServerLevel level, Player player) {
		ItemStack hung = hung();
		if (hung.isEmpty()) return false;
		CarcassData data = CarcassData.forCarcass(hung).orElse(null);
		if (!player.isShiftKeyDown() && data != null && data.dressing() == CarcassData.Dressing.PLUCK) {
			dress(level, player, data);
			return true;
		}
		if (!player.isShiftKeyDown() && data != null) {
			tell(player, data.dressing() == CarcassData.Dressing.SKIN ? "skin_with_knife" : "scrape_with_knife");
			return true;
		}
		items.set(0, ItemStack.EMPTY);
		give(level, player, hung);
		changedForClients();
		return true;
	}

	/** Skins, plucks, or scrapes the hung carcass, giving what comes off and leaving the dressed carcass on the hook. */
	private void dress(ServerLevel level, Player player, CarcassData data) {
		ItemStack hung = hung();
		var state = Butchery.state(hung);
		if (data.scald() && !state.scalded()) {
			tell(player, "needs_scalding");
			return;
		}
		List<ItemStack> yields = new ArrayList<>();
		for (ItemStackTemplate template : data.dressingGives()) yields.add(template.create());
		state.fleece().ifPresent(color -> yields.add(new ItemStack(BuiltInRegistries.ITEM.getValue(
			Identifier.withDefaultNamespace(color.getSerializedName() + "_wool")))));
		for (ItemStack yield : yields) give(level, player, yield);
		items.set(0, data.dressed().map(ItemStack::new).orElse(ItemStack.EMPTY));
		if (data.dressing() == CarcassData.Dressing.PLUCK) {
			level.playSound(null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.8F, 1.4F);
		} else {
			player.getMainHandItem().hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
			level.playSound(null, worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 0.9F);
		}
		changedForClients();
	}

	private void give(ServerLevel level, Player player, ItemStack stack) {
		if (!player.getInventory().add(stack) && !stack.isEmpty()) Block.popResource(level, worldPosition.below(), stack);
	}

	private static void tell(Player player, String message) {
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Component.translatable("message.portable_pocket_cabin.hook." + message), true);
		}
	}
}
