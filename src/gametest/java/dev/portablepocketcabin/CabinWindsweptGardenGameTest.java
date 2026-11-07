package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** B-0050 delivery 4: windswept savannas and hills. */
public final class CabinWindsweptGardenGameTest {
	private static final BlockPos LEAVES = new BlockPos(2, 2, 2);
	static final String[] RECIPES = {
		"mill/olive_paste",
		"press/unsettled_olive_oil",
		"ageing/olive_oil",
		"drying/dried_oregano",
		"drying/dried_rosemary",
		"drying/dried_savory",
		"drying/dried_thyme",
		"mixing_bowl/herbes_de_provence",
		"mixing_bowl/anticucho_marinade",
		"cutting/diced_aubergine",
		"cutting/diced_courgette",
		"cutting/diced_bell_pepper",
		"wok/sauteed_vegetables",
		"pot/ratatouille"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void oliveLeavesGiveGreenOlivesThenBlackOlives(GameTestHelper helper) {
		var level = helper.getLevel();
		var stage = FruitingLeavesBlock.TwoStageFruit.STAGE;
		BlockPos pos = helper.absolutePos(LEAVES);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		var leaves = CookingPlants.OLIVE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		level.setBlockAndUpdate(pos, leaves.setValue(stage, 1));
		helper.assertTrue(level.getBlockState(pos).useWithoutItem(level, player, hit) == net.minecraft.world.InteractionResult.PASS,
			"Flowering olive leaves have nothing to pick");
		level.setBlockAndUpdate(pos, leaves.setValue(stage, 2));
		helper.assertTrue(level.getBlockState(pos).isRandomlyTicking(), "Green olives keep ripening on the tree");
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		helper.assertTrue(level.getBlockState(pos).getValue(stage) == 0 && dropped(helper, pos, new ItemStack(CookingPlants.GREEN_OLIVE)),
			"Olive leaves at the first fruit stage give green olives");
		clearDrops(helper, pos);
		level.setBlockAndUpdate(pos, leaves.setValue(stage, 3));
		helper.assertTrue(!level.getBlockState(pos).isRandomlyTicking(), "Black olives are the last stage");
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		helper.assertTrue(level.getBlockState(pos).getValue(stage) == 0 && dropped(helper, pos, new ItemStack(CookingPlants.BLACK_OLIVE))
			&& !dropped(helper, pos, new ItemStack(CookingPlants.GREEN_OLIVE)), "Olive leaves left one stage longer give black olives");
		helper.assertTrue(CookingPlants.GIVES.get("olive_leaves").equals(java.util.List.of(CookingPlants.GREEN_OLIVE, CookingPlants.BLACK_OLIVE)),
			"The garden page lists both olives");
		helper.assertTrue(new ItemStack(CookingPlants.item("olive_oil")).is(CookingItems.COOKING_OILS), "Olive oil fills the pot and wok");
		helper.succeed();
	}

	private static boolean dropped(GameTestHelper helper, BlockPos pos, ItemStack stack) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
			.anyMatch(entity -> ItemStack.isSameItem(entity.getItem(), stack));
	}

	private static void clearDrops(GameTestHelper helper, BlockPos pos) {
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(ItemEntity::discard);
	}
}
