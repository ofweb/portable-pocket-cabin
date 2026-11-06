package dev.portablepocketcabin;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
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
		var replacementSoup = new net.minecraft.world.item.crafting.RecipeHolder<>(
			net.minecraft.resources.ResourceKey.create(Registries.RECIPE, PortablePocketCabin.id("test/replacement_soup")),
			recipe(helper, "cooking/pot/vegetable_soup"));
		var changedPot = reload(helper, pot, new CookingPotBlockEntity(pot.getBlockPos(), pot.getBlockState()));
		changedPot.process().lookup((world, input) -> Optional.of(replacementSoup));
		CookingPotBlockEntity.serverTick(level, changedPot.getBlockPos(), changedPot.getBlockState(), changedPot);
		helper.assertTrue(changedPot.progress() == 1, "Saved pot progress resets when a different recipe matches");

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
		var replacementEgg = new net.minecraft.world.item.crafting.RecipeHolder<>(
			net.minecraft.resources.ResourceKey.create(Registries.RECIPE, PortablePocketCabin.id("test/replacement_egg")),
			recipe(helper, "cooking/skillet/fried_egg"));
		var changedSkillet = reload(helper, skillet, new SkilletBlockEntity(skillet.getBlockPos(), skillet.getBlockState()));
		changedSkillet.lookup((world, input) -> Optional.of(replacementEgg));
		SkilletBlockEntity.serverTick(level, changedSkillet.getBlockPos(), changedSkillet.getBlockState(), changedSkillet);
		helper.assertTrue(changedSkillet.progress(0) == 1, "Saved skillet progress resets when a different recipe matches");
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

	@GameTest
	public void knifeCutsOneOrTheWholeStack(GameTestHelper helper) {
		var level = helper.getLevel();
		var board = board(helper);
		helper.assertTrue(!board.place(level, new ItemStack(Items.COBBLESTONE)), "Cobblestone does not go on the board");
		var carrots = new ItemStack(Items.CARROT, 5);
		helper.assertTrue(board.place(level, carrots) && carrots.isEmpty() && board.getItem(0).getCount() == 5,
			"The whole carrot stack goes on the board");
		var player = FakePlayer.get(level);
		player.getInventory().clearContent();
		use(helper, BASE, new ItemStack(CookingItems.KITCHEN_KNIFE));
		helper.assertTrue(board.getItem(0).getCount() == 4 && player.getInventory().countItem(CookingItems.CHOPPED_CARROT) == 1,
			"The knife cuts one carrot into the player's inventory");
		player.setShiftKeyDown(true);
		use(helper, BASE, new ItemStack(CookingItems.KITCHEN_KNIFE));
		player.setShiftKeyDown(false);
		helper.assertTrue(board.isEmpty() && player.getInventory().countItem(CookingItems.CHOPPED_CARROT) == 5,
			"Sneaking with the knife cuts the whole stack");
		board.place(level, new ItemStack(Items.POTATO, 2));
		helper.assertTrue(board.take().getCount() == 2 && board.isEmpty(), "The stack can be taken back");
		player.getInventory().clearContent();
		helper.succeed();
	}

	@GameTest
	public void cutFoodThatDoesNotFitDropsBesideTheBoard(GameTestHelper helper) {
		var level = helper.getLevel();
		var board = board(helper);
		board.place(level, new ItemStack(Items.BEETROOT, 2));
		var player = FakePlayer.get(level);
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
		}
		helper.assertTrue(board.cut(level, player, true), "The knife cuts with a full inventory");
		var dropped = level.getEntitiesOfClass(ItemEntity.class,
			new AABB(board.getBlockPos()).inflate(1));
		helper.assertTrue(dropped.stream().mapToInt(item -> item.getItem().is(CookingItems.CHOPPED_BEETROOT) ? item.getItem().getCount() : 0).sum() == 2,
			"Cut food that does not fit drops beside the board");
		player.getInventory().clearContent();
		helper.succeed();
	}

	@GameTest
	public void hoppersCannotUseTheBoardOrSkillet(GameTestHelper helper) {
		var board = board(helper);
		board.place(helper.getLevel(), new ItemStack(Items.CARROT));
		helper.setBlock(BASE.offset(2, 0, 0), CookingBlocks.SKILLET);
		var skillet = (SkilletBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BASE.offset(2, 0, 0)));
		skillet.add(helper.getLevel(), new ItemStack(Items.EGG));
		var hopper = new SimpleContainer(5);
		helper.assertTrue(!board.canTakeItem(hopper, 0, board.getItem(0)) && !board.canPlaceItem(0, new ItemStack(Items.CARROT)),
			"Hoppers cannot move items on the cutting board");
		helper.assertTrue(!skillet.canTakeItem(hopper, 0, skillet.getItem(0)) && !skillet.canPlaceItem(1, new ItemStack(Items.EGG)),
			"Hoppers cannot move items on the skillet");
		helper.succeed();
	}

	@GameTest
	public void recipePanelFillsThePotAndReturnsWhatWasThere(GameTestHelper helper) {
		var pot = pot(helper);
		pot.setItem(0, new ItemStack(Items.STICK));
		pot.setItem(CookingPotBlockEntity.CONTAINER_SLOT, new ItemStack(Items.BOWL));
		var player = FakePlayer.get(helper.getLevel());
		var inventory = player.getInventory();
		inventory.clearContent();
		var named = new ItemStack(CookingItems.CHOPPED_CARROT);
		named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Prize carrot"));
		inventory.setItem(0, named);
		inventory.setItem(1, new ItemStack(CookingItems.CHOPPED_CARROT, 4));
		inventory.setItem(2, new ItemStack(CookingItems.DICED_POTATO));
		inventory.setItem(3, new ItemStack(CookingItems.CHOPPED_BEETROOT));
		inventory.setItem(4, new ItemStack(Items.BOWL, 5));
		helper.assertTrue(CookingFill.fill(pot, CookingStation.POT, recipe(helper, "cooking/pot/vegetable_soup"), inventory), "The soup fills");
		helper.assertTrue(pot.getItem(0).is(CookingItems.CHOPPED_CARROT) && pot.getItem(0).getCount() == 1
			&& !pot.getItem(0).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME), "An ordinary carrot goes in, not the named one");
		helper.assertTrue(pot.getItem(1).is(CookingItems.DICED_POTATO) && pot.getItem(2).is(CookingItems.CHOPPED_BEETROOT),
			"The other vegetables go in");
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.CONTAINER_SLOT).getCount() == 3, "Three bowls go in");
		helper.assertTrue(inventory.countItem(Items.STICK) == 1 && inventory.countItem(Items.BOWL) == 3,
			"The stick comes back, and the pot uses its own bowl first");
		helper.assertTrue(inventory.getItem(0).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
			&& inventory.countItem(CookingItems.CHOPPED_CARROT) == 4, "The named carrot stays with the player");

		inventory.clearContent();
		inventory.setItem(0, new ItemStack(Items.WHEAT, 2));
		pot.clearContent();
		pot.setItem(0, new ItemStack(Items.STICK));
		var oven = oven(helper, ItemStack.EMPTY);
		oven.setItem(0, new ItemStack(Items.STICK));
		helper.assertTrue(!CookingFill.fill(oven, CookingStation.OVEN, recipe(helper, "cooking/oven/bread"), inventory) && oven.getItem(0).is(Items.STICK) && inventory.countItem(Items.WHEAT) == 2,
			"Nothing moves when an ingredient is missing");
		inventory.clearContent();
		helper.succeed();
	}

	@GameTest
	public void recipePanelFillsCakeIntoSixSlots(GameTestHelper helper) {
		var oven = oven(helper, new ItemStack(Items.OAK_PLANKS));
		var player = FakePlayer.get(helper.getLevel());
		var inventory = player.getInventory();
		inventory.clearContent();
		inventory.setItem(0, new ItemStack(Items.WHEAT, 10));
		inventory.setItem(1, new ItemStack(Items.SUGAR, 5));
		inventory.setItem(2, new ItemStack(Items.EGG, 4));
		for (int slot = 3; slot < 7; slot++) inventory.setItem(slot, new ItemStack(Items.MILK_BUCKET));
		helper.assertTrue(CookingFill.fill(oven, CookingStation.OVEN, recipe(helper, "cooking/oven/cake"), inventory), "The cake fills");
		helper.assertTrue(oven.getItem(0).getCount() == 3 && oven.getItem(1).getCount() == 2 && oven.getItem(2).getCount() == 1,
			"Wheat, sugar, and egg go in by their counts");
		for (int slot = 3; slot < 6; slot++) helper.assertTrue(oven.getItem(slot).is(Items.MILK_BUCKET), "Each milk bucket takes a slot");
		helper.assertTrue(oven.getItem(OvenBlockEntity.FUEL_SLOT).is(Items.OAK_PLANKS) && inventory.countItem(Items.MILK_BUCKET) == 1,
			"The fuel stays and one milk bucket is left over");
		inventory.clearContent();
		helper.succeed();
	}

	@GameTest
	public void recipePanelKeepsBowlsAlreadyInThePot(GameTestHelper helper) {
		var pot = pot(helper);
		pot.setItem(CookingPotBlockEntity.CONTAINER_SLOT, new ItemStack(Items.BOWL, 8));
		var inventory = FakePlayer.get(helper.getLevel()).getInventory();
		inventory.clearContent();
		inventory.setItem(0, new ItemStack(CookingItems.CHOPPED_BEETROOT, 6));
		helper.assertTrue(CookingFill.fill(pot, CookingStation.POT, recipe(helper, "cooking/pot/beetroot_soup"), inventory), "Beetroot soup fills from the pot's own bowls");
		helper.assertTrue(pot.getItem(CookingPotBlockEntity.CONTAINER_SLOT).getCount() == 8 && inventory.countItem(Items.BOWL) == 0,
			"Bowls that already suffice stay in the pot");
		inventory.clearContent();
		helper.succeed();
	}

	@GameTest
	public void overlappingIngredientsFindAWorkingAssignment(GameTestHelper helper) {
		var planks = helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.PLANKS);
		var recipe = new CookingRecipe(CookingStation.OVEN, "baking",
			List.of(new CookingRecipe.Counted(Ingredient.of(planks), 1), new CookingRecipe.Counted(Ingredient.of(Items.OAK_PLANKS), 1)),
			Optional.empty(), 0, List.of(), Optional.of(CookingHeat.LOW), 100, new ItemStackTemplate(Items.BREAD, 1));
		helper.assertTrue(CookbookAvailability.canMake(recipe, List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS))),
			"The tag takes birch so the exact ingredient can take oak");
		helper.assertTrue(!CookbookAvailability.canMake(recipe, List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.STICK))),
			"One oak plank cannot fill both ingredients");
		var oven = oven(helper, ItemStack.EMPTY);
		var inventory = FakePlayer.get(helper.getLevel()).getInventory();
		inventory.clearContent();
		inventory.setItem(0, new ItemStack(Items.OAK_PLANKS));
		inventory.setItem(1, new ItemStack(Items.BIRCH_PLANKS));
		helper.assertTrue(CookingFill.fill(oven, CookingStation.OVEN, recipe, inventory),
			"Filling uses the same assignment");
		helper.assertTrue(oven.getItem(0).is(Items.BIRCH_PLANKS) && oven.getItem(1).is(Items.OAK_PLANKS) && inventory.isEmpty(),
			"Birch goes in for the tag and oak for the exact ingredient");
		helper.assertTrue(recipe.assign(CookingInput.of(oven.getItems(6))).isPresent(), "The filled oven matches the recipe");

		oven.clearContent();
		inventory.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
		helper.assertTrue(CookingFill.fill(oven, CookingStation.OVEN, recipe, inventory),
			"Two oak planks fill both ingredients");
		helper.assertTrue(oven.getItem(0).getCount() == 1 && oven.getItem(1).getCount() == 1
			&& recipe.assign(CookingInput.of(oven.getItems(6))).isPresent(), "Each ingredient keeps its own slot, so the oven matches");
		helper.assertTrue(CookingFill.fill(oven, CookingStation.OVEN, recipe, inventory)
			&& oven.getItem(0).getCount() == 1 && oven.getItem(1).getCount() == 1, "Filling again keeps a valid layout");
		inventory.clearContent();
		helper.succeed();
	}

	@GameTest
	public void fillingWithAFullInventoryLosesNothing(GameTestHelper helper) {
		var oven = oven(helper, ItemStack.EMPTY);
		oven.setItem(0, new ItemStack(Items.WHEAT, 3));
		var inventory = FakePlayer.get(helper.getLevel()).getInventory();
		inventory.clearContent();
		for (int slot = 0; slot < 36; slot++) inventory.setItem(slot, new ItemStack(Items.STONE, 64));
		var bread = recipe(helper, "cooking/oven/bread");
		helper.assertTrue(CookingFill.fill(oven, CookingStation.OVEN, bread, inventory)
			&& oven.getItem(0).is(Items.WHEAT) && oven.getItem(0).getCount() == 3, "Wheat already in the oven stays there");
		oven.setItem(1, new ItemStack(Items.STICK));
		helper.assertTrue(!CookingFill.fill(oven, CookingStation.OVEN, bread, inventory)
			&& oven.getItem(0).getCount() == 3 && oven.getItem(1).is(Items.STICK), "Nothing moves when a displaced stick has no room");
		var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(oven.getBlockPos()).inflate(3));
		helper.assertTrue(dropped.isEmpty(), "Nothing is dropped");
		inventory.clearContent();
		helper.succeed();
	}

	@GameTest
	public void recipeReloadsRefreshCachedLookups(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.setBlock(BASE, Blocks.CAMPFIRE);
		var pot = pot(helper);
		fillSoup(pot);
		@SuppressWarnings("unchecked")
		var soup = (net.minecraft.world.item.crafting.RecipeHolder<CookingRecipe>) level.getServer().getRecipeManager()
			.byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, PortablePocketCabin.id("cooking/pot/vegetable_soup"))).orElseThrow();
		var current = new Object[] {null};
		pot.process().lookup((world, input) -> Optional.ofNullable((net.minecraft.world.item.crafting.RecipeHolder<CookingRecipe>) current[0]));
		tickPot(helper, pot, 1);
		helper.assertTrue(pot.process().time() == 0, "No recipe matches before it exists");
		current[0] = soup;
		tickPot(helper, pot, 1);
		helper.assertTrue(pot.process().time() == 0, "Unchanged contents keep the cached lookup");
		CookingStation.recipesReloaded();
		tickPot(helper, pot, 1);
		helper.assertTrue(pot.process().time() == soup.value().time() && pot.progress() == 1, "An added recipe is found after a reload");
		var quick = soup.value();
		current[0] = new net.minecraft.world.item.crafting.RecipeHolder<>(soup.id(), new CookingRecipe(quick.station(), quick.technique(),
			quick.ingredients(), quick.container(), quick.water(), quick.remainders(), quick.heat(), 50, quick.result()));
		CookingStation.recipesReloaded();
		tickPot(helper, pot, 1);
		helper.assertTrue(pot.process().time() == 50 && pot.progress() == 2, "A replaced recipe takes effect and keeps its progress");
		current[0] = new net.minecraft.world.item.crafting.RecipeHolder<>(
			net.minecraft.resources.ResourceKey.create(Registries.RECIPE, PortablePocketCabin.id("test/other_soup")), quick);
		CookingStation.recipesReloaded();
		tickPot(helper, pot, 1);
		helper.assertTrue(pot.progress() == 1, "A different matching recipe starts the pot over after reload");
		current[0] = null;
		CookingStation.recipesReloaded();
		tickPot(helper, pot, 1);
		helper.assertTrue(pot.process().time() == 0 && pot.progress() == 0, "A removed recipe stops cooking");

		helper.setBlock(BASE.offset(2, 0, 0), Blocks.CAMPFIRE);
		helper.setBlock(BASE.offset(2, 1, 0), CookingBlocks.SKILLET);
		var skillet = (SkilletBlockEntity) level.getBlockEntity(helper.absolutePos(BASE.offset(2, 1, 0)));
		skillet.add(level, new ItemStack(Items.EGG));
		var egg = new Object[] {null};
		skillet.lookup((world, input) -> Optional.ofNullable((net.minecraft.world.item.crafting.RecipeHolder<CookingRecipe>) egg[0]));
		helper.assertTrue(!skillet.wantsHeat(level), "The skillet has no recipe for the egg");
		egg[0] = level.getServer().getRecipeManager()
			.byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, PortablePocketCabin.id("cooking/skillet/fried_egg"))).orElseThrow();
		helper.assertTrue(!skillet.wantsHeat(level), "The skillet keeps its cached lookup");
		CookingStation.recipesReloaded();
		helper.assertTrue(skillet.wantsHeat(level), "The skillet finds the recipe after a reload");
		tickSkillet(helper, skillet, 10);
		egg[0] = new net.minecraft.world.item.crafting.RecipeHolder<>(
			net.minecraft.resources.ResourceKey.create(Registries.RECIPE, PortablePocketCabin.id("test/other_egg")),
			recipe(helper, "cooking/skillet/fried_egg"));
		CookingStation.recipesReloaded();
		tickSkillet(helper, skillet, 1);
		helper.assertTrue(skillet.progress(0) == 1, "A different matching recipe starts the skillet over after reload");
		helper.succeed();
	}

	private static CookingRecipe recipe(GameTestHelper helper, String path) {
		return (CookingRecipe) helper.getLevel().getServer().getRecipeManager()
			.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, PortablePocketCabin.id(path)))
			.orElseThrow().value();
	}

	private static CuttingBoardBlockEntity board(GameTestHelper helper) {
		helper.setBlock(BASE, CookingBlocks.CUTTING_BOARD);
		return (CuttingBoardBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BASE));
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
