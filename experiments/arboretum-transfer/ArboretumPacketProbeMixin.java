package dev.portablepocketcabin.transfer.mixin;

import dev.portablepocketcabin.ArboretumTransferProbe;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ArboretumPacketProbeMixin {
    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void beforeMove(ClientboundPlayerPositionPacket packet, CallbackInfo callback) {
        ArboretumTransferProbe.beforePacket();
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void afterMove(ClientboundPlayerPositionPacket packet, CallbackInfo callback) {
        ArboretumTransferProbe.afterPacket();
    }
}
