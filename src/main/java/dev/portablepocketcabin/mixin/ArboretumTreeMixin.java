package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinArboretum;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Trees in an arboretum grove grow only inside it (B-0023). Without this, branching trees such as cherry would reach
 * through the walls into the empty dimension outside, and block the grove from growing bigger.
 */
@Mixin(TreeFeature.class)
abstract class ArboretumTreeMixin {
	@Inject(method = "validTreePos", at = @At("HEAD"), cancellable = true)
	private static void portablePocketCabin$keepTreesInTheGrove(LevelSimulatedReader level, BlockPos pos, CallbackInfoReturnable<Boolean> result) {
		if (level instanceof ServerLevel server && CabinArboretum.isArboretum(server) && !CabinArboretum.insideGrove(server.getServer(), pos))
			result.setReturnValue(false);
	}
}
