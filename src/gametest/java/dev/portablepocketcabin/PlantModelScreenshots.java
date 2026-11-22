package dev.portablepocketcabin;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;

import java.util.Set;

/**
 * Plant block models for review: each plant in screenshots/plants at every growth stage, in a row from youngest to
 * ripe. Run with PPC_CAPTURE_PLANTS=1 ./gradlew runClientGameTest, optionally naming the plants in PPC_PLANTS.
 */
final class PlantModelScreenshots {
	private PlantModelScreenshots() {
	}

	static void capture(ClientGameTestContext context) {
		String[] plants = System.getenv().getOrDefault("PPC_PLANTS", "cabbage,tomato_vine").split(",");
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			for (String plant : plants) {
				world.getServer().runOnServer(server -> {
					var player = world.getConnection().getServerPlayer();
					var level = player.level();
					player.setGameMode(GameType.SPECTATOR);
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
					BlockPos base = player.blockPosition().offset(0, 30, 0);
					for (int x = -6; x <= 6; x++) for (int z = -3; z <= 3; z++) {
						level.setBlockAndUpdate(base.offset(x, -2, z), Blocks.DIRT.defaultBlockState());
						level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
						for (int y = 0; y < 4; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
					}
					var crop = (CropBlock) CookingPlants.PLANTS.get(plant);
					// One plant per growth stage, left to right, on watered farmland.
					for (int age = 0; age <= crop.getMaxAge(); age++) {
						var pos = base.offset(age - crop.getMaxAge() / 2, 0, 0);
						level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
						level.setBlockAndUpdate(pos, crop.getStateForAge(age));
					}
					player.teleportTo(level, base.getX() + .5, base.getY() + 1.5, base.getZ() + 3.8, Set.of(), 180, 24, false);
				});
				world.getConnection().waitForClientboundPackets();
				world.getConnection().waitForChunksRender();
				context.waitTicks(30);
				context.getInput().setCursorPos(0, 0);
				context.runOnClient(client -> client.gui.toastManager().clear());
				context.waitTick();
				context.takeScreenshot(TestScreenshotOptions.of("plant-" + plant).disableCounterPrefix());
			}
		}
	}
}
