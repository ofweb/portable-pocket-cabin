package dev.portablepocketcabin;

import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import java.util.*;

public final class CabinCookingGameTest {
	private static final List<String> REMOVED_VANILLA_RECIPES = List.of(
		"baked_potato", "cooked_beef", "cooked_chicken", "cooked_cod", "cooked_mutton", "cooked_porkchop",
		"cooked_rabbit", "cooked_salmon", "dried_kelp_from_smelting",
		"baked_potato_from_smoking", "cooked_beef_from_smoking", "cooked_chicken_from_smoking", "cooked_cod_from_smoking",
		"cooked_mutton_from_smoking", "cooked_porkchop_from_smoking", "cooked_rabbit_from_smoking",
		"cooked_salmon_from_smoking", "dried_kelp_from_smoking",
		"baked_potato_from_campfire_cooking", "cooked_beef_from_campfire_cooking", "cooked_chicken_from_campfire_cooking",
		"cooked_cod_from_campfire_cooking", "cooked_mutton_from_campfire_cooking", "cooked_porkchop_from_campfire_cooking",
		"cooked_rabbit_from_campfire_cooking", "cooked_salmon_from_campfire_cooking", "dried_kelp_from_campfire_cooking",
		"bread", "cake", "cookie", "pumpkin_pie", "mushroom_stew", "beetroot_soup",
		"rabbit_stew_from_brown_mushroom", "rabbit_stew_from_red_mushroom"
	);

	@GameTest
	public void starterRecipesMatchTheirStations(GameTestHelper helper) {
		record Case(CookingStation station, List<ItemStack> inputs, ItemStack container, int water, Item result, int count) {
		}
		var bowl = new ItemStack(Items.BOWL, 3);
		var cases = List.of(
			new Case(CookingStation.CUTTING, List.of(new ItemStack(Items.CARROT)), ItemStack.EMPTY, 0, CookingItems.CHOPPED_CARROT, 1),
			new Case(CookingStation.CUTTING, List.of(new ItemStack(Items.RED_MUSHROOM)), ItemStack.EMPTY, 0, CookingItems.SLICED_RED_MUSHROOMS, 1),
			new Case(CookingStation.SKILLET, List.of(new ItemStack(Items.BEEF)), ItemStack.EMPTY, 0, Items.COOKED_BEEF, 1),
			new Case(CookingStation.SKILLET, List.of(new ItemStack(Items.SALMON)), ItemStack.EMPTY, 0, Items.COOKED_SALMON, 1),
			new Case(CookingStation.SKILLET, List.of(new ItemStack(Items.EGG)), ItemStack.EMPTY, 0, CookingItems.FRIED_EGG, 1),
			new Case(CookingStation.POT, List.of(new ItemStack(CookingItems.CHOPPED_BEETROOT, 6)), bowl, 1, Items.BEETROOT_SOUP, 1),
			new Case(CookingStation.POT, List.of(new ItemStack(CookingItems.CHOPPED_CARROT), new ItemStack(CookingItems.DICED_POTATO),
				new ItemStack(CookingItems.CHOPPED_BEETROOT)), bowl, 1, CookingItems.VEGETABLE_SOUP, 3),
			new Case(CookingStation.OVEN, List.of(new ItemStack(Items.WHEAT, 3)), ItemStack.EMPTY, 0, Items.BREAD, 1),
			new Case(CookingStation.OVEN, List.of(new ItemStack(Items.WHEAT, 2), new ItemStack(Items.COCOA_BEANS)), ItemStack.EMPTY, 0, Items.COOKIE, 8),
			new Case(CookingStation.DRYING, List.of(new ItemStack(Items.KELP)), ItemStack.EMPTY, 0, Items.DRIED_KELP, 1)
		);
		for (var test : cases) {
			var input = new CookingInput(test.inputs(), test.container(), test.water());
			var found = test.station().find(helper.getLevel(), input).orElse(null);
			helper.assertTrue(found != null, "A " + test.station().path() + " recipe makes " + test.result());
			var made = found.value().assemble(input);
			helper.assertTrue(made.is(test.result()) && made.getCount() == test.count(),
				"The " + test.station().path() + " recipe makes " + test.count() + " " + test.result());
		}
		var dryPot = new CookingInput(List.of(new ItemStack(CookingItems.CHOPPED_BEETROOT, 6)), bowl, 0);
		helper.assertTrue(CookingStation.POT.find(helper.getLevel(), dryPot).isEmpty(), "Pot recipes need their water");
		var noBowl = new CookingInput(List.of(new ItemStack(CookingItems.CHOPPED_BEETROOT, 6)), ItemStack.EMPTY, 1);
		helper.assertTrue(CookingStation.POT.find(helper.getLevel(), noBowl).isEmpty(), "Pot recipes need their container");
		helper.succeed();
	}

	@GameTest
	public void countedMatchingFitsCakeInSixSlots(GameTestHelper helper) {
		var cake = (CookingRecipe) helper.getLevel().getServer().getRecipeManager()
			.byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse("portable_pocket_cabin_gametest:legacy_cake"))).orElseThrow().value();
		var slots = new ArrayList<>(List.of(new ItemStack(Items.WHEAT, 5), new ItemStack(Items.SUGAR, 2), new ItemStack(Items.EGG),
			new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET)));
		var input = CookingInput.of(slots);
		var assignment = cake.assign(input).orElse(null);
		helper.assertTrue(assignment != null, "Cake fits the six oven slots with three unstackable milk buckets");
		helper.assertTrue(Arrays.equals(cake.consumption(input, assignment), new int[] {3, 2, 1, 1, 1, 1}),
			"Cake takes only its counted items and leaves extra wheat in its slot");
		helper.assertTrue(cake.remainders().equals(List.of(new ItemStackTemplate(Items.BUCKET, 3))),
			"Cake returns three empty buckets");
		slots.set(1, new ItemStack(Items.SUGAR, 1));
		helper.assertTrue(cake.assign(CookingInput.of(slots)).isEmpty(), "A short stack does not satisfy a count");
		slots.set(1, new ItemStack(Items.SUGAR, 2));
		slots.set(5, new ItemStack(Items.DIRT));
		helper.assertTrue(cake.assign(CookingInput.of(slots)).isEmpty(), "An unmatched item in any slot prevents a match");

		var overlapping = new CookingRecipe(CookingStation.OVEN, "", List.of(
			new CookingRecipe.Counted(Ingredient.of(Items.WHEAT, Items.SUGAR), 1),
			new CookingRecipe.Counted(Ingredient.of(Items.WHEAT), 2)
		), Optional.empty(), 0, List.of(), Optional.of(CookingHeat.LOW), 20, new ItemStackTemplate(Items.BREAD));
		var mixed = CookingInput.of(List.of(new ItemStack(Items.WHEAT, 2), new ItemStack(Items.SUGAR)));
		helper.assertTrue(overlapping.assign(mixed).isPresent(), "Overlapping ingredients try every assignment");
		helper.succeed();
	}

	@GameTest
	public void cookingRecipeFormatRoundTripsAndRejectsInvalidRecipes(GameTestHelper helper) {
		var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		var codec = Recipe.CODEC;
		var recipes = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
			.filter(holder -> holder.value() instanceof CookingRecipe).toList();
		helper.assertTrue(recipes.stream().filter(holder -> holder.id().identifier().getNamespace().equals(PortablePocketCabin.MOD_ID)).count() == 406,
			"All 406 cooking recipes load, found " + recipes.size());
		for (var holder : recipes) {
			var recipe = (CookingRecipe) holder.value();
			for (var output : concat(recipe.result(), recipe.remainders())) {
				helper.assertTrue(output.count() <= output.create().getMaxStackSize(),
					"Cooking recipe " + holder.id().identifier() + " makes no more " + output.item().getRegisteredName() + " than one stack holds");
			}
			var json = codec.encodeStart(ops, holder.value()).getOrThrow();
			helper.assertTrue(codec.parse(ops, json).getOrThrow().equals(holder.value()),
				"Cooking recipe " + holder.id().identifier() + " survives a round trip");
		}
		var invalid = List.of(
			"{\"type\":\"portable_pocket_cabin:pot\",\"ingredients\":[{\"item\":\"minecraft:carrot\"}],\"result\":{\"id\":\"minecraft:carrot\"}}",
			"{\"type\":\"portable_pocket_cabin:cutting\",\"ingredients\":[{\"item\":\"minecraft:carrot\"}],\"heat\":\"low\",\"result\":{\"id\":\"minecraft:carrot\"}}",
			"{\"type\":\"portable_pocket_cabin:skillet\",\"ingredients\":[{\"item\":\"minecraft:egg\",\"count\":2}],\"heat\":\"low\",\"time\":20,\"result\":{\"id\":\"minecraft:egg\"}}",
			"{\"type\":\"portable_pocket_cabin:oven\",\"ingredients\":[{\"item\":\"minecraft:wheat\"}],\"water\":1,\"heat\":\"low\",\"time\":20,\"result\":{\"id\":\"minecraft:bread\"}}"
		);
		for (String text : invalid) {
			var parsed = codec.parse(ops, com.google.gson.JsonParser.parseString(text));
			helper.assertTrue(parsed.error().isPresent(), "Invalid cooking recipe is rejected: " + text);
		}
		helper.succeed();
	}

	private static List<ItemStackTemplate> concat(ItemStackTemplate first, List<ItemStackTemplate> rest) {
		var all = new ArrayList<ItemStackTemplate>(rest);
		all.addFirst(first);
		return all;
	}

	@GameTest
	public void fuelTagsSetHeatLevels(GameTestHelper helper) {
		var fuel = helper.getLevel().fuelValues();
		Map<Item, Optional<CookingHeat>> expected = new LinkedHashMap<>();
		expected.put(Items.STICK, Optional.of(CookingHeat.LOW));
		expected.put(Items.OAK_PLANKS, Optional.of(CookingHeat.LOW));
		expected.put(Items.BOWL, Optional.of(CookingHeat.LOW));
		expected.put(Items.OAK_LOG, Optional.of(CookingHeat.MEDIUM));
		expected.put(Items.COAL, Optional.of(CookingHeat.MEDIUM));
		expected.put(Items.CHARCOAL, Optional.of(CookingHeat.MEDIUM));
		expected.put(Items.COAL_BLOCK, Optional.of(CookingHeat.HIGH));
		expected.put(Items.BLAZE_ROD, Optional.of(CookingHeat.HIGH));
		expected.put(Items.LAVA_BUCKET, Optional.of(CookingHeat.HIGH));
		expected.put(Items.CRAFTING_TABLE, Optional.of(CookingHeat.MEDIUM));
		expected.put(Items.STONE, Optional.empty());
		expected.forEach((item, heat) -> helper.assertTrue(CookingHeat.ofFuel(new ItemStack(item), fuel).equals(heat),
			item + " burns at " + heat.map(CookingHeat::getSerializedName).orElse("no heat")));
		helper.succeed();
	}

	@GameTest
	public void vanillaFoodRecipesAreRemoved(GameTestHelper helper) {
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String id : REMOVED_VANILLA_RECIPES) {
			helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(id))).isEmpty(),
				"Vanilla recipe " + id + " is removed");
		}
		for (String id : List.of("golden_carrot", "golden_apple", "suspicious_stew_from_poppy", "dried_kelp", "honey_bottle")) {
			helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(id))).isPresent(),
				"Vanilla recipe " + id + " is kept");
		}
		var level = helper.getLevel();
		helper.assertTrue(recipes.getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(new ItemStack(Items.BEEF)), level).isEmpty(),
			"A campfire no longer accepts raw beef");
		helper.assertTrue(recipes.getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(new ItemStack(Items.POTATO)), level).isEmpty(),
			"A smoker no longer cooks food");
		helper.assertTrue(recipes.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), level).isPresent(),
			"Furnaces still smelt what is not food");
		helper.succeed();
	}

	@GameTest
	public void newDishesFeedAndReturnTheirBowls(GameTestHelper helper) {
		var egg = new ItemStack(CookingItems.FRIED_EGG);
		var food = egg.get(DataComponents.FOOD);
		helper.assertTrue(food != null && food.nutrition() == 3 && Math.abs(food.saturation() - 3.6F) < 0.001F,
			"A fried egg gives 3 hunger and 3.6 saturation");
		var soup = new ItemStack(CookingItems.VEGETABLE_SOUP);
		helper.assertTrue(soup.getMaxStackSize() == 16 && soup.get(DataComponents.USE_REMAINDER) != null
			&& soup.get(DataComponents.FOOD).nutrition() == 7, "Vegetable soup stacks to 16, gives 7 hunger, and returns its bowl");
		helper.assertTrue(new ItemStack(CookingItems.CHOPPED_CARROT).get(DataComponents.FOOD) == null,
			"Prepared ingredients are not edible");
		helper.succeed();
	}

	@GameTest
	public void cookbookCountsInventoryAndHouseholdStorage(GameTestHelper helper) {
		var recipes = CookbookAvailability.recipes(helper.getLevel().getServer().getRecipeManager().getRecipes());
		Identifier bread = Identifier.parse("portable_pocket_cabin_gametest:legacy_bread");
		Identifier soup = PortablePocketCabin.id("cooking/pot/vegetable_soup");
		Identifier cake = Identifier.parse("portable_pocket_cabin_gametest:legacy_cake");
		helper.assertTrue(CookbookAvailability.available(recipes, List.of(new ItemStack(Items.WHEAT, 3))).contains(bread),
			"Three wheat make bread");
		helper.assertTrue(!CookbookAvailability.available(recipes, List.of(new ItemStack(Items.WHEAT, 2))).contains(bread),
			"Two wheat do not");
		helper.assertTrue(CookbookAvailability.available(recipes, List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT, 2))).contains(bread),
			"Wheat counts across stacks");
		var named = new ItemStack(Items.WHEAT, 3);
		named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Seed wheat"));
		helper.assertTrue(!CookbookAvailability.available(recipes, List.of(named)).contains(bread), "Named stacks do not count");
		var vegetables = List.of(new ItemStack(CookingItems.CHOPPED_CARROT), new ItemStack(CookingItems.DICED_POTATO),
			new ItemStack(CookingItems.CHOPPED_BEETROOT));
		helper.assertTrue(!CookbookAvailability.available(recipes, java.util.stream.Stream.concat(vegetables.stream(), java.util.stream.Stream.of(new ItemStack(Items.BOWL, 2))).toList()).contains(soup),
			"Soup needs its three bowls");
		helper.assertTrue(CookbookAvailability.available(recipes, java.util.stream.Stream.concat(vegetables.stream(), java.util.stream.Stream.of(new ItemStack(Items.BOWL, 3))).toList()).contains(soup),
			"Soup is ready with three bowls");
		helper.assertTrue(CookbookAvailability.available(recipes, List.of(new ItemStack(Items.WHEAT, 3), new ItemStack(Items.SUGAR, 2),
			new ItemStack(Items.EGG), new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET))).contains(cake),
			"Cake counts three single milk buckets");

		var registry = new CabinRegistry();
		var cabin = CabinEnchantingGameTest.cabin(registry);
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(
			new CabinStorageState(true, 1, List.of(new ItemStack(Items.WHEAT, 2)))));
		var inventory = List.of(new ItemStack(Items.WHEAT));
		helper.assertTrue(CookbookAvailability.available(recipes, CookbookAvailability.sources(cabin, cabin.owner(), inventory)).contains(bread),
			"Household storage adds to the inventory");
		helper.assertTrue(!CookbookAvailability.available(recipes, CookbookAvailability.sources(cabin, UUID.randomUUID(), inventory)).contains(bread),
			"A guest's cookbook does not count the cabin's storage");
		helper.succeed();
	}
}
