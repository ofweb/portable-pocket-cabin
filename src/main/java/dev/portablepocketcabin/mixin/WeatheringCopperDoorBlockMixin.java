package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.WeatheringCopperDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatheringCopperDoorBlock.class)
abstract class WeatheringCopperDoorBlockMixin {
	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$preventPortalDoorOxidation(
		BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback
	) {
		if (CabinProtection.isProtected(level, pos)) {
			callback.cancel();
		}
	}
}
