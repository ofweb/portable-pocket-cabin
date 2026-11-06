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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
	 * Fills the station for the recipe. The whole transfer is planned first: which stacks supply each ingredient,
	 * preferring what the station already holds, the station's new contents, and where displaced items go. Returns
	 * false, changing nothing, when the recipe cannot be made or the displaced items do not fit in the inventory.
	 */
	static boolean fill(Container station, CookingStation kind, CookingRecipe recipe, Inventory player, List<Source> sources) {
		int slots = kind.ingredientSlots();
		boolean usesContainer = recipe.container().isPresent();
		int containerSlot = usesContainer ? CookingPotBlockEntity.CONTAINER_SLOT : -1;
		List<ItemStack> stacks = new ArrayList<>();
		for (int slot = 0; slot < slots; slot++) stacks.add(station.getItem(slot));
		if (usesContainer) stacks.add(station.getItem(containerSlot));
		int held = stacks.size();
		for (Source source : sources) stacks.addAll(source.stacks());
		List<CookingRecipe.Counted> demands = CookbookAvailability.demands(recipe);
		int[][] amounts = CookingAllocation.allocate(demands, stacks).orElse(null);
		if (amounts == null) return false;
		int[] taken = CookingAllocation.taken(amounts, stacks.size());

		List<ItemStack> planned = new ArrayList<>();
		List<ItemStack> displaced = new ArrayList<>();
		for (int demand = 0; demand < recipe.ingredients().size(); demand++) {
			for (int stack = 0; stack < stacks.size(); stack++) {
				if (amounts[demand][stack] > 0 && !merge(planned, stacks.get(stack).copyWithCount(amounts[demand][stack]), slots)) return false;
			}
		}
		for (int slot = 0; slot < slots; slot++) {
			ItemStack current = stacks.get(slot);
			if (current.getCount() > taken[slot]) displaced.add(current.copyWithCount(current.getCount() - taken[slot]));
		}
		ItemStack container = ItemStack.EMPTY;
		if (usesContainer) {
			int demand = demands.size() - 1;
			int heldIndex = slots;
			ItemStack current = stacks.get(heldIndex);
			int kept = current.getCount() - taken[heldIndex];
			List<ItemStack> parts = new ArrayList<>();
			if (amounts[demand][heldIndex] > 0) {
				parts.add(current.copyWithCount(kept + amounts[demand][heldIndex]));
			} else if (kept > 0) {
				displaced.add(current.copyWithCount(kept));
			}
			for (int stack = 0; stack < stacks.size(); stack++) {
				if (stack != heldIndex && amounts[demand][stack] > 0) parts.add(stacks.get(stack).copyWithCount(amounts[demand][stack]));
			}
			List<ItemStack> merged = new ArrayList<>();
			for (ItemStack part : parts) if (!merge(merged, part, 1)) return false;
			container = merged.isEmpty() ? ItemStack.EMPTY : merged.getFirst();
		}

		if (!fits(player, stacks, held, taken, displaced)) return false;

		for (int stack = held; stack < stacks.size(); stack++) stacks.get(stack).shrink(taken[stack]);
		for (int slot = 0; slot < slots; slot++) station.setItem(slot, slot < planned.size() ? planned.get(slot) : ItemStack.EMPTY);
		if (usesContainer) station.setItem(containerSlot, container);
		for (ItemStack stack : displaced) {
			player.add(stack);
			if (!stack.isEmpty()) player.placeItemBackInInventory(stack);
		}
		station.setChanged();
		player.setChanged();
		return true;
	}

	/** Adds the stack to the planned slots, merging with the same item first. Returns false when it needs more than {@code limit} slots. */
	private static boolean merge(List<ItemStack> planned, ItemStack stack, int limit) {
		for (ItemStack slot : planned) {
			if (stack.isEmpty()) return true;
			if (ItemStack.isSameItemSameComponents(slot, stack)) {
				int moved = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(moved);
				stack.shrink(moved);
			}
		}
		while (!stack.isEmpty()) {
			if (planned.size() >= limit) return false;
			planned.add(stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
		}
		return true;
	}

	/**
	 * Whether the displaced items fit in the main inventory once the planned ingredients have left it. Only the
	 * 36 main slots are counted, so the real insert, which can also use the offhand, never has less room.
	 */
	private static boolean fits(Inventory player, List<ItemStack> stacks, int held, int[] taken, List<ItemStack> displaced) {
		List<ItemStack> main = player.getNonEquipmentItems();
		Map<ItemStack, Integer> index = new IdentityHashMap<>();
		for (int slot = 0; slot < main.size(); slot++) index.put(main.get(slot), slot);
		List<ItemStack> after = new ArrayList<>();
		for (ItemStack stack : main) after.add(stack.copy());
		for (int stack = held; stack < stacks.size(); stack++) {
			Integer slot = index.get(stacks.get(stack));
			if (slot != null) after.get(slot).shrink(taken[stack]);
		}
		for (ItemStack item : displaced) {
			ItemStack rest = item.copy();
			for (ItemStack slot : after) {
				if (rest.isEmpty()) break;
				if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
					int moved = Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount());
					slot.grow(moved);
					rest.shrink(moved);
				}
			}
			for (int slot = 0; slot < after.size() && !rest.isEmpty(); slot++) {
				if (after.get(slot).isEmpty()) after.set(slot, rest.split(Math.min(rest.getCount(), rest.getMaxStackSize())));
			}
			if (!rest.isEmpty()) return false;
		}
		return true;
	}
}
