package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** The last catalogue gaps: rum, kirsch, queso fresco, dried fruit, and the dishes they complete. */
public final class CabinCatalogueGapGameTest {
	static final String[] RECIPES = {
		"pot/queso_fresco_curds",
		"cheese_press/queso_fresco",
		"cutting/halved_stone_fruit",
		"drying/dried_fruit",
		"pot/vanilla_milk",
		"mixing_bowl/canele_batter",
		"ageing/rested_canele_batter",
		"oven/caneles",
		"cutting/split_chocolate_sponge",
		"counter/schwarzwalder_kirschtorte",
		"pot/dipped_tortillas",
		"counter/rolled_enchiladas",
		"oven/enchiladas_verdes",
		"counter/raw_sopes",
		"skillet/sope_shells",
		"counter/sopes",
		"counter/raw_tlacoyos",
		"skillet/griddled_tlacoyos",
		"counter/tlacoyos",
		"mixing_bowl/pambazo_filling",
		"skillet/fried_pambazo_filling",
		"pot/dipped_bread_roll",
		"skillet/fried_bread_roll",
		"counter/pambazos",
		"pot/mazamorra_morada",
		"mixing_bowl/achiote_potato_mash",
		"counter/raw_llapingachos",
		"skillet/fried_llapingachos",
		"counter/llapingachos",
		"cutting/scored_pork_loin",
		"oven/flaeskesteg",
		"pot/rodkal",
		"pot/brun_sovs",
		"counter/flaeskesteg_med_rodkal"
	};

	@GameTest
	public void everyGapRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void quesoFrescoCurdlesWithEitherRennetAndSpiritsAreSold(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String rennet : new String[] {"vegetable_rennet", "calf_rennet"}) {
			var curds = CookingStation.POT.find(level, CookingInput.of(java.util.List.of(new ItemStack(net.minecraft.world.item.Items.MILK_BUCKET),
				new ItemStack(CookingPlants.item(rennet)), new ItemStack(CookingPlants.item("salt"))))).orElse(null);
			helper.assertTrue(curds != null && curds.value().result().create().is(CookingPlants.item("queso_fresco_curds")), rennet + " curdles queso fresco");
		}
		var trades = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_TRADE);
		for (String spirit : new String[] {"rum", "kirsch"}) {
			helper.assertTrue(trades.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_TRADE,
				PortablePocketCabin.id("wandering_trader/emerald_" + spirit))).isPresent(), "Wandering traders sell " + spirit);
		}
		helper.succeed();
	}
}
