package dev.portablepocketcabin;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Vanilla references. Run with PPC_CAPTURE_STATIONS=1 ./gradlew runClientGameTest. */
final class VanillaStationScreenshots {
	private VanillaStationScreenshots() { }

	static void capture(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runOnServer(server -> {
				var player = world.getConnection().getServerPlayer();
				player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
				player.awardRecipes(server.getRecipeManager().getRecipes());
				player.getInventory().clearContent();
				player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 16));
				player.getInventory().setItem(1, new ItemStack(Items.STICK, 8));
				player.getInventory().setItem(2, new ItemStack(Items.COBBLESTONE, 16));
				player.getInventory().setItem(3, new ItemStack(Items.RAW_IRON, 8));
				player.getInventory().setItem(4, new ItemStack(Items.COAL, 8));
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
			});
			world.getConnection().waitForClientboundPackets();
			world.getConnection().waitForChunksRender();
			bookSettings(context, false, false);
			open(context, world, Blocks.CRAFTING_TABLE, net.minecraft.client.gui.screens.inventory.CraftingScreen.class);
			shot(context, "01-crafting-table");
			bookSettings(context, true, false);
			open(context, world, Blocks.CRAFTING_TABLE, net.minecraft.client.gui.screens.inventory.CraftingScreen.class);
			shot(context, "02-crafting-recipe-book-all");
			bookSettings(context, true, true);
			open(context, world, Blocks.CRAFTING_TABLE, net.minecraft.client.gui.screens.inventory.CraftingScreen.class);
			shot(context, "03-crafting-recipe-book-craftable");
			bookSettings(context, false, false);
			open(context, world, Blocks.LOOM, net.minecraft.client.gui.screens.inventory.LoomScreen.class);
			shot(context, "04-loom-empty");
			inputs(world, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
				net.minecraft.resources.Identifier.withDefaultNamespace("white_banner")).orElseThrow()),
				new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
					net.minecraft.resources.Identifier.withDefaultNamespace("red_dye")).orElseThrow()));
			world.getConnection().waitForClientboundPackets();
			shot(context, "05-loom-patterns");
			open(context, world, Blocks.CARTOGRAPHY_TABLE, net.minecraft.client.gui.screens.inventory.CartographyTableScreen.class);
			shot(context, "06-cartography-empty");
			world.getServer().runOnServer(server -> {
				var player = world.getConnection().getServerPlayer();
				var map = net.minecraft.world.item.MapItem.create(player.level(), 0, 0, (byte) 0, true, false);
				player.containerMenu.slots.get(0).set(map);
				player.containerMenu.slots.get(1).set(new ItemStack(Items.PAPER));
				player.containerMenu.broadcastChanges();
			});
			world.getConnection().waitForClientboundPackets();
			shot(context, "07-cartography-expand-map");
			open(context, world, Blocks.STONECUTTER, net.minecraft.client.gui.screens.inventory.StonecutterScreen.class);
			shot(context, "08-stonecutter-empty");
			inputs(world, new ItemStack(Items.STONE, 8));
			world.getConnection().waitForClientboundPackets();
			shot(context, "09-stonecutter-recipes");
			open(context, world, Blocks.SMITHING_TABLE, net.minecraft.client.gui.screens.inventory.SmithingScreen.class);
			shot(context, "10-smithing-empty");
			var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
			pickaxe.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("My pickaxe"));
			inputs(world, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), pickaxe, new ItemStack(Items.NETHERITE_INGOT));
			world.getConnection().waitForClientboundPackets();
			shot(context, "11-smithing-netherite-upgrade");
			bookSettings(context, true, false);
			open(context, world, Blocks.FURNACE, net.minecraft.client.gui.screens.inventory.FurnaceScreen.class);
			shot(context, "12-furnace-recipe-book-all");
			bookSettings(context, true, true);
			open(context, world, Blocks.FURNACE, net.minecraft.client.gui.screens.inventory.FurnaceScreen.class);
			shot(context, "13-furnace-recipe-book-craftable");
		}
	}

	private static void bookSettings(ClientGameTestContext context, boolean open, boolean filtering) {
		context.runOnClient(client -> {
			for (var type : RecipeBookType.values()) client.player.getRecipeBook().setBookSetting(type, open, filtering);
		});
	}

	private static void open(ClientGameTestContext context, TestSingleplayerContext world, Block block,
		Class<? extends net.minecraft.client.gui.screens.Screen> screen) {
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			player.closeContainer();
			BlockPos position = player.blockPosition().offset(2, 0, 0);
			player.level().setBlockAndUpdate(position, block.defaultBlockState());
			player.openMenu(player.level().getBlockState(position).getMenuProvider(player.level(), position));
		});
		world.getConnection().waitForClientboundPackets();
		context.waitForScreen(screen);
	}

	private static void inputs(TestSingleplayerContext world, ItemStack... inputs) {
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			for (int i = 0; i < inputs.length; i++) player.containerMenu.slots.get(i).set(inputs[i]);
			player.containerMenu.broadcastChanges();
		});
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(3);
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTick();
		context.takeScreenshot(TestScreenshotOptions.of("vanilla-" + name).disableCounterPrefix());
	}
}
