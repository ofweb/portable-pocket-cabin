package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** B-0054: the composter makes compost, compost makes ground rich, and rich ground grows plants faster until it wears out. */
public final class CabinRichSoilGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);

	@GameTest
	public void aFullComposterGivesCompost(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, ComposterBlock.READY));
		BlockPos pos = helper.absolutePos(BASE);
		var container = ((ComposterBlock) Blocks.COMPOSTER).getContainer(level.getBlockState(pos), level, pos);
		helper.assertTrue(container.getItem(0).is(RichSoil.COMPOST), "A hopper finds compost in a full composter");
		helper.assertTrue(container.canTakeItemThroughFace(0, container.getItem(0), Direction.DOWN), "A hopper can take the compost");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
		helper.assertTrue(drops.stream().anyMatch(entity -> entity.getItem().is(RichSoil.COMPOST))
			&& drops.stream().noneMatch(entity -> entity.getItem().is(Items.BONE_MEAL)), "Emptying a full composter gives compost, not bone meal");
		helper.succeed();
	}

	@GameTest
	public void compostMakesGroundRichAndItStaysRich(GameTestHelper helper) {
		var level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(BASE, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
		compost(helper, player, BASE);
		var farmland = level.getBlockState(helper.absolutePos(BASE));
		helper.assertTrue(farmland.is(RichSoil.RICH_FARMLAND) && farmland.getValue(RichSoil.CHARGE) == RichSoil.MAX_CHARGE
			&& farmland.getValue(FarmlandBlock.MOISTURE) == 7, "Compost on farmland makes moist rich farmland, fully charged");
		helper.setBlock(BASE.east(2), Blocks.GRASS_BLOCK);
		compost(helper, player, BASE.east(2));
		helper.assertTrue(level.getBlockState(helper.absolutePos(BASE.east(2))).is(RichSoil.RICH_SOIL), "Compost on grass makes rich soil");
		helper.setBlock(BASE.east(2), RichSoil.RICH_SOIL.defaultBlockState().setValue(RichSoil.CHARGE, 1));
		compost(helper, player, BASE.east(2));
		helper.assertTrue(level.getBlockState(helper.absolutePos(BASE.east(2))).getValue(RichSoil.CHARGE) == RichSoil.MAX_CHARGE, "More compost tops it up");

		helper.setBlock(BASE.east(4), RichSoil.RICH_SOIL.defaultBlockState().setValue(RichSoil.CHARGE, 3));
		BlockPos soil = helper.absolutePos(BASE.east(4));
		var hoe = new ItemStack(Items.IRON_HOE);
		player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
		level.getBlockState(soil).useItemOn(hoe, level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false));
		var tilled = level.getBlockState(soil);
		helper.assertTrue(tilled.is(RichSoil.RICH_FARMLAND) && tilled.getValue(RichSoil.CHARGE) == 3, "A hoe tills rich soil into rich farmland, keeping its charge");
		FarmlandBlock.turnToDirt(null, tilled, level, soil);
		helper.assertTrue(level.getBlockState(soil).is(RichSoil.RICH_SOIL) && level.getBlockState(soil).getValue(RichSoil.CHARGE) == 3,
			"Rich farmland that dries out or is trampled becomes rich soil");
		helper.assertTrue(Blocks.WHEAT.defaultBlockState().canSurvive(level, helper.absolutePos(BASE.above())), "Wheat grows on rich farmland");
		helper.succeed();
	}

	@GameTest
	public void richGroundFeedsThePlantAndWearsOut(GameTestHelper helper) {
		var level = helper.getLevel();
		var original = PlantNeeds.of(CookingPlants.BARLEY_CROP);
		try {
			PlantNeeds.override(CookingPlants.BARLEY_CROP, new PlantNeeds(List.of("minecraft:farmland"), 0, PlantNeeds.Water.NONE,
				PlantNeeds.Support.NONE, 1.0F, false, 0F));
			helper.setBlock(BASE, RichSoil.RICH_FARMLAND.defaultBlockState().setValue(RichSoil.CHARGE, 2).setValue(FarmlandBlock.MOISTURE, 7));
			helper.setBlock(BASE.above(), CookingPlants.BARLEY_CROP);
			BlockPos ground = helper.absolutePos(BASE);
			BlockPos plant = ground.above();
			helper.assertTrue(level.getBlockState(plant).is(CookingPlants.BARLEY_CROP), "Barley grows on rich farmland");
			level.getBlockState(ground).randomTick(level, ground, level.getRandom());
			helper.assertTrue(level.getBlockState(plant).getValue(CropBlock.AGE) == 1, "Each tick of rich ground gives the plant above an extra tick");

			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			var ripe = CookingPlants.BARLEY_CROP.defaultBlockState().setValue(CropBlock.AGE, 7);
			PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, plant, CookingPlants.BARLEY_CROP.defaultBlockState(), null);
			helper.assertTrue(level.getBlockState(ground).getValue(RichSoil.CHARGE) == 2, "Breaking an unripe crop spends nothing");
			PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, plant, ripe, null);
			helper.assertTrue(level.getBlockState(ground).getValue(RichSoil.CHARGE) == 1, "A ripe harvest spends one charge");
			PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, plant, ripe, null);
			helper.assertTrue(level.getBlockState(ground).is(Blocks.FARMLAND), "With no charge left, rich farmland turns back into farmland");

			helper.setBlock(BASE.east(3), RichSoil.RICH_SOIL.defaultBlockState().setValue(RichSoil.CHARGE, 4));
			helper.setBlock(BASE.east(3).above(), CookingPlants.THYME_BUSH.defaultBlockState().setValue(HerbBushBlock.AGE, HerbBushBlock.MAX_AGE));
			BlockPos bush = helper.absolutePos(BASE.east(3).above());
			level.getBlockState(bush).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(bush), Direction.UP, bush, false));
			helper.assertTrue(level.getBlockState(bush.below()).getValue(RichSoil.CHARGE) == 3, "Picking a ripe bush on rich soil spends one charge");
		} finally {
			PlantNeeds.override(CookingPlants.BARLEY_CROP, original);
		}
		helper.succeed();
	}

	@GameTest
	public void richGroundDoublesGreenhouseCatchUp(GameTestHelper helper) {
		var level = helper.getLevel();
		var original = PlantNeeds.of(CookingPlants.BARLEY_CROP);
		try {
			PlantNeeds.override(CookingPlants.BARLEY_CROP, new PlantNeeds(List.of("minecraft:farmland"), 0, PlantNeeds.Water.NONE,
				PlantNeeds.Support.NONE, 1.0F, false, 0F));
			helper.setBlock(BASE, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
			helper.setBlock(BASE.east(2), RichSoil.RICH_FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
			helper.setBlock(BASE.above(), CookingPlants.BARLEY_CROP);
			helper.setBlock(BASE.east(2).above(), CookingPlants.BARLEY_CROP);
			BlockPos plain = helper.absolutePos(BASE.above());
			BlockPos rich = helper.absolutePos(BASE.east(2).above());
			CabinGreenhouseGrowth.catchUp(level, java.util.Set.of(plain, rich), 4096L * 4, 42);
			helper.assertTrue(level.getBlockState(plain).getValue(CropBlock.AGE) == 4, "Four rounds of catch-up grow plain-ground barley four stages, found "
				+ level.getBlockState(plain).getValue(CropBlock.AGE));
			helper.assertTrue(level.getBlockState(rich).getValue(CropBlock.AGE) == 7, "Rich ground doubles catch-up, so the same rounds ripen it, found "
				+ level.getBlockState(rich).getValue(CropBlock.AGE));
			helper.assertTrue(level.getBlockState(rich.below()).getValue(RichSoil.CHARGE) == RichSoil.MAX_CHARGE, "Catch-up harvests nothing, so spends no charge");
		} finally {
			PlantNeeds.override(CookingPlants.BARLEY_CROP, original);
		}
		helper.succeed();
	}

	private static void compost(GameTestHelper helper, Player player, BlockPos relative) {
		BlockPos pos = helper.absolutePos(relative);
		var stack = new ItemStack(RichSoil.COMPOST, 4);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
	}
}
