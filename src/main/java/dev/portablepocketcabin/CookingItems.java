package dev.portablepocketcabin;

import net.fabricmc.fabric.api.registry.CompostableRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;

import java.util.function.Function;
import java.util.function.UnaryOperator;

final class CookingItems {
	static final Item KITCHEN_KNIFE = register("kitchen_knife", properties -> properties.stacksTo(1));
	static final Item COOKBOOK = register("cookbook", properties -> properties.stacksTo(1), CookbookItem::new);
	/** Works the mixing bowl: each use mixes, kneads, whisks, or rubs in once. */
	static final Item SPOON = register("spoon", properties -> properties.stacksTo(1));
	/** Works the mortar: each use pounds or grinds once. */
	static final Item PESTLE = register("pestle", properties -> properties.stacksTo(1));

	/** Wooden skewers, cut from a stick on the cutting board. Meat and vegetables are threaded onto them. */
	static final Item SKEWER = register("skewer", UnaryOperator.identity());
	static final Item RAW_BEEF_SKEWER = register("raw_beef_skewer", UnaryOperator.identity());
	static final Item RAW_PORK_SKEWER = register("raw_pork_skewer", UnaryOperator.identity());
	static final Item RAW_CHICKEN_SKEWER = register("raw_chicken_skewer", UnaryOperator.identity());
	static final Item RAW_MUTTON_SKEWER = register("raw_mutton_skewer", UnaryOperator.identity());
	static final Item GRILLED_BEEF_SKEWER = register("grilled_beef_skewer", properties -> properties.food(new FoodProperties(10, 12.0F, false)));
	static final Item GRILLED_PORK_SKEWER = register("grilled_pork_skewer", properties -> properties.food(new FoodProperties(10, 12.0F, false)));
	static final Item GRILLED_CHICKEN_SKEWER = register("grilled_chicken_skewer", properties -> properties.food(new FoodProperties(9, 10.8F, false)));
	static final Item GRILLED_MUTTON_SKEWER = register("grilled_mutton_skewer", properties -> properties.food(new FoodProperties(9, 10.8F, false)));
	/**
	 * Frying oils, which fill the cooking pot and wok. Sunflower oil is the first. Pumpkin seed oil is not one: toasted,
	 * it smokes at frying heat, so it is kept for dressings.
	 */
	static final net.minecraft.tags.TagKey<Item> COOKING_OILS = net.minecraft.tags.TagKey.create(
		Registries.ITEM, PortablePocketCabin.id("cooking_oils"));

	static final Item CHOPPED_CARROT = register("chopped_carrot", UnaryOperator.identity());
	static final Item DICED_POTATO = register("diced_potato", UnaryOperator.identity());
	static final Item CHOPPED_BEETROOT = register("chopped_beetroot", UnaryOperator.identity());
	static final Item CHOPPED_APPLE = register("chopped_apple", UnaryOperator.identity());
	static final Item SLICED_BROWN_MUSHROOMS = register("sliced_brown_mushrooms", UnaryOperator.identity());
	static final Item SLICED_RED_MUSHROOMS = register("sliced_red_mushrooms", UnaryOperator.identity());
	static final Item GRATED_CARROT = register("grated_carrot", UnaryOperator.identity());
	static final Item SUNFLOWER_SEEDS = register("sunflower_seeds", UnaryOperator.identity());
	static final Item TOASTED_PUMPKIN_SEEDS = register("toasted_pumpkin_seeds", UnaryOperator.identity());
	static final Item SUNFLOWER_OIL = register("sunflower_oil", properties -> properties.stacksTo(16));
	static final Item PUMPKIN_SEED_OIL = register("pumpkin_seed_oil", properties -> properties.stacksTo(16));
	static final Item FRIED_POTATOES = register("fried_potatoes", properties -> properties.food(food(3, 3.6F)));
	/** Rock salt from the ore, ground in the mill. */
	static final Item ROCK_SALT = register("rock_salt", UnaryOperator.identity());
	static final Item SALT = register("salt", UnaryOperator.identity());
	static final Item COARSE_SALT = register("coarse_salt", UnaryOperator.identity());
	/** Salt left wet from boiling seawater down; it dries on the rack. */
	static final Item WET_SALT = register("wet_salt", UnaryOperator.identity());
	/** A bottle filled from water in an ocean. */
	static final Item SEAWATER = register("seawater", properties -> properties.stacksTo(16));
	/** The bitter liquid left from making sea salt. It sets tofu in later features. */
	static final Item NIGARI = register("nigari", properties -> properties.stacksTo(16));
	/** Juice crushed from sugar cane in the mill. */
	static final Item CANE_JUICE = register("cane_juice", properties -> properties.stacksTo(16));
	/** Cane juice boiled down to a thick paste that sets as panela or dries into brown sugar. */
	static final Item CANE_SUGAR_PASTE = register("cane_sugar_paste", UnaryOperator.identity());
	static final Item BROWN_SUGAR = register("brown_sugar", UnaryOperator.identity());
	static final Item PANELA = register("panela", UnaryOperator.identity());
	static final Item SIMPLE_SYRUP = register("simple_syrup", properties -> properties.stacksTo(16));
	static final Item CARAMEL = register("caramel", properties -> properties.food(food(2, 1.2F)));
	/** Sugars a phase 3 recipe accepts wherever it asks for sugar: white sugar and brown sugar. */
	static final net.minecraft.tags.TagKey<Item> SUGARS = net.minecraft.tags.TagKey.create(Registries.ITEM, PortablePocketCabin.id("sugars"));
	/** Cream skimmed from milk settled in the dairy pan, in a bottle. */
	static final Item CREAM = register("cream", properties -> properties.stacksTo(16));
	static final Item SKIMMED_MILK_BUCKET = register("skimmed_milk_bucket",
		properties -> drink(properties, 2, 1.2F).stacksTo(1).usingConvertsTo(Items.BUCKET).craftRemainder(Items.BUCKET));
	/** Milk warmed in the pot, ready to culture or to set with rennet. */
	static final Item WARM_MILK_BUCKET = register("warm_milk_bucket", properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));
	static final Item RAW_BUTTER = register("raw_butter", UnaryOperator.identity());
	static final Item SALTED_BUTTER = register("salted_butter", UnaryOperator.identity());
	/** The liquid left from churning butter. Live buttermilk starts the first yogurt. */
	static final Item BUTTERMILK = register("buttermilk", properties -> drink(properties, 2, 1.2F));
	static final Item WHIPPED_CREAM = register("whipped_cream",
		properties -> properties.stacksTo(16).food(food(3, 1.8F)).usingConvertsTo(Items.BOWL));
	static final Item YOGURT = register("yogurt", properties -> properties.stacksTo(16).food(food(4, 4.8F)).usingConvertsTo(Items.BOWL));
	static final Item CREME_FRAICHE = register("creme_fraiche", properties -> properties.stacksTo(16));
	static final Item STIR_FRIED_VEGETABLES = register("stir_fried_vegetables",
		properties -> properties.stacksTo(16).food(food(8, 9.6F)).usingConvertsTo(Items.BOWL));

	/** The solids a press leaves behind. All three compost, and press cake also feeds cows, sheep, and goats. */
	static final Item APPLE_POMACE = register("apple_pomace", UnaryOperator.identity());
	static final Item PULP = register("pulp", UnaryOperator.identity());
	static final Item PRESS_CAKE = register("press_cake", UnaryOperator.identity());

	static final Item APPLE_JUICE = register("apple_juice", properties -> drink(properties, 3, 1.2F));
	static final Item SWEET_BERRY_JUICE = register("sweet_berry_juice", properties -> drink(properties, 3, 1.2F));
	static final Item MELON_JUICE = register("melon_juice", properties -> drink(properties, 3, 1.2F));
	static final Item CARROT_JUICE = register("carrot_juice", properties -> drink(properties, 3, 1.2F));
	/** Berries simmered with sugar and water, still full of skins and seeds until it is strained. */
	static final Item UNSTRAINED_BERRY_CORDIAL = register("unstrained_berry_cordial", properties -> properties.stacksTo(16));
	/** A concentrate that the B-0046 mixing bowl dilutes into saftevand. Until then it is a sweet drink on its own. */
	static final Item SWEET_BERRY_CORDIAL = register("sweet_berry_cordial", properties -> drink(properties, 4, 1.2F));
	static final Item APPLE_SYRUP = register("apple_syrup", properties -> drink(properties, 6, 1.2F));

	static final Item SAUTEED_MUSHROOMS = register("sauteed_mushrooms", properties -> properties.food(food(3, 3.6F)));
	static final Item FRIED_EGG = register("fried_egg", properties -> properties.food(food(3, 3.6F)));
	static final Item VEGETABLE_SOUP = register("vegetable_soup",
		properties -> properties.stacksTo(16).food(food(7, 8.4F)).usingConvertsTo(Items.BOWL));
	static final Item APPLE_COMPOTE = register("apple_compote",
		properties -> properties.stacksTo(16).food(food(5, 6.0F)).usingConvertsTo(Items.BOWL));

	private CookingItems() {
	}

	static void register() {
		CompostableRegistry.INSTANCE.add(APPLE_POMACE, 0.65F);
		CompostableRegistry.INSTANCE.add(PULP, 0.65F);
		CompostableRegistry.INSTANCE.add(PRESS_CAKE, 0.65F);
	}

	private static FoodProperties food(int nutrition, float saturation) {
		return new FoodProperties(nutrition, saturation, false);
	}

	/** A bottled drink: it can be drunk when full, like a honey bottle, and gives the glass bottle back. */
	private static Item.Properties drink(Item.Properties properties, int nutrition, float saturation) {
		return properties.stacksTo(16).food(new FoodProperties(nutrition, saturation, true), Consumables.DEFAULT_DRINK)
			.usingConvertsTo(Items.GLASS_BOTTLE);
	}

	private static Item register(String name, UnaryOperator<Item.Properties> properties) {
		return register(name, properties, Item::new);
	}

	private static Item register(String name, UnaryOperator<Item.Properties> properties, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, PortablePocketCabin.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.apply(new Item.Properties().setId(key))));
	}
}
