package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** B-0046 dairy: cream, butter and buttermilk, whipped cream, yogurt, and crème fraîche. */
public final class CabinDairyGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void milkSettlesIntoCreamThatChurnsIntoButter(GameTestHelper helper) {
		var level = helper.getLevel();
		var pan = station(helper, BASE, CookingBlocks.DAIRY_PAN);
		pan.setItem(0, new ItemStack(Items.MILK_BUCKET));
		pan.setItem(CookingMenu.Kind.DAIRY_PAN.containerSlot, new ItemStack(Items.GLASS_BOTTLE, 2));
		pass(helper, pan, CookingStation.DAY);
		helper.assertTrue(pan.getItem(CookingMenu.Kind.DAIRY_PAN.outputSlot).is(CookingItems.CREAM)
			&& pan.getItem(CookingMenu.Kind.DAIRY_PAN.outputSlot).getCount() == 2
			&& pan.getItem(CookingMenu.Kind.DAIRY_PAN.byproductSlot).is(CookingItems.SKIMMED_MILK_BUCKET), "A day settles cream off skimmed milk");

		var churn = station(helper, BASE.east(2), CookingBlocks.BUTTER_CHURN);
		var kind = CookingMenu.Kind.CHURN;
		churn.setItem(0, new ItemStack(CookingItems.CREAM, 2));
		for (int i = 0; i < 11; i++) churn.work(level);
		helper.assertTrue(churn.getItem(kind.outputSlot).isEmpty(), "Eleven strokes are not yet butter");
		churn.work(level);
		helper.assertTrue(churn.getItem(kind.outputSlot).is(CookingItems.RAW_BUTTER)
			&& churn.getItem(kind.byproductSlots[0]).is(CookingItems.BUTTERMILK) && churn.getItem(kind.byproductSlots[1]).is(Items.GLASS_BOTTLE),
			"Twelve strokes churn butter, buttermilk, and a bottle");

		helper.setBlock(BASE.south(2), CookingBlocks.PREP_COUNTER);
		var bowl = station(helper, BASE.south(2).above(), CookingBlocks.MIXING_BOWL);
		var mixing = CookingMenu.Kind.MIXING_BOWL;
		bowl.setItem(0, new ItemStack(CookingItems.RAW_BUTTER));
		bowl.setItem(1, new ItemStack(CookingItems.SALT));
		for (int i = 0; i < 3; i++) bowl.work(level);
		helper.assertTrue(bowl.getItem(mixing.outputSlot).is(CookingItems.SALTED_BUTTER), "Salt worked into raw butter salts it");
		bowl.setItem(mixing.outputSlot, ItemStack.EMPTY);
		bowl.setItem(0, new ItemStack(CookingItems.CREAM, 2));
		bowl.setItem(1, new ItemStack(Items.SUGAR));
		bowl.setItem(mixing.containerSlot, new ItemStack(Items.BOWL));
		for (int i = 0; i < 8; i++) bowl.work(level);
		helper.assertTrue(bowl.getItem(mixing.outputSlot).is(CookingItems.WHIPPED_CREAM)
			&& bowl.getItem(mixing.byproductSlot).getCount() == 2, "Cream whisked with sugar gives whipped cream and the bottles");
		helper.succeed();
	}

	@GameTest
	public void buttermilkStartsYogurtAndYogurtKeepsItGoing(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		pot.setItem(0, new ItemStack(Items.MILK_BUCKET));
		stove.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
		for (int i = 0; i < 105; i++) {
			StoveBlockEntity.serverTick(level, stove.getBlockPos(), stove.getBlockState(), stove);
			CookingPotBlockEntity.serverTick(level, pot.getBlockPos(), pot.getBlockState(), pot);
		}
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.WARM_MILK_BUCKET), "Milk warms at low heat");

		var crock = station(helper, BASE.east(2), CookingBlocks.CROCK);
		var kind = CookingMenu.Kind.CROCK;
		crock.setItem(0, new ItemStack(CookingItems.WARM_MILK_BUCKET));
		crock.setItem(1, new ItemStack(CookingItems.BUTTERMILK));
		crock.setItem(kind.containerSlot, new ItemStack(Items.BOWL, 4));
		pass(helper, crock, CookingStation.DAY);
		helper.assertTrue(crock.getItem(kind.outputSlot).is(CookingItems.YOGURT) && crock.getItem(kind.outputSlot).getCount() == 4
			&& crock.getItem(kind.byproductSlots[0]).is(Items.BUCKET) && crock.getItem(kind.byproductSlots[1]).is(Items.GLASS_BOTTLE),
			"Buttermilk starts the first yogurt");

		crock.setItem(0, new ItemStack(CookingItems.WARM_MILK_BUCKET));
		crock.setItem(1, crock.getItem(kind.outputSlot).split(1));
		crock.setItem(kind.containerSlot, new ItemStack(Items.BOWL, 3));
		pass(helper, crock, CookingStation.DAY);
		helper.assertTrue(crock.getItem(kind.outputSlot).getCount() == 7, "A spoonful of yogurt starts the next batch");

		for (int slot = 0; slot < kind.size; slot++) crock.setItem(slot, ItemStack.EMPTY);
		crock.setItem(0, new ItemStack(CookingItems.CREAM, 2));
		crock.setItem(1, new ItemStack(CookingItems.BUTTERMILK));
		crock.setItem(kind.containerSlot, new ItemStack(Items.BOWL));
		pass(helper, crock, CookingStation.DAY);
		helper.assertTrue(crock.getItem(kind.outputSlot).is(CookingItems.CREME_FRAICHE)
			&& crock.getItem(kind.byproductSlots[0]).getCount() == 3, "Cream cultured with buttermilk gives crème fraîche");
		helper.succeed();
	}

	/** Lets a time station live through {@code ticks} of game time at once, as if its chunk had been unloaded. */
	static void pass(GameTestHelper helper, ProcessStationBlockEntity station, int ticks) {
		ProcessStationBlockEntity.serverTick(helper.getLevel(), station.getBlockPos(), station.getBlockState(), station);
		station.rewind(ticks);
		ProcessStationBlockEntity.serverTick(helper.getLevel(), station.getBlockPos(), station.getBlockState(), station);
	}

	static ProcessStationBlockEntity station(GameTestHelper helper, BlockPos pos, Block block) {
		helper.setBlock(pos, block);
		return (ProcessStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
	}
}
