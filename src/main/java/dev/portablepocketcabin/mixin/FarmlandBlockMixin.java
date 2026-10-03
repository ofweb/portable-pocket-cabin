package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinCorridors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
abstract class FarmlandBlockMixin {
	@Inject(method = "turnToDirt", at = @At("HEAD"), cancellable = true)
	private static void portablePocketCabin$preventGuestTrampling(Entity entity, BlockState state, Level level, BlockPos pos, CallbackInfo callback) {
		if (entity instanceof Player player && level instanceof ServerLevel serverLevel
			&& !CabinCorridors.mayChange(serverLevel, player.getUUID(), pos)) callback.cancel();
	}
}
