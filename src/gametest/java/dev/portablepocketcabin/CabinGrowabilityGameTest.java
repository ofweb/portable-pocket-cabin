package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * B-0054: every food plant can be grown. Each plant with a Garden page has a planting item that places it, and a grown
 * plant multiplies: a perennial is harvested and stays, and an annual gives back more than one planting on average.
 */
public final class CabinGrowabilityGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);
	private static final int HARVESTS = 50;

	@GameTest
	public void everyFoodPlantCanBeGrownAndMultiplies(GameTestHelper helper) {
		var level = helper.getLevel();
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos pos = helper.absolutePos(BASE);
		List<String> problems = new ArrayList<>();
		for (var entry : CookingPlants.garden().entrySet()) {
			String plant = entry.getKey();
			Block block = entry.getValue();
			Item planted = CookingPlants.PLANTED_FROM.get(plant);
			if (!(planted instanceof BlockItem item)) {
				problems.add(plant + " has no planting item");
				continue;
			}
			boolean tree = block instanceof FruitingLeavesBlock || block instanceof HarvestTrunkBlock;
			if (item.getBlock() != block && !(tree && item.getBlock() instanceof FruitSaplingBlock)) {
				problems.add(plant + " is not planted by " + planted);
			}
			BlockState grown = grown(block.defaultBlockState());
			if (block instanceof HarvestTrunkBlock) {
				if (!grown.setValue(HarvestTrunkBlock.READY, false).isRandomlyTicking()) problems.add(plant + " doesn't grow back after a harvest");
				continue;
			}
			if (perennial(block)) {
				place(level, pos, block, grown);
				var result = level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
				if (result != InteractionResult.SUCCESS || !level.getBlockState(pos).is(block)) {
					problems.add(plant + " isn't harvested and kept: " + result + ", left " + level.getBlockState(pos));
				}
				continue;
			}
			int back = 0;
			for (int i = 0; i < HARVESTS; i++) {
				for (ItemStack stack : Block.getDrops(grown, level, pos, null)) {
					if (stack.is(planted)) back += stack.getCount();
				}
			}
			if (back <= HARVESTS) problems.add(plant + " gives back " + back + " " + planted + " over " + HARVESTS + " harvests");
		}
		helper.assertTrue(problems.isEmpty(), "Every food plant can be grown: " + problems);
		helper.succeed();
	}

	/** Perennials are harvested in place and grow again; annuals are broken to harvest. */
	private static boolean perennial(Block block) {
		return block instanceof HerbBushBlock || block instanceof TrellisCropBlock || block instanceof PerennialCropBlock
			|| block instanceof CardoonBlock || block instanceof FruitingLeavesBlock || block instanceof SeaPlantBlock sea && sea.gatheredByHand();
	}

	/** The plant at its last growth stage. */
	private static BlockState grown(BlockState state) {
		for (var property : state.getProperties()) {
			if (property instanceof IntegerProperty age && age.getName().equals("age")) {
				state = state.setValue(age, age.getPossibleValues().stream().max(Integer::compare).orElseThrow());
			}
		}
		return state;
	}

	/** Places the plant on ground it grows on, without block updates. */
	private static void place(net.minecraft.server.level.ServerLevel level, BlockPos pos, Block block, BlockState state) {
		BlockState ground = block instanceof SeaPlantBlock ? Blocks.STONE.defaultBlockState()
			: block instanceof HerbBushBlock && block == CookingPlants.OYSTER_MUSHROOM ? Blocks.OAK_LOG.defaultBlockState()
			: block instanceof HerbBushBlock || block instanceof CardoonBlock ? Blocks.GRASS_BLOCK.defaultBlockState()
			: Blocks.FARMLAND.defaultBlockState();
		level.setBlock(pos.below(), ground, Block.UPDATE_CLIENTS);
		level.setBlock(pos, state, Block.UPDATE_CLIENTS);
	}
}
