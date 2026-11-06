package dev.portablepocketcabin;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Function;
import java.util.function.UnaryOperator;

final class CookingItems {
	static final Item KITCHEN_KNIFE = register("kitchen_knife", properties -> properties.stacksTo(1));
	static final Item COOKBOOK = register("cookbook", properties -> properties.stacksTo(1), CookbookItem::new);

	static final Item CHOPPED_CARROT = register("chopped_carrot", UnaryOperator.identity());
	static final Item DICED_POTATO = register("diced_potato", UnaryOperator.identity());
	static final Item CHOPPED_BEETROOT = register("chopped_beetroot", UnaryOperator.identity());
	static final Item CHOPPED_APPLE = register("chopped_apple", UnaryOperator.identity());
	static final Item SLICED_BROWN_MUSHROOMS = register("sliced_brown_mushrooms", UnaryOperator.identity());
	static final Item SLICED_RED_MUSHROOMS = register("sliced_red_mushrooms", UnaryOperator.identity());

	static final Item SAUTEED_MUSHROOMS = register("sauteed_mushrooms", properties -> properties.food(food(3, 3.6F)));
	static final Item FRIED_EGG = register("fried_egg", properties -> properties.food(food(3, 3.6F)));
	static final Item VEGETABLE_SOUP = register("vegetable_soup",
		properties -> properties.stacksTo(16).food(food(7, 8.4F)).usingConvertsTo(Items.BOWL));
	static final Item APPLE_COMPOTE = register("apple_compote",
		properties -> properties.stacksTo(16).food(food(5, 6.0F)).usingConvertsTo(Items.BOWL));

	private CookingItems() {
	}

	static void register() {
		// Triggers static registration during mod initialization.
	}

	private static FoodProperties food(int nutrition, float saturation) {
		return new FoodProperties(nutrition, saturation, false);
	}

	private static Item register(String name, UnaryOperator<Item.Properties> properties) {
		return register(name, properties, Item::new);
	}

	private static Item register(String name, UnaryOperator<Item.Properties> properties, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, PortablePocketCabin.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.apply(new Item.Properties().setId(key))));
	}
}
