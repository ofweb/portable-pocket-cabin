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

	/** The butcherable animals, in the order the cookbook lists them. Ducks join in delivery 3. */
	static final String[] ANIMALS = {"cow", "pig", "sheep", "chicken", "rabbit", "goat"};
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
		for (String chicken : new String[] {"chicken_breast", "chicken_thighs", "chicken_drumsticks", "chicken_wings"}) {
			PARTS.put(chicken, item(chicken, new Item.Properties().food(new net.minecraft.world.food.FoodProperties(2, 1.2F, false),
				net.minecraft.world.item.component.Consumables.CHICKEN)));
		}
		String[] other = {"beef_tongue", "beef_liver", "beef_heart", "beef_kidneys", "tripe", "calf_stomach", "beef_bones", "marrow_bones", "suet",
			"pig_trotters", "pork_skin", "pork_liver", "pork_heart", "pork_casing", "pork_bones", "back_fat",
			"lamb_liver", "lamb_kidneys", "lamb_heart", "sheep_casing", "lamb_bones", "lamb_fat",
			"goat_liver", "goat_heart", "goat_casing", "goat_bones", "goat_fat",
			"chicken_giblets", "chicken_feet", "chicken_bones", "chicken_skin",
			"rabbit_offal", "rabbit_bones"};
		for (String part : other) PARTS.put(part, item(part, new Item.Properties()));
	}

	/** Goat meat seared on the skillet, as mutton is. */
	static final Item COOKED_CHEVON = item("cooked_chevon", new Item.Properties().food(new net.minecraft.world.food.FoodProperties(6, 9.6F, false)));

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

	private Butchery() {
	}

	static void register() {
		CarcassData.register();
		net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(entries -> {
				entries.accept(HANGING_HOOK);
				entries.accept(BUTCHER_BLOCK);
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
}
