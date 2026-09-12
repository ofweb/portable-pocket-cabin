package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum CabinLifecycle {
	PACKED,
	DEPLOYING,
	DEPLOYED,
	PACKING,
	ORPHANED;

	public static final Codec<CabinLifecycle> CODEC = Codec.STRING.comapFlatMap(
		value -> {
			try {
				return DataResult.success(valueOf(value.toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException exception) {
				return DataResult.error(() -> "Unknown cabin lifecycle: " + value);
			}
		},
		value -> value.name().toLowerCase(Locale.ROOT)
	);
}
