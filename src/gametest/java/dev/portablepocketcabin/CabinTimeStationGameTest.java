package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;

/** Phase 3 time stations: the crock, brine barrel, proofing basket, dairy pan, ageing shelf, and drying rack. */
public final class CabinTimeStationGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void crockCatchesUpOneBatchAfterBeingAway(GameTestHelper helper) {
		var level = helper.getLevel();
		var crock = station(helper, BASE, CookingBlocks.CROCK);
		var kind = CookingMenu.Kind.CROCK;
		crock.setItem(0, new ItemStack(Items.WHEAT, 4));
		crock.setItem(1, PotionContents.createItemStack(Items.POTION, Potions.WATER));
		crock.setItem(2, PotionContents.createItemStack(Items.POTION, Potions.WATER));
		crock.setItem(kind.containerSlot, new ItemStack(Items.BOWL, 2));
		tick(helper, crock);
		helper.assertTrue(crock.progress() == 1, "A loaded crock progresses a tick at a time");
		crock.rewind(3 * CookingStation.DAY);
		tick(helper, crock);
		helper.assertTrue(crock.getItem(kind.outputSlot).is(Items.BREAD) && crock.getItem(kind.outputSlot).getCount() == 1,
			"Three days away finish the batch that was running");
		helper.assertTrue(crock.progress() == 0 && crock.getItem(0).getCount() == 2, "The next batch waits for live time");
		helper.assertTrue(crock.getItem(kind.byproductSlots[0]).is(Items.GLASS_BOTTLE) && crock.getItem(kind.byproductSlots[1]).is(Items.STICK),
			"The crock fills both byproduct slots");

		var copy = (ProcessStationBlockEntity) reload(helper, crock);
		tick(helper, crock);
		tick(helper, copy);
		helper.assertTrue(copy.progress() == crock.progress(), "The saved time survives saving and loading");
		helper.succeed();
	}

	@GameTest
	public void otherSingleBatchStationsCountGameTime(GameTestHelper helper) {
		var pan = station(helper, BASE, CookingBlocks.DAIRY_PAN);
		pan.setItem(0, new ItemStack(Items.MILK_BUCKET));
		pan.setItem(CookingMenu.Kind.DAIRY_PAN.containerSlot, new ItemStack(Items.GLASS_BOTTLE, 2));
		var barrel = station(helper, BASE.east(2), CookingBlocks.BRINE_BARREL);
		barrel.setItem(0, new ItemStack(Items.COD, 2));
		barrel.setItem(1, new ItemStack(Items.SUGAR));
		var basket = station(helper, BASE.south(2), CookingBlocks.PROOFING_BASKET);
		basket.setItem(0, new ItemStack(Items.WHEAT));
		for (var station : new ProcessStationBlockEntity[] {pan, barrel, basket}) {
			tick(helper, station);
			station.rewind(CookingStation.DAY);
			tick(helper, station);
		}
		helper.assertTrue(pan.getItem(CookingMenu.Kind.DAIRY_PAN.outputSlot).getCount() == 2
			&& pan.getItem(CookingMenu.Kind.DAIRY_PAN.byproductSlot).is(Items.BUCKET), "Milk settles in the dairy pan");
		helper.assertTrue(barrel.getItem(CookingMenu.Kind.BRINE.outputSlot).is(Items.COOKED_COD), "Fish cures in the brine barrel");
		helper.assertTrue(basket.getItem(CookingMenu.Kind.PROOFING.outputSlot).is(Items.BREAD), "Dough proofs in the basket");
		helper.succeed();
	}

	@GameTest
	public void shelfAgesEachSpotAndTalksToHoppers(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.AGEING_SHELF);
		var shelf = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		helper.assertTrue(shelf.canPlaceItemThroughFace(0, new ItemStack(Items.COBBLESTONE), Direction.UP), "Hoppers add what a recipe ages");
		helper.assertTrue(!shelf.canPlaceItemThroughFace(0, new ItemStack(Items.STICK), Direction.UP), "Hoppers cannot add other items");
		helper.assertTrue(shelf.add(level, new ItemStack(Items.COBBLESTONE)) && shelf.add(level, new ItemStack(Items.COBBLESTONE)),
			"Items go on the shelf by hand");
		helper.assertTrue(!shelf.canTakeItemThroughFace(0, shelf.getItem(0), Direction.DOWN), "Hoppers leave unfinished items");
		SpotStationBlockEntity.serverTick(level, shelf.getBlockPos(), shelf.getBlockState(), shelf);
		shelf.rewind(3 * CookingStation.DAY);
		SpotStationBlockEntity.serverTick(level, shelf.getBlockPos(), shelf.getBlockState(), shelf);
		helper.assertTrue(shelf.getItem(0).is(Items.MOSSY_COBBLESTONE) && shelf.getItem(1).is(Items.MOSSY_COBBLESTONE),
			"Every spot finishes its item after time away");
		helper.assertTrue(shelf.canTakeItemThroughFace(0, shelf.getItem(0), Direction.DOWN), "Hoppers take finished items");
		helper.assertTrue(shelf.take(level).is(Items.MOSSY_COBBLESTONE), "A finished item comes off by hand");
		helper.succeed();
	}

	@GameTest
	public void dryingRackUsesSunOrShade(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.DRYING_RACK);
		var rack = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		boolean sky = SpotStationBlockEntity.openSky(level, helper.absolutePos(BASE));
		helper.assertTrue(rack.add(level, new ItemStack(Items.ROTTEN_FLESH)), "Flesh goes on the rack");
		dry(helper, rack, 400);
		helper.assertTrue(rack.getItem(0).is(sky ? Items.LEATHER : Items.STRING), "The rack follows its own sky");
		rack.take(level);
		helper.setBlock(BASE.above(), Blocks.STONE);
		helper.assertTrue(rack.add(level, new ItemStack(Items.ROTTEN_FLESH)), "Flesh goes on the covered rack");
		dry(helper, rack, 150);
		helper.assertTrue(rack.getItem(0).is(Items.ROTTEN_FLESH) && rack.progress(0) > 0, "Under a roof, the slower shade recipe runs, found " + rack.getItem(0) + " at " + rack.progress(0) + ", sky at first " + sky
			+ ", now " + SpotStationBlockEntity.openSky(level, helper.absolutePos(BASE)));
		dry(helper, rack, 200);
		helper.assertTrue(rack.getItem(0).is(Items.STRING), "The shade recipe finishes");
		helper.succeed();
	}

	private static void dry(GameTestHelper helper, SpotStationBlockEntity rack, int ticks) {
		SpotStationBlockEntity.serverTick(helper.getLevel(), rack.getBlockPos(), rack.getBlockState(), rack);
		rack.rewind(ticks);
		SpotStationBlockEntity.serverTick(helper.getLevel(), rack.getBlockPos(), rack.getBlockState(), rack);
	}

	private static ProcessStationBlockEntity station(GameTestHelper helper, BlockPos pos, Block block) {
		helper.setBlock(pos, block);
		return (ProcessStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
	}

	private static void tick(GameTestHelper helper, ProcessStationBlockEntity station) {
		ProcessStationBlockEntity.serverTick(helper.getLevel(), station.getBlockPos(), station.getBlockState(), station);
	}

	private static net.minecraft.world.level.block.entity.BlockEntity reload(GameTestHelper helper, ProcessStationBlockEntity saved) {
		var registries = helper.getLevel().registryAccess();
		var fresh = new ProcessStationBlockEntity(CookingBlocks.CROCK_ENTITY, saved.getBlockPos(), saved.getBlockState(), CookingMenu.Kind.CROCK);
		fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved.saveWithFullMetadata(registries)));
		return fresh;
	}
}
