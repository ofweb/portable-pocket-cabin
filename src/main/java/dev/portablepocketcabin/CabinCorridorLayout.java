package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Fixed complete floors from experiments/room-layout, relative to the main-room walls. */
final class CabinCorridorLayout {
	private CabinCorridorLayout() { }

	private record Key(long cell, int size, CabinPalette palette, CabinCorridor corridor) { }
	private static final Map<Key, Map<BlockPos, BlockState>> SHELLS = new LinkedHashMap<>() {
		@Override protected boolean removeEldestEntry(Map.Entry<Key, Map<BlockPos, BlockState>> entry) { return size() > 256; }
	};
	private static final Map<Key, Set<BlockPos>> VOLUMES = new LinkedHashMap<>() {
		@Override protected boolean removeEldestEntry(Map.Entry<Key, Set<BlockPos>> entry) { return size() > 256; }
	};

	static Set<BlockPos> floor(long cell, int size, CabinCorridor corridor) {
		if (size < 5 || !CabinProgression.isSupportedGeneralSize(size)) {
			throw new IllegalArgumentException("Corridors require supported general space of at least 5x5");
		}
		int r = size / 2;
		var floor = new LinkedHashSet<BlockPos>();
		BlockPos center = PocketDimension.cellCenter(cell);
		switch (corridor) {
			case NORTH -> rectangle(floor, center, -1, 1, -r - 17, -r - 1);
			case EAST -> rectangle(floor, center, r + 1, r + 8, -1, 1);
			case WEST -> {
				rectangle(floor, center, -r - 37, -r - 1, -1, 1);
				rectangle(floor, center, -r - 37, -r - 35, -30, -2);
			}
		}
		return Set.copyOf(floor);
	}

	private static void rectangle(Set<BlockPos> result, BlockPos center, int minX, int maxX, int minZ, int maxZ) {
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) result.add(center.offset(x, 0, z));
		}
	}

	static BlockPos entrance(long cell, int size, CabinCorridor corridor) {
		return PocketDimension.cellCenter(cell).offset(corridor.expansionOffset().multiply(size / 2 + 1)).above();
	}

	/** Generated shell and the two air blocks that open the main-room entrance. */
	static synchronized Map<BlockPos, BlockState> blocks(long cell, int size, CabinPalette palette, CabinCorridor corridor) {
		return SHELLS.computeIfAbsent(new Key(cell, size, palette, corridor), key -> buildBlocks(cell, size, palette, corridor));
	}

	private static Map<BlockPos, BlockState> buildBlocks(long cell, int size, CabinPalette palette, CabinCorridor corridor) {
		Set<BlockPos> floor = floor(cell, size, corridor);
		var result = new LinkedHashMap<BlockPos, BlockState>();
		BlockPos entrance = entrance(cell, size, corridor);
		var mainShell = PocketDimension.shellBlocks(cell, size, palette);
		for (BlockPos pos : floor) {
			result.put(pos, palette.floor().planksBlock().defaultBlockState());
			result.put(pos.above(4), palette.roof().planksBlock().defaultBlockState());
			for (Direction side : Direction.Plane.HORIZONTAL) {
				BlockPos edge = pos.relative(side);
				if (floor.contains(edge)) continue;
				result.put(edge, palette.floor().planksBlock().defaultBlockState());
				result.put(edge.above(4), palette.roof().planksBlock().defaultBlockState());
				for (int y = 1; y <= 3; y++) result.put(edge.above(y), palette.walls().planksBlock().defaultBlockState());
			}
			// The shared main-room wall keeps a 1x2 opening rather than a 3x3 gap.
			for (int y = 1; y <= 4; y++) {
				BlockPos wall = pos.above(y);
				if (mainShell.containsKey(wall)) result.put(wall, mainShell.get(wall));
			}
		}
		BlockPos center = PocketDimension.cellCenter(cell);
		int radius = size / 2;
		result.keySet().removeIf(pos -> Math.abs(pos.getX() - center.getX()) <= radius
			&& Math.abs(pos.getZ() - center.getZ()) <= radius);
		result.replaceAll((pos, state) -> mainShell.getOrDefault(pos, state));
		result.put(entrance, Blocks.AIR.defaultBlockState());
		result.put(entrance.above(), Blocks.AIR.defaultBlockState());
		return Map.copyOf(result);
	}

	static synchronized Set<BlockPos> volume(long cell, int size, CabinCorridor corridor) {
		return VOLUMES.computeIfAbsent(new Key(cell, size, CabinPalette.DEFAULT, corridor), key -> buildVolume(cell, size, corridor));
	}

	private static Set<BlockPos> buildVolume(long cell, int size, CabinCorridor corridor) {
		var result = new LinkedHashSet<>(blocks(cell, size, CabinPalette.DEFAULT, corridor).keySet());
		for (BlockPos pos : floor(cell, size, corridor)) {
			for (int y = 1; y <= 3; y++) result.add(pos.above(y));
		}
		return Set.copyOf(result);
	}

	static Set<ChunkPos> chunks(CabinRecord cabin) {
		var result = new LinkedHashSet<>(CabinSimulation.chunksForCell(cabin.cellIndex(), cabin.progression().generalSize()));
		for (CabinCorridor corridor : cabin.progression().corridors()) {
			for (BlockPos pos : volume(cabin.cellIndex(), cabin.progression().generalSize(), corridor)) {
				result.add(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
			}
		}
		for (CabinRoom room : cabin.progression().rooms()) {
			room.space().ifPresent(space -> {
				for (BlockPos pos : space.volume(cabin.cellIndex())) result.add(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
			});
		}
		return Set.copyOf(result);
	}

	static boolean isEntrance(CabinRecord cabin, BlockPos pos) {
		return cabin.progression().corridors().stream().anyMatch(corridor -> {
			BlockPos entrance = entrance(cabin.cellIndex(), cabin.progression().generalSize(), corridor);
			return pos.equals(entrance) || pos.equals(entrance.above());
		});
	}

	static boolean isShell(CabinRecord cabin, BlockPos pos) {
		BlockPos relative = pos.subtract(PocketDimension.cellCenter(cabin.cellIndex()));
		if (cabin.progression().rooms().stream().anyMatch(room -> room.space().filter(space ->
			relative.equals(space.entrance()) || relative.equals(space.entrance().above())).isPresent())) return false;
		return cabin.progression().corridors().stream().anyMatch(corridor -> {
			BlockState state = blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), corridor).get(pos);
			return state != null && !state.isAir();
		});
	}
}
