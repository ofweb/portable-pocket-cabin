package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinCorridors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
abstract class CorridorLevelMixin {
	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$keepMovingContents(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> callback) {
		if ((Object) this instanceof ServerLevel level && !CabinCorridors.applyingWorldEffect()
			&& (flags & Block.UPDATE_SKIP_ALL_SIDEEFFECTS) == 0 && CabinCorridors.isPaused(level, pos)) callback.setReturnValue(false);
	}

	@Inject(method = "getBlockEntity", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$isolateMovingInventories(BlockPos pos, CallbackInfoReturnable<BlockEntity> callback) {
		if ((Object) this instanceof ServerLevel level && !CabinCorridors.applyingWorldEffect()
			&& CabinCorridors.isPaused(level, pos)) callback.setReturnValue(null);
	}
}
