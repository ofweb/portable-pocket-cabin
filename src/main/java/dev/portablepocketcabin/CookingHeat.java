package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;

import java.util.Optional;

/** The heat a cooking station gives. Fuel sets it through item tags; untagged fuel burns at medium. */
enum CookingHeat implements StringRepresentable {
	LOW("low"),
	MEDIUM("medium"),
	HIGH("high");

	static final Codec<CookingHeat> CODEC = StringRepresentable.fromEnum(CookingHeat::values);
	static final StreamCodec<ByteBuf, CookingHeat> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
		index -> values()[index], CookingHeat::ordinal
	);

	private final String serializedName;
	private final TagKey<Item> fuelTag;

	CookingHeat(String serializedName) {
		this.serializedName = serializedName;
		this.fuelTag = TagKey.create(Registries.ITEM, PortablePocketCabin.id("heat/" + serializedName));
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}

	TagKey<Item> fuelTag() {
		return fuelTag;
	}

	/** The heat a fuel item burns at, or empty when the item is not fuel. */
	static Optional<CookingHeat> ofFuel(ItemStack stack, FuelValues fuelValues) {
		if (stack.isEmpty() || !fuelValues.isFuel(stack)) return Optional.empty();
		if (stack.is(LOW.fuelTag)) return Optional.of(LOW);
		if (stack.is(HIGH.fuelTag)) return Optional.of(HIGH);
		return Optional.of(MEDIUM);
	}
}
