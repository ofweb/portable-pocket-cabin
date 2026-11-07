package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** B-0047: rock salt and sea salt, coarse salt, and sugars and syrups. */
public final class CabinSaltAndSugarGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void rockSaltOreDropsSaltAndGenerates(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = helper.absolutePos(BASE);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		var drops = Block.getDrops(CookingBlocks.ROCK_SALT_ORE.defaultBlockState(), level, pos, null, player, new ItemStack(Items.STONE_PICKAXE));
		helper.assertTrue(drops.size() == 1 && drops.getFirst().is(CookingItems.ROCK_SALT) && drops.getFirst().getCount() >= 1,
			"Mining rock salt ore drops rock salt, found " + drops);
		var silk = new ItemStack(Items.IRON_PICKAXE);
		silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		var silkDrops = Block.getDrops(CookingBlocks.ROCK_SALT_ORE.defaultBlockState(), level, pos, null, player, silk);
		helper.assertTrue(silkDrops.getFirst().is(CookingBlocks.ROCK_SALT_ORE.asItem()), "Silk Touch keeps the ore");
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(CookingWorld.ROCK_SALT_ORE).isPresent(),
			"The rock salt ore feature loads");
		var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		helper.assertTrue(CookingWorld.isSea(biomes.getOrThrow(Biomes.DEEP_OCEAN)) && !CookingWorld.isSea(biomes.getOrThrow(Biomes.RIVER)),
			"Only ocean water fills seawater");
		helper.succeed();
	}

	@GameTest
	public void seawaterBoilsDownIntoSaltAndNigari(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		pot.setItem(0, new ItemStack(CookingItems.SEAWATER, 3));
		stove.setItem(0, new ItemStack(Items.BLAZE_ROD));
		for (int i = 0; i < 305; i++) {
			StoveBlockEntity.serverTick(level, stove.getBlockPos(), stove.getBlockState(), stove);
			CookingPotBlockEntity.serverTick(level, pot.getBlockPos(), pot.getBlockState(), pot);
		}
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.WET_SALT), "Seawater boils down to wet salt");
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.BYPRODUCT_SLOT).is(CookingItems.NIGARI)
			&& pot.getItem(CookingPotBlockEntity.SECOND_BYPRODUCT_SLOT).is(Items.GLASS_BOTTLE)
			&& pot.getItem(CookingPotBlockEntity.SECOND_BYPRODUCT_SLOT).getCount() == 2, "It leaves nigari and two bottles");

		helper.setBlock(BASE.east(2), CookingBlocks.DRYING_RACK);
		helper.setBlock(BASE.east(4), CookingBlocks.DRYING_RACK);
		helper.setBlock(BASE.east(4).above(), Blocks.STONE);
		var open = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2)));
		var shaded = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(4)));
		boolean sky = SpotStationBlockEntity.openSky(level, helper.absolutePos(BASE.east(2)));
		open.add(level, new ItemStack(CookingItems.WET_SALT));
		shaded.add(level, new ItemStack(CookingItems.WET_SALT));
		for (var rack : List.of(open, shaded)) {
			SpotStationBlockEntity.serverTick(level, rack.getBlockPos(), rack.getBlockState(), rack);
			rack.rewind(3 * CookingStation.DAY);
			SpotStationBlockEntity.serverTick(level, rack.getBlockPos(), rack.getBlockState(), rack);
		}
		helper.assertTrue(open.getItem(0).is(sky ? CookingItems.SALT : CookingItems.COARSE_SALT) && open.getItem(0).getCount() == 2,
			"Wet salt dries on the rack");
		helper.assertTrue(shaded.getItem(0).is(CookingItems.COARSE_SALT), "In the shade it grows coarse salt");

		helper.setBlock(BASE.south(2), CookingBlocks.PREP_COUNTER);
		helper.setBlock(BASE.south(2).above(), CookingBlocks.MORTAR);
		var mortar = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.south(2).above()));
		mortar.setItem(0, new ItemStack(CookingItems.COARSE_SALT));
		for (int i = 0; i < 4; i++) mortar.work(level);
		helper.assertTrue(mortar.getItem(CookingMenu.Kind.MORTAR.outputSlot).is(CookingItems.SALT), "The pestle grinds coarse salt fine");

		helper.setBlock(BASE.south(4), CookingBlocks.MILL);
		var mill = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.south(4)));
		mill.setItem(0, new ItemStack(CookingItems.ROCK_SALT));
		for (int i = 0; i < 100; i++) ProcessStationBlockEntity.serverTick(level, mill.getBlockPos(), mill.getBlockState(), mill);
		helper.assertTrue(mill.getItem(CookingMenu.Kind.MILL.outputSlot).is(CookingItems.SALT)
			&& mill.getItem(CookingMenu.Kind.MILL.outputSlot).getCount() == 2, "The mill grinds rock salt into two salt");
		helper.succeed();
	}

	@GameTest
	public void sugarCaneBecomesBrownSugarAndPanela(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.MILL);
		var mill = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		mill.setItem(0, new ItemStack(Items.SUGAR_CANE, 4));
		mill.setItem(CookingMenu.Kind.MILL.containerSlot, new ItemStack(Items.GLASS_BOTTLE));
		for (int i = 0; i < 100; i++) ProcessStationBlockEntity.serverTick(level, mill.getBlockPos(), mill.getBlockState(), mill);
		helper.assertTrue(mill.getItem(CookingMenu.Kind.MILL.outputSlot).is(CookingItems.CANE_JUICE), "The mill crushes cane into juice");

		helper.setBlock(BASE.east(2), CookingBlocks.STOVE);
		helper.setBlock(BASE.east(2).above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2)));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2).above()));
		pot.setItem(0, new ItemStack(CookingItems.CANE_JUICE, 3));
		stove.setItem(0, new ItemStack(Items.BLAZE_ROD));
		cook(helper, stove, pot, 305);
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.CANE_SUGAR_PASTE)
			&& pot.getItem(CookingPotBlockEntity.BYPRODUCT_SLOT).getCount() == 3, "Cane juice boils down to paste, giving bottles back");

		helper.setBlock(BASE.south(2), CookingBlocks.DRYING_RACK);
		helper.setBlock(BASE.south(4), CookingBlocks.AGEING_SHELF);
		var rack = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.south(2)));
		var shelf = (SpotStationBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.south(4)));
		rack.add(level, new ItemStack(CookingItems.CANE_SUGAR_PASTE));
		shelf.add(level, new ItemStack(CookingItems.CANE_SUGAR_PASTE));
		for (var spots : List.of(rack, shelf)) {
			SpotStationBlockEntity.serverTick(level, spots.getBlockPos(), spots.getBlockState(), spots);
			spots.rewind(2 * CookingStation.DAY);
			SpotStationBlockEntity.serverTick(level, spots.getBlockPos(), spots.getBlockState(), spots);
		}
		helper.assertTrue(rack.getItem(0).is(CookingItems.BROWN_SUGAR) && rack.getItem(0).getCount() == 2, "Paste dries into brown sugar");
		helper.assertTrue(shelf.getItem(0).is(CookingItems.PANELA), "Paste sets into panela");
		helper.succeed();
	}

	@GameTest
	public void sugarMakesSyrupAndCaramel(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		pot.fillWater();
		pot.setItem(0, new ItemStack(CookingItems.BROWN_SUGAR, 2));
		pot.setItem(CookingPotBlockEntity.CONTAINER_SLOT, new ItemStack(Items.GLASS_BOTTLE));
		stove.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
		cook(helper, stove, pot, 105);
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.SIMPLE_SYRUP), "Brown sugar dissolves into syrup");

		helper.setBlock(BASE.east(2), CookingBlocks.STOVE);
		helper.setBlock(BASE.east(2).above(), CookingBlocks.SKILLET);
		var skilletStove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2)));
		var skillet = (SkilletBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.east(2).above()));
		skilletStove.setItem(0, new ItemStack(Items.COAL));
		helper.assertTrue(skillet.add(level, new ItemStack(Items.SUGAR)), "Sugar goes on the skillet");
		for (int i = 0; i < 105; i++) {
			StoveBlockEntity.serverTick(level, skilletStove.getBlockPos(), skilletStove.getBlockState(), skilletStove);
			SkilletBlockEntity.serverTick(level, skillet.getBlockPos(), skillet.getBlockState(), skillet);
		}
		helper.assertTrue(skillet.getItem(0).is(CookingItems.CARAMEL), "Sugar caramelises at medium heat");
		helper.succeed();
	}

	private static void cook(GameTestHelper helper, StoveBlockEntity stove, CookingPotBlockEntity pot, int ticks) {
		for (int i = 0; i < ticks; i++) {
			StoveBlockEntity.serverTick(helper.getLevel(), stove.getBlockPos(), stove.getBlockState(), stove);
			CookingPotBlockEntity.serverTick(helper.getLevel(), pot.getBlockPos(), pot.getBlockState(), pot);
		}
	}
}
