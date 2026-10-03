package dev.portablepocketcabin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import java.util.Map;

final class CabinIngredientFallbacks {
	private static final Map<String, String> FALLBACKS = Map.of(
		"lavender", "allium", "barley", "wheat_seeds", "clover", "dandelion", "marigold", "orange_tulip",
		"white_petals", "lily_of_the_valley", "blue_hydrangea", "cornflower", "icy_iris", "azure_bluet",
		"glowflower", "oxeye_daisy", "toadstool", "red_mushroom");
	private CabinIngredientFallbacks() { }
	static Identifier resolve(Identifier preferred) {
		if (BuiltInRegistries.ITEM.getOptional(preferred).filter(item -> item != Items.AIR).isPresent()) return preferred;
		String fallback = preferred.getNamespace().equals("biomesoplenty") ? FALLBACKS.get(preferred.getPath()) : null;
		if (fallback == null) throw new IllegalStateException("No ingredient fallback for " + preferred);
		return Identifier.withDefaultNamespace(fallback);
	}
}
