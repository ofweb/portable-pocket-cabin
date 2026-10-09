package dev.portablepocketcabin.transfer.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep test windows hidden; captures still use Minecraft's real GPU renderer. */
@Mixin(Minecraft.class)
abstract class ArboretumHiddenWindowMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwShowWindow(J)V"))
    private void keepTestWindowHidden(long window) { }
}
