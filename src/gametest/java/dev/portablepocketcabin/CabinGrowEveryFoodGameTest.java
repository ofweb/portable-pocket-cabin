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
}
