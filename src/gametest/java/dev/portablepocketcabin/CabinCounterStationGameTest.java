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

	private static <T extends BlockEntity> T reload(GameTestHelper helper, BlockEntity saved, T fresh) {
		var registries = helper.getLevel().registryAccess();
		fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved.saveWithFullMetadata(registries)));
		return fresh;
	}
}
