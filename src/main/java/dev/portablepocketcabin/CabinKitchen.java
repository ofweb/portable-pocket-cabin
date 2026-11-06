package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The kitchen: an empty 5x5 room south of the east corridor, lit by two hanging lanterns. The household places
 * its own cooking stations there, and stations inside it draw on central storage.
 */
final class CabinKitchen {
	static final Identifier TYPE = PortablePocketCabin.id("kitchen");
	/** Lantern spots on the 5x5 floor, as x and z from its north-west corner. */
	private static final List<int[]> LAMPS = List.of(new int[] {1, 1}, new int[] {3, 3});

	private CabinKitchen() {
	}

	/** The installed room, or where it would go: shell x s/2+3..s/2+9, y 0..4, z 2..8, entrance in the north wall. */
	static CabinRoomSpace space(CabinRecord cabin) {
		return room(cabin).orElseGet(() -> {
			int x = cabin.progression().generalSize() / 2;
			return new CabinRoomSpace(CabinCorridor.EAST, new BlockPos(x + 3, 0, 2),
				new BlockPos(x + 9, 4, 8), new BlockPos(x + 6, 1, 2));
		});
	}

	private static java.util.Optional<CabinRoomSpace> room(CabinRecord cabin) {
		return cabin.progression().rooms().stream().filter(room -> room.type().equals(TYPE))
			.findFirst().flatMap(CabinRoom::space);
	}

	static boolean installed(CabinRecord cabin) {
		return cabin.upgrades().kitchen().level() > 0 && room(cabin).isPresent();
	}

	/** Whether the position is inside the kitchen's floor and clearance, where stations use central storage. */
	static boolean contains(CabinRecord cabin, BlockPos pos) {
		var space = room(cabin).orElse(null);
		return space != null && cabin.upgrades().kitchen().level() > 0
			&& space.contains(cabin.cellIndex(), pos) && !space.shell(cabin.cellIndex(), pos);
	}

	/** The hanging lanterns the kitchen comes with. */
	static Map<BlockPos, BlockState> lamps(CabinRecord cabin) {
		BlockPos corner = PocketDimension.cellCenter(cabin.cellIndex()).offset(space(cabin).minimum()).offset(1, 3, 1);
		var result = new LinkedHashMap<BlockPos, BlockState>();
		for (int[] lamp : LAMPS) {
			result.put(corner.offset(lamp[0], 0, lamp[1]), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		}
		return result;
	}

	static CabinUpgradeService.Outcome validate(ServerLevel pocket, CabinRecord cabin) {
		if (cabin.upgrades().kitchen().level() != 0) return CabinUpgradeService.Outcome.failure("The kitchen is already installed.");
		if (cabin.progression().generalSize() < 5) return CabinUpgradeService.Outcome.failure("Expand the main room to 5x5 first.");
		var corridor = CabinCorridors.validateInstall(pocket, cabin, CabinCorridor.EAST);
		if (!corridor.success()) return corridor;
		var volume = space(cabin).volume(cabin.cellIndex());
		for (CabinRoom room : cabin.progression().rooms()) {
			if (room.space().isPresent() && !Collections.disjoint(volume, room.space().get().volume(cabin.cellIndex()))) {
				return CabinUpgradeService.Outcome.failure("The kitchen overlaps an installed room.");
			}
		}
		var shared = CabinCorridorLayout.blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), CabinCorridor.EAST);
		for (BlockPos pos : volume) {
			BlockState state = pocket.getBlockState(pos);
			if (!state.isAir() && !state.equals(shared.get(pos))) {
				return CabinUpgradeService.Outcome.failure("Kitchen installation is obstructed at " + pos.toShortString());
			}
		}
		return CabinUpgradeService.Outcome.success("");
	}

	/** While the kitchen installs, its room and a newly added corridor pause like other installing rooms. */
	static boolean paused(CabinRecord cabin, BlockPos pos) {
		if (cabin.upgrades().installation().filter(installation -> installation.target().isKitchen()).isEmpty()) return false;
		return space(cabin).volume(cabin.cellIndex()).contains(pos)
			|| !cabin.progression().corridors().contains(CabinCorridor.EAST) && CabinCorridorLayout.volume(cabin.cellIndex(),
				cabin.progression().generalSize(), CabinCorridor.EAST).contains(pos);
	}

	/** Builds the corridor if needed, the shell, the entrance, and the lamps. Every step is a plain placement, so recovery can repeat it. */
	static void install(ServerLevel pocket, CabinRecord cabin) {
		CabinCorridors.applyWorldEffect(() -> {
			if (!cabin.progression().corridors().contains(CabinCorridor.EAST)) {
				CabinCorridorLayout.blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), CabinCorridor.EAST)
					.forEach((pos, state) -> pocket.setBlock(pos, state, CabinCorridorSnapshot.FLAGS));
			}
			var space = space(cabin);
			BlockPos center = PocketDimension.cellCenter(cabin.cellIndex());
			for (BlockPos pos : space.volume(cabin.cellIndex())) {
				if (!space.shell(cabin.cellIndex(), pos)) continue;
				int y = pos.getY() - center.getY();
				Block block = y == 0 ? cabin.palette().floor().planksBlock()
					: y == 4 ? cabin.palette().roof().planksBlock() : cabin.palette().walls().planksBlock();
				pocket.setBlock(pos, block.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
			}
			BlockPos entrance = center.offset(space.entrance());
			pocket.setBlock(entrance, Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
			pocket.setBlock(entrance.above(), Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
			lamps(cabin).forEach((pos, state) -> pocket.setBlock(pos, state, CabinCorridorSnapshot.FLAGS));
		});
	}
}
