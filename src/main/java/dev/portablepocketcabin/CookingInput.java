package dev.portablepocketcabin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * A station's ingredient slots, plus the container slot and the liquid levels of a pot or wok, and whether a drying
 * rack has open sky above it.
 */
record CookingInput(List<ItemStack> ingredients, ItemStack container, int water, int oil, boolean sky) implements RecipeInput {
	/** The water or oil a pot or wok holds. A station holds one liquid at a time. */
	record Liquid(int water, int oil) {
		static final Liquid NONE = new Liquid(0, 0);
	}

	CookingInput {
		ingredients = List.copyOf(ingredients);
	}

	CookingInput(List<ItemStack> ingredients, ItemStack container, int water, int oil) {
		this(ingredients, container, water, oil, true);
	}

	CookingInput(List<ItemStack> ingredients, ItemStack container, int water) {
		this(ingredients, container, water, 0);
	}

	static CookingInput of(List<ItemStack> ingredients) {
		return new CookingInput(ingredients, ItemStack.EMPTY, 0);
	}

	@Override
	public ItemStack getItem(int index) {
		return ingredients.get(index);
	}

	@Override
	public int size() {
		return ingredients.size();
	}
}
