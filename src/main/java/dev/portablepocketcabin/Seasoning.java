package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The five basic tastes and dish quality (ADR-0014). Tastes are item tags, so datapacks can change them. A dish
 * recipe lists the tastes that balance it; each seasoning used on it scores a level for a wanted taste no earlier
 * seasoning brought, and loses one otherwise. Quality is saved on the dish as a data component and raises its
 * saturation a little.
 */
final class Seasoning {
	/** The five basic tastes. */
	enum Taste implements StringRepresentable {
		SALTY, SWEET, SOUR, BITTER, UMAMI;

		static final Codec<Taste> CODEC = StringRepresentable.fromEnum(Taste::values);
		static final StreamCodec<io.netty.buffer.ByteBuf, Taste> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Taste::ordinal);
		private final TagKey<Item> tag = TagKey.create(Registries.ITEM, PortablePocketCabin.id("tastes/" + getSerializedName()));

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}

		TagKey<Item> tag() {
			return tag;
		}
	}

	static final int SEASONED = 1;
	static final int WELL_BALANCED = 2;
	/** Each quality level adds this share of the dish's own saturation. */
	static final float SATURATION_BONUS = 0.15F;
	/** What may go in a seasoning slot. */
	static final TagKey<Item> SEASONINGS = TagKey.create(Registries.ITEM, PortablePocketCabin.id("seasonings"));
	static final DataComponentType<Integer> DISH_QUALITY = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		PortablePocketCabin.id("dish_quality"), DataComponentType.<Integer>builder()
			.persistent(Codec.intRange(SEASONED, WELL_BALANCED)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	private Seasoning() {
	}

	static void register() {
		// Triggers static registration during mod initialization.
	}

	/** The tastes an item has, from the taste tags. */
	static Set<Taste> tastes(ItemStack stack) {
		Set<Taste> tastes = EnumSet.noneOf(Taste.class);
		for (Taste taste : Taste.values()) if (stack.is(taste.tag())) tastes.add(taste);
		return tastes;
	}

	/** The quality that these seasonings give a dish wanting these tastes: 0 plain, 1 seasoned, 2 well balanced. */
	static int quality(List<Taste> wanted, List<ItemStack> seasonings) {
		int score = 0;
		Set<Taste> brought = EnumSet.noneOf(Taste.class);
		for (ItemStack seasoning : seasonings) {
			for (Taste taste : tastes(seasoning)) {
				if (wanted.contains(taste) && brought.add(taste)) score++;
				else score--;
			}
		}
		return Math.max(0, Math.min(WELL_BALANCED, score));
	}

	/** Marks a dish with its quality and raises its saturation. A plain dish stays exactly as the recipe made it. */
	static ItemStack apply(ItemStack dish, int quality) {
		if (quality <= 0) return dish;
		dish.set(DISH_QUALITY, quality);
		FoodProperties food = dish.get(DataComponents.FOOD);
		if (food != null) {
			dish.set(DataComponents.FOOD, new FoodProperties(food.nutrition(), food.saturation() * (1 + SATURATION_BONUS * quality),
				food.canAlwaysEat()));
		}
		return dish;
	}

	/** The non-empty seasonings in the given slots, one of each, in slot order. */
	static List<ItemStack> used(net.minecraft.world.Container container, int[] slots) {
		List<ItemStack> used = new ArrayList<>();
		for (int slot : slots) if (!container.getItem(slot).isEmpty()) used.add(container.getItem(slot).copyWithCount(1));
		return used;
	}
}
