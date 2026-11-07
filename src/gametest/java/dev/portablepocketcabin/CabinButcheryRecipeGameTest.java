package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** B-0049 delivery 1: the recipes butchery completes. */
public final class CabinButcheryRecipeGameTest {
	static final String[] RECIPES = {
		"mill/bone_meal_from_bones",
		"pot/unstrained_lard",
		"pot/unstrained_tallow",
		"pot/unstrained_tallow_from_lamb_fat",
		"pot/unstrained_tallow_from_goat_fat",
		"pot/unstrained_schmaltz",
		"strainer/lard",
		"strainer/tallow",
		"strainer/schmaltz",
		"pot/gelatin_from_pig_trotters",
		"pot/gelatin_from_chicken_feet",
		"pot/gelatin_from_pork_skin",
		"crock/calf_rennet",
		"oven/roast_chicken",
		"pot/boiled_potatoes",
		"oven/roasted_beef_bones",
		"pot/unstrained_beef_stock",
		"pot/unstrained_chicken_stock",
		"pot/unstrained_pork_stock",
		"pot/unstrained_pork_bone_broth",
		"strainer/beef_stock",
		"strainer/chicken_stock",
		"strainer/pork_stock",
		"strainer/pork_bone_broth",
		"brine/cured_pork_belly",
		"brine/cured_pork_shoulder",
		"pot/ham",
		"brine/salted_beef_round",
		"drying/carne_seca",
		"cutting/diced_beef",
		"cutting/thin_beef_slices",
		"cutting/sliced_pork_belly",
		"pot/poached_chicken_thighs",
		"cutting/poached_chicken",
		"mixing_bowl/raw_pino",
		"skillet/pino",
		"mixing_bowl/fresh_empanada_dough",
		"ageing/empanada_dough",
		"mixing_bowl/red_chili_blend",
		"pot/red_chili_sauce",
		"pot/cooked_black_beans",
		"mixing_bowl/black_bean_mash",
		"skillet/refried_beans",
		"mixing_bowl/caldillo_base",
		"pot/tomato_caldillo",
		"skillet/crisp_bacon",
		"skillet/browned_beef",
		"mixing_bowl/garniture",
		"pot/onion_soup",
		"oven/french_onion_soup",
		"oven/blind_baked_shell",
		"mixing_bowl/quiche_custard",
		"oven/quiche_lorraine",
		"pot/boeuf_bourguignon",
		"pot/coq_au_vin",
		"counter/unbaked_croque_monsieur",
		"oven/croque_monsieur",
		"counter/raw_rinderrouladen",
		"skillet/seared_rinderrouladen",
		"pot/rinderrouladen",
		"mixing_bowl/sauerbraten_marinade",
		"brine/marinated_beef_round",
		"skillet/seared_marinated_beef",
		"pot/sauerbraten",
		"counter/unbaked_flammkuchen",
		"oven/flammkuchen",
		"skillet/crisp_pork_belly",
		"pot/persillesovs",
		"counter/stegt_flaesk_med_persillesovs",
		"pot/creamed_chicken_sauce",
		"pot/hons_i_asparges",
		"counter/tarteletter_med_hons_i_asparges",
		"pot/massaman_sauce",
		"pot/massaman_curry",
		"skillet/charred_poblano",
		"counter/stuffed_poblano",
		"mixing_bowl/relleno_batter",
		"pot/fried_chile_relleno",
		"counter/chiles_rellenos",
		"pot/carnitas_pork",
		"skillet/crisp_carnitas",
		"counter/carnitas_tacos",
		"pot/chili_pork",
		"mixing_bowl/tamal_masa",
		"counter/raw_tamales",
		"pot/tamales",
		"pot/pozole_base",
		"counter/pozole_rojo",
		"brine/marinated_al_pastor",
		"oven/roasted_al_pastor",
		"cutting/al_pastor_meat",
		"counter/tacos_al_pastor",
		"pot/mole_chicken",
		"counter/mole_poblano",
		"mixing_bowl/concha_topping",
		"counter/unbaked_conchas",
		"oven/conchas",
		"counter/raw_empanadas",
		"oven/empanadas_de_carne",
		"mill/corn_paste",
		"pot/pastelera",
		"oven/pastel_de_choclo",
		"pot/coxinha_dough",
		"counter/raw_coxinhas",
		"pot/coxinhas",
		"brine/marinated_beef_heart",
		"counter/raw_anticucho_skewer",
		"grill/anticucho_skewer",
		"counter/anticuchos",
		"skillet/seared_chevon",
		"pot/scalded_pig_carcass",
		"pot/scalded_chicken_carcass"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void fatsFryAndRennetCurdles(GameTestHelper helper) {
		for (String fat : new String[] {"lard", "tallow", "schmaltz"}) {
			helper.assertTrue(new ItemStack(CookingPlants.item(fat)).is(CookingItems.COOKING_OILS), fat + " is a frying oil");
		}
		helper.assertTrue(new ItemStack(CookingPlants.item("calf_rennet")).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
			PortablePocketCabin.id("rennets"))), "Calf rennet curdles milk like vegetable rennet");
		var level = helper.getLevel();
		var meal = CookingStation.MILL.find(level, CookingInput.of(java.util.List.of(new ItemStack(Butchery.PARTS.get("beef_bones"))))).orElse(null);
		helper.assertTrue(meal != null && meal.value().result().create().is(net.minecraft.world.item.Items.BONE_MEAL), "The mill grinds bones into bone meal");
		helper.succeed();
	}
}
