package dev.portablepocketcabin;

import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.Arrays;
import java.util.List;

/** B-0052: the press, juices, oils, cordial and syrup, and the phase 2 frying recipes. */
public final class CabinPressGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);
	private static final CookingMenu.Kind PRESS = CookingMenu.Kind.PRESS;

	@GameTest
	public void pressRecipesTakeBottlesButNoLiquidOrHeat(GameTestHelper helper) {
		var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		String valid = "{\"type\":\"portable_pocket_cabin:press\",\"ingredients\":[{\"item\":\"minecraft:apple\",\"count\":4}],"
			+ "\"container\":{\"item\":\"minecraft:glass_bottle\"},\"time\":60,\"result\":{\"id\":\"minecraft:honey_bottle\"}}";
		var invalid = List.of(
			"{\"type\":\"portable_pocket_cabin:press\",\"ingredients\":[{\"item\":\"minecraft:apple\"}],\"water\":1,\"time\":60,\"result\":{\"id\":\"minecraft:honey_bottle\"}}",
			"{\"type\":\"portable_pocket_cabin:press\",\"ingredients\":[{\"item\":\"minecraft:apple\"}],\"oil\":1,\"time\":60,\"result\":{\"id\":\"minecraft:honey_bottle\"}}",
			"{\"type\":\"portable_pocket_cabin:press\",\"ingredients\":[{\"item\":\"minecraft:apple\"}],\"heat\":\"low\",\"time\":60,\"result\":{\"id\":\"minecraft:honey_bottle\"}}",
			"{\"type\":\"portable_pocket_cabin:press\",\"ingredients\":[{\"item\":\"minecraft:apple\"}],\"result\":{\"id\":\"minecraft:honey_bottle\"}}",
			"{\"type\":\"portable_pocket_cabin:press\",\"ingredients\":[{\"item\":\"minecraft:apple\"},{\"item\":\"minecraft:carrot\"}],\"time\":60,\"result\":{\"id\":\"minecraft:honey_bottle\"}}"
		);
		helper.assertTrue(Recipe.CODEC.parse(ops, com.google.gson.JsonParser.parseString(valid)).error().isEmpty(), "Accepted: " + valid);
		for (String text : invalid) {
			helper.assertTrue(Recipe.CODEC.parse(ops, com.google.gson.JsonParser.parseString(text)).error().isPresent(), "Rejected: " + text);
		}
		helper.succeed();
	}

	@GameTest
	public void pressFillsBottlesAndKeepsTheSolids(GameTestHelper helper) {
		var press = press(helper);
		press.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 4));
		tick(helper, press, 30);
		helper.assertTrue(press.getItem(PRESS.outputSlot).isEmpty() && press.progress() == 0, "The press waits for bottles");
		press.setItem(PRESS.containerSlot, new ItemStack(Items.GLASS_BOTTLE, 2));
		tick(helper, press, 20);
		helper.assertTrue(press.getItem(PRESS.outputSlot).is(Items.HONEY_BOTTLE) && press.getItem(PRESS.byproductSlot).is(Items.STICK),
			"Pressing fills a bottle and leaves the solids");
		helper.assertTrue(press.getItem(0).getCount() == 2 && press.getItem(PRESS.containerSlot).getCount() == 1,
			"Each pressing uses its ingredients and one bottle");
		tick(helper, press, 10);
		var copy = reload(helper, press);
		helper.assertTrue(copy.progress() == 10, "The press keeps its progress when saved, found " + copy.progress());
		press.setItem(PRESS.outputSlot, new ItemStack(Items.HONEY_BOTTLE, 64));
		tick(helper, press, 40);
		helper.assertTrue(press.getItem(0).getCount() == 2, "A full output stops the press");
		helper.succeed();
	}

	@GameTest
	public void hoppersFeedAndEmptyThePress(GameTestHelper helper) {
		var press = press(helper);
		helper.assertTrue(Arrays.equals(press.getSlotsForFace(Direction.UP), new int[] {0}), "Hoppers above feed the ingredient");
		helper.assertTrue(Arrays.equals(press.getSlotsForFace(Direction.NORTH), new int[] {PRESS.containerSlot}), "Side hoppers feed bottles");
		helper.assertTrue(Arrays.equals(press.getSlotsForFace(Direction.DOWN), new int[] {PRESS.outputSlot, PRESS.byproductSlot}),
			"Hoppers below take the liquid and the solids");
		helper.assertTrue(!press.canTakeItemThroughFace(0, new ItemStack(Items.WHEAT_SEEDS), Direction.DOWN),
			"Hoppers cannot take the ingredient back out");
		helper.succeed();
	}

	private static ProcessStationBlockEntity press(GameTestHelper helper) {
		helper.setBlock(BASE, CookingBlocks.PRESS);
		return (ProcessStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BASE));
	}

	private static void tick(GameTestHelper helper, ProcessStationBlockEntity station, int ticks) {
		for (int i = 0; i < ticks; i++) {
			ProcessStationBlockEntity.serverTick(helper.getLevel(), station.getBlockPos(), station.getBlockState(), station);
		}
	}

	private static ProcessStationBlockEntity reload(GameTestHelper helper, ProcessStationBlockEntity saved) {
		var registries = helper.getLevel().registryAccess();
		var fresh = ProcessStationBlockEntity.press(saved.getBlockPos(), saved.getBlockState());
		fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved.saveWithFullMetadata(registries)));
		return fresh;
	}
}
