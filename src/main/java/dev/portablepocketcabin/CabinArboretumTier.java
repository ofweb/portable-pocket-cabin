package dev.portablepocketcabin;

import java.util.OptionalInt;

/** B-0023 purchase tiers; the woodland tier keeps the largest cultivated floor. */
enum CabinArboretumTier {
	INITIAL(7, 5, 1),
	EXPANDED(19, 10, 2),
	LARGE(29, 15, 3),
	WOODLANDS(29, 15, 4);

	private final int floorSize;
	private final int entranceWallHeight;
	private final int selectedTreeLimit;

	CabinArboretumTier(int floorSize, int entranceWallHeight, int selectedTreeLimit) {
		this.floorSize = floorSize;
		this.entranceWallHeight = entranceWallHeight;
		this.selectedTreeLimit = selectedTreeLimit;
	}

	int floorSize() { return floorSize; }
	int entranceWallHeight() { return entranceWallHeight; }
	int selectedTreeLimit() { return selectedTreeLimit; }

	OptionalInt enclosedClearHeight() {
		return this == WOODLANDS ? OptionalInt.empty() : OptionalInt.of(entranceWallHeight);
	}
}
