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
				oven.setItem(0, new ItemStack(Items.WHEAT, 3));
				oven.setItem(1, new ItemStack(Items.SUGAR, 2));
				oven.setItem(2, new ItemStack(Items.EGG));
				for (int slot = 3; slot < 6; slot++) oven.setItem(slot, new ItemStack(Items.MILK_BUCKET));
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
		}
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
