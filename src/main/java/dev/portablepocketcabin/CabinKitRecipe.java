package dev.portablepocketcabin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

final class CabinKitRecipe implements CraftingRecipe {
	CabinKitRecipe() {
	}

	private static PlacementInfo currentPlacementInfo() {
		List<Ingredient> ingredients = List.of(
			woodIngredient(CabinMaterialProfiles.plankIngredients()),
			woodIngredient(CabinMaterialProfiles.plankIngredients()),
			woodIngredient(CabinMaterialProfiles.plankIngredients()),
			woodIngredient(CabinMaterialProfiles.structuralIngredients()),
			woodIngredient(CabinMaterialProfiles.doorIngredients()),
			woodIngredient(CabinMaterialProfiles.structuralIngredients()),
			woodIngredient(CabinMaterialProfiles.plankIngredients()),
			Ingredient.of(CabinItems.DIMENSIONAL_FOUNDATION),
			woodIngredient(CabinMaterialProfiles.plankIngredients())
		);
		return PlacementInfo.create(ingredients);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return resolve(input) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		CabinPalette palette = resolve(input);
		return palette == null ? ItemStack.EMPTY : CabinItems.createUnbound(palette);
	}

	private CabinPalette resolve(CraftingInput input) {
		if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 9) {
			return null;
		}
		var roof = CabinMaterialProfiles.matchPlanks(input.getItem(0, 0)).orElse(null);
		if (roof == null || !same(roof, CabinMaterialProfiles.matchPlanks(input.getItem(1, 0)).orElse(null))
			|| !same(roof, CabinMaterialProfiles.matchPlanks(input.getItem(2, 0)).orElse(null))) {
			return null;
		}
		var walls = CabinMaterialProfiles.matchStructuralWood(input.getItem(0, 1)).orElse(null);
		if (walls == null || !same(walls,
			CabinMaterialProfiles.matchStructuralWood(input.getItem(2, 1)).orElse(null))) {
			return null;
		}
		var door = CabinMaterialProfiles.matchDoor(input.getItem(1, 1)).orElse(null);
		if (door == null) {
			return null;
		}
		var floor = CabinMaterialProfiles.matchPlanks(input.getItem(0, 2)).orElse(null);
		if (floor == null || !same(floor, CabinMaterialProfiles.matchPlanks(input.getItem(2, 2)).orElse(null))
			|| !input.getItem(1, 2).is(CabinItems.DIMENSIONAL_FOUNDATION)) {
			return null;
		}
		return new CabinPalette(floor, walls, roof, door);
	}

	private static boolean same(CabinPalette.WoodSelection first, CabinPalette.WoodSelection second) {
		return first != null && first.profileId().equals(second.profileId());
	}

	private static Ingredient woodIngredient(List<Item> items) {
		return Ingredient.of(items.stream());
	}

	@Override
	public boolean isSpecial() {
		return false;
	}

	@Override
	public boolean showNotification() {
		return true;
	}

	@Override
	public String group() {
		return "portable_pocket_cabin";
	}

	@Override
	public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
		return CabinRecipes.CABIN_KIT_SERIALIZER;
	}

	@Override
	public CraftingBookCategory category() {
		return CraftingBookCategory.MISC;
	}

	@Override
	public PlacementInfo placementInfo() {
		return currentPlacementInfo();
	}

	@Override
	public List<RecipeDisplay> display() {
		List<SlotDisplay> slots = new ArrayList<>();
		slots.add(composite(CabinMaterialProfiles.plankIngredients()));
		slots.add(composite(CabinMaterialProfiles.plankIngredients()));
		slots.add(composite(CabinMaterialProfiles.plankIngredients()));
		slots.add(composite(CabinMaterialProfiles.structuralIngredients()));
		slots.add(composite(CabinMaterialProfiles.doorIngredients()));
		slots.add(composite(CabinMaterialProfiles.structuralIngredients()));
		slots.add(composite(CabinMaterialProfiles.plankIngredients()));
		slots.add(new SlotDisplay.ItemSlotDisplay(CabinItems.DIMENSIONAL_FOUNDATION));
		slots.add(composite(CabinMaterialProfiles.plankIngredients()));
		return List.of(new ShapedCraftingRecipeDisplay(
			3, 3, slots,
			new SlotDisplay.ItemSlotDisplay(CabinItems.PACKED_CABIN),
			new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
		));
	}

	private static SlotDisplay composite(List<Item> items) {
		return new SlotDisplay.Composite(items.stream().map(SlotDisplay.ItemSlotDisplay::new)
			.map(value -> (SlotDisplay) value).toList());
	}
}
