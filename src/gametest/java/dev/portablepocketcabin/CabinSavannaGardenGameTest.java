package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/** B-0050 delivery 3: savannas, deserts, and badlands. */
public final class CabinSavannaGardenGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);
	static final String[] RECIPES = {
		"skillet/toasted_sesame_seeds",
		"mill/sesame_paste",
		"press/sesame_oil",
		"drying/dried_chickpeas",
		"mill/chickpea_flour",
		"pot/blue_hominy",
		"mill/blue_corn_masa",
		"skillet/charred_tomato",
		"mixing_bowl/raw_salsa_roja",
		"pot/salsa_roja",
		"skillet/roasted_tomatillo",
		"mixing_bowl/salsa_verde",
		"cutting/tamarind_pulp",
		"mixing_bowl/soaked_tamarind",
		"strainer/tamarind_paste",
		"drying/anise",
		"drying/dried_cumin_seed",
		"skillet/toasted_cumin_seed",
		"mill/cumin",
		"drying/dried_paprika_pepper",
		"mill/paprika",
		"drying/dried_chilies",
		"mill/chili_powder",
		"drying/bay_leaf",
		"counter/bouquet_garni",
		"oven/roasted_peanut_pods",
		"cutting/roasted_peanuts",
		"pot/blanched_almonds",
		"cutting/sliced_almonds",
		"mortar/chopped_almonds",
		"mill/crushed_red_grapes",
		"crock/fermented_red_grapes",
		"press/young_red_wine",
		"ageing/red_wine",
		"crock/wine_vinegar_from_red_wine",
		"drying/raisins",
		"drying/dried_hibiscus",
		"skillet/brown_butter",
		"mixing_bowl/madeleine_batter",
		"oven/madeleines",
		"press/lemon_juice",
		"mixing_bowl/citron_presse",
		"mixing_bowl/cheesecake_filling",
		"counter/unbaked_kasekuchen",
		"oven/kasekuchen",
		"mixing_bowl/obatzda",
		"counter/obatzda_with_bread",
		"mixing_bowl/fruchtetee_blend",
		"pot/fruchtetee",
		"pot/hot_hibiscus",
		"strainer/strained_hibiscus",
		"ageing/agua_de_jamaica",
		"counter/nam_krajeab",
		"pot/tamarind_water",
		"mixing_bowl/agua_de_tamarindo",
		"crock/infused_elderflower",
		"strainer/hyldeblomstsaft",
		"cutting/chopped_rhubarb",
		"pot/unstrained_rabarbersaft",
		"strainer/rabarbersaft",
		"counter/unbaked_quesadilla",
		"skillet/quesadilla",
		"counter/seasoned_quesadilla"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void cuminGrowsWildOnDesertSand(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.below(), Blocks.SAND);
		var configured = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
		configured.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
			PortablePocketCabin.id("wild_cumin"))).value().place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), helper.absolutePos(BASE));
		helper.assertTrue(level.getBlockState(helper.absolutePos(BASE)).is(CookingPlants.PLANTS.get("cumin")), "Wild cumin stands on sand");
		helper.assertTrue(CookingPlants.PLANTS.containsKey("lemon_leaves") && CookingPlants.PLANTS.containsKey("tomato_vine"),
			"Citrus trees and tomato vines are registered");
		helper.succeed();
	}
}
