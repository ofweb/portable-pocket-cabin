package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Koji: the first spores from wandering traders, spores kept from every batch, and what koji makes. */
public final class CabinKojiGameTest {
	static final String[] RECIPES = {
		"crock/koji_rice",
		"pot/cooked_soybeans",
		"mortar/soybean_paste",
		"crock/miso",
		"skillet/roasted_wheat",
		"mixing_bowl/shoyu_koji",
		"crock/moromi",
		"press/soy_sauce",
		"crock/sake_mash",
		"press/sake",
		"crock/rice_vinegar",
		"crock/mirin_mash",
		"press/mirin",
		"mixing_bowl/sushi_rice",
		"pot/oyster_sauce",
		"mixing_bowl/marinating_eggs",
		"ageing/ajitsuke_tamago",
		"counter/rolled_pork_belly",
		"pot/chashu_pork",
		"mixing_bowl/japanese_mayonnaise",
		"pot/okonomi_sauce",
		"pot/tentsuyu",
		"pot/shoyu_tare",
		"pot/yakitori_tare",
		"pot/mitarashi_glaze",
		"pot/sweet_chili_sauce",
		"mixing_bowl/cucumber_relish",
		"cutting/sliced_ginger",
		"crock/pickled_ginger",
		"cutting/cubed_tofu",
		"pot/miso_soup",
		"mixing_bowl/okonomiyaki_batter",
		"skillet/fried_okonomiyaki",
		"counter/okonomiyaki",
		"counter/teriyaki_salmon",
		"mixing_bowl/tempura_batter",
		"pot/tempura_pieces",
		"counter/tempura",
		"mixing_bowl/gyoza_filling",
		"counter/raw_gyoza",
		"skillet/gyoza",
		"counter/maki_sushi",
		"pot/boiled_ramen_noodles",
		"pot/ramen_broth",
		"counter/shoyu_ramen",
		"mixing_bowl/dango_dough",
		"pot/boiled_dango",
		"counter/raw_dango_skewer",
		"grill/charred_dango_skewer",
		"counter/mitarashi_dango",
		"mixing_bowl/takoyaki_batter",
		"aebleskive_pan/takoyaki_balls",
		"counter/takoyaki",
		"mixing_bowl/karaage_chicken",
		"pot/karaage",
		"counter/raw_yakitori",
		"grill/grilled_yakitori",
		"counter/yakitori",
		"pot/koji_amazake",
		"mixing_bowl/kra_pao_sauce",
		"wok/kra_pao_pork",
		"counter/pad_kra_pao",
		"mortar/curried_fish_paste",
		"mixing_bowl/tod_mun_mix",
		"counter/raw_tod_mun",
		"pot/fried_tod_mun",
		"counter/tod_mun_pla",
		"mixing_bowl/marinated_satay_chicken",
		"counter/raw_satay_skewer",
		"grill/grilled_satay_skewer",
		"counter/gai_satay",
		"wok/spring_roll_filling",
		"counter/raw_spring_rolls",
		"pot/fried_spring_rolls",
		"counter/poh_pia_tod",
		"mixing_bowl/lomo_saltado_sauce",
		"wok/lomo_saltado_beef",
		"counter/lomo_saltado"
	};

	@GameTest
	public void everyKojiRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void kojiBatchesGiveSporesBackAndTradersSellTheFirst(GameTestHelper helper) {
		var level = helper.getLevel();
		var spores = CookingPlants.item("koji_spores");
		var batch = CookingStation.CROCK.find(level, CookingInput.of(java.util.List.of(new ItemStack(CookingPlants.item("steamed_rice"), 4), new ItemStack(spores))))
			.orElseThrow().value();
		helper.assertTrue(batch.result().create().is(CookingPlants.item("koji_rice")), "Steamed rice and spores culture into koji rice");
		helper.assertTrue(batch.remainders().stream().anyMatch(r -> r.create().is(spores) && r.create().getCount() == 2),
			"Each koji batch gives two spores back, more than it used");
		var trades = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_TRADE);
		helper.assertTrue(trades.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_TRADE,
			PortablePocketCabin.id("wandering_trader/emerald_koji_spores"))).isPresent(), "Wandering traders sell the first koji spores");
		helper.succeed();
	}
}
