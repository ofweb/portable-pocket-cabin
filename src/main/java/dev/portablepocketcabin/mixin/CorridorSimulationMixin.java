package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinCorridors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class CorridorSimulationMixin {
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$pauseMovingEntity(Entity entity, CallbackInfo callback) {
		if (CabinCorridors.isPaused((ServerLevel) (Object) this, entity.blockPosition())) callback.cancel();
	}

	@Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$pauseMovingPassenger(Entity vehicle, Entity passenger, CallbackInfo callback) {
		if (CabinCorridors.isPaused((ServerLevel) (Object) this, passenger.blockPosition())) callback.cancel();
	}

	@Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$pauseMovingBlock(BlockPos pos, Block block, CallbackInfo callback) {
		ServerLevel level = (ServerLevel) (Object) this;
		if (CabinCorridors.isPaused(level, pos)) { level.scheduleTick(pos, block, 1); callback.cancel(); }
	}

	@Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$pauseMovingFluid(BlockPos pos, Fluid fluid, CallbackInfo callback) {
		ServerLevel level = (ServerLevel) (Object) this;
		if (CabinCorridors.isPaused(level, pos)) { level.scheduleTick(pos, fluid, 1); callback.cancel(); }
	}
}
