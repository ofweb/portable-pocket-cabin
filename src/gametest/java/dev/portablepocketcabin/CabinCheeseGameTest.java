package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static dev.portablepocketcabin.CabinDairyGameTest.pass;
import static dev.portablepocketcabin.CabinDairyGameTest.station;

/** B-0046 and B-0048 cheese: cardoon rennet, curds and whey, and every cheese from fresh to aged. */
public final class CabinCheeseGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void cardoonGrowsFlowersAndGenerates(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(BASE, CookingBlocks.CARDOON);
		var pos = helper.absolutePos(BASE);
		var young = Block.getDrops(level.getBlockState(pos), level, pos, null);
		helper.assertTrue(young.size() == 1 && young.getFirst().is(CookingBlocks.CARDOON.asItem()), "A young cardoon drops only itself");
		var cardoon = (CardoonBlock) CookingBlocks.CARDOON;
		for (int i = 0; i < 3; i++) cardoon.performBonemeal(level, level.getRandom(), pos, level.getBlockState(pos));
		helper.assertTrue(level.getBlockState(pos).getValue(CardoonBlock.AGE) == CardoonBlock.MAX_AGE, "Bone meal grows it to flower");
		var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
		helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(CookingItems.CARDOON_FLOWER))
			&& drops.stream().anyMatch(stack -> stack.is(CookingBlocks.CARDOON.asItem())), "A mature cardoon drops flowers and itself");
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(CookingWorld.CARDOON_PATCH).isPresent(),
			"Wild cardoon patches load");
		var pig = helper.spawn(EntityTypes.PIG, BASE.east(2));
		helper.assertTrue(pig.isFood(new ItemStack(CookingItems.WHEY)), "Pigs eat whey");
		helper.succeed();
	}

	@GameTest
	public void rennetSetsCurdsThatPressBrineAndAge(GameTestHelper helper) {
		var level = helper.getLevel();
		var crock = station(helper, BASE, CookingBlocks.CROCK);
		crock.setItem(0, new ItemStack(CookingItems.CARDOON_FLOWER, 3));
		crock.setItem(1, new ItemStack(CookingItems.SALT));
		crock.setItem(2, water());
		pass(helper, crock, CookingStation.DAY);
		var rennet = crock.getItem(CookingMenu.Kind.CROCK.outputSlot);
		helper.assertTrue(rennet.is(CookingItems.VEGETABLE_RENNET), "Cardoon flowers steep into rennet");

		helper.setBlock(BASE.east(2), CookingBlocks.STOVE);
		helper.setBlock(BASE.east(2).above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2)));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2).above()));
		pot.setItem(0, new ItemStack(Items.MILK_BUCKET));
		pot.setItem(1, new ItemStack(Items.MILK_BUCKET));
		pot.setItem(2, rennet.copy());
		stove.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
		for (int i = 0; i < 205; i++) {
			StoveBlockEntity.serverTick(level, stove.getBlockPos(), stove.getBlockState(), stove);
			CookingPotBlockEntity.serverTick(level, pot.getBlockPos(), pot.getBlockState(), pot);
		}
		var curds = pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(curds.is(CookingItems.CURDS) && curds.getCount() == 4, "Warm milk and rennet set four curds");
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.BYPRODUCT_SLOT).getCount() == 2
			&& pot.getItem(CookingPotBlockEntity.SECOND_BYPRODUCT_SLOT).is(CookingItems.WHEY), "The buckets come back with the whey");

		var press = station(helper, BASE.south(2), CookingBlocks.CHEESE_PRESS);
		press.setItem(0, new ItemStack(CookingItems.CURDS, 4));
		for (int i = 0; i < 400; i++) ProcessStationBlockEntity.serverTick(level, press.getBlockPos(), press.getBlockState(), press);
		helper.assertTrue(press.getItem(CookingMenu.Kind.CHEESE_PRESS.outputSlot).is(CookingItems.PRESSED_CHEESE), "Four curds press hard");
		press.setItem(CookingMenu.Kind.CHEESE_PRESS.outputSlot, ItemStack.EMPTY);
		press.setItem(0, new ItemStack(CookingItems.CURDS, 2));
		press.setItem(1, new ItemStack(CookingItems.SALT));
		for (int i = 0; i < 100; i++) ProcessStationBlockEntity.serverTick(level, press.getBlockPos(), press.getBlockState(), press);
		helper.assertTrue(press.getItem(CookingMenu.Kind.CHEESE_PRESS.outputSlot).is(CookingItems.FRESH_CHEESE), "Salted curds press lightly into fresh cheese");

		var barrel = station(helper, BASE.south(4), CookingBlocks.BRINE_BARREL);
		barrel.setItem(0, new ItemStack(CookingItems.PRESSED_CHEESE));
		barrel.setItem(1, new ItemStack(CookingItems.SALT));
		barrel.setItem(2, water());
		pass(helper, barrel, CookingStation.DAY);
		helper.assertTrue(barrel.getItem(CookingMenu.Kind.BRINE.outputSlot).is(CookingItems.BRINED_CHEESE), "Pressed cheese brines for a day");

		helper.setBlock(BASE.east(4), CookingBlocks.AGEING_SHELF);
		var shelf = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(4)));
		shelf.add(level, new ItemStack(CookingItems.BRINED_CHEESE));
		shelf.add(level, new ItemStack(CookingItems.SOFT_CHEESE_CURDS));
		SpotStationBlockEntity.serverTick(level, shelf.getBlockPos(), shelf.getBlockState(), shelf);
		shelf.rewind(2 * CookingStation.DAY);
		SpotStationBlockEntity.serverTick(level, shelf.getBlockPos(), shelf.getBlockState(), shelf);
		helper.assertTrue(shelf.getItem(0).is(CookingItems.BRINED_CHEESE) && shelf.getItem(1).is(CookingItems.AGED_SOFT_CHEESE),
			"After two days the soft cheese is ripe and the hard cheese still ages");
		shelf.rewind(CookingStation.DAY);
		SpotStationBlockEntity.serverTick(level, shelf.getBlockPos(), shelf.getBlockState(), shelf);
		helper.assertTrue(shelf.getItem(0).is(CookingItems.AGED_HARD_CHEESE), "The hard cheese is aged after three days");
		var wedges = CookingStation.CUTTING.find(level, CookingInput.of(List.of(new ItemStack(CookingItems.AGED_HARD_CHEESE)))).orElseThrow();
		helper.assertTrue(wedges.value().result().count() == 4, "An aged cheese cuts into four wedges");
		helper.succeed();
	}

	@GameTest
	public void culturedMilkAndCreamDrainIntoSoftCheeses(GameTestHelper helper) {
		var level = helper.getLevel();
		var crock = station(helper, BASE, CookingBlocks.CROCK);
		crock.setItem(0, new ItemStack(CookingItems.SKIMMED_MILK_BUCKET));
		crock.setItem(1, new ItemStack(CookingItems.BUTTERMILK));
		pass(helper, crock, CookingStation.DAY);
		helper.assertTrue(crock.getItem(CookingMenu.Kind.CROCK.outputSlot).is(CookingItems.SOURED_MILK_BUCKET), "Skimmed milk sours with buttermilk");

		helper.setBlock(BASE.east(2), CookingBlocks.PREP_COUNTER);
		var strainer = station(helper, BASE.east(2).above(), CookingBlocks.STRAINER);
		var kind = CookingMenu.Kind.STRAINER;
		strainer.setItem(0, new ItemStack(CookingItems.SOURED_MILK_BUCKET));
		strainer.setItem(kind.containerSlot, new ItemStack(Items.BOWL));
		strain(helper, strainer);
		helper.assertTrue(strainer.getItem(kind.outputSlot).is(CookingItems.QUARK) && strainer.getItem(kind.byproductSlot).is(Items.BUCKET),
			"Soured milk drains into quark");

		for (int slot = 0; slot < kind.size; slot++) strainer.setItem(slot, ItemStack.EMPTY);
		strainer.setItem(0, new ItemStack(CookingItems.CREME_FRAICHE));
		strain(helper, strainer);
		helper.assertTrue(strainer.getItem(kind.outputSlot).is(CookingItems.DRAINED_CREAM), "Crème fraîche drains thick");

		for (int slot = 0; slot < kind.size; slot++) strainer.setItem(slot, ItemStack.EMPTY);
		strainer.setItem(0, new ItemStack(CookingItems.CURDS, 2));
		strainer.setItem(1, new ItemStack(CookingItems.SALT));
		strain(helper, strainer);
		helper.assertTrue(strainer.getItem(kind.outputSlot).is(CookingItems.SOFT_CHEESE_CURDS), "Salted curds drain for a soft cheese");

		helper.setBlock(BASE.south(2), CookingBlocks.PREP_COUNTER);
		var bowl = station(helper, BASE.south(2).above(), CookingBlocks.MIXING_BOWL);
		bowl.setItem(0, new ItemStack(CookingItems.DRAINED_CREAM));
		bowl.setItem(1, new ItemStack(CookingItems.SALT));
		for (int i = 0; i < 3; i++) bowl.work(level);
		helper.assertTrue(bowl.getItem(CookingMenu.Kind.MIXING_BOWL.outputSlot).is(CookingItems.CREAM_CHEESE), "Salted drained cream is cream cheese");
		helper.succeed();
	}

	private static void strain(GameTestHelper helper, ProcessStationBlockEntity strainer) {
		for (int i = 0; i < 100; i++) {
			ProcessStationBlockEntity.serverTick(helper.getLevel(), strainer.getBlockPos(), strainer.getBlockState(), strainer);
		}
	}

	private static ItemStack water() {
		return PotionContents.createItemStack(Items.POTION, Potions.WATER);
	}
}
