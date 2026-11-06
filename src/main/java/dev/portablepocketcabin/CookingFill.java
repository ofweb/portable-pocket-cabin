package dev.portablepocketcabin;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Fills a pot or oven from the recipe panel. The station's ingredient slots go back to the player first, then the
 * recipe's ingredients and container move in from the sources. Named and customised stacks are never taken.
 */
final class CookingFill {
	private CookingFill() {
	}

	/** Sent by the recipe panel of an open pot or oven screen. */
	record Request(BlockPos pos, Identifier recipe) implements CustomPacketPayload {
		static final Type<Request> TYPE = new Type<>(PortablePocketCabin.id("cooking_fill"));
		static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, Request::pos, Identifier.STREAM_CODEC, Request::recipe, Request::new);

		@Override
		public Type<Request> type() {
			return TYPE;
		}
	}

	/** One place ingredients can come from. B-0041 adds central storage in front of the inventory. */
	interface Source {
		List<ItemStack> stacks();
	}

	static Source inventory(Inventory inventory) {
		return () -> inventory.getNonEquipmentItems();
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (request, context) -> handle(context.player(), request));
	}

	private static void handle(ServerPlayer player, Request request) {
		if (!(player.containerMenu instanceof CookingMenu menu) || !menu.pos().equals(request.pos()) || !menu.stillValid(player)) return;
		if (!(player.level().getBlockEntity(request.pos()) instanceof CookingStationBlockEntity station)) return;
		CookingStation kind = switch (menu.kind()) {
			case POT -> CookingStation.POT;
			case OVEN -> CookingStation.OVEN;
			default -> null;
		};
		if (kind == null) return;
		var holder = player.level().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, request.recipe())).orElse(null);
		if (holder == null || !(holder.value() instanceof CookingRecipe recipe) || recipe.station() != kind) return;
		fill(station, kind, recipe, player.getInventory(), List.of(inventory(player.getInventory())));
		menu.broadcastChanges();
		ServerPlayNetworking.send(player, new CookbookAvailability.Reply(CookbookAvailability.fillable(player, station, kind), true));
	}

	/**
	 * Fills the station for the recipe. Returns false, changing nothing, when the sources and the station's own
	 * ingredients together cannot make it.
	 */
	static boolean fill(Container station, CookingStation kind, CookingRecipe recipe, Inventory player, List<Source> sources) {
		int slots = kind.ingredientSlots();
		int containerSlot = kind == CookingStation.POT ? CookingPotBlockEntity.CONTAINER_SLOT : -1;
		List<ItemStack> everything = new ArrayList<>();
		for (Source source : sources) everything.addAll(source.stacks());
		for (int slot = 0; slot < slots; slot++) everything.add(station.getItem(slot));
		if (containerSlot >= 0) everything.add(station.getItem(containerSlot));
		if (!CookbookAvailability.canMake(recipe, everything)) return false;

		for (int slot = 0; slot < slots; slot++) player.placeItemBackInInventory(station.removeItemNoUpdate(slot));
		boolean keepContainer = recipe.container().map(needed -> {
			ItemStack held = station.getItem(containerSlot);
			return needed.ingredient().test(held) && held.getCount() >= needed.count();
		}).orElse(true);
		if (containerSlot >= 0 && !keepContainer) player.placeItemBackInInventory(station.removeItemNoUpdate(containerSlot));

		for (CookingRecipe.Counted entry : recipe.ingredients()) {
			for (ItemStack taken : take(sources, entry.ingredient(), entry.count())) {
				ItemStack left = place(station, 0, slots, taken);
				if (!left.isEmpty()) player.placeItemBackInInventory(left);
			}
		}
		if (!keepContainer) {
			CookingRecipe.Counted needed = recipe.container().orElseThrow();
			for (ItemStack taken : take(sources, needed.ingredient(), needed.count())) {
				ItemStack left = place(station, containerSlot, containerSlot + 1, taken);
				if (!left.isEmpty()) player.placeItemBackInInventory(left);
			}
		}
		station.setChanged();
		player.setChanged();
		return true;
	}

	/** Takes {@code count} ordinary items that the ingredient accepts from the sources, in order. */
	private static List<ItemStack> take(List<Source> sources, Ingredient ingredient, int count) {
		List<ItemStack> taken = new ArrayList<>();
		int needed = count;
		for (Source source : sources) {
			for (ItemStack stack : source.stacks()) {
				if (needed == 0) return taken;
				if (!CabinStation.ordinary(stack) || !ingredient.test(stack)) continue;
				ItemStack split = stack.split(Math.min(needed, stack.getCount()));
				needed -= split.getCount();
				taken.add(split);
			}
		}
		return taken;
	}

	/** Puts the stack into the slots, merging with the same item first. Returns what did not fit. */
	private static ItemStack place(Container station, int from, int to, ItemStack stack) {
		for (int slot = from; slot < to && !stack.isEmpty(); slot++) {
			ItemStack current = station.getItem(slot);
			if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)) {
				int moved = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
				current.grow(moved);
				stack.shrink(moved);
			}
		}
		for (int slot = from; slot < to && !stack.isEmpty(); slot++) {
			if (station.getItem(slot).isEmpty()) {
				station.setItem(slot, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
			}
		}
		return stack;
	}
}
