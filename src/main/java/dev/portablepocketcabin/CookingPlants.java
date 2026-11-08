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

	// Delivery 2: taigas, cold waters, and coasts.
	static final Item RED_CABBAGE = ingredient("red_cabbage", food(2, 1.2F));
	static final Item RHUBARB = ingredient("rhubarb", null);
	static final Block RED_CABBAGE_CROP = fieldCrop("red_cabbage", "red_cabbage_seeds");
	static final Block RHUBARB_CROP = fieldCrop("rhubarb", "rhubarb_crown");
	/** Wasabi grows in cold, running water on gravel, and replants from its root. */
	static final Block WASABI_CROP = fieldCrop("wasabi", "wasabi_root");
	static final Block BLACKCURRANT_BUSH = selfBush("blackcurrant_bush", "blackcurrant", food(2, 1.2F));
	static final Block REDCURRANT_BUSH = selfBush("redcurrant_bush", "redcurrant", food(2, 1.2F));
	static final Block WAKAME = seaPlant("wakame_plant", "wakame");
	static final Block LAVER = seaPlant("laver_plant", "laver");
	static final Block SEA_LETTUCE = seaPlant("sea_lettuce_plant", "sea_lettuce");

	// Delivery 3: savannas, deserts, and badlands.
	static final Item BLUE_CORN = ingredient("blue_corn", null);
	static final Item AUBERGINE = ingredient("aubergine", food(1, 0.6F));
	static final Item BELL_PEPPER = ingredient("bell_pepper", food(2, 1.2F));
	static final Item PAPRIKA_PEPPER = ingredient("paprika_pepper", null);
	static final Item TOMATILLO = ingredient("tomatillo", food(1, 0.6F));
	static final Item GREEN_CHILI = ingredient("green_chili", null);
	static final Item POBLANO_CHILI = ingredient("poblano_chili", null);
	static final Item RED_CHILI = ingredient("red_chili", null);
	static final Item BASIL = ingredient("basil", null);
	static final Item ROSELLE = ingredient("roselle", null);
	static final Item TOMATO = ingredient("tomato", food(2, 1.2F));
	static final Item GRAPE = ingredient("grape", food(2, 1.2F));
	static final Item RED_GRAPE = ingredient("red_grape", food(2, 1.2F));
	static final Item BAY_LAUREL_LEAF = ingredient("bay_laurel_leaf", null);
	static final Item APRICOT = ingredient("apricot", food(3, 1.8F));
	static final Item LEMON = ingredient("lemon", food(1, 0.6F));
	static final Item ORANGE = ingredient("orange", food(3, 1.8F));
	static final Item TAMARIND_POD = ingredient("tamarind_pod", null);
	static final Item ALMOND = ingredient("almond", food(2, 1.2F));
	static {
		String[][] crops = {{"blue_corn", "blue_corn_seeds"}, {"aubergine", "aubergine_seeds"}, {"bell_pepper", "bell_pepper_seeds"},
			{"paprika_pepper", "paprika_pepper_seeds"}, {"tomatillo", "tomatillo_seeds"}, {"green_chili", "green_chili_seeds"},
			{"poblano_chili", "poblano_chili_seeds"}, {"red_chili", "red_chili_seeds"}, {"basil", "basil_seeds"}, {"roselle", "roselle_seeds"},
			{"sesame", "sesame_seed"}, {"peanut", "peanut"}, {"chickpea", "chickpea"}, {"anise", "anise_seed"}, {"cumin", "cumin_seed"}};
		for (String[] crop : crops) fieldCrop(crop[0], crop[1]);
		trellisCrop("tomato_vine", "tomato_seeds", () -> TOMATO);
		trellisCrop("grape_vine", "grape_cutting", () -> GRAPE);
		trellisCrop("red_grape_vine", "red_grape_cutting", () -> RED_GRAPE);
		fruitTree("bay_laurel", () -> BAY_LAUREL_LEAF);
		fruitTree("apricot", () -> APRICOT);
		fruitTree("lemon", () -> LEMON);
		fruitTree("orange", () -> ORANGE);
		fruitTree("tamarind", () -> TAMARIND_POD);
		fruitTree("almond", () -> ALMOND);
	}

	// Delivery 4: windswept savannas and hills.
	static final Item PURPLE_CORN = ingredient("purple_corn", null);
	static final Item AJI_AMARILLO = ingredient("aji_amarillo", null);
	static final Item GREEN_OLIVE = ingredient("green_olive", null);
	static final Item BLACK_OLIVE = ingredient("black_olive", null);
	/** Olive leaves give green olives at their first fruit stage, and black olives if left to ripen one stage more. */
	static final Block OLIVE_LEAVES = fruitTree("olive",
		properties -> new FruitingLeavesBlock.TwoStageFruit(properties, () -> GREEN_OLIVE, () -> BLACK_OLIVE), List.of(GREEN_OLIVE, BLACK_OLIVE), false);
	static {
		for (String herb : new String[] {"rosemary", "oregano", "savory"}) selfBush(herb + "_bush", herb, null);
		fieldCrop("purple_corn", "purple_corn_seeds");
		fieldCrop("aji_amarillo", "aji_amarillo_seeds");
	}

	// Delivery 5: rivers, swamps, cherry groves, and bamboo jungles.
	static final Item RICE = ingredient("rice", null);
	static final Item GLUTINOUS_RICE = ingredient("glutinous_rice", null);
	static final Item CELERY = ingredient("celery", food(1, 0.6F));
	static final Item CHERRY = ingredient("cherry", food(2, 1.2F));
	static final Item UME_PLUM = ingredient("ume_plum", null);
	static final Item STAR_ANISE_POD = ingredient("star_anise_pod", null);
	static final Item TEA_LEAF = ingredient("tea_leaf", null);
	static final Item SHADED_TEA_LEAF = ingredient("shaded_tea_leaf", null);
	/** Rice, glutinous rice, and water chestnut grow standing in water one block deep. */
	static final Block RICE_CROP = paddyCrop("rice", "rice_seedlings");
	static final Block GLUTINOUS_RICE_CROP = paddyCrop("glutinous_rice", "glutinous_rice_seedlings");
	static final Block WATER_CHESTNUT_CROP = paddyCrop("water_chestnut", "water_chestnut");
	/** Tea gives tea leaves, or shaded tea leaves when it ripens under a roof. */
	static final Block TEA_BUSH;
	static {
		TEA_BUSH = registerBlock("tea_bush", properties -> new HerbBushBlock.Tea(properties, () -> TEA_LEAF, () -> SHADED_TEA_LEAF), bushProperties());
		PLANTED_FROM.put("tea_bush", registerItem("tea_cutting", properties -> new BlockItem(TEA_BUSH, properties.useItemDescriptionPrefix())));
		PLANTS.put("tea_bush", TEA_BUSH);
		GIVES.put("tea_bush", List.of(TEA_LEAF, SHADED_TEA_LEAF));
		fieldCrop("soybean", "soybean");
		fieldCrop("azuki_bean", "azuki_bean");
		fieldCrop("celery", "celery_seeds");
		selfBush("mint_bush", "mint", null);
		fruitTree("sweet_cherry", () -> CHERRY);
		fruitTree("ume", () -> UME_PLUM);
		fruitTree("star_anise", () -> STAR_ANISE_POD);
	}

	// Delivery 6: jungles. Field crops replant from what they give, apart from the chilies, basils, cardamom, and pineapple.
	static final Item THAI_AUBERGINE = ingredient("thai_aubergine", food(1, 0.6F));
	static final Item BIRDS_EYE_CHILI = ingredient("birds_eye_chili", null);
	static final Item HOLY_BASIL = ingredient("holy_basil", null);
	static final Item THAI_BASIL = ingredient("thai_basil", null);
	static final Item CARDAMOM_POD = ingredient("cardamom_pod", null);
	static final Item PINEAPPLE = ingredient("pineapple", food(3, 1.8F));
	static final Item LONG_BEAN = ingredient("long_bean", food(1, 0.6F));
	static final Item PEPPERCORN = ingredient("peppercorn", null);
	static final Item VANILLA_POD = ingredient("vanilla_pod", null);
	static final Item BETEL_LEAF = ingredient("betel_leaf", null);
	static final Item GREEN_PAPAYA = ingredient("green_papaya", food(1, 0.6F));
	static final Item MAKRUT_LIME_LEAF = ingredient("makrut_lime_leaf", null);
	static final Item ALLSPICE = ingredient("allspice", null);
	static final Item ANNATTO_SEED = ingredient("annatto_seed", null);
	static final Item CLOVE_BUD = ingredient("clove_bud", null);
	static final Item NUTMEG_SEED = ingredient("nutmeg_seed", null);
	static final Item LIME = ingredient("lime", food(1, 0.6F));
	static final Item MANGO = ingredient("mango", food(4, 2.4F));
	static final Item COFFEE_CHERRY = ingredient("coffee_cherry", null);
	static final Item YERBA_MATE_LEAF = ingredient("yerba_mate_leaf", null);
	static final Item COCONUT = ingredient("coconut", null);
	static final Item OIL_PALM_FRUIT = ingredient("oil_palm_fruit", null);
	static final Item CINNAMON_BARK = ingredient("cinnamon_bark", null);
	static final Item PALM_SAP = registerItem("palm_sap", properties -> new Item(properties.stacksTo(16)));
	/** Cinnamon trunks give their bark to an axe and grow it back. */
	static final Block CINNAMON_LOG = registerBlockWithItem("cinnamon_log",
		properties -> new HarvestTrunkBlock(properties, HarvestTrunkBlock.Harvest.BARK, () -> CINNAMON_BARK), trunkProperties());
	/** Oil palm and coconut palm trunks fill with sap that a glass bottle taps. */
	static final Block PALM_LOG = registerBlockWithItem("palm_log",
		properties -> new HarvestTrunkBlock(properties, HarvestTrunkBlock.Harvest.SAP, () -> PALM_SAP), trunkProperties());
	static {
		String[][] crops = {{"black_bean", "black_bean"}, {"thai_aubergine", "thai_aubergine_seeds"}, {"cassava", "cassava"},
			{"sweet_potato", "sweet_potato"}, {"birds_eye_chili", "birds_eye_chili_seeds"}, {"holy_basil", "holy_basil_seeds"},
			{"thai_basil", "thai_basil_seeds"}, {"lemongrass", "lemongrass"}, {"cardamom", "cardamom_seeds"}, {"galangal", "galangal"},
			{"ginger", "ginger"}, {"turmeric", "turmeric"}, {"pineapple", "pineapple_crown"}};
		for (String[] crop : crops) fieldCrop(crop[0], crop[1]);
		GIVES.put("cardamom", List.of(CARDAMOM_POD, PLANTED_FROM.get("cardamom")));
		trellisCrop("long_bean_vine", "long_bean_seeds", () -> LONG_BEAN);
		trellisCrop("peppercorn_vine", "peppercorn_cutting", () -> PEPPERCORN);
		trellisCrop("vanilla_vine", "vanilla_cutting", () -> VANILLA_POD);
		trellisCrop("betel_vine", "betel_cutting", () -> BETEL_LEAF);
		selfBush("pandan_bush", "pandan_leaf", null);
		fruitTree("papaya", () -> GREEN_PAPAYA);
		fruitTree("makrut_lime", () -> MAKRUT_LIME_LEAF);
		fruitTree("allspice", () -> ALLSPICE);
		fruitTree("annatto", () -> ANNATTO_SEED);
		fruitTree("clove", () -> CLOVE_BUD);
		fruitTree("nutmeg", () -> NUTMEG_SEED);
		fruitTree("lime", () -> LIME);
		fruitTree("mango", () -> MANGO);
		fruitTree("coffee", () -> COFFEE_CHERRY);
		fruitTree("yerba_mate", () -> YERBA_MATE_LEAF);
		fruitTree("oil_palm", properties -> new FruitingLeavesBlock(properties, () -> OIL_PALM_FRUIT), List.of(OIL_PALM_FRUIT, PALM_SAP), false);
		fruitTree("coconut", properties -> new FruitingLeavesBlock(properties, () -> COCONUT), List.of(COCONUT, PALM_SAP), true);
		// The cinnamon tree's leaves only drop saplings. Its garden entry is its trunk, which gives the bark.
		registerBlockWithItem("cinnamon_leaves", properties -> new net.minecraft.world.level.block.UntintedParticleLeavesBlock(0.01F,
			net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES, properties), BlockBehaviour.Properties.of().strength(0.2F).randomTicks()
			.sound(SoundType.GRASS).noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY).isValidSpawn((state, level, pos, type) -> false)
			.isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false));
		Block sapling = registerBlockWithItem("cinnamon_sapling", properties -> new FruitSaplingBlock(FruitSaplingBlock.grower("cinnamon"), properties),
			saplingProperties());
		PLANTS.put("cinnamon_log", CINNAMON_LOG);
		PLANTED_FROM.put("cinnamon_log", sapling.asItem());
		GIVES.put("cinnamon_log", List.of(CINNAMON_BARK));
	}

	// B-0049 seafood: mussels on stony shores and cold-ocean rocks, and oysters in lukewarm oceans and mangrove swamps.
	static final Block MUSSEL_BED = shellfish("mussel_bed", "mussels");
	static final Block OYSTER_BED = shellfish("oyster_bed", "oyster");

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

	/** Delivery 2 products: wasabi, sea vegetables, dried berries, and the blackcurrant and redcurrant drinks and desserts. */
	static {
		String[] plain = {"wasabi", "chopped_laver", "nori", "dried_sea_lettuce", "aonori", "dried_wakame", "dried_blackcurrant", "dried_raspberry",
			"dried_berries", "crushed_blackcurrants", "buttered_crumbs", "toasted_sweet_crumbs", "rodgrod"};
		for (String name : plain) PRODUCTS.add(ingredient(name, null));
		String[] bottles = {"cassis_syrup", "unstrained_solbaersaft", "unstrained_redcurrant_jelly", "strained_redcurrant_jelly", "redcurrant_jelly"};
		for (String name : bottles) PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16))));
		for (String name : new String[] {"blackcurrant_juice", "solbaersaft", "sirop_de_cassis"}) {
			PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16)
				.food(new FoodProperties(3, 1.2F, true), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
				.usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE))));
		}
		PRODUCTS.add(ingredient("gammeldags_aeblekage", food(8, 9.6F)));
		PRODUCTS.add(registerItem("rodgrod_med_flode", properties -> new Item(properties.stacksTo(16).food(food(7, 8.4F))
			.usingConvertsTo(net.minecraft.world.item.Items.BOWL))));
	}

	/** Delivery 3 products: spices and chilies, salsas, sesame, almonds and peanuts, red wine, citrus and hibiscus drinks, and the dishes they complete. */
	static {
		String[] plain = {"toasted_sesame_seeds", "sesame_paste", "dried_chickpeas", "chickpea_flour", "blue_hominy", "blue_corn_masa", "charred_tomato",
			"raw_salsa_roja", "salsa_roja", "roasted_tomatillo", "salsa_verde", "tamarind_pulp", "anise", "dried_cumin_seed", "toasted_cumin_seed", "cumin",
			"dried_paprika_pepper", "paprika", "dried_chilies", "chili_powder", "bay_leaf", "bouquet_garni", "roasted_peanut_pods", "blanched_almonds",
			"chopped_almonds", "sliced_almonds", "crushed_red_grapes", "fermented_red_grapes", "dried_hibiscus", "brown_butter", "madeleine_batter",
			"cheesecake_filling", "unbaked_kasekuchen", "fruchtetee_blend", "chopped_rhubarb", "unbaked_quesadilla", "obatzda"};
		for (String name : plain) PRODUCTS.add(ingredient(name, null));
		String[] bottles = {"sesame_oil", "soaked_tamarind", "tamarind_paste", "young_red_wine", "red_wine", "lemon_juice", "hot_hibiscus",
			"strained_hibiscus", "tamarind_water", "infused_elderflower", "unstrained_rabarbersaft"};
		for (String name : bottles) PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16))));
		for (String name : new String[] {"citron_presse", "fruchtetee", "nam_krajeab", "agua_de_jamaica", "agua_de_tamarindo", "hyldeblomstsaft", "rabarbersaft"}) {
			PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16)
				.food(new FoodProperties(3, 1.2F, true), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
				.usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE))));
		}
		PRODUCTS.add(ingredient("roasted_peanuts", food(2, 1.2F)));
		PRODUCTS.add(ingredient("raisins", food(2, 1.2F)));
		PRODUCTS.add(ingredient("madeleines", food(3, 2.4F)));
		PRODUCTS.add(ingredient("kasekuchen", food(8, 9.6F)));
		PRODUCTS.add(ingredient("obatzda_with_bread", food(8, 9.6F)));
		PRODUCTS.add(ingredient("quesadilla", food(6, 7.2F)));
	}

	/** Delivery 4 products: olive oil, dried Mediterranean herbs, the anticucho marinade, and ratatouille. */
	static {
		String[] plain = {"olive_paste", "dried_oregano", "dried_rosemary", "dried_savory", "dried_thyme", "herbes_de_provence", "anticucho_marinade",
			"diced_aubergine", "diced_courgette", "diced_bell_pepper", "sauteed_vegetables"};
		for (String name : plain) PRODUCTS.add(ingredient(name, null));
		for (String name : new String[] {"unsettled_olive_oil", "olive_oil"}) PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16))));
		PRODUCTS.add(registerItem("ratatouille", properties -> new Item(properties.stacksTo(16).food(food(8, 9.6F))
			.usingConvertsTo(net.minecraft.world.item.Items.BOWL))));
	}

	/** Delivery 5 products: rice, tofu, tea, anko, mint, cherry, and ume, and the drinks and sweets they complete. */
	static {
		String[] plain = {"dried_azuki_beans", "dried_mint", "dried_soybeans", "star_anise", "bean_sprouts", "soaked_soybeans", "soybean_mash", "soy_curds",
			"tofu", "rice_flour", "steamed_rice", "soaked_glutinous_rice", "wet_glutinous_rice_flour", "glutinous_rice_flour", "rice_noodle_batter",
			"rice_sheets", "fresh_rice_noodles", "rice_noodles", "steamed_tea_leaves", "rolled_tea_leaves", "sencha_leaves", "withered_tea_leaves",
			"bruised_tea_leaves", "oxidised_tea_leaves", "black_tea", "steamed_shaded_tea_leaves", "tencha", "matcha_powder", "hojicha_leaves",
			"salted_ume", "umeboshi", "simmered_azuki_beans", "azuki_mash", "anko", "dorayaki_batter", "dorayaki_pancakes", "taiyaki_batter",
			"steamed_mochi_dough", "mochi", "cherry_compote", "cherry_sauce"};
		for (String name : plain) PRODUCTS.add(ingredient(name, null));
		for (String name : new String[] {"unstrained_soy_milk", "soy_milk", "unstrained_mint_syrup", "mint_syrup"}) {
			PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16))));
		}
		for (String name : new String[] {"menthe_a_leau", "citronnade_maison", "pfefferminztee", "sencha", "hojicha", "matcha"}) {
			PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16)
				.food(new FoodProperties(3, 1.2F, true), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
				.usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE))));
		}
		PRODUCTS.add(ingredient("onigiri", food(6, 7.2F)));
		PRODUCTS.add(ingredient("dorayaki", food(6, 7.2F)));
		PRODUCTS.add(ingredient("taiyaki", food(6, 7.2F)));
		PRODUCTS.add(ingredient("daifuku_mochi", food(4, 4.8F)));
	}

	/** Delivery 6 intermediates: jungle spices, coconut, starches, oils, sugars, sauces, bakes, tea, coffee, and mate. */
	static {
		String[] plain = {"cinnamon", "cloves", "dried_peppercorns", "black_pepper", "dried_cardamom_pods", "cardamom", "dried_nutmeg", "nutmeg", "dried_black_beans",
			"blanched_vanilla_pods", "sweated_vanilla_pods", "vanilla", "curry_spice_mix", "toasted_curry_spices", "curry_powder", "pickling_spice",
			"massaman_spices", "toasted_massaman_spices", "massaman_curry_mix", "massaman_curry_paste", "satay_spices", "soaked_chilies",
			"al_pastor_adobo", "boiled_oil_palm_fruit", "oil_palm_mash", "grated_coconut", "soaked_grated_coconut", "desiccated_coconut",
			"toasted_coconut", "satay_marinade", "grated_cassava", "wet_tapioca_starch", "tapioca_starch", "grated_sweet_potato",
			"wet_sweet_potato_starch", "sweet_potato_starch", "fermented_tapioca_starch", "sour_cassava_starch", "glass_noodle_dough",
			"fresh_glass_noodles", "glass_noodles", "thick_palm_syrup", "palm_sugar", "mexican_chocolate_mix", "mexican_chocolate_paste",
			"mexican_chocolate", "toasted_chilies", "mole_spices", "mole_base", "ground_mole_base", "mole_paste", "bechamel", "crema",
			"queso_blanco_curds", "queso_blanco", "curry_ketchup_base", "curry_ketchup", "karrysalat", "chopped_gherkins", "remoulade",
			"gingerbread_dough", "rested_gingerbread_dough", "gingerbread", "kammerjunkere_dough", "shaped_kammerjunkere", "baked_kammerjunkere",
			"kammerjunkere", "warm_rice_pudding", "rice_pudding", "warm_vanilla_custard", "vanilla_custard", "thai_tea_leaves",
			"fermented_coffee_cherries", "dried_coffee_cherries", "green_coffee_beans", "roasted_coffee_beans", "roasted_coffee",
			"seared_yerba_mate_leaves", "dried_yerba_mate_leaves", "aged_yerba_mate", "yerba_mate"};
		for (String name : plain) PRODUCTS.add(ingredient(name, null));
		String[] bottles = {"lime_juice", "coconut_milk", "settled_coconut_milk", "coconut_cream", "unstrained_achiote_oil", "achiote_oil", "dende_oil",
			"unstrained_chancaca_syrup", "chancaca_syrup"};
		for (String name : bottles) PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16))));
	}

	/** Delivery 6 dishes: the jungle desserts, drinks, breads, and snacks. */
	static {
		String[] plain = {"creme_brulee_custard", "baked_creme_brulee", "socca_batter", "unbaked_apfelstrudel", "unbaked_bienenstich", "baked_bienenstich",
			"risalamande", "aebleskiver_batter", "plain_aebleskiver", "steamed_glutinous_rice", "coconut_sticky_rice", "sliced_mango", "bua_loy_dough",
			"bua_loy_balls", "boiled_bua_loy", "khanom_krok_batter", "diced_water_chestnut", "coated_water_chestnut", "boiled_tub_tim",
			"hollowed_pumpkin", "sangkhaya_custard", "bruised_lemongrass", "churro_dough", "fried_churros", "flan_custard", "charred_corn",
			"soaked_horchata_rice", "horchata_base", "arepa_dough", "shaped_arepas", "arepas", "scalded_starch", "pao_de_queijo_dough",
			"pao_de_queijo_balls", "alfajor_dough", "alfajor_rounds", "alfajor_biscuits", "boiled_picarones_vegetables", "picarones_dough",
			"rested_picarones_dough", "fried_picarones", "raw_tequenos"};
		for (String name : plain) PRODUCTS.add(ingredient(name, null));
		String[] bottles = {"vanilla_infused_cream", "koldskal", "unstrained_hyldebaersaft", "sweet_coconut_sauce", "strong_thai_tea", "strained_thai_tea",
			"brewed_black_tea", "unstrained_nam_takrai", "strained_horchata", "unstrained_cafe_de_olla", "unstrained_chicha_morada",
			"strained_chicha_morada", "limonada_suica_mix", "limonada_de_coco_mix", "aguapanela"};
		for (String name : bottles) PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16))));
		String[] drinks = {"kinderpunsch", "varm_hyldebaersaft", "nam_manao", "cha_yen", "cha_manao", "nam_takrai", "horchata_de_arroz", "champurrado", "cafe_de_olla",
			"chicha_morada", "aguapanela_con_limon", "limonada_suica", "terere", "limonada_de_coco", "chilled_koldskal"};
		for (String name : drinks) {
			PRODUCTS.add(registerItem(name, properties -> new Item(properties.stacksTo(16)
				.food(new FoodProperties(3, 1.2F, true), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
				.usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE))));
		}
		String[] dishes = {"creme_brulee", "socca", "apfelstrudel", "bienenstich", "risalamande_med_kirsebaersauce", "koldskal_med_kammerjunkere", "aebleskiver",
			"mango_sticky_rice", "bua_loy", "khanom_krok", "tub_tim_grob", "sangkhaya_fak_thong", "churros", "flan_napolitano", "arroz_con_leche",
			"elote", "arepas_con_queso", "pao_de_queijo", "alfajores_de_maicena", "picarones", "tequenos"};
		for (String name : dishes) PRODUCTS.add(ingredient(name, food(6, 7.2F)));
	}

	/** Picked from vanilla rose bushes with shears. */
	static final Item ROSEHIP = ingredient("rosehip", null);

	private CookingPlants() {
	}

	/** The name a plant's wild feature and biome tag use: the plant, or the tree for fruiting leaves or a harvested trunk. */
	static String wildName(String plant) {
		if (plant.endsWith("_leaves")) return plant.substring(0, plant.length() - "_leaves".length());
		return plant.endsWith("_log") ? plant.substring(0, plant.length() - "_log".length()) : plant;
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
		return crop(plant, seed, false);
	}

	/** A paddy crop named after its plant, planted from {@code seed} into a water source one block deep. */
	private static Block paddyCrop(String plant, String seed) {
		return crop(plant, seed, true);
	}

	private static Block crop(String plant, String seed, boolean paddy) {
		Block[] block = new Block[1];
		Item[] seeds = new Item[1];
		block[0] = registerBlock(plant, properties -> paddy ? new PaddyCropBlock(properties, () -> seeds[0]) : new FieldCropBlock(properties, () -> seeds[0]),
			BlockBehaviour.Properties.of()
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
		return fruitTree(tree, properties -> new FruitingLeavesBlock(properties, fruit), List.of(fruit.get()), false);
	}

	/**
	 * A fruit tree species whose leaves are made by {@code leaves}, giving {@code gives} in order of ripening. A beach
	 * tree's sapling can also be planted on sand.
	 */
	private static Block fruitTree(String tree, java.util.function.Function<BlockBehaviour.Properties, Block> factory, List<Item> gives, boolean beach) {
		Block leaves = registerBlockWithItem(tree + "_leaves", factory, BlockBehaviour.Properties.of()
			.strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY)
			.isValidSpawn((state, level, pos, type) -> false).isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false));
		Block sapling = registerBlockWithItem(tree + "_sapling", properties -> new FruitSaplingBlock(FruitSaplingBlock.grower(tree), beach, properties),
			saplingProperties());
		PLANTS.put(tree + "_leaves", leaves);
		PLANTED_FROM.put(tree + "_leaves", sapling.asItem());
		GIVES.put(tree + "_leaves", gives);
		return leaves;
	}

	/** A seaweed that grows wild underwater. What it gives also plants it again in water. */
	private static Block seaPlant(String plant, String produce) {
		return seaPlant(plant, produce, SeaPlantBlock::new);
	}

	/** A shellfish bed underwater, gathered by hand, and planted again from its shellfish. */
	private static Block shellfish(String plant, String produce) {
		return seaPlant(plant, produce, SeaPlantBlock::shellfish);
	}

	private static Block seaPlant(String plant, String produce, Function<BlockBehaviour.Properties, Block> factory) {
		Block block = registerBlock(plant, factory, BlockBehaviour.Properties.of().replaceable().noCollision().instabreak()
			.sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY));
		Item item = registerItem(produce, properties -> new BlockItem(block, properties.useItemDescriptionPrefix()));
		PLANTS.put(plant, block);
		PLANTED_FROM.put(plant, item);
		GIVES.put(plant, List.of(item));
		return block;
	}

	private static BlockBehaviour.Properties saplingProperties() {
		return BlockBehaviour.Properties.of().noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY);
	}

	private static BlockBehaviour.Properties trunkProperties() {
		return BlockBehaviour.Properties.of().strength(2.0F).randomTicks().sound(SoundType.WOOD).ignitedByLava()
			.instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASS);
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
