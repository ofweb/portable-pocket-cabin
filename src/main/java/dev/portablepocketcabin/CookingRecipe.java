package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * One cooking recipe at one station. Ingredients are counted and matched shapelessly: each filled slot
 * belongs to one entry, and an entry matches when its slots hold at least its count.
 */
record CookingRecipe(
	CookingStation station,
	String technique,
	List<Counted> ingredients,
	Optional<Counted> container,
	int water,
	List<ItemStackTemplate> remainders,
	Optional<CookingHeat> heat,
	int time,
	ItemStackTemplate result
) implements Recipe<CookingInput> {
	record Counted(Ingredient ingredient, int count) {
		static final Codec<Counted> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Ingredient.CODEC.fieldOf("item").forGetter(Counted::ingredient),
			Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(Counted::count)
		).apply(instance, Counted::new));
		static final StreamCodec<RegistryFriendlyByteBuf, Counted> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, Counted::ingredient,
			ByteBufCodecs.VAR_INT, Counted::count,
			Counted::new
		);
	}

	CookingRecipe {
		ingredients = List.copyOf(ingredients);
		remainders = List.copyOf(remainders);
	}

	static MapCodec<CookingRecipe> codec(CookingStation station) {
		MapCodec<CookingRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.optionalFieldOf("technique", "").forGetter(CookingRecipe::technique),
			Counted.CODEC.listOf(1, station.ingredientSlots()).fieldOf("ingredients").forGetter(CookingRecipe::ingredients),
			Counted.CODEC.optionalFieldOf("container").forGetter(CookingRecipe::container),
			Codec.intRange(0, CookingStation.MAX_WATER).optionalFieldOf("water", 0).forGetter(CookingRecipe::water),
			ItemStackTemplate.CODEC.listOf().optionalFieldOf("remainders", List.of()).forGetter(CookingRecipe::remainders),
			CookingHeat.CODEC.optionalFieldOf("heat").forGetter(CookingRecipe::heat),
			Codec.intRange(0, 72000).optionalFieldOf("time", 0).forGetter(CookingRecipe::time),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(CookingRecipe::result)
		).apply(instance, (technique, ingredients, container, water, remainders, heat, time, result) ->
			new CookingRecipe(station, technique, ingredients, container, water, remainders, heat, time, result)));
		return codec.validate(recipe -> recipe.validate().map(error -> DataResult.<CookingRecipe>error(() -> error))
			.orElseGet(() -> DataResult.success(recipe)));
	}

	static StreamCodec<RegistryFriendlyByteBuf, CookingRecipe> streamCodec(CookingStation station) {
		return StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, CookingRecipe::technique,
			Counted.STREAM_CODEC.apply(ByteBufCodecs.list()), CookingRecipe::ingredients,
			ByteBufCodecs.optional(Counted.STREAM_CODEC), CookingRecipe::container,
			ByteBufCodecs.VAR_INT, CookingRecipe::water,
			ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), CookingRecipe::remainders,
			ByteBufCodecs.optional(CookingHeat.STREAM_CODEC), CookingRecipe::heat,
			ByteBufCodecs.VAR_INT, CookingRecipe::time,
			ItemStackTemplate.STREAM_CODEC, CookingRecipe::result,
			(technique, ingredients, container, water, remainders, heat, time, result) ->
				new CookingRecipe(station, technique, ingredients, container, water, remainders, heat, time, result)
		);
	}

	private Optional<String> validate() {
		if (station.heated() != heat.isPresent()) {
			return Optional.of(station.heated() ? "A " + station.name() + " recipe needs a heat level"
				: "A " + station.name() + " recipe has no heat level");
		}
		if (station.heated() && time <= 0) return Optional.of("A heated recipe needs a positive time");
		if (!station.holdsWater() && (water > 0 || container.isPresent())) {
			return Optional.of("Only cooking pot recipes use water or a container");
		}
		if (station.singleInput() && (ingredients.size() != 1 || ingredients.getFirst().count() != 1)) {
			return Optional.of("A " + station.name() + " recipe takes exactly one item");
		}
		return Optional.empty();
	}

	/**
	 * Assigns each filled ingredient slot to the entry it satisfies, or returns empty when the input
	 * does not match. With at most six slots, every assignment is tried.
	 */
	Optional<int[]> assign(CookingInput input) {
		if (input.water() < water) return Optional.empty();
		if (container.isPresent()) {
			Counted needed = container.get();
			if (!needed.ingredient().test(input.container()) || input.container().getCount() < needed.count()) {
				return Optional.empty();
			}
		}
		int[] assignment = new int[input.size()];
		Arrays.fill(assignment, -1);
		int[] supplied = new int[ingredients.size()];
		return search(input, 0, assignment, supplied) ? Optional.of(assignment) : Optional.empty();
	}

	private boolean search(CookingInput input, int slot, int[] assignment, int[] supplied) {
		if (slot == input.size()) {
			for (int entry = 0; entry < ingredients.size(); entry++) {
				if (supplied[entry] < ingredients.get(entry).count()) return false;
			}
			return true;
		}
		ItemStack stack = input.getItem(slot);
		if (stack.isEmpty()) return search(input, slot + 1, assignment, supplied);
		for (int entry = 0; entry < ingredients.size(); entry++) {
			if (!ingredients.get(entry).ingredient().test(stack)) continue;
			assignment[slot] = entry;
			supplied[entry] += stack.getCount();
			if (search(input, slot + 1, assignment, supplied)) return true;
			supplied[entry] -= stack.getCount();
			assignment[slot] = -1;
		}
		return false;
	}

	/** How many items to take from each ingredient slot, given a matching assignment. */
	int[] consumption(CookingInput input, int[] assignment) {
		int[] taken = new int[input.size()];
		int[] remaining = ingredients.stream().mapToInt(Counted::count).toArray();
		for (int slot = 0; slot < input.size(); slot++) {
			int entry = assignment[slot];
			if (entry < 0) continue;
			int take = Math.min(remaining[entry], input.getItem(slot).getCount());
			taken[slot] = take;
			remaining[entry] -= take;
		}
		return taken;
	}

	@Override
	public boolean matches(CookingInput input, Level level) {
		return assign(input).isPresent();
	}

	@Override
	public ItemStack assemble(CookingInput input) {
		return result.create();
	}

	/** Cooking recipes stay out of the vanilla recipe book; the cookbook and station panels show them. */
	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeSerializer<CookingRecipe> getSerializer() {
		return station.serializer();
	}

	@Override
	public RecipeType<CookingRecipe> getType() {
		return station.type();
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return CookingStation.RECIPE_BOOK_CATEGORY;
	}
}
