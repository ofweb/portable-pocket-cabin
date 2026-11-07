package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

import java.util.List;

/** B-0050 plant engine: growing needs from data, the plant kinds, and greenhouse support. */
public final class CabinPlantGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);

	@GameTest
	public void fieldCropsGrowOnlyWhereTheirNeedsAreMet(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.west(), Blocks.WATER);
		helper.setBlock(BASE.below(), Blocks.FARMLAND);
		helper.setBlock(BASE, CookingPlants.BARLEY_CROP);
		helper.setBlock(BASE.east(2).below(), Blocks.GRASS_BLOCK);
		helper.setBlock(BASE.east(2), CookingPlants.BARLEY_CROP);
		tick(helper, BASE, 300);
		tick(helper, BASE.east(2), 300);
		helper.assertTrue(age(helper, BASE) == 7, "Barley on watered farmland grows to harvest");
		helper.assertTrue(age(helper, BASE.east(2)) == 0 && level.getBlockState(helper.absolutePos(BASE.east(2))).is(CookingPlants.BARLEY_CROP),
			"Wild barley on grass survives but does not grow");

		var dry = BASE.east(6).south(6);
		helper.setBlock(dry.below(), Blocks.FARMLAND);
		helper.setBlock(dry, CookingPlants.RYE_CROP);
		tick(helper, dry, 300);
		helper.assertTrue(age(helper, dry) == 0, "Without water nearby, rye waits");

		var original = PlantNeeds.of(CookingPlants.BARLEY_CROP);
		try {
			PlantNeeds.override(CookingPlants.BARLEY_CROP, new PlantNeeds(List.of("minecraft:sand"), 9, PlantNeeds.Water.NONE,
				PlantNeeds.Support.NONE, 1.0F, false));
			helper.setBlock(BASE, CookingPlants.BARLEY_CROP);
			tick(helper, BASE, 20);
			helper.assertTrue(age(helper, BASE) == 0, "Data that moves barley to sand stops it growing on farmland");
		} finally {
			PlantNeeds.override(CookingPlants.BARLEY_CROP, original);
		}
		helper.succeed();
	}

	@GameTest
	public void cropsDropTheirHarvestAndCilantroFlowersIntoCoriander(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.below(), Blocks.FARMLAND);
		var pos = helper.absolutePos(BASE);
		helper.setBlock(BASE, ((CropBlock) CookingPlants.CABBAGE_CROP).getStateForAge(7));
		var cabbage = Block.getDrops(level.getBlockState(pos), level, pos, null);
		helper.assertTrue(has(cabbage, CookingPlants.CABBAGE) && has(cabbage, CookingPlants.item("cabbage_seeds")), "Ripe cabbage drops cabbage and seeds");
		helper.setBlock(BASE, ((CropBlock) CookingPlants.GARLIC_CROP).getStateForAge(7));
		var garlic = Block.getDrops(level.getBlockState(pos), level, pos, null);
		helper.assertTrue(garlic.stream().allMatch(stack -> stack.is(CookingPlants.item("garlic"))) && count(garlic, CookingPlants.item("garlic")) >= 1,
			"Garlic replants from its own bulbs");
		helper.setBlock(BASE, ((CropBlock) CookingPlants.CILANTRO_CROP).getStateForAge(6));
		helper.assertTrue(has(Block.getDrops(level.getBlockState(pos), level, pos, null), CookingPlants.CILANTRO), "Ripe cilantro gives its leaves");
		helper.setBlock(BASE, ((CropBlock) CookingPlants.CILANTRO_CROP).getStateForAge(7));
		var flowered = Block.getDrops(level.getBlockState(pos), level, pos, null);
		helper.assertTrue(!has(flowered, CookingPlants.CILANTRO) && count(flowered, CookingPlants.item("coriander_seed")) >= 3,
			"Cilantro left to flower gives coriander seed");
		helper.succeed();
	}

	@GameTest
	public void greenhouseGrowsCookingPlantsUntilRipe(GameTestHelper helper) {
		var young = ((CropBlock) CookingPlants.ONION_CROP).getStateForAge(3);
		var ripe = ((CropBlock) CookingPlants.ONION_CROP).getStateForAge(7);
		helper.assertTrue(CabinGreenhouseGrowth.canProgress(young) && !CabinGreenhouseGrowth.canProgress(ripe),
			"Greenhouse catch-up grows cooking crops and stops at harvest");
		helper.assertTrue(CookingPlants.PLANTS.size() >= 20, "Delivery 1 field crops are registered");
		helper.succeed();
	}

	private static void tick(GameTestHelper helper, BlockPos relative, int ticks) {
		var level = helper.getLevel();
		var pos = helper.absolutePos(relative);
		var random = RandomSource.create(42);
		for (int i = 0; i < ticks; i++) {
			var state = level.getBlockState(pos);
			if (state.isRandomlyTicking()) state.randomTick(level, pos, random);
		}
	}

	private static int age(GameTestHelper helper, BlockPos relative) {
		var state = helper.getLevel().getBlockState(helper.absolutePos(relative));
		return state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1;
	}

	static boolean has(List<ItemStack> drops, Item item) {
		return drops.stream().anyMatch(stack -> stack.is(item));
	}

	static int count(List<ItemStack> drops, Item item) {
		return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}
}
