package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/** B-0050 delivery 2: taigas, cold waters, and coasts. */
public final class CabinNorthernGardenGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);
	static final String[] RECIPES = {
		"cutting/wasabi",
		"cutting/chopped_laver",
		"drying/nori",
		"drying/dried_sea_lettuce",
		"mill/aonori",
		"drying/dried_wakame",
		"drying/dried_blackcurrant",
		"drying/dried_raspberry",
		"mixing_bowl/dried_berries",
		"mill/crushed_blackcurrants",
		"press/blackcurrant_juice",
		"pot/cassis_syrup",
		"mixing_bowl/sirop_de_cassis",
		"pot/unstrained_solbaersaft",
		"strainer/solbaersaft",
		"pot/unstrained_redcurrant_jelly",
		"strainer/strained_redcurrant_jelly",
		"ageing/redcurrant_jelly",
		"mixing_bowl/buttered_crumbs",
		"skillet/toasted_sweet_crumbs",
		"counter/gammeldags_aeblekage",
		"pot/rodgrod",
		"counter/rodgrod_med_flode"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void wasabiGrowsOnlyBesideRunningWater(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.below(), Blocks.GRAVEL);
		helper.setBlock(BASE, CookingPlants.WASABI_CROP);
		var still = BASE.east(4);
		helper.setBlock(still.below(), Blocks.GRAVEL);
		helper.setBlock(still, CookingPlants.WASABI_CROP);
		helper.setBlock(still.east(), Blocks.WATER);
		helper.setBlock(BASE.west(), Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 2));
		var random = RandomSource.create(3);
		for (int i = 0; i < 400; i++) {
			for (BlockPos spot : new BlockPos[] {BASE, still}) {
				var pos = helper.absolutePos(spot);
				var state = level.getBlockState(pos);
				if (state.isRandomlyTicking()) state.randomTick(level, pos, random);
			}
		}
		var crop = (CropBlock) CookingPlants.WASABI_CROP;
		helper.assertTrue(crop.getAge(level.getBlockState(helper.absolutePos(BASE))) == 7, "Wasabi beside flowing water grows");
		helper.assertTrue(crop.getAge(level.getBlockState(helper.absolutePos(still))) == 0, "Wasabi beside still water waits");
		helper.succeed();
	}

	@GameTest
	public void seaweedsStandUnderwater(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE.below(), Blocks.SAND);
		helper.setBlock(BASE, Blocks.WATER);
		var pos = helper.absolutePos(BASE);
		helper.assertTrue(CookingPlants.WAKAME.defaultBlockState().canSurvive(level, pos), "Wakame can grow on a sandy sea floor");
		helper.setBlock(BASE, CookingPlants.WAKAME);
		helper.assertTrue(level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER), "Wakame stands in water");
		var drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, null);
		helper.assertTrue(CabinPlantGameTest.has(drops, CookingPlants.item("wakame")), "Breaking it gives wakame");
		var configured = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
		var spot = BASE.east(3);
		helper.setBlock(spot.below(), Blocks.GRAVEL);
		helper.setBlock(spot, Blocks.WATER);
		configured.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
			PortablePocketCabin.id("wild_laver_plant"))).value().place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), helper.absolutePos(spot));
		helper.assertTrue(level.getBlockState(helper.absolutePos(spot)).is(CookingPlants.LAVER), "Wild laver generates in water");
		helper.succeed();
	}
}
