package dev.portablepocketcabin;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The B-0050 cooking plants. Blocks are registered here from a fixed list, because Minecraft registers blocks at
 * startup; their growing needs and wild placement come from data. Each delivery adds its region's plants.
 */
final class CookingPlants {
	/** Every plant block, by plant name, in registration order. */
	static final Map<String, Block> PLANTS = new LinkedHashMap<>();
	/** Every item the plants give or are planted from, in registration order, for the creative tab. */
	static final List<Item> ITEMS = new ArrayList<>();

	// Delivery 1: field crops of the plains, meadows, and forests.
	static final Item BARLEY = ingredient("barley", null);
	static final Item RYE = ingredient("rye", null);
	static final Item CORN = ingredient("corn", null);
	static final Item ASPARAGUS = ingredient("asparagus", food(2, 1.2F));
	static final Item CABBAGE = ingredient("cabbage", food(2, 1.2F));
	static final Item COURGETTE = ingredient("courgette", food(1, 0.6F));
	static final Item LETTUCE = ingredient("lettuce", food(2, 1.2F));
	static final Item RADISH = ingredient("radish", food(1, 0.6F));
	static final Item SPRING_ONION = ingredient("spring_onion", null);
	static final Item CILANTRO = ingredient("cilantro", null);
	static final Item DILL = ingredient("dill", null);
	static final Item PARSLEY = ingredient("parsley", null);

	static final Block BARLEY_CROP = fieldCrop("barley", "barley_seeds");
	static final Block RYE_CROP = fieldCrop("rye", "rye_seeds");
	static final Block CORN_CROP = fieldCrop("corn", "corn_seeds");
	static final Block ASPARAGUS_CROP = fieldCrop("asparagus", "asparagus_crown");
	static final Block CABBAGE_CROP = fieldCrop("cabbage", "cabbage_seeds");
	static final Block COURGETTE_CROP = fieldCrop("courgette", "courgette_seeds");
	static final Block LETTUCE_CROP = fieldCrop("lettuce", "lettuce_seeds");
	static final Block RADISH_CROP = fieldCrop("radish", "radish_seeds");
	static final Block SPRING_ONION_CROP = fieldCrop("spring_onion", "spring_onion_seeds");
	/** Cilantro gives its leaves when ripe; left to flower, it gives coriander seed, which is also what it grows from. */
	static final Block CILANTRO_CROP = fieldCrop("cilantro", "coriander_seed");
	static final Block DILL_CROP = fieldCrop("dill", "dill_seeds");
	static final Block PARSLEY_CROP = fieldCrop("parsley", "parsley_seeds");
	// Crops that replant from what they give: bulbs, beans, and seeds that are themselves the ingredient.
	static final Block GARLIC_CROP = fieldCrop("garlic", "garlic");
	static final Block ONION_CROP = fieldCrop("onion", "onion");
	static final Block RED_ONION_CROP = fieldCrop("red_onion", "red_onion");
	static final Block SHALLOT_CROP = fieldCrop("shallot", "shallot");
	static final Block WHITE_BEAN_CROP = fieldCrop("white_bean", "white_bean");
	static final Block RAPESEED_CROP = fieldCrop("rapeseed", "rapeseed");
	static final Block MUSTARD_CROP = fieldCrop("mustard", "mustard_seed");
	static final Block CARAWAY_CROP = fieldCrop("caraway", "caraway_seed");

	private CookingPlants() {
	}

	static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			for (Item item : ITEMS) entries.accept(item);
		});
	}

	static Item item(String name) {
		return BuiltInRegistries.ITEM.getValue(PortablePocketCabin.id(name));
	}

	private static FoodProperties food(int nutrition, float saturation) {
		return new FoodProperties(nutrition, saturation, false);
	}

	private static Item ingredient(String name, FoodProperties food) {
		return registerItem(name, properties -> new Item(food == null ? properties : properties.food(food)));
	}

	/** A field crop block named after its plant, planted from {@code seed}, which may be the ingredient itself. */
	private static Block fieldCrop(String plant, String seed) {
		Block[] block = new Block[1];
		Item[] seeds = new Item[1];
		block[0] = registerBlock(plant, properties -> new FieldCropBlock(properties, () -> seeds[0]), BlockBehaviour.Properties.of()
			.noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY));
		seeds[0] = registerItem(seed, properties -> new BlockItem(block[0], properties.useItemDescriptionPrefix()));
		PLANTS.put(plant, block[0]);
		return block[0];
	}

	private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, PortablePocketCabin.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item registerItem(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, PortablePocketCabin.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
		ITEMS.add(item);
		return item;
	}
}
