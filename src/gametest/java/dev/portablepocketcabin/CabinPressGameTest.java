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

	@GameTest
	public void pressMakesJuicesAndTheirSolids(GameTestHelper helper) {
		var level = helper.getLevel();
		var grated = CookingStation.CUTTING.find(level, CookingInput.of(List.of(new ItemStack(CookingItems.CHOPPED_CARROT)))).orElseThrow();
		helper.assertTrue(grated.value().result().item().value() == CookingItems.GRATED_CARROT, "Chopped carrot grates on the cutting board");
		var press = press(helper);
		Object[][] juices = {
			{CookingItems.CHOPPED_APPLE, CookingItems.APPLE_JUICE, CookingItems.APPLE_POMACE},
			{Items.SWEET_BERRIES, CookingItems.SWEET_BERRY_JUICE, CookingItems.PULP},
			{Items.MELON_SLICE, CookingItems.MELON_JUICE, CookingItems.PULP},
			{CookingItems.GRATED_CARROT, CookingItems.CARROT_JUICE, CookingItems.PULP},
		};
		for (Object[] juice : juices) {
			for (int slot = 0; slot < PRESS.size; slot++) press.setItem(slot, ItemStack.EMPTY);
			press.setItem(0, new ItemStack((net.minecraft.world.item.Item) juice[0], 4));
			press.setItem(PRESS.containerSlot, new ItemStack(Items.GLASS_BOTTLE));
			tick(helper, press, 60);
			helper.assertTrue(press.getItem(PRESS.outputSlot).is((net.minecraft.world.item.Item) juice[1])
				&& press.getItem(PRESS.byproductSlot).is((net.minecraft.world.item.Item) juice[2]) && press.getItem(0).isEmpty(),
				"Four " + juice[0] + " press into " + juice[1] + " and " + juice[2]);
		}
		helper.succeed();
	}

	@GameTest
	public void juicesGiveTheBottleBackAndSolidsAreUseful(GameTestHelper helper) {
		var level = helper.getLevel();
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		for (var juice : List.of(CookingItems.APPLE_JUICE, CookingItems.SWEET_BERRY_JUICE, CookingItems.MELON_JUICE, CookingItems.CARROT_JUICE)) {
			var stack = new ItemStack(juice);
			helper.assertTrue(stack.has(net.minecraft.core.component.DataComponents.FOOD)
				&& stack.get(net.minecraft.core.component.DataComponents.FOOD).canAlwaysEat(), juice + " can be drunk when full");
			helper.assertTrue(stack.finishUsingItem(level, player).is(Items.GLASS_BOTTLE), juice + " gives the bottle back");
		}
		for (var solid : List.of(CookingItems.APPLE_POMACE, CookingItems.PULP, CookingItems.PRESS_CAKE)) {
			helper.assertTrue(net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.getFloat(solid) > 0, solid + " composts");
		}
		var cake = new ItemStack(CookingItems.PRESS_CAKE);
		var cow = helper.spawn(net.minecraft.world.entity.EntityTypes.COW, BASE.above());
		var sheep = helper.spawn(net.minecraft.world.entity.EntityTypes.SHEEP, BASE.above());
		var goat = helper.spawn(net.minecraft.world.entity.EntityTypes.GOAT, BASE.above());
		var pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, BASE.above());
		helper.assertTrue(cow.isFood(cake) && sheep.isFood(cake) && goat.isFood(cake), "Press cake feeds cows, sheep, and goats");
		helper.assertTrue(!pig.isFood(cake), "Pigs do not eat press cake");
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
