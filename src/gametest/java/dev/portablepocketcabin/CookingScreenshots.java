package dev.portablepocketcabin;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.core.Direction;

import java.util.Set;

/** Cooking station references for screenshots/cooking. Run with PPC_CAPTURE_COOKING=1 ./gradlew runClientGameTest. */
final class CookingScreenshots {
	private CookingScreenshots() {
	}

	static void capture(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			BlockPos[] origin = new BlockPos[1];
			world.getServer().runOnServer(server -> {
				var player = world.getConnection().getServerPlayer();
				var level = player.level();
				player.setGameMode(GameType.SURVIVAL);
				player.getInventory().clearContent();
				player.getInventory().setItem(0, new ItemStack(CookingItems.KITCHEN_KNIFE));
				player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 16));
				player.getInventory().setItem(2, new ItemStack(Items.BOWL, 6));
				player.getInventory().setItem(3, new ItemStack(CookingItems.CHOPPED_CARROT, 2));
				player.getInventory().setItem(4, new ItemStack(CookingItems.DICED_POTATO, 2));
				player.getInventory().setItem(5, new ItemStack(CookingItems.CHOPPED_BEETROOT, 2));
				player.getInventory().setItem(6, new ItemStack(CookingItems.COOKBOOK));
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
				BlockPos base = player.blockPosition().offset(0, 40, 0);
				origin[0] = base;
				for (int x = -5; x <= 5; x++) for (int z = -5; z <= 2; z++) {
					level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
					for (int y = 0; y < 4; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
				}
				var facing = CookingBlocks.STOVE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
				level.setBlockAndUpdate(base.offset(-4, 0, -3), facing);
				level.setBlockAndUpdate(base.offset(-4, 1, -3), CookingBlocks.COOKING_POT.defaultBlockState());
				level.setBlockAndUpdate(base.offset(-2, 0, -3), facing);
				level.setBlockAndUpdate(base.offset(-2, 1, -3), CookingBlocks.SKILLET.defaultBlockState());
				level.setBlockAndUpdate(base.offset(2, 0, -3), CookingBlocks.OVEN.defaultBlockState()
					.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
				level.setBlockAndUpdate(base.offset(4, 0, -3), Blocks.OAK_PLANKS.defaultBlockState());
				level.setBlockAndUpdate(base.offset(4, 1, -3), CookingBlocks.CUTTING_BOARD.defaultBlockState());

				level.setBlockAndUpdate(base.offset(0, 0, -3), Blocks.CAMPFIRE.defaultBlockState());
				level.setBlockAndUpdate(base.offset(0, 1, -3), CookingBlocks.COOKING_POT.defaultBlockState());
				var potStove = (StoveBlockEntity) level.getBlockEntity(base.offset(-4, 0, -3));
				potStove.setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
				var pot = (CookingPotBlockEntity) level.getBlockEntity(base.offset(-4, 1, -3));
				pot.setItem(0, new ItemStack(CookingItems.CHOPPED_CARROT, 2));
				pot.setItem(1, new ItemStack(CookingItems.DICED_POTATO, 2));
				pot.setItem(2, new ItemStack(CookingItems.CHOPPED_BEETROOT, 2));
				pot.setItem(CookingPotBlockEntity.CONTAINER_SLOT, new ItemStack(Items.BOWL, 6));
				pot.fillWater();
				var skilletStove = (StoveBlockEntity) level.getBlockEntity(base.offset(-2, 0, -3));
				skilletStove.setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
				var skillet = (SkilletBlockEntity) level.getBlockEntity(base.offset(-2, 1, -3));
				skillet.add(level, new ItemStack(Items.EGG));
				skillet.add(level, new ItemStack(Items.EGG));
				skillet.add(level, new ItemStack(Items.BROWN_MUSHROOM));
				var oven = (OvenBlockEntity) level.getBlockEntity(base.offset(2, 0, -3));
				oven.setItem(0, new ItemStack(CookingItems.CAKE_BATTER));
				oven.setItem(CookingMenu.Kind.OVEN.seasoningSlots[0], new ItemStack(Items.SUGAR));
				oven.setItem(OvenBlockEntity.FUEL_SLOT, new ItemStack(Items.OAK_PLANKS, 8));
				var board = (CuttingBoardBlockEntity) level.getBlockEntity(base.offset(4, 1, -3));
				board.place(level, new ItemStack(Items.CARROT, 6));
				player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() + 2.5, Set.of(), 180, 30, false);
			});
			world.getConnection().waitForClientboundPackets();
			world.getConnection().waitForChunksRender();
			context.waitTicks(60);
			shot(context, "stations");
			open(context, world, origin[0].offset(-4, 1, -3), CookingMenu.Kind.POT);
			shot(context, "pot-screen");
			context.runOnClient(client -> ((CookingScreen) client.gui.screen()).togglePanel());
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			shot(context, "pot-recipe-panel");
			open(context, world, origin[0].offset(-4, 0, -3), CookingMenu.Kind.STOVE);
			shot(context, "stove-screen");
			open(context, world, origin[0].offset(2, 0, -3), CookingMenu.Kind.OVEN);
			shot(context, "oven-screen");
			world.getServer().runOnServer(server -> world.getConnection().getServerPlayer().closeContainer());
			world.getConnection().waitForClientboundPackets();
			context.runOnClient(client -> CookbookScreen.open());
			context.waitForScreen(CookbookScreen.class);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			shot(context, "cookbook-list");
			context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).select(PortablePocketCabin.id("cooking/pot/vegetable_soup")));
			shot(context, "cookbook-detail");
			context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).select(PortablePocketCabin.id("cooking/oven/cake")));
			shot(context, "cookbook-cake");
			context.runOnClient(client -> client.gui.setScreen(null));
			captureCounterStations(context, world, origin[0]);
			capturePress(context, world, origin[0]);
			capturePhase3(context, world, origin[0]);
			captureGarden(context, world, origin[0]);
			captureButchery(context, world, origin[0]);
		}
	}

	/** B-0050 delivery 1: a plot of crops, vines on trellises, bushes, and a fruiting plum tree, and two Garden pages. */
	private static void captureGarden(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base) {
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			var level = player.level();
			player.closeContainer();
			for (int x = -6; x <= 6; x++) for (int z = 3; z <= 12; z++) {
				level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
				for (int y = 0; y < 8; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
			}
			String[] crops = {"barley", "corn", "cabbage", "onion", "lettuce", "radish", "cilantro", "garlic"};
			for (int i = 0; i < crops.length; i++) {
				var pos = base.offset(-4 + i + (i >= 4 ? 1 : 0), 0, 4);
				// B-0054: the first four crops grow on rich farmland made with compost.
				level.setBlockAndUpdate(pos.below(), i < 4 ? RichSoil.RICH_FARMLAND.defaultBlockState()
					.setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE, 7) : Blocks.FARMLAND.defaultBlockState());
				var crop = (net.minecraft.world.level.block.CropBlock) CookingPlants.PLANTS.get(crops[i]);
				level.setBlockAndUpdate(pos, crop.getStateForAge(crops[i].equals("cilantro") ? 6 : crops[i].equals("onion") ? 5 : 7));
			}
			level.setBlockAndUpdate(base.offset(0, -1, 4), Blocks.WATER.defaultBlockState());
			String[] vines = {"cucumber_vine", "white_grape_vine", "hop_vine"};
			for (int i = 0; i < vines.length; i++) {
				var pos = base.offset(-4 + i, 0, 3);
				level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
				level.setBlockAndUpdate(pos, ((net.minecraft.world.level.block.CropBlock) CookingPlants.PLANTS.get(vines[i])).getStateForAge(7));
			}
			String[] bushes = {"thyme_bush", "strawberry_bush", "raspberry_bush", "elder_bush"};
			for (int i = 0; i < bushes.length; i++) {
				level.setBlockAndUpdate(base.offset(i, 0, 3), CookingPlants.PLANTS.get(bushes[i]).defaultBlockState()
					.setValue(HerbBushBlock.AGE, bushes[i].equals("elder_bush") ? 2 : 3));
			}
			// Delivery 2: taiga crops and currants, and wasabi in a gravel bed beside flowing water.
			level.setBlockAndUpdate(base.offset(-4, -1, 5), Blocks.FARMLAND.defaultBlockState());
			level.setBlockAndUpdate(base.offset(-4, 0, 5), ((net.minecraft.world.level.block.CropBlock) CookingPlants.RED_CABBAGE_CROP).getStateForAge(7));
			level.setBlockAndUpdate(base.offset(-3, -1, 5), Blocks.FARMLAND.defaultBlockState());
			level.setBlockAndUpdate(base.offset(-3, 0, 5), ((net.minecraft.world.level.block.CropBlock) CookingPlants.RHUBARB_CROP).getStateForAge(7));
			level.setBlockAndUpdate(base.offset(-2, 0, 5), CookingPlants.BLACKCURRANT_BUSH.defaultBlockState().setValue(HerbBushBlock.AGE, 3));
			level.setBlockAndUpdate(base.offset(-1, 0, 5), CookingPlants.REDCURRANT_BUSH.defaultBlockState().setValue(HerbBushBlock.AGE, 3));
			level.setBlockAndUpdate(base.offset(1, -1, 5), Blocks.GRAVEL.defaultBlockState());
			level.setBlockAndUpdate(base.offset(1, 0, 5), ((net.minecraft.world.level.block.CropBlock) CookingPlants.WASABI_CROP).getStateForAge(7));
			level.setBlockAndUpdate(base.offset(4, -1, 3), Blocks.OAK_LOG.defaultBlockState());
			level.setBlockAndUpdate(base.offset(4, 0, 3), CookingPlants.OYSTER_MUSHROOM.defaultBlockState().setValue(HerbBushBlock.AGE, 3));
			var treePos = base.offset(5, 0, 6);
			var sapling = (net.minecraft.world.level.block.SaplingBlock) ((net.minecraft.world.item.BlockItem) CookingPlants.item("plum_sapling")).getBlock();
			level.setBlockAndUpdate(treePos, sapling.defaultBlockState());
			for (int i = 0; i < 10 && level.getBlockState(treePos).is(sapling); i++) {
				sapling.advanceTree(level, treePos, level.getBlockState(treePos), net.minecraft.util.RandomSource.create(i));
			}
			for (BlockPos near : BlockPos.betweenClosed(treePos.offset(-3, 1, -3), treePos.offset(3, 8, 3))) {
				var state = level.getBlockState(near);
				if (state.is(CookingPlants.PLUM_LEAVES)) level.setBlockAndUpdate(near, state.setValue(FruitingLeavesBlock.AGE, (near.getX() + near.getZ()) % 3 == 0 ? 1 : 2));
			}
			player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() + 10.5, java.util.Set.of(), 180, 25, false);
		});
		world.getConnection().waitForClientboundPackets();
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		shot(context, "garden");
		context.runOnClient(client -> CookbookScreen.open());
		context.waitForScreen(CookbookScreen.class);
		world.getConnection().waitForServerboundPackets();
		world.getConnection().waitForClientboundPackets();
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant("plum_leaves"));
		shot(context, "cookbook-garden-plum");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant("cucumber_vine"));
		shot(context, "cookbook-garden-cucumber");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant("wasabi"));
		shot(context, "cookbook-garden-wasabi");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant("lemon_leaves"));
		shot(context, "cookbook-garden-lemon");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant("olive_leaves"));
		shot(context, "cookbook-garden-olive");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant("rice"));
		shot(context, "cookbook-garden-rice");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant("tea_bush"));
		shot(context, "cookbook-garden-tea");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant("cinnamon_log"));
		shot(context, "cookbook-garden-cinnamon");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant("coconut_leaves"));
		shot(context, "cookbook-garden-coconut");
		// B-0054: a spreading shellfish bed, a leafy crop that goes to seed, a perennial crown, and the cardoon.
		for (String plant : new String[] {"mussel_bed", "lettuce", "asparagus", "cardoon"}) {
			context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectPlant(plant));
			shot(context, "cookbook-garden-" + plant.replace('_', '-'));
		}
		context.runOnClient(client -> client.gui.setScreen(null));
	}

	/** B-0049: a cow carcass on a hanging hook, a dressed pig part-butchered on the block, and a Butchery page. */
	private static void captureButchery(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base) {
		BlockPos spot = base.offset(14, 0, 0);
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			var level = player.level();
			player.closeContainer();
			for (int x = -3; x <= 3; x++) for (int z = -3; z <= 5; z++) {
				level.setBlockAndUpdate(spot.offset(x, -1, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
				for (int y = 0; y < 5; y++) level.setBlockAndUpdate(spot.offset(x, y, z), Blocks.AIR.defaultBlockState());
			}
			BlockPos hookPos = spot.offset(-1, 1, 0);
			for (int x = -2; x <= 0; x++) for (int y = 0; y <= 2; y++) level.setBlockAndUpdate(spot.offset(x, y, -1), Blocks.SPRUCE_PLANKS.defaultBlockState());
			for (int x = -2; x <= 0; x++) level.setBlockAndUpdate(spot.offset(x, 2, 0), Blocks.SPRUCE_PLANKS.defaultBlockState());
			level.setBlockAndUpdate(hookPos, Butchery.HANGING_HOOK.defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
			var hook = (HangingHookBlockEntity) level.getBlockEntity(hookPos);
			hook.setItem(0, new ItemStack(Butchery.CARCASSES.get("cow")));
			level.sendBlockUpdated(hookPos, level.getBlockState(hookPos), level.getBlockState(hookPos), 3);
			level.setBlockAndUpdate(spot.offset(1, 0, 0), Butchery.BUTCHER_BLOCK.defaultBlockState());
			var block = (ButcherBlockBlockEntity) level.getBlockEntity(spot.offset(1, 0, 0));
			ItemStack pig = new ItemStack(Butchery.DRESSED.get("pig"));
			pig.set(Butchery.BUTCHERED, 3);
			block.setItem(0, pig);
			level.sendBlockUpdated(spot.offset(1, 0, 0), level.getBlockState(spot.offset(1, 0, 0)), level.getBlockState(spot.offset(1, 0, 0)), 3);
			player.teleportTo(level, spot.getX() + .5, spot.getY(), spot.getZ() + 3.2, java.util.Set.of(), 180, 18, false);
		});
		world.getConnection().waitForClientboundPackets();
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		shot(context, "butchery");
		context.runOnClient(client -> CookbookScreen.open());
		context.waitForScreen(CookbookScreen.class);
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectCarcass("cow"));
		shot(context, "cookbook-butchery-cow");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectCarcass("chicken"));
		shot(context, "cookbook-butchery-chicken");
		context.runOnClient(client -> client.gui.setScreen(null));
	}

	/** Phase 3: the mill, mixing bowl, crock, ageing shelf, and drying rack, a seasoned dish, and the cookbook's tastes. */
	private static void capturePhase3(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base) {
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			var level = player.level();
			player.closeContainer();
			var facing = Direction.SOUTH;
			level.setBlockAndUpdate(base.offset(-4, 0, -5), CookingBlocks.MILL.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
			var mill = (ProcessStationBlockEntity) level.getBlockEntity(base.offset(-4, 0, -5));
			mill.setItem(0, new ItemStack(Items.WHEAT, 12));
			mill.setItem(CookingMenu.Kind.MILL.outputSlot, new ItemStack(CookingItems.WHEAT_FLOUR, 5));
			level.setBlockAndUpdate(base.offset(-2, 0, -5), CookingBlocks.PREP_COUNTER.defaultBlockState());
			level.setBlockAndUpdate(base.offset(-2, 1, -5), CookingBlocks.MIXING_BOWL.defaultBlockState());
			var bowl = (ProcessStationBlockEntity) level.getBlockEntity(base.offset(-2, 1, -5));
			bowl.setItem(0, new ItemStack(CookingItems.WHEAT_FLOUR, 3));
			bowl.setItem(1, new ItemStack(CookingItems.SOURDOUGH_STARTER));
			bowl.setItem(2, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER));
			bowl.setItem(3, new ItemStack(CookingItems.SALT));
			bowl.work(level);
			bowl.work(level);
			bowl.work(level);
			level.setBlockAndUpdate(base.offset(0, 0, -5), CookingBlocks.CROCK.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
			var crock = (ProcessStationBlockEntity) level.getBlockEntity(base.offset(0, 0, -5));
			crock.setItem(0, new ItemStack(CookingItems.APPLE_JUICE, 4));
			level.setBlockAndUpdate(base.offset(2, 0, -5), CookingBlocks.AGEING_SHELF.defaultBlockState());
			var shelf = (SpotStationBlockEntity) level.getBlockEntity(base.offset(2, 0, -5));
			for (var item : java.util.List.of(CookingItems.BRINED_CHEESE, CookingItems.SOFT_CHEESE_CURDS, CookingItems.CANE_SUGAR_PASTE, CookingItems.SHORTCRUST_DOUGH)) {
				shelf.add(level, new ItemStack(item));
			}
			level.setBlockAndUpdate(base.offset(4, 0, -5), CookingBlocks.DRYING_RACK.defaultBlockState());
			var rack = (SpotStationBlockEntity) level.getBlockEntity(base.offset(4, 0, -5));
			for (var item : java.util.List.of(Items.KELP, CookingItems.SALTED_COD, CookingItems.CHOPPED_APPLE, CookingItems.CURED_BEEF_STRIPS)) {
				rack.add(level, new ItemStack(item));
			}
			level.setBlockAndUpdate(base.offset(-4, 0, -4), CookingBlocks.BUTTER_CHURN.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
			level.setBlockAndUpdate(base.offset(0, 0, -4), CookingBlocks.CHEESE_PRESS.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
			level.setBlockAndUpdate(base.offset(2, 0, -4), CookingBlocks.BRINE_BARREL.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
			level.setBlockAndUpdate(base.offset(4, 0, -4), CookingBlocks.DAIRY_PAN.defaultBlockState());
			level.setBlockAndUpdate(base.offset(-2, 0, -4), CookingBlocks.PROOFING_BASKET.defaultBlockState());
			var pot = (CookingPotBlockEntity) level.getBlockEntity(base.offset(-4, 1, -3));
			var soup = Seasoning.apply(new ItemStack(CookingItems.VEGETABLE_SOUP, 3), Seasoning.WELL_BALANCED);
			pot.setItem(CookingPotBlockEntity.OUTPUT_SLOT, soup);
			pot.setItem(CookingMenu.Kind.POT.seasoningSlots[0], new ItemStack(Items.DRIED_KELP, 4));
			// A strip of floor north of the stations, to look back at the new rows from the other side.
			for (int x = -5; x <= 5; x++) for (int z = -9; z <= -6; z++) {
				level.setBlockAndUpdate(base.offset(x, -1, z), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
				for (int y = 0; y < 4; y++) level.setBlockAndUpdate(base.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
			}
			player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() - 8.5, java.util.Set.of(), 0, 25, false);
		});
		world.getConnection().waitForClientboundPackets();
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		shot(context, "phase3-stations");
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			player.teleportTo(player.level(), base.getX() + .5, base.getY(), base.getZ() - 1.5, java.util.Set.of(), 180, 35, false);
		});
		world.getConnection().waitForClientboundPackets();
		open(context, world, base.offset(-4, 0, -5), CookingMenu.Kind.MILL);
		shot(context, "mill-screen");
		open(context, world, base.offset(-2, 1, -5), CookingMenu.Kind.MIXING_BOWL);
		shot(context, "mixing-bowl-screen");
		open(context, world, base.offset(0, 0, -5), CookingMenu.Kind.CROCK);
		context.waitTicks(5);
		shot(context, "crock-screen");
		open(context, world, base.offset(-4, 1, -3), CookingMenu.Kind.POT);
		double[] cursor = context.computeOnClient(client -> {
			int[] centre = ((CookingScreen) client.gui.screen()).slotCentre(CookingPotBlockEntity.OUTPUT_SLOT);
			double scale = client.getWindow().getGuiScale();
			return new double[] {centre[0] * scale, centre[1] * scale};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
		context.waitTicks(3);
		context.takeScreenshot(TestScreenshotOptions.of("cooking-seasoned-tooltip").disableCounterPrefix());
		world.getServer().runOnServer(server -> world.getConnection().getServerPlayer().closeContainer());
		world.getConnection().waitForClientboundPackets();
		context.runOnClient(client -> CookbookScreen.open());
		context.waitForScreen(CookbookScreen.class);
		world.getConnection().waitForServerboundPackets();
		world.getConnection().waitForClientboundPackets();
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).selectTaste(Seasoning.Taste.UMAMI));
		shot(context, "cookbook-umami");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).select(PortablePocketCabin.id("cooking/crock/sourdough_starter")));
		shot(context, "cookbook-sourdough");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).select(PortablePocketCabin.id("cooking/pot/vegetable_soup")));
		shot(context, "cookbook-soup-tastes");
		context.runOnClient(client -> client.gui.setScreen(null));
	}

	/** B-0052: the press part-way through a batch of apple juice, and the cookbook's sunflower oil page. */
	private static void capturePress(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base) {
		BlockPos pos = base.offset(1, 0, -1);
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			var level = player.level();
			player.closeContainer();
			level.setBlockAndUpdate(pos, CookingBlocks.PRESS.defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
			var press = (ProcessStationBlockEntity) level.getBlockEntity(pos);
			var kind = CookingMenu.Kind.PRESS;
			press.setItem(0, new ItemStack(CookingItems.CHOPPED_APPLE, 8));
			press.setItem(kind.containerSlot, new ItemStack(Items.GLASS_BOTTLE, 3));
			press.setItem(kind.outputSlot, new ItemStack(CookingItems.APPLE_JUICE));
			press.setItem(kind.byproductSlot, new ItemStack(CookingItems.APPLE_POMACE));
		});
		world.getConnection().waitForClientboundPackets();
		open(context, world, pos, CookingMenu.Kind.PRESS);
		context.waitTicks(20);
		shot(context, "press-screen");
		world.getServer().runOnServer(server -> world.getConnection().getServerPlayer().closeContainer());
		world.getConnection().waitForClientboundPackets();
		context.runOnClient(client -> CookbookScreen.open());
		context.waitForScreen(CookbookScreen.class);
		world.getConnection().waitForServerboundPackets();
		world.getConnection().waitForClientboundPackets();
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).select(PortablePocketCabin.id("cooking/press/sunflower_oil")));
		shot(context, "cookbook-sunflower-oil");
		context.runOnClient(client -> ((CookbookScreen) client.gui.screen()).select(PortablePocketCabin.id("cooking/press/pumpkin_seed_oil")));
		shot(context, "cookbook-pumpkin-seed-oil");
		context.runOnClient(client -> client.gui.setScreen(null));
	}

	/** B-0053: the prep counter with a strainer, a counter threading a skewer, the grill, the wok, and the æbleskive pan. */
	private static void captureCounterStations(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base) {
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			var level = player.level();
			player.closeContainer();
			// Extend the floor so the overview can stand further back.
			for (int x = -5; x <= 5; x++) for (int z = 3; z <= 6; z++) {
				level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
				for (int y = 0; y < 4; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
			}
			level.setBlockAndUpdate(base.offset(-4, 0, -1), CookingBlocks.PREP_COUNTER.defaultBlockState());
			level.setBlockAndUpdate(base.offset(-4, 1, -1), CookingBlocks.STRAINER.defaultBlockState());
			level.setBlockAndUpdate(base.offset(-2, 0, -1), CookingBlocks.PREP_COUNTER.defaultBlockState());
			var counter = (ProcessStationBlockEntity) level.getBlockEntity(base.offset(-2, 0, -1));
			counter.setItem(0, new ItemStack(CookingItems.SKEWER, 4));
			counter.setItem(1, new ItemStack(Items.BEEF, 4));
			counter.setItem(2, new ItemStack(Items.CARROT, 4));
			level.setBlockAndUpdate(base.offset(0, 0, -1), CookingBlocks.GRILL.defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
			var grill = (GrillBlockEntity) level.getBlockEntity(base.offset(0, 0, -1));
			grill.addFuel(new ItemStack(Items.COAL_BLOCK));
			grill.add(level, new ItemStack(CookingItems.RAW_BEEF_SKEWER));
			grill.add(level, new ItemStack(CookingItems.RAW_PORK_SKEWER));
			grill.add(level, new ItemStack(CookingItems.RAW_CHICKEN_SKEWER));
			var facing = CookingBlocks.STOVE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
			level.setBlockAndUpdate(base.offset(2, 0, -1), facing);
			level.setBlockAndUpdate(base.offset(2, 1, -1), CookingBlocks.WOK.defaultBlockState());
			level.setBlockAndUpdate(base.offset(4, 0, -1), facing);
			level.setBlockAndUpdate(base.offset(4, 1, -1), CookingBlocks.AEBLESKIVE_PAN.defaultBlockState());
			((ProcessStationBlockEntity) level.getBlockEntity(base.offset(2, 1, -1))).fillOil();
		});
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			player.teleportTo(player.level(), base.getX() + .5, base.getY(), base.getZ() + 5.5, java.util.Set.of(), 180, 20, false);
		});
		world.getConnection().waitForClientboundPackets();
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		shot(context, "counter-stations");
		// Back within reach: a station menu closes when the player is too far away.
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			player.teleportTo(player.level(), base.getX() + .5, base.getY(), base.getZ() + 1.5, java.util.Set.of(), 180, 35, false);
		});
		world.getConnection().waitForClientboundPackets();
		open(context, world, base.offset(-2, 0, -1), CookingMenu.Kind.COUNTER);
		shot(context, "counter-screen");
		open(context, world, base.offset(-4, 1, -1), CookingMenu.Kind.STRAINER);
		shot(context, "strainer-screen");
		open(context, world, base.offset(2, 1, -1), CookingMenu.Kind.WOK);
		shot(context, "wok-screen");
	}

	private static void open(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos, CookingMenu.Kind kind) {
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			player.closeContainer();
			CookingStationBlock.openMenu(player.level(), pos, player, kind);
		});
		world.getConnection().waitForClientboundPackets();
		context.waitForScreen(CookingScreen.class);
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(3);
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTick();
		context.takeScreenshot(TestScreenshotOptions.of("cooking-" + name).disableCounterPrefix());
	}
}
