package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinCorridors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class CorridorMovementMixin {
	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$closeMovingPassages(MoverType type, Vec3 movement, CallbackInfo callback) {
		Entity entity = (Entity) (Object) this;
		if (entity.level() instanceof ServerLevel level && CabinCorridors.isPaused(level,
			BlockPos.containing(entity.position().add(movement)))) callback.cancel();
	}
}
