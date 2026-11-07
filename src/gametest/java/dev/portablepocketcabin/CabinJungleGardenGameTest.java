package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** B-0050 delivery 6: jungles. */
public final class CabinJungleGardenGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);
	static final String[] RECIPES = {
		"drying/cinnamon",
		"drying/cloves",
		"drying/dried_peppercorns",
		"mill/black_pepper",
		"drying/dried_cardamom_pods",
		"mill/cardamom",
		"drying/dried_nutmeg",
		"mill/nutmeg",
		"drying/dried_black_beans",
		"pot/blanched_vanilla_pods",
		"crock/sweated_vanilla_pods",
		"drying/vanilla",
		"mixing_bowl/curry_spice_mix",
		"skillet/toasted_curry_spices",
		"mill/curry_powder",
		"mixing_bowl/pickling_spice",
		"mixing_bowl/massaman_spices",
		"skillet/toasted_massaman_spices",
		"mixing_bowl/massaman_curry_mix",
		"mortar/massaman_curry_paste",
		"mortar/satay_spices",
		"pot/soaked_chilies",
		"mixing_bowl/al_pastor_adobo",
		"pot/unstrained_achiote_oil",
		"strainer/achiote_oil",
		"pot/boiled_oil_palm_fruit",
		"mill/oil_palm_mash",
		"press/dende_oil",
		"press/lime_juice",
		"cutting/grated_coconut",
		"mixing_bowl/soaked_grated_coconut",
		"press/coconut_milk",
		"ageing/settled_coconut_milk",
		"cutting/coconut_cream",
		"drying/desiccated_coconut",
		"skillet/toasted_coconut",
		"mixing_bowl/satay_marinade",
		"cutting/grated_cassava",
		"strainer/wet_tapioca_starch",
		"drying/tapioca_starch",
		"cutting/grated_sweet_potato",
		"strainer/wet_sweet_potato_starch",
		"drying/sweet_potato_starch",
		"crock/fermented_tapioca_starch",
		"drying/sour_cassava_starch",
		"mixing_bowl/glass_noodle_dough",
		"pot/fresh_glass_noodles",
		"drying/glass_noodles",
		"pot/thick_palm_syrup",
		"ageing/palm_sugar",
		"pot/unstrained_chancaca_syrup",
		"strainer/chancaca_syrup",
		"mixing_bowl/mexican_chocolate_mix",
		"mortar/mexican_chocolate_paste",
		"ageing/mexican_chocolate",
		"skillet/toasted_chilies",
		"mortar/mole_spices",
		"mixing_bowl/mole_base",
		"mill/ground_mole_base",
		"mortar/mole_paste",
		"pot/bechamel",
		"mixing_bowl/crema",
		"pot/queso_blanco_curds",
		"cheese_press/queso_blanco",
		"pot/curry_ketchup_base",
		"mill/curry_ketchup",
		"mixing_bowl/karrysalat",
		"cutting/chopped_gherkins",
		"mixing_bowl/remoulade",
		"mixing_bowl/gingerbread_dough",
		"ageing/rested_gingerbread_dough",
		"oven/gingerbread",
		"mixing_bowl/kammerjunkere_dough",
		"counter/shaped_kammerjunkere",
		"oven/baked_kammerjunkere",
		"oven/kammerjunkere",
		"pot/warm_rice_pudding",
		"ageing/rice_pudding",
		"pot/warm_vanilla_custard",
		"ageing/vanilla_custard",
		"mixing_bowl/thai_tea_leaves",
		"crock/fermented_coffee_cherries",
		"drying/dried_coffee_cherries",
		"cutting/green_coffee_beans",
		"oven/roasted_coffee_beans",
		"mill/roasted_coffee",
		"skillet/seared_yerba_mate_leaves",
		"drying/dried_yerba_mate_leaves",
		"ageing/aged_yerba_mate",
		"mill/yerba_mate",
		"pot/vanilla_infused_cream",
		"mixing_bowl/creme_brulee_custard",
		"oven/baked_creme_brulee",
		"oven/creme_brulee",
		"mixing_bowl/socca_batter",
		"oven/socca",
		"counter/unbaked_apfelstrudel",
		"oven/apfelstrudel",
		"counter/unbaked_bienenstich",
		"oven/baked_bienenstich",
		"counter/bienenstich",
		"pot/kinderpunsch",
		"mixing_bowl/risalamande",
		"counter/risalamande_med_kirsebaersauce",
		"mixing_bowl/koldskal",
		"ageing/chilled_koldskal",
		"counter/koldskal_med_kammerjunkere",
		"mixing_bowl/aebleskiver_batter",
		"aebleskive_pan/plain_aebleskiver",
		"counter/aebleskiver",
		"pot/unstrained_hyldebaersaft",
		"strainer/varm_hyldebaersaft",
		"pot/steamed_glutinous_rice",
		"pot/sweet_coconut_sauce",
		"mixing_bowl/coconut_sticky_rice",
		"cutting/sliced_mango",
		"counter/mango_sticky_rice",
		"mixing_bowl/bua_loy_dough",
		"counter/bua_loy_balls",
		"pot/boiled_bua_loy",
		"pot/bua_loy",
		"mixing_bowl/khanom_krok_batter",
		"aebleskive_pan/khanom_krok",
		"cutting/diced_water_chestnut",
		"mixing_bowl/coated_water_chestnut",
		"pot/boiled_tub_tim",
		"counter/tub_tim_grob",
		"cutting/hollowed_pumpkin",
		"mixing_bowl/sangkhaya_custard",
		"pot/sangkhaya_fak_thong",
		"mixing_bowl/nam_manao",
		"pot/strong_thai_tea",
		"strainer/strained_thai_tea",
		"mixing_bowl/cha_yen",
		"pot/brewed_black_tea",
		"mixing_bowl/cha_manao",
		"cutting/bruised_lemongrass",
		"pot/unstrained_nam_takrai",
		"strainer/nam_takrai",
		"pot/churro_dough",
		"pot/fried_churros",
		"counter/churros",
		"mixing_bowl/flan_custard",
		"oven/flan_napolitano",
		"pot/arroz_con_leche",
		"grill/charred_corn",
		"counter/elote",
		"crock/soaked_horchata_rice",
		"mill/horchata_base",
		"strainer/strained_horchata",
		"mixing_bowl/horchata_de_arroz",
		"pot/champurrado",
		"pot/unstrained_cafe_de_olla",
		"strainer/cafe_de_olla",
		"mixing_bowl/arepa_dough",
		"counter/shaped_arepas",
		"skillet/arepas",
		"counter/arepas_con_queso",
		"pot/scalded_starch",
		"mixing_bowl/pao_de_queijo_dough",
		"counter/pao_de_queijo_balls",
		"oven/pao_de_queijo",
		"mixing_bowl/alfajor_dough",
		"cutting/alfajor_rounds",
		"oven/alfajor_biscuits",
		"counter/alfajores_de_maicena",
		"pot/boiled_picarones_vegetables",
		"mixing_bowl/picarones_dough",
		"proofing/rested_picarones_dough",
		"pot/fried_picarones",
		"counter/picarones",
		"counter/raw_tequenos",
		"pot/tequenos",
		"pot/unstrained_chicha_morada",
		"strainer/strained_chicha_morada",
		"mixing_bowl/chicha_morada",
		"pot/aguapanela",
		"mixing_bowl/aguapanela_con_limon",
		"mixing_bowl/limonada_suica_mix",
		"strainer/limonada_suica",
		"mixing_bowl/terere",
		"mixing_bowl/limonada_de_coco_mix",
		"strainer/limonada_de_coco"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void cinnamonBarkStripsWithAnAxeAndGrowsBack(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(BASE);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		level.setBlockAndUpdate(pos, CookingPlants.CINNAMON_LOG.defaultBlockState());
		helper.assertTrue(level.getBlockState(pos).getValue(HarvestTrunkBlock.READY), "A cinnamon trunk has its bark");
		var axe = new ItemStack(Items.IRON_AXE);
		player.setItemInHand(InteractionHand.MAIN_HAND, axe);
		var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
		level.getBlockState(pos).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(level.getBlockState(pos).is(CookingPlants.CINNAMON_LOG) && !level.getBlockState(pos).getValue(HarvestTrunkBlock.READY),
			"An axe strips the bark and leaves the trunk standing");
		helper.assertTrue(axe.getDamageValue() == 1, "Stripping wears the axe");
		helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
			.anyMatch(entity -> entity.getItem().is(CookingPlants.CINNAMON_BARK)), "Stripping drops cinnamon bark");
		helper.assertTrue(level.getBlockState(pos).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit)
			== net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND, "A stripped trunk has no bark to take");
		var random = RandomSource.create(9);
		for (int i = 0; i < 400 && !level.getBlockState(pos).getValue(HarvestTrunkBlock.READY); i++) level.getBlockState(pos).randomTick(level, pos, random);
		helper.assertTrue(level.getBlockState(pos).getValue(HarvestTrunkBlock.READY), "The bark grows back");
		helper.assertTrue(level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS), "Cinnamon trunks keep their leaves alive");
		helper.succeed();
	}

	@GameTest
	public void palmTrunksFillWithSapForAGlassBottle(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(BASE);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		level.setBlockAndUpdate(pos, CookingPlants.PALM_LOG.defaultBlockState());
		var bottles = new ItemStack(Items.GLASS_BOTTLE, 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, bottles);
		var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
		level.getBlockState(pos).useItemOn(bottles, level, player, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(bottles.getCount() == 2, "A new palm trunk has no sap yet");
		var random = RandomSource.create(4);
		for (int i = 0; i < 400 && !level.getBlockState(pos).getValue(HarvestTrunkBlock.READY); i++) level.getBlockState(pos).randomTick(level, pos, random);
		helper.assertTrue(level.getBlockState(pos).getValue(HarvestTrunkBlock.READY), "The trunk fills with sap");
		level.getBlockState(pos).useItemOn(bottles, level, player, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(bottles.getCount() == 1 && player.getInventory().countItem(CookingPlants.PALM_SAP) == 1
			&& !level.getBlockState(pos).getValue(HarvestTrunkBlock.READY), "A glass bottle taps the sap");
		helper.setBlock(BASE.east(2).below(), Blocks.SAND);
		var coconut = ((net.minecraft.world.item.BlockItem) CookingPlants.item("coconut_sapling")).getBlock();
		helper.assertTrue(coconut.defaultBlockState().canSurvive(level, helper.absolutePos(BASE.east(2))), "Coconut palms grow on sand");
		var lime = ((net.minecraft.world.item.BlockItem) CookingPlants.item("lime_sapling")).getBlock();
		helper.assertTrue(!lime.defaultBlockState().canSurvive(level, helper.absolutePos(BASE.east(2))), "Other fruit trees don't");
		helper.succeed();
	}
}
