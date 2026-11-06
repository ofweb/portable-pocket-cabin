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

	@GameTest
	public void sunflowerSeedsPressIntoFryingOil(GameTestHelper helper) {
		var level = helper.getLevel();
		var threshed = CookingStation.CUTTING.find(level, CookingInput.of(List.of(new ItemStack(Items.SUNFLOWER)))).orElseThrow();
		helper.assertTrue(threshed.value().result().item().value() == CookingItems.SUNFLOWER_SEEDS && threshed.value().result().count() == 4,
			"A sunflower threshes into four seeds");
		var press = press(helper);
		press.setItem(0, new ItemStack(CookingItems.SUNFLOWER_SEEDS, 4));
		press.setItem(PRESS.containerSlot, new ItemStack(Items.GLASS_BOTTLE));
		tick(helper, press, 60);
		var oil = press.getItem(PRESS.outputSlot);
		helper.assertTrue(oil.is(CookingItems.SUNFLOWER_OIL) && press.getItem(PRESS.byproductSlot).is(CookingItems.PRESS_CAKE),
			"Sunflower seeds press into oil and press cake");

		helper.setBlock(BASE.offset(2, 0, 0), CookingBlocks.STOVE);
		helper.setBlock(BASE.offset(2, 1, 0), CookingBlocks.WOK);
		var wok = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 1, 0)));
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		use(helper, player, oil.copy(), BASE.offset(2, 1, 0));
		helper.assertTrue(wok.oil() == 1, "Sunflower oil fills the wok");
		helper.assertTrue(player.getInventory().contains(new ItemStack(Items.GLASS_BOTTLE)), "Pouring the oil gives the bottle back");
		helper.succeed();
	}

	@GameTest
	public void toastedPumpkinSeedsPressIntoDressingOil(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.offset(2, 0, 0), CookingBlocks.STOVE);
		helper.setBlock(BASE.offset(2, 1, 0), CookingBlocks.SKILLET);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 0, 0)));
		var skillet = (SkilletBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 1, 0)));
		stove.setItem(0, new ItemStack(Items.COAL));
		helper.assertTrue(skillet.add(level, new ItemStack(Items.PUMPKIN_SEEDS)), "Pumpkin seeds go on the skillet");
		for (int i = 0; i < 110; i++) {
			StoveBlockEntity.serverTick(level, stove.getBlockPos(), stove.getBlockState(), stove);
			SkilletBlockEntity.serverTick(level, skillet.getBlockPos(), skillet.getBlockState(), skillet);
		}
		helper.assertTrue(skillet.getItem(0).is(CookingItems.TOASTED_PUMPKIN_SEEDS), "Pumpkin seeds toast at medium heat");

		var press = press(helper);
		press.setItem(0, new ItemStack(CookingItems.TOASTED_PUMPKIN_SEEDS, 4));
		press.setItem(PRESS.containerSlot, new ItemStack(Items.GLASS_BOTTLE));
		tick(helper, press, 60);
		var oil = press.getItem(PRESS.outputSlot);
		helper.assertTrue(oil.is(CookingItems.PUMPKIN_SEED_OIL), "Toasted seeds press into pumpkin seed oil");

		helper.setBlock(BASE.offset(0, 0, 2), CookingBlocks.COOKING_POT);
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(0, 0, 2)));
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		use(helper, player, oil.copy(), BASE.offset(0, 0, 2));
		helper.assertTrue(pot.oil() == 0, "Pumpkin seed oil is not a frying oil, so it does not fill the pot");
		helper.succeed();
	}

	@GameTest
	public void sunflowerOilDeepFriesPotatoesAndStirFriesVegetables(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		use(helper, player, new ItemStack(CookingItems.SUNFLOWER_OIL), BASE.above());
		use(helper, player, new ItemStack(CookingItems.SUNFLOWER_OIL), BASE.above());
		pot.setItem(0, new ItemStack(CookingItems.DICED_POTATO, 4));
		stove.setItem(0, new ItemStack(Items.BLAZE_ROD));
		for (int i = 0; i < 105; i++) {
			StoveBlockEntity.serverTick(level, stove.getBlockPos(), stove.getBlockState(), stove);
			CookingPotBlockEntity.serverTick(level, pot.getBlockPos(), pot.getBlockState(), pot);
		}
		var fried = pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(fried.is(CookingItems.FRIED_POTATOES) && fried.getCount() == 4 && pot.oil() == 1,
			"Diced potatoes deep-fry in sunflower oil and use one level");

		helper.setBlock(BASE.offset(2, 0, 0), CookingBlocks.STOVE);
		helper.setBlock(BASE.offset(2, 1, 0), CookingBlocks.WOK);
		var wokStove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 0, 0)));
		var wok = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 1, 0)));
		use(helper, player, new ItemStack(CookingItems.SUNFLOWER_OIL), BASE.offset(2, 1, 0));
		wok.setItem(0, new ItemStack(CookingItems.CHOPPED_CARROT));
		wok.setItem(1, new ItemStack(CookingItems.SLICED_BROWN_MUSHROOMS));
		wok.setItem(2, new ItemStack(CookingItems.CHOPPED_BEETROOT));
		wok.setItem(CookingMenu.Kind.WOK.containerSlot, new ItemStack(Items.BOWL));
		wokStove.setItem(0, new ItemStack(Items.BLAZE_ROD));
		for (int i = 0; i < 125; i++) {
			StoveBlockEntity.serverTick(level, wokStove.getBlockPos(), wokStove.getBlockState(), wokStove);
			ProcessStationBlockEntity.serverTick(level, wok.getBlockPos(), wok.getBlockState(), wok);
		}
		helper.assertTrue(wok.getItem(CookingMenu.Kind.WOK.outputSlot).is(CookingItems.STIR_FRIED_VEGETABLES) && wok.oil() == 0,
			"Vegetables stir-fry in the oiled wok");
		helper.succeed();
	}

	@GameTest
	public void berriesMakeCordialAndJuiceReducesToSyrup(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		pot.fillWater();
		pot.setItem(0, new ItemStack(Items.SWEET_BERRIES, 6));
		pot.setItem(1, new ItemStack(Items.SUGAR, 2));
		pot.setItem(CookingPotBlockEntity.CONTAINER_SLOT, new ItemStack(Items.GLASS_BOTTLE));
		stove.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
		tickPot(helper, stove, pot, 205);
		var unstrained = pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(unstrained.is(CookingItems.UNSTRAINED_BERRY_CORDIAL) && pot.water() == CookingStation.MAX_WATER - 1,
			"Berries, sugar, and water simmer into unstrained cordial");

		helper.setBlock(BASE.offset(2, 0, 0), CookingBlocks.PREP_COUNTER);
		helper.setBlock(BASE.offset(2, 1, 0), CookingBlocks.STRAINER);
		var strainer = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 1, 0)));
		strainer.setItem(0, unstrained.copy());
		for (int i = 0; i < 60; i++) ProcessStationBlockEntity.serverTick(level, strainer.getBlockPos(), strainer.getBlockState(), strainer);
		var kind = CookingMenu.Kind.STRAINER;
		helper.assertTrue(strainer.getItem(kind.outputSlot).is(CookingItems.SWEET_BERRY_CORDIAL) && strainer.getItem(kind.byproductSlot).is(CookingItems.PULP),
			"Straining leaves cordial in its bottle and the pulp behind");

		for (int slot = 0; slot < pot.getContainerSize(); slot++) pot.setItem(slot, ItemStack.EMPTY);
		pot.setItem(0, new ItemStack(CookingItems.APPLE_JUICE, 4));
		tickPot(helper, stove, pot, 405);
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.APPLE_SYRUP), "Apple juice reduces into syrup");
		var bottles = pot.getItem(CookingPotBlockEntity.BYPRODUCT_SLOT);
		helper.assertTrue(bottles.is(Items.GLASS_BOTTLE) && bottles.getCount() == 3, "Reducing gives back three bottles");
		helper.succeed();
	}

	private static void tickPot(GameTestHelper helper, StoveBlockEntity stove, CookingPotBlockEntity pot, int ticks) {
		for (int i = 0; i < ticks; i++) {
			if (stove.getItem(0).isEmpty()) stove.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
			StoveBlockEntity.serverTick(helper.getLevel(), stove.getBlockPos(), stove.getBlockState(), stove);
			CookingPotBlockEntity.serverTick(helper.getLevel(), pot.getBlockPos(), pot.getBlockState(), pot);
		}
	}

	private static void use(GameTestHelper helper, net.minecraft.world.entity.player.Player player, ItemStack stack, BlockPos relative) {
		var pos = helper.absolutePos(relative);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false);
		helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
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
