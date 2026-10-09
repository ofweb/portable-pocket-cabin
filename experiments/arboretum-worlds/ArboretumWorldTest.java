package dev.portablepocketcabin;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.Files;
import java.util.Map;

public final class ArboretumWorldTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try {
            Files.createDirectories(ArboretumTransferProbe.OUTPUT);
            if (Boolean.getBoolean("arboretum.observer")) {
                ArboretumMultiplayerTest.observe(context);
                return;
            }
            context.getInput().resizeWindow(960, 540);
            var multiplayer = ArboretumMultiplayerTest.run(context);
            ArboretumTransferProbe.save(Map.of("multiplayer", multiplayer,
                "world_keys", java.util.List.of(ArboretumWorldHost.A.identifier().toString(),
                    ArboretumWorldHost.B.identifier().toString()),
                "worlds_created_after_actor_login", true,
                "java", System.getProperty("java.version")));
            System.out.println("ARBORETUM_WORLD_CLIENT_PASSED");
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
