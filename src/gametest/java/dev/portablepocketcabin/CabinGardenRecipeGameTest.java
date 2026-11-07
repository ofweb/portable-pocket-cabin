package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static dev.portablepocketcabin.CabinDairyGameTest.pass;
import static dev.portablepocketcabin.CabinDairyGameTest.station;

/** B-0050 delivery 1: the recipe chains the plains, meadow, and forest plants complete. */
public final class CabinGardenRecipeGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);
	/** Every recipe delivery 1 adds. Each must be found by its own station from exactly its inputs. */
	static final String[] RECIPES = {
		"mill/rye_flour",
		"mortar/cracked_rye",
		"crock/soaked_corn",
		"mill/wet_cornstarch",
		"drying/cornstarch",
		"cutting/corn_husk",
		"drying/dried_corn_husks",
		"drying/dried_corn",
		"oven/wood_ash",
		"strainer/lye_solution",
		"pot/hominy",
		"mill/masa",
		"pot/cooked_corn",
		"drying/dried_cooked_corn",
		"mill/masarepa",
		"counter/raw_tortillas",
		"skillet/corn_tortilla",
		"skillet/toasted_corn",
		"oven/roasted_barley",
		"crock/sprouted_barley",
		"drying/malted_barley",
		"oven/roasted_malt",
		"mixing_bowl/rugbrod_dough",
		"proofing/proofed_rugbrod_dough",
		"oven/rugbrod",
		"mixing_bowl/spatzle_dough",
		"pot/spatzle",
		"mill/ground_mustard",
		"mixing_bowl/prepared_mustard",
		"mixing_bowl/mayonnaise",
		"drying/dried_white_beans",
		"cutting/shredded_cabbage",
		"crock/sauerkraut",
		"crock/gherkins",
		"mill/crushed_white_grapes",
		"press/white_grape_must",
		"crock/white_wine",
		"crock/wine_vinegar_first",
		"crock/wine_vinegar",
		"pot/wort",
		"crock/beer",
		"pot/strawberry_jam",
		"pot/raspberry_jam",
		"drying/dried_rosehip",
		"cutting/chopped_onion",
		"cutting/grated_potato",
		"mixing_bowl/potato_batter",
		"skillet/kartoffelpuffer",
		"counter/seasoned_kartoffelpuffer",
		"skillet/caramelised_onion",
		"oven/kasespatzle",
		"mill/ground_malt",
		"pot/malzkaffee",
		"pot/mugicha"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		var level = helper.getLevel();
		var manager = level.getServer().getRecipeManager();
		for (String path : RECIPES) {
			var id = PortablePocketCabin.id("cooking/" + path);
			var holder = manager.byKey(ResourceKey.create(Registries.RECIPE, id)).orElse(null);
			helper.assertTrue(holder != null && holder.value() instanceof CookingRecipe, "Recipe loads: " + id);
			var recipe = (CookingRecipe) holder.value();
			List<ItemStack> slots = new ArrayList<>();
			for (var entry : recipe.ingredients()) {
				var stack = sample(entry.ingredient()).copyWithCount(1);
				int count = entry.count();
				while (count > 0) {
					int now = Math.min(count, stack.getMaxStackSize());
					slots.add(stack.copyWithCount(now));
					count -= now;
				}
			}
			var container = recipe.container().map(entry -> sample(entry.ingredient()).copyWithCount(entry.count())).orElse(ItemStack.EMPTY);
			var input = new CookingInput(slots, container, recipe.water(), recipe.oil(), recipe.sun().orElse(true));
			var found = recipe.station().find(level, input);
			helper.assertTrue(found.isPresent() && found.get().id().identifier().equals(id),
				"Its station finds " + id + " from its inputs, found " + found.map(f -> f.id().identifier().toString()).orElse("nothing"));
		}
		helper.succeed();
	}

	@GameTest
	public void whiteGrapesBecomeWineThenVinegar(GameTestHelper helper) {
		var level = helper.getLevel();
		var mill = station(helper, BASE, CookingBlocks.MILL);
		mill.setItem(0, new ItemStack(CookingPlants.WHITE_GRAPE, 4));
		for (int i = 0; i < 100; i++) ProcessStationBlockEntity.serverTick(level, mill.getBlockPos(), mill.getBlockState(), mill);
		var press = station(helper, BASE.east(2), CookingBlocks.PRESS);
		press.setItem(0, mill.getItem(CookingMenu.Kind.MILL.outputSlot).copy());
		press.setItem(CookingMenu.Kind.PRESS.containerSlot, new ItemStack(Items.GLASS_BOTTLE));
		for (int i = 0; i < 60; i++) ProcessStationBlockEntity.serverTick(level, press.getBlockPos(), press.getBlockState(), press);
		var must = press.getItem(CookingMenu.Kind.PRESS.outputSlot);
		helper.assertTrue(must.is(CookingPlants.item("white_grape_must")), "Crushed grapes press into must");
		var crock = station(helper, BASE.south(2), CookingBlocks.CROCK);
		crock.setItem(0, must.copyWithCount(4));
		pass(helper, crock, 3 * CookingStation.DAY);
		helper.assertTrue(crock.getItem(CookingMenu.Kind.CROCK.outputSlot).is(CookingPlants.item("white_wine")), "Must ferments into white wine");
		crock.setItem(0, crock.removeItem(CookingMenu.Kind.CROCK.outputSlot, 4));
		pass(helper, crock, 5 * CookingStation.DAY);
		helper.assertTrue(crock.getItem(CookingMenu.Kind.CROCK.outputSlot).is(CookingPlants.item("wine_vinegar")), "Wine left open sours into vinegar");
		helper.succeed();
	}

	@GameTest
	public void cornBecomesTortillas(GameTestHelper helper) {
		var level = helper.getLevel();
		var counter = station(helper, BASE, CookingBlocks.PREP_COUNTER);
		counter.setItem(0, new ItemStack(CookingPlants.item("masa")));
		for (int i = 0; i < 40; i++) ProcessStationBlockEntity.serverTick(level, counter.getBlockPos(), counter.getBlockState(), counter);
		helper.assertTrue(counter.getItem(CookingMenu.Kind.COUNTER.outputSlot).is(CookingPlants.item("raw_tortilla")), "Masa is pressed into tortillas");
		var tortilla = CookingStation.SKILLET.find(level, CookingInput.of(List.of(new ItemStack(CookingPlants.item("raw_tortilla"))))).orElseThrow();
		helper.assertTrue(tortilla.value().result().item().value() == CookingPlants.item("corn_tortilla"), "Tortillas cook on the skillet");
		var husk = CookingStation.CUTTING.find(level, CookingInput.of(List.of(new ItemStack(CookingPlants.CORN)))).orElseThrow();
		helper.assertTrue(husk.value().remainders().getFirst().item().value() == CookingPlants.item("corn_kernels"), "Husking corn keeps the kernels");
		helper.succeed();
	}

	/** A stack that the ingredient accepts: its first item, or a water bottle for the water ingredient. */
	static ItemStack sample(net.minecraft.world.item.crafting.Ingredient ingredient) {
		var water = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
		if (ingredient.test(water)) return water;
		return ingredient.items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
	}
}
