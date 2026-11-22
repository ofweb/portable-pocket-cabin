package dev.portablepocketcabin;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.Set;

/**
 * Plant block models for review: each plant in screenshots/plants at every growth stage, in a row from youngest to
 * ripe. Run with PPC_CAPTURE_PLANTS=1 ./gradlew runClientGameTest, optionally naming the plants in PPC_PLANTS.
 */
final class PlantModelScreenshots {
	private PlantModelScreenshots() {
	}

	static void capture(ClientGameTestContext context) {
		String[] plants = System.getenv().getOrDefault("PPC_PLANTS", "cabbage,tomato_vine,barley,onion,red_onion,radish,courgette,strawberry_bush,corn,white_grape_vine,rice,rhubarb,dill").split(",");
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
					var block = CookingPlants.PLANTS.get(plant);
					var age = (IntegerProperty) block.getStateDefinition().getProperty("age");
					int maxAge = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElseThrow();
					// One plant per growth stage, left to right: paddy crops standing in water on mud, walled in so the
					// water stays put; other crops on watered farmland; the rest on the grass.
					boolean paddy = block instanceof PaddyCropBlock;
					if (paddy) {
						for (int x = -6; x <= 6; x++) for (int z = -1; z <= 1; z++) {
							if (z != 0 || x < -maxAge / 2 || x > maxAge - maxAge / 2) level.setBlockAndUpdate(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
						}
					}
					for (int stage = 0; stage <= maxAge; stage++) {
						var pos = base.offset(stage - maxAge / 2, 0, 0);
						if (paddy) {
							level.setBlockAndUpdate(pos.below(), Blocks.MUD.defaultBlockState());
						} else if (block instanceof CropBlock) {
							level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
						}
						level.setBlock(pos, block.defaultBlockState().setValue(age, stage), Block.UPDATE_CLIENTS);
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
				// The same row from a front corner, since crossed planes look different from the diagonal.
				world.getServer().runOnServer(server -> {
					var player = world.getConnection().getServerPlayer();
					player.teleportTo(player.level(), player.getX() + 3.2, player.getY(), player.getZ() - 0.6, Set.of(), 140, 26, false);
				});
				world.getConnection().waitForClientboundPackets();
				context.waitTicks(10);
				context.takeScreenshot(TestScreenshotOptions.of("plant-" + plant + "-corner").disableCounterPrefix());
			}
		}
	}
}
