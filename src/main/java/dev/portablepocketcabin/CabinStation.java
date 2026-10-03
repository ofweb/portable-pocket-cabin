package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import java.util.function.Predicate;

public final class CabinStation {
	private static final Map<AbstractContainerMenu, CabinStation> OPEN = new IdentityHashMap<>();
	private final ServerPlayer player;
	private final UUID cabinId;
	private final BlockPos position;
	private final AbstractContainerMenu menu;
	private final int[] inputs;
	private final int result;
	private final Map<Integer, ItemStack> storageInputs = new HashMap<>();
	private List<Choice> choices = List.of();
	private Choice selected;
	private boolean internal;
	private boolean shifting;
	private int shiftRemaining;

	record Choice(String title, ItemStack icon, List<Predicate<ItemStack>> ingredients, int targetSlot,
		ItemStack target, RecipeHolder<?> recipe, int operation) { }

	private CabinStation(ServerPlayer player, CabinRecord cabin, BlockPos position, AbstractContainerMenu menu) {
		this.player = player; this.cabinId = cabin.uuid(); this.position = position.immutable(); this.menu = menu;
		if (menu instanceof CraftingMenu) { inputs = new int[]{1,2,3,4,5,6,7,8,9}; result = 0; }
		else if (menu instanceof LoomMenu || menu instanceof SmithingMenu) { inputs = new int[]{0,1,2}; result = 3; }
		else if (menu instanceof CartographyTableMenu) { inputs = new int[]{0,1}; result = 2; }
		else { inputs = new int[]{0}; result = 1; }
	}

	static InteractionResult open(ServerPlayer player, CabinRecord cabin, BlockPos pos) {
		if (!CabinStorage.mayUse(cabin, player.getUUID())) return InteractionResult.FAIL;
		var state = player.level().getBlockState(pos);
		var access = ContainerLevelAccess.create(player.level(), pos);
		var provider = new SimpleMenuProvider((id, inventory, actor) -> {
			AbstractContainerMenu menu = state.is(Blocks.CRAFTING_TABLE) ? new CraftingMenu(id, inventory, access)
				: state.is(Blocks.LOOM) ? new LoomMenu(id, inventory, access)
				: state.is(Blocks.CARTOGRAPHY_TABLE) ? new CartographyTableMenu(id, inventory, access)
				: state.is(Blocks.STONECUTTER) ? new StonecutterMenu(id, inventory, access)
				: new SmithingMenu(id, inventory, access);
			OPEN.put(menu, new CabinStation(player, cabin, pos, menu));
			return menu;
		}, state.getBlock().getName());
		if (player.openMenu(provider).isEmpty()) return InteractionResult.FAIL;
		OPEN.get(player.containerMenu).sendChoices();
		return InteractionResult.SUCCESS_SERVER;
	}

	private CabinRecord cabin() { return CabinRegistry.get(player.level().getServer()).find(cabinId).orElse(null); }
	private boolean valid() {
		var cabin = cabin();
		if (!CabinStorage.mayUse(cabin, player.getUUID()) || cabin.upgrades().operationInProgress()
			|| !player.level().dimension().equals(PocketDimension.LEVEL_KEY)) return false;
		var station = CabinCrafting.stations(cabin, cabin.upgrades().crafting().level()).get(position);
		var relative = player.blockPosition().subtract(PocketDimension.cellCenter(cabin.cellIndex()));
		var space = CabinCrafting.space(cabin);
		return station != null && player.level().getBlockState(position).equals(station)
			&& relative.getX() > space.minimum().getX() && relative.getX() < space.maximum().getX()
			&& relative.getZ() > space.minimum().getZ() && relative.getZ() < space.maximum().getZ()
			&& player.distanceToSqr(position.getX() + .5, position.getY() + .5, position.getZ() + .5) <= 64;
	}

	public static boolean beforeClick(AbstractContainerMenu menu, Player actor) {
		var station = OPEN.get(menu);
		if (station == null || station.internal) return true;
		if (actor != station.player || !station.valid()) return false;
		menu.slotsChanged(menu.getSlot(station.inputs[0]).container);
		return true;
	}
	public static boolean shiftClick(AbstractContainerMenu menu, int slot, int button, ContainerInput input, Player actor) {
		var station = OPEN.get(menu);
		if (station == null || station.internal || slot != station.result || input != ContainerInput.QUICK_MOVE) return false;
		if (!station.valid()) return true;
		station.shiftRemaining = menu.getSlot(slot).getItem().getMaxStackSize();
		station.shifting = true; station.internal = true;
		try {
			while (station.shiftRemaining > 0 && menu.getSlot(slot).hasItem()) {
				int before = station.shiftRemaining;
				menu.clicked(slot, button, input, actor);
				station.reconcileSources();
				if (before == station.shiftRemaining || station.selected == null) break;
				station.fill(station.selected);
			}
		} finally { station.shifting = false; station.internal = false; }
		station.checkpoint(); station.sendChoices();
		return true;
	}
	public static boolean mayResultClick(AbstractContainerMenu menu, int slot, ContainerInput input) {
		var station = OPEN.get(menu);
		if (station == null || slot != station.result || input != ContainerInput.PICKUP) return true;
		ItemStack result = menu.getSlot(slot).getItem(), cursor = menu.getCarried();
		return cursor.isEmpty() || ItemStack.isSameItemSameComponents(cursor, result)
			&& cursor.getCount() + result.getCount() <= cursor.getMaxStackSize();
	}
	public static boolean mayQuickMove(AbstractContainerMenu menu, int slot) {
		var station = OPEN.get(menu);
		if (station == null || slot != station.result) return true;
		ItemStack result = menu.getSlot(slot).getItem();
		if (station.shifting && result.getCount() > station.shiftRemaining) return false;
		int space = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack stack = station.player.getInventory().getItem(i);
			if (stack.isEmpty()) space += result.getMaxStackSize();
			else if (ItemStack.isSameItemSameComponents(stack, result)) space += stack.getMaxStackSize() - stack.getCount();
		}
		return space >= result.getCount();
	}

	public static void didQuickMove(AbstractContainerMenu menu, int slot, ItemStack moved) {
		var station = OPEN.get(menu);
		if (station != null && station.shifting && slot == station.result && !moved.isEmpty())
			station.shiftRemaining -= moved.getCount();
	}

	public static void afterClick(AbstractContainerMenu menu, int slot) {
		var station = OPEN.get(menu);
		if (station == null || station.internal) return;
		station.reconcileSources();
		if (slot == station.result && station.selected != null && station.valid()) station.fill(station.selected);
		station.checkpoint();
		station.sendChoices();
	}
	public static boolean button(AbstractContainerMenu menu, Player actor, int button) {
		var station = OPEN.get(menu);
		if (station == null || button < 10000) return false;
		if (actor != station.player || !station.valid()) return true;
		int index = button - 10000;
		if (index >= 0 && index < station.choices.size()) {
			station.selected = station.choices.get(index);
			station.returnInputs();
			station.fill(station.selected);
			station.checkpoint();
			station.sendChoices();
		}
		return true;
	}
	public static boolean place(AbstractContainerMenu menu, RecipeHolder<?> recipe) {
		var station = OPEN.get(menu);
		if (station == null) return false;
		if (!station.valid()) return true;
		var choice = station.recipeChoice(recipe);
		if (choice != null) {
			station.returnInputs(); station.selected = choice; station.fill(choice); station.checkpoint();
		}
		return true;
	}
	public static void close(AbstractContainerMenu menu) {
		var station = OPEN.remove(menu);
		if (station == null) return;
		station.reconcileSources();
		station.checkpoint();
		station.internal = true;
		try {
			for (int slot : station.inputs) menu.getSlot(slot).set(ItemStack.EMPTY);
			menu.setCarried(ItemStack.EMPTY);
			CabinStorage.recover(station.player);
		} finally { station.internal = false; }
	}

	static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var station : List.copyOf(OPEN.values())) {
				if (station.player.containerMenu != station.menu) close(station.menu);
				else if (!station.valid()) station.player.closeContainer();
			}
		});
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (OPEN.containsKey(handler.player.containerMenu)) handler.player.closeContainer();
		});
	}

	static boolean ordinary(ItemStack stack) {
		return !stack.isEmpty() && stack.getComponentsPatch().isEmpty();
	}
	private List<ItemStack> available() {
		var result = new ArrayList<>(cabin().upgrades().storage().stacks());
		for (int i = 0; i < 36; i++) if (!player.getInventory().getItem(i).isEmpty()) result.add(player.getInventory().getItem(i).copy());
		for (int i : inputs) if (menu.getSlot(i).hasItem()) result.add(menu.getSlot(i).getItem().copy());
		return result;
	}
	private void reconcileSources() {
		storageInputs.replaceAll((slot, original) -> {
			ItemStack current = menu.getSlot(slot).getItem();
			return ItemStack.isSameItemSameComponents(original, current) ? original.copyWithCount(Math.min(original.getCount(), current.getCount())) : ItemStack.EMPTY;
		});
		storageInputs.values().removeIf(ItemStack::isEmpty);
	}
	private void fill(Choice choice) {
		if (!valid()) return;
		if (choice.recipe() != null && player.level().getServer().getRecipeManager().byKey(choice.recipe().id())
			.map(current -> current.value() != choice.recipe().value()).orElse(true)) return;
		boolean previousInternal = internal; internal = true;
		try {
			var registry = CabinRegistry.get(player.level().getServer());
			var cabin = cabin(); var storage = cabin.upgrades().storage();
			var plannedStorage = storage;
			var inventory = new ArrayList<ItemStack>();
			for (int i = 0; i < 36; i++) inventory.add(player.getInventory().getItem(i).copy());
			var planned = new HashMap<Integer, ItemStack>();
			var sources = new HashMap<Integer, ItemStack>();
			for (int index = 0; index < inputs.length; index++) {
				Predicate<ItemStack> ingredient = choice.ingredients().get(index);
				if (ingredient == null) continue;
				int slot = inputs[index];
				if (menu.getSlot(slot).hasItem()) {
					if (!ingredient.test(menu.getSlot(slot).getItem())) return;
					continue;
				}
				boolean explicit = index == choice.targetSlot() && !choice.target().isEmpty();
				Predicate<ItemStack> accepts = stack -> ingredient.test(stack) && (explicit
					? ItemStack.isSameItemSameComponents(choice.target(), stack) : ordinary(stack));
				ItemStack storageStack = plannedStorage.stacks().stream().filter(accepts).findFirst().orElse(ItemStack.EMPTY);
				if (!storageStack.isEmpty()) {
					var transfer = plannedStorage.withdraw(storageStack, 1); plannedStorage = transfer.state();
					planned.put(slot, transfer.moved()); sources.put(slot, transfer.moved()); continue;
				}
				boolean found = false;
				for (ItemStack stack : inventory) if (accepts.test(stack)) {
					planned.put(slot, stack.copyWithCount(1)); stack.shrink(1); found = true; break;
				}
				if (!found) return;
			}
			registry.updateUpgradeState(cabinId, cabin.upgrades().withStorage(plannedStorage));
			for (int i = 0; i < 36; i++) player.getInventory().setItem(i, inventory.get(i));
			planned.forEach((slot, stack) -> menu.getSlot(slot).set(stack)); storageInputs.putAll(sources);
			if (menu instanceof StonecutterMenu stone && choice.recipe() != null) {
				var entries = stone.getVisibleRecipes().entries();
				for (int i = 0; i < entries.size(); i++) if (entries.get(i).recipe().recipe().map(r -> r.id().equals(choice.recipe().id())).orElse(false))
					stone.clickMenuButton(player, i);
			}
			if (menu instanceof LoomMenu loom) loom.clickMenuButton(player, choice.operation());
			menu.broadcastChanges();
		} finally { internal = previousInternal; }
	}

	private void returnInputs() {
		reconcileSources();
		checkpoint();
		boolean previousInternal = internal; internal = true;
		try {
			for (int slot : inputs) menu.getSlot(slot).set(ItemStack.EMPTY);
			menu.setCarried(ItemStack.EMPTY);
			CabinStorage.recover(player);
			storageInputs.clear();
		} finally { internal = previousInternal; }
	}

	private void checkpoint() {
		var cabin = cabin(); if (cabin == null) return;
		var receipt = CabinStorage.receipt(player, menu.getCarried());
		List<CabinStorageState.Escrow> escrow = new ArrayList<>();
		for (int slot : inputs) {
			ItemStack stack = menu.getSlot(slot).getItem().copy();
			int stored = storageInputs.getOrDefault(slot, ItemStack.EMPTY).getCount();
			if (stored > 0) { escrow.add(new CabinStorageState.Escrow(stack.copyWithCount(stored), true)); stack.shrink(stored); }
			if (!stack.isEmpty()) escrow.add(new CabinStorageState.Escrow(stack, false));
		}
		var session = new CabinStorageState.Session(receipt.player(), receipt.inventory(), receipt.cursor(), receipt.operation(), receipt.delivery(), escrow);
		CabinRegistry.get(player.level().getServer()).updateUpgradeState(cabinId, cabin.upgrades().withStorage(cabin.upgrades().storage().withSession(session)));
		CabinRegistry.flush(player.level().getServer());
		player.level().getServer().getPlayerList().saveAll();
	}

	private Choice recipeChoice(RecipeHolder<?> holder) {
		var recipe = holder.value();
		List<Predicate<ItemStack>> ingredients = new ArrayList<>(Collections.nCopies(inputs.length, null));
		if (recipe instanceof CraftingRecipe && menu instanceof CraftingMenu) {
			var info = recipe.placementInfo();
			if (info.isImpossibleToPlace()) return null;
			int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
			for (int index = 0; index < info.slotsToIngredientIndex().size(); index++) {
				int ingredient = info.slotsToIngredientIndex().getInt(index);
				int slot = index / width * 3 + index % width;
				if (ingredient >= 0 && slot < 9) ingredients.set(slot, info.ingredients().get(ingredient));
			}
		} else if (recipe instanceof StonecutterRecipe stone && menu instanceof StonecutterMenu) ingredients.set(0, stone.input());
		else if (recipe instanceof SmithingRecipe smith && menu instanceof SmithingMenu) {
			smith.templateIngredient().ifPresent(value -> ingredients.set(0, value));
			ingredients.set(1, smith.baseIngredient());
			smith.additionIngredient().ifPresent(value -> ingredients.set(2, value));
		} else return null;
		ItemStack icon = recipe.display().stream().findFirst().map(display -> display.result().resolveForFirstStack(
			SlotDisplayContext.fromLevel(player.level()))).orElse(ItemStack.EMPTY);
		if (icon.isEmpty()) icon = new ItemStack(Items.CRAFTING_TABLE);
		return new Choice(icon.getHoverName().getString(), icon, ingredients, -1, ItemStack.EMPTY, holder, -1);
	}
	private void sendChoices() {
		if (!valid()) return;
		List<Choice> result = new ArrayList<>();
		List<ItemStack> available = available();
		for (var holder : player.level().getServer().getRecipeManager().getRecipes()) {
			Choice choice = recipeChoice(holder);
			if (choice == null) continue;
			if (menu instanceof SmithingMenu) {
				for (ItemStack target : distinct(available, choice.ingredients().get(1)))
					result.add(new Choice(choice.title() + ": " + target.getHoverName().getString(), target,
						choice.ingredients(), 1, target.copyWithCount(1), holder, -1));
			} else result.add(choice);
		}
		if (menu instanceof CartographyTableMenu) {
			for (var target : distinct(available, stack -> stack.is(Items.FILLED_MAP))) {
				var items = List.of(Items.PAPER, Items.MAP, Items.GLASS_PANE);
				var names = List.of("Expand", "Copy", "Lock");
				for (int i = 0; i < items.size(); i++) result.add(new Choice(names.get(i) + ": " + target.getHoverName().getString(), target,
					List.of(stack -> stack.is(Items.FILLED_MAP), Ingredient.of(items.get(i))), 0, target.copyWithCount(1), null, i));
			}
		}
		if (menu instanceof LoomMenu) {
			var registry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN);
			var patterns = registry.getOrThrow(net.minecraft.tags.BannerPatternTags.NO_ITEM_REQUIRED);
			List<ItemStack> dyes = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
				.filter(item -> item instanceof net.minecraft.world.item.DyeItem).map(ItemStack::new).toList();
			List<ItemStack> patternItems = distinct(available, stack -> stack.has(DataComponents.PROVIDES_BANNER_PATTERNS));
			patternItems.add(ItemStack.EMPTY);
			for (ItemStack target : distinct(available, stack -> stack.getItem() instanceof net.minecraft.world.item.BannerItem)) {
				for (ItemStack patternItem : patternItems) {
					var tag = patternItem.get(DataComponents.PROVIDES_BANNER_PATTERNS);
					var options = tag == null ? patterns : tag;
					for (int i = 0; i < options.size(); i++) for (ItemStack dye : dyes) {
						List<Predicate<ItemStack>> ingredients = new ArrayList<>();
						ingredients.add(stack -> stack.getItem() instanceof net.minecraft.world.item.BannerItem);
						ingredients.add(Ingredient.of(dye.getItem()));
						ingredients.add(patternItem.isEmpty() ? null : Ingredient.of(patternItem.getItem()));
						String name = options.get(i).value().assetId().getPath();
						ItemStack preview = target.copyWithCount(1);
						preview.set(DataComponents.BANNER_PATTERNS, new net.minecraft.world.level.block.entity.BannerPatternLayers.Builder()
							.addAll(target.getOrDefault(DataComponents.BANNER_PATTERNS, net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY))
							.add(options.get(i), dye.get(DataComponents.DYE)).build());
						result.add(new Choice(name + " / " + dye.getHoverName().getString() + ": " + target.getHoverName().getString(), preview,
							ingredients, 0, target.copyWithCount(1), null, i));
					}
				}
			}
		}
		result.sort(Comparator.comparing(Choice::title));
		var stable = new ArrayList<>(choices);
		for (Choice choice : result) if (stable.stream().noneMatch(old -> old.title().equals(choice.title())
			&& old.operation() == choice.operation() && ItemStack.isSameItemSameComponents(old.target(), choice.target())
			&& Objects.equals(old.recipe() == null ? null : old.recipe().id(), choice.recipe() == null ? null : choice.recipe().id()))) stable.add(choice);
		choices = List.copyOf(stable);
		CabinStationPayload.send(player, menu.containerId, choices.stream().map(choice ->
			new CabinStationPayload.Entry(choice.title(), choice.icon(), canFill(choice), choice == selected)).toList());
	}
	private boolean canFill(Choice choice) {
		var candidates = available().stream().map(ItemStack::copy).toList();
		for (int i = 0; i < choice.ingredients().size(); i++) {
			var ingredient = choice.ingredients().get(i);
			if (ingredient == null) continue;
			boolean explicit = i == choice.targetSlot();
			var stack = candidates.stream().filter(value -> ingredient.test(value) && (explicit
				? ItemStack.isSameItemSameComponents(value, choice.target()) : ordinary(value))).findFirst().orElse(ItemStack.EMPTY);
			if (stack.isEmpty()) return false;
			stack.shrink(1);
		}
		return true;
	}

	private static List<ItemStack> distinct(List<ItemStack> items, Predicate<ItemStack> filter) {
		var result = new ArrayList<ItemStack>();
		for (ItemStack stack : items) if (filter.test(stack) && result.stream().noneMatch(s -> ItemStack.isSameItemSameComponents(s, stack)))
			result.add(stack.copyWithCount(1));
		return result;
	}
}
