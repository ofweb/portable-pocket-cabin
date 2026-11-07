package dev.portablepocketcabin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cooking in a container station. The recipe is looked up only when the inputs change. Progress advances
 * only at the recipe's heat and holds otherwise; a full output pauses it, and a different recipe resets it.
 */
final class CookingProcess {
	private final CookingStation station;
	private final int inputSlots;
	private final int containerSlot;
	private final int outputSlot;
	private final int[] byproductSlots;
	private int[] seasoningSlots = new int[0];
	private RecipeHolder<CookingRecipe> recipe;
	private boolean dirty = true;
	private int generation;
	private CookingStation.Lookup lookup;
	private boolean loaded;
	private String savedRecipeId;
	private int progress;

	CookingProcess(CookingStation station, int containerSlot, int outputSlot, int... byproductSlots) {
		this.station = station;
		this.inputSlots = station.ingredientSlots();
		this.containerSlot = containerSlot;
		this.outputSlot = outputSlot;
		this.byproductSlots = byproductSlots.clone();
		this.lookup = station::find;
	}

	/** Sets the slots whose seasonings a finished dish uses for its quality. */
	CookingProcess seasoning(int... slots) {
		this.seasoningSlots = slots.clone();
		return this;
	}

	/** Replaces the recipe lookup, for GameTests. */
	void lookup(CookingStation.Lookup lookup) {
		this.lookup = lookup;
		dirty = true;
	}

	void markDirty() {
		dirty = true;
	}

	int progress() {
		return progress;
	}

	Optional<CookingRecipe> recipe(ServerLevel level, Container container, CookingInput.Liquid liquid) {
		if (dirty || generation != CookingStation.recipeGeneration()) {
			var found = lookup.find(level, input(container, liquid)).orElse(null);
			boolean same = found != null && (loaded
				? savedRecipeId == null || savedRecipeId.equals(found.id().identifier().toString())
				: recipe != null && found.id().equals(recipe.id()));
			if (!same) progress = 0;
			recipe = found;
			dirty = false;
			generation = CookingStation.recipeGeneration();
			loaded = false;
			savedRecipeId = null;
		}
		return Optional.ofNullable(recipe).map(RecipeHolder::value);
	}

	/** Whether the station has a recipe whose output fits, so that heat, time, or a tool use would advance it. */
	boolean ready(ServerLevel level, Container container, CookingInput.Liquid liquid) {
		return recipe(level, container, liquid).filter(r -> canOutput(container, r)).isPresent();
	}

	/** Whether the station has a recipe that heat would advance. */
	boolean wantsHeat(ServerLevel level, Container container, CookingInput.Liquid liquid) {
		return ready(level, container, liquid);
	}

	/**
	 * Advances cooking for one tick. Stations without heat pass no heat and progress on recipes that need none.
	 * Returns the water or oil used when a recipe completes, or zero.
	 */
	int tick(ServerLevel level, Container container, CookingInput.Liquid liquid, Optional<CookingHeat> heat) {
		return tick(level, container, liquid, heat, 1);
	}

	/**
	 * Advances cooking by {@code elapsed} ticks, or tool uses at a worked station. A time station passes the game time
	 * since its last tick, so it catches up after its chunk was unloaded. At most the current batch finishes: the rest
	 * of the elapsed time is dropped, so catch-up never starts another batch (ADR-0013).
	 */
	int tick(ServerLevel level, Container container, CookingInput.Liquid liquid, Optional<CookingHeat> heat, long elapsed) {
		if (elapsed <= 0) return 0;
		CookingRecipe current = recipe(level, container, liquid).orElse(null);
		if (current == null || !canOutput(container, current) || !heat.equals(current.heat())) return 0;
		progress = (int) Math.min(current.time(), progress + elapsed);
		if (progress < current.time()) return 0;
		CookingInput input = input(container, liquid);
		int[] assignment = current.assign(input).orElse(null);
		progress = 0;
		dirty = true;
		if (assignment == null) return 0;
		int[] taken = current.consumption(input, assignment);
		for (int slot = 0; slot < inputSlots; slot++) container.getItem(slot).shrink(taken[slot]);
		current.container().ifPresent(needed -> container.getItem(containerSlot).shrink(needed.count()));
		ItemStack made = made(container, current);
		List<ItemStackTemplate> leftovers = leftovers(container, current);
		if (!current.tastes().isEmpty()) for (int slot : seasoningSlots) container.getItem(slot).shrink(1);
		if (container.getItem(outputSlot).isEmpty()) container.setItem(outputSlot, made);
		else container.getItem(outputSlot).grow(made.getCount());
		for (ItemStackTemplate remainder : leftovers) {
			int slot = byproductSlotFor(container, remainder);
			if (slot >= 0) insert(container, slot, remainder);
		}
		container.setChanged();
		return current.water() + current.oil();
	}

	/** The recipe's heat, for the screen's status line. */
	Optional<CookingHeat> neededHeat() {
		return Optional.ofNullable(recipe).flatMap(holder -> holder.value().heat());
	}

	int time() {
		return recipe == null ? 0 : recipe.value().time();
	}

	private CookingInput input(Container container, CookingInput.Liquid liquid) {
		List<ItemStack> ingredients = new ArrayList<>(inputSlots);
		for (int slot = 0; slot < inputSlots; slot++) ingredients.add(container.getItem(slot));
		ItemStack held = containerSlot < 0 ? ItemStack.EMPTY : container.getItem(containerSlot);
		return new CookingInput(ingredients, held, liquid.water(), liquid.oil(), true);
	}

	/** The dish a batch makes: the recipe's result, with the quality its seasonings give when it lists tastes. */
	private ItemStack made(Container container, CookingRecipe recipe) {
		ItemStack made = recipe.result().create();
		if (recipe.tastes().isEmpty() || seasoningSlots.length == 0) return made;
		return Seasoning.apply(made, Seasoning.quality(recipe.tastes(), Seasoning.used(container, seasoningSlots)));
	}

	/** The recipe's remainders, plus what used seasonings leave behind, such as the bottle of a splash of vinegar. */
	private List<ItemStackTemplate> leftovers(Container container, CookingRecipe recipe) {
		if (recipe.tastes().isEmpty() || seasoningSlots.length == 0) return recipe.remainders();
		List<ItemStackTemplate> leftovers = new ArrayList<>(recipe.remainders());
		for (ItemStack seasoning : Seasoning.used(container, seasoningSlots)) {
			ItemStackTemplate remainder = seasoning.getItem().getCraftingRemainder();
			if (remainder != null) leftovers.add(remainder);
		}
		return leftovers;
	}

	/** Whether the result and every remainder fit. Remainders fill the byproduct slots in order, one kind of item per slot. */
	private boolean canOutput(Container container, CookingRecipe recipe) {
		ItemStack output = container.getItem(outputSlot);
		ItemStack made = made(container, recipe);
		if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, made) || output.getCount() + made.getCount() > output.getMaxStackSize())) {
			return false;
		}
		ItemStack[] byproducts = new ItemStack[byproductSlots.length];
		for (int i = 0; i < byproducts.length; i++) byproducts[i] = container.getItem(byproductSlots[i]).copy();
		for (ItemStackTemplate remainder : leftovers(container, recipe)) {
			int chosen = choose(byproducts, remainder);
			if (chosen < 0) return false;
			if (byproducts[chosen].isEmpty()) byproducts[chosen] = remainder.create();
			else byproducts[chosen].grow(remainder.count());
		}
		return true;
	}

	private int byproductSlotFor(Container container, ItemStackTemplate remainder) {
		ItemStack[] byproducts = new ItemStack[byproductSlots.length];
		for (int i = 0; i < byproducts.length; i++) byproducts[i] = container.getItem(byproductSlots[i]);
		int chosen = choose(byproducts, remainder);
		return chosen < 0 ? -1 : byproductSlots[chosen];
	}

	/** The byproduct slot a remainder goes to: one already holding the same item, otherwise the first empty one. */
	private static int choose(ItemStack[] byproducts, ItemStackTemplate remainder) {
		for (int i = 0; i < byproducts.length; i++) {
			if (!byproducts[i].isEmpty() && fits(byproducts[i], remainder)) return i;
		}
		for (int i = 0; i < byproducts.length; i++) if (byproducts[i].isEmpty()) return i;
		return -1;
	}

	private static boolean fits(ItemStack slot, ItemStackTemplate output) {
		if (slot.isEmpty()) return true;
		ItemStack made = output.create();
		return ItemStack.isSameItemSameComponents(slot, made) && slot.getCount() + made.getCount() <= slot.getMaxStackSize();
	}

	private static void insert(Container container, int slot, ItemStackTemplate output) {
		ItemStack current = container.getItem(slot);
		if (current.isEmpty()) container.setItem(slot, output.create());
		else current.grow(output.count());
	}

	void save(ValueOutput output) {
		output.putInt("progress", progress);
		String id = loaded ? savedRecipeId : recipe == null ? null : recipe.id().identifier().toString();
		if (id != null) output.putString("recipe", id);
	}

	void load(ValueInput input) {
		progress = input.getIntOr("progress", 0);
		savedRecipeId = input.getString("recipe").orElse(null);
		recipe = null;
		dirty = true;
		loaded = true;
	}
}
