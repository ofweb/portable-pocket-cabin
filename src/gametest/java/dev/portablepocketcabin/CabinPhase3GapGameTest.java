package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static dev.portablepocketcabin.CabinDairyGameTest.pass;
import static dev.portablepocketcabin.CabinDairyGameTest.station;

/** The phase 3 products and dishes that phase 3's ingredients allowed but its deliveries left out. */
public final class CabinPhase3GapGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);
	static final String[] RECIPES = {
		"strainer/wet_potato_starch",
		"drying/potato_starch",
		"drying/dried_bread",
		"mill/breadcrumbs",
		"cutting/crustless_bread",
		"mill/fresh_panko",
		"drying/panko",
		"mixing_bowl/flatbread_dough",
		"ageing/strudel_dough",
		"mixing_bowl/plain_dough",
		"counter/gyoza_wrappers",
		"mixing_bowl/spring_roll_batter",
		"skillet/spring_roll_wrappers",
		"mixing_bowl/kansui",
		"mixing_bowl/ramen_dough",
		"counter/ramen_noodles",
		"mixing_bowl/tenkasu_batter",
		"pot/tenkasu",
		"mixing_bowl/puff_dough",
		"counter/laminated_dough",
		"ageing/puff_pastry",
		"pot/choux_panade",
		"mixing_bowl/choux_pastry",
		"cutting/tartlet_rings",
		"oven/tartlet_shells",
		"mixing_bowl/pretzel_dough",
		"proofing/proofed_pretzel_dough",
		"counter/shaped_pretzels",
		"mixing_bowl/dipped_pretzels",
		"oven/pretzel",
		"mixing_bowl/sponge_batter",
		"oven/chocolate_sponge",
		"pot/stretched_curds",
		"counter/queso_oaxaca",
		"drying/cotija_cheese",
		"ageing/minas_cheese",
		"pot/condensed_milk",
		"pot/evaporated_milk",
		"pot/dulce_de_leche",
		"pot/hard_boiled_eggs",
		"cutting/diced_nopales",
		"pot/nopales",
		"crock/fermented_cocoa_beans",
		"drying/dried_cocoa_beans",
		"oven/roasted_cocoa_beans",
		"cutting/roasted_cocoa_nibs",
		"press/cocoa_butter",
		"mill/cocoa_powder",
		"mill/cocoa_mass",
		"mixing_bowl/tempered_chocolate",
		"ageing/chocolate",
		"pot/chocolate_sauce",
		"mill/crushed_ice",
		"oven/tarte_tatin",
		"oven/choux_buns",
		"counter/profiteroles",
		"mixing_bowl/gougere_dough",
		"oven/gougeres",
		"pot/chocolat_chaud",
		"pot/warm_cocoa",
		"ageing/kalter_kakao",
		"pot/brigadeiro_mixture",
		"ageing/cooled_brigadeiro_mixture",
		"counter/brigadeiros",
		"counter/unbaked_brunsviger",
		"oven/brunsviger"
	};

	@GameTest
	public void everyGapRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void cocoaBeansBecomeChocolate(GameTestHelper helper) {
		var level = helper.getLevel();
		var crock = station(helper, BASE, CookingBlocks.CROCK);
		crock.setItem(0, new ItemStack(Items.COCOA_BEANS, 4));
		pass(helper, crock, 2 * CookingStation.DAY);
		helper.assertTrue(crock.getItem(CookingMenu.Kind.CROCK.outputSlot).is(item("fermented_cocoa_beans")), "Cocoa beans ferment in the crock");
		var nibs = CookingStation.CUTTING.find(level, CookingInput.of(List.of(new ItemStack(item("roasted_cocoa_beans"))))).orElseThrow();
		helper.assertTrue(nibs.value().result().item().value() == item("roasted_cocoa_nibs"), "Roasted beans winnow into nibs");
		var press = station(helper, BASE.east(2), CookingBlocks.PRESS);
		press.setItem(0, new ItemStack(item("roasted_cocoa_nibs"), 4));
		press.setItem(CookingMenu.Kind.PRESS.containerSlot, new ItemStack(Items.GLASS_BOTTLE));
		for (int i = 0; i < 60; i++) ProcessStationBlockEntity.serverTick(level, press.getBlockPos(), press.getBlockState(), press);
		helper.assertTrue(press.getItem(CookingMenu.Kind.PRESS.outputSlot).is(item("cocoa_butter"))
			&& press.getItem(CookingMenu.Kind.PRESS.byproductSlot).is(item("cocoa_press_cake")), "Nibs press into cocoa butter and press cake");
		helper.setBlock(BASE.south(2), CookingBlocks.PREP_COUNTER);
		var bowl = station(helper, BASE.south(2).above(), CookingBlocks.MIXING_BOWL);
		bowl.setItem(0, new ItemStack(item("cocoa_mass")));
		bowl.setItem(1, new ItemStack(Items.SUGAR));
		bowl.setItem(2, new ItemStack(item("cocoa_butter")));
		for (int i = 0; i < 10; i++) bowl.work(level);
		helper.assertTrue(bowl.getItem(CookingMenu.Kind.MIXING_BOWL.outputSlot).is(item("tempered_chocolate")), "Cocoa mass, sugar, and cocoa butter temper");
		helper.succeed();
	}

	@GameTest
	public void counterFillsProfiterolesWithTwoByproducts(GameTestHelper helper) {
		var level = helper.getLevel();
		var counter = station(helper, BASE, CookingBlocks.PREP_COUNTER);
		var kind = CookingMenu.Kind.COUNTER;
		counter.setItem(0, new ItemStack(item("choux_buns"), 4));
		counter.setItem(1, new ItemStack(CookingItems.WHIPPED_CREAM));
		counter.setItem(2, new ItemStack(item("chocolate_sauce")));
		for (int i = 0; i < 40; i++) ProcessStationBlockEntity.serverTick(level, counter.getBlockPos(), counter.getBlockState(), counter);
		helper.assertTrue(counter.getItem(kind.outputSlot).is(item("profiteroles")) && counter.getItem(kind.outputSlot).getCount() == 4,
			"Filled choux buns become profiteroles");
		helper.assertTrue(counter.getItem(kind.byproductSlots[0]).is(Items.BOWL) && counter.getItem(kind.byproductSlots[1]).is(Items.GLASS_BOTTLE),
			"The cream's bowl and the sauce's bottle come back");
		helper.succeed();
	}

	private static net.minecraft.world.item.Item item(String name) {
		return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(PortablePocketCabin.id(name));
	}
}
