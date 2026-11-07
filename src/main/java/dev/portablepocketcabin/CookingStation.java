package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Optional;

/** Each kind of cooking station has its own recipe type and serializer, sharing the cooking recipe format. */
enum CookingStation {
	CUTTING("cutting", 1, false),
	SKILLET("skillet", 1, true),
	POT("pot", 6, true),
	OVEN("oven", 6, true),
	COUNTER("counter", 6, false),
	STRAINER("strainer", 4, false),
	GRILL("grill", 1, true),
	WOK("wok", 4, true),
	AEBLESKIVE_PAN("aebleskive_pan", 1, true),
	PRESS("press", 1, false),
	MILL("mill", 1, false),
	MIXING_BOWL("mixing_bowl", 6, false),
	MORTAR("mortar", 2, false),
	CHURN("churn", 2, false),
	CHEESE_PRESS("cheese_press", 2, false),
	PROOFING("proofing", 1, false),
	DAIRY_PAN("dairy_pan", 1, false),
	CROCK("crock", 4, false),
	BRINE("brine", 4, false),
	AGEING("ageing", 1, false),
	DRYING("drying", 1, false);

	/** Minecraft time: a day and an hour in ticks, as time stations count them. */
	static final int DAY = 24000;
	static final int HOUR = 1000;

	static final int MAX_WATER = 3;
	static final RecipeBookCategory RECIPE_BOOK_CATEGORY = Registry.register(
		BuiltInRegistries.RECIPE_BOOK_CATEGORY, PortablePocketCabin.id("cooking"), new RecipeBookCategory()
	);

	private final String path;
	private final int ingredientSlots;
	private final boolean heated;
	private final RecipeType<CookingRecipe> type;
	private final RecipeSerializer<CookingRecipe> serializer;

	CookingStation(String path, int ingredientSlots, boolean heated) {
		this.path = path;
		this.ingredientSlots = ingredientSlots;
		this.heated = heated;
		String id = PortablePocketCabin.id(path).toString();
		this.type = Registry.register(BuiltInRegistries.RECIPE_TYPE, PortablePocketCabin.id(path), new RecipeType<>() {
			@Override
			public String toString() {
				return id;
			}
		});
		this.serializer = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, PortablePocketCabin.id(path),
			new RecipeSerializer<>(CookingRecipe.codec(this), CookingRecipe.streamCodec(this)));
	}

	/** Looks up a station's recipe. Stations take one so GameTests can change recipes without a datapack reload. */
	interface Lookup {
		Optional<RecipeHolder<CookingRecipe>> find(ServerLevel level, CookingInput input);
	}

	private static int recipeGeneration;

	/** Changes whenever recipes reload, so stations know their cached recipe lookups are stale. */
	static int recipeGeneration() {
		return recipeGeneration;
	}

	static void recipesReloaded() {
		recipeGeneration++;
	}

	static void register() {
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
			if (success) recipesReloaded();
		});
		for (CookingStation station : values()) {
			RecipeSynchronization.synchronizeRecipeSerializer(station.serializer);
		}
	}

	String path() {
		return path;
	}

	int ingredientSlots() {
		return ingredientSlots;
	}

	boolean heated() {
		return heated;
	}

	boolean holdsWater() {
		return this == POT;
	}

	/** Whether the station holds cooking oil: the pot for deep-frying and the wok for stir-frying. */
	boolean holdsOil() {
		return this == POT || this == WOK;
	}

	/** Whether recipes may name a container, such as the bowls soup is served in or the bottles a strainer or press fills. */
	boolean takesContainer() {
		return switch (this) {
			case POT, WOK, STRAINER, PRESS, MILL, MIXING_BOWL, DAIRY_PAN, CROCK -> true;
			default -> false;
		};
	}

	/** Whether recipes need a positive time. Only the cutting board, which cuts at once, does without. */
	boolean timed() {
		return this != CUTTING;
	}

	boolean singleInput() {
		return switch (this) {
			case CUTTING, SKILLET, GRILL, AEBLESKIVE_PAN, AGEING, DRYING -> true;
			default -> false;
		};
	}

	/** Whether the station's screen has seasoning slots, so that its dish recipes can list the tastes they want. */
	boolean seasoned() {
		return switch (this) {
			case POT, OVEN, WOK, COUNTER, MIXING_BOWL -> true;
			default -> false;
		};
	}

	/** Whether a recipe's time counts tool uses rather than ticks: the spoon, the pestle, and the churn's handle. */
	boolean worked() {
		return this == MIXING_BOWL || this == MORTAR || this == CHURN;
	}

	/**
	 * Whether the station counts game time, so that a batch keeps progressing while its chunk is unloaded or its
	 * cabin is packed (ADR-0013).
	 */
	boolean timeStation() {
		return switch (this) {
			case PROOFING, DAIRY_PAN, CROCK, BRINE, AGEING, DRYING -> true;
			default -> false;
		};
	}

	RecipeType<CookingRecipe> type() {
		return type;
	}

	RecipeSerializer<CookingRecipe> serializer() {
		return serializer;
	}

	Optional<RecipeHolder<CookingRecipe>> find(ServerLevel level, CookingInput input) {
		return level.getServer().getRecipeManager().getRecipeFor(type, input, level);
	}
}
