package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Physical broken-container-style output for an owner-confirmed fund cancellation. */
final class CabinFundEjection {
	private CabinFundEjection() {
	}

	static boolean eject(ServerLevel level, CabinRecord cabin, List<ItemStack> stacks) {
		if (!level.dimension().equals(PocketDimension.LEVEL_KEY)) {
			return false;
		}
		var controller = PocketDimension.interiorController(cabin.cellIndex(), cabin.progression().generalSize());
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

	static boolean ejectMarked(
		ServerLevel level, CabinRecord cabin, UUID operationId, List<ItemStack> stacks
	) {
		if (!level.dimension().equals(PocketDimension.LEVEL_KEY)) {
			return false;
		}
		return ejectMarkedInCabinLevel(level, cabin, operationId, stacks);
	}

	/** Performs marked ejection after the caller has established that this is the cabin level. */
	static boolean ejectMarkedInCabinLevel(
		ServerLevel level, CabinRecord cabin, UUID operationId, List<ItemStack> stacks
	) {
		var controller = PocketDimension.interiorController(cabin.cellIndex(), cabin.progression().generalSize());
		if (!level.getBlockState(controller).is(Blocks.LODESTONE)) {
			return false;
		}
		BlockPos drop = dropPosition(cabin);
		level.getChunkAt(drop);
		Map<Integer, ItemEntity> existing = new HashMap<>();
		String operationPrefix = markerPrefix(operationId);
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(drop).inflate(2.0))) {
			for (String tag : entity.entityTags()) {
				if (!tag.startsWith(operationPrefix)) {
					continue;
				}
				int index;
				try {
					index = Integer.parseInt(tag.substring(operationPrefix.length()));
				} catch (NumberFormatException exception) {
					throw new IllegalStateException("Refund entity has an invalid payload index", exception);
				}
				if (index < 0 || index >= stacks.size()) {
					throw new IllegalStateException("Refund entity payload index " + index + " is out of range");
				}
				if (existing.putIfAbsent(index, entity) != null
					|| !ItemStack.matches(stacks.get(index), entity.getItem())) {
					throw new IllegalStateException(
						"Refund entity payload index " + index + " is duplicated or does not match"
					);
				}
			}
		}

		List<ItemEntity> spawned = new ArrayList<>();
		for (int index = 0; index < stacks.size(); index++) {
			if (existing.containsKey(index)) {
				continue;
			}
			ItemEntity entity = new ItemEntity(
				level, drop.getX() + 0.5, drop.getY() + 0.25, drop.getZ() + 0.5, stacks.get(index).copy()
			);
			entity.addTag(marker(operationId, index));
			entity.setDefaultPickUpDelay();
			if (!level.addFreshEntity(entity)) {
				spawned.forEach(ItemEntity::discard);
				return false;
			}
			spawned.add(entity);
		}
		return true;
	}

	private static String marker(UUID operationId, int index) {
		return markerPrefix(operationId) + index;
	}

	private static String markerPrefix(UUID operationId) {
		return PortablePocketCabin.MOD_ID + ".refund." + operationId + ".";
	}

	static BlockPos dropPosition(CabinRecord cabin) {
		return PocketDimension.interiorController(cabin.cellIndex(), cabin.progression().generalSize()).relative(Direction.NORTH);
	}
}
