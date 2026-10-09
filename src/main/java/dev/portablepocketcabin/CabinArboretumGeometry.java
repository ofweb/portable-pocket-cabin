package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;

/** Core coordinates stay fixed in the cabin's own arboretum world. */
final class CabinArboretumGeometry {
	static final int FLOOR_Y = 63;
	static final int TRANSITION_SIZE = 51;
	static final int FIRST_BIOME_MARKER_SIZE = 39;
	static final int WILDLIFE_MARKER_SIZE = 45;

	enum Zone { CULTIVATED_CORE, TRANSITION, WOODLAND }

	private CabinArboretumGeometry() { }

	static BlockPos cabinDoor(CabinRecord cabin) {
		return PocketDimension.cellCenter(cabin.cellIndex())
			.offset(-cabin.progression().generalSize() / 2 - 28, 1, 2);
	}

	static BlockPos arboretumDoor() { return new BlockPos(0, FLOOR_Y + 1, -1); }

	/** Returns sapling anchors, with one usable floor block before enclosed walls. */
	static List<BlockPos> plantingGrid(CabinArboretumTier tier) {
		int half = tier.floorSize() / 2;
		var positions = new ArrayList<BlockPos>();
		for (int x = -14; x <= 14; x += 4) {
			for (int z = 1; z < tier.floorSize() - 1; z += 4) {
				if (x > -half && x < half) positions.add(new BlockPos(x, FLOOR_Y + 1, z));
			}
		}
		return List.copyOf(positions);
	}

	/** Permanent cultivation covers the largest core, including construction above it. */
	static Zone zone(BlockPos position) {
		if (withinSquare(position, 29)) return Zone.CULTIVATED_CORE;
		if (withinSquare(position, TRANSITION_SIZE)) return Zone.TRANSITION;
		return Zone.WOODLAND;
	}

	private static boolean withinSquare(BlockPos position, int size) {
		int half = size / 2;
		return Math.abs((long) position.getX()) <= half
			&& Math.abs((long) position.getZ() - 14) <= half;
	}
}
