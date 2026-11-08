package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * B-0049 butchery: animals drop carcasses, which are bled, scalded, and dressed on the hanging hook, then broken down
 * into parts on the butcher block.
 */
final class Butchery {
	/**
	 * What has been done to a carcass. A fresh carcass has no component. Blood can be collected only while it is fresh,
	 * and scalding ends that chance. A sheep's carcass also records its fleece colour, so skinning gives matching wool.
	 */
	record CarcassState(boolean bled, boolean scalded, Optional<DyeColor> fleece) {
		static final CarcassState FRESH = new CarcassState(false, false, Optional.empty());
		static final Codec<CarcassState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("bled", false).forGetter(CarcassState::bled),
			Codec.BOOL.optionalFieldOf("scalded", false).forGetter(CarcassState::scalded),
			DyeColor.CODEC.optionalFieldOf("fleece").forGetter(CarcassState::fleece)
		).apply(instance, CarcassState::new));
		static final StreamCodec<ByteBuf, CarcassState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, CarcassState::bled,
			ByteBufCodecs.BOOL, CarcassState::scalded,
			ByteBufCodecs.optional(DyeColor.STREAM_CODEC), CarcassState::fleece,
			CarcassState::new);

		/** Whether blood can still be collected: neither bled nor scalded. */
		boolean fresh() {
			return !bled && !scalded;
		}

		CarcassState withBled() {
			return new CarcassState(true, scalded, fleece);
		}
	}

	static final DataComponentType<CarcassState> CARCASS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		PortablePocketCabin.id("carcass"), DataComponentType.<CarcassState>builder()
			.persistent(CarcassState.CODEC).networkSynchronized(CarcassState.STREAM_CODEC).build());

	/** The butcherable animals, in the order the cookbook lists them. */
	static final String[] ANIMALS = {"cow", "pig", "sheep", "chicken", "rabbit", "goat", "duck"};
	/** Each animal's carcass, as animals drop them. */
	static final Map<String, Item> CARCASSES = new LinkedHashMap<>();
	/** Each animal's dressed carcass, skinned, plucked, or scraped, ready for the butcher block. */
	static final Map<String, Item> DRESSED = new LinkedHashMap<>();

	static {
		for (String animal : ANIMALS) {
			CARCASSES.put(animal, item(animal + "_carcass", new Item.Properties().stacksTo(1)));
			DRESSED.put(animal, item("dressed_" + animal, new Item.Properties().stacksTo(1)));
		}
	}

	/** Blood collected from a fresh carcass, for blood sausage. */
	static final Item BLOOD = item("blood", new Item.Properties().stacksTo(16));
	/** Scraped from a scalded pig. They make brushes. */
	static final Item PIG_BRISTLES = item("pig_bristles", new Item.Properties());

	static final net.minecraft.world.level.block.Block HANGING_HOOK = CookingBlocks.register("hanging_hook", HangingHookBlock::new,
		net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.0F).sound(net.minecraft.world.level.block.SoundType.CHAIN)
			.noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
	static final net.minecraft.world.level.block.entity.BlockEntityType<HangingHookBlockEntity> HANGING_HOOK_ENTITY = CookingBlocks.registerEntity(
		"hanging_hook", new net.minecraft.world.level.block.entity.BlockEntityType<>(HangingHookBlockEntity::new, java.util.Set.of(HANGING_HOOK)));

	/** How many steps of its data a carcass on the butcher block has given. */
	static final DataComponentType<Integer> BUTCHERED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		PortablePocketCabin.id("butchered"), DataComponentType.<Integer>builder()
			.persistent(Codec.intRange(0, 256)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** Breaks a dressed carcass down on the butcher block, one part per use. */
	static final Item CLEAVER = item("cleaver", new Item.Properties().durability(250));
	static final net.minecraft.world.level.block.Block BUTCHER_BLOCK = CookingBlocks.register("butcher_block", ButcherBlockBlock::new,
		net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.5F).sound(net.minecraft.world.level.block.SoundType.WOOD)
			.ignitedByLava().noOcclusion());
	static final net.minecraft.world.level.block.entity.BlockEntityType<ButcherBlockBlockEntity> BUTCHER_BLOCK_ENTITY = CookingBlocks.registerEntity(
		"butcher_block", new net.minecraft.world.level.block.entity.BlockEntityType<>(ButcherBlockBlockEntity::new, java.util.Set.of(BUTCHER_BLOCK)));

	/**
	 * Every part the butcher block gives, by name. Muscle cuts are edible raw like vanilla meat, raw chicken with the
	 * same chance of food poisoning. Offal, bones, and fat are not eaten as they are.
	 */
	static final Map<String, Item> PARTS = new LinkedHashMap<>();
	static {
		String[][] meat = {
			{"3", "beef_sirloin", "beef_rib_roast", "beef_brisket", "beef_chuck", "beef_round", "beef_shank", "stew_beef", "oxtail"},
			{"3", "pork_loin", "pork_shoulder", "pork_belly", "pork_leg", "spare_ribs", "pork_cheek"},
			{"2", "lamb_chops", "leg_of_lamb", "lamb_shoulder", "lamb_stew_meat", "lamb_shanks"},
			{"2", "chevon_chops", "goat_leg", "goat_shoulder", "goat_stew_meat"},
			{"3", "rabbit_legs", "rabbit_saddle", "rabbit_stew_meat"}};
		for (String[] group : meat) {
			int nutrition = Integer.parseInt(group[0]);
			for (int i = 1; i < group.length; i++) {
				PARTS.put(group[i], item(group[i], new Item.Properties().food(new net.minecraft.world.food.FoodProperties(nutrition, nutrition * 0.6F, false))));
			}
		}
		for (String chicken : new String[] {"chicken_breast", "chicken_thighs", "chicken_drumsticks", "chicken_wings", "duck_breasts", "duck_legs", "duck_wings"}) {
			PARTS.put(chicken, item(chicken, new Item.Properties().food(new net.minecraft.world.food.FoodProperties(2, 1.2F, false),
				net.minecraft.world.item.component.Consumables.CHICKEN)));
		}
		String[] other = {"beef_tongue", "beef_liver", "beef_heart", "beef_kidneys", "tripe", "calf_stomach", "beef_bones", "marrow_bones", "suet",
			"pig_trotters", "pork_skin", "pork_liver", "pork_heart", "pork_casing", "pork_bones", "back_fat",
			"lamb_liver", "lamb_kidneys", "lamb_heart", "sheep_casing", "lamb_bones", "lamb_fat",
			"goat_liver", "goat_heart", "goat_casing", "goat_bones", "goat_fat",
			"chicken_giblets", "chicken_feet", "chicken_bones", "chicken_skin",
			"rabbit_offal", "rabbit_bones", "duck_liver", "duck_heart_and_gizzard", "duck_bones", "duck_fat"};
		for (String part : other) PARTS.put(part, item(part, new Item.Properties()));
	}

	/** Goat meat seared on the skillet, as mutton is. */
	static final Item COOKED_CHEVON = item("cooked_chevon", new Item.Properties().food(new net.minecraft.world.food.FoodProperties(6, 9.6F, false)));

	/** Minces meat and fish: each turn of its handle, with an empty hand, grinds once. It stands on the prep counter. */
	static final net.minecraft.world.level.block.Block MEAT_GRINDER = CookingBlocks.register("meat_grinder",
		properties -> new ProcessStationBlock.CounterTopStation(properties, ProcessStationBlock.Config.of(CookingMenu.Kind.MEAT_GRINDER,
			() -> Butchery.MEAT_GRINDER_ENTITY).shape(net.minecraft.world.level.block.Block.box(3, 0, 3, 13, 12, 13))
			.handWork(net.minecraft.sounds.SoundEvents.GRINDSTONE_USE)),
		net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.0F).sound(net.minecraft.world.level.block.SoundType.METAL)
			.noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
	/** Fills casings with a filling: each turn of its handle presses once. It stands on the prep counter. */
	static final net.minecraft.world.level.block.Block SAUSAGE_STUFFER = CookingBlocks.register("sausage_stuffer",
		properties -> new ProcessStationBlock.CounterTopStation(properties, ProcessStationBlock.Config.of(CookingMenu.Kind.SAUSAGE_STUFFER,
			() -> Butchery.SAUSAGE_STUFFER_ENTITY).shape(net.minecraft.world.level.block.Block.box(4, 0, 2, 12, 14, 14))
			.handWork(net.minecraft.sounds.SoundEvents.PISTON_EXTEND)),
		net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.0F).sound(net.minecraft.world.level.block.SoundType.METAL)
			.noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
	static final net.minecraft.world.level.block.entity.BlockEntityType<ProcessStationBlockEntity> MEAT_GRINDER_ENTITY =
		CookingBlocks.process("meat_grinder", MEAT_GRINDER, CookingMenu.Kind.MEAT_GRINDER);
	static final net.minecraft.world.level.block.entity.BlockEntityType<ProcessStationBlockEntity> SAUSAGE_STUFFER_ENTITY =
		CookingBlocks.process("sausage_stuffer", SAUSAGE_STUFFER, CookingMenu.Kind.SAUSAGE_STUFFER);

	/** The duck: a chicken-like bird of rivers and swamps, with its own carcass. */
	static final net.minecraft.world.entity.EntityType<Duck> DUCK = net.minecraft.core.Registry.register(BuiltInRegistries.ENTITY_TYPE,
		PortablePocketCabin.id("duck"), net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType.Builder.createMob(Duck::new,
			net.minecraft.world.entity.MobCategory.CREATURE, mob -> mob.defaultAttributes(net.minecraft.world.entity.animal.chicken.Chicken::createAttributes)
				.spawnPlacement(net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules))
			.sized(0.4F, 0.7F).eyeHeight(0.644F).clientTrackingRange(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, PortablePocketCabin.id("duck"))));
	static final Item DUCK_SPAWN_EGG = item("duck_spawn_egg", new Item.Properties().spawnEgg(DUCK), net.minecraft.world.item.SpawnEggItem::new);

	// Delivery 4: fish and seafood.
	static final Item ANCHOVY = item("anchovy", new Item.Properties().food(new net.minecraft.world.food.FoodProperties(1, 0.2F, false)));
	static final Item BONITO = item("bonito", new Item.Properties().food(new net.minecraft.world.food.FoodProperties(2, 0.4F, false)));
	static final Item HERRING = item("herring", new Item.Properties().food(new net.minecraft.world.food.FoodProperties(2, 0.4F, false)));
	/** A whole squid, which squid drop in place of ink sacs. The butcher block cleans it into tentacles and an ink sac. */
	static final Item SQUID = item("squid", new Item.Properties());
	static final Item PRAWNS = item("prawns", new Item.Properties().food(new net.minecraft.world.food.FoodProperties(1, 0.6F, false)));
	static {
		for (String part : new String[] {"white_fish_fillet", "salmon_fillet", "bonito_loin", "squid_tentacles"}) {
			PARTS.put(part, item(part, new Item.Properties().food(new net.minecraft.world.food.FoodProperties(2, 0.4F, false))));
		}
		for (String part : new String[] {"fish_head", "fish_bones", "cod_roe", "salmon_skin", "salmon_roe", "fish_scales", "fish_guts"}) {
			PARTS.put(part, item(part, new Item.Properties()));
		}
	}
	static final net.minecraft.world.level.block.Block PRAWN_TRAP = CookingBlocks.register("prawn_trap", PrawnTrapBlock::new,
		net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1.0F).sound(net.minecraft.world.level.block.SoundType.WOOD)
			.noOcclusion().ignitedByLava());
	static final net.minecraft.world.level.block.entity.BlockEntityType<PrawnTrapBlockEntity> PRAWN_TRAP_ENTITY = CookingBlocks.registerEntity(
		"prawn_trap", new net.minecraft.world.level.block.entity.BlockEntityType<>(PrawnTrapBlockEntity::new, java.util.Set.of(PRAWN_TRAP)));

	/** Delivery 1 products: stocks, rendered fats, gelatin, cured meats, and the dishes the cuts complete. */
	static final java.util.List<Item> PRODUCTS = new java.util.ArrayList<>();
	static {
		String[] plain = {"gelatin", "roasted_beef_bones", "cured_pork_belly", "bacon", "cured_pork_shoulder", "smoked_pork_shoulder", "ham", "salted_beef_round",
			"carne_seca", "diced_beef", "thin_beef_slices", "sliced_pork_belly", "poached_chicken_thighs", "poached_chicken", "raw_pino", "pino",
			"fresh_empanada_dough", "empanada_dough", "red_chili_blend", "red_chili_sauce", "cooked_black_beans", "black_bean_mash", "refried_beans",
			"caldillo_base", "tomato_caldillo", "crisp_bacon", "browned_beef", "garniture", "onion_soup", "blind_baked_shell", "quiche_custard",
			"unbaked_croque_monsieur", "raw_rinderrouladen", "seared_rinderrouladen", "marinated_beef_round", "seared_marinated_beef",
			"unbaked_flammkuchen", "crisp_pork_belly", "boiled_potatoes", "persillesovs", "creamed_chicken_sauce", "hons_i_asparges", "massaman_sauce",
			"charred_poblano", "stuffed_poblano", "relleno_batter", "fried_chile_relleno", "carnitas_pork", "crisp_carnitas", "chili_pork",
			"tamal_masa", "raw_tamales", "pozole_base", "marinated_al_pastor", "roasted_al_pastor", "al_pastor_meat", "mole_chicken", "concha_topping",
			"unbaked_conchas", "raw_empanadas", "corn_paste", "pastelera", "coxinha_dough", "raw_coxinhas", "marinated_beef_heart",
			"raw_anticucho_skewer", "anticucho_skewer"};
		for (String name : plain) PRODUCTS.add(item(name, new Item.Properties()));
		String[] bottles = {"sauerbraten_marinade", "unstrained_lard", "lard", "unstrained_tallow", "tallow", "unstrained_schmaltz", "schmaltz", "unstrained_beef_stock", "beef_stock",
			"unstrained_chicken_stock", "chicken_stock", "unstrained_pork_stock", "pork_stock", "unstrained_pork_bone_broth", "pork_bone_broth",
			"calf_rennet"};
		for (String name : bottles) PRODUCTS.add(item(name, new Item.Properties().stacksTo(16)));
		PRODUCTS.add(item("cracklings", new Item.Properties().food(new net.minecraft.world.food.FoodProperties(2, 1.6F, false))));
		String[] dishes = {"roast_chicken", "french_onion_soup", "quiche_lorraine", "boeuf_bourguignon", "coq_au_vin", "croque_monsieur", "rinderrouladen",
			"sauerbraten", "flammkuchen", "stegt_flaesk_med_persillesovs", "tarteletter_med_hons_i_asparges", "massaman_curry", "chiles_rellenos",
			"carnitas_tacos", "tamales", "pozole_rojo", "tacos_al_pastor", "mole_poblano", "conchas", "empanadas_de_carne", "pastel_de_choclo",
			"coxinhas", "anticuchos"};
		for (String name : dishes) PRODUCTS.add(item(name, new Item.Properties().food(new net.minecraft.world.food.FoodProperties(9, 10.8F, false))));
	}

	/** Delivery 2 products: mince, sausages, and the dishes they complete. */
	static {
		String[] plain = {"minced_beef", "minced_pork", "bratwurst_filling", "raw_bratwurst", "toulouse_filling", "chorizo_filling", "fresh_chorizo",
			"smoked_sausage_filling", "raw_smoked_sausage", "cocktail_sausage_filling", "raw_cocktail_sausages", "poached_cocktail_sausages",
			"blood_sausage_filling", "raw_blood_sausage", "warm_sauerkraut", "feijoada_beans", "frikadeller_fars", "poached_boller",
			"boller_i_karry_stew", "raw_polsehorn", "proofed_polsehorn"};
		for (String name : plain) PRODUCTS.add(item(name, new Item.Properties()));
		String[] sausages = {"blood_sausage", "chorizo", "cocktail_sausages", "fried_bratwurst", "smoked_sausage", "toulouse_sausage"};
		for (String name : sausages) PRODUCTS.add(item(name, new Item.Properties().food(new net.minecraft.world.food.FoodProperties(5, 6.0F, false))));
		String[] dishes = {"bratwurst_mit_sauerkraut", "currywurst", "feijoada", "frikadeller", "boller_i_karry", "polsehorn"};
		for (String name : dishes) PRODUCTS.add(item(name, new Item.Properties().food(new net.minecraft.world.food.FoodProperties(9, 10.8F, false))));
	}

	/** Delivery 3 products: rendered duck fat, duck confit, roast duck, and cassoulet. */
	static {
		for (String name : new String[] {"salted_duck_legs", "cassoulet_beans"}) PRODUCTS.add(item(name, new Item.Properties()));
		for (String name : new String[] {"unstrained_duck_fat", "rendered_duck_fat"}) PRODUCTS.add(item(name, new Item.Properties().stacksTo(16)));
		for (String name : new String[] {"duck_confit", "roast_duck", "cassoulet"}) {
			PRODUCTS.add(item(name, new Item.Properties().food(new net.minecraft.world.food.FoodProperties(9, 10.8F, false))));
		}
	}

	/** Delivery 4 products: stocks, fish sauce, cured and dried fish, curry pastes, and the seafood dishes. */
	static {
		String[] plain = {"peeled_prawns", "prawn_shells", "toasted_prawn_shells", "boiled_prawns", "dried_shrimp", "pounded_shrimp", "fermented_shrimp",
			"shrimp_paste", "fermenting_anchovies", "salted_anchovies", "salted_salmon_fillet", "grilled_salted_salmon", "salted_salmon",
			"salted_herring", "pickled_herring", "salted_bonito_loin", "smoked_bonito_loin", "katsuobushi", "bonito_flakes", "worcestershire_mash",
			"ground_peanuts", "green_curry_mix", "green_curry_paste", "red_curry_mix", "red_curry_paste", "peanut_sauce", "miang_sauce", "nam_prik_mix",
			"fried_nam_prik_mix", "nam_prik_paste", "nam_prik_pao", "liver_mince", "leverpostej_mix", "leverpostej", "unbaked_pissaladiere",
			"browned_mince", "korokke_mash", "raw_korokke", "fried_korokke", "shredded_papaya", "chili_garlic", "sliced_galangal", "tom_kha_base",
			"tom_yum_broth", "soaked_rice_noodles", "stir_fried_rice_noodles", "green_curry_sauce", "miang_filling", "diced_white_fish",
			"lime_cured_fish", "boiled_sweet_potato", "lime_marinated_fish", "moqueca_vegetables", "sliced_rugbrod", "minced_fish",
			"fiskefrikadeller_fars", "fried_fiskefrikadeller"};
		for (String name : plain) PRODUCTS.add(item(name, new Item.Properties()));
		String[] bottles = {"unstrained_prawn_stock", "prawn_stock", "unstrained_fish_stock", "fish_stock", "fish_sauce", "herring_pickling_liquid", "unstrained_dashi",
			"dashi", "unstrained_worcestershire_sauce", "worcestershire_sauce", "tonkatsu_sauce", "som_tam_dressing", "pad_thai_sauce"};
		for (String name : bottles) PRODUCTS.add(item(name, new Item.Properties().stacksTo(16)));
		String[] dishes = {"moules_marinieres", "pissaladiere", "fischbrotchen", "korokke", "som_tam", "tom_kha_gai", "tom_yum_goong", "pad_thai", "green_curry",
			"miang_kham", "ceviche", "moqueca_baiana", "sildemad_med_karrysalat", "smorrebrod_med_leverpostej", "fiskefrikadeller"};
		for (String name : dishes) PRODUCTS.add(item(name, new Item.Properties().food(new net.minecraft.world.food.FoodProperties(8, 9.6F, false))));
	}

	private Butchery() {
	}

	static void register() {
		CarcassData.register();
		net.fabricmc.fabric.api.registry.CompostableRegistry.INSTANCE.add(PARTS.get("fish_scales"), 0.3F);
		net.fabricmc.fabric.api.registry.CompostableRegistry.INSTANCE.add(PARTS.get("fish_guts"), 0.65F);
		// Ducks live in rivers, swamps, and mangrove swamps, in small groups.
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addSpawn(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.includeByKey(
			net.minecraft.world.level.biome.Biomes.RIVER, net.minecraft.world.level.biome.Biomes.SWAMP, net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP),
			net.minecraft.world.entity.MobCategory.CREATURE, DUCK, 8, 2, 4);
		net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS)
			.register(entries -> entries.accept(DUCK_SPAWN_EGG));
		net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(entries -> {
				entries.accept(HANGING_HOOK);
				entries.accept(BUTCHER_BLOCK);
				entries.accept(MEAT_GRINDER);
				entries.accept(SAUSAGE_STUFFER);
				entries.accept(PRAWN_TRAP);
			});
		net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)
			.register(entries -> entries.accept(CLEAVER));
		net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.FOOD_AND_DRINKS)
			.register(entries -> {
				for (Item carcass : CARCASSES.values()) entries.accept(carcass);
				for (Item dressed : DRESSED.values()) entries.accept(dressed);
				entries.accept(BLOOD);
				entries.accept(PIG_BRISTLES);
				for (Item part : PARTS.values()) entries.accept(part);
				entries.accept(COOKED_CHEVON);
				for (Item seafood : new Item[] {ANCHOVY, BONITO, HERRING, SQUID, PRAWNS}) entries.accept(seafood);
				for (Item product : PRODUCTS) entries.accept(product);
			});
	}

	/** A carcass's state: fresh when it has none recorded. */
	static CarcassState state(ItemStack stack) {
		return stack.getOrDefault(CARCASS, CarcassState.FRESH);
	}

	static boolean isCarcass(ItemStack stack) {
		return CARCASSES.containsValue(stack.getItem());
	}

	static Item item(String name, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, PortablePocketCabin.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)));
	}

	static Item item(String name, Item.Properties properties, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, PortablePocketCabin.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
