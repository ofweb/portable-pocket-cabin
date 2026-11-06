package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A skillet holds up to four items, like a campfire, and cooks each one on its own while heated. */
final class SkilletBlockEntity extends DisplayStationBlockEntity implements CookingVessel {
	static final int SLOTS = 4;
	private final int[] progress = new int[SLOTS];
	private final CookingRecipe[] recipes = new CookingRecipe[SLOTS];
	private final String[] recipeIds = new String[SLOTS];
	private final boolean[] looked = new boolean[SLOTS];
	private final boolean[] loaded = new boolean[SLOTS];
	private int generation;
	private CookingStation.Lookup lookup = CookingStation.SKILLET::find;

	SkilletBlockEntity(BlockPos pos, BlockState state) {
		super(CookingBlocks.SKILLET_ENTITY, pos, state, SLOTS);
	}

	/** Replaces the recipe lookup, for GameTests. */
	void lookup(CookingStation.Lookup lookup) {
		this.lookup = lookup;
		Arrays.fill(looked, false);
	}

	int progress(int slot) {
		return progress[slot];
	}

	private Optional<CookingRecipe> recipe(ServerLevel level, int slot) {
		if (generation != CookingStation.recipeGeneration()) {
			Arrays.fill(looked, false);
			generation = CookingStation.recipeGeneration();
		}
		if (!looked[slot]) {
			ItemStack stack = items.get(slot);
			var found = stack.isEmpty() ? null
				: lookup.find(level, CookingInput.of(List.of(stack))).orElse(null);
			String id = found == null ? null : found.id().identifier().toString();
			if (!Objects.equals(recipeIds[slot], id) && !(loaded[slot] && recipeIds[slot] == null && id != null)) {
				progress[slot] = 0;
			}
			recipes[slot] = found == null ? null : found.value();
			recipeIds[slot] = id;
			loaded[slot] = false;
			looked[slot] = true;
		}
		return Optional.ofNullable(recipes[slot]);
	}

	/** Puts one item on a free spot if the skillet can cook it. */
	boolean add(ServerLevel level, ItemStack held) {
		if (lookup.find(level, CookingInput.of(List.of(held.copyWithCount(1)))).isEmpty()) return false;
		for (int slot = 0; slot < SLOTS; slot++) {
			if (items.get(slot).isEmpty()) {
				items.set(slot, held.split(1));
				progress[slot] = 0;
				looked[slot] = false;
				changedForClients();
				return true;
			}
		}
		return false;
	}

	/** Takes a finished item first, otherwise the last item placed. */
	ItemStack take(ServerLevel level) {
		int chosen = -1;
		for (int slot = 0; slot < SLOTS; slot++) {
			if (items.get(slot).isEmpty()) continue;
			if (recipe(level, slot).isEmpty()) {
				chosen = slot;
				break;
			}
			chosen = slot;
		}
		if (chosen < 0) return ItemStack.EMPTY;
		ItemStack taken = items.get(chosen);
		items.set(chosen, ItemStack.EMPTY);
		progress[chosen] = 0;
		looked[chosen] = false;
		changedForClients();
		return taken;
	}

	@Override
	public boolean wantsHeat(ServerLevel level) {
		for (int slot = 0; slot < SLOTS; slot++) {
			if (recipe(level, slot).isPresent()) return true;
		}
		return false;
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SkilletBlockEntity skillet) {
		var heat = CookingVessel.heatAt(level, pos);
		boolean finished = false, cooking = false;
		for (int slot = 0; slot < SLOTS; slot++) {
			CookingRecipe recipe = skillet.recipe(level, slot).orElse(null);
			if (recipe == null || !heat.equals(recipe.heat())) continue;
			cooking = true;
			if (++skillet.progress[slot] >= recipe.time()) {
				skillet.items.set(slot, recipe.result().create());
				skillet.progress[slot] = 0;
				skillet.looked[slot] = false;
				finished = true;
			}
		}
		if (finished) skillet.changedForClients();
		else if (cooking) skillet.saveChanged();
	}

	@Override
	protected void contentsChanged() {
		Arrays.fill(looked, false);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putIntArray("progress", progress);
		for (int slot = 0; slot < SLOTS; slot++) {
			if (recipeIds[slot] != null) output.putString("recipe_" + slot, recipeIds[slot]);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		int[] saved = input.getIntArray("progress").orElse(new int[SLOTS]);
		System.arraycopy(saved, 0, progress, 0, Math.min(saved.length, SLOTS));
		for (int slot = 0; slot < SLOTS; slot++) {
			recipeIds[slot] = input.getString("recipe_" + slot).orElse(null);
			loaded[slot] = true;
			looked[slot] = false;
		}
	}
}
