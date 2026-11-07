package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;

import static dev.portablepocketcabin.CabinDairyGameTest.pass;
import static dev.portablepocketcabin.CabinDairyGameTest.station;

/** B-0048: cider and vinegar, pickles, drying, curing, and smoking. */
public final class CabinPreservingGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void appleJuiceTurnsToCiderAndVinegarThenPickles(GameTestHelper helper) {
		var crock = station(helper, BASE, CookingBlocks.CROCK);
		var kind = CookingMenu.Kind.CROCK;
		crock.setItem(0, new ItemStack(CookingItems.APPLE_JUICE, 4));
		pass(helper, crock, 2 * CookingStation.DAY);
		helper.assertTrue(crock.getItem(kind.outputSlot).is(CookingItems.CIDER) && crock.getItem(kind.outputSlot).getCount() == 4,
			"Apple juice ferments into cider in two days");

		crock.setItem(0, crock.removeItem(kind.outputSlot, 4));
		pass(helper, crock, 2 * CookingStation.DAY);
		helper.assertTrue(crock.getItem(kind.outputSlot).isEmpty(), "Without a mother, two days are not enough");
		pass(helper, crock, 3 * CookingStation.DAY);
		helper.assertTrue(crock.getItem(kind.outputSlot).is(CookingItems.CIDER_VINEGAR), "Cider left open sours into the first vinegar");

		crock.setItem(0, new ItemStack(CookingItems.CIDER, 3));
		crock.setItem(1, new ItemStack(CookingItems.CIDER_VINEGAR));
		crock.setItem(kind.outputSlot, ItemStack.EMPTY);
		pass(helper, crock, 2 * CookingStation.DAY);
		helper.assertTrue(crock.getItem(kind.outputSlot).getCount() == 4 && crock.getItem(0).isEmpty(), "A splash of vinegar sours cider in two days");

		var level = helper.getLevel();
		var sliced = CookingStation.CUTTING.find(level, CookingInput.of(List.of(new ItemStack(CookingItems.BOILED_BEETROOT)))).orElseThrow();
		helper.assertTrue(sliced.value().result().item().value() == CookingItems.SLICED_BEETROOT, "Boiled beetroot slices on the board");
		var pickles = station(helper, BASE.east(2), CookingBlocks.CROCK);
		pickles.setItem(0, new ItemStack(CookingItems.SLICED_BEETROOT, 4));
		pickles.setItem(1, new ItemStack(CookingItems.CIDER_VINEGAR));
		pickles.setItem(2, new ItemStack(Items.SUGAR));
		pass(helper, pickles, CookingStation.DAY);
		helper.assertTrue(pickles.getItem(kind.outputSlot).is(CookingItems.PICKLED_BEETROOT)
			&& pickles.getItem(kind.byproductSlot).is(Items.GLASS_BOTTLE), "Beetroot pickles in vinegar and sugar");
		for (int slot = 0; slot < kind.size; slot++) pickles.setItem(slot, ItemStack.EMPTY);
		pickles.setItem(0, new ItemStack(CookingItems.CHOPPED_CARROT, 4));
		pickles.setItem(1, new ItemStack(CookingItems.CIDER_VINEGAR));
		pickles.setItem(2, new ItemStack(CookingItems.SALT));
		pass(helper, pickles, CookingStation.DAY);
		helper.assertTrue(pickles.getItem(kind.outputSlot).is(CookingItems.PICKLED_CARROTS), "Carrots pickle in vinegar and salt");
		helper.succeed();
	}

	@GameTest
	public void fishAndMeatCureDryAndSmoke(GameTestHelper helper) {
		var level = helper.getLevel();
		var barrel = station(helper, BASE, CookingBlocks.BRINE_BARREL);
		var kind = CookingMenu.Kind.BRINE;
		barrel.setItem(0, new ItemStack(Items.COD, 2));
		barrel.setItem(1, new ItemStack(CookingItems.SALT));
		pass(helper, barrel, CookingStation.DAY);
		helper.assertTrue(barrel.getItem(kind.outputSlot).is(CookingItems.SALTED_COD), "Cod salts in the barrel");

		var strips = CookingStation.CUTTING.find(level, CookingInput.of(List.of(new ItemStack(Items.BEEF)))).orElseThrow();
		helper.assertTrue(strips.value().result().count() == 2, "Beef cuts into two strips");
		for (int slot = 0; slot < kind.size; slot++) barrel.setItem(slot, ItemStack.EMPTY);
		barrel.setItem(0, new ItemStack(CookingItems.BEEF_STRIPS, 4));
		barrel.setItem(1, new ItemStack(CookingItems.SALT));
		pass(helper, barrel, CookingStation.DAY);
		helper.assertTrue(barrel.getItem(kind.outputSlot).is(CookingItems.CURED_BEEF_STRIPS), "Strips cure with salt");

		helper.setBlock(BASE.east(2), CookingBlocks.DRYING_RACK);
		var rack = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2)));
		for (var item : List.of(CookingItems.SALTED_COD, CookingItems.CURED_BEEF_STRIPS, CookingItems.CHOPPED_APPLE, Items.KELP)) {
			helper.assertTrue(rack.add(level, new ItemStack(item)), item + " goes on the rack");
		}
		SpotStationBlockEntity.serverTick(level, rack.getBlockPos(), rack.getBlockState(), rack);
		rack.rewind(3 * CookingStation.DAY);
		SpotStationBlockEntity.serverTick(level, rack.getBlockPos(), rack.getBlockState(), rack);
		helper.assertTrue(rack.getItem(0).is(CookingItems.SALT_COD) && rack.getItem(1).is(CookingItems.JERKY)
			&& rack.getItem(2).is(CookingItems.DRIED_APPLE) && rack.getItem(3).is(Items.DRIED_KELP), "Each item dries on its own spot");

		for (int slot = 0; slot < kind.size; slot++) barrel.setItem(slot, ItemStack.EMPTY);
		barrel.setItem(0, new ItemStack(Items.SALMON, 2));
		barrel.setItem(1, new ItemStack(CookingItems.SALT));
		barrel.setItem(2, new ItemStack(CookingItems.BROWN_SUGAR));
		pass(helper, barrel, CookingStation.DAY);
		helper.assertTrue(barrel.getItem(kind.outputSlot).is(CookingItems.CURED_SALMON), "Salmon cures with salt and sugar");
		for (int slot = 0; slot < kind.size; slot++) barrel.setItem(slot, ItemStack.EMPTY);
		barrel.setItem(0, new ItemStack(Items.PORKCHOP, 2));
		barrel.setItem(1, new ItemStack(CookingItems.SALT));
		barrel.setItem(2, PotionContents.createItemStack(Items.POTION, Potions.WATER));
		pass(helper, barrel, CookingStation.DAY);
		helper.assertTrue(barrel.getItem(kind.outputSlot).is(CookingItems.BRINED_PORK) && barrel.getItem(kind.byproductSlot).is(Items.GLASS_BOTTLE),
			"Pork brines in salt water");

		var manager = level.getServer().getRecipeManager();
		var salmon = manager.getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(new ItemStack(CookingItems.CURED_SALMON)), level).orElseThrow();
		var pork = manager.getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(new ItemStack(CookingItems.BRINED_PORK)), level).orElseThrow();
		helper.assertTrue(salmon.value().assemble(new SingleRecipeInput(new ItemStack(CookingItems.CURED_SALMON))).is(CookingItems.SMOKED_SALMON)
			&& pork.value().assemble(new SingleRecipeInput(new ItemStack(CookingItems.BRINED_PORK))).is(CookingItems.SMOKED_PORK),
			"The smoker smokes cured salmon and brined pork");
		helper.assertTrue(manager.getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(new ItemStack(Items.SALMON)), level).isEmpty(),
			"Raw salmon still does not cook in the smoker");
		helper.succeed();
	}
}
