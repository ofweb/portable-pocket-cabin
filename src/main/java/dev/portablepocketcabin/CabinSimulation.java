package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CabinSimulation {
	// Vanilla ticket level 31 (radius 2) is the entity-ticking level. Smaller radii
	// merely load the target chunk or allow block ticks, which is not full simulation.
	private static final int FULL_SIMULATION_RADIUS = 2;
	private static final TicketType CABIN_SIMULATION_TICKET = new TicketType(
		TicketType.NO_TIMEOUT,
		TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE
	);
	private static final Map<MinecraftServer, State> STATES = new IdentityHashMap<>();

	private static final class State {
		private final Set<Long> activeCells = new LinkedHashSet<>();
		private long registryRevision = Long.MIN_VALUE;
	}

	private CabinSimulation() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(CabinSimulation::sync);
		ServerLifecycleEvents.SERVER_STOPPED.register(STATES::remove);
	}

	static void sync(MinecraftServer server) {
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			STATES.remove(server);
			return;
		}
		CabinRegistry registry = CabinRegistry.get(server);
		State state = STATES.computeIfAbsent(server, ignored -> new State());
		long revision = registry.revision();
		if (state.registryRevision == revision) {
			return;
		}

		Set<Long> desired = new LinkedHashSet<>();
		for (CabinRecord cabin : registry.cabins()) {
			if (shouldSimulate(cabin)) {
				desired.add(cabin.cellIndex());
			}
		}

		Set<Long> active = state.activeCells;
		for (long cellIndex : new ArrayList<>(active)) {
			if (!desired.contains(cellIndex)) {
				removeTickets(pocket, cellIndex);
				active.remove(cellIndex);
			}
		}
		for (long cellIndex : desired) {
			if (active.add(cellIndex)) {
				addTickets(pocket, cellIndex);
			}
		}
		state.registryRevision = revision;
	}

	static boolean shouldSimulate(CabinRecord cabin) {
		return cabin.lifecycle() == CabinLifecycle.DEPLOYED && cabin.interiorGenerated();
	}

	static List<ChunkPos> chunksForCell(long cellIndex) {
		var center = PocketDimension.cellCenter(cellIndex);
		int minimumChunkX = Math.floorDiv(center.getX() - PocketDimension.INTERIOR_SHELL_RADIUS, 16);
		int maximumChunkX = Math.floorDiv(center.getX() + PocketDimension.INTERIOR_SHELL_RADIUS, 16);
		int minimumChunkZ = Math.floorDiv(center.getZ() - PocketDimension.INTERIOR_SHELL_RADIUS, 16);
		int maximumChunkZ = Math.floorDiv(center.getZ() + PocketDimension.INTERIOR_SHELL_RADIUS, 16);
		List<ChunkPos> chunks = new ArrayList<>();
		for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
			for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
				chunks.add(new ChunkPos(chunkX, chunkZ));
			}
		}
		return List.copyOf(chunks);
	}

	static boolean isTicketed(MinecraftServer server, long cellIndex) {
		State state = STATES.get(server);
		return state != null && state.activeCells.contains(cellIndex);
	}

	static int ticketedCabinCount(MinecraftServer server) {
		State state = STATES.get(server);
		return state == null ? 0 : state.activeCells.size();
	}

	private static void addTickets(ServerLevel pocket, long cellIndex) {
		for (ChunkPos chunk : chunksForCell(cellIndex)) {
			pocket.getChunkSource().addTicketWithRadius(
				CABIN_SIMULATION_TICKET, chunk, FULL_SIMULATION_RADIUS
			);
			pocket.getChunk(chunk.x(), chunk.z());
		}
	}

	private static void removeTickets(ServerLevel pocket, long cellIndex) {
		for (ChunkPos chunk : chunksForCell(cellIndex)) {
			pocket.getChunkSource().removeTicketWithRadius(
				CABIN_SIMULATION_TICKET, chunk, FULL_SIMULATION_RADIUS
			);
		}
	}
}
