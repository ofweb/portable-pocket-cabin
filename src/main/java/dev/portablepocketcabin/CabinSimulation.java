package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
		private final Map<Long, Set<ChunkPos>> activeCells = new java.util.LinkedHashMap<>();
		private long registryRevision = Long.MIN_VALUE;
		private Set<UUID> pendingCorridors = Set.of();
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
		CabinGreenhouseGrowth.sync(server);
		CabinRegistry registry = CabinRegistry.get(server);
		State state = STATES.computeIfAbsent(server, ignored -> new State());
		long revision = registry.revision();
		Set<UUID> pending = registry.cabins().stream().map(CabinRecord::uuid)
			.filter(id -> CabinCorridors.hasPending(server, id)).collect(java.util.stream.Collectors.toUnmodifiableSet());
		if (state.registryRevision == revision && state.pendingCorridors.equals(pending)) return;

		Map<Long, Set<ChunkPos>> desired = new java.util.LinkedHashMap<>();
		for (CabinRecord cabin : registry.cabins()) {
			if (shouldSimulate(cabin) && !pending.contains(cabin.uuid())) {
				desired.put(cabin.cellIndex(), CabinCorridorLayout.chunks(cabin));
			}
		}

		Map<Long, Set<ChunkPos>> active = state.activeCells;
		for (Map.Entry<Long, Set<ChunkPos>> entry : new ArrayList<>(active.entrySet())) {
			Set<ChunkPos> desiredSize = desired.get(entry.getKey());
			if (!entry.getValue().equals(desiredSize)) {
				removeTickets(pocket, entry.getKey(), entry.getValue());
				active.remove(entry.getKey());
			}
		}
		for (Map.Entry<Long, Set<ChunkPos>> entry : desired.entrySet()) {
			if (!active.containsKey(entry.getKey())) {
				addTickets(pocket, entry.getKey(), entry.getValue());
				active.put(entry.getKey(), entry.getValue());
			}
		}
		state.registryRevision = revision;
		state.pendingCorridors = pending;
	}

	static boolean shouldSimulate(CabinRecord cabin) {
		return cabin.lifecycle() == CabinLifecycle.DEPLOYED && cabin.interiorGenerated()
			&& !cabin.upgrades().operationInProgress();
	}

	static List<ChunkPos> chunksForCell(long cellIndex) {
		return chunksForCell(cellIndex, CabinProgression.INITIAL_GENERAL_SIZE);
	}

	static List<ChunkPos> chunksForCell(long cellIndex, int generalSize) {
		var center = PocketDimension.cellCenter(cellIndex);
		var bounds = PocketDimension.bounds(generalSize);
		int minimumChunkX = Math.floorDiv(center.getX() + bounds.shellMinimumX(), 16);
		int maximumChunkX = Math.floorDiv(center.getX() + bounds.shellMaximumX(), 16);
		int minimumChunkZ = Math.floorDiv(center.getZ() + bounds.shellMinimumZ(), 16);
		int maximumChunkZ = Math.floorDiv(center.getZ() + bounds.shellMaximumZ(), 16);
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
		return state != null && state.activeCells.containsKey(cellIndex);
	}

	static int ticketedCabinCount(MinecraftServer server) {
		State state = STATES.get(server);
		return state == null ? 0 : state.activeCells.size();
	}

	private static void addTickets(ServerLevel pocket, long cellIndex, java.util.Set<ChunkPos> chunks) {
		for (ChunkPos chunk : chunks) {
			pocket.getChunkSource().addTicketWithRadius(
				CABIN_SIMULATION_TICKET, chunk, FULL_SIMULATION_RADIUS
			);
			pocket.getChunk(chunk.x(), chunk.z());
		}
	}

	private static void removeTickets(ServerLevel pocket, long cellIndex, java.util.Set<ChunkPos> chunks) {
		for (ChunkPos chunk : chunks) {
			pocket.getChunkSource().removeTicketWithRadius(
				CABIN_SIMULATION_TICKET, chunk, FULL_SIMULATION_RADIUS
			);
		}
	}
}
