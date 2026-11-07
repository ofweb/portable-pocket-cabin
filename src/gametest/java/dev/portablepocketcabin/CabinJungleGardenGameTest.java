package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** B-0050 delivery 6: jungles. */
public final class CabinJungleGardenGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);
	static final String[] RECIPES = {
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void cinnamonBarkStripsWithAnAxeAndGrowsBack(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(BASE);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		level.setBlockAndUpdate(pos, CookingPlants.CINNAMON_LOG.defaultBlockState());
		helper.assertTrue(level.getBlockState(pos).getValue(HarvestTrunkBlock.READY), "A cinnamon trunk has its bark");
		var axe = new ItemStack(Items.IRON_AXE);
		player.setItemInHand(InteractionHand.MAIN_HAND, axe);
		var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
		level.getBlockState(pos).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(level.getBlockState(pos).is(CookingPlants.CINNAMON_LOG) && !level.getBlockState(pos).getValue(HarvestTrunkBlock.READY),
			"An axe strips the bark and leaves the trunk standing");
		helper.assertTrue(axe.getDamageValue() == 1, "Stripping wears the axe");
		helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
			.anyMatch(entity -> entity.getItem().is(CookingPlants.CINNAMON_BARK)), "Stripping drops cinnamon bark");
		helper.assertTrue(level.getBlockState(pos).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit)
			== net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND, "A stripped trunk has no bark to take");
		var random = RandomSource.create(9);
		for (int i = 0; i < 400 && !level.getBlockState(pos).getValue(HarvestTrunkBlock.READY); i++) level.getBlockState(pos).randomTick(level, pos, random);
		helper.assertTrue(level.getBlockState(pos).getValue(HarvestTrunkBlock.READY), "The bark grows back");
		helper.assertTrue(level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS), "Cinnamon trunks keep their leaves alive");
		helper.succeed();
	}

	@GameTest
	public void palmTrunksFillWithSapForAGlassBottle(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(BASE);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		level.setBlockAndUpdate(pos, CookingPlants.PALM_LOG.defaultBlockState());
		var bottles = new ItemStack(Items.GLASS_BOTTLE, 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, bottles);
		var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
		level.getBlockState(pos).useItemOn(bottles, level, player, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(bottles.getCount() == 2, "A new palm trunk has no sap yet");
		var random = RandomSource.create(4);
		for (int i = 0; i < 400 && !level.getBlockState(pos).getValue(HarvestTrunkBlock.READY); i++) level.getBlockState(pos).randomTick(level, pos, random);
		helper.assertTrue(level.getBlockState(pos).getValue(HarvestTrunkBlock.READY), "The trunk fills with sap");
		level.getBlockState(pos).useItemOn(bottles, level, player, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(bottles.getCount() == 1 && player.getInventory().countItem(CookingPlants.PALM_SAP) == 1
			&& !level.getBlockState(pos).getValue(HarvestTrunkBlock.READY), "A glass bottle taps the sap");
		helper.setBlock(BASE.east(2).below(), Blocks.SAND);
		var coconut = ((net.minecraft.world.item.BlockItem) CookingPlants.item("coconut_sapling")).getBlock();
		helper.assertTrue(coconut.defaultBlockState().canSurvive(level, helper.absolutePos(BASE.east(2))), "Coconut palms grow on sand");
		var lime = ((net.minecraft.world.item.BlockItem) CookingPlants.item("lime_sapling")).getBlock();
		helper.assertTrue(!lime.defaultBlockState().canSurvive(level, helper.absolutePos(BASE.east(2))), "Other fruit trees don't");
		helper.succeed();
	}
}
