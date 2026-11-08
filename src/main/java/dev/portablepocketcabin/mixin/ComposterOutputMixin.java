package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.RichSoilAccess;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.objectweb.asm.Opcodes;

/** B-0054: a hopper takes compost, not bone meal, from a full composter. */
@Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$OutputContainer")
abstract class ComposterOutputMixin {
	@Redirect(method = "canTakeItemThroughFace",
		at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;BONE_MEAL:Lnet/minecraft/world/item/Item;", opcode = Opcodes.GETSTATIC))
	private Item portablePocketCabin$compost() {
		return RichSoilAccess.compost();
	}
}
