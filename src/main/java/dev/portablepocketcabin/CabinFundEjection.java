package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** Physical broken-container-style output for an owner-confirmed fund cancellation. */
final class CabinFundEjection {
	private CabinFundEjection() {
	}

	static boolean eject(ServerLevel level, CabinRecord cabin, List<ItemStack> stacks) {
		if (!level.dimension().equals(PocketDimension.LEVEL_KEY)) {
			return false;
		}
		var controller = PocketDimension.interiorController(cabin.cellIndex());
		if (!level.getBlockState(controller).is(Blocks.LODESTONE)) {
			return false;
		}
		var drop = dropPosition(cabin);
		List<ItemEntity> spawned = new ArrayList<>();
		for (ItemStack stack : stacks) {
			ItemEntity entity = new ItemEntity(
				level, drop.getX() + 0.5, drop.getY() + 0.25, drop.getZ() + 0.5, stack.copy()
			);
			entity.setDefaultPickUpDelay();
			if (!level.addFreshEntity(entity)) {
				spawned.forEach(ItemEntity::discard);
				return false;
			}
			spawned.add(entity);
		}
		return true;
	}

	static BlockPos dropPosition(CabinRecord cabin) {
		return PocketDimension.interiorController(cabin.cellIndex()).relative(Direction.NORTH);
	}
}
