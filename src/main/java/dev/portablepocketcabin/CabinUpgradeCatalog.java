package dev.portablepocketcabin;
import net.minecraft.resources.Identifier;

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

	/** Reads legacy context without selecting or validating upgrade ingredients. */
	static WorldAttunement resolveAttunement(
		CabinRegistry registry,
		ServerLevel pocket,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		return registry.worldAttunement().orElseGet(() -> new WorldAttunement(
			definitions.definitionVersion(), CabinPalette.DEFAULT.walls().profileId()
		));
	}

	static Optional<Offer> next(
		CabinRecord cabin, WorldAttunement attunement, CabinUpgradeDefinitions.Definitions definitions
	) {
		int currentSize = cabin.progression().generalSize();
		if (currentSize >= definitions.maximumGeneralSize()) {
			return Optional.empty();
		}
		int targetSize = currentSize + CabinProgression.GENERAL_SIZE_STEP;
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
			resolve(expansion.ingredients(), cabin.palette())
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
		List<Group> groups = new ArrayList<>();
		groups.add(new Group(CABIN_GROUP, "Cabin", CABIN_ICON, panels));
		if (cabin.upgrades().storage().revealed()) {
			int current = cabin.upgrades().storage().level();
			int next = Math.min(6, current + 1);
			groups.add(new Group(PortablePocketCabin.id("storage"), "Storage",
				net.minecraft.resources.Identifier.parse("minecraft:chest"), List.of(new Offer(
					CabinUpgradeState.Target.storage(next), "Central Storage",
					current == 6 ? "Fully upgraded" : "Store " + CabinStorageState.CAPACITIES[next] + " stacks",
					net.minecraft.resources.Identifier.parse("minecraft:chest"), current, next,
					current == 6 ? List.of() : storageRequirements(next), current == 6, ""))));
		}
		if (cabin.upgrades().crafting().revealed()) {
			int current = cabin.upgrades().crafting().level();
			int next = Math.min(3, current + 1);
			String[] titles = {"Crafting room", "Stonecutting", "Smithing"};
			String[] icons = {"crafting_table", "stonecutter", "smithing_table"};
			String[] effects = {"Install a 5x5 room with crafting, loom and cartography", "Add a stonecutter", "Add a smithing table"};
			groups.add(new Group(PortablePocketCabin.id("crafting"), "Crafting",
				net.minecraft.resources.Identifier.withDefaultNamespace("crafting_table"), List.of(new Offer(
					CabinUpgradeState.Target.crafting(next), titles[next - 1],
					current == 3 ? "Fully upgraded" : effects[next - 1],
					net.minecraft.resources.Identifier.withDefaultNamespace(icons[next - 1]), current, next,
					current == 3 ? List.of() : craftingRequirements(cabin, next), current == 3,
					current == 0 && cabin.progression().generalSize() < 5 ? "Expand the main room to 5x5 first" : ""))));
		}
		if (cabin.upgrades().greenhouse().revealed()) {
			int current = cabin.upgrades().greenhouse().level();
			int next = Math.min(4, current + 1);
			groups.add(new Group(PortablePocketCabin.id("greenhouse"), "Greenhouse",
				Identifier.withDefaultNamespace("wheat"), List.of(new Offer(
					CabinUpgradeState.Target.greenhouse(next), "Greenhouse",
					current == 4 ? "Fully upgraded" : "Grow a " + CabinGreenhouse.WIDTHS[next] + "x" + CabinGreenhouse.LENGTHS[next] + " garden",
					Identifier.withDefaultNamespace("wheat"), current, next,
					current == 4 ? List.of() : greenhouseRequirements(next), current == 4,
					current == 0 && cabin.progression().generalSize() < 5 ? "Expand the main room to 5x5 first" : ""))));
		}
		return List.copyOf(groups);
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
			complete ? List.of() : resolve(definitions.windowIngredients(targetTier), cabin.palette()),
			complete, prerequisite
		);
	}

	static List<CabinUpgradeState.Requirement> greenhouseRequirements(int level) {
		String[][] plants = {{"sunflower", "biomesoplenty:lavender", "biomesoplenty:barley"},
			{"sweet_berries", "cactus", "blue_orchid", "biomesoplenty:clover"},
			{"cocoa_beans", "pink_petals", "biomesoplenty:marigold", "sea_pickle", "biomesoplenty:white_petals"},
			{"spore_blossom", "kelp", "biomesoplenty:blue_hydrangea", "biomesoplenty:icy_iris", "biomesoplenty:glowflower", "biomesoplenty:toadstool"}};
		var result = new ArrayList<CabinUpgradeState.Requirement>();
		result.add(new CabinUpgradeState.Requirement(Identifier.withDefaultNamespace("glass"), new int[]{16,32,48,64}[level-1]));
		result.add(new CabinUpgradeState.Requirement(Identifier.withDefaultNamespace("iron_ingot"), new int[]{8,12,16,24}[level-1]));
		for (String plant : plants[level-1]) result.add(new CabinUpgradeState.Requirement(CabinIngredientFallbacks.resolve(Identifier.parse(plant)), 1));
		return List.copyOf(result);
	}

	static List<CabinUpgradeState.Requirement> craftingRequirements(CabinRecord cabin, int level) {
		String[][] names = {{"crafting_table", "loom", "cartography_table", "iron_ingot", "amethyst_block"},
			{"stonecutter", "copper_ingot", "amethyst_block"},
			{"smithing_table", "copper_ingot", "diamond", "amethyst_block"}};
		int[][] counts = {{1,1,1,8,4}, {1,8,4}, {1,16,1,4}};
		List<CabinUpgradeState.Requirement> result = new ArrayList<>();
		for (int i = 0; i < names[level - 1].length; i++)
			result.add(new CabinUpgradeState.Requirement(net.minecraft.resources.Identifier.withDefaultNamespace(
				names[level - 1][i]), counts[level - 1][i]));
		if (level == 1) result.add(new CabinUpgradeState.Requirement(
			BuiltInRegistries.ITEM.getKey(cabin.palette().walls().structuralWoodBlock().asItem()), 16));
		return List.copyOf(result);
	}

	static List<CabinUpgradeState.Requirement> storageRequirements(int level) {
		String[][] names = {
			{"chest", "iron_ingot", "amethyst_block"},
			{"copper_ingot", "amethyst_block"},
			{"copper_ingot", "amethyst_block", "redstone"},
			{"copper_ingot", "amethyst_block", "redstone", "ender_pearl"},
			{"copper_ingot", "amethyst_block", "redstone", "crying_obsidian", "quartz"},
			{"copper_ingot", "amethyst_block", "redstone", "shulker_shell"}
		};
		int[][] counts = {{2,4,4}, {8,4}, {16,4,4}, {24,4,8,1}, {32,4,12,1,1}, {48,4,16,2}};
		List<CabinUpgradeState.Requirement> result = new ArrayList<>();
		for (int i = 0; i < names[level - 1].length; i++)
			result.add(new CabinUpgradeState.Requirement(net.minecraft.resources.Identifier.parse(
				"minecraft:" + names[level - 1][i]), counts[level - 1][i]));
		return List.copyOf(result);
	}

	private static String dimensions(int tier) {
		CabinWindowLayout.Dimensions dimensions = CabinWindowLayout.dimensions(tier);
		return dimensions.width() + "x" + dimensions.height();
	}

	static List<CabinUpgradeState.Requirement> resolve(
		List<CabinUpgradeDefinitions.Ingredient> ingredients, CabinPalette palette
	) {
		Map<net.minecraft.resources.Identifier, Integer> consolidated = new LinkedHashMap<>();
		for (CabinUpgradeDefinitions.Ingredient ingredient : ingredients) {
			if (ingredient.palettePlanks()) {
				List<CabinPalette.WoodSelection> woods = List.of(palette.floor(), palette.walls(), palette.roof());
				for (int index = 0; index < woods.size(); index++) {
					int count = ingredient.count() / woods.size()
						+ (index < ingredient.count() % woods.size() ? 1 : 0);
					if (count > 0) {
						var item = woods.get(index).planksBlock().asItem();
						if (item == net.minecraft.world.item.Items.AIR) {
							throw new IllegalStateException("Cabin palette planks have no item: " + woods.get(index).planks());
						}
						consolidated.merge(BuiltInRegistries.ITEM.getKey(item), count, Math::addExact);
					}
				}
			} else {
				var itemId = BuiltInRegistries.ITEM.getKey(ingredient.resolveItem());
				consolidated.merge(itemId, ingredient.count(), Math::addExact);
			}
		}
		List<CabinUpgradeState.Requirement> result = new ArrayList<>(consolidated.size());
		consolidated.forEach((item, count) -> result.add(new CabinUpgradeState.Requirement(item, count)));
		result.sort(java.util.Comparator.comparing(value -> value.itemId().toString()));
		return List.copyOf(result);
	}
}
