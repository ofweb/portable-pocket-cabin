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
				PlantNeeds.Support.NONE, 1.0F, false, 0F));
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
	public void tallCropsTakeTheBlockAboveAndGoWithIt(GameTestHelper helper) {
		var level = helper.getLevel();
		var corn = (FieldCropBlock) CookingPlants.CORN_CROP;
		helper.setBlock(BASE.west(), Blocks.WATER);
		helper.setBlock(BASE.below(), Blocks.FARMLAND);
		helper.setBlock(BASE, corn);
		tick(helper, BASE, 400);
		var top = helper.absolutePos(BASE.above());
		helper.assertTrue(age(helper, BASE) == 7 && level.getBlockState(top).is(CookingPlants.PLANT_TOP),
			"Corn grows to harvest and takes the block above for its top");

		// Using the top acts on the plant, and breaking the top breaks the whole plant with its harvest.
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		helper.assertTrue(level.getBlockState(top).getShape(level, top).max(net.minecraft.core.Direction.Axis.Y) > 0.5,
			"The top gives the upper block an outline");
		CookingPlants.PLANT_TOP.playerWillDestroy(level, top, level.getBlockState(top), player);
		level.removeBlock(top, false);
		helper.assertTrue(level.getBlockState(helper.absolutePos(BASE)).isAir() && level.getBlockState(top).isAir(),
			"Breaking the top breaks the corn below");

		// With no room above, corn waits at the stage before it grows tall, and bone meal does nothing.
		var roofed = BASE.east(3);
		helper.setBlock(roofed.west(), Blocks.WATER);
		helper.setBlock(roofed.below(), Blocks.FARMLAND);
		helper.setBlock(roofed, corn);
		helper.setBlock(roofed.above(), Blocks.OAK_LEAVES);
		tick(helper, roofed, 400);
		var roofedPos = helper.absolutePos(roofed);
		helper.assertTrue(age(helper, roofed) == corn.tallFrom() - 1, "Corn with no room above stops below its tall stages");
		helper.assertTrue(!corn.isValidBonemealTarget(level, roofedPos, level.getBlockState(roofedPos)), "Bone meal cannot push it past");
		helper.assertTrue(!CabinGreenhouseGrowth.canProgress(level, roofedPos, level.getBlockState(roofedPos)),
			"Greenhouse catch-up does not wait on it");

		// It never grows into another block, not even one that could be replaced such as a light block, nor through a
		// greenhouse's glass roof.
		for (var cover : List.of(Blocks.LIGHT, Blocks.GLASS)) {
			var covered = roofed.east(2);
			helper.setBlock(covered.west(), Blocks.WATER);
			helper.setBlock(covered.below(), Blocks.FARMLAND);
			helper.setBlock(covered, corn);
			helper.setBlock(covered.above(), cover);
			tick(helper, covered, 400);
			helper.assertTrue(age(helper, covered) == corn.tallFrom() - 1 && level.getBlockState(helper.absolutePos(covered.above())).is(cover),
				"Corn under " + cover.getName().getString() + " waits and leaves it be");
		}

		// Breaking the plant takes its top with it.
		helper.setBlock(roofed.above(), Blocks.AIR);
		tick(helper, roofed, 400);
		helper.assertTrue(level.getBlockState(helper.absolutePos(roofed.above())).is(CookingPlants.PLANT_TOP), "Given room, it grows on");
		level.destroyBlock(roofedPos, true);
		helper.assertTrue(level.getBlockState(helper.absolutePos(roofed.above())).isAir(), "Breaking the plant takes its top too");
		helper.succeed();
	}

	@GameTest
	public void tallVinesClimbAStackedTrellis(GameTestHelper helper) {
		var level = helper.getLevel();
		var tomato = (TrellisCropBlock) CookingPlants.PLANTS.get("tomato_vine");
		var random = RandomSource.create(7);
		helper.setBlock(BASE.west(), Blocks.WATER);
		helper.setBlock(BASE.below(), Blocks.FARMLAND);
		helper.setBlock(BASE, tomato.getStateForAge(0));
		helper.setBlock(BASE.above(), CookingPlants.TRELLIS);
		var pos = helper.absolutePos(BASE);
		var top = pos.above();
		while (!tomato.isMaxAge(level.getBlockState(pos))) tomato.performBonemeal(level, random, pos, level.getBlockState(pos));
		helper.assertTrue(level.getBlockState(pos).getValue(TrellisCropBlock.TALL) && level.getBlockState(top).is(CookingPlants.PLANT_TOP)
			&& level.getBlockState(top).getValue(PlantTopBlock.TRELLIS), "A tomato vine climbs the trellis stacked on its own");

		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(top), net.minecraft.core.Direction.NORTH, top, false);
		level.getBlockState(top).useWithoutItem(level, player, hit);
		helper.assertTrue(age(helper, BASE) == 7 - TrellisCropBlock.PICKED_BACK && level.getBlockState(pos).getValue(TrellisCropBlock.TALL),
			"Picking from the climbed trellis picks the vine, which stays tall");
		helper.assertTrue(Block.getDrops(level.getBlockState(top), level, top, null).stream().anyMatch(stack -> stack.is(CookingPlants.TRELLIS.asItem())),
			"The climbed trellis gives back a trellis");

		level.removeBlock(top, false);
		helper.assertTrue(!level.getBlockState(pos).getValue(TrellisCropBlock.TALL) && age(helper, BASE) == 7 - TrellisCropBlock.PICKED_BACK,
			"Without the trellis above, the vine stays, one block high");
		level.setBlockAndUpdate(top, CookingPlants.TRELLIS.defaultBlockState());
		level.getBlockState(pos).tick(level, pos, random);
		helper.assertTrue(level.getBlockState(pos).getValue(TrellisCropBlock.TALL), "A grown vine climbs a trellis stacked on it again");

		// On a single trellis a tall vine grows and fruits at one block high.
		var single = BASE.east(3);
		helper.setBlock(single.west(), Blocks.WATER);
		helper.setBlock(single.below(), Blocks.FARMLAND);
		helper.setBlock(single, tomato.getStateForAge(0));
		var singlePos = helper.absolutePos(single);
		while (!tomato.isMaxAge(level.getBlockState(singlePos))) tomato.performBonemeal(level, random, singlePos, level.getBlockState(singlePos));
		helper.assertTrue(!level.getBlockState(singlePos).getValue(TrellisCropBlock.TALL) && level.getBlockState(singlePos.above()).isAir(),
			"On one trellis the vine ripens one block high");
		helper.assertTrue(CookingPlants.TRELLIS.defaultBlockState().canSurvive(level, singlePos.above()), "A trellis stands on a vine");
		helper.succeed();
	}

	@GameTest
	public void brokenFruitingLeavesDropTheirFruit(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = helper.absolutePos(BASE);
		var leaves = CookingPlants.PLUM_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
		helper.setBlock(BASE, leaves.setValue(FruitingLeavesBlock.AGE, FruitingLeavesBlock.FRUITING));
		int most = 0;
		for (int i = 0; i < 40; i++) {
			var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
			int plums = count(drops, CookingPlants.PLUM);
			helper.assertTrue(plums >= 1 && plums <= 2, "Breaking or decaying fruiting plum leaves drops one or two plums, as picking does");
			most = Math.max(most, plums);
		}
		helper.assertTrue(most == 2, "Sometimes two");
		var shears = new ItemStack(net.minecraft.world.item.Items.SHEARS);
		var sheared = Block.getDrops(level.getBlockState(pos), level, pos, null, null, shears);
		helper.assertTrue(has(sheared, CookingPlants.PLUM_LEAVES.asItem()) && !has(sheared, CookingPlants.PLUM), "Shears take the leaves, not the fruit");
		helper.setBlock(BASE, leaves.setValue(FruitingLeavesBlock.AGE, 1));
		helper.assertTrue(!has(Block.getDrops(level.getBlockState(pos), level, pos, null), CookingPlants.PLUM), "Leaves in blossom drop no fruit");
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
		var sapling = (net.minecraft.world.level.block.SaplingBlock) ((net.minecraft.world.item.BlockItem) CookingPlants.item("plum_sapling")).getBlock();
		// A plum tree is bigger than the test area, so it grows in open air high above it.
		var pos = helper.absolutePos(new BlockPos(4, 2, 4)).above(CabinGrowEveryFoodGameTest.TREE_LIFT);
		level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		level.setBlockAndUpdate(pos, sapling.defaultBlockState());
		for (int i = 0; i < 10 && level.getBlockState(pos).is(sapling); i++) {
			sapling.advanceTree(level, pos, level.getBlockState(pos), RandomSource.create(i));
		}
		helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_LOG), "A plum sapling grows an oak-wood trunk");
		BlockPos leaves = null;
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-6, 1, -6), pos.offset(6, 12, 6))) {
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
		CabinGrowEveryFoodGameTest.clearTree(level, pos);
		helper.succeed();
	}

	@GameTest
	public void everyPlantGrowsWildAndTradersSellPlantingStock(GameTestHelper helper) {
		var level = helper.getLevel();
		var access = level.registryAccess();
		var placed = access.lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
		var biomes = access.lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
		for (String plant : CookingPlants.PLANTS.keySet()) {
			String name = CookingPlants.wildName(plant);
			helper.assertTrue(placed.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
				PortablePocketCabin.id("wild_" + name))).isPresent(), name + " has a wild placed feature");
			var tag = biomes.get(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME, PortablePocketCabin.id("grows_wild/" + name)));
			helper.assertTrue(tag.isPresent() && tag.get().size() > 0, name + " grows wild in some biome");
		}
		var configured = access.lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
		var generator = level.getChunkSource().getGenerator();
		int x = 0;
		for (String plant : List.of("barley", "thyme_bush", "cucumber_vine")) {
			BlockPos spot = new BlockPos(1 + x, 2, 7);
			x += 2;
			helper.setBlock(spot.below(), Blocks.GRASS_BLOCK);
			var feature = configured.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
				PortablePocketCabin.id("wild_" + plant)));
			feature.value().place(level, generator, RandomSource.create(1), helper.absolutePos(spot));
			helper.assertTrue(level.getBlockState(helper.absolutePos(spot)).is(CookingPlants.PLANTS.get(plant)), "Wild " + plant + " stands on grass");
		}
		var trades = access.lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_TRADE);
		helper.assertTrue(trades.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_TRADE,
			PortablePocketCabin.id("wandering_trader/emerald_plum_sapling"))).isPresent(), "Wandering traders can sell plum saplings");
		helper.assertTrue(trades.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_TRADE,
			PortablePocketCabin.id("farmer/3/emerald_barley"))).isPresent(), "Journeyman farmers can sell barley to sow");
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
