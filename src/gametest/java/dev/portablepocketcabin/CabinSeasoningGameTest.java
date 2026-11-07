package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;

import static dev.portablepocketcabin.CabinDairyGameTest.station;

/** B-0047 seasoning: tastes, seasoning slots, and dish quality. */
public final class CabinSeasoningGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);
	private static final int[] SEASONING = CookingMenu.Kind.POT.seasoningSlots;

	@GameTest
	public void qualityRewardsWantedTastesOnce(GameTestHelper helper) {
		var soup = List.of(Seasoning.Taste.SALTY, Seasoning.Taste.UMAMI);
		helper.assertTrue(Seasoning.quality(soup, List.of()) == 0, "Without seasoning a dish is plain");
		helper.assertTrue(Seasoning.quality(soup, List.of(new ItemStack(CookingItems.SALT))) == Seasoning.SEASONED, "Salt seasons a soup");
		helper.assertTrue(Seasoning.quality(soup, List.of(new ItemStack(Items.DRIED_KELP))) == Seasoning.WELL_BALANCED,
			"Dried kelp brings both salt and umami, balancing the soup");
		helper.assertTrue(Seasoning.quality(soup, List.of(new ItemStack(CookingItems.SALT), new ItemStack(CookingItems.COARSE_SALT))) == 0,
			"A second dose of salt undoes the first");
		helper.assertTrue(Seasoning.quality(soup, List.of(new ItemStack(CookingItems.SALT), new ItemStack(CookingItems.CIDER_VINEGAR))) == 0,
			"An unwanted sour taste spoils the seasoning");
		helper.assertTrue(Seasoning.tastes(new ItemStack(CookingItems.NIGARI)).contains(Seasoning.Taste.BITTER), "Nigari is bitter");
		helper.succeed();
	}

	@GameTest
	public void potSeasonsSoupAndKeepsQualitiesApart(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		helper.assertTrue(!pot.canPlaceItem(SEASONING[0], new ItemStack(Items.CARROT)) && pot.canPlaceItem(SEASONING[0], new ItemStack(CookingItems.SALT)),
			"Only seasonings go in a seasoning slot");
		pot.fillWater();
		pot.setItem(SEASONING[0], new ItemStack(CookingItems.SALT, 2));
		pot.setItem(SEASONING[1], new ItemStack(CookingItems.CIDER_VINEGAR));
		pot.setItem(0, new ItemStack(Items.MILK_BUCKET));
		cook(helper, stove, pot, 105);
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.WARM_MILK_BUCKET)
			&& pot.getItem(SEASONING[0]).getCount() == 2, "A recipe without tastes leaves the seasonings alone");

		pot.setItem(CookingPotBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		pot.setItem(SEASONING[1], ItemStack.EMPTY);
		soup(pot);
		cook(helper, stove, pot, 305);
		var made = pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(made.is(CookingItems.VEGETABLE_SOUP) && made.getCount() == 3 && made.get(Seasoning.DISH_QUALITY) == Seasoning.SEASONED,
			"Salt seasons the whole batch of soup");
		helper.assertTrue(pot.getItem(SEASONING[0]).getCount() == 1, "One salt seasons one batch");
		float plain = new ItemStack(CookingItems.VEGETABLE_SOUP).get(DataComponents.FOOD).saturation();
		helper.assertTrue(made.get(DataComponents.FOOD).saturation() > plain, "Seasoned soup is more filling");

		pot.setItem(SEASONING[0], ItemStack.EMPTY);
		soup(pot);
		cook(helper, stove, pot, 305);
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).getCount() == 3 && pot.getItem(0).getCount() == 2,
			"Plain soup waits rather than stacking with seasoned soup");

		pot.setItem(CookingPotBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		pot.setItem(SEASONING[0], new ItemStack(CookingItems.SALT));
		pot.setItem(SEASONING[1], new ItemStack(CookingItems.CIDER_VINEGAR));
		cook(helper, stove, pot, 305);
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).get(Seasoning.DISH_QUALITY) == null,
			"Salt and vinegar cancel out, leaving plain soup");
		boolean bottle = pot.getItem(CookingPotBlockEntity.BYPRODUCT_SLOT).is(Items.GLASS_BOTTLE)
			|| pot.getItem(CookingPotBlockEntity.SECOND_BYPRODUCT_SLOT).is(Items.GLASS_BOTTLE);
		helper.assertTrue(bottle && pot.getItem(SEASONING[1]).isEmpty(), "A splash of vinegar gives its bottle back");
		helper.succeed();
	}

	@GameTest
	public void counterSeasonsSkilletDishesAndFillSkipsSeasonings(GameTestHelper helper) {
		var level = helper.getLevel();
		var counter = station(helper, BASE, CookingBlocks.PREP_COUNTER);
		var kind = CookingMenu.Kind.COUNTER;
		counter.setItem(0, new ItemStack(Items.COOKED_BEEF));
		counter.setItem(kind.seasoningSlots[0], new ItemStack(Items.DRIED_KELP));
		for (int i = 0; i < 20; i++) ProcessStationBlockEntity.serverTick(level, counter.getBlockPos(), counter.getBlockState(), counter);
		var beef = counter.getItem(kind.outputSlot);
		helper.assertTrue(beef.is(Items.COOKED_BEEF) && beef.get(Seasoning.DISH_QUALITY) == Seasoning.WELL_BALANCED,
			"Seared beef seasoned with dried kelp on the counter is well balanced");

		helper.setBlock(BASE.east(2), CookingBlocks.COOKING_POT);
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2)));
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		var inventory = player.getInventory();
		inventory.clearContent();
		inventory.add(new ItemStack(CookingItems.CHOPPED_CARROT));
		inventory.add(new ItemStack(CookingItems.DICED_POTATO));
		inventory.add(new ItemStack(CookingItems.CHOPPED_BEETROOT));
		inventory.add(new ItemStack(Items.BOWL, 3));
		inventory.add(new ItemStack(CookingItems.SALT, 4));
		var recipe = (CookingRecipe) level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
			net.minecraft.core.registries.Registries.RECIPE, PortablePocketCabin.id("cooking/pot/vegetable_soup"))).orElseThrow().value();
		helper.assertTrue(CookingFill.fill(pot, CookingStation.POT, recipe, inventory), "The soup fills from the inventory");
		helper.assertTrue(pot.getItem(SEASONING[0]).isEmpty() && pot.getItem(SEASONING[1]).isEmpty() && inventory.countItem(CookingItems.SALT) == 4,
			"Filling never adds seasonings");
		helper.succeed();
	}

	private static void soup(CookingPotBlockEntity pot) {
		pot.setItem(0, new ItemStack(CookingItems.CHOPPED_CARROT, 2));
		pot.setItem(1, new ItemStack(CookingItems.DICED_POTATO, 2));
		pot.setItem(2, new ItemStack(CookingItems.CHOPPED_BEETROOT, 2));
		pot.setItem(CookingPotBlockEntity.CONTAINER_SLOT, new ItemStack(Items.BOWL, 6));
		if (pot.water() == 0) pot.fillWater();
	}

	private static void cook(GameTestHelper helper, StoveBlockEntity stove, CookingPotBlockEntity pot, int ticks) {
		for (int i = 0; i < ticks; i++) {
			if (stove.getItem(0).isEmpty() && !stove.burner().burning()) stove.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
			StoveBlockEntity.serverTick(helper.getLevel(), stove.getBlockPos(), stove.getBlockState(), stove);
			CookingPotBlockEntity.serverTick(helper.getLevel(), pot.getBlockPos(), pot.getBlockState(), pot);
		}
	}
}
