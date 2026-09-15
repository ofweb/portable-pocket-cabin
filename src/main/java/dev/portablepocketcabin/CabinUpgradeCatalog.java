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
	private static final net.minecraft.resources.Identifier WINDOW_ICON =
		net.minecraft.resources.Identifier.parse("minecraft:glass_pane");

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
		List<CabinUpgradeState.Requirement> requirements,
		boolean complete,
		String prerequisite
	) {
		Offer {
			java.util.Objects.requireNonNull(target, "target");
			java.util.Objects.requireNonNull(title, "title");
			java.util.Objects.requireNonNull(effect, "effect");
			java.util.Objects.requireNonNull(iconItem, "iconItem");
			java.util.Objects.requireNonNull(prerequisite, "prerequisite");
			requirements = List.copyOf(requirements);
			if (title.isBlank() || effect.isBlank()) {
				throw new IllegalArgumentException("Upgrade panel title and effect must not be blank");
			}
		}

		Offer(
			CabinUpgradeState.Target target,
			String title,
			String effect,
			net.minecraft.resources.Identifier iconItem,
			int currentSize,
			int targetSize,
			List<CabinUpgradeState.Requirement> requirements
		) {
			this(target, title, effect, iconItem, currentSize, targetSize, requirements, false, "");
		}

		boolean locked() {
			return !prerequisite.isEmpty();
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
			, false, ""
		));
	}

	/** Returns only implemented, non-empty groups; later deliveries can add panels without menu rewrites. */
	static List<Group> groups(
		CabinRecord cabin, WorldAttunement attunement, CabinUpgradeDefinitions.Definitions definitions
	) {
		List<Offer> panels = new ArrayList<>();
		next(cabin, attunement, definitions).ifPresent(panels::add);
		for (CabinWindowState.Wall wall : CabinWindowState.Wall.values()) {
			for (int slot = 0; slot < 2; slot++) {
				panels.add(window(cabin, attunement, definitions, new CabinWindowState.Identity(wall, slot)));
			}
		}
		return List.of(new Group(CABIN_GROUP, "Cabin", CABIN_ICON, panels));
	}

	static Optional<Offer> offer(
		CabinRecord cabin,
		CabinUpgradeState.Target target,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		return groups(cabin, attunement, definitions).stream()
			.flatMap(group -> group.panels().stream())
			.filter(value -> value.target().equals(target))
			.findFirst();
	}

	static boolean isStale(
		CabinUpgradeState.Fund fund,
		CabinRecord cabin,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		Optional<Offer> current = offer(cabin, fund.target(), attunement, definitions);
		return current.isEmpty()
			|| current.get().complete()
			|| !current.get().requirements().equals(fund.requirements());
	}

	private static Offer window(
		CabinRecord cabin,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions,
		CabinWindowState.Identity identity
	) {
		int currentTier = cabin.upgrades().windows().tier(identity);
		boolean complete = currentTier == CabinWindowState.MAX_TIER;
		int targetTier = complete ? currentTier : currentTier + 1;
		String wall = Character.toUpperCase(identity.wall().key().charAt(0))
			+ identity.wall().key().substring(1);
		String title = wall + " Window " + (identity.slot() + 1);
		String effect = complete
			? "Installed at maximum tier " + currentTier
			: currentTier == 0
				? "Install a " + dimensions(targetTier)
				: "Resize from " + dimensions(currentTier) + " to " + dimensions(targetTier);
		String prerequisite = identity.slot() == 1 && currentTier == 0
			&& cabin.upgrades().windows().tier(new CabinWindowState.Identity(identity.wall(), 0)) == 0
			? "Install the first window on this wall before the second" : "";
		return new Offer(
			CabinUpgradeState.Target.window(identity), title, effect, WINDOW_ICON,
			currentTier, targetTier,
			complete ? List.of() : resolve(definitions.windowIngredients(targetTier), attunement),
			complete, prerequisite
		);
	}

	private static String dimensions(int tier) {
		CabinWindowLayout.Dimensions dimensions = CabinWindowLayout.dimensions(tier);
		return dimensions.width() + "x" + dimensions.height();
	}

	static List<CabinUpgradeState.Requirement> resolve(
		CabinUpgradeDefinitions.Expansion expansion, WorldAttunement attunement
	) {
		return resolve(expansion.ingredients(), attunement);
	}

	static List<CabinUpgradeState.Requirement> resolve(
		List<CabinUpgradeDefinitions.Ingredient> ingredients, WorldAttunement attunement
	) {
		Map<net.minecraft.resources.Identifier, Integer> consolidated = new LinkedHashMap<>();
		for (CabinUpgradeDefinitions.Ingredient ingredient : ingredients) {
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
