package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Whether the kitchen book has been installed and whether the kitchen room has been purchased. */
record CabinKitchenState(boolean revealed, int level) {
	static final CabinKitchenState EMPTY = new CabinKitchenState(false, 0);
	static final Codec<CabinKitchenState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("revealed", false).forGetter(CabinKitchenState::revealed),
		Codec.INT.optionalFieldOf("level", 0).forGetter(CabinKitchenState::level)
	).apply(instance, CabinKitchenState::new));

	CabinKitchenState {
		if (level < 0 || level > 1 || level > 0 && !revealed)
			throw new IllegalArgumentException("Invalid kitchen installation");
	}

	CabinKitchenState reveal() {
		return new CabinKitchenState(true, level);
	}

	CabinKitchenState install() {
		if (level != 0) throw new IllegalArgumentException("The kitchen is already installed");
		return new CabinKitchenState(revealed, 1);
	}
}
