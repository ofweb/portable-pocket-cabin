package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** B-0054: every food the player finds can be grown. */
public final class CabinGrowEveryFoodGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);

	@GameTest
	public void glassDoesNotShadeButStoneDoes(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos plant = helper.absolutePos(BASE);
		clearAbove(helper, BASE);
		helper.setBlock(BASE.above(3), Blocks.GLASS);
		helper.assertTrue(!PlantNeeds.roofed(level, plant), "Glass lets the sky through, so it is no shade");
		helper.setBlock(BASE.above(2), Blocks.STONE);
		helper.assertTrue(PlantNeeds.roofed(level, plant), "Stone overhead shades the plant");
		helper.succeed();
	}

	@GameTest
	public void prawnTrapsCatchAtHalfRateOutsidePrawnWaters(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, Butchery.PRAWN_TRAP.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
		BlockPos pos = helper.absolutePos(BASE);
		var trap = (PrawnTrapBlockEntity) level.getBlockEntity(pos);
		trap.tick(level, level.getBlockState(pos));
		trap.rewind(PrawnTrapBlockEntity.TICKS_PER_PRAWN * 4L);
		trap.tick(level, level.getBlockState(pos));
		boolean prawnWaters = level.getBiome(pos).is(PrawnTrapBlock.PRAWN_WATERS);
		helper.assertTrue(trap.getItem(0).getCount() == (prawnWaters ? 4 : 2),
			"Four hours give four prawns in prawn waters and two elsewhere, found " + trap.getItem(0) + " in prawn waters " + prawnWaters);
		helper.succeed();
	}

	@GameTest
	public void aGrownCardoonGivesBackTwo(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(BASE, CookingBlocks.CARDOON.defaultBlockState().setValue(CardoonBlock.AGE, CardoonBlock.MAX_AGE));
		BlockPos pos = helper.absolutePos(BASE);
		var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
		int cardoons = drops.stream().filter(stack -> stack.is(CookingBlocks.CARDOON.asItem())).mapToInt(ItemStack::getCount).sum();
		helper.assertTrue(cardoons == 2, "A grown cardoon gives back two, found " + drops);
		helper.setBlock(BASE, CookingBlocks.CARDOON.defaultBlockState());
		var young = Block.getDrops(level.getBlockState(pos), level, pos, null);
		helper.assertTrue(young.stream().filter(stack -> stack.is(CookingBlocks.CARDOON.asItem())).mapToInt(ItemStack::getCount).sum() == 1,
			"A young cardoon gives back one");
		helper.succeed();
	}

	@GameTest
	public void appleTreesGrowAndBearVanillaApples(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.below(), Blocks.GRASS_BLOCK);
		clearAbove(helper, BASE);
		var sapling = (net.minecraft.world.level.block.SaplingBlock) ((net.minecraft.world.item.BlockItem) CookingPlants.item("apple_sapling")).getBlock();
		BlockPos pos = helper.absolutePos(BASE);
		level.setBlockAndUpdate(pos, sapling.defaultBlockState());
		for (int i = 0; i < 10 && level.getBlockState(pos).is(sapling); i++) {
			sapling.advanceTree(level, pos, level.getBlockState(pos), net.minecraft.util.RandomSource.create(i));
		}
		helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_LOG), "An apple sapling grows an oak-wood trunk");
		BlockPos leaves = null;
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-3, 1, -3), pos.offset(3, 7, 3))) {
			if (level.getBlockState(near).is(CookingPlants.APPLE_LEAVES)) {
				leaves = near.immutable();
				break;
			}
		}
		helper.assertTrue(leaves != null, "The tree has apple leaves");
		level.setBlockAndUpdate(leaves, level.getBlockState(leaves).setValue(FruitingLeavesBlock.AGE, FruitingLeavesBlock.FRUITING));
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		level.getBlockState(leaves).useWithoutItem(level, player,
			new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(leaves), net.minecraft.core.Direction.UP, leaves, false));
		helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(leaves).inflate(2)).stream()
			.anyMatch(entity -> entity.getItem().is(net.minecraft.world.item.Items.APPLE)), "Picking apple leaves gives vanilla apples");
		helper.assertTrue(level.getBlockState(leaves).getValue(FruitingLeavesBlock.AGE) == 0, "The leaves fruit again after picking");
		helper.succeed();
	}

	@GameTest
	public void oakLeavesRarelyDropAnAppleSapling(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(BASE);
		var oak = Blocks.OAK_LEAVES.defaultBlockState();
		int saplings = 0;
		int apples = 0;
		for (int i = 0; i < 6000; i++) {
			for (ItemStack drop : Block.getDrops(oak, level, pos, null)) {
				if (drop.is(CookingPlants.item("apple_sapling"))) saplings++;
				if (drop.is(net.minecraft.world.item.Items.APPLE)) apples++;
			}
		}
		helper.assertTrue(saplings > 0 && saplings < 120, "About 1 in 200 oak leaves drops an apple sapling, found " + saplings + " in 6000");
		helper.assertTrue(apples > 0, "Oak leaves still drop vanilla apples");
		helper.succeed();
	}

	/** Clears the GameTest structure's barrier ceiling above a position, so the sky is open above it. */
	static void clearAbove(GameTestHelper helper, BlockPos pos) {
		var level = helper.getLevel();
		for (int y = 1; y < 16; y++) {
			BlockPos above = helper.absolutePos(pos.above(y));
			if (level.getBlockState(above).is(Blocks.BARRIER)) level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
		}
	}

	@GameTest
	public void seaPlantsGrowAndGiveMoreWhenGrown(GameTestHelper helper) {
		var level = helper.getLevel();
		var original = PlantNeeds.of(CookingPlants.WAKAME);
		try {
			PlantNeeds.override(CookingPlants.WAKAME, new PlantNeeds(java.util.List.of(), 0, PlantNeeds.Water.STANDING,
				PlantNeeds.Support.NONE, 1.0F, false, 0F));
			helper.setBlock(BASE.below(), Blocks.SAND);
			helper.setBlock(BASE, Blocks.WATER);
			helper.setBlock(BASE, CookingPlants.WAKAME);
			BlockPos pos = helper.absolutePos(BASE);
			int before = level.getBlockState(pos).getValue(SeaPlantBlock.AGE);
			helper.assertTrue(before == 0, "Planted wakame starts young");
			var young = Block.getDrops(level.getBlockState(pos), level, pos, null);
			helper.assertTrue(count(young, CookingPlants.item("wakame")) == 1, "Young wakame gives back one, found " + young);
			for (int i = 0; i < 3; i++) level.getBlockState(pos).randomTick(level, pos, level.getRandom());
			helper.assertTrue(level.getBlockState(pos).getValue(SeaPlantBlock.AGE) == SeaPlantBlock.MAX_AGE, "Wakame grows in water");
			var grown = Block.getDrops(level.getBlockState(pos), level, pos, null);
			int wakame = count(grown, CookingPlants.item("wakame"));
			helper.assertTrue(wakame >= 2 && wakame <= 3, "Grown wakame gives two or three, found " + grown);

			helper.setBlock(BASE, Blocks.WATER);
			helper.setBlock(BASE, CookingPlants.LAVER);
			var laver = (SeaPlantBlock) CookingPlants.LAVER;
			helper.assertTrue(laver.isValidBonemealTarget(level, pos, level.getBlockState(pos)), "Bone meal helps laver");
			laver.performBonemeal(level, level.getRandom(), pos, level.getBlockState(pos));
			helper.assertTrue(level.getBlockState(pos).getValue(SeaPlantBlock.AGE) == 1, "Bone meal ages laver one stage");
		} finally {
			PlantNeeds.override(CookingPlants.WAKAME, original);
		}
		helper.succeed();
	}

	@GameTest
	public void shellfishBedsAreGatheredWhenGrownAndSpread(GameTestHelper helper) {
		var level = helper.getLevel();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.setBlock(BASE.offset(dx, -1, dz), Blocks.STONE);
				helper.setBlock(BASE.offset(dx, 0, dz), Blocks.WATER);
			}
		}
		BlockPos pos = helper.absolutePos(BASE);
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
		helper.setBlock(BASE, CookingPlants.MUSSEL_BED);
		helper.assertTrue(level.getBlockState(pos).useWithoutItem(level, player, hit) == net.minecraft.world.InteractionResult.PASS,
			"A young bed has nothing to gather yet");
		helper.setBlock(BASE, CookingPlants.MUSSEL_BED.defaultBlockState().setValue(SeaPlantBlock.AGE, SeaPlantBlock.MAX_AGE));
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		int mussels = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).stream()
			.filter(entity -> entity.getItem().is(CookingPlants.item("mussels"))).mapToInt(entity -> entity.getItem().getCount()).sum();
		helper.assertTrue(mussels >= 2 && mussels <= 3, "A grown bed gives two or three mussels, found " + mussels);
		helper.assertTrue(level.getBlockState(pos).is(CookingPlants.MUSSEL_BED) && level.getBlockState(pos).getValue(SeaPlantBlock.AGE) == 1,
			"Gathering sets the bed back to its second stage");
		var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
		helper.assertTrue(count(drops, CookingPlants.item("mussels")) == 1, "Breaking a bed gives one, to move it");

		var bed = (SeaPlantBlock) CookingPlants.MUSSEL_BED;
		var random = net.minecraft.util.RandomSource.create(7);
		for (int i = 0; i < 64; i++) bed.spread(level, pos, random);
		int spread = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if ((dx != 0 || dz != 0) && level.getBlockState(pos.offset(dx, 0, dz)).is(CookingPlants.MUSSEL_BED)) {
					spread++;
					helper.assertTrue(level.getBlockState(pos.offset(dx, 0, dz)).getValue(SeaPlantBlock.AGE) == 0, "A bed spreads as a young bed");
				}
			}
		}
		helper.assertTrue(spread == 8, "A bed spreads to the water floor around it, found " + spread);

		BlockPos dry = helper.absolutePos(BASE.east(4));
		helper.setBlock(BASE.east(4).below(), Blocks.STONE);
		helper.setBlock(BASE.east(4), Blocks.WATER);
		helper.setBlock(BASE.east(4), CookingPlants.OYSTER_BED.defaultBlockState().setValue(SeaPlantBlock.AGE, SeaPlantBlock.MAX_AGE));
		for (int i = 0; i < 64; i++) ((SeaPlantBlock) CookingPlants.OYSTER_BED).spread(level, dry, random);
		int oysters = 0;
		for (BlockPos near : BlockPos.betweenClosed(dry.offset(-1, -1, -1), dry.offset(1, 0, 1))) {
			if (level.getBlockState(near).is(CookingPlants.OYSTER_BED)) oysters++;
		}
		helper.assertTrue(oysters == 1, "A bed with no water beside it doesn't spread");
		helper.succeed();
	}

	@GameTest
	public void greenhouseCatchUpGrowsSaplingsTrunksAndSeaPlants(GameTestHelper helper) {
		var level = helper.getLevel();
		var sapling = (net.minecraft.world.level.block.SaplingBlock) ((net.minecraft.world.item.BlockItem) CookingPlants.item("apple_sapling")).getBlock();
		BlockPos open = BASE;
		BlockPos cramped = BASE.east(4);
		helper.setBlock(open.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(cramped.below(), Blocks.GRASS_BLOCK);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) clearAbove(helper, open.offset(dx, 0, dz));
		}
		clearAbove(helper, cramped);
		helper.setBlock(cramped.above(2), Blocks.GLASS);
		BlockPos openPos = helper.absolutePos(open);
		BlockPos crampedPos = helper.absolutePos(cramped);
		helper.startSequence().thenIdle(20).thenWaitUntil(() -> helper.assertTrue(level.getMaxLocalRawBrightness(openPos.above()) >= 9
			&& level.getMaxLocalRawBrightness(crampedPos.above()) >= 9, "The saplings have light")).thenExecute(() -> {
			level.setBlockAndUpdate(openPos, sapling.defaultBlockState());
			level.setBlockAndUpdate(crampedPos, sapling.defaultBlockState());
			helper.assertTrue(CabinGreenhouseGrowth.canProgress(sapling.defaultBlockState()), "Saplings grow in greenhouse catch-up");
			CabinGreenhouseGrowth.catchUp(level, java.util.Set.of(openPos, crampedPos), 4096L * 8192, 42);
			helper.assertTrue(level.getBlockState(openPos).is(Blocks.OAK_LOG), "A sapling with room grows into its tree while away");
			helper.assertTrue(level.getBlockState(crampedPos).is(sapling), "A sapling with too little headroom waits");

			var cinnamon = CookingPlants.CINNAMON_LOG;
			BlockPos trunk = helper.absolutePos(BASE.south(4));
			level.setBlockAndUpdate(trunk, cinnamon.defaultBlockState().setValue(HarvestTrunkBlock.READY, false));
			helper.assertTrue(CabinGreenhouseGrowth.canProgress(level.getBlockState(trunk)), "A stripped trunk grows back in catch-up");
			CabinGreenhouseGrowth.catchUp(level, java.util.Set.of(trunk), 4096L * 8192, 42);
			helper.assertTrue(level.getBlockState(trunk).getValue(HarvestTrunkBlock.READY), "Cinnamon bark grows back while away");
			helper.assertTrue(!CabinGreenhouseGrowth.canProgress(level.getBlockState(trunk)), "A ready trunk doesn't progress");

			helper.assertTrue(CabinGreenhouseGrowth.canProgress(CookingPlants.MUSSEL_BED.defaultBlockState())
				&& !CabinGreenhouseGrowth.canProgress(CookingPlants.MUSSEL_BED.defaultBlockState().setValue(SeaPlantBlock.AGE, SeaPlantBlock.MAX_AGE)),
				"Shellfish beds grow in catch-up until grown");
			helper.succeed();
		});
	}

	private static int count(java.util.List<ItemStack> drops, net.minecraft.world.item.Item item) {
		return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}
}
