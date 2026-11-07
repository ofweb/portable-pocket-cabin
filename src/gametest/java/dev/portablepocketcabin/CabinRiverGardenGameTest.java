package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** B-0050 delivery 5: rivers, swamps, cherry groves, and bamboo jungles. */
public final class CabinRiverGardenGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);
	static final String[] RECIPES = {
		"drying/dried_azuki_beans",
		"drying/dried_mint",
		"drying/dried_soybeans",
		"drying/star_anise",
		"crock/bean_sprouts",
		"mixing_bowl/soaked_soybeans",
		"mill/soybean_mash",
		"pot/unstrained_soy_milk",
		"strainer/soy_milk",
		"mixing_bowl/soy_curds",
		"cheese_press/tofu",
		"mill/rice_flour",
		"pot/steamed_rice",
		"crock/soaked_glutinous_rice",
		"mill/wet_glutinous_rice_flour",
		"drying/glutinous_rice_flour",
		"mixing_bowl/rice_noodle_batter",
		"pot/rice_sheets",
		"cutting/fresh_rice_noodles",
		"drying/rice_noodles",
		"pot/steamed_tea_leaves",
		"counter/rolled_tea_leaves",
		"oven/sencha_leaves",
		"drying/withered_tea_leaves",
		"counter/bruised_tea_leaves",
		"ageing/oxidised_tea_leaves",
		"oven/black_tea",
		"pot/steamed_shaded_tea_leaves",
		"drying/tencha",
		"mill/matcha_powder",
		"pot/sencha",
		"skillet/hojicha_leaves",
		"pot/hojicha",
		"mixing_bowl/matcha",
		"pot/pfefferminztee",
		"mixing_bowl/unstrained_mint_syrup",
		"strainer/mint_syrup",
		"mixing_bowl/menthe_a_leau",
		"mixing_bowl/citronnade_maison",
		"crock/salted_ume",
		"drying/umeboshi",
		"counter/onigiri",
		"pot/simmered_azuki_beans",
		"mortar/azuki_mash",
		"pot/anko",
		"mixing_bowl/dorayaki_batter",
		"skillet/dorayaki_pancakes",
		"counter/dorayaki",
		"mixing_bowl/taiyaki_batter",
		"oven/taiyaki",
		"pot/steamed_mochi_dough",
		"mortar/mochi",
		"counter/daifuku_mochi",
		"pot/cherry_compote",
		"strainer/cherry_sauce"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void riceGrowsStandingInWaterAndLeavesTheWater(GameTestHelper helper) {
		var level = helper.getLevel();
		var rice = (CropBlock) CookingPlants.RICE_CROP;
		BlockPos pos = helper.absolutePos(BASE);
		helper.setBlock(BASE.below(), Blocks.MUD);
		helper.setBlock(BASE, Blocks.WATER);
		helper.assertTrue(rice.defaultBlockState().canSurvive(level, pos), "Rice stands in water one block deep over mud");
		level.setBlockAndUpdate(pos, rice.getStateForAge(0));
		helper.assertTrue(level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource(), "A rice plant holds its water");
		var random = RandomSource.create(3);
		for (int i = 0; i < 400 && !rice.isMaxAge(level.getBlockState(pos)); i++) level.getBlockState(pos).randomTick(level, pos, random);
		helper.assertTrue(rice.isMaxAge(level.getBlockState(pos)), "Rice ripens standing in water, found " + level.getBlockState(pos) + " needs " + PlantNeeds.of(rice) + " can grow "
			+ PlantNeeds.of(rice).canGrow(level, pos) + " below " + level.getBlockState(pos.below()) + " light " + level.getRawBrightness(pos, 0) + " fluid " + level.getFluidState(pos));
		level.destroyBlock(pos, true);
		helper.assertTrue(level.getBlockState(pos).is(Blocks.WATER), "Harvesting rice leaves the water behind");
		helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
			.anyMatch(entity -> entity.getItem().is(CookingPlants.RICE)), "Ripe rice drops rice");
		helper.setBlock(BASE.above(), Blocks.WATER);
		helper.assertTrue(!rice.defaultBlockState().canSurvive(level, pos), "Rice can't grow under deeper water");
		helper.setBlock(BASE.above(), Blocks.AIR);
		helper.setBlock(BASE.east().below(), Blocks.FARMLAND);
		var seedlings = new ItemStack(CookingPlants.item("rice_seedlings"));
		player(helper).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, seedlings);
		var onDryFarmland = new net.minecraft.world.item.context.BlockPlaceContext(player(helper), net.minecraft.world.InteractionHand.MAIN_HAND, seedlings,
			new BlockHitResult(Vec3.atCenterOf(pos.east().below()), Direction.UP, pos.east().below(), false));
		helper.assertTrue(rice.getStateForPlacement(onDryFarmland) == null, "Rice seedlings can't be planted on dry farmland");
		helper.succeed();
	}

	@GameTest
	public void teaRipenedUnderARoofGivesShadedLeaves(GameTestHelper helper) {
		var level = helper.getLevel();
		var tea = CookingPlants.TEA_BUSH;
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos open = helper.absolutePos(BASE);
		BlockPos roofed = helper.absolutePos(BASE.east(2));
		for (BlockPos pos : new BlockPos[] {open, roofed}) {
			level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
			level.setBlockAndUpdate(pos, tea.defaultBlockState().setValue(HerbBushBlock.AGE, 2));
		}
		for (int y = 1; y < 16; y++) {
			for (BlockPos pos : new BlockPos[] {open.above(y), roofed.above(y)}) {
				if (level.getBlockState(pos).is(Blocks.BARRIER)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
		}
		helper.assertTrue(!PlantNeeds.roofed(level, open), "The open tea bush stands under the sky");
		level.setBlockAndUpdate(roofed.above(2), Blocks.OAK_PLANKS.defaultBlockState());
		var random = RandomSource.create(5);
		for (int i = 0; i < 400 && (level.getBlockState(open).getValue(HerbBushBlock.AGE) < 3 || level.getBlockState(roofed).getValue(HerbBushBlock.AGE) < 3); i++) {
			level.getBlockState(open).randomTick(level, open, random);
			level.getBlockState(roofed).randomTick(level, roofed, random);
		}
		helper.assertTrue(!level.getBlockState(open).getValue(HerbBushBlock.Tea.SHADED) && level.getBlockState(roofed).getValue(HerbBushBlock.Tea.SHADED),
			"Tea remembers whether it ripened under a roof, found " + level.getBlockState(open) + " and " + level.getBlockState(roofed)
			+ " open y " + open.getY() + " height " + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, open.getX(), open.getZ())
			+ " at " + level.getBlockState(open.atY(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, open.getX(), open.getZ()) - 1)) + " roofed " + PlantNeeds.roofed(level, open) + " " + PlantNeeds.roofed(level, roofed));
		for (BlockPos pos : new BlockPos[] {open, roofed}) {
			level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		}
		helper.assertTrue(dropped(helper, open, CookingPlants.TEA_LEAF) && dropped(helper, roofed, CookingPlants.SHADED_TEA_LEAF)
			&& !dropped(helper, roofed, CookingPlants.TEA_LEAF), "Open tea gives tea leaves, and roofed tea gives shaded tea leaves");
		helper.succeed();
	}

	private static net.minecraft.world.entity.player.Player player(GameTestHelper helper) {
		return helper.makeMockPlayer(GameType.SURVIVAL);
	}

	private static boolean dropped(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item item) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.9)).stream().anyMatch(entity -> entity.getItem().is(item));
	}
}
