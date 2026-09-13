package dev.portablepocketcabin;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class CabinMaterialProfiles {
	record WoodProfile(Item planksIngredient, Item structuralIngredient, CabinPalette.WoodSelection selection) {
	}

	record DoorProfile(Item ingredient, CabinPalette.DoorSelection selection) {
	}

	private static volatile ProfileSet current = vanillaProfiles();

	private CabinMaterialProfiles() {
	}

	static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
			PortablePocketCabin.id("material_profiles"),
			new SimpleReloadListener<ProfileSet>() {
				@Override
				protected ProfileSet prepare(PreparableReloadListener.SharedState state) {
					return load(state);
				}

				@Override
				protected void apply(ProfileSet prepared, PreparableReloadListener.SharedState state) {
					current = prepared;
					PortablePocketCabin.LOGGER.info(
						"Loaded {} cabin wood profiles and {} door profiles",
						prepared.woodByPlanks.size(), prepared.doors.size()
					);
				}
			}
		);
	}

	static Optional<CabinPalette.WoodSelection> matchPlanks(ItemStack stack) {
		return Optional.ofNullable(current.woodByPlanks.get(stack.getItem())).map(WoodProfile::selection);
	}

	static Optional<CabinPalette.WoodSelection> matchStructuralWood(ItemStack stack) {
		return Optional.ofNullable(current.woodByStructural.get(stack.getItem())).map(WoodProfile::selection);
	}

	static Optional<CabinPalette.DoorSelection> matchDoor(ItemStack stack) {
		return Optional.ofNullable(current.doors.get(stack.getItem())).map(DoorProfile::selection);
	}

	static List<Item> plankIngredients() {
		return current.woodByPlanks.keySet().stream().toList();
	}

	static List<Item> structuralIngredients() {
		return current.woodByStructural.keySet().stream().toList();
	}

	static List<Item> doorIngredients() {
		return current.doors.keySet().stream().toList();
	}

	static int woodProfileCount() {
		return current.woodByPlanks.size();
	}

	static int doorProfileCount() {
		return current.doors.size();
	}

	private static ProfileSet load(PreparableReloadListener.SharedState state) {
		List<WoodProfile> woods = new ArrayList<>();
		List<DoorProfile> doors = new ArrayList<>();
		Map<Identifier, Resource> resources = state.resourceManager().listResources(
			"portable_pocket_cabin/material_profiles", id -> id.getPath().endsWith(".json")
		);
		for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
			Identifier resourceId = entry.getKey();
			Identifier profileId = profileId(resourceId);
			JsonObject json;
			try (var reader = entry.getValue().openAsReader()) {
				json = GsonHelper.parse(reader);
			} catch (Exception exception) {
				throw new IllegalStateException("Could not read cabin material profile " + resourceId, exception);
			}
			int schemaVersion = GsonHelper.getAsInt(json, "schema_version");
			if (schemaVersion != 1) {
				throw new IllegalStateException("Cabin material profile " + profileId
					+ " uses unsupported schema_version " + schemaVersion + "; expected 1");
			}
			String requiredMod = GsonHelper.getAsString(json, "required_mod", "");
			if (!requiredMod.isEmpty() && !FabricLoader.getInstance().isModLoaded(requiredMod)) {
				continue;
			}
			String type = GsonHelper.getAsString(json, "type");
			switch (type) {
				case "wood_family" -> woods.add(parseWood(profileId, json));
				case "door" -> doors.add(parseDoor(profileId, json));
				default -> throw new IllegalStateException("Cabin material profile " + profileId
					+ " has unsupported type " + type);
			}
		}
		if (woods.isEmpty() || doors.isEmpty()) {
			throw new IllegalStateException("Cabin material profiles must define at least one wood family and door");
		}
		return new ProfileSet(woods, doors);
	}

	private static WoodProfile parseWood(Identifier profileId, JsonObject json) {
		Item planksIngredient = requireItem(profileId, json, "planks_ingredient");
		Item structuralIngredient = requireItem(profileId, json, "structural_wood_ingredient");
		Block planks = requireBlock(profileId, json, "planks");
		Block structural = requireBlock(profileId, json, "structural_wood");
		Block stairs = requireBlock(profileId, json, "stairs");
		Block slab = requireBlock(profileId, json, "slab");
		if (planks.asItem() != planksIngredient || structural.asItem() != structuralIngredient) {
			throw new IllegalStateException("Cabin wood profile " + profileId
				+ " ingredients must be the item forms of its planks and structural blocks");
		}
		return new WoodProfile(planksIngredient, structuralIngredient, new CabinPalette.WoodSelection(
			profileId, blockId(planks), blockId(structural), blockId(stairs), blockId(slab)
		));
	}

	private static DoorProfile parseDoor(Identifier profileId, JsonObject json) {
		Item ingredient = requireItem(profileId, json, "door_ingredient");
		Block block = requireBlock(profileId, json, "door");
		if (!(block instanceof net.minecraft.world.level.block.DoorBlock) || block.asItem() != ingredient) {
			throw new IllegalStateException("Cabin door profile " + profileId
				+ " must reference a door block and its matching item");
		}
		return new DoorProfile(ingredient, new CabinPalette.DoorSelection(profileId, blockId(block)));
	}

	private static Item requireItem(Identifier profileId, JsonObject json, String field) {
		Identifier id = Identifier.parse(GsonHelper.getAsString(json, field));
		Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
		if (item == null || item == Items.AIR) {
			throw new IllegalStateException("Cabin material profile " + profileId
				+ " references missing item " + id + " in " + field);
		}
		return item;
	}

	private static Block requireBlock(Identifier profileId, JsonObject json, String field) {
		Identifier id = Identifier.parse(GsonHelper.getAsString(json, field));
		Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
		if (block == null || block == Blocks.AIR) {
			throw new IllegalStateException("Cabin material profile " + profileId
				+ " references missing block " + id + " in " + field);
		}
		return block;
	}

	private static Identifier profileId(Identifier resourceId) {
		String prefix = "portable_pocket_cabin/material_profiles/";
		String path = resourceId.getPath();
		return Identifier.fromNamespaceAndPath(
			resourceId.getNamespace(), path.substring(prefix.length(), path.length() - ".json".length())
		);
	}

	private static ProfileSet vanillaProfiles() {
		List<WoodProfile> woods = new ArrayList<>();
		addVanillaWood(woods, "oak", Items.OAK_PLANKS, Items.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_LOG,
			Blocks.OAK_STAIRS, Blocks.OAK_SLAB);
		addVanillaWood(woods, "spruce", Items.SPRUCE_PLANKS, Items.SPRUCE_LOG, Blocks.SPRUCE_PLANKS,
			Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB);
		addVanillaWood(woods, "birch", Items.BIRCH_PLANKS, Items.BIRCH_LOG, Blocks.BIRCH_PLANKS, Blocks.BIRCH_LOG,
			Blocks.BIRCH_STAIRS, Blocks.BIRCH_SLAB);
		addVanillaWood(woods, "jungle", Items.JUNGLE_PLANKS, Items.JUNGLE_LOG, Blocks.JUNGLE_PLANKS,
			Blocks.JUNGLE_LOG, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB);
		addVanillaWood(woods, "acacia", Items.ACACIA_PLANKS, Items.ACACIA_LOG, Blocks.ACACIA_PLANKS,
			Blocks.ACACIA_LOG, Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB);
		addVanillaWood(woods, "dark_oak", Items.DARK_OAK_PLANKS, Items.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS,
			Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB);
		addVanillaWood(woods, "mangrove", Items.MANGROVE_PLANKS, Items.MANGROVE_LOG, Blocks.MANGROVE_PLANKS,
			Blocks.MANGROVE_LOG, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_SLAB);
		addVanillaWood(woods, "cherry", Items.CHERRY_PLANKS, Items.CHERRY_LOG, Blocks.CHERRY_PLANKS,
			Blocks.CHERRY_LOG, Blocks.CHERRY_STAIRS, Blocks.CHERRY_SLAB);
		addVanillaWood(woods, "pale_oak", Items.PALE_OAK_PLANKS, Items.PALE_OAK_LOG, Blocks.PALE_OAK_PLANKS,
			Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_STAIRS, Blocks.PALE_OAK_SLAB);
		addVanillaWood(woods, "bamboo", Items.BAMBOO_PLANKS, Items.BAMBOO_BLOCK, Blocks.BAMBOO_PLANKS,
			Blocks.BAMBOO_BLOCK, Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_SLAB);
		addVanillaWood(woods, "crimson", Items.CRIMSON_PLANKS, Items.CRIMSON_STEM, Blocks.CRIMSON_PLANKS,
			Blocks.CRIMSON_STEM, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_SLAB);
		addVanillaWood(woods, "warped", Items.WARPED_PLANKS, Items.WARPED_STEM, Blocks.WARPED_PLANKS,
			Blocks.WARPED_STEM, Blocks.WARPED_STAIRS, Blocks.WARPED_SLAB);

		List<DoorProfile> doors = new ArrayList<>();
		addDoor(doors, Items.OAK_DOOR, Blocks.OAK_DOOR);
		addDoor(doors, Items.SPRUCE_DOOR, Blocks.SPRUCE_DOOR);
		addDoor(doors, Items.BIRCH_DOOR, Blocks.BIRCH_DOOR);
		addDoor(doors, Items.JUNGLE_DOOR, Blocks.JUNGLE_DOOR);
		addDoor(doors, Items.ACACIA_DOOR, Blocks.ACACIA_DOOR);
		addDoor(doors, Items.DARK_OAK_DOOR, Blocks.DARK_OAK_DOOR);
		addDoor(doors, Items.MANGROVE_DOOR, Blocks.MANGROVE_DOOR);
		addDoor(doors, Items.CHERRY_DOOR, Blocks.CHERRY_DOOR);
		addDoor(doors, Items.PALE_OAK_DOOR, Blocks.PALE_OAK_DOOR);
		addDoor(doors, Items.BAMBOO_DOOR, Blocks.BAMBOO_DOOR);
		addDoor(doors, Items.CRIMSON_DOOR, Blocks.CRIMSON_DOOR);
		addDoor(doors, Items.WARPED_DOOR, Blocks.WARPED_DOOR);
		addDoor(doors, Items.IRON_DOOR, Blocks.IRON_DOOR);
		List<Item> copperItems = Items.COPPER_DOOR.asList();
		List<Block> copperBlocks = Blocks.COPPER_DOOR.asList();
		for (int index = 0; index < copperItems.size(); index++) {
			addDoor(doors, copperItems.get(index), copperBlocks.get(index));
		}
		return new ProfileSet(woods, doors);
	}

	private static void addVanillaWood(
		List<WoodProfile> profiles, String name, Item planksIngredient, Item structuralIngredient,
		Block planks, Block structural, Block stairs, Block slab
	) {
		profiles.add(new WoodProfile(planksIngredient, structuralIngredient, new CabinPalette.WoodSelection(
			PortablePocketCabin.id("vanilla/wood/" + name),
			blockId(planks), blockId(structural), blockId(stairs), blockId(slab)
		)));
	}

	private static void addDoor(List<DoorProfile> profiles, Item ingredient, Block block) {
		Identifier id = blockId(block);
		String name = id.getPath().substring(0, id.getPath().length() - "_door".length());
		profiles.add(new DoorProfile(ingredient, new CabinPalette.DoorSelection(
			PortablePocketCabin.id("vanilla/door/" + name), id
		)));
	}

	private static Identifier blockId(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block);
	}

	private static final class ProfileSet {
		private final Map<Item, WoodProfile> woodByPlanks;
		private final Map<Item, WoodProfile> woodByStructural;
		private final Map<Item, DoorProfile> doors;

		private ProfileSet(List<WoodProfile> woods, List<DoorProfile> doorProfiles) {
			Map<Item, WoodProfile> planks = new LinkedHashMap<>();
			Map<Item, WoodProfile> structural = new LinkedHashMap<>();
			for (WoodProfile profile : woods) {
				WoodProfile priorPlanks = planks.put(profile.planksIngredient(), profile);
				WoodProfile priorStructural = structural.put(profile.structuralIngredient(), profile);
				if (priorPlanks != null || priorStructural != null) {
					throw new IllegalStateException("Overlapping cabin wood profile ingredient: "
						+ BuiltInRegistries.ITEM.getKey(priorPlanks != null
							? profile.planksIngredient() : profile.structuralIngredient()));
				}
			}
			Map<Item, DoorProfile> doorMap = new LinkedHashMap<>();
			for (DoorProfile profile : doorProfiles) {
				if (doorMap.put(profile.ingredient(), profile) != null) {
					throw new IllegalStateException("Overlapping cabin door profile ingredient: "
						+ BuiltInRegistries.ITEM.getKey(profile.ingredient()));
				}
			}
			woodByPlanks = Collections.unmodifiableMap(planks);
			woodByStructural = Collections.unmodifiableMap(structural);
			doors = Collections.unmodifiableMap(doorMap);
		}
	}
}
