package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "interactWithBlocks", at = @At("HEAD"))
	private void portablePocketCabin$protectCabinBlocks(List<BlockPos> positions, CallbackInfo callback) {
		positions.removeIf(pos -> CabinProtection.isProtected(level, pos));
	}
}
