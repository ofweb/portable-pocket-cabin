package dev.portablepocketcabin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Resolves the visible, code-defined upgrade offer from cabin state and reloadable costs. */
final class CabinUpgradeCatalog {
	record Offer(
		CabinUpgradeState.Target target,
		int currentSize,
		int targetSize,
		List<CabinUpgradeState.Requirement> requirements,
		String effect
	) {
		Offer {
			requirements = List.copyOf(requirements);
		}
	}

	private CabinUpgradeCatalog() {
	}

	static WorldAttunement resolveAttunement(
		CabinRegistry registry,
		ServerLevel pocket,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		WorldAttunement current = registry.worldAttunement().orElse(null);
		if (current != null) {
			if (!definitions.isValid(current)) {
				throw new IllegalStateException("The saved world attunement is invalid under the loaded definitions; "
					+ "an operator must validate the datapacks before upgrades can continue.");
			}
			return current;
		}
		WorldAttunement resolved = registry.resolveWorldAttunement(
			definitions.resolve(pocket.getServer().overworld().getSeed())
		);
		CabinRegistry.flush(pocket.getServer());
		return resolved;
	}

	static Optional<Offer> next(
		CabinRecord cabin, WorldAttunement attunement, CabinUpgradeDefinitions.Definitions definitions
	) {
		int currentSize = cabin.progression().generalSize();
		if (currentSize >= definitions.maximumGeneralSize()) {
			return Optional.empty();
		}
		int targetSize = currentSize + 1;
		CabinUpgradeDefinitions.Expansion expansion = definitions.expansion(targetSize);
		if (expansion == null) {
			return Optional.empty();
		}
		return Optional.of(new Offer(
			CabinUpgradeState.Target.generalSpace(targetSize),
			currentSize,
			targetSize,
			resolve(expansion, attunement),
			"Expand the general cabin space to " + targetSize + "x" + targetSize
		));
	}

	static boolean isStale(
		CabinUpgradeState.TrackedUpgrade tracked,
		CabinRecord cabin,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		Optional<Offer> current = next(cabin, attunement, definitions);
		return current.isEmpty()
			|| !current.get().target().equals(tracked.target())
			|| !current.get().requirements().equals(tracked.requirements());
	}

	static List<CabinUpgradeState.Requirement> resolve(
		CabinUpgradeDefinitions.Expansion expansion, WorldAttunement attunement
	) {
		Map<net.minecraft.resources.Identifier, Integer> consolidated = new LinkedHashMap<>();
		for (CabinUpgradeDefinitions.Ingredient ingredient : expansion.ingredients()) {
			var item = ingredient.resolve(attunement);
			var itemId = BuiltInRegistries.ITEM.getKey(item);
			consolidated.merge(itemId, ingredient.count(), Math::addExact);
		}
		List<CabinUpgradeState.Requirement> result = new ArrayList<>(consolidated.size());
		consolidated.forEach((item, count) -> result.add(new CabinUpgradeState.Requirement(item, count)));
		result.sort(java.util.Comparator.comparing(value -> value.itemId().toString()));
		return List.copyOf(result);
	}
}
