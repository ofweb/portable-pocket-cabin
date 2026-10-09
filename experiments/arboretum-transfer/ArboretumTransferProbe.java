package dev.portablepocketcabin;

import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Observes the real packet handler and rendered frames without changing either. */
public final class ArboretumTransferProbe {
    static final Path OUTPUT = Path.of(System.getenv("ARBORETUM_EVIDENCE"));
    static final List<Map<String, Object>> CASES = new ArrayList<>();
    private static Map<String, Object> current;
    private static List<Map<String, Object>> frames;
    private static List<Map<String, Object>> ticks;
    private static Vec3 lastTickPosition;
    private static int subsequentPositionPackets;
    private static Vec3 beforePosition, beforeOld, beforeVelocity;
    private static float beforeYaw, beforePitch;
    private static boolean received;

    static void arm(String name, double offset) {
        current = new LinkedHashMap<>();
        current.put("name", name);
        current.put("offset", offset);
        frames = new ArrayList<>();
        current.put("frames", frames);
        ticks = new ArrayList<>();
        current.put("ticks", ticks);
        subsequentPositionPackets = 0;
        received = false;
    }

    public static void beforePacket() {
        if (current == null || received || !Minecraft.getInstance().isSameThread()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        beforePosition = player.position();
        beforeOld = player.oldPosition();
        beforeVelocity = player.getDeltaMovement();
        beforeYaw = player.getYRot();
        beforePitch = player.getXRot();
    }

    public static void afterPacket() {
        if (current == null || beforePosition == null) return;
        if (received) { subsequentPositionPackets++; return; }
        var client = Minecraft.getInstance();
        var player = client.player;
        double offset = (double) current.get("offset");
        if (Math.abs(player.getX() - beforePosition.x - offset) > 1) return;
        received = true;
        var delta = new Vec3(offset, 0, 0);
        lastTickPosition = beforePosition.add(delta);
        current.put("chunk_ready_at_packet", chunkReady(client));
        current.put("floor_ready_at_packet", client.level.getBlockState(player.blockPosition().below()).is(Blocks.OAK_PLANKS));
        current.put("position_translation_error", player.position().subtract(beforePosition).subtract(delta).length());
        current.put("interpolation_translation_error", player.oldPosition().subtract(beforeOld).subtract(delta).length());
        current.put("velocity_before", vector(beforeVelocity));
        current.put("velocity_after", vector(player.getDeltaMovement()));
        current.put("velocity_error", player.getDeltaMovement().subtract(beforeVelocity).length());
        current.put("yaw_error", player.getYRot() - beforeYaw);
        current.put("pitch_error", player.getXRot() - beforePitch);
    }

    public static void frame() {
        if (current == null || !received || frames.size() >= 12) return;
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || client.level == null) return;
        var section = client.levelRenderer.viewArea().getRenderSectionAt(player.blockPosition());
        var sample = new LinkedHashMap<String, Object>();
        sample.put("frame", frames.size());
        sample.put("chunk_ready", chunkReady(client));
        sample.put("floor_ready", client.level.getBlockState(player.blockPosition().below()).is(Blocks.OAK_PLANKS));
        sample.put("passage_mesh_ready", section != null && section.getSectionMesh() != null && section.getSectionMesh().hasRenderableLayers());
        sample.put("camera", vector(client.gameRenderer.mainCamera().position()));
        sample.put("player", vector(player.position()));
        sample.put("screen", client.gui.screen() == null ? "none" : client.gui.screen().getClass().getSimpleName());
        sample.put("velocity", vector(player.getDeltaMovement()));
        frames.add(sample);
        if (frames.size() <= 4) {
            String name = current.get("name") + "-frame-" + (frames.size() - 1) + ".png";
            Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
                try (image) { image.writeToFile(OUTPUT.resolve(name)); }
                catch (Exception error) { throw new RuntimeException(error); }
            });
        }
    }

    static Map<String, Object> finish() {
        if (!received) throw new AssertionError("Transfer packet was not observed");
        var result = current;
        result.put("subsequent_position_packets", subsequentPositionPackets);
        CASES.add(result);
        current = null;
        return result;
    }

    static List<Double> vector(Vec3 value) { return List.of(value.x, value.y, value.z); }

    public static void tick() {
        if (current == null || !received || ticks.size() >= 80) return;
        var player = Minecraft.getInstance().player;
        var position = player.position();
        double direction = -Math.sin(Math.toRadians(beforeYaw));
        ticks.add(Map.of("tick", ticks.size(), "player", vector(position), "velocity", vector(player.getDeltaMovement()),
            "step_along_walk", (position.x - lastTickPosition.x) * direction));
        lastTickPosition = position;
    }

    private static boolean chunkReady(Minecraft client) {
        var pos = client.player.blockPosition();
        return client.level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false) != null;
    }

    static void save(Map<String, Object> extra) throws Exception {
        var report = new LinkedHashMap<String, Object>();
        report.put("minecraft", "26.2");
        report.put("dimension", "portable_pocket_cabin:pocket_home");
        report.put("cases", CASES);
        report.putAll(extra);
        Files.writeString(OUTPUT.resolve("results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
