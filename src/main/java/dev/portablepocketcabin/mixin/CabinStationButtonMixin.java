package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinStation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractContainerMenu.class, LoomMenu.class, StonecutterMenu.class})
abstract class CabinStationButtonMixin {
	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void cabin$select(Player player, int button, CallbackInfoReturnable<Boolean> ci) {
		if (CabinStation.button((AbstractContainerMenu)(Object)this, player, button)) ci.setReturnValue(true);
		else if (!CabinStation.beforeClick((AbstractContainerMenu)(Object)this, player)) ci.setReturnValue(false);
	}
}
