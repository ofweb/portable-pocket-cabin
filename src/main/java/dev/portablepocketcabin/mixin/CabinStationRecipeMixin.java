package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinStation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCraftingMenu.class)
abstract class CabinStationRecipeMixin {
	@Inject(method = "handlePlacement", at = @At("HEAD"), cancellable = true)
	private void cabin$fill(boolean all, boolean creative, RecipeHolder<?> recipe, ServerLevel level, Inventory inventory,
		CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> ci) {
		if (CabinStation.place((AbstractContainerMenu)(Object)this, recipe)) ci.setReturnValue(RecipeBookMenu.PostPlaceAction.NOTHING);
	}
}
