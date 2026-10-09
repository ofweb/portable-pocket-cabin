package dev.portablepocketcabin;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/** Two actual clients connected to one disposable offline dedicated server. */
public final class ArboretumMultiplayerTest {
    private static final Path OUTPUT = ArboretumTransferProbe.OUTPUT;
    private static final BlockPos SOURCE = new BlockPos(1024, 63, 1024);
    private static final BlockPos FAR = SOURCE.offset(4096, 0, 0);

    static Map<String, Object> run(ClientGameTestContext context) throws Exception {
        int port;
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { port = socket.getLocalPort(); }
        var properties = new Properties();
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("server-port", Integer.toString(port));
        properties.setProperty("max-players", "2");
        properties.setProperty("view-distance", "8");
        properties.setProperty("enforce-secure-profile", "false");
        properties.setProperty("level-name", "arboretum-multiplayer-world");
        Process observer = null;
        try (var server = context.worldBuilder().createServer(properties); var connection = server.connect()) {
            server.runOnServer(instance -> {
                var level = instance.getLevel(PocketDimension.LEVEL_KEY);
                ArboretumTransferTest.build(level, SOURCE);
                ArboretumTransferTest.build(level, FAR);
                var player = connection.getServerPlayer();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 7));
                player.teleportTo(level, SOURCE.getX() - 2.5, 64, SOURCE.getZ() + 4.5, Set.of(), 90, 0, false);
            });
            connection.waitForClientboundPackets();
            UUID actor = server.computeOnServer(instance -> connection.getServerPlayer().getUUID());
            write("observer-config.json", Map.of("port", port, "actor", actor.toString()));
            observer = launchObserver();
            await(context, observer, "observer-login.ready");
            server.waitFor(instance -> instance.getPlayerCount() == 2);
            moveObserver(server, actor, SOURCE);
            var stages = new ArrayList<Map<String, Object>>();
            stages.add(stage(context, observer, "together-source", true));
            moveActor(server, connection, FAR);
            stages.add(stage(context, observer, "actor-far", false));
            moveObserver(server, actor, FAR);
            stages.add(stage(context, observer, "together-far", true));
            moveActor(server, connection, SOURCE);
            stages.add(stage(context, observer, "actor-source", false));
            moveObserver(server, actor, SOURCE);
            stages.add(stage(context, observer, "together-returned", true));
            boolean retained = server.computeOnServer(instance -> {
                var player = connection.getServerPlayer();
                return player.getUUID().equals(actor) && player.getMainHandItem().is(Items.DIAMOND)
                    && player.getMainHandItem().getCount() == 7 && player.getHealth() == 20;
            });
            if (!retained) throw new AssertionError("Transfer changed actor identity, inventory, or health");
            Files.writeString(OUTPUT.resolve("observer-done"), "done");
            await(context, observer, "observer-finished.ready");
            return Map.of("clients", 2, "connection", "localhost TCP; offline dedicated server", "stages", stages,
                "actor_identity_inventory_health_retained", retained,
                "observer_report", "observer-results.json");
        } finally {
            if (observer != null && observer.isAlive()) {
                observer.destroy();
                if (!observer.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) observer.destroyForcibly();
            }
        }
    }

    private static void moveActor(TestDedicatedServerContext server, TestDedicatedServerConnection connection, BlockPos center) {
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer();
            player.teleportTo(instance.getLevel(PocketDimension.LEVEL_KEY), center.getX() - 2.5, 64,
                center.getZ() + 4.5, Set.of(), 90, 0, false);
        });
    }

    private static void moveObserver(TestDedicatedServerContext server, UUID actor, BlockPos center) {
        server.runOnServer(instance -> {
            var observer = instance.getPlayerList().getPlayers().stream().filter(player -> !player.getUUID().equals(actor)).findFirst().orElseThrow();
            observer.teleportTo(instance.getLevel(PocketDimension.LEVEL_KEY), center.getX() - 5.5, 64,
                center.getZ() + 4.5, Set.of(), -90, 0, false);
        });
    }

    private static Map<String, Object> stage(ClientGameTestContext context, Process observer, String name, boolean visible) throws Exception {
        write("observer-phase.json", Map.of("name", name, "expected_visible", visible));
        String filename = name + "-observed.json";
        await(context, observer, filename);
        var json = JsonParser.parseString(Files.readString(OUTPUT.resolve(filename))).getAsJsonObject();
        if (json.get("visible").getAsBoolean() != visible) throw new AssertionError("Observer did not reach expected tracking state");
        return Map.of("phase", name, "actor_visible", visible, "samples", json.get("samples").getAsInt(),
            "first_sample_visible", json.get("first_sample_visible").getAsBoolean());
    }

    private static void await(ClientGameTestContext context, Process observer, String filename) throws Exception {
        long deadline = System.nanoTime() + 120_000_000_000L;
        while (!Files.exists(OUTPUT.resolve(filename))) {
            if (!observer.isAlive()) throw new AssertionError("Observer exited before " + filename + "; inspect observer-output.txt");
            if (System.nanoTime() > deadline) throw new AssertionError("Observer timed out at " + filename);
            context.waitTick();
            Thread.sleep(10);
        }
    }

    private static Process launchObserver() throws Exception {
        Path directory = OUTPUT.resolve("observer-client");
        Files.createDirectories(directory);
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin/java").toString());
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        command.add("-Darboretum.observer=true");
        command.add("-Xmx1G");
        command.addAll(List.of("-cp", System.getProperty("java.class.path"), "net.fabricmc.devlaunchinjector.Main",
            "--username", "ArboObserver", "--gameDir", directory.toString()));
        return new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true)
            .redirectOutput(OUTPUT.resolve("observer-output.txt").toFile()).start();
    }

    static void observe(ClientGameTestContext context) throws Exception {
        var config = JsonParser.parseString(Files.readString(OUTPUT.resolve("observer-config.json"))).getAsJsonObject();
        UUID actor = UUID.fromString(config.get("actor").getAsString());
        String address = "127.0.0.1:" + config.get("port").getAsInt();
        context.runOnClient(client -> ConnectScreen.startConnecting(new TitleScreen(), client,
            ServerAddress.parseString(address), new ServerData("Arboretum experiment", address, ServerData.Type.OTHER), false, null));
        context.waitFor(client -> client.level != null && client.player != null && client.gui.screen() == null, 1200);
        Files.writeString(OUTPUT.resolve("observer-login.ready"), "ready");
        String phase = "";
        int samples = 0;
        boolean firstVisible = false;
        var trace = new ArrayList<Map<String, Object>>();
        long deadline = System.nanoTime() + 240_000_000_000L;
        while (!Files.exists(OUTPUT.resolve("observer-done"))) {
            if (System.nanoTime() > deadline) throw new AssertionError("Observer did not receive completion signal");
            context.waitTick();
            Path phaseFile = OUTPUT.resolve("observer-phase.json");
            if (!Files.exists(phaseFile)) continue;
            var desired = JsonParser.parseString(Files.readString(phaseFile)).getAsJsonObject();
            String name = desired.get("name").getAsString();
            var sample = context.computeOnClient(client -> {
                var data = new LinkedHashMap<String, Object>();
                var entities = new ArrayList<net.minecraft.world.entity.Entity>();
                client.level.entitiesForRendering().forEach(entity -> { if (entity.getUUID().equals(actor)) entities.add(entity); });
                data.put("phase", name);
                data.put("visible", !entities.isEmpty());
                data.put("copies", entities.size());
                data.put("observer", ArboretumTransferProbe.vector(client.player.position()));
                if (!entities.isEmpty()) data.put("actor", ArboretumTransferProbe.vector(entities.getFirst().position()));
                return data;
            });
            boolean visible = (boolean) sample.get("visible");
            if (!phase.equals(name)) { phase = name; samples = 0; firstVisible = visible; }
            samples++;
            trace.add(sample);
            if ((int) sample.get("copies") > 1) throw new AssertionError("Observer sees duplicate actor identity");
            Path acknowledgement = OUTPUT.resolve(name + "-observed.json");
            if (!Files.exists(acknowledgement) && samples >= 15 && visible == desired.get("expected_visible").getAsBoolean()) {
                write(name + "-observed.json", Map.of("visible", visible, "samples", samples, "first_sample_visible", firstVisible));
                context.takeScreenshot("observer-" + name);
            }
        }
        write("observer-results.json", Map.of("observer", "ArboObserver", "samples", trace));
        context.runOnClient(client -> client.disconnect(new TitleScreen(), false));
        Files.writeString(OUTPUT.resolve("observer-finished.ready"), "done");
    }

    private static void write(String filename, Object data) throws Exception {
        Path temporary = OUTPUT.resolve(filename + ".partial");
        Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(data));
        Files.move(temporary, OUTPUT.resolve(filename), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
