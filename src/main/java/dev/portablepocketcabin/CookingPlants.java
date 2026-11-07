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
	/** Ground a wild plant can stand on even when it is not the plant's farming soil: grass, dirt, moss, mud, and farmland. */
	static final net.minecraft.tags.TagKey<Block> WILD_GROUND = net.minecraft.tags.TagKey.create(Registries.BLOCK,
		net.minecraft.resources.Identifier.withDefaultNamespace("supports_vegetation"));
	/** Every plant block, by plant name, in registration order. */
	static final Map<String, Block> PLANTS = new LinkedHashMap<>();
	/** What each plant gives, by plant name, for the cookbook's Garden pages. */
	static final Map<String, List<Item>> GIVES = new LinkedHashMap<>();
	/** What each plant is planted from: its seeds, cutting, sapling, or the produce itself. */
	static final Map<String, Item> PLANTED_FROM = new LinkedHashMap<>();
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

	// Delivery 1: climbing plants on trellises.
	static final Block TRELLIS = registerBlockWithItem("trellis", TrellisBlock::new, BlockBehaviour.Properties.of()
		.strength(0.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY));
	static final Item CUCUMBER = ingredient("cucumber", food(2, 1.2F));
	static final Item WHITE_GRAPE = ingredient("white_grape", food(2, 1.2F));
	static final Item HOPS = ingredient("hops", null);
	static final Block CUCUMBER_VINE = trellisCrop("cucumber_vine", "cucumber_seeds", () -> CUCUMBER);
	static final Block WHITE_GRAPE_VINE = trellisCrop("white_grape_vine", "white_grape_cutting", () -> WHITE_GRAPE);
	static final Block HOP_VINE = trellisCrop("hop_vine", "hop_rhizome", () -> HOPS);

	// Delivery 1: perennial bushes that regrow after picking. Each is planted from what it gives, like sweet berries.
	static final Block THYME_BUSH = selfBush("thyme_bush", "thyme", null);
	static final Block MARJORAM_BUSH = selfBush("marjoram_bush", "marjoram", null);
	static final Block STRAWBERRY_BUSH = selfBush("strawberry_bush", "strawberry", food(2, 1.2F));
	static final Block RASPBERRY_BUSH = selfBush("raspberry_bush", "raspberry", food(2, 1.2F));
	static final Block OYSTER_MUSHROOM = selfBush("oyster_mushrooms", "oyster_mushroom", null);
	/** The elder flowers, then fruits: elderflower at its flowering stage, elderberries when ripe. */
	static final Item ELDERFLOWER = ingredient("elderflower", null);
	static final Item ELDERBERRY = ingredient("elderberry", null);
	static final Block ELDER_BUSH = bush("elder_bush", "elder_cutting", () -> ELDERFLOWER, () -> ELDERBERRY);
	// Delivery 1: fruit trees. Each species has a sapling and fruiting leaves on a vanilla trunk.
	static final Item PLUM = ingredient("plum", food(3, 1.8F));
	static final Block PLUM_LEAVES = fruitTree("plum", () -> PLUM);

	/** Delivery 1 products: what the plains, meadow, and forest plants complete in the recipe catalogue. */
	static final List<Item> PRODUCTS = new ArrayList<>();
	static {
		String[] plain = {"rye_flour", "cracked_rye", "soaked_corn", "wet_cornstarch", "cornstarch", "corn_kernels", "corn_husk", "dried_corn_husks",
			"dried_corn", "wood_ash", "hominy", "masa", "cooked_corn", "dried_cooked_corn", "masarepa", "raw_tortilla", "roasted_barley",
			"sprouted_barley", "malted_barley", "roasted_malt", "ground_malt", "rugbrod_dough", "proofed_rugbrod_dough", "spatzle_dough",
			"ground_mustard", "dried_white_beans", "shredded_cabbage", "crushed_white_grapes", "dried_rosehip", "chopped_onion",
			"grated_potato", "potato_batter", "caramelised_onion"};
		for (String name : plain) PRODUCTS.add(ingredient(name, null));
		String[] bottles = {"lye_solution", "prepared_mustard", "mayonnaise", "white_grape_must", "white_wine", "wine_vinegar", "wort", "beer",
			"strawberry_jam", "raspberry_jam"};
		for (String name : bottles) PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16))));
		PRODUCTS.add(ingredient("toasted_corn", food(2, 1.2F)));
		PRODUCTS.add(ingredient("corn_tortilla", food(2, 1.2F)));
		PRODUCTS.add(ingredient("rugbrod", food(5, 6.0F)));
		PRODUCTS.add(ingredient("spatzle", food(5, 6.0F)));
		PRODUCTS.add(ingredient("sauerkraut", food(2, 2.4F)));
		PRODUCTS.add(ingredient("gherkins", food(2, 1.6F)));
		PRODUCTS.add(ingredient("kartoffelpuffer", food(5, 6.0F)));
		PRODUCTS.add(registerItem("kasespatzle", properties -> new Item(properties.stacksTo(16).food(food(9, 10.8F)))));
		PRODUCTS.add(registerItem("malzkaffee", properties -> new Item(properties.stacksTo(16)
			.food(new FoodProperties(2, 1.2F, true), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
			.usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE))));
		PRODUCTS.add(registerItem("mugicha", properties -> new Item(properties.stacksTo(16)
			.food(new FoodProperties(1, 0.6F, true), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
			.usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE))));
	}

	/** Picked from vanilla rose bushes with shears. */
	static final Item ROSEHIP = ingredient("rosehip", null);

	private CookingPlants() {
	}

	/** The name a plant's wild feature and biome tag use: the plant, or the tree for fruiting leaves. */
	static String wildName(String plant) {
		return plant.endsWith("_leaves") ? plant.substring(0, plant.length() - "_leaves".length()) : plant;
	}

	static void register() {
		// Each plant grows wild in the biomes of its tag, grows_wild/<plant>, through its placed feature, wild_<plant>.
		for (String plant : PLANTS.keySet()) {
			String name = wildName(plant);
			net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
				net.fabricmc.fabric.api.biome.v1.BiomeSelectors.tag(net.minecraft.tags.TagKey.create(Registries.BIOME, PortablePocketCabin.id("grows_wild/" + name))),
				net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION,
				ResourceKey.create(Registries.PLACED_FEATURE, PortablePocketCabin.id("wild_" + name)));
		}
		// Shears on a vanilla rose bush pick its hips, about one time in three, and wear the shears.
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			var stack = player.getItemInHand(hand);
			if (!stack.is(net.minecraft.world.item.Items.SHEARS) || !level.getBlockState(hit.getBlockPos()).is(net.minecraft.world.level.block.Blocks.ROSE_BUSH)) {
				return net.minecraft.world.InteractionResult.PASS;
			}
			if (level instanceof net.minecraft.server.level.ServerLevel server) {
				if (server.getRandom().nextInt(3) == 0) {
					Block.popResource(level, hit.getBlockPos(), new net.minecraft.world.item.ItemStack(ROSEHIP, 1 + server.getRandom().nextInt(2)));
				}
				level.playSound(null, hit.getBlockPos(), net.minecraft.sounds.SoundEvents.SHEEP_SHEAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
				stack.hurtAndBreak(1, player, hand);
			}
			return net.minecraft.world.InteractionResult.SUCCESS;
		});
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
		PLANTED_FROM.put(plant, seeds[0]);
		Item produce = BuiltInRegistries.ITEM.getOptional(PortablePocketCabin.id(plant)).orElse(null);
		GIVES.put(plant, produce != null && produce != seeds[0] ? List.of(produce, seeds[0]) : List.of(seeds[0]));
		return block[0];
	}

	private static Block trellisCrop(String plant, String seed, java.util.function.Supplier<Item> produce) {
		Block[] block = new Block[1];
		Item[] seeds = new Item[1];
		block[0] = registerBlock(plant, properties -> new TrellisCropBlock(properties, () -> seeds[0], produce), BlockBehaviour.Properties.of()
			.randomTicks().strength(0.3F).sound(SoundType.CROP).noOcclusion().pushReaction(PushReaction.DESTROY));
		seeds[0] = registerItem(seed, properties -> new BlockItem(block[0], properties.useItemDescriptionPrefix()));
		PLANTS.put(plant, block[0]);
		PLANTED_FROM.put(plant, seeds[0]);
		GIVES.put(plant, List.of(produce.get()));
		return block[0];
	}

	/** A bush planted from a cutting, giving {@code flowering} at its flowering stage when not null, and {@code ripe} when ripe. */
	private static Block bush(String plant, String cutting, java.util.function.Supplier<Item> flowering, java.util.function.Supplier<Item> ripe) {
		Block block = registerBlock(plant, properties -> new HerbBushBlock(properties, flowering, ripe), bushProperties());
		PLANTED_FROM.put(plant, registerItem(cutting, properties -> new BlockItem(block, properties.useItemDescriptionPrefix())));
		PLANTS.put(plant, block);
		GIVES.put(plant, flowering == null ? List.of(ripe.get()) : List.of(flowering.get(), ripe.get()));
		return block;
	}

	/** A bush planted from what it gives, as sweet berries are. */
	private static Block selfBush(String plant, String produce, FoodProperties food) {
		Item[] item = new Item[1];
		Block block = registerBlock(plant, properties -> new HerbBushBlock(properties, null, () -> item[0]), bushProperties());
		item[0] = registerItem(produce, properties -> new BlockItem(block, (food == null ? properties : properties.food(food)).useItemDescriptionPrefix()));
		PLANTS.put(plant, block);
		PLANTED_FROM.put(plant, item[0]);
		GIVES.put(plant, List.of(item[0]));
		return block;
	}

	/** A fruit tree species: {@code <tree>_leaves} that fruit, and a {@code <tree>_sapling} that grows the {@code <tree>} feature. */
	private static Block fruitTree(String tree, java.util.function.Supplier<Item> fruit) {
		Block leaves = registerBlockWithItem(tree + "_leaves", properties -> new FruitingLeavesBlock(properties, fruit), BlockBehaviour.Properties.of()
			.strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY)
			.isValidSpawn((state, level, pos, type) -> false).isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false));
		Block sapling = registerBlockWithItem(tree + "_sapling", properties -> new FruitSaplingBlock(FruitSaplingBlock.grower(tree), properties),
			BlockBehaviour.Properties.of().noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY));
		PLANTS.put(tree + "_leaves", leaves);
		PLANTED_FROM.put(tree + "_leaves", sapling.asItem());
		GIVES.put(tree + "_leaves", List.of(fruit.get()));
		return leaves;
	}

	private static BlockBehaviour.Properties bushProperties() {
		return BlockBehaviour.Properties.of().randomTicks().noCollision().instabreak().sound(SoundType.SWEET_BERRY_BUSH)
			.pushReaction(PushReaction.DESTROY);
	}

	private static Block registerBlockWithItem(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		Block block = registerBlock(name, factory, properties);
		registerItem(name, itemProperties -> new BlockItem(block, itemProperties.useBlockDescriptionPrefix()));
		return block;
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
