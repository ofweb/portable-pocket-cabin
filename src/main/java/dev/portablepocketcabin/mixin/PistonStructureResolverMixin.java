package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PistonStructureResolver.class)
abstract class PistonStructureResolverMixin {
	@Shadow
	@Final
	private Level level;

	@Shadow
	@Final
	private List<BlockPos> toPush;

	@Shadow
	@Final
	private List<BlockPos> toDestroy;

	@Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
	private void portablePocketCabin$preventCabinMovement(CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValue() || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (toPush.stream().anyMatch(pos -> CabinProtection.isProtected(serverLevel, pos))
			|| toDestroy.stream().anyMatch(pos -> CabinProtection.isProtected(serverLevel, pos))) {
			callback.setReturnValue(false);
		}
	}
}
