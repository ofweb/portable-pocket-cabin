package dev.portablepocketcabin;

/**
 * A B-0050 cooking plant. Its growing needs come from data, and it keeps an {@code age} property, so that greenhouse
 * catch-up grows it toward maturity like a vanilla crop without ever harvesting or replanting it.
 */
interface CookingPlant {
	default PlantNeeds needs() {
		return PlantNeeds.of((net.minecraft.world.level.block.Block) this);
	}
}
