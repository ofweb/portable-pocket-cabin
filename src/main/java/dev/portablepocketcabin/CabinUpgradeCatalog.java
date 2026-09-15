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
	static final net.minecraft.resources.Identifier CABIN_GROUP = PortablePocketCabin.id("cabin");
	private static final net.minecraft.resources.Identifier CABIN_ICON =
		net.minecraft.resources.Identifier.parse("minecraft:oak_door");
	private static final net.minecraft.resources.Identifier GENERAL_SPACE_ICON =
		net.minecraft.resources.Identifier.parse("minecraft:amethyst_block");

	record Group(
		net.minecraft.resources.Identifier id,
		String title,
		net.minecraft.resources.Identifier iconItem,
		List<Offer> panels
	) {
		Group {
			java.util.Objects.requireNonNull(id, "id");
			java.util.Objects.requireNonNull(title, "title");
			java.util.Objects.requireNonNull(iconItem, "iconItem");
			panels = List.copyOf(panels);
			if (title.isBlank()) {
				throw new IllegalArgumentException("Upgrade group title must not be blank");
			}
			if (panels.isEmpty()) {
				throw new IllegalArgumentException("Empty upgrade groups must not be displayed");
			}
		}
	}

	record Offer(
		CabinUpgradeState.Target target,
		String title,
		String effect,
		net.minecraft.resources.Identifier iconItem,
		int currentSize,
		int targetSize,
		List<CabinUpgradeState.Requirement> requirements
	) {
		Offer {
			java.util.Objects.requireNonNull(target, "target");
			java.util.Objects.requireNonNull(title, "title");
			java.util.Objects.requireNonNull(effect, "effect");
			java.util.Objects.requireNonNull(iconItem, "iconItem");
			requirements = List.copyOf(requirements);
			if (title.isBlank() || effect.isBlank()) {
				throw new IllegalArgumentException("Upgrade panel title and effect must not be blank");
			}
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
			"General Space",
			"Expand the general cabin space to " + targetSize + "x" + targetSize,
			GENERAL_SPACE_ICON,
			currentSize,
			targetSize,
			resolve(expansion, attunement)
		));
	}

	/** Returns only implemented, non-empty groups; later deliveries can add panels without menu rewrites. */
	static List<Group> groups(
		CabinRecord cabin, WorldAttunement attunement, CabinUpgradeDefinitions.Definitions definitions
	) {
		return next(cabin, attunement, definitions)
			.map(offer -> List.of(new Group(CABIN_GROUP, "Cabin", CABIN_ICON, List.of(offer))))
			.orElse(List.of());
	}

	static boolean isStale(
		CabinUpgradeState.Fund fund,
		CabinRecord cabin,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		Optional<Offer> current = next(cabin, attunement, definitions);
		return current.isEmpty()
			|| !current.get().target().equals(fund.target())
			|| !current.get().requirements().equals(fund.requirements());
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
