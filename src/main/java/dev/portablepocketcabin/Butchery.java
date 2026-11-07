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

	private Butchery() {
	}

	static void register() {
		// Triggers static registration during mod initialization.
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
