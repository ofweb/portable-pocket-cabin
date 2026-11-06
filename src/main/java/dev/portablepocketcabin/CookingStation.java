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
	OVEN("oven", 6, true);

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

	boolean singleInput() {
		return this == CUTTING || this == SKILLET;
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
