package dev.portablepocketcabin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import java.util.concurrent.CompletionStage;

/** Chunk forest records, catch-up, and journal recovery. B-0023 effects remain unimplemented. */
final class CabinArboretumEcology {
	private CabinArboretumEcology() { }

	/** Persisted server operating ticks; server shutdown contributes no elapsed time. */
	record OperatingTime(long ticks) {
		OperatingTime {
			if (ticks < 0) throw new IllegalArgumentException("Operating time cannot be negative");
		}
	}

	enum ChunkResult { READY, RECOVERY_PENDING, UNSUPPORTED_CONTENT, SAVE_FAILED }
	enum EmptyResult { RECORDED, STILL_OCCUPIED, RECOVERY_PENDING, SAVE_FAILED }

	static OperatingTime clock(MinecraftServer server) {
		throw new UnsupportedOperationException("B-0023 Design stub: persisted operating clock");
	}

	/**
	 * Recovers and accounts for this chunk before it becomes available to players.
	 * Growth stops at maturity or ripeness; animals and harvesting receive no catch-up.
	 * Completion includes durable time accounting. World effects run on the server thread.
	 */
	static CompletionStage<ChunkResult> prepareChunk(MinecraftServer server,
		CabinArboretumWorlds.LoadedWorld world, ChunkPos chunk, OperatingTime asOf) {
		throw new UnsupportedOperationException("B-0023 Design stub: forest chunk recovery and catch-up");
	}

	/**
	 * Rechecks live occupants and records one reclamation cycle for an occupied-to-empty transition.
	 * Includes disconnects; quotas span the world, and unloaded work stays saved without terrain loads.
	 */
	static EmptyResult becameEmpty(MinecraftServer server,
		CabinArboretumWorlds.LoadedWorld world, OperatingTime asOf) {
		throw new UnsupportedOperationException("B-0023 Design stub: woodland reclamation cycle");
	}
}
