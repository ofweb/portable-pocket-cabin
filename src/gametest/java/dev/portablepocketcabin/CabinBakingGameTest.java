package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import static dev.portablepocketcabin.CabinDairyGameTest.pass;
import static dev.portablepocketcabin.CabinDairyGameTest.station;

/** B-0046 baking: flour, sourdough and yeast, doughs and pastry, and the bakes that replace the B-0045 oven recipes. */
public final class CabinBakingGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void starterKneadsAndProofsIntoBreadAndRolls(GameTestHelper helper) {
		var level = helper.getLevel();
		var mill = station(helper, BASE, CookingBlocks.MILL);
		mill.setItem(0, new ItemStack(Items.WHEAT, 3));
		for (int i = 0; i < 300; i++) ProcessStationBlockEntity.serverTick(level, mill.getBlockPos(), mill.getBlockState(), mill);
		helper.assertTrue(mill.getItem(CookingMenu.Kind.MILL.outputSlot).is(CookingItems.WHEAT_FLOUR)
			&& mill.getItem(CookingMenu.Kind.MILL.outputSlot).getCount() == 3, "The mill grinds wheat into flour");

		var crock = station(helper, BASE.east(2), CookingBlocks.CROCK);
		var kind = CookingMenu.Kind.CROCK;
		crock.setItem(0, new ItemStack(CookingItems.WHEAT_FLOUR, 2));
		crock.setItem(1, water());
		pass(helper, crock, CookingStation.DAY);
		helper.assertTrue(crock.getItem(kind.outputSlot).is(CookingItems.SOURDOUGH_STARTER), "Flour and water sour into a starter in a day");
		crock.setItem(0, crock.getItem(kind.outputSlot).copy());
		crock.setItem(kind.outputSlot, ItemStack.EMPTY);
		crock.setItem(1, new ItemStack(CookingItems.WHEAT_FLOUR));
		crock.setItem(2, water());
		pass(helper, crock, CookingStation.DAY / 2);
		helper.assertTrue(crock.getItem(kind.outputSlot).getCount() == 2, "Feeding the starter doubles it");

		helper.setBlock(BASE.south(2), CookingBlocks.PREP_COUNTER);
		var bowl = station(helper, BASE.south(2).above(), CookingBlocks.MIXING_BOWL);
		var mixing = CookingMenu.Kind.MIXING_BOWL;
		bowl.setItem(0, new ItemStack(CookingItems.WHEAT_FLOUR, 3));
		bowl.setItem(1, new ItemStack(CookingItems.SOURDOUGH_STARTER));
		bowl.setItem(2, water());
		bowl.setItem(3, new ItemStack(CookingItems.SALT));
		for (int i = 0; i < 8; i++) bowl.work(level);
		helper.assertTrue(bowl.getItem(mixing.outputSlot).is(CookingItems.BREAD_DOUGH) && bowl.getItem(mixing.byproductSlot).is(Items.GLASS_BOTTLE),
			"Flour, starter, water, and salt knead into bread dough");

		var basket = station(helper, BASE.south(4), CookingBlocks.PROOFING_BASKET);
		basket.setItem(0, new ItemStack(CookingItems.BREAD_DOUGH, 2));
		pass(helper, basket, 2 * CookingStation.HOUR);
		pass(helper, basket, 2 * CookingStation.HOUR);
		helper.assertTrue(basket.getItem(CookingMenu.Kind.PROOFING.outputSlot).getCount() == 2, "Dough proofs in two hours");

		var oven = oven(helper, BASE.east(4), Items.COAL);
		oven.setItem(0, new ItemStack(CookingItems.PROOFED_BREAD_DOUGH));
		bake(helper, oven, 205);
		helper.assertTrue(oven.getItem(OvenBlockEntity.OUTPUT_SLOT).is(Items.BREAD), "Proofed dough bakes into bread");

		var counter = station(helper, BASE.east(6), CookingBlocks.PREP_COUNTER);
		counter.setItem(0, new ItemStack(CookingItems.PROOFED_BREAD_DOUGH));
		for (int i = 0; i < 40; i++) ProcessStationBlockEntity.serverTick(level, counter.getBlockPos(), counter.getBlockState(), counter);
		helper.assertTrue(counter.getItem(CookingMenu.Kind.COUNTER.outputSlot).getCount() == 2, "The counter shapes two rolls");
		oven.setItem(OvenBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		oven.setItem(0, counter.getItem(CookingMenu.Kind.COUNTER.outputSlot).copy());
		bake(helper, oven, 165);
		helper.assertTrue(oven.getItem(OvenBlockEntity.OUTPUT_SLOT).is(CookingItems.BREAD_ROLL)
			&& oven.getItem(OvenBlockEntity.OUTPUT_SLOT).getCount() == 2, "Rolls bake");
		helper.succeed();
	}

	@GameTest
	public void butterMakesPastryPieCookiesAndCake(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.PREP_COUNTER);
		var bowl = station(helper, BASE.above(), CookingBlocks.MIXING_BOWL);
		var out = CookingMenu.Kind.MIXING_BOWL.outputSlot;
		mix(bowl, level, 6, new ItemStack(CookingItems.WHEAT_FLOUR, 2), new ItemStack(CookingItems.SALTED_BUTTER), water());
		helper.assertTrue(bowl.getItem(out).is(CookingItems.SHORTCRUST_DOUGH), "Butter rubbed into flour gives shortcrust dough");

		helper.setBlock(BASE.east(2), CookingBlocks.AGEING_SHELF);
		var shelf = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2)));
		shelf.add(level, bowl.removeItem(out, 1));
		SpotStationBlockEntity.serverTick(level, shelf.getBlockPos(), shelf.getBlockState(), shelf);
		shelf.rewind(CookingStation.HOUR);
		SpotStationBlockEntity.serverTick(level, shelf.getBlockPos(), shelf.getBlockState(), shelf);
		helper.assertTrue(shelf.getItem(0).is(CookingItems.SHORTCRUST_PASTRY), "An hour on the shelf chills the pastry");

		var counter = station(helper, BASE.south(2), CookingBlocks.PREP_COUNTER);
		counter.setItem(0, shelf.take(level));
		counter.setItem(1, new ItemStack(Items.PUMPKIN));
		counter.setItem(2, new ItemStack(Items.EGG));
		counter.setItem(3, new ItemStack(CookingItems.BROWN_SUGAR));
		for (int i = 0; i < 40; i++) ProcessStationBlockEntity.serverTick(level, counter.getBlockPos(), counter.getBlockState(), counter);
		var oven = oven(helper, BASE.east(4), Items.COAL);
		oven.setItem(0, counter.getItem(CookingMenu.Kind.COUNTER.outputSlot).copy());
		bake(helper, oven, 205);
		helper.assertTrue(oven.getItem(OvenBlockEntity.OUTPUT_SLOT).is(Items.PUMPKIN_PIE), "A filled pastry case bakes into pumpkin pie");

		mix(bowl, level, 4, new ItemStack(CookingItems.WHEAT_FLOUR, 2), new ItemStack(CookingItems.SALTED_BUTTER), new ItemStack(Items.SUGAR),
			new ItemStack(Items.EGG), new ItemStack(Items.COCOA_BEANS));
		oven.setItem(OvenBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		oven.setItem(0, bowl.removeItem(out, 1));
		bake(helper, oven, 165);
		helper.assertTrue(oven.getItem(OvenBlockEntity.OUTPUT_SLOT).is(Items.COOKIE) && oven.getItem(OvenBlockEntity.OUTPUT_SLOT).getCount() == 8,
			"Cookie dough bakes into eight cookies");

		mix(bowl, level, 6, new ItemStack(CookingItems.WHEAT_FLOUR, 3), new ItemStack(Items.SUGAR, 2), new ItemStack(Items.EGG, 2),
			new ItemStack(CookingItems.SALTED_BUTTER), new ItemStack(Items.MILK_BUCKET));
		helper.assertTrue(bowl.getItem(out).is(CookingItems.CAKE_BATTER) && bowl.getItem(CookingMenu.Kind.MIXING_BOWL.byproductSlot).is(Items.BUCKET),
			"Cake batter whisks up and gives the bucket back");
		var lowOven = oven(helper, BASE.east(6), Items.OAK_PLANKS);
		lowOven.setItem(0, bowl.removeItem(out, 1));
		bake(helper, lowOven, 305);
		helper.assertTrue(lowOven.getItem(OvenBlockEntity.OUTPUT_SLOT).is(Items.CAKE), "Batter bakes into cake at low heat");
		helper.succeed();
	}

	@GameTest
	public void starterGrowsYeastForSweetDough(GameTestHelper helper) {
		var level = helper.getLevel();
		var crock = station(helper, BASE, CookingBlocks.CROCK);
		crock.setItem(0, new ItemStack(CookingItems.SOURDOUGH_STARTER));
		crock.setItem(1, new ItemStack(Items.SUGAR));
		crock.setItem(2, water());
		pass(helper, crock, CookingStation.DAY);
		var culture = crock.getItem(CookingMenu.Kind.CROCK.outputSlot);
		helper.assertTrue(culture.is(CookingItems.YEAST_CULTURE), "A starter grows yeast in sugar water");
		helper.setBlock(BASE.east(2), CookingBlocks.DRYING_RACK);
		var rack = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2)));
		rack.add(level, culture.copy());
		SpotStationBlockEntity.serverTick(level, rack.getBlockPos(), rack.getBlockState(), rack);
		rack.rewind(CookingStation.DAY);
		SpotStationBlockEntity.serverTick(level, rack.getBlockPos(), rack.getBlockState(), rack);
		helper.assertTrue(rack.getItem(0).is(CookingItems.YEAST), "The culture dries into yeast");

		helper.setBlock(BASE.south(2), CookingBlocks.PREP_COUNTER);
		var bowl = station(helper, BASE.south(2).above(), CookingBlocks.MIXING_BOWL);
		mix(bowl, level, 8, new ItemStack(CookingItems.WHEAT_FLOUR, 2), new ItemStack(CookingItems.YEAST), new ItemStack(Items.MILK_BUCKET),
			new ItemStack(Items.SUGAR), new ItemStack(Items.EGG), new ItemStack(CookingItems.SALTED_BUTTER));
		helper.assertTrue(bowl.getItem(CookingMenu.Kind.MIXING_BOWL.outputSlot).is(CookingItems.SWEET_YEAST_DOUGH), "Enriched dough kneads with yeast");
		var basket = station(helper, BASE.south(4), CookingBlocks.PROOFING_BASKET);
		basket.setItem(0, bowl.removeItem(CookingMenu.Kind.MIXING_BOWL.outputSlot, 1));
		pass(helper, basket, 2 * CookingStation.HOUR);
		helper.assertTrue(basket.getItem(CookingMenu.Kind.PROOFING.outputSlot).is(CookingItems.PROOFED_SWEET_DOUGH), "Sweet dough proofs");
		helper.succeed();
	}

	private static void mix(ProcessStationBlockEntity bowl, net.minecraft.server.level.ServerLevel level, int uses, ItemStack... ingredients) {
		for (int slot = 0; slot < 6; slot++) bowl.setItem(slot, slot < ingredients.length ? ingredients[slot] : ItemStack.EMPTY);
		bowl.setItem(CookingMenu.Kind.MIXING_BOWL.outputSlot, ItemStack.EMPTY);
		bowl.setItem(CookingMenu.Kind.MIXING_BOWL.byproductSlot, ItemStack.EMPTY);
		for (int i = 0; i < uses; i++) bowl.work(level);
	}

	private static OvenBlockEntity oven(GameTestHelper helper, BlockPos pos, Item fuel) {
		helper.setBlock(pos, CookingBlocks.OVEN);
		var oven = (OvenBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
		oven.setItem(OvenBlockEntity.FUEL_SLOT, new ItemStack(fuel, 8));
		return oven;
	}

	private static void bake(GameTestHelper helper, OvenBlockEntity oven, int ticks) {
		for (int i = 0; i < ticks; i++) OvenBlockEntity.serverTick(helper.getLevel(), oven.getBlockPos(), oven.getBlockState(), oven);
	}

	private static ItemStack water() {
		return PotionContents.createItemStack(Items.POTION, Potions.WATER);
	}
}
