package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

record CabinCraftingState(boolean revealed, int level) {
	static final CabinCraftingState EMPTY = new CabinCraftingState(false, 0);
	static final Codec<CabinCraftingState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("revealed", false).forGetter(CabinCraftingState::revealed),
		Codec.INT.optionalFieldOf("level", 0).forGetter(CabinCraftingState::level)
	).apply(instance, CabinCraftingState::new));
	CabinCraftingState {
		if (level < 0 || level > 3 || level > 0 && !revealed)
			throw new IllegalArgumentException("Invalid crafting installation");
	}
	CabinCraftingState reveal() { return new CabinCraftingState(true, level); }
	CabinCraftingState upgrade(int target) {
		if (target != level + 1) throw new IllegalArgumentException("Crafting requires the previous level");
		return new CabinCraftingState(revealed, target);
	}
}
