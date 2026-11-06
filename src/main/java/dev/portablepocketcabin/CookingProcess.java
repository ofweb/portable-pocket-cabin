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
	private final int byproductSlot;
	private RecipeHolder<CookingRecipe> recipe;
	private boolean dirty = true;
	private boolean loaded;
	private int progress;

	CookingProcess(CookingStation station, int containerSlot, int outputSlot, int byproductSlot) {
		this.station = station;
		this.inputSlots = station.ingredientSlots();
		this.containerSlot = containerSlot;
		this.outputSlot = outputSlot;
		this.byproductSlot = byproductSlot;
	}

	void markDirty() {
		dirty = true;
	}

	int progress() {
		return progress;
	}

	Optional<CookingRecipe> recipe(ServerLevel level, Container container, int water) {
		if (dirty) {
			var found = station.find(level, input(container, water)).orElse(null);
			boolean same = found != null && (loaded || recipe != null && found.id().equals(recipe.id()));
			if (!same) progress = 0;
			recipe = found;
			dirty = false;
			loaded = false;
		}
		return Optional.ofNullable(recipe).map(RecipeHolder::value);
	}

	/** Whether the station has a recipe that heat would advance. */
	boolean wantsHeat(ServerLevel level, Container container, int water) {
		return recipe(level, container, water).filter(r -> canOutput(container, r)).isPresent();
	}

	/** Advances cooking for one tick. Returns the water used when a recipe completes, or zero. */
	int tick(ServerLevel level, Container container, int water, Optional<CookingHeat> heat) {
		CookingRecipe current = recipe(level, container, water).orElse(null);
		if (current == null || !canOutput(container, current) || !heat.equals(current.heat())) return 0;
		if (++progress < current.time()) return 0;
		CookingInput input = input(container, water);
		int[] assignment = current.assign(input).orElse(null);
		progress = 0;
		dirty = true;
		if (assignment == null) return 0;
		int[] taken = current.consumption(input, assignment);
		for (int slot = 0; slot < inputSlots; slot++) container.getItem(slot).shrink(taken[slot]);
		current.container().ifPresent(needed -> container.getItem(containerSlot).shrink(needed.count()));
		insert(container, outputSlot, current.result());
		for (ItemStackTemplate remainder : current.remainders()) insert(container, byproductSlot, remainder);
		container.setChanged();
		return current.water();
	}

	/** The recipe's heat, for the screen's status line. */
	Optional<CookingHeat> neededHeat() {
		return Optional.ofNullable(recipe).flatMap(holder -> holder.value().heat());
	}

	int time() {
		return recipe == null ? 0 : recipe.value().time();
	}

	private CookingInput input(Container container, int water) {
		List<ItemStack> ingredients = new ArrayList<>(inputSlots);
		for (int slot = 0; slot < inputSlots; slot++) ingredients.add(container.getItem(slot));
		ItemStack held = containerSlot < 0 ? ItemStack.EMPTY : container.getItem(containerSlot);
		return new CookingInput(ingredients, held, water);
	}

	private boolean canOutput(Container container, CookingRecipe recipe) {
		if (!fits(container.getItem(outputSlot), recipe.result())) return false;
		ItemStack byproduct = container.getItem(byproductSlot).copy();
		for (ItemStackTemplate remainder : recipe.remainders()) {
			if (!fits(byproduct, remainder)) return false;
			if (byproduct.isEmpty()) byproduct = remainder.create();
			else byproduct.grow(remainder.count());
		}
		return true;
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
	}

	void load(ValueInput input) {
		progress = input.getIntOr("progress", 0);
		dirty = true;
		loaded = true;
	}
}
