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

	/** Clears the GameTest structure's barrier ceiling above a position, so the sky is open above it. */
	static void clearAbove(GameTestHelper helper, BlockPos pos) {
		var level = helper.getLevel();
		for (int y = 1; y < 16; y++) {
			BlockPos above = helper.absolutePos(pos.above(y));
			if (level.getBlockState(above).is(Blocks.BARRIER)) level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
		}
	}
}
