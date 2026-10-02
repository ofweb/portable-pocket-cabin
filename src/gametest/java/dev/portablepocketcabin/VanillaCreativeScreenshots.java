package dev.portablepocketcabin;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

final class VanillaCreativeScreenshots {
	private VanillaCreativeScreenshots() { }

	static void capture(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			world.getServer().runOnServer(server -> {
				var player = world.getConnection().getServerPlayer();
				player.setGameMode(GameType.CREATIVE);
				player.getInventory().clearContent();
				player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 16));
				player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 3));
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
			});
			world.getConnection().waitForClientboundPackets();
			world.getConnection().waitForChunksRender();
			context.waitFor(client -> client.player.getAbilities().instabuild);
			context.runOnClient(client -> client.gui.setScreen(new CreativeModeInventoryScreen(
				client.player, client.level.enabledFeatures(), false)));
			context.waitForScreen(CreativeModeInventoryScreen.class);
			select(context, "building_blocks");
			shot(context, "01-building-blocks");
			context.runOnClient(client -> client.gui.screen().mouseScrolled(0, 0, 0, -3));
			shot(context, "02-building-blocks-scrolled");
			select(context, "colored_blocks");
			shot(context, "03-colored-blocks");
			select(context, "search");
			shot(context, "04-search-all");
			context.runOnClient(client -> "diamond".codePoints().forEach(codepoint ->
				client.gui.screen().charTyped(new CharacterEvent(codepoint))));
			shot(context, "05-search-diamond");
			int[] cursor = context.computeOnClient(client -> {
				var screen = client.gui.screen();
				return new int[] { ((screen.width - 195) / 2 + 17) * 3,
					((screen.height - 136) / 2 + 37) * 3 };
			});
			context.getInput().setCursorPos(cursor[0], cursor[1]);
			shot(context, "06-item-tooltip");
			select(context, "inventory");
			shot(context, "07-player-inventory");
		}
	}

	private static void select(ClientGameTestContext context, String name) {
		context.getInput().setCursorPos(0, 0);
		context.runOnClient(client -> {
			var screen = client.gui.screen();
			var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(Identifier.withDefaultNamespace(name)).orElseThrow();
			int x = tab.isAlignedRight() ? 195 - 27 * (7 - tab.column()) + 1 : 27 * tab.column();
			int y = tab.row() == CreativeModeTab.Row.TOP ? -32 : 136;
			var click = new MouseButtonEvent((screen.width - 195) / 2 + x + 13,
				(screen.height - 136) / 2 + y + 16, new MouseButtonInfo(0, 0));
			screen.mouseClicked(click, false);
			screen.mouseReleased(click);
		});
	}

	private static void shot(ClientGameTestContext context, String name) {
		if (!name.endsWith("tooltip")) context.getInput().setCursorPos(0, 0);
		context.waitTicks(3);
		context.runOnClient(client -> {
			client.gui.toastManager().clear();
			client.gui.hud.getChat().clearMessages(true);
		});
		context.waitTick();
		context.takeScreenshot(TestScreenshotOptions.of("vanilla-creative-" + name).disableCounterPrefix());
	}
}
