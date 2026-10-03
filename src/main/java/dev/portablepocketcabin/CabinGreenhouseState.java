package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

record CabinGreenhouseState(boolean revealed, int level) {
	static final CabinGreenhouseState EMPTY = new CabinGreenhouseState(false, 0);
	static final Codec<CabinGreenhouseState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("revealed", false).forGetter(CabinGreenhouseState::revealed),
		Codec.INT.optionalFieldOf("level", 0).forGetter(CabinGreenhouseState::level)
	).apply(instance, CabinGreenhouseState::new));
	CabinGreenhouseState {
		if (level < 0 || level > 4 || level > 0 && !revealed)
			throw new IllegalArgumentException("Invalid greenhouse installation");
	}
	CabinGreenhouseState reveal() { return new CabinGreenhouseState(true, level); }
	CabinGreenhouseState upgrade(int target) {
		if (target != level + 1) throw new IllegalArgumentException("Greenhouse requires the previous level");
		return new CabinGreenhouseState(revealed, target);
	}
}
