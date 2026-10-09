package dev.portablepocketcabin;

import dev.portablepocketcabin.worldtest.ArboretumWorldServerAccess;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import java.nio.file.Files;
import java.util.List;

/** Disposable host prototype; no production cabin state is changed. */
public final class ArboretumWorldHost {
    public static final ResourceKey<Level> A = key("11111111-1111-1111-1111-111111111111");
    public static final ResourceKey<Level> B = key("22222222-2222-2222-2222-222222222222");
    private static final BlockPos CHEST = new BlockPos(2, 64, 0);
    private static int ticks;

    private static ResourceKey<Level> key(String cabin) {
        return ResourceKey.create(Registries.DIMENSION, PortablePocketCabin.id("arboretum/" + cabin));
    }

    public static ServerLevel load(MinecraftServer server, ResourceKey<Level> key) {
        var existing = server.getLevel(key);
        if (existing != null) return existing;
        var access = (ArboretumWorldServerAccess) server;
        var template = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM)
            .getOrThrow(PocketDimension.STEM_KEY).value();
        var stem = new LevelStem(template.type(), template.generator());
        var world = new ServerLevel(server, access.executor(), access.storage(),
            new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()),
            key, stem, false, 1234L, List.of(), false);
        access.worlds().put(key, world);
        return world;
    }

    public static void platform(ServerLevel level, BlockPos origin, Block floor) {
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            level.setBlockAndUpdate(origin.offset(x, 0, z), floor.defaultBlockState());
        }
    }

    public static void register() {
        String phase = System.getenv("ARBORETUM_WORLD_PHASE");
        if (phase == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                var a = load(server, A);
                var b = load(server, B);
                require(a != b && a.dimension().equals(A) && b.dimension().equals(B), "Different worlds");
                if (phase.equals("seed")) {
                    platform(a, new BlockPos(0, 63, 0), Blocks.OAK_PLANKS);
                    platform(b, new BlockPos(0, 63, 0), Blocks.BIRCH_PLANKS);
                    a.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
                    ((ChestBlockEntity) a.getBlockEntity(CHEST)).setItem(0, new ItemStack(Items.APPLE, 13));
                    server.saveEverything(false, true, false);
                    a.close();
                    access(server).worlds().remove(A);
                    a = load(server, A);
                    require(a.getBlockState(new BlockPos(0, 63, 0)).is(Blocks.OAK_PLANKS), "Terrain survived unload");
                    var reloaded = (ChestBlockEntity) a.getBlockEntity(CHEST);
                    require(reloaded != null && reloaded.getItem(0).getCount() == 13, "Inventory survived unload");
                    require(!access(server).storage().getDimensionPath(A)
                        .equals(access(server).storage().getDimensionPath(B)), "Separate save paths");
                    Files.writeString(access(server).storage().getDimensionPath(A).resolve("identity.txt"), A.identifier().toString());
                    PortablePocketCabin.LOGGER.info("ARBORETUM_WORLD_SEED_PASSED");
                    PortablePocketCabin.LOGGER.info("ARBORETUM_WORLD_UNLOAD_PASSED");
                    server.halt(false);
                } else if (!phase.equals("verify")) throw new AssertionError("Unknown phase");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!phase.equals("verify") || ticks++ < 5) return;
            var a = server.getLevel(A);
            var b = server.getLevel(B);
            require(a.getBlockState(new BlockPos(0, 63, 0)).is(Blocks.OAK_PLANKS), "A terrain survived restart");
            require(b.getBlockState(new BlockPos(0, 63, 0)).is(Blocks.BIRCH_PLANKS), "B terrain stayed isolated");
            var chest = (ChestBlockEntity) a.getBlockEntity(CHEST);
            require(chest != null && chest.getItem(0).is(Items.APPLE) && chest.getItem(0).getCount() == 13,
                "Inventory survived restart without duplication");
            PortablePocketCabin.LOGGER.info("ARBORETUM_WORLD_RESTART_PASSED");
            server.halt(false);
        });
    }

    private static ArboretumWorldServerAccess access(MinecraftServer server) {
        return (ArboretumWorldServerAccess) server;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
