package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinStationPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LoomScreen.class, CartographyTableScreen.class, SmithingScreen.class, StonecutterScreen.class})
abstract class CabinStationBackgroundMixin {
	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void cabin$overlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if ((Object)this instanceof CabinStationPanel panel && panel.cabin$panelOnly()) {
			((Screen)(Object)this).extractTransparentBackground(graphics);
			ci.cancel();
		}
	}
}
