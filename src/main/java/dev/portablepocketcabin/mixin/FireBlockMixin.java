package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
abstract class FireBlockMixin {
	@Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$preventCabinBurn(
		Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo callback
	) {
		if (level instanceof ServerLevel serverLevel && CabinProtection.isProtected(serverLevel, pos)) {
			callback.cancel();
		}
	}
}
