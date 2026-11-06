package dev.portablepocketcabin;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Optional;

public final class CabinCookingStationGameTest {
	private static final BlockPos BASE = new BlockPos(1, 2, 1);

	@GameTest
	public void campfireGivesLowHeat(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, Blocks.CAMPFIRE);
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		BlockPos pot = helper.absolutePos(BASE.above());
		helper.assertTrue(CookingVessel.heatAt(level, pot).equals(Optional.of(CookingHeat.LOW)), "A lit campfire gives low heat");
		helper.setBlock(BASE, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
		helper.assertTrue(CookingVessel.heatAt(level, pot).isEmpty(), "An unlit campfire gives no heat");
		helper.setBlock(BASE, CookingBlocks.STOVE);
		helper.assertTrue(CookingVessel.heatAt(level, pot).isEmpty(), "A cold stove gives no heat");
		helper.succeed();
	}

	@GameTest
	public void stoveLightsOnlyForWorkAboveAndBurnsDown(GameTestHelper helper) {
		var stove = stove(helper, new ItemStack(Items.OAK_PLANKS, 4));
		tickStove(helper, stove, 5);
		helper.assertTrue(!stove.burner().burning() && stove.getItem(0).getCount() == 4, "A stove with nothing above stays cold");
		var pot = pot(helper);
		tickStove(helper, stove, 5);
		helper.assertTrue(!stove.burner().burning(), "A stove under an empty pot stays cold");
		fillSoup(pot);
		tickStove(helper, stove, 1);
		helper.assertTrue(stove.heat().equals(Optional.of(CookingHeat.LOW)) && stove.getItem(0).getCount() == 3,
			"Planks light at low heat when the pot has a recipe");
		helper.assertTrue(stove.getBlockState().getValue(BlockStateProperties.LIT), "A burning stove shows as lit");
		int lit = stove.burner().litTime();
		pot.clearContent();
		tickStove(helper, stove, 10);
		helper.assertTrue(stove.burner().litTime() == lit - 10 && stove.getItem(0).getCount() == 3,
			"Lit fuel burns down with nothing to cook, and no new fuel lights");
		helper.succeed();
	}

	@GameTest
	public void leftoverHighHeatBurnsOutBeforeLowHeatFuelLights(GameTestHelper helper) {
		var stove = stove(helper, new ItemStack(Items.BLAZE_ROD));
		var pot = pot(helper);
		fillSoup(pot);
		tick(helper, stove, pot);
		helper.assertTrue(stove.heat().equals(Optional.of(CookingHeat.HIGH)), "The blaze rod lights at high heat");
		stove.setItem(0, new ItemStack(Items.OAK_PLANKS));
		int ticks = 0;
		while (stove.heat().equals(Optional.of(CookingHeat.HIGH)) && ticks++ < 3000) {
			helper.assertTrue(pot.progress() == 0, "Soup does not cook at the wrong heat");
			tick(helper, stove, pot);
		}
		helper.assertTrue(stove.heat().equals(Optional.of(CookingHeat.LOW)), "The planks light once the blaze rod burns out");
		tick(helper, stove, pot);
		helper.assertTrue(pot.progress() == 2, "Soup cooks once the heat is right");
		helper.succeed();
	}

	@GameTest
	public void shovelPutsOutTheStove(GameTestHelper helper) {
		var stove = stove(helper, new ItemStack(Items.BLAZE_ROD));
		var pot = pot(helper);
		fillSoup(pot);
		tick(helper, stove, pot);
		var shovel = new ItemStack(Items.IRON_SHOVEL);
		use(helper, BASE, shovel);
		helper.assertTrue(!stove.burner().burning() && !stove.getBlockState().getValue(BlockStateProperties.LIT),
			"A shovel puts the stove out");
		helper.assertTrue(shovel.getDamageValue() == 1, "Putting the stove out wears the shovel");
		stove.setItem(0, new ItemStack(Items.OAK_PLANKS));
		tick(helper, stove, pot);
		helper.assertTrue(stove.heat().equals(Optional.of(CookingHeat.LOW)), "The next fuel lights at its own heat");
		helper.succeed();
	}

	@GameTest
	public void potCooksOnlyAtItsHeatAndUsesItsInputs(GameTestHelper helper) {
		helper.setBlock(BASE, Blocks.CAMPFIRE);
		var pot = pot(helper);
		fillSoup(pot);
		pot.getItem(0).grow(1);
		pot.setItem(CookingPotBlockEntity.CONTAINER_SLOT, new ItemStack(Items.BOWL, 4));
		tickPot(helper, pot, 50);
		helper.assertTrue(pot.progress() == 50, "Soup cooks on a campfire");
		helper.setBlock(BASE, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
		tickPot(helper, pot, 20);
		helper.assertTrue(pot.progress() == 50, "Progress holds without heat");
		helper.setBlock(BASE, Blocks.CAMPFIRE);
		tickPot(helper, pot, 250);
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).is(CookingItems.VEGETABLE_SOUP)
			&& pot.getItem(CookingPotBlockEntity.OUTPUT_SLOT).getCount() == 3, "The pot makes three bowls of soup");
		helper.assertTrue(pot.getItem(0).getCount() == 1 && pot.getItem(1).isEmpty() && pot.getItem(2).isEmpty(),
			"The pot uses one of each vegetable and leaves the extra carrot");
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.CONTAINER_SLOT).getCount() == 1, "The pot uses three bowls");
		helper.assertTrue(pot.water() == CookingStation.MAX_WATER - 1, "The pot uses one water");
		helper.assertTrue(pot.progress() == 0, "Progress starts over after a dish");
		helper.succeed();
	}

	@GameTest
	public void potPausesWhenFullAndResetsForANewRecipe(GameTestHelper helper) {
		helper.setBlock(BASE, Blocks.CAMPFIRE);
		var pot = pot(helper);
		fillSoup(pot);
		pot.setItem(CookingPotBlockEntity.OUTPUT_SLOT, new ItemStack(CookingItems.VEGETABLE_SOUP, 16));
		tickPot(helper, pot, 10);
		helper.assertTrue(pot.progress() == 0 && !pot.wantsHeat(helper.getLevel()), "A full output slot pauses the pot");
		pot.setItem(CookingPotBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		tickPot(helper, pot, 10);
		helper.assertTrue(pot.progress() == 10, "The pot cooks once the output is taken");
		pot.setItem(0, new ItemStack(CookingItems.CHOPPED_CARROT, 2));
		tickPot(helper, pot, 1);
		helper.assertTrue(pot.progress() == 11, "More of the same ingredients keep the progress");
		for (int slot = 0; slot < 6; slot++) pot.setItem(slot, new ItemStack(CookingItems.CHOPPED_BEETROOT));
		tickPot(helper, pot, 1);
		helper.assertTrue(pot.progress() == 1, "A different recipe starts over");
		helper.succeed();
	}

	@GameTest
	public void waterBucketFillsThePot(GameTestHelper helper) {
		var pot = pot(helper);
		var player = use(helper, BASE.above(), new ItemStack(Items.WATER_BUCKET));
		helper.assertTrue(pot.water() == CookingStation.MAX_WATER && player.getMainHandItem().is(Items.BUCKET),
			"A water bucket fills the pot and leaves an empty bucket");
		player = use(helper, BASE.above(), new ItemStack(Items.WATER_BUCKET));
		helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "A full pot leaves the water in the bucket");
		helper.succeed();
	}

	@GameTest
	public void skilletCooksEachItemOnItsOwn(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, Blocks.CAMPFIRE);
		helper.setBlock(BASE.above(), CookingBlocks.SKILLET);
		var skillet = (SkilletBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.above()));
		helper.assertTrue(skillet.add(level, new ItemStack(Items.EGG)), "An egg goes on the skillet");
		helper.assertTrue(!skillet.add(level, new ItemStack(Items.STICK)), "A stick does not");
		tickSkillet(helper, skillet, 50);
		var eggs = new ItemStack(Items.EGG, 2);
		helper.assertTrue(skillet.add(level, eggs) && eggs.getCount() == 1, "The skillet takes one item at a time");
		helper.assertTrue(skillet.add(level, new ItemStack(Items.BEEF)), "Beef goes on the skillet");
		tickSkillet(helper, skillet, 50);
		helper.assertTrue(skillet.getItem(0).is(CookingItems.FRIED_EGG), "The first egg is fried");
		helper.assertTrue(skillet.getItem(1).is(Items.EGG) && skillet.progress(1) == 50, "The second egg is half done");
		helper.assertTrue(skillet.getItem(2).is(Items.BEEF) && skillet.progress(2) == 0, "Beef needs high heat");
		helper.assertTrue(skillet.take(level).is(CookingItems.FRIED_EGG), "Taking from the skillet takes finished food first");
		helper.assertTrue(skillet.take(level).is(Items.BEEF), "Otherwise it takes the last item placed");
		helper.assertTrue(skillet.add(level, new ItemStack(Items.EGG)) && skillet.add(level, new ItemStack(Items.EGG))
			&& skillet.add(level, new ItemStack(Items.EGG)) && !skillet.add(level, new ItemStack(Items.EGG)), "The skillet holds four");
		helper.succeed();
	}

	@GameTest
	public void stationsKeepTheirStateWhenSaved(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, Blocks.CAMPFIRE);
		var pot = pot(helper);
		fillSoup(pot);
		tickPot(helper, pot, 40);
		var copy = reload(helper, pot, new CookingPotBlockEntity(pot.getBlockPos(), pot.getBlockState()));
		helper.assertTrue(copy.water() == CookingStation.MAX_WATER && copy.progress() == 40
			&& copy.getItem(CookingPotBlockEntity.CONTAINER_SLOT).getCount() == 3, "The pot keeps its water, progress, and items");
		CookingPotBlockEntity.serverTick(level, copy.getBlockPos(), copy.getBlockState(), copy);
		helper.assertTrue(copy.progress() == 41, "Saved progress survives the first recipe lookup");

		helper.setBlock(BASE.offset(2, 0, 0), CookingBlocks.STOVE);
		helper.setBlock(BASE.offset(2, 1, 0), CookingBlocks.SKILLET);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 0, 0)));
		var skillet = (SkilletBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 1, 0)));
		stove.setItem(0, new ItemStack(Items.OAK_PLANKS));
		skillet.add(level, new ItemStack(Items.EGG));
		for (int i = 0; i < 30; i++) {
			StoveBlockEntity.serverTick(level, stove.getBlockPos(), stove.getBlockState(), stove);
			SkilletBlockEntity.serverTick(level, skillet.getBlockPos(), skillet.getBlockState(), skillet);
		}
		var stoveCopy = reload(helper, stove, new StoveBlockEntity(stove.getBlockPos(), stove.getBlockState()));
		helper.assertTrue(stoveCopy.burner().litTime() == stove.burner().litTime()
			&& stoveCopy.burner().litDuration() == stove.burner().litDuration() && stoveCopy.heat().equals(stove.heat()),
			"The stove keeps its fire");
		var skilletCopy = reload(helper, skillet, new SkilletBlockEntity(skillet.getBlockPos(), skillet.getBlockState()));
		helper.assertTrue(skilletCopy.getItem(0).is(Items.EGG) && skilletCopy.progress(0) == 30, "The skillet keeps its food and progress");
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void eggFriesOnAStoveInTheWorld(GameTestHelper helper) {
		var stove = stove(helper, new ItemStack(Items.OAK_PLANKS));
		helper.setBlock(BASE.above(), CookingBlocks.SKILLET);
		var skillet = (SkilletBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BASE.above()));
		skillet.add(helper.getLevel(), new ItemStack(Items.EGG));
		helper.succeedWhen(() -> {
			helper.assertTrue(stove.getBlockState().getValue(BlockStateProperties.LIT), "The stove is lit");
			helper.assertTrue(skillet.getItem(0).is(CookingItems.FRIED_EGG), "The egg is fried");
		});
	}

	@GameTest
	public void ovenBakesCakeInSixSlots(GameTestHelper helper) {
		var oven = oven(helper, new ItemStack(Items.OAK_PLANKS, 2));
		fillCake(oven);
		tickOven(helper, oven, 1);
		helper.assertTrue(oven.heat().equals(Optional.of(CookingHeat.LOW)) && oven.getBlockState().getValue(BlockStateProperties.LIT),
			"The oven lights its own fuel for the cake");
		tickOven(helper, oven, 199);
		helper.assertTrue(oven.getItem(OvenBlockEntity.OUTPUT_SLOT).is(Items.CAKE), "The oven bakes a cake");
		helper.assertTrue(oven.getItem(OvenBlockEntity.BYPRODUCT_SLOT).is(Items.BUCKET)
			&& oven.getItem(OvenBlockEntity.BYPRODUCT_SLOT).getCount() == 3, "The cake returns three empty buckets");
		helper.assertTrue(oven.getItem(0).is(Items.WHEAT) && oven.getItem(0).getCount() == 2, "Extra wheat stays in its slot");
		for (int slot = 1; slot < 6; slot++) helper.assertTrue(oven.getItem(slot).isEmpty(), "The other ingredients are used up");
		helper.succeed();
	}

	@GameTest
	public void ovenBakesOnlyAtTheRecipeHeat(GameTestHelper helper) {
		var oven = oven(helper, new ItemStack(Items.OAK_PLANKS));
		oven.setItem(0, new ItemStack(Items.WHEAT, 3));
		tickOven(helper, oven, 20);
		helper.assertTrue(oven.heat().equals(Optional.of(CookingHeat.LOW)) && oven.progress() == 0, "Bread does not bake at low heat");
		var shovel = new ItemStack(Items.IRON_SHOVEL);
		use(helper, BASE, shovel);
		helper.assertTrue(!oven.burner().burning() && !oven.getBlockState().getValue(BlockStateProperties.LIT), "A shovel puts the oven out");
		oven.setItem(OvenBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
		tickOven(helper, oven, 200);
		helper.assertTrue(oven.getItem(OvenBlockEntity.OUTPUT_SLOT).is(Items.BREAD), "Bread bakes at medium heat");
		helper.succeed();
	}

	@GameTest
	public void ovenStaysColdWithNothingToBake(GameTestHelper helper) {
		var oven = oven(helper, new ItemStack(Items.OAK_PLANKS, 4));
		oven.setItem(0, new ItemStack(Items.STICK));
		tickOven(helper, oven, 5);
		helper.assertTrue(!oven.burner().burning() && oven.getItem(OvenBlockEntity.FUEL_SLOT).getCount() == 4,
			"An oven without a recipe does not light");
		oven.clearContent();
		oven.setItem(OvenBlockEntity.FUEL_SLOT, new ItemStack(Items.OAK_PLANKS, 4));
		fillCake(oven);
		oven.setItem(OvenBlockEntity.BYPRODUCT_SLOT, new ItemStack(Items.STONE));
		tickOven(helper, oven, 5);
		helper.assertTrue(!oven.burner().burning() && oven.progress() == 0, "A blocked byproduct slot keeps the oven cold");
		helper.succeed();
	}

	@GameTest
	public void hoppersUseStationsLikeAFurnace(GameTestHelper helper) {
		var oven = oven(helper, ItemStack.EMPTY);
		helper.assertTrue(Arrays.equals(oven.getSlotsForFace(Direction.UP), new int[] {0, 1, 2, 3, 4, 5}),
			"Ingredients go into the top of the oven");
		helper.assertTrue(oven.canPlaceItemThroughFace(OvenBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL), Direction.NORTH)
			&& !oven.canPlaceItemThroughFace(OvenBlockEntity.FUEL_SLOT, new ItemStack(Items.WHEAT), Direction.NORTH),
			"Only fuel goes into the side of the oven");
		helper.assertTrue(oven.canTakeItemThroughFace(OvenBlockEntity.FUEL_SLOT, new ItemStack(Items.BUCKET), Direction.DOWN)
			&& !oven.canTakeItemThroughFace(OvenBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL), Direction.DOWN)
			&& !oven.canTakeItemThroughFace(0, new ItemStack(Items.WHEAT), Direction.DOWN),
			"Only spent fuel, output, and byproducts come out of the oven");
		var pot = pot(helper);
		helper.assertTrue(Arrays.equals(pot.getSlotsForFace(Direction.EAST), new int[] {CookingPotBlockEntity.CONTAINER_SLOT})
			&& !pot.canTakeItemThroughFace(0, new ItemStack(CookingItems.CHOPPED_CARROT), Direction.DOWN)
			&& pot.canTakeItemThroughFace(CookingPotBlockEntity.OUTPUT_SLOT, new ItemStack(CookingItems.VEGETABLE_SOUP), Direction.DOWN),
			"Containers go into the side of the pot, and only food comes out");
		helper.succeed();
	}

	@GameTest
	public void ovenKeepsItsStateWhenSaved(GameTestHelper helper) {
		var oven = oven(helper, new ItemStack(Items.OAK_PLANKS));
		fillCake(oven);
		tickOven(helper, oven, 60);
		var copy = reload(helper, oven, new OvenBlockEntity(oven.getBlockPos(), oven.getBlockState()));
		helper.assertTrue(copy.progress() == 60 && copy.burner().litTime() == oven.burner().litTime()
			&& copy.heat().equals(Optional.of(CookingHeat.LOW)) && copy.getItem(3).is(Items.MILK_BUCKET),
			"The oven keeps its progress, fire, and ingredients");
		helper.succeed();
	}

	private static OvenBlockEntity oven(GameTestHelper helper, ItemStack fuel) {
		helper.setBlock(BASE, CookingBlocks.OVEN);
		var oven = (OvenBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BASE));
		oven.setItem(OvenBlockEntity.FUEL_SLOT, fuel);
		return oven;
	}

	private static void fillCake(OvenBlockEntity oven) {
		oven.setItem(0, new ItemStack(Items.WHEAT, 5));
		oven.setItem(1, new ItemStack(Items.SUGAR, 2));
		oven.setItem(2, new ItemStack(Items.EGG));
		for (int slot = 3; slot < 6; slot++) oven.setItem(slot, new ItemStack(Items.MILK_BUCKET));
	}

	private static void tickOven(GameTestHelper helper, OvenBlockEntity oven, int ticks) {
		for (int i = 0; i < ticks; i++) OvenBlockEntity.serverTick(helper.getLevel(), oven.getBlockPos(), oven.getBlockState(), oven);
	}

	private static StoveBlockEntity stove(GameTestHelper helper, ItemStack fuel) {
		helper.setBlock(BASE, CookingBlocks.STOVE);
		var stove = (StoveBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BASE));
		stove.setItem(0, fuel);
		return stove;
	}

	private static CookingPotBlockEntity pot(GameTestHelper helper) {
		helper.setBlock(BASE.above(), CookingBlocks.COOKING_POT);
		return (CookingPotBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BASE.above()));
	}

	private static void fillSoup(CookingPotBlockEntity pot) {
		pot.setItem(0, new ItemStack(CookingItems.CHOPPED_CARROT));
		pot.setItem(1, new ItemStack(CookingItems.DICED_POTATO));
		pot.setItem(2, new ItemStack(CookingItems.CHOPPED_BEETROOT));
		pot.setItem(CookingPotBlockEntity.CONTAINER_SLOT, new ItemStack(Items.BOWL, 3));
		pot.fillWater();
	}

	private static void tickStove(GameTestHelper helper, StoveBlockEntity stove, int ticks) {
		for (int i = 0; i < ticks; i++) StoveBlockEntity.serverTick(helper.getLevel(), stove.getBlockPos(), stove.getBlockState(), stove);
	}

	private static void tickPot(GameTestHelper helper, CookingPotBlockEntity pot, int ticks) {
		for (int i = 0; i < ticks; i++) CookingPotBlockEntity.serverTick(helper.getLevel(), pot.getBlockPos(), pot.getBlockState(), pot);
	}

	private static void tickSkillet(GameTestHelper helper, SkilletBlockEntity skillet, int ticks) {
		for (int i = 0; i < ticks; i++) SkilletBlockEntity.serverTick(helper.getLevel(), skillet.getBlockPos(), skillet.getBlockState(), skillet);
	}

	private static void tick(GameTestHelper helper, StoveBlockEntity stove, CookingPotBlockEntity pot) {
		tickStove(helper, stove, 1);
		tickPot(helper, pot, 1);
	}

	private static FakePlayer use(GameTestHelper helper, BlockPos relative, ItemStack held) {
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(relative);
		var player = FakePlayer.get(level);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		level.getBlockState(pos).useItemOn(held, level, player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		return player;
	}

	private static <T extends BlockEntity> T reload(GameTestHelper helper, BlockEntity saved, T fresh) {
		var registries = helper.getLevel().registryAccess();
		fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved.saveWithFullMetadata(registries)));
		return fresh;
	}
}
