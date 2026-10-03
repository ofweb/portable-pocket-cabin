package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinStation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
abstract class CabinStationMenuMixin {
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void cabin$validate(int slot, int button, ContainerInput input, Player player, CallbackInfo ci) {
		if (!CabinStation.beforeClick((AbstractContainerMenu)(Object)this, player)
			|| !CabinStation.mayResultClick((AbstractContainerMenu)(Object)this, slot, input)
			|| CabinStation.shiftClick((AbstractContainerMenu)(Object)this, slot, button, input, player)) ci.cancel();
	}
	@Inject(method = "clicked", at = @At("RETURN"))
	private void cabin$receipt(int slot, int button, ContainerInput input, Player player, CallbackInfo ci) {
		CabinStation.afterClick((AbstractContainerMenu)(Object)this, slot);
	}
	@Inject(method = "removed", at = @At("HEAD"))
	private void cabin$returnInputs(Player player, CallbackInfo ci) { CabinStation.close((AbstractContainerMenu)(Object)this); }
}
