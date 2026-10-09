package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Checks the accepted geometry surface without requiring arboretum gameplay. */
public final class CabinArboretumDesignGameTest {
	@GameTest
	public void expansionPreservesPlantingAnchorsAndCorridorDoorFit(GameTestHelper helper) {
		Set<BlockPos> previous = Set.of();
		int[] counts = {4, 20, 42, 42};
		for (var tier : CabinArboretumTier.values()) {
			var grid = Set.copyOf(CabinArboretumGeometry.plantingGrid(tier));
			helper.assertTrue(grid.size() == counts[tier.ordinal()], "Tier has its agreed planting capacity");
			helper.assertTrue(grid.containsAll(previous), "Expansion keeps earlier planting anchors");
			helper.assertTrue(grid.stream().allMatch(p -> Math.abs(p.getX()) < tier.floorSize() / 2
				&& p.getZ() >= 1 && p.getZ() < tier.floorSize() - 1), "Planting leaves space before walls");
			previous = grid;
		}
		helper.assertTrue(CabinArboretumTier.WOODLANDS.enclosedClearHeight().isEmpty(), "Woodlands have no ceiling");
		for (int size = 5; size <= 21; size += 2) {
			var cabin = new CabinRecord(UUID.randomUUID(), UUID.randomUUID(), 0, CabinLifecycle.PACKED,
				Optional.empty(), Optional.empty(), true, 0, false, CabinPalette.DEFAULT,
				Optional.empty(), false, CabinEntryPermission.OWNER_ONLY, List.of(),
				new CabinProgression(size, List.of(), List.of(CabinCorridor.WEST)), CabinUpgradeState.EMPTY);
			var door = CabinArboretumGeometry.cabinDoor(cabin);
			var corridor = CabinCorridorLayout.blocks(0, size, cabin.palette(), CabinCorridor.WEST);
			helper.assertTrue(corridor.containsKey(door) && corridor.containsKey(door.above()),
				"Arboretum door occupies the west corridor wall at every cabin size");
			helper.assertTrue(CabinCorridorLayout.floor(0, size, CabinCorridor.WEST).contains(door.north().below()),
				"The cabin-side door has an adjacent corridor arrival floor");
		}
		helper.assertTrue(CabinArboretumGeometry.zone(new BlockPos(14, 100, 28))
			== CabinArboretumGeometry.Zone.CULTIVATED_CORE, "Core construction remains cultivated above tree height");
		helper.assertTrue(CabinArboretumGeometry.zone(new BlockPos(15, 64, 28))
			== CabinArboretumGeometry.Zone.TRANSITION, "Transition begins outside the permanent core");
		helper.assertTrue(CabinArboretumGeometry.zone(new BlockPos(26, 64, 14))
			== CabinArboretumGeometry.Zone.WOODLAND, "Woodland begins beyond the 51-block transition");
		helper.succeed();
	}
}
