package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** B-0049 delivery 2: the meat grinder, the sausage stuffer, and the recipes they complete. */
public final class CabinSausageGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);
	static final String[] RECIPES = {
		"meat_grinder/minced_beef",
		"meat_grinder/minced_pork",
		"mixing_bowl/bratwurst_filling",
		"sausage_stuffer/raw_bratwurst",
		"mixing_bowl/toulouse_filling",
		"sausage_stuffer/toulouse_sausage",
		"mixing_bowl/chorizo_filling",
		"sausage_stuffer/fresh_chorizo",
		"ageing/chorizo",
		"mixing_bowl/smoked_sausage_filling",
		"sausage_stuffer/raw_smoked_sausage",
		"mixing_bowl/cocktail_sausage_filling",
		"sausage_stuffer/raw_cocktail_sausages",
		"pot/poached_cocktail_sausages",
		"mixing_bowl/blood_sausage_filling",
		"sausage_stuffer/raw_blood_sausage",
		"pot/blood_sausage",
		"skillet/fried_bratwurst",
		"pot/warm_sauerkraut",
		"counter/bratwurst_mit_sauerkraut",
		"counter/currywurst",
		"pot/feijoada_beans",
		"pot/feijoada",
		"mixing_bowl/frikadeller_fars",
		"skillet/frikadeller",
		"pot/poached_boller",
		"pot/boller_i_karry_stew",
		"counter/boller_i_karry",
		"counter/raw_polsehorn",
		"proofing/proofed_polsehorn",
		"oven/polsehorn"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void theGrinderMincesAndTheStufferFillsCasings(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.PREP_COUNTER);
		helper.setBlock(BASE.above(), Butchery.MEAT_GRINDER);
		var grinder = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		grinder.setItem(0, new ItemStack(Butchery.PARTS.get("pork_shoulder")));
		grinder.setItem(1, new ItemStack(Butchery.PARTS.get("back_fat")));
		for (int turn = 0; turn < 3; turn++) grinder.work(level);
		helper.assertTrue(grinder.getItem(CookingMenu.Kind.MEAT_GRINDER.outputSlot).isEmpty(), "Three turns aren't enough to mince");
		grinder.work(level);
		helper.assertTrue(grinder.getItem(CookingMenu.Kind.MEAT_GRINDER.outputSlot).is(CookingPlants.item("minced_pork"))
			&& grinder.getItem(CookingMenu.Kind.MEAT_GRINDER.outputSlot).getCount() == 3, "Four turns mince shoulder and back fat into pork");

		helper.setBlock(BASE.east(2), CookingBlocks.PREP_COUNTER);
		helper.setBlock(BASE.east(2).above(), Butchery.SAUSAGE_STUFFER);
		var stuffer = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2).above()));
		stuffer.setItem(0, new ItemStack(CookingPlants.item("bratwurst_filling")));
		for (int turn = 0; turn < 4; turn++) stuffer.work(level);
		helper.assertTrue(stuffer.getItem(CookingMenu.Kind.SAUSAGE_STUFFER.outputSlot).isEmpty(), "The stuffer waits for a casing");
		stuffer.setItem(CookingMenu.Kind.SAUSAGE_STUFFER.containerSlot, new ItemStack(Butchery.PARTS.get("pork_casing")));
		for (int turn = 0; turn < 4; turn++) stuffer.work(level);
		helper.assertTrue(stuffer.getItem(CookingMenu.Kind.SAUSAGE_STUFFER.outputSlot).is(CookingPlants.item("raw_bratwurst"))
			&& stuffer.getItem(CookingMenu.Kind.SAUSAGE_STUFFER.containerSlot).isEmpty(), "With a casing, the filling becomes bratwurst");
		helper.succeed();
	}
}
