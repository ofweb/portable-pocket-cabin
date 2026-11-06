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
import java.util.List;
import java.util.Optional;

/**
 * Fills a pot or oven from the recipe panel. The whole transfer is planned first, and nothing moves unless all of it
 * can. Inside the kitchen, central storage supplies ingredients and fuel first and takes displaced items when the
 * inventory is full. Named and customised stacks are never taken.
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

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (request, context) -> handle(context.player(), request));
	}

	private static void handle(ServerPlayer player, Request request) {
		if (!(player.containerMenu instanceof CookingMenu menu) || !menu.pos().equals(request.pos()) || !menu.stillValid(player)) return;
		if (!(player.level().getBlockEntity(request.pos()) instanceof CookingStationBlockEntity station)) return;
		CookingStation kind = menu.kind().station;
		if (kind == null) return;
		var holder = player.level().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, request.recipe())).orElse(null);
		if (holder == null || !(holder.value() instanceof CookingRecipe recipe) || recipe.station() != kind) return;
		var kitchen = CabinKitchenStorage.cabinFor(player, request.pos());
		if (kitchen.isPresent()) fillFromStorage(player, kitchen.get(), station, kind, recipe);
		else fill(station, kind, recipe, player.getInventory());
		menu.broadcastChanges();
		ServerPlayNetworking.send(player, new CookbookAvailability.Reply(CookbookAvailability.fillable(player, station, kind), true));
	}

	/**
	 * A planned fill: every slot of the station afterwards, the heating station's slots when it gets fuel, the
	 * player's inventory, and storage with what it gives and receives.
	 */
	record Plan(List<ItemStack> station, Optional<List<ItemStack>> heater, List<ItemStack> inventory,
		Optional<CabinStorageState> storage, List<ItemStack> withdrawn, List<ItemStack> deposited) {
	}

	/** Fills the station from the player's inventory alone. Returns false, changing nothing, when it cannot. */
	static boolean fill(Container station, CookingStation kind, CookingRecipe recipe, Inventory player) {
		Plan plan = plan(station, kind, recipe, player, null, null, -1).orElse(null);
		if (plan == null) return false;
		for (int slot = 0; slot < plan.station().size(); slot++) station.setItem(slot, plan.station().get(slot));
		for (int slot = 0; slot < plan.inventory().size(); slot++) player.setItem(slot, plan.inventory().get(slot));
		station.setChanged();
		player.setChanged();
		return true;
	}

	/**
	 * Fills a kitchen station from storage first, then the inventory, and tops up the fuel that heats it. The
	 * transfer commits through a receipt (ADR-0012). Returns false, changing nothing, when it cannot be planned.
	 */
	static boolean fillFromStorage(ServerPlayer player, CabinRecord cabin, CookingStationBlockEntity station, CookingStation kind,
		CookingRecipe recipe) {
		CookingStationBlockEntity heater = null;
		int heaterSlot = -1;
		if (station instanceof OvenBlockEntity) {
			heater = station;
			heaterSlot = OvenBlockEntity.FUEL_SLOT;
		} else if (kind.heated() && player.level().getBlockEntity(station.getBlockPos().below()) instanceof StoveBlockEntity stove
			&& CabinKitchen.contains(cabin, stove.getBlockPos())) {
			heater = stove;
			heaterSlot = StoveBlockEntity.FUEL_SLOT;
		}
		Plan plan = plan(station, kind, recipe, player.getInventory(), cabin.upgrades().storage(), heater, heaterSlot).orElse(null);
		if (plan == null) return false;
		List<CabinKitchenStorage.StationAfter> stations = new ArrayList<>();
		List<ItemStack> contents = plan.station();
		if (heater == station && plan.heater().isPresent()) contents = plan.heater().get();
		stations.add(new CabinKitchenStorage.StationAfter(station, contents));
		if (heater != null && heater != station && plan.heater().isPresent()) {
			stations.add(new CabinKitchenStorage.StationAfter(heater, plan.heater().get()));
		}
		CabinKitchenStorage.commit(player, cabin, stations, plan.inventory(), plan.storage().orElseThrow(), plan.withdrawn(), plan.deposited());
		return true;
	}

	/**
	 * Plans a fill without changing anything. Ingredients come from what the station already holds, then storage,
	 * then the main inventory, by the shared allocation. Each ingredient keeps its own slots and the layout must match
	 * the recipe. Displaced items go to the inventory, then storage. With a heater whose fuel slot is empty, it also
	 * plans enough fuel at the recipe's heat for one batch, from storage first. Named and customised stacks are never taken.
	 */
	static Optional<Plan> plan(Container station, CookingStation kind, CookingRecipe recipe, Inventory player,
		CabinStorageState storage, Container heater, int heaterSlot) {
		int slots = kind.ingredientSlots();
		boolean usesContainer = recipe.container().isPresent();
		int containerSlot = usesContainer ? CookingMenu.Kind.of(kind).containerSlot : -1;
		List<ItemStack> stacks = new ArrayList<>();
		for (int slot = 0; slot < slots; slot++) stacks.add(station.getItem(slot).copy());
		if (usesContainer) stacks.add(station.getItem(containerSlot).copy());
		int held = stacks.size();
		List<ItemStack> stored = storage == null ? List.of() : new ArrayList<>(storage.stacks());
		stacks.addAll(stored);
		int inventoryStart = stacks.size();
		List<ItemStack> main = new ArrayList<>();
		for (ItemStack stack : player.getNonEquipmentItems()) main.add(stack.copy());
		stacks.addAll(main);
		List<CookingRecipe.Counted> demands = CookbookAvailability.demands(recipe);
		int[][] amounts = CookingAllocation.allocate(demands, stacks).orElse(null);
		if (amounts == null) return Optional.empty();
		int[] taken = CookingAllocation.taken(amounts, stacks.size());

		List<ItemStack> planned = new ArrayList<>();
		for (int demand = 0; demand < recipe.ingredients().size(); demand++) {
			// Each ingredient gets its own slots: the recipe matches every slot to exactly one ingredient.
			List<ItemStack> own = new ArrayList<>();
			for (int stack = 0; stack < stacks.size(); stack++) {
				if (amounts[demand][stack] > 0 && !merge(own, stacks.get(stack).copyWithCount(amounts[demand][stack]), slots)) return Optional.empty();
			}
			planned.addAll(own);
		}
		if (planned.size() > slots) return Optional.empty();
		List<ItemStack> displaced = new ArrayList<>();
		for (int slot = 0; slot < slots; slot++) {
			ItemStack current = stacks.get(slot);
			if (current.getCount() > taken[slot]) displaced.add(current.copyWithCount(current.getCount() - taken[slot]));
		}
		ItemStack container = ItemStack.EMPTY;
		if (usesContainer) {
			int demand = demands.size() - 1;
			ItemStack current = stacks.get(slots);
			int kept = current.getCount() - taken[slots];
			List<ItemStack> parts = new ArrayList<>();
			if (amounts[demand][slots] > 0) parts.add(current.copyWithCount(kept + amounts[demand][slots]));
			else if (kept > 0) displaced.add(current.copyWithCount(kept));
			for (int stack = 0; stack < stacks.size(); stack++) {
				if (stack != slots && amounts[demand][stack] > 0) parts.add(stacks.get(stack).copyWithCount(amounts[demand][stack]));
			}
			List<ItemStack> merged = new ArrayList<>();
			for (ItemStack part : parts) if (!merge(merged, part, 1)) return Optional.empty();
			container = merged.isEmpty() ? ItemStack.EMPTY : merged.getFirst();
		}
		List<ItemStack> layout = new ArrayList<>(planned);
		while (layout.size() < slots) layout.add(ItemStack.EMPTY);
		if (recipe.assign(new CookingInput(layout, container, recipe.water(), recipe.oil())).isEmpty()) return Optional.empty();

		List<ItemStack> withdrawn = new ArrayList<>();
		for (int stack = held; stack < stacks.size(); stack++) {
			if (taken[stack] == 0) continue;
			if (stack < inventoryStart) withdrawn.add(stacks.get(stack).copyWithCount(taken[stack]));
			stacks.get(stack).shrink(taken[stack]);
		}

		Optional<List<ItemStack>> heaterAfter = Optional.empty();
		ItemStack fuel = ItemStack.EMPTY;
		if (heater != null && heater.getItem(heaterSlot).isEmpty() && recipe.heat().isPresent()) {
			var fuelValues = ((net.minecraft.world.level.block.entity.BlockEntity) heater).getLevel().fuelValues();
			// The longest-burning fuel at the recipe's heat, so planks are used before bowls or sticks; ties go to storage.
			int best = -1;
			for (int stack = held; stack < stacks.size(); stack++) {
				ItemStack candidate = stacks.get(stack);
				if (!CabinStation.ordinary(candidate) || !CookingHeat.ofFuel(candidate, fuelValues).equals(recipe.heat())) continue;
				int burn = fuelValues.burnDuration(candidate);
				if (burn > 0 && (best < 0 || burn > fuelValues.burnDuration(stacks.get(best)))) best = stack;
			}
			if (best >= 0) {
				ItemStack candidate = stacks.get(best);
				int burn = fuelValues.burnDuration(candidate);
				int count = Math.min(candidate.getCount(), Math.min(candidate.getMaxStackSize(), Math.max(1, (recipe.time() + burn - 1) / burn)));
				fuel = candidate.split(count);
				if (best < inventoryStart) withdrawn.add(fuel.copy());
			}
		}

		// Displaced items go to the main inventory; only what does not fit is left for storage.
		for (int index = 0; index < displaced.size(); index++) {
			ItemStack rest = CabinKitchenStorage.insert(main, displaced.get(index));
			if (!rest.isEmpty() && storage == null) return Optional.empty();
			displaced.set(index, rest);
		}
		CabinStorageState storageAfter = null;
		List<ItemStack> deposited = new ArrayList<>();
		if (storage != null) {
			storageAfter = storage.withStacks(stored.stream().filter(stack -> !stack.isEmpty()).toList());
			for (ItemStack rest : displaced) {
				if (rest.isEmpty()) continue;
				var transfer = storageAfter.deposit(rest, rest.getCount());
				if (transfer.moved().getCount() != rest.getCount()) return Optional.empty();
				storageAfter = transfer.state();
				deposited.add(rest.copy());
			}
		}

		List<ItemStack> stationAfter = new ArrayList<>();
		for (int slot = 0; slot < station.getContainerSize(); slot++) stationAfter.add(station.getItem(slot).copy());
		for (int slot = 0; slot < slots; slot++) stationAfter.set(slot, slot < planned.size() ? planned.get(slot) : ItemStack.EMPTY);
		if (usesContainer) stationAfter.set(containerSlot, container);
		if (!fuel.isEmpty()) {
			List<ItemStack> heated = heater == station ? stationAfter : new ArrayList<>();
			if (heater != station) for (int slot = 0; slot < heater.getContainerSize(); slot++) heated.add(heater.getItem(slot).copy());
			heated.set(heaterSlot, fuel);
			heaterAfter = Optional.of(List.copyOf(heated));
		}
		List<ItemStack> inventoryAfter = new ArrayList<>();
		for (int slot = 0; slot < player.getContainerSize(); slot++) {
			inventoryAfter.add(slot < main.size() ? main.get(slot) : player.getItem(slot).copy());
		}
		return Optional.of(new Plan(List.copyOf(stationAfter), heaterAfter, inventoryAfter, Optional.ofNullable(storageAfter), withdrawn, deposited));
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
}
