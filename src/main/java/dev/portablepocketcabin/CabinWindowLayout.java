package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Pure, deterministic placement for purchased windows on the three eligible interior walls. */
final class CabinWindowLayout {
	private static final List<Dimensions> TIERS = List.of(
		new Dimensions(1, 2),
		new Dimensions(2, 2),
		new Dimensions(3, 3),
		new Dimensions(5, 4),
		new Dimensions(7, 6),
		new Dimensions(9, 8)
	);

	record Dimensions(int width, int height) {
	}

	record PlannedTier(CabinWindowState.Identity identity, int tier) {
		PlannedTier {
			Objects.requireNonNull(identity, "identity");
			if (tier < 1 || tier > CabinWindowState.MAX_TIER) {
				throw new IllegalArgumentException("Planned window tier is outside the supported range");
			}
		}
	}

	record Footprint(CabinWindowState.Identity identity, int tier, Set<BlockPos> positions) {
		Footprint {
			positions = Set.copyOf(positions);
		}
	}

	record Result(boolean valid, String message, List<Footprint> windows) {
		Result {
			windows = List.copyOf(windows);
		}

		Set<BlockPos> positions() {
			Set<BlockPos> result = new LinkedHashSet<>();
			for (Footprint window : windows) {
				result.addAll(window.positions());
			}
			return Set.copyOf(result);
		}
	}

	private CabinWindowLayout() {
	}

	static Dimensions dimensions(int tier) {
		if (tier < 1 || tier > TIERS.size()) {
			throw new IllegalArgumentException("Cabin window tier is outside the supported range");
		}
		return TIERS.get(tier - 1);
	}

	static Result current(long cellIndex, int generalSize, CabinWindowState state) {
		return derive(cellIndex, generalSize, state, Optional.empty());
	}

	static Result installing(
		long cellIndex,
		int generalSize,
		CabinWindowState state,
		CabinWindowState.Identity identity,
		int targetTier
	) {
		int currentTier = state.tier(identity);
		if (targetTier != currentTier + 1) {
			return new Result(false, "Cabin windows advance exactly one tier at a time", List.of());
		}
		if (identity.slot() == 1
			&& currentTier == 0
			&& state.tier(new CabinWindowState.Identity(identity.wall(), 0)) == 0) {
			return new Result(false, "Install the first window on this wall before the second", List.of());
		}
		return derive(cellIndex, generalSize, state, Optional.of(new PlannedTier(identity, targetTier)));
	}

	private static Result derive(
		long cellIndex,
		int generalSize,
		CabinWindowState state,
		Optional<PlannedTier> planned
	) {
		Map<CabinWindowState.Identity, Integer> tiers = new LinkedHashMap<>();
		for (CabinWindowState.Window window : state.windows()) {
			tiers.put(window.identity(), window.tier());
		}
		planned.ifPresent(value -> tiers.put(value.identity(), value.tier()));

		List<Footprint> result = new ArrayList<>();
		for (CabinWindowState.Wall wall : CabinWindowState.Wall.values()) {
			List<Map.Entry<CabinWindowState.Identity, Integer>> wallWindows = tiers.entrySet().stream()
				.filter(entry -> entry.getKey().wall() == wall)
				.sorted(Comparator.comparingInt(entry -> entry.getKey().slot()))
				.toList();
			if (wallWindows.isEmpty()) {
				continue;
			}
			int totalWidth = wallWindows.stream()
				.mapToInt(entry -> dimensions(entry.getValue()).width())
				.sum() + wallWindows.size() - 1;
			if (totalWidth > generalSize) {
				return invalidSize(generalSize, wall, totalWidth);
			}
			for (var entry : wallWindows) {
				Dimensions dimensions = dimensions(entry.getValue());
				if (dimensions.height() > PocketDimension.clearInteriorHeight(generalSize)) {
					return new Result(false, title(wall) + " window tier " + entry.getValue()
						+ " needs a clear interior height of " + dimensions.height(), List.of());
				}
			}

			PocketDimension.InteriorBounds bounds = PocketDimension.bounds(generalSize);
			int minimum = wall == CabinWindowState.Wall.REAR ? bounds.minimumX() : bounds.minimumZ();
			int cursor = minimum + Math.floorDiv(generalSize - totalWidth, 2);
			for (var entry : wallWindows) {
				Dimensions dimensions = dimensions(entry.getValue());
				Set<BlockPos> positions = new LinkedHashSet<>();
				for (int horizontal = cursor; horizontal < cursor + dimensions.width(); horizontal++) {
					for (int y = 1; y <= dimensions.height(); y++) {
						positions.add(position(cellIndex, bounds, wall, horizontal, y));
					}
				}
				result.add(new Footprint(entry.getKey(), entry.getValue(), positions));
				cursor += dimensions.width() + 1;
			}
		}
		return new Result(true, "Window layout fits", result);
	}

	private static Result invalidSize(int generalSize, CabinWindowState.Wall wall, int required) {
		return new Result(false, title(wall) + " windows need general-space size " + required
			+ " (current size " + generalSize + ")", List.of());
	}

	private static BlockPos position(
		long cellIndex,
		PocketDimension.InteriorBounds bounds,
		CabinWindowState.Wall wall,
		int horizontal,
		int y
	) {
		BlockPos center = PocketDimension.cellCenter(cellIndex);
		return switch (wall) {
			case LEFT -> center.offset(bounds.shellMinimumX(), y, horizontal);
			case REAR -> center.offset(horizontal, y, bounds.shellMinimumZ());
			case RIGHT -> center.offset(bounds.shellMaximumX(), y, horizontal);
		};
	}

	private static String title(CabinWindowState.Wall wall) {
		String key = wall.key();
		return Character.toUpperCase(key.charAt(0)) + key.substring(1);
	}
}
