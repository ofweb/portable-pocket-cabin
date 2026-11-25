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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.Set;

/**
 * Plant block models for review: each plant in screenshots/plants at every growth stage, in a row from youngest to
 * ripe. Run with PPC_CAPTURE_PLANTS=1 ./gradlew runClientGameTest, optionally naming the plants in PPC_PLANTS.
 */
final class PlantModelScreenshots {
	/** Plants that sprawl past their own block, shown spaced out. */
	private static final Set<String> WIDE = Set.of("courgette", "rhubarb");

	private PlantModelScreenshots() {
	}

	static void capture(ClientGameTestContext context) {
		String[] plants = System.getenv().getOrDefault("PPC_PLANTS", "cabbage,tomato_vine,barley,onion,red_onion,radish,courgette,strawberry_bush,corn,white_grape_vine,rice,rhubarb,dill,plum_leaves,raspberry_bush,asparagus,wakame_plant,white_bean,redcurrant_bush,red_cabbage,lettuce,blue_corn,purple_corn,grape_vine,red_grape_vine,glutinous_rice,rye,garlic,shallot,spring_onion,cucumber_vine,hop_vine,long_bean_vine,peppercorn_vine,vanilla_vine,betel_vine,blackcurrant_bush,tea_bush,thyme_bush,marjoram_bush,oregano_bush,savory_bush,mint_bush,rosemary_bush,pandan_bush,red_chili,green_chili,paprika_pepper,aji_amarillo,birds_eye_chili,bell_pepper,poblano_chili,aubergine,thai_aubergine,tomatillo,azuki_bean,black_bean,chickpea,soybean,peanut,cilantro,parsley,celery,anise,caraway,cumin,basil,holy_basil,thai_basil,mustard,rapeseed,sesame").split(",");
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			// One pad for every plant, fixed above where the player first stands; taken afresh from the player each time,
			// it would climb with the camera until it passed the top of the world.
			BlockPos[] origin = new BlockPos[1];
			for (String plant : plants) {
				world.getServer().runOnServer(server -> {
					var player = world.getConnection().getServerPlayer();
					var level = player.level();
					player.setGameMode(GameType.SPECTATOR);
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
					if (origin[0] == null) origin[0] = player.blockPosition().offset(0, 30, 0);
					BlockPos base = origin[0];
					// Rows of whole trees need a bigger pad, cleared higher, than a row of plants.
					boolean trees = plant.startsWith("tree:");
					int halfWidth = trees ? 30 : 12, halfDepth = trees ? 7 : 4, height = trees ? 18 : 12;
					for (int x = -halfWidth; x <= halfWidth; x++) for (int z = -halfDepth; z <= halfDepth; z++) {
						level.setBlockAndUpdate(base.offset(x, -2, z), Blocks.DIRT.defaultBlockState());
						level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
						for (int y = 0; y < height; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
					}
					// tree:<feature> grows five trees of a tree feature, vanilla's or ours, each from a different seed, since
					// no two come out quite alike.
					if (trees) {
						var features = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
						var tree = features.getValue(net.minecraft.resources.Identifier.parse(plant.substring("tree:".length())));
						for (int i = 0; i < 5; i++) {
							var trunk = base.offset((i - 2) * 12, 0, 0);
							tree.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(1000 + i * 7919), trunk);
							// The five trees show their leaves' stages in turn: leaves, blossom, then fruit, on the leaves that bear.
							for (var pos : BlockPos.betweenClosed(trunk.offset(-7, 0, -7), trunk.offset(7, 16, 7))) {
								var found = level.getBlockState(pos);
								if (found.getBlock() instanceof FruitingLeavesBlock fruiting && FruitingLeavesBlock.bears(pos)) {
									var stage = fruiting.stage();
									int last = stage.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
									level.setBlock(pos, found.setValue(stage, Math.min(i, last)).setValue(LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
								}
							}
							// Log each tree's height and width, to check what fits under a greenhouse roof.
							int top = 0, minX = 99, maxX = -99, leafBlocks = 0;
							for (var pos : BlockPos.betweenClosed(trunk.offset(-6, 0, -6), trunk.offset(6, 16, 6))) {
								if (level.getBlockState(pos).isAir()) continue;
								if (level.getBlockState(pos).getBlock() instanceof FruitingLeavesBlock) leafBlocks++;
								top = Math.max(top, pos.getY() - trunk.getY() + 1);
								minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
							}
							System.out.println("TREE " + plant + " #" + i + " height " + top + " width " + (maxX - minX + 1) + " leaves " + leafBlocks);
						}
						server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=item]");
						player.teleportTo(level, base.getX() + .5, base.getY() + 5, base.getZ() + 30, Set.of(), 180, 4, false);
						return;
					}
					// Minecraft's own bushes, for comparison: sweet berry bush through its stages, azalea, flowering azalea,
					// bush, and firefly bush.
					if (plant.equals("vanilla_bushes")) {
						var bushes = new java.util.ArrayList<net.minecraft.world.level.block.state.BlockState>();
						for (int stage = 1; stage <= 3; stage++) bushes.add(Blocks.SWEET_BERRY_BUSH.defaultBlockState()
							.setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE, stage));
						for (var other : java.util.List.of(Blocks.AZALEA, Blocks.FLOWERING_AZALEA, Blocks.BUSH, Blocks.FIREFLY_BUSH)) bushes.add(other.defaultBlockState());
						for (int i = 0; i < bushes.size(); i++) level.setBlock(base.offset(i - bushes.size() / 2, 0, 0), bushes.get(i), Block.UPDATE_CLIENTS);
						server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=item]");
						player.teleportTo(level, base.getX() + .5, base.getY() + 1.5, base.getZ() + 3.8, Set.of(), 180, 24, false);
						return;
					}
					var block = CookingPlants.PLANTS.get(plant);
					var age = (IntegerProperty) block.getStateDefinition().getProperty("age");
					int maxAge = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElseThrow();
					// One plant per growth stage, left to right: paddy crops standing in water on mud, walled in so the
					// water stays put; seaweed on sand under a block of water, walled in too; other crops on watered
					// farmland; the rest on the grass.
					boolean paddy = block instanceof PaddyCropBlock;
					boolean sea = block instanceof SeaPlantBlock;
					boolean leaves = block instanceof FruitingLeavesBlock;
					// Fruit trees are grown whole from their own tree feature, one tree per stage of their leaves.
					if (leaves) {
						var features = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
						var tree = features.getValue(PortablePocketCabin.id(plant.replace("_leaves", "")));
						for (int stage = 0; stage <= maxAge; stage++) {
							var trunk = base.offset((stage - maxAge / 2) * 7, 0, 0);
							tree.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(42), trunk);
							for (var pos : BlockPos.betweenClosed(trunk.offset(-3, 0, -3), trunk.offset(3, 10, 3))) {
								var found = level.getBlockState(pos);
								if (found.is(block)) level.setBlock(pos, found.setValue(age, stage).setValue(LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
							}
						}
						server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=item]");
						player.teleportTo(level, base.getX() + .5, base.getY() + 3.5, base.getZ() + 14, Set.of(), 180, 6, false);
						return;
					}
					if (sea) {
						for (int x = -9; x <= 9; x++) for (int z = -2; z <= 2; z++) {
							boolean pool = Math.abs(z) <= 1 && x >= -maxAge / 2 - 1 && x <= maxAge - maxAge / 2 + 1;
							level.setBlockAndUpdate(base.offset(x, -1, z), (pool ? Blocks.SAND : Blocks.GRASS_BLOCK).defaultBlockState());
							level.setBlockAndUpdate(base.offset(x, -2, z), Blocks.DIRT.defaultBlockState());
							if (pool) level.setBlockAndUpdate(base.offset(x, 0, z), Blocks.WATER.defaultBlockState());
							else if (Math.abs(z) <= 2) level.setBlockAndUpdate(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
						}
					}
					if (paddy) {
						for (int x = -9; x <= 9; x++) for (int z = -1; z <= 1; z++) {
							if (z != 0 || x < -maxAge / 2 || x > maxAge - maxAge / 2) level.setBlockAndUpdate(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
						}
					}
					// Plants that sprawl past their block, such as courgette, stand two blocks apart so each can be seen whole.
					int spacing = WIDE.contains(plant) ? 2 : 1;
					for (int stage = 0; stage <= maxAge; stage++) {
						var pos = base.offset((stage - maxAge / 2) * spacing, 0, 0);
						if (paddy) {
							level.setBlockAndUpdate(pos.below(), Blocks.MUD.defaultBlockState());
						} else if (block instanceof CropBlock) {
							level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
						}
						var state = block.defaultBlockState().setValue(age, stage);
						// Tall vines get a second trellis to climb, climbed from their tall stage on; tall crops their top.
						if (block instanceof TrellisCropBlock vine && vine.tallFrom() >= 0) {
							boolean tall = stage >= vine.tallFrom();
							state = state.setValue(TrellisCropBlock.TALL, tall);
							level.setBlock(pos.above(), tall ? CookingPlants.PLANT_TOP.defaultBlockState().setValue(PlantTopBlock.TRELLIS, true)
								: CookingPlants.TRELLIS.defaultBlockState(), Block.UPDATE_CLIENTS);
						} else if (block instanceof FieldCropBlock crop && crop.fillsAbove(state)) {
							level.setBlock(pos.above(), CookingPlants.PLANT_TOP.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
						level.setBlock(pos, state, Block.UPDATE_CLIENTS);
					}
					// Clearing the last plant can drop its trellis; tidy away anything dropped.
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=item]");
					// Plants two blocks tall are seen from further back and higher up, so their tops are in view.
					boolean tall = block instanceof FieldCropBlock crop && crop.tallFrom() >= 0;
					if (sea) player.teleportTo(level, base.getX() + .5, base.getY() + 1.4, base.getZ() + 3.2, Set.of(), 180, 38, false);
					else if (spacing > 1) player.teleportTo(level, base.getX() + 1.5, base.getY() + 2.4, base.getZ() + 5.0, Set.of(), 180, 24, false);
					else player.teleportTo(level, base.getX() + .5, base.getY() + (tall ? 2.1 : 1.5), base.getZ() + (tall ? 4.4 : 3.8), Set.of(), 180,
						tall ? 16 : 24, false);
				});
				world.getConnection().waitForClientboundPackets();
				world.getConnection().waitForChunksRender();
				context.waitTicks(30);
				context.getInput().setCursorPos(0, 0);
				context.runOnClient(client -> client.gui.toastManager().clear());
				// Blocks cleared for this plant can drop items a few ticks later; clear them again before the picture.
				world.getServer().runOnServer(server -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=item]"));
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
