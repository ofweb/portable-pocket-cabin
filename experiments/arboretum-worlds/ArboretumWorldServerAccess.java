package dev.portablepocketcabin.worldtest;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public interface ArboretumWorldServerAccess {
    @Accessor("levels") Map<ResourceKey<Level>, ServerLevel> worlds();
    @Accessor("storageSource") LevelStorageSource.LevelStorageAccess storage();
    @Accessor("executor") Executor executor();
}
