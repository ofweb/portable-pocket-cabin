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

	@GameTest
	public void vinesClimbTrellisesAndFruitAgain(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.west(), Blocks.WATER);
		helper.setBlock(BASE.below(), Blocks.FARMLAND);
		helper.setBlock(BASE, CookingPlants.TRELLIS);
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var seeds = new ItemStack(CookingPlants.item("cucumber_seeds"));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, seeds);
		var pos = helper.absolutePos(BASE);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.NORTH, pos, false);
		level.getBlockState(pos).useItemOn(seeds, level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(level.getBlockState(pos).is(CookingPlants.CUCUMBER_VINE) && seeds.isEmpty(), "Cucumber seeds plant a vine on the trellis");
		tick(helper, BASE, 400);
		helper.assertTrue(age(helper, BASE) == 7, "The vine grows to fruit");
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		helper.assertTrue(age(helper, BASE) == 7 - TrellisCropBlock.PICKED_BACK, "Picking cucumbers sets the vine back two stages");
		var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
		helper.assertTrue(has(drops, CookingPlants.TRELLIS.asItem()) && has(drops, CookingPlants.item("cucumber_seeds")),
			"Breaking a vine gives back the trellis and its seeds");
		helper.assertTrue(!CookingPlants.TRELLIS.defaultBlockState().canSurvive(level, helper.absolutePos(BASE.east(3))),
			"A trellis needs farmland");
		helper.succeed();
	}

	@GameTest
	public void bushesRegrowAndElderGivesTwoHarvests(GameTestHelper helper) {
		var level = helper.getLevel();
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		helper.setBlock(BASE.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(BASE, CookingPlants.THYME_BUSH);
		tick(helper, BASE, 200);
		var pos = helper.absolutePos(BASE);
		var bush = (HerbBushBlock) CookingPlants.THYME_BUSH;
		helper.assertTrue(level.getBlockState(pos).getValue(HerbBushBlock.AGE) == HerbBushBlock.MAX_AGE, "Thyme grows on grass, found " + level.getBlockState(pos) + " with " + PlantNeeds.of(CookingPlants.THYME_BUSH)
			+ ", can grow " + PlantNeeds.of(CookingPlants.THYME_BUSH).canGrow(level, pos) + ", below " + level.getBlockState(pos.below())
			+ " soil " + PlantNeeds.of(CookingPlants.THYME_BUSH).soilMatches(level.getBlockState(pos.below())) + ", light " + level.getRawBrightness(pos, 0));
		helper.assertTrue(bush.harvest(level.getBlockState(pos)) == CookingPlants.item("thyme"), "Ripe thyme can be picked");
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		helper.assertTrue(level.getBlockState(pos).getValue(HerbBushBlock.AGE) == 1, "Picking leaves the bush to regrow");

		var elder = (HerbBushBlock) CookingPlants.ELDER_BUSH;
		helper.assertTrue(elder.harvest(elder.defaultBlockState().setValue(HerbBushBlock.AGE, 2)) == CookingPlants.ELDERFLOWER
			&& elder.harvest(elder.defaultBlockState().setValue(HerbBushBlock.AGE, 3)) == CookingPlants.ELDERBERRY,
			"The elder gives elderflower when flowering and elderberries when ripe");

		helper.setBlock(BASE.east(3).below(), Blocks.OAK_LOG);
		helper.setBlock(BASE.east(3), CookingPlants.OYSTER_MUSHROOM);
		helper.assertTrue(level.getBlockState(helper.absolutePos(BASE.east(3))).is(CookingPlants.OYSTER_MUSHROOM), "Oyster mushrooms grow on logs");
		tick(helper, BASE.east(3), 300);
		helper.assertTrue(level.getBlockState(helper.absolutePos(BASE.east(3))).getValue(HerbBushBlock.AGE) == HerbBushBlock.MAX_AGE,
			"Oyster mushrooms grow on a log");
		helper.assertTrue(CabinGreenhouseGrowth.canProgress(CookingPlants.RASPBERRY_BUSH.defaultBlockState()), "Bushes grow in the greenhouse");
		helper.succeed();
	}

	@GameTest
	public void shearsPickRosehipsFromRoseBushes(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(BASE, Blocks.ROSE_BUSH);
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var shears = new ItemStack(net.minecraft.world.item.Items.SHEARS);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shears);
		var pos = helper.absolutePos(BASE);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
		for (int i = 0; i < 30; i++) {
			net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		}
		var found = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2))
			.stream().anyMatch(entity -> entity.getItem().is(CookingPlants.ROSEHIP));
		helper.assertTrue(found && shears.getDamageValue() == 30 && level.getBlockState(pos).is(Blocks.ROSE_BUSH),
			"Shears pick rosehips, wear down, and leave the rose bush in place");
		helper.succeed();
	}

	@GameTest
	public void plumSaplingsGrowTreesThatFruitAgain(GameTestHelper helper) {
		var level = helper.getLevel();
		var base = new BlockPos(4, 2, 4);
		helper.setBlock(base.below(), Blocks.GRASS_BLOCK);
		var sapling = (net.minecraft.world.level.block.SaplingBlock) ((net.minecraft.world.item.BlockItem) CookingPlants.item("plum_sapling")).getBlock();
		helper.setBlock(base, sapling);
		var pos = helper.absolutePos(base);
		for (int i = 0; i < 10 && level.getBlockState(pos).is(sapling); i++) {
			sapling.advanceTree(level, pos, level.getBlockState(pos), RandomSource.create(i));
		}
		helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_LOG), "A plum sapling grows an oak-wood trunk");
		BlockPos leaves = null;
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-3, 1, -3), pos.offset(3, 7, 3))) {
			if (level.getBlockState(near).is(CookingPlants.PLUM_LEAVES)) {
				leaves = near.immutable();
				break;
			}
		}
		helper.assertTrue(leaves != null, "The tree has plum leaves");
		var random = RandomSource.create(7);
		for (int i = 0; i < 200 && level.getBlockState(leaves).getValue(FruitingLeavesBlock.AGE) < FruitingLeavesBlock.FRUITING; i++) {
			level.getBlockState(leaves).randomTick(level, leaves, random);
		}
		helper.assertTrue(level.getBlockState(leaves).getValue(FruitingLeavesBlock.AGE) == FruitingLeavesBlock.FRUITING, "Plum leaves flower, then fruit");
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(leaves), net.minecraft.core.Direction.UP, leaves, false);
		level.getBlockState(leaves).useWithoutItem(level, player, hit);
		helper.assertTrue(level.getBlockState(leaves).is(CookingPlants.PLUM_LEAVES) && level.getBlockState(leaves).getValue(FruitingLeavesBlock.AGE) == 0,
			"Picking plums leaves the leaves on the tree to fruit again");
		var plums = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(leaves).inflate(2));
		helper.assertTrue(plums.stream().anyMatch(entity -> entity.getItem().is(CookingPlants.PLUM)), "Picking drops plums");
		helper.assertTrue(CabinGreenhouseGrowth.canProgress(CookingPlants.PLUM_LEAVES.defaultBlockState())
			&& !CabinGreenhouseGrowth.canProgress(CookingPlants.PLUM_LEAVES.defaultBlockState().setValue(FruitingLeavesBlock.AGE, 2)),
			"The greenhouse brings fruit trees into fruit, and stops there");
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
