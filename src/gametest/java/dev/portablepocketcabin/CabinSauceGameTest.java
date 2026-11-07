package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static dev.portablepocketcabin.CabinDairyGameTest.station;

/** B-0047 sauces, and the Berliner that ships once jam exists. */
public final class CabinSauceGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void potMakesRouxJamAndCaramelSauce(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		pot.fillWater();
		cook(helper, stove, pot, 105, new ItemStack(Items.BOWL), new ItemStack(CookingItems.SALTED_BUTTER), new ItemStack(CookingItems.WHEAT_FLOUR));
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.ROUX), "Butter and flour cook into roux");
		cook(helper, stove, pot, 305, new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.SWEET_BERRIES, 6), new ItemStack(Items.SUGAR, 3),
			new ItemStack(CookingItems.APPLE_POMACE));
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.SWEET_BERRY_JAM) && pot.water() == CookingStation.MAX_WATER,
			"Berries, sugar, and pomace set into jam even in a pot of water");
		cook(helper, stove, pot, 205, new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.SWEET_BERRIES, 6), new ItemStack(Items.SUGAR, 2));
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.UNSTRAINED_BERRY_CORDIAL),
			"Without pomace, berries and sugar still make cordial");
		cook(helper, stove, pot, 105, ItemStack.EMPTY, new ItemStack(CookingItems.CARAMEL), new ItemStack(CookingItems.CREAM));
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.CARAMEL_SAUCE), "Caramel and cream make caramel sauce");
		helper.succeed();
	}

	@GameTest
	public void sweetDoughFriesAndFillsIntoBerliners(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		pot.fillOil();
		stove.setItem(0, new ItemStack(Items.BLAZE_ROD));
		cook(helper, stove, pot, 105, ItemStack.EMPTY, new ItemStack(CookingItems.PROOFED_SWEET_DOUGH));
		var doughnuts = pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(doughnuts.is(CookingItems.FRIED_DOUGHNUT) && doughnuts.getCount() == 4, "Sweet dough deep-fries into four doughnuts");

		var counter = station(helper, BASE.east(2), CookingBlocks.PREP_COUNTER);
		counter.setItem(0, doughnuts.copy());
		counter.setItem(1, new ItemStack(CookingItems.SWEET_BERRY_JAM));
		counter.setItem(2, new ItemStack(Items.SUGAR));
		for (int i = 0; i < 40; i++) ProcessStationBlockEntity.serverTick(level, counter.getBlockPos(), counter.getBlockState(), counter);
		helper.assertTrue(counter.getItem(CookingMenu.Kind.COUNTER.outputSlot).is(CookingItems.BERLINER)
			&& counter.getItem(CookingMenu.Kind.COUNTER.outputSlot).getCount() == 4
			&& counter.getItem(CookingMenu.Kind.COUNTER.byproductSlot).is(Items.GLASS_BOTTLE), "Jam and sugar finish four Berliners");
		helper.succeed();
	}

	/** Empties the pot's slots but keeps its liquid, fills the inputs, and cooks over the stove. */
	private static void cook(GameTestHelper helper, StoveBlockEntity stove, CookingPotBlockEntity pot, int ticks, ItemStack container,
		ItemStack... ingredients) {
		for (int slot = 0; slot < pot.getContainerSize(); slot++) pot.setItem(slot, ItemStack.EMPTY);
		for (int slot = 0; slot < ingredients.length; slot++) pot.setItem(slot, ingredients[slot]);
		pot.setItem(CookingPotBlockEntity.CONTAINER_SLOT, container);
		for (int i = 0; i < ticks; i++) {
			if (stove.getItem(0).isEmpty() && !stove.burner().burning()) stove.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
			StoveBlockEntity.serverTick(helper.getLevel(), stove.getBlockPos(), stove.getBlockState(), stove);
			CookingPotBlockEntity.serverTick(helper.getLevel(), pot.getBlockPos(), pot.getBlockState(), pot);
		}
	}
}
