package dev.portablepocketcabin.transfer.mixin;

import dev.portablepocketcabin.ArboretumTransferProbe;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class ArboretumFrameProbeMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void afterFrame(DeltaTracker delta, boolean renderLevel, CallbackInfo callback) {
        ArboretumTransferProbe.frame();
    }
}
