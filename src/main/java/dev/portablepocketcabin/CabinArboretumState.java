package dev.portablepocketcabin;

import net.minecraft.resources.Identifier;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Registry value for an installed arboretum; absence is represented outside this value. */
record CabinArboretumState(
	CabinArboretumTier tier,
	List<Identifier> selectedTreeTypes,
	CabinArboretumWorlds.Pattern pattern
) {
	CabinArboretumState {
		Objects.requireNonNull(tier);
		Objects.requireNonNull(pattern);
		selectedTreeTypes = List.copyOf(selectedTreeTypes);
		if (selectedTreeTypes.isEmpty() || selectedTreeTypes.size() > tier.selectedTreeLimit()
			|| new HashSet<>(selectedTreeTypes).size() != selectedTreeTypes.size()) {
			throw new IllegalArgumentException("Arboretum selection must contain distinct types within its tier limit");
		}
	}

	/** Tree type identifiers resolve through compatible planting profiles, independent of purchase materials. */
	static CabinArboretumState initial(CabinArboretumWorlds.Pattern pattern) {
		return new CabinArboretumState(CabinArboretumTier.INITIAL,
			List.of(Identifier.withDefaultNamespace("oak")), pattern);
	}
}
