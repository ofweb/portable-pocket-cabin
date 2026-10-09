package dev.portablepocketcabin;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.util.List;
import java.util.UUID;

/** Planting-control boundary; no arboretum commands or gameplay are registered during Design. */
final class CabinArboretumCultivation {
	private CabinArboretumCultivation() { }

	enum SelectionResult {
		APPLIED, CABIN_UNAVAILABLE, NOT_INSTALLED, NOT_OWNER, INVALID_SELECTION,
		UNSUPPORTED_TREE, RECOVERY_PENDING, WORLD_UNAVAILABLE, SAVE_FAILED
	}

	/** Revalidates the actor and current registry state before saving selection and replacing eligible saplings. */
	static SelectionResult selectTrees(MinecraftServer server, CabinRecord observed, UUID actor,
		List<Identifier> requestedTreeTypes) {
		throw new UnsupportedOperationException("B-0023 Design stub: planting selection");
	}
}
