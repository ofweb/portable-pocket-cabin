package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.HashMap;

record CabinEnchantingState(boolean revealed, int level, Map<Identifier, Integer> known) {
	static final CabinEnchantingState EMPTY = new CabinEnchantingState(false, 0, Map.of());
	static final Codec<CabinEnchantingState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("revealed", false).forGetter(CabinEnchantingState::revealed),
		Codec.INT.optionalFieldOf("level", 0).forGetter(CabinEnchantingState::level),
		Codec.unboundedMap(Identifier.CODEC, Codec.intRange(1, 255)).optionalFieldOf("known", Map.of())
			.forGetter(CabinEnchantingState::known)
	).apply(instance, CabinEnchantingState::new));

	CabinEnchantingState {
		known = Map.copyOf(known);
		if (level < 0 || level > 5 || level > 0 && !revealed
			|| known.values().stream().anyMatch(value -> value < 1 || value > 255))
			throw new IllegalArgumentException("Invalid enchantment library");
	}
	CabinEnchantingState reveal() { return new CabinEnchantingState(true, level, known); }
	CabinEnchantingState upgrade(int target) {
		if (target != level + 1) throw new IllegalArgumentException("Enchanting requires the previous tier");
		return new CabinEnchantingState(revealed, target, known);
	}
	CabinEnchantingState learn(Identifier enchantment, int sourceLevel) {
		if (sourceLevel <= known.getOrDefault(enchantment, 0)) return this;
		var updated = new HashMap<>(known);
		updated.put(enchantment, sourceLevel);
		return new CabinEnchantingState(revealed, level, updated);
	}
	int limit() { return level == 5 ? 255 : level; }
}
