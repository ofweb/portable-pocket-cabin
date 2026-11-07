package dev.portablepocketcabin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Items lying on a cooking surface, like a campfire's: the skillet, the grill, and the æbleskive pan. Each spot
 * holds one item and cooks it on its own at the surface's heat. A spot's recipe is looked up again when its item
 * changes or recipes reload. With turning, a spot stops at half its time until it is turned.
 */
final class SurfaceCooking {
	private final int spots;
	private final boolean turning;
	private final int[] progress;
	private final boolean[] turned;
	private final CookingRecipe[] recipes;
	private final String[] recipeIds;
	private final boolean[] looked;
	private final boolean[] loaded;
	private int generation;
	private CookingStation.Lookup lookup;
	/** Whether a drying rack has open sky above it, which sun recipes need and shade recipes refuse. */
	private boolean sky = true;

	/** What a tick did: whether any spot finished, and whether any spot cooked. */
	record Tick(boolean finished, boolean cooking) {
	}

	SurfaceCooking(CookingStation station, int spots, boolean turning) {
		this.spots = spots;
		this.turning = turning;
		this.progress = new int[spots];
		this.turned = new boolean[spots];
		this.recipes = new CookingRecipe[spots];
		this.recipeIds = new String[spots];
		this.looked = new boolean[spots];
		this.loaded = new boolean[spots];
		this.lookup = station::find;
	}

	/** Replaces the recipe lookup, for GameTests. */
	void lookup(CookingStation.Lookup lookup) {
		this.lookup = lookup;
		invalidate();
	}

	void invalidate() {
		Arrays.fill(looked, false);
	}

	/** Sets whether the surface has open sky above it. A change looks the recipes up again. */
	void sky(boolean sky) {
		if (this.sky != sky) {
			this.sky = sky;
			invalidate();
		}
	}

	private CookingInput input(ItemStack stack) {
		return new CookingInput(List.of(stack), ItemStack.EMPTY, 0, 0, sky);
	}

	int progress(int spot) {
		return progress[spot];
	}

	/** Whether the spot is half done and waits to be turned. */
	boolean needsTurning(int spot) {
		return turning && !turned[spot] && recipes[spot] != null && progress[spot] >= recipes[spot].time() / 2;
	}

	Optional<CookingRecipe> recipe(ServerLevel level, List<ItemStack> items, int spot) {
		if (generation != CookingStation.recipeGeneration()) {
			invalidate();
			generation = CookingStation.recipeGeneration();
		}
		if (!looked[spot]) {
			ItemStack stack = items.get(spot);
			var found = stack.isEmpty() ? null : lookup.find(level, input(stack)).orElse(null);
			String id = found == null ? null : found.id().identifier().toString();
			if (!Objects.equals(recipeIds[spot], id) && !(loaded[spot] && recipeIds[spot] == null && id != null)) {
				progress[spot] = 0;
				turned[spot] = false;
			}
			recipes[spot] = found == null ? null : found.value();
			recipeIds[spot] = id;
			loaded[spot] = false;
			looked[spot] = true;
		}
		return Optional.ofNullable(recipes[spot]);
	}

	/** Whether the surface has a recipe for the item. */
	boolean cooks(ServerLevel level, ItemStack held) {
		return !held.isEmpty() && lookup.find(level, input(held.copyWithCount(1))).isPresent();
	}

	/** Puts one item on a free spot if the surface can cook it. */
	boolean add(ServerLevel level, List<ItemStack> items, ItemStack held) {
		if (!cooks(level, held)) return false;
		for (int spot = 0; spot < spots; spot++) {
			if (items.get(spot).isEmpty()) {
				items.set(spot, held.split(1));
				reset(spot);
				return true;
			}
		}
		return false;
	}

	/** Takes a finished item first, otherwise the last item placed. Returns empty when the surface is bare. */
	ItemStack take(ServerLevel level, List<ItemStack> items) {
		int chosen = -1;
		for (int spot = 0; spot < spots; spot++) {
			if (items.get(spot).isEmpty()) continue;
			chosen = spot;
			if (recipe(level, items, spot).isEmpty()) break;
		}
		if (chosen < 0) return ItemStack.EMPTY;
		ItemStack taken = items.get(chosen);
		items.set(chosen, ItemStack.EMPTY);
		reset(chosen);
		return taken;
	}

	/** Turns every spot that waits to be turned. Returns whether any did. */
	boolean turn() {
		boolean any = false;
		for (int spot = 0; spot < spots; spot++) {
			if (needsTurning(spot)) {
				turned[spot] = true;
				any = true;
			}
		}
		return any;
	}

	/** Whether any spot holds something a recipe would cook. */
	boolean anyRecipe(ServerLevel level, List<ItemStack> items) {
		for (int spot = 0; spot < spots; spot++) if (recipe(level, items, spot).isPresent()) return true;
		return false;
	}

	/** Advances every spot whose recipe wants this heat. A spot that waits to be turned holds still. */
	Tick tick(ServerLevel level, List<ItemStack> items, Optional<CookingHeat> heat) {
		return tick(level, items, heat, 1);
	}

	/**
	 * Advances every spot by {@code elapsed} ticks. Time stations pass the game time since their last tick, and each
	 * spot finishes at most its current item, so catch-up never starts the next one (ADR-0013).
	 */
	Tick tick(ServerLevel level, List<ItemStack> items, Optional<CookingHeat> heat, long elapsed) {
		boolean finished = false, cooking = false;
		if (elapsed <= 0) return new Tick(false, false);
		for (int spot = 0; spot < spots; spot++) {
			CookingRecipe recipe = recipe(level, items, spot).orElse(null);
			if (recipe == null || !heat.equals(recipe.heat()) || needsTurning(spot)) continue;
			cooking = true;
			int limit = turning && !turned[spot] ? recipe.time() / 2 : recipe.time();
			progress[spot] = (int) Math.min(limit, progress[spot] + elapsed);
			if (progress[spot] >= recipe.time()) {
				items.set(spot, recipe.result().create());
				reset(spot);
				finished = true;
			}
		}
		return new Tick(finished, cooking);
	}

	private void reset(int spot) {
		progress[spot] = 0;
		turned[spot] = false;
		looked[spot] = false;
	}

	void save(ValueOutput output) {
		output.putIntArray("progress", progress);
		if (turning) {
			int[] flags = new int[spots];
			for (int spot = 0; spot < spots; spot++) flags[spot] = turned[spot] ? 1 : 0;
			output.putIntArray("turned", flags);
		}
		for (int spot = 0; spot < spots; spot++) {
			if (recipeIds[spot] != null) output.putString("recipe_" + spot, recipeIds[spot]);
		}
	}

	void load(ValueInput input) {
		int[] saved = input.getIntArray("progress").orElse(new int[spots]);
		System.arraycopy(saved, 0, progress, 0, Math.min(saved.length, spots));
		int[] flags = input.getIntArray("turned").orElse(new int[spots]);
		for (int spot = 0; spot < spots; spot++) {
			turned[spot] = spot < flags.length && flags[spot] != 0;
			recipeIds[spot] = input.getString("recipe_" + spot).orElse(null);
			loaded[spot] = true;
			looked[spot] = false;
		}
	}
}
