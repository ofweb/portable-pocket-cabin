package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinStation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CraftingMenu.class, LoomMenu.class, CartographyTableMenu.class, StonecutterMenu.class, ItemCombinerMenu.class})
abstract class CabinStationQuickMoveMixin {
	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void cabin$limit(Player player, int slot, CallbackInfoReturnable<ItemStack> ci) {
		if (!CabinStation.mayQuickMove((AbstractContainerMenu)(Object)this, slot)) ci.setReturnValue(ItemStack.EMPTY);
	}
	@Inject(method = "quickMoveStack", at = @At("RETURN"))
	private void cabin$count(Player player, int slot, CallbackInfoReturnable<ItemStack> ci) {
		CabinStation.didQuickMove((AbstractContainerMenu)(Object)this, slot, ci.getReturnValue());
	}
}
