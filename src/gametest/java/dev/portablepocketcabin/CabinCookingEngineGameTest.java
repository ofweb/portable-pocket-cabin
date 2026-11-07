package dev.portablepocketcabin;

import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The phase 3 cooking engine: worked stations, two byproduct slots, sun, water bottles, and time station catch-up. */
public final class CabinCookingEngineGameTest {
	/** A crock's layout: four ingredient slots, a container, an output, and two byproduct slots. */
	private static final int CONTAINER = 4, OUTPUT = 5, FIRST = 6, SECOND = 7;

	@GameTest
	public void recipeFormatKeepsNewFieldsToTheirStations(GameTestHelper helper) {
		var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		var valid = List.of(
			"{\"type\":\"portable_pocket_cabin:drying\",\"ingredients\":[{\"item\":\"minecraft:kelp\"}],\"sun\":true,\"time\":100,\"result\":{\"id\":\"minecraft:dried_kelp\"}}",
			"{\"type\":\"portable_pocket_cabin:crock\",\"ingredients\":[{\"item\":\"minecraft:wheat\"}],\"container\":{\"item\":\"minecraft:bowl\"},\"time\":120000,\"result\":{\"id\":\"minecraft:bread\"}}",
			"{\"type\":\"portable_pocket_cabin:mortar\",\"ingredients\":[{\"item\":\"minecraft:wheat\"}],\"time\":4,\"result\":{\"id\":\"minecraft:bread\"}}"
		);
		var invalid = List.of(
			"{\"type\":\"portable_pocket_cabin:crock\",\"ingredients\":[{\"item\":\"minecraft:wheat\"}],\"sun\":true,\"time\":100,\"result\":{\"id\":\"minecraft:bread\"}}",
			"{\"type\":\"portable_pocket_cabin:crock\",\"ingredients\":[{\"item\":\"minecraft:wheat\"}],\"heat\":\"low\",\"time\":100,\"result\":{\"id\":\"minecraft:bread\"}}",
			"{\"type\":\"portable_pocket_cabin:brine\",\"ingredients\":[{\"item\":\"minecraft:cod\"}],\"container\":{\"item\":\"minecraft:bowl\"},\"time\":100,\"result\":{\"id\":\"minecraft:cod\"}}",
			"{\"type\":\"portable_pocket_cabin:mortar\",\"ingredients\":[{\"item\":\"minecraft:wheat\"}],\"container\":{\"item\":\"minecraft:bowl\"},\"time\":4,\"result\":{\"id\":\"minecraft:bread\"}}",
			"{\"type\":\"portable_pocket_cabin:drying\",\"ingredients\":[{\"item\":\"minecraft:kelp\",\"count\":2}],\"time\":100,\"result\":{\"id\":\"minecraft:dried_kelp\"}}",
			"{\"type\":\"portable_pocket_cabin:ageing\",\"ingredients\":[{\"item\":\"minecraft:kelp\"}],\"result\":{\"id\":\"minecraft:dried_kelp\"}}"
		);
		for (String text : valid) {
			helper.assertTrue(Recipe.CODEC.parse(ops, com.google.gson.JsonParser.parseString(text)).error().isEmpty(), "Accepted: " + text);
		}
		for (String text : invalid) {
			helper.assertTrue(Recipe.CODEC.parse(ops, com.google.gson.JsonParser.parseString(text)).error().isPresent(), "Rejected: " + text);
		}
		helper.succeed();
	}

	@GameTest
	public void timeStationsCatchUpOneBatchAndFillBothByproductSlots(GameTestHelper helper) {
		var level = helper.getLevel();
		var crock = new SimpleContainer(8);
		var process = new CookingProcess(CookingStation.CROCK, CONTAINER, OUTPUT, FIRST, SECOND);
		crock.setItem(0, new ItemStack(Items.WHEAT, 4));
		crock.setItem(1, waterBottle());
		crock.setItem(2, waterBottle());
		crock.setItem(CONTAINER, new ItemStack(Items.BOWL, 2));
		helper.assertTrue(process.ready(level, crock, CookingInput.Liquid.NONE), "Water bottles match the water ingredient");
		process.tick(level, crock, CookingInput.Liquid.NONE, Optional.empty(), 1000);
		helper.assertTrue(process.progress() == 1000 && crock.getItem(OUTPUT).isEmpty(), "An hour of game time is an hour of progress");
		process.tick(level, crock, CookingInput.Liquid.NONE, Optional.empty(), 3 * CookingStation.DAY);
		helper.assertTrue(crock.getItem(OUTPUT).is(Items.BREAD) && crock.getItem(OUTPUT).getCount() == 1,
			"Three days of catch-up finish one batch, not two");
		helper.assertTrue(process.progress() == 0 && crock.getItem(0).getCount() == 2, "The next batch has not started");
		helper.assertTrue(crock.getItem(FIRST).is(Items.GLASS_BOTTLE) && crock.getItem(SECOND).is(Items.STICK),
			"Two kinds of remainder fill the two byproduct slots");

		crock.setItem(SECOND, new ItemStack(Items.STONE, 64));
		process.markDirty();
		helper.assertTrue(!process.ready(level, crock, CookingInput.Liquid.NONE), "A batch waits while a remainder has nowhere to go");
		crock.setItem(SECOND, ItemStack.EMPTY);
		process.markDirty();
		process.tick(level, crock, CookingInput.Liquid.NONE, Optional.empty(), CookingStation.DAY);
		helper.assertTrue(crock.getItem(FIRST).getCount() == 2 && crock.getItem(SECOND).getCount() == 1,
			"The same remainder joins its slot again, found " + crock.getItem(FIRST) + ", " + crock.getItem(SECOND) + ", output "
				+ crock.getItem(OUTPUT) + ", inputs " + crock.getItem(0) + " " + crock.getItem(1) + " " + crock.getItem(CONTAINER)
				+ ", progress " + process.progress());

		var notWater = new ItemStack(Items.POTION);
		notWater.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.AWKWARD));
		crock.setItem(0, new ItemStack(Items.WHEAT, 2));
		crock.setItem(1, notWater);
		crock.setItem(OUTPUT, ItemStack.EMPTY);
		process.markDirty();
		helper.assertTrue(!process.ready(level, crock, CookingInput.Liquid.NONE), "Another potion is not water");
		helper.succeed();
	}

	@GameTest
	public void workedStationsCountToolUses(GameTestHelper helper) {
		var level = helper.getLevel();
		var bowl = new SimpleContainer(9);
		var process = new CookingProcess(CookingStation.MIXING_BOWL, 6, 7, 8);
		bowl.setItem(0, new ItemStack(Items.WHEAT, 2));
		bowl.setItem(1, new ItemStack(Items.SUGAR));
		process.tick(level, bowl, CookingInput.Liquid.NONE, Optional.empty(), 1);
		process.tick(level, bowl, CookingInput.Liquid.NONE, Optional.empty(), 1);
		helper.assertTrue(process.progress() == 2 && bowl.getItem(7).isEmpty(), "Two uses of three leave the batch unfinished");
		process.tick(level, bowl, CookingInput.Liquid.NONE, Optional.empty(), 1);
		helper.assertTrue(bowl.getItem(7).is(Items.COOKIE) && bowl.getItem(7).getCount() == 4, "The third use finishes it");
		helper.succeed();
	}

	@GameTest
	public void dryingRecipesFollowTheSky(GameTestHelper helper) {
		var level = helper.getLevel();
		var rack = new SurfaceCooking(CookingStation.DRYING, 2, false);
		List<ItemStack> items = new ArrayList<>(List.of(new ItemStack(Items.ROTTEN_FLESH), new ItemStack(Items.ROTTEN_FLESH)));
		rack.sky(true);
		rack.tick(level, items, Optional.empty(), 150);
		helper.assertTrue(items.get(0).is(Items.LEATHER) && items.get(1).is(Items.LEATHER), "Under open sky, the sun recipe dries both");
		items.set(0, new ItemStack(Items.ROTTEN_FLESH));
		rack.invalidate();
		rack.sky(false);
		rack.tick(level, items, Optional.empty(), 100);
		helper.assertTrue(items.get(0).is(Items.ROTTEN_FLESH) && rack.progress(0) == 100, "In shade, the slower shade recipe applies");
		rack.tick(level, items, Optional.empty(), 1000);
		helper.assertTrue(items.get(0).is(Items.STRING), "The shade recipe finishes its own result");
		rack.sky(true);
		items.set(0, new ItemStack(Items.ROTTEN_FLESH));
		rack.invalidate();
		rack.tick(level, items, Optional.empty(), 50);
		helper.assertTrue(rack.progress(0) == 50, "Back in the sun, the item starts over on the sun recipe");
		helper.succeed();
	}

	/** Potions stack to one, so each water bottle takes its own slot. */
	private static ItemStack waterBottle() {
		return PotionContents.createItemStack(Items.POTION, Potions.WATER);
	}
}
