package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.RichSoilAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ComposterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.objectweb.asm.Opcodes;

/** B-0054: a full composter gives compost instead of bone meal, by hand and to a hopper. */
@Mixin(ComposterBlock.class)
abstract class ComposterBlockMixin {
	@Redirect(method = {"extractProduce", "getContainer"},
		at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;BONE_MEAL:Lnet/minecraft/world/item/Item;", opcode = Opcodes.GETSTATIC))
	private static Item portablePocketCabin$compost() {
		return RichSoilAccess.compost();
	}
}
