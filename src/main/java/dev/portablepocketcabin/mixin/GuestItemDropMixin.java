package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinCorridors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
abstract class GuestItemDropMixin {
	// Return a guest's dropped stack to inventory; death drops and any overflow still drop normally.
	@Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
		at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$keepGuestDrops(ItemStack stack, boolean randomly, boolean thrownFromHand,
		CallbackInfoReturnable<ItemEntity> callback) {
		var player = (ServerPlayer) (Object) this;
		if (stack.isEmpty() || player.isDeadOrDying()
			|| CabinCorridors.mayChange(player.level(), player.getUUID(), player.blockPosition())) return;
		player.getInventory().add(stack);
		player.containerMenu.broadcastChanges();
		player.sendSystemMessage(Component.literal("Guests cannot drop items in this cabin."), true);
		if (stack.isEmpty()) callback.setReturnValue(null);
	}
}
