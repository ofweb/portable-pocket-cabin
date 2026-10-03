package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import java.util.*;

final class CabinGreenhouse {
	static final Identifier TYPE = PortablePocketCabin.id("greenhouse_room");
	static final int[] WIDTHS = {0, 5, 7, 9, 11};
	static final int[] LENGTHS = {0, 8, 11, 20, 29};
	static final int[] HEIGHTS = {0, 4, 5, 6, 7};
	private CabinGreenhouse() { }

	static CabinRoomSpace space(CabinRecord cabin, int level) {
		int x = cabin.progression().rooms().stream().filter(room -> room.type().equals(TYPE))
			.flatMap(room -> room.space().stream()).mapToInt(room -> room.entrance().getX())
			.findFirst().orElse(-cabin.progression().generalSize() / 2 - 9);
		int half = WIDTHS[level] / 2;
		return new CabinRoomSpace(CabinCorridor.WEST, new BlockPos(x - half - 1, -1, 2),
			new BlockPos(x + half + 1, HEIGHTS[level] + 1, LENGTHS[level] + 3), new BlockPos(x, 1, 2));
	}

	static Map<BlockPos, BlockState> defaults(CabinRecord cabin, int level) {
		var space = space(cabin, level);
		var result = new LinkedHashMap<BlockPos, BlockState>();
		BlockPos center = PocketDimension.cellCenter(cabin.cellIndex());
		for (BlockPos relative : BlockPos.betweenClosed(space.minimum(), space.maximum())) {
			BlockPos pos = center.offset(relative);
			if (space.shell(cabin.cellIndex(), pos)) {
				result.put(pos, (relative.getY() == -1 ? cabin.palette().floor().planksBlock()
					: relative.getY() == 1 ? Blocks.SEA_LANTERN : Blocks.GLASS).defaultBlockState());
			} else if (relative.getY() == 0 && relative.getZ() > 2) {
				boolean path = relative.getX() == space.entrance().getX() || (relative.getZ() - 3) % 3 == 2;
				result.put(pos, path ? Blocks.STONE_BRICK_SLAB.defaultBlockState()
					.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP).setValue(BlockStateProperties.WATERLOGGED, true)
					: Blocks.FARMLAND.defaultBlockState());
			}
		}
		result.put(center.offset(space.entrance()), Blocks.AIR.defaultBlockState());
		result.put(center.offset(space.entrance()).above(), Blocks.AIR.defaultBlockState());
		return result;
	}

	static CabinUpgradeService.Outcome validate(ServerLevel pocket, CabinRecord cabin, int target) {
		if (CabinGreenhouseGrowth.hasPending(pocket.getServer(), cabin.uuid()))
			return CabinUpgradeService.Outcome.failure("Greenhouse catch-up is in progress.");
		int current = cabin.upgrades().greenhouse().level();
		if (target != current + 1) return CabinUpgradeService.Outcome.failure("Greenhouse requires the previous size.");
		var corridor = CabinCorridors.validateInstall(pocket, cabin, CabinCorridor.WEST);
		if (!corridor.success()) return corridor;
		var reserved = space(cabin, 4).volume(cabin.cellIndex());
		for (var room : cabin.progression().rooms())
			if (!room.type().equals(TYPE) && room.space().isPresent()
				&& !Collections.disjoint(reserved, room.space().get().volume(cabin.cellIndex())))
				return CabinUpgradeService.Outcome.failure("Greenhouse reservation overlaps an installed room.");
		var old = current == 0 ? Set.<BlockPos>of() : space(cabin, current).volume(cabin.cellIndex());
		var shared = CabinCorridorLayout.blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), CabinCorridor.WEST);
		for (var pos : space(cabin, target).volume(cabin.cellIndex()))
			if (!old.contains(pos) && !pocket.getBlockState(pos).isAir() && !pocket.getBlockState(pos).equals(shared.get(pos)))
				return CabinUpgradeService.Outcome.failure("Greenhouse growth is obstructed at " + pos.toShortString());
		return CabinUpgradeService.Outcome.success("");
	}

	static boolean paused(CabinRecord cabin, BlockPos pos) {
		if (cabin.lifecycle() == CabinLifecycle.PACKED && cabin.upgrades().greenhouse().level() > 0
			&& space(cabin, cabin.upgrades().greenhouse().level()).contains(cabin.cellIndex(), pos)) return true;
		var installation = cabin.upgrades().installation().filter(i -> i.target().isGreenhouse());
		return installation.isPresent() && (space(cabin, installation.get().targetState()).contains(cabin.cellIndex(), pos)
			|| installation.get().expectedState() == 0 && CabinCorridorLayout.volume(cabin.cellIndex(),
				cabin.progression().generalSize(), CabinCorridor.WEST).contains(pos));
	}

	static void install(ServerLevel pocket, CabinRecord cabin, CabinUpgradeState.Installation installation) {
		CabinCorridors.applyWorldEffect(() -> {
			if (!cabin.progression().corridors().contains(CabinCorridor.WEST))
				CabinCorridorLayout.blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), CabinCorridor.WEST)
					.forEach((pos, state) -> pocket.setBlock(pos, state, CabinCorridorSnapshot.FLAGS));
			var added = defaults(cabin, installation.targetState());
			if (installation.expectedState() > 0) {
				var oldSpace = space(cabin, installation.expectedState());
				for (var pos : oldSpace.volume(cabin.cellIndex())) {
					if (!oldSpace.shell(cabin.cellIndex(), pos)) { added.remove(pos); continue; }
					// Old boundary blocks become new beds or open space. Interior contents stay in place.
					if (!added.containsKey(pos)) pocket.setBlock(pos, Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
				}
			}
			added.forEach((pos, state) -> pocket.setBlock(pos, state, CabinCorridorSnapshot.FLAGS));
		});
		pocket.getServer().saveEverything(false, true, false);
	}
}
