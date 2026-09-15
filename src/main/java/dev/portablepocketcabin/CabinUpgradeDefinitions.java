package dev.portablepocketcabin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reloadable balancing definitions; resolved world choices are persisted in {@link CabinRegistry}. */
final class CabinUpgradeDefinitions {
	private static final int DEFAULT_MAX_GENERAL_SIZE = 21;

	record Ingredient(Identifier itemId, boolean attunedPlanks, int count) {
		Ingredient {
			if ((itemId == null) != attunedPlanks) {
				throw new IllegalArgumentException("An upgrade ingredient must select exactly one source");
			}
			if (count <= 0) {
				throw new IllegalArgumentException("Upgrade ingredient counts must be positive");
			}
		}

		Item resolve(WorldAttunement attunement) {
			if (attunedPlanks) {
				return CabinMaterialProfiles.woodProfile(attunement.woodProfile())
					.orElseThrow(() -> new IllegalStateException(
						"World attunement references unavailable wood profile " + attunement.woodProfile()
					))
					.planksIngredient();
			}
			return BuiltInRegistries.ITEM.getOptional(itemId)
				.filter(item -> item != Items.AIR)
				.orElseThrow(() -> new IllegalStateException("Upgrade definition references missing item " + itemId));
		}
	}

	record Expansion(int targetSize, List<Ingredient> ingredients) {
		Expansion {
			ingredients = List.copyOf(ingredients);
			if (targetSize <= CabinProgression.INITIAL_GENERAL_SIZE || ingredients.isEmpty()) {
				throw new IllegalArgumentException("Each expansion must target a larger size and have ingredients");
			}
		}
	}

	record WindowTier(int targetTier, List<Ingredient> ingredients) {
		WindowTier {
			ingredients = List.copyOf(ingredients);
			if (targetTier < 2 || targetTier > CabinWindowState.MAX_TIER || ingredients.isEmpty()) {
				throw new IllegalArgumentException("Each window tier must target tier 2 through 6 and have ingredients");
			}
		}
	}

	record Definitions(
		int definitionVersion,
		int maximumGeneralSize,
		List<Identifier> woodPool,
		List<Expansion> expansions,
		List<Ingredient> windowBase,
		List<WindowTier> windowTiers
	) {
		Definitions {
			woodPool = List.copyOf(woodPool);
			expansions = List.copyOf(expansions);
			windowBase = List.copyOf(windowBase);
			windowTiers = List.copyOf(windowTiers);
			if (definitionVersion <= 0) {
				throw new IllegalArgumentException("definition_version must be positive");
			}
			if (maximumGeneralSize <= CabinProgression.INITIAL_GENERAL_SIZE
				|| maximumGeneralSize > CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE) {
				throw new IllegalArgumentException("maximum_general_size is outside the supported range");
			}
			if (woodPool.isEmpty()) {
				throw new IllegalArgumentException("wood_pool must include a vanilla fallback");
			}
			for (int size = CabinProgression.INITIAL_GENERAL_SIZE + 1; size <= maximumGeneralSize; size++) {
				int targetSize = size;
				if (expansions.stream().noneMatch(value -> value.targetSize() == targetSize)) {
					throw new IllegalArgumentException("Missing general expansion definition for size " + size);
				}
			}
			if (windowBase.isEmpty()) {
				throw new IllegalArgumentException("window_base must have ingredients");
			}
			for (int tier = 2; tier <= CabinWindowState.MAX_TIER; tier++) {
				int targetTier = tier;
				if (windowTiers.stream().noneMatch(value -> value.targetTier() == targetTier)) {
					throw new IllegalArgumentException("Missing cabin window definition for tier " + tier);
				}
			}
		}

		Definitions(
			int definitionVersion, int maximumGeneralSize, List<Identifier> woodPool, List<Expansion> expansions
		) {
			this(definitionVersion, maximumGeneralSize, woodPool, expansions, builtInWindowBase(), builtInWindowTiers());
		}

		Expansion expansion(int targetSize) {
			return expansions.stream().filter(value -> value.targetSize() == targetSize).findFirst().orElse(null);
		}

		List<Ingredient> windowIngredients(int targetTier) {
			if (targetTier == 1) {
				return windowBase;
			}
			return windowTiers.stream()
				.filter(value -> value.targetTier() == targetTier)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unsupported cabin window tier " + targetTier))
				.ingredients();
		}

		WorldAttunement resolve(long worldSeed) {
			List<Identifier> available = woodPool.stream()
				.filter(id -> CabinMaterialProfiles.woodProfile(id).isPresent())
				.toList();
			if (available.isEmpty()) {
				throw new IllegalStateException("World attunement wood pool has no loaded acquisition profile");
			}
			int index = Math.floorMod(Long.hashCode(mix(worldSeed)), available.size());
			return new WorldAttunement(definitionVersion, available.get(index));
		}

		boolean isValid(WorldAttunement attunement) {
			return attunement.definitionVersion() == definitionVersion
				&& woodPool.contains(attunement.woodProfile())
				&& CabinMaterialProfiles.woodProfile(attunement.woodProfile()).isPresent();
		}
	}

	private static volatile Definitions current = builtIn();

	private CabinUpgradeDefinitions() {
	}

	static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
			PortablePocketCabin.id("upgrade_definitions"),
			new SimpleReloadListener<Definitions>() {
				@Override
				protected Definitions prepare(PreparableReloadListener.SharedState state) {
					return load(state);
				}

				@Override
				protected void apply(Definitions prepared, PreparableReloadListener.SharedState state) {
					current = prepared;
					PortablePocketCabin.LOGGER.info(
						"Loaded cabin progression definition {} with maximum general size {}",
						prepared.definitionVersion(), prepared.maximumGeneralSize()
					);
				}
			}
		);
	}

	static Definitions current() {
		return current;
	}

	private static Definitions load(PreparableReloadListener.SharedState state) {
		Map<Identifier, Resource> resources = state.resourceManager().listResources(
			"portable_pocket_cabin/progression", id -> id.getPath().endsWith(".json")
		);
		Resource resource = resources.get(PortablePocketCabin.id(
			"portable_pocket_cabin/progression/default.json"
		));
		if (resource == null) {
			throw new IllegalStateException("Missing cabin progression definition default.json");
		}
		try (var reader = resource.openAsReader()) {
			return parse(GsonHelper.parse(reader));
		} catch (Exception exception) {
			throw new IllegalStateException("Could not load cabin progression definition", exception);
		}
	}

	static Definitions parse(JsonObject json) {
		int schemaVersion = GsonHelper.getAsInt(json, "schema_version");
		if (schemaVersion != 2) {
			throw new IllegalArgumentException("Unsupported progression schema_version " + schemaVersion);
		}
		int definitionVersion = GsonHelper.getAsInt(json, "definition_version");
		int maximum = GsonHelper.getAsInt(json, "maximum_general_size");
		List<Identifier> woodPool = new ArrayList<>();
		for (var element : GsonHelper.getAsJsonArray(json, "wood_pool")) {
			woodPool.add(Identifier.parse(element.getAsString()));
		}
		List<Expansion> expansions = new ArrayList<>();
		for (var expansionElement : GsonHelper.getAsJsonArray(json, "general_expansions")) {
			JsonObject expansionJson = expansionElement.getAsJsonObject();
			expansions.add(new Expansion(
				GsonHelper.getAsInt(expansionJson, "target_size"),
				parseIngredients(GsonHelper.getAsJsonArray(expansionJson, "ingredients"))
			));
		}
		List<Ingredient> windowBase = parseIngredients(GsonHelper.getAsJsonArray(json, "window_base"));
		List<WindowTier> windowTiers = new ArrayList<>();
		for (var tierElement : GsonHelper.getAsJsonArray(json, "window_tiers")) {
			JsonObject tier = tierElement.getAsJsonObject();
			windowTiers.add(new WindowTier(
				GsonHelper.getAsInt(tier, "target_tier"),
				parseIngredients(GsonHelper.getAsJsonArray(tier, "ingredients"))
			));
		}
		return new Definitions(definitionVersion, maximum, woodPool, expansions, windowBase, windowTiers);
	}

	private static List<Ingredient> parseIngredients(JsonArray ingredientArray) {
		List<Ingredient> ingredients = new ArrayList<>();
		for (var ingredientElement : ingredientArray) {
			JsonObject ingredient = ingredientElement.getAsJsonObject();
			String slot = GsonHelper.getAsString(ingredient, "attuned_slot", "");
			Identifier itemId = ingredient.has("item")
				? Identifier.parse(GsonHelper.getAsString(ingredient, "item")) : null;
			if (!slot.isEmpty() && !"planks".equals(slot)) {
				throw new IllegalArgumentException("Unsupported attuned_slot " + slot);
			}
			ingredients.add(new Ingredient(
				itemId, "planks".equals(slot), GsonHelper.getAsInt(ingredient, "count")
			));
		}
		return List.copyOf(ingredients);
	}

	private static Definitions builtIn() {
		List<Identifier> woods = List.of(
			wood("oak"), wood("spruce"), wood("birch"), wood("jungle"), wood("acacia"),
			wood("dark_oak"), wood("mangrove"), wood("cherry"), wood("pale_oak"), wood("bamboo")
		);
		List<Expansion> expansions = new ArrayList<>();
		for (int size = 5; size <= DEFAULT_MAX_GENERAL_SIZE; size++) {
			int step = size - CabinProgression.INITIAL_GENERAL_SIZE;
			expansions.add(new Expansion(size, List.of(
				new Ingredient(null, true, 8 + step * 4),
				new Ingredient(Identifier.parse("minecraft:amethyst_block"), false, step),
				new Ingredient(Identifier.parse("minecraft:obsidian"), false, 2 + step * 2)
			)));
		}
		return new Definitions(
			1, DEFAULT_MAX_GENERAL_SIZE, woods, expansions, builtInWindowBase(), builtInWindowTiers()
		);
	}

	private static List<Ingredient> builtInWindowBase() {
		List<Ingredient> result = new ArrayList<>();
		result.add(item("glass_pane", 16));
		result.add(item("amethyst_shard", 4));
		for (String dye : List.of(
			"yellow_dye", "orange_dye", "white_dye", "light_blue_dye", "magenta_dye", "blue_dye",
			"light_gray_dye", "cyan_dye", "gray_dye", "red_dye", "purple_dye"
		)) {
			result.add(item(dye, 1));
		}
		return List.copyOf(result);
	}

	private static List<WindowTier> builtInWindowTiers() {
		int[] panes = {4, 8, 12, 24, 32};
		int[] shards = {1, 2, 3, 6, 8};
		List<WindowTier> result = new ArrayList<>();
		for (int index = 0; index < panes.length; index++) {
			result.add(new WindowTier(index + 2, List.of(
				item("glass_pane", panes[index]), item("amethyst_shard", shards[index])
			)));
		}
		return List.copyOf(result);
	}

	private static Ingredient item(String path, int count) {
		return new Ingredient(Identifier.parse("minecraft:" + path), false, count);
	}

	private static Identifier wood(String name) {
		return PortablePocketCabin.id("vanilla/wood/" + name);
	}

	private static long mix(long value) {
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdl;
		value ^= value >>> 33;
		value *= 0xc4ceb9fe1a85ec53l;
		return value ^ value >>> 33;
	}
}
