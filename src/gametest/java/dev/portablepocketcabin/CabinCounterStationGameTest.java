package dev.portablepocketcabin;

import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.List;

/** B-0053: the counter, strainer, grill, wok, æbleskive pan, and oil in the pot. */
public final class CabinCounterStationGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void recipeFormatKeepsLiquidsAndContainersToTheirStations(GameTestHelper helper) {
		var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		var valid = List.of(
			"{\"type\":\"portable_pocket_cabin:pot\",\"ingredients\":[{\"item\":\"minecraft:potato\"}],\"oil\":1,\"heat\":\"high\",\"time\":20,\"result\":{\"id\":\"minecraft:baked_potato\"}}",
			"{\"type\":\"portable_pocket_cabin:wok\",\"ingredients\":[{\"item\":\"minecraft:carrot\"}],\"oil\":1,\"heat\":\"high\",\"time\":20,\"result\":{\"id\":\"minecraft:carrot\"}}",
			"{\"type\":\"portable_pocket_cabin:strainer\",\"ingredients\":[{\"item\":\"minecraft:sweet_berries\"}],\"container\":{\"item\":\"minecraft:glass_bottle\"},\"time\":20,\"result\":{\"id\":\"minecraft:honey_bottle\"}}",
			"{\"type\":\"portable_pocket_cabin:counter\",\"ingredients\":[{\"item\":\"minecraft:beef\"},{\"item\":\"portable_pocket_cabin:skewer\"}],\"time\":40,\"result\":{\"id\":\"minecraft:beef\"}}"
		);
		var invalid = List.of(
			"{\"type\":\"portable_pocket_cabin:oven\",\"ingredients\":[{\"item\":\"minecraft:potato\"}],\"oil\":1,\"heat\":\"high\",\"time\":20,\"result\":{\"id\":\"minecraft:baked_potato\"}}",
			"{\"type\":\"portable_pocket_cabin:wok\",\"ingredients\":[{\"item\":\"minecraft:carrot\"}],\"water\":1,\"heat\":\"high\",\"time\":20,\"result\":{\"id\":\"minecraft:carrot\"}}",
			"{\"type\":\"portable_pocket_cabin:pot\",\"ingredients\":[{\"item\":\"minecraft:potato\"}],\"water\":1,\"oil\":1,\"heat\":\"high\",\"time\":20,\"result\":{\"id\":\"minecraft:baked_potato\"}}",
			"{\"type\":\"portable_pocket_cabin:counter\",\"ingredients\":[{\"item\":\"minecraft:beef\"}],\"container\":{\"item\":\"minecraft:bowl\"},\"time\":40,\"result\":{\"id\":\"minecraft:beef\"}}",
			"{\"type\":\"portable_pocket_cabin:counter\",\"ingredients\":[{\"item\":\"minecraft:beef\"}],\"result\":{\"id\":\"minecraft:beef\"}}",
			"{\"type\":\"portable_pocket_cabin:grill\",\"ingredients\":[{\"item\":\"minecraft:beef\",\"count\":2}],\"heat\":\"high\",\"time\":20,\"result\":{\"id\":\"minecraft:cooked_beef\"}}"
		);
		for (String text : valid) {
			helper.assertTrue(Recipe.CODEC.parse(ops, com.google.gson.JsonParser.parseString(text)).error().isEmpty(), "Accepted: " + text);
		}
		for (String text : invalid) {
			helper.assertTrue(Recipe.CODEC.parse(ops, com.google.gson.JsonParser.parseString(text)).error().isPresent(), "Rejected: " + text);
		}
		var skewers = CookingStation.CUTTING.find(helper.getLevel(), CookingInput.of(List.of(new ItemStack(Items.STICK)))).orElseThrow();
		helper.assertTrue(skewers.value().result().count() == 4 && skewers.value().result().item().value() == CookingItems.SKEWER,
			"A stick cuts into four skewers");
		helper.succeed();
	}

	@GameTest
	public void potHoldsWaterOrOilAndDeepFriesInOil(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		helper.assertTrue(pot.fillOil() && pot.fillOil() && pot.oil() == 2 && pot.water() == 0, "Oil fills one level at a time");
		helper.assertTrue(!pot.fillWater() && pot.oil() == 2, "A pot of oil refuses water");
		pot.setItem(0, new ItemStack(Items.POTATO));
		stove.setItem(0, new ItemStack(Items.BLAZE_ROD));
		for (int tick = 0; tick < 25; tick++) {
			StoveBlockEntity.serverTick(level, stove.getBlockPos(), stove.getBlockState(), stove);
			CookingPotBlockEntity.serverTick(level, pot.getBlockPos(), pot.getBlockState(), pot);
		}
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(Items.BAKED_POTATO) && pot.oil() == 1,
			"A deep-frying recipe cooks in hot oil and uses one level");

		var reloaded = reload(helper, pot, new CookingPotBlockEntity(pot.getBlockPos(), pot.getBlockState()));
		helper.assertTrue(reloaded.oil() == 1 && reloaded.water() == 0, "The pot keeps its oil when saved");

		helper.setBlock(BASE.offset(2, 1, 0), CookingBlocks.COOKING_POT);
		var water = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 1, 0)));
		helper.assertTrue(water.fillWater() && !water.fillOil() && water.water() == CookingStation.MAX_WATER, "A pot of water refuses oil");
		var registries = level.registryAccess();
		var tag = water.saveWithFullMetadata(registries);
		tag.remove("liquid");
		var older = new CookingPotBlockEntity(water.getBlockPos(), water.getBlockState());
		older.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
		helper.assertTrue(older.water() == CookingStation.MAX_WATER && older.oil() == 0, "A pot saved before oil existed loads as water");
		helper.succeed();
	}

	@GameTest
	public void counterThreadsSkewersAndPausesUnderAStation(GameTestHelper helper) {
		var level = helper.getLevel();
		var counter = counter(helper, BASE);
		counter.setItem(0, new ItemStack(CookingItems.SKEWER));
		counter.setItem(1, new ItemStack(Items.BEEF));
		counter.setItem(2, new ItemStack(Items.CARROT));
		tick(helper, counter, 20);
		helper.assertTrue(counter.progress() == 20, "Hand work progresses without heat");
		helper.setBlock(BASE.above(), CookingBlocks.STRAINER);
		tick(helper, counter, 10);
		helper.assertTrue(counter.progress() == 20 && counter.covered(), "Hand work pauses while a station stands on the counter");
		var reloaded = reload(helper, counter, ProcessStationBlockEntity.counter(counter.getBlockPos(), counter.getBlockState()));
		helper.assertTrue(reloaded.progress() == 20 && reloaded.getItem(1).is(Items.BEEF), "The counter keeps its work when saved");
		helper.setBlock(BASE.above(), net.minecraft.world.level.block.Blocks.AIR);
		tick(helper, counter, 20);
		helper.assertTrue(counter.getItem(CookingMenu.Kind.COUNTER.outputSlot).is(CookingItems.RAW_BEEF_SKEWER) && counter.getItem(1).isEmpty(),
			"Once clear, the counter threads a beef and carrot skewer");
		helper.succeed();
	}

	@GameTest
	public void counterTopStationsNeedACounter(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos dirt = helper.absolutePos(BASE.offset(2, 0, 0));
		helper.setBlock(BASE.offset(2, -1, 0), net.minecraft.world.level.block.Blocks.DIRT);
		helper.assertTrue(!CookingBlocks.STRAINER.defaultBlockState().canSurvive(level, dirt), "A strainer cannot stand on dirt");
		helper.assertTrue(CookingBlocks.CUTTING_BOARD.defaultBlockState().canSurvive(level, dirt), "The cutting board still works anywhere");
		counter(helper, BASE);
		helper.assertTrue(CookingBlocks.STRAINER.defaultBlockState().canSurvive(level, helper.absolutePos(BASE.above())), "A strainer stands on a counter");
		helper.setBlock(BASE.above(), CookingBlocks.STRAINER);
		level.setBlockAndUpdate(helper.absolutePos(BASE), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
		helper.assertTrue(level.getBlockState(helper.absolutePos(BASE.above())).isAir(), "The strainer comes off when its counter is removed");
		var dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
			new net.minecraft.world.phys.AABB(helper.absolutePos(BASE)).inflate(2));
		helper.assertTrue(dropped.stream().anyMatch(item -> item.getItem().is(CookingBlocks.STRAINER.asItem())), "It drops as an item");
		dropped.forEach(net.minecraft.world.entity.Entity::discard);
		helper.succeed();
	}

	@GameTest
	public void strainerSeparatesLiquidFromSolids(GameTestHelper helper) {
		var level = helper.getLevel();
		counter(helper, BASE);
		helper.setBlock(BASE.above(), CookingBlocks.STRAINER);
		var strainer = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		var kind = CookingMenu.Kind.STRAINER;
		strainer.setItem(0, new ItemStack(Items.SWEET_BERRIES, 2));
		strainer.setItem(kind.containerSlot, new ItemStack(Items.GLASS_BOTTLE, 2));
		tick(helper, strainer, 20);
		helper.assertTrue(strainer.getItem(kind.outputSlot).is(Items.HONEY_BOTTLE) && strainer.getItem(kind.byproductSlot).is(Items.STICK),
			"The liquid fills its bottle and the solids go to the byproduct slot");
		helper.assertTrue(strainer.getItem(kind.containerSlot).getCount() == 1 && strainer.getItem(0).isEmpty(), "Straining uses one bottle and the berries");
		helper.succeed();
	}

	private static ProcessStationBlockEntity counter(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, CookingBlocks.PREP_COUNTER);
		return (ProcessStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
	}

	private static void tick(GameTestHelper helper, ProcessStationBlockEntity station, int ticks) {
		for (int i = 0; i < ticks; i++) {
			ProcessStationBlockEntity.serverTick(helper.getLevel(), station.getBlockPos(), station.getBlockState(), station);
		}
	}

	private static <T extends BlockEntity> T reload(GameTestHelper helper, BlockEntity saved, T fresh) {
		var registries = helper.getLevel().registryAccess();
		fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved.saveWithFullMetadata(registries)));
		return fresh;
	}
}
