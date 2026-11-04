package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinCorridors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class GuestItemPickupMixin {
	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void portablePocketCabin$preventGuestPickup(Player player, CallbackInfo callback) {
		var item = (ItemEntity) (Object) this;
		if (item.level() instanceof ServerLevel serverLevel
			&& !CabinCorridors.mayChange(serverLevel, player.getUUID(), item.blockPosition())) callback.cancel();
	}
}
