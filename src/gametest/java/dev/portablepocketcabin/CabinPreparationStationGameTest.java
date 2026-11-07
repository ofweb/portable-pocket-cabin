package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Phase 3 preparation stations: the mill, mixing bowl, mortar and pestle, butter churn, and cheese press. */
public final class CabinPreparationStationGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void spoonWorksTheMixingBowlOnACounter(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.assertTrue(!CookingBlocks.MIXING_BOWL.defaultBlockState().canSurvive(level, helper.absolutePos(BASE)),
			"A mixing bowl needs a counter");
		helper.setBlock(BASE, CookingBlocks.PREP_COUNTER);
		var bowl = station(helper, BASE.above(), CookingBlocks.MIXING_BOWL);
		var counter = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		helper.assertTrue(counter.covered(), "The bowl covers the counter, pausing its hand work");
		var kind = CookingMenu.Kind.MIXING_BOWL;
		bowl.setItem(0, new ItemStack(Items.WHEAT, 2));
		bowl.setItem(1, new ItemStack(Items.SUGAR));
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (int i = 0; i < 20; i++) ProcessStationBlockEntity.serverTick(level, bowl.getBlockPos(), bowl.getBlockState(), bowl);
		helper.assertTrue(bowl.progress() == 0, "Time alone does not mix");
		use(helper, player, new ItemStack(CookingItems.SPOON), BASE.above());
		use(helper, player, new ItemStack(CookingItems.SPOON), BASE.above());
		helper.assertTrue(bowl.progress() == 2 && bowl.getItem(kind.outputSlot).isEmpty(), "Each spoon use mixes once");
		use(helper, player, new ItemStack(Items.STICK), BASE.above());
		helper.assertTrue(bowl.progress() == 2, "Another item does not mix");
		use(helper, player, new ItemStack(CookingItems.SPOON), BASE.above());
		helper.assertTrue(bowl.getItem(kind.outputSlot).is(Items.COOKIE) && bowl.getItem(0).isEmpty(), "The last use finishes the batch");
		helper.succeed();
	}

	@GameTest
	public void pestleGrindsInTheMortar(GameTestHelper helper) {
		helper.setBlock(BASE, CookingBlocks.PREP_COUNTER);
		var mortar = station(helper, BASE.above(), CookingBlocks.MORTAR);
		mortar.setItem(0, new ItemStack(Items.GRAVEL, 2));
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		use(helper, player, new ItemStack(CookingItems.PESTLE), BASE.above());
		use(helper, player, new ItemStack(CookingItems.PESTLE), BASE.above());
		helper.assertTrue(mortar.getItem(CookingMenu.Kind.MORTAR.outputSlot).is(Items.FLINT), "Two pestle uses grind the gravel");
		helper.succeed();
	}

	@GameTest
	public void churnWorksByHandAndFillsBothByproductSlots(GameTestHelper helper) {
		var churn = station(helper, BASE, CookingBlocks.BUTTER_CHURN);
		var kind = CookingMenu.Kind.CHURN;
		churn.setItem(0, new ItemStack(Items.SNOWBALL, 2));
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		hand(helper, player, BASE);
		helper.assertTrue(churn.progress() == 1, "An empty hand churns once");
		hand(helper, player, BASE);
		helper.assertTrue(churn.getItem(kind.outputSlot).is(Items.SNOW_BLOCK) && churn.getItem(kind.byproductSlots[0]).is(Items.STICK)
			&& churn.getItem(kind.byproductSlots[1]).is(Items.STRING), "Churning finishes with both byproducts");
		helper.succeed();
	}

	@GameTest
	public void millAndCheesePressWorkOverTime(GameTestHelper helper) {
		var level = helper.getLevel();
		var mill = station(helper, BASE, CookingBlocks.MILL);
		var press = station(helper, BASE.east(2), CookingBlocks.CHEESE_PRESS);
		mill.setItem(0, new ItemStack(Items.COBBLESTONE));
		press.setItem(0, new ItemStack(Items.CLAY_BALL, 2));
		for (int i = 0; i < 20; i++) {
			ProcessStationBlockEntity.serverTick(level, mill.getBlockPos(), mill.getBlockState(), mill);
			ProcessStationBlockEntity.serverTick(level, press.getBlockPos(), press.getBlockState(), press);
		}
		helper.assertTrue(mill.getItem(CookingMenu.Kind.MILL.outputSlot).is(Items.GRAVEL), "The mill grinds over time");
		helper.assertTrue(press.getItem(CookingMenu.Kind.CHEESE_PRESS.outputSlot).is(Items.BRICK), "The cheese press presses over time");
		helper.succeed();
	}

	private static ProcessStationBlockEntity station(GameTestHelper helper, BlockPos pos, Block block) {
		helper.setBlock(pos, block);
		return (ProcessStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
	}

	private static void use(GameTestHelper helper, Player player, ItemStack stack, BlockPos relative) {
		var pos = helper.absolutePos(relative);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		var result = helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
		if (result instanceof net.minecraft.world.InteractionResult.TryEmptyHandInteraction) {
			helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit);
		}
	}

	private static void hand(GameTestHelper helper, Player player, BlockPos relative) {
		var pos = helper.absolutePos(relative);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}
}
