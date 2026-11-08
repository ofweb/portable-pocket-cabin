package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** B-0049 delivery 4: fish preparation, seafood, and the recipes they complete. */
public final class CabinSeafoodGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);
	static final String[] RECIPES = {
		"cutting/peeled_prawns",
		"skillet/toasted_prawn_shells",
		"pot/unstrained_prawn_stock",
		"strainer/prawn_stock",
		"pot/unstrained_fish_stock",
		"strainer/fish_stock",
		"pot/boiled_prawns",
		"drying/dried_shrimp",
		"mortar/pounded_shrimp",
		"crock/fermented_shrimp",
		"drying/shrimp_paste",
		"crock/fermenting_anchovies",
		"crock/fermenting_fish_guts",
		"press/fish_sauce",
		"brine/salted_anchovies",
		"brine/salted_salmon_fillet",
		"grill/grilled_salted_salmon",
		"cutting/salted_salmon",
		"brine/salted_herring",
		"mixing_bowl/herring_pickling_liquid",
		"crock/pickled_herring",
		"brine/salted_tuna_loin",
		"drying/katsuobushi",
		"cutting/bonito_flakes",
		"pot/unstrained_dashi",
		"strainer/dashi",
		"mixing_bowl/worcestershire_mash",
		"crock/unstrained_worcestershire_sauce",
		"strainer/worcestershire_sauce",
		"pot/tonkatsu_sauce",
		"mill/ground_peanuts",
		"mixing_bowl/green_curry_mix",
		"mortar/green_curry_paste",
		"mixing_bowl/red_curry_mix",
		"mortar/red_curry_paste",
		"pot/peanut_sauce",
		"pot/miang_sauce",
		"mixing_bowl/nam_prik_mix",
		"skillet/fried_nam_prik_mix",
		"mortar/nam_prik_paste",
		"pot/nam_prik_pao",
		"meat_grinder/liver_mince",
		"mixing_bowl/leverpostej_mix",
		"oven/leverpostej",
		"pot/moules_marinieres",
		"counter/unbaked_pissaladiere",
		"oven/pissaladiere",
		"counter/fischbrotchen",
		"skillet/browned_mince",
		"mixing_bowl/korokke_mash",
		"counter/raw_korokke",
		"pot/fried_korokke",
		"counter/korokke",
		"cutting/shredded_papaya",
		"mortar/chili_garlic",
		"mixing_bowl/som_tam_dressing",
		"counter/som_tam",
		"cutting/sliced_galangal",
		"pot/tom_kha_base",
		"counter/tom_kha_gai",
		"pot/tom_yum_broth",
		"pot/tom_yum_goong",
		"pot/soaked_rice_noodles",
		"mixing_bowl/pad_thai_sauce",
		"wok/stir_fried_rice_noodles",
		"counter/pad_thai",
		"pot/green_curry_sauce",
		"pot/green_curry",
		"mixing_bowl/miang_filling",
		"counter/miang_kham",
		"cutting/diced_white_fish",
		"mixing_bowl/lime_cured_fish",
		"pot/boiled_sweet_potato",
		"counter/ceviche",
		"mixing_bowl/lime_marinated_fish",
		"mixing_bowl/moqueca_vegetables",
		"pot/moqueca_baiana",
		"cutting/sliced_rugbrod",
		"counter/sildemad_med_karrysalat",
		"counter/smorrebrod_med_leverpostej",
		"meat_grinder/minced_fish",
		"mixing_bowl/fiskefrikadeller_fars",
		"skillet/fried_fiskefrikadeller",
		"counter/fiskefrikadeller"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void theKnifeScalesGutsAndFilletsFish(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, Butchery.BUTCHER_BLOCK);
		BlockPos pos = helper.absolutePos(BASE);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.getInventory().setSelectedSlot(8);
		var hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
		use(level, player, pos, hit, new ItemStack(Items.COD));
		ItemStack cleaver = new ItemStack(Butchery.CLEAVER);
		use(level, player, pos, hit, cleaver);
		helper.assertTrue(cleaver.getDamageValue() == 0, "Fish want the knife, not the cleaver");
		use(level, player, pos, hit, new ItemStack(CookingItems.KITCHEN_KNIFE));
		helper.assertTrue(player.getInventory().countItem(Butchery.PARTS.get("fish_scales")) == 1 && player.getInventory().countItem(Butchery.PARTS.get("fish_guts")) == 1
			&& player.getInventory().countItem(Butchery.PARTS.get("white_fish_fillet")) == 0, "The knife first scales and guts the cod");
		player.setShiftKeyDown(true);
		use(level, player, pos, hit, new ItemStack(CookingItems.KITCHEN_KNIFE));
		player.setShiftKeyDown(false);
		helper.assertTrue(player.getInventory().countItem(Butchery.PARTS.get("white_fish_fillet")) == 2 && player.getInventory().countItem(Butchery.PARTS.get("cod_roe")) == 1,
			"Then it fillets the cod");
		use(level, player, pos, hit, new ItemStack(Butchery.SQUID));
		player.setShiftKeyDown(true);
		use(level, player, pos, hit, new ItemStack(CookingItems.KITCHEN_KNIFE));
		player.setShiftKeyDown(false);
		helper.assertTrue(player.getInventory().countItem(Butchery.PARTS.get("squid_tentacles")) == 2 && player.getInventory().countItem(Items.INK_SAC) == 1,
			"A squid cleans into tentacles and an ink sac");
		use(level, player, pos, hit, new ItemStack(Items.PUFFERFISH));
		player.setShiftKeyDown(true);
		use(level, player, pos, hit, new ItemStack(CookingItems.KITCHEN_KNIFE));
		player.setShiftKeyDown(false);
		helper.assertTrue(player.getInventory().countItem(Butchery.PARTS.get("fish_bones")) == 2, "Cod and pufferfish give fish bones");
		var friedCod = CookingStation.SKILLET.find(level, CookingInput.of(java.util.List.of(new ItemStack(Butchery.PARTS.get("white_fish_fillet"))))).orElse(null);
		helper.assertTrue(friedCod != null, "A white fish fillet fries like cod");
		helper.succeed();
	}

	@GameTest
	public void prawnTrapsCatchInWaterAndShellfishAreGathered(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, Butchery.PRAWN_TRAP.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
		BlockPos pos = helper.absolutePos(BASE);
		var trap = (PrawnTrapBlockEntity) level.getBlockEntity(pos);
		trap.tick(level, level.getBlockState(pos));
		trap.rewind(PrawnTrapBlockEntity.TICKS_PER_PRAWN * 20L);
		trap.tick(level, level.getBlockState(pos));
		helper.assertTrue(trap.getItem(0).getCount() == PrawnTrapBlockEntity.MAX_PRAWNS, "A trap in water catches up to eight prawns, found " + trap.getItem(0));
		helper.setBlock(BASE.east(2), Butchery.PRAWN_TRAP);
		var dry = (PrawnTrapBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2)));
		dry.tick(level, level.getBlockState(helper.absolutePos(BASE.east(2))));
		dry.rewind(PrawnTrapBlockEntity.TICKS_PER_PRAWN * 20L);
		dry.tick(level, level.getBlockState(helper.absolutePos(BASE.east(2))));
		helper.assertTrue(dry.getItem(0).isEmpty(), "A trap out of water catches nothing");

		helper.setBlock(BASE.south(2).below(), Blocks.STONE);
		helper.setBlock(BASE.south(2), Blocks.WATER);
		helper.setBlock(BASE.south(2), CookingPlants.MUSSEL_BED);
		BlockPos bed = helper.absolutePos(BASE.south(2));
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		level.getBlockState(bed).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(bed), net.minecraft.core.Direction.UP, bed, false));
		helper.assertTrue(level.getBlockState(bed).is(Blocks.WATER) && level.getEntitiesOfClass(ItemEntity.class, new AABB(bed).inflate(2)).stream()
			.anyMatch(entity -> entity.getItem().is(CookingPlants.item("mussels"))), "Mussels are gathered by hand, leaving water");

		var squid = helper.spawn(EntityTypes.SQUID, BASE.above(3));
		var area = new AABB(squid.blockPosition()).inflate(3);
		squid.kill(level);
		helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).stream().anyMatch(entity -> entity.getItem().is(Butchery.SQUID)), "Squid drop whole");
		helper.succeed();
	}

	private static void use(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player, BlockPos pos, BlockHitResult hit, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		var result = level.getBlockState(pos).useItemOn(stack, level, player, InteractionHand.MAIN_HAND, hit);
		if (result == net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND) level.getBlockState(pos).useWithoutItem(level, player, hit);
	}
}
