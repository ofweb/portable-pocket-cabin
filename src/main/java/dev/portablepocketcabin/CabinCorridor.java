package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;

import java.util.Locale;

/** The three complete passages attached to the main room. */
public enum CabinCorridor {
	NORTH, WEST, EAST;

	public static final Codec<CabinCorridor> CODEC = Codec.STRING.xmap(
		name -> valueOf(name.toUpperCase(Locale.ROOT)),
		value -> value.name().toLowerCase(Locale.ROOT)
	);

	BlockPos expansionOffset() {
		return switch (this) {
			case NORTH -> new BlockPos(0, 0, -1);
			case WEST -> new BlockPos(-1, 0, 0);
			case EAST -> new BlockPos(1, 0, 0);
		};
	}
}
