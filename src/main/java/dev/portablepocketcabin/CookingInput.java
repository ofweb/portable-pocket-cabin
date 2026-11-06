package dev.portablepocketcabin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** A station's ingredient slots, plus the pot's container slot and water. */
record CookingInput(List<ItemStack> ingredients, ItemStack container, int water) implements RecipeInput {
	CookingInput {
		ingredients = List.copyOf(ingredients);
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
