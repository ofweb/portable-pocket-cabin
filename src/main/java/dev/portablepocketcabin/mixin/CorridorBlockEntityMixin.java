package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinCorridors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
abstract class CorridorBlockEntityMixin {
	@Shadow @Final private BlockEntity blockEntity;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$pauseMovingWork(CallbackInfo callback) {
		if (blockEntity.getLevel() instanceof ServerLevel level && CabinCorridors.isPaused(level, blockEntity.getBlockPos())) callback.cancel();
	}
}
