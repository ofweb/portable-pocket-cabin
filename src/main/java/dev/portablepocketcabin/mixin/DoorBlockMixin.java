package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DoorBlock.class)
abstract class DoorBlockMixin {
	@Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$keepPortalDoorClosed(
		Entity entity, Level level, BlockState state, BlockPos pos, boolean open, CallbackInfo callback
	) {
		if (level instanceof ServerLevel serverLevel && CabinProtection.isProtected(serverLevel, pos)) {
			callback.cancel();
		}
	}

	@Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$ignorePortalDoorRedstone(
		BlockState state, Level level, BlockPos pos, Block neighbor,
		Orientation orientation, boolean movedByPiston, CallbackInfo callback
	) {
		if (level instanceof ServerLevel serverLevel && CabinProtection.isProtected(serverLevel, pos)) {
			callback.cancel();
		}
	}
}
