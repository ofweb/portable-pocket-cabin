package dev.portablepocketcabin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import java.util.Map;

final class CabinIngredientFallbacks {
	private static final Map<String, String> FALLBACKS = Map.ofEntries(
		Map.entry("lavender", "allium"),
		Map.entry("barley", "wheat_seeds"),
		Map.entry("clover", "dandelion"),
		Map.entry("marigold", "orange_tulip"),
		Map.entry("white_petals", "lily_of_the_valley"),
		Map.entry("blue_hydrangea", "cornflower"),
		Map.entry("icy_iris", "azure_bluet"),
		Map.entry("glowflower", "oxeye_daisy"),
		Map.entry("toadstool", "red_mushroom"),
		Map.entry("flowering_oak_sapling", "cherry_sapling"),
		Map.entry("red_maple_sapling", "cherry_sapling"),
		Map.entry("willow_sapling", "mangrove_propagule"),
		Map.entry("fir_sapling", "spruce_sapling"),
		Map.entry("mahogany_sapling", "jungle_sapling"));
	private CabinIngredientFallbacks() { }
	static Identifier resolve(Identifier preferred) {
		if (BuiltInRegistries.ITEM.getOptional(preferred).filter(item -> item != Items.AIR).isPresent()) return preferred;
		String fallback = preferred.getNamespace().equals("biomesoplenty") ? FALLBACKS.get(preferred.getPath()) : null;
		if (fallback == null) throw new IllegalStateException("No ingredient fallback for " + preferred);
		return Identifier.withDefaultNamespace(fallback);
	}
}
