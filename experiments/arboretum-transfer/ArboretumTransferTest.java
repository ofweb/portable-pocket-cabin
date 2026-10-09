package dev.portablepocketcabin;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ArboretumTransferTest implements FabricClientGameTest {
    private static final BlockPos SOURCE = new BlockPos(1024, 63, 1024);
    private static final BlockPos NEAR = SOURCE.offset(64, 0, 0);
    private static final BlockPos FAR = SOURCE.offset(4096, 0, 0);

    @Override public void runTest(ClientGameTestContext context) {
        try {
            Files.createDirectories(ArboretumTransferProbe.OUTPUT);
            if (Boolean.getBoolean("arboretum.observer")) {
                ArboretumMultiplayerTest.observe(context);
                return;
            }
            context.getInput().resizeWindow(960, 540);
            context.runOnClient(client -> {
                client.options.renderDistance().set(8);
                client.options.bobView().set(false);
            });
            try (var world = context.worldBuilder().create()) {
                world.getServer().runOnServer(server -> {
                    ServerLevel level = server.getLevel(PocketDimension.LEVEL_KEY);
                    for (BlockPos center : new BlockPos[]{SOURCE, NEAR, FAR}) build(level, center);
                    var player = world.getConnection().getServerPlayer();
                    player.teleportTo(level, SOURCE.getX() - 2.5, 64, SOURCE.getZ() + 4.5, Set.of(), 90, 0, false);
                });
                context.waitFor(client -> client.level != null && client.level.dimension().equals(PocketDimension.LEVEL_KEY)
                    && client.player.getX() > 900);
                waitForPassage(context);
                runCase(context, world, "same-location-rendered-control", SOURCE, SOURCE, Relative.DELTA);
                runCase(context, world, "near-loaded-default", SOURCE, NEAR, Set.of());
                runCase(context, world, "near-visited-preserve-motion", SOURCE, NEAR, Relative.DELTA);
                runCase(context, world, "far-server-preloaded-default", SOURCE, FAR, Set.of());
                runCase(context, world, "far-server-preloaded-preserve-motion", SOURCE, FAR, Relative.DELTA);
                runCase(context, world, "return-server-preloaded-preserve-motion", FAR, SOURCE, Relative.DELTA);
                runCase(context, world, "far-relative-translation", SOURCE, FAR, Relative.ALL);
                runCase(context, world, "return-relative-translation", FAR, SOURCE, Relative.ALL);
            }
            var multiplayer = ArboretumMultiplayerTest.run(context);
            ArboretumTransferProbe.save(Map.of("multiplayer", multiplayer,
                "trees", "absent; passage concealment uses fixed walls",
                "java", System.getProperty("java.version"), "render_distance", 8, "hidden_test_windows", true));
            System.out.println("ARBORETUM_TRANSFER_EXPERIMENT_COMPLETED");
        } catch (Exception error) { throw new RuntimeException(error); }
    }

    private static void runCase(ClientGameTestContext context, TestSingleplayerContext world,
                                String name, BlockPos source, BlockPos destination, Set<Relative> flags) {
        float yaw = destination.getX() > source.getX() ? 90 : -90;
        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            player.teleportTo((ServerLevel) player.level(), source.getX() - 2.5, 64, source.getZ() + 4.5, Set.of(), yaw, 0, false);
        });
        world.getConnection().waitForClientboundPackets();
        context.waitFor(client -> Math.abs(client.player.getX() - (source.getX() - 2.5)) < .1);
        waitForPassage(context);
        context.takeScreenshot(TestScreenshotOptions.of(name + "-before").disableCounterPrefix().withDestinationDir(ArboretumTransferProbe.OUTPUT));
        context.getInput().holdKey(options -> options.keyUp);
        context.waitTicks(3);
        world.getConnection().waitForServerboundPackets();
        context.runOnClient(client -> ArboretumTransferProbe.arm(name, destination.getX() - source.getX()));
        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            boolean relative = flags.contains(Relative.X);
            double offset = destination.getX() - source.getX();
            if (!player.teleportTo((ServerLevel) player.level(), relative ? offset : player.getX() + offset,
                relative ? 0 : player.getY(), relative ? 0 : player.getZ(), flags,
                relative ? 0 : player.getYRot(), relative ? 0 : player.getXRot(), false)) {
                throw new AssertionError("Same-dimension transfer refused");
            }
        });
        context.waitTicks(6);
        context.getInput().releaseKey(options -> options.keyUp);
        world.getConnection().waitForClientboundPackets();
        waitForPassage(context);
        context.waitTicks(3);
        context.takeScreenshot(TestScreenshotOptions.of(name + "-settled").disableCounterPrefix().withDestinationDir(ArboretumTransferProbe.OUTPUT));
        context.runOnClient(client -> {
            var result = ArboretumTransferProbe.finish();
            var frames = (List<?>) result.get("frames");
            if (frames.isEmpty()) throw new AssertionError("No rendered frames were sampled");
            if (flags.contains(Relative.DELTA_X) && (double) result.get("velocity_error") > 1e-6) {
                throw new AssertionError("Relative motion flags did not preserve velocity");
            }
            if (flags.contains(Relative.X) && ((double) result.get("position_translation_error") > 1e-6
                || (double) result.get("interpolation_translation_error") > 1e-6)) {
                throw new AssertionError("Relative translation introduced a movement correction");
            }
            var ticks = (List<?>) result.get("ticks");
            if (ticks.size() < 3) throw new AssertionError("Too few movement ticks were sampled");
            if (flags.contains(Relative.X) && (int) result.get("subsequent_position_packets") != 0) {
                throw new AssertionError("Relative translation caused a later server correction");
            }
            if (flags.contains(Relative.X) && ticks.stream().anyMatch(tick ->
                ((Number) ((Map<?, ?>) tick).get("step_along_walk")).doubleValue() < -.01)) {
                throw new AssertionError("Relative translation caused a backward movement step");
            }
            if (name.equals("same-location-rendered-control") && !((Map<?, ?>) frames.getFirst()).get("passage_mesh_ready").equals(true)) {
                throw new AssertionError("The rendered control did not detect its existing passage mesh");
            }
            if (Math.abs(((Number) result.get("yaw_error")).doubleValue()) > .001
                || Math.abs(((Number) result.get("pitch_error")).doubleValue()) > .001) {
                throw new AssertionError("Transfer changed camera rotation");
            }
        });
    }

    private static void waitForPassage(ClientGameTestContext context) {
        context.waitFor(client -> {
            var section = client.levelRenderer.viewArea().getRenderSectionAt(client.player.blockPosition());
            return client.level.getBlockState(client.player.blockPosition().below()).is(Blocks.OAK_PLANKS)
                && section != null && section.getSectionMesh() != null && section.getSectionMesh().hasRenderableLayers();
        }, 200);
    }

    static void build(ServerLevel level, BlockPos center) {
        var floor = new LinkedHashSet<BlockPos>();
        rectangle(floor, center, -1, 1, -5, 5);
        rectangle(floor, center, -7, 1, 3, 5);
        rectangle(floor, center, -7, -5, 3, 12);
        for (BlockPos pos : floor) {
            level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 2);
            level.setBlock(pos.above(4), Blocks.SEA_LANTERN.defaultBlockState(), 2);
            for (int y = 1; y <= 3; y++) level.setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), 2);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos side = pos.relative(direction);
                if (!floor.contains(side) && !(pos.getZ() == center.getZ() + 12 && direction == Direction.SOUTH)) {
                    for (int y = 0; y <= 4; y++) level.setBlock(side.above(y), Blocks.OAK_PLANKS.defaultBlockState(), 2);
                }
            }
        }
        for (int x = -31; x <= 19; x++) {
            for (int z = 13; z <= 63; z++) level.setBlock(center.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        }
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 4; z++) {
            level.setChunkForced((center.getX() >> 4) + x, (center.getZ() >> 4) + z, true);
            level.getChunk((center.getX() >> 4) + x, (center.getZ() >> 4) + z);
        }
    }

    private static void rectangle(Set<BlockPos> floor, BlockPos center, int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) floor.add(center.offset(x, 0, z));
    }
}
