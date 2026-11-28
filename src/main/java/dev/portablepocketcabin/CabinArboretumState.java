package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The arboretum's progress (B-0023): whether its book is installed, the grove size, the tree types the household has
 * taught it, and the types it currently plants.
 */
record CabinArboretumState(boolean revealed, int level, Set<Identifier> taught, List<Identifier> selected) {
	static final int MAX_LEVEL = 3;
	static final CabinArboretumState EMPTY = new CabinArboretumState(false, 0, Set.of(), List.of());
	static final Codec<CabinArboretumState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("revealed", false).forGetter(CabinArboretumState::revealed),
		Codec.INT.optionalFieldOf("level", 0).forGetter(CabinArboretumState::level),
		Identifier.CODEC.listOf().optionalFieldOf("taught", List.of()).forGetter(state -> List.copyOf(state.taught())),
		Identifier.CODEC.listOf().optionalFieldOf("selected", List.of()).forGetter(CabinArboretumState::selected)
	).apply(instance, (revealed, level, taught, selected) -> new CabinArboretumState(revealed, level, new LinkedHashSet<>(taught), selected)));

	CabinArboretumState {
		taught = java.util.Collections.unmodifiableSet(new LinkedHashSet<>(taught));
		selected = List.copyOf(selected);
		if (level < 0 || level > MAX_LEVEL || level > 0 && !revealed)
			throw new IllegalArgumentException("Invalid arboretum installation");
		if (selected.size() > limit(level) || !taught.containsAll(selected) || new LinkedHashSet<>(selected).size() != selected.size())
			throw new IllegalArgumentException("Invalid arboretum selection");
	}

	/** How many tree types a grove of this size plants at once. */
	static int limit(int level) { return level; }

	CabinArboretumState reveal() { return new CabinArboretumState(true, level, taught, selected); }

	CabinArboretumState upgrade(int target) {
		if (target != level + 1) throw new IllegalArgumentException("Arboretum requires the previous size");
		return new CabinArboretumState(revealed, target, taught, selected);
	}

	CabinArboretumState teach(Identifier sapling) {
		var updated = new LinkedHashSet<>(taught);
		updated.add(sapling);
		return new CabinArboretumState(revealed, level, updated, selected);
	}

	CabinArboretumState select(List<Identifier> value) { return new CabinArboretumState(revealed, level, taught, value); }
}
