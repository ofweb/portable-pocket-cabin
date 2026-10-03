package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Shared free corridor effects. Room features choose when their installation needs one. */
public final class CabinCorridors {
	private static final Map<Path, Set<BlockPos>> PAUSED = new HashMap<>();
	private static final ThreadLocal<Boolean> WORLD_EFFECT = ThreadLocal.withInitial(() -> false);
	private CabinCorridors() { }

	public static boolean applyingWorldEffect() { return WORLD_EFFECT.get(); }

	static boolean mayChange(ServerLevel level, UUID actor, BlockPos pos) {
		if (!level.dimension().equals(PocketDimension.LEVEL_KEY)) return true;
		if (isPaused(level, pos)) return false;
		CabinRecord cabin = CabinRegistry.get(level.getServer()).findByCell(PocketDimension.cellIndexAt(pos).orElse(-1L)).orElse(null);
		if (cabin == null) return true;
		boolean wing = cabin.progression().corridors().stream().anyMatch(corridor -> CabinCorridorLayout.volume(
			cabin.cellIndex(), cabin.progression().generalSize(), corridor).contains(pos))
			|| cabin.progression().rooms().stream().anyMatch(room -> room.space().filter(space -> space.volume(cabin.cellIndex()).contains(pos)).isPresent());
		return !wing || cabin.owner().equals(actor) || cabin.trustedPlayers().contains(actor) && cabin.canEnter(actor);
	}

	static void applyWorldEffect(Runnable effect) {
		boolean previous = WORLD_EFFECT.get();
		WORLD_EFFECT.set(true);
		try { effect.run(); } finally { WORLD_EFFECT.set(previous); }
	}

	static boolean hasPending(MinecraftServer server, UUID cabin) {
		return Files.exists(journal(server, cabin));
	}

	static CabinUpgradeService.Outcome validateInstall(ServerLevel pocket, CabinRecord cabin, CabinCorridor corridor) {
		if (cabin.progression().corridors().contains(corridor)) return CabinUpgradeService.Outcome.success("");
		if (cabin.progression().generalSize() < 5) return CabinUpgradeService.Outcome.failure("Expand the main room to 5x5 first.");
		var main = PocketDimension.shellBlocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette());
		for (BlockPos pos : CabinCorridorLayout.volume(cabin.cellIndex(), cabin.progression().generalSize(), corridor)) {
			var actual = pocket.getBlockState(pos);
			if (!actual.isAir() && !actual.equals(main.get(pos))) {
				return CabinUpgradeService.Outcome.failure("Corridor installation is obstructed at " + pos.toShortString());
			}
		}
		return CabinUpgradeService.Outcome.success("");
	}

	/** No menu, fund, or cost: this is called by a room's installation effect. */
	static CabinUpgradeService.Outcome installForRoom(ServerLevel pocket, UUID cabinId, UUID owner, CabinCorridor corridor) {
		var server = pocket.getServer();
		var registry = CabinRegistry.get(server);
		CabinRecord cabin = registry.find(cabinId).orElse(null);
		if (cabin == null || !cabin.owner().equals(owner) || cabin.lifecycle() != CabinLifecycle.DEPLOYED || !cabin.interiorGenerated()) {
			return CabinUpgradeService.Outcome.failure("Only the owner can install a room in a deployed cabin.");
		}
		if (hasPending(server, cabinId)) return CabinUpgradeService.Outcome.failure("Corridor recovery is in progress.");
		if (cabin.upgrades().operationInProgress()) return CabinUpgradeService.Outcome.failure("Another cabin upgrade is in progress.");
		if (cabin.progression().corridors().contains(corridor)) return CabinUpgradeService.Outcome.success("");
		var validation = validateInstall(pocket, cabin, corridor);
		if (!validation.success()) return validation;
		var tag = new CompoundTag();
		tag.putString("cabin", cabinId.toString());
		tag.putString("corridor", corridor.name());
		tag.putInt("source_size", cabin.progression().generalSize());
		tag.putInt("target_size", cabin.progression().generalSize());
		write(server, cabinId, tag);
		applyInstall(pocket, cabin, corridor);
		pocket.getServer().saveEverything(false, true, false);
		registry.completeCorridor(cabinId, corridor);
		CabinRegistry.flush(server);
		finish(server, cabinId);
		CabinSimulation.sync(server);
		return CabinUpgradeService.Outcome.success("");
	}

	private static void applyInstall(ServerLevel pocket, CabinRecord cabin, CabinCorridor corridor) {
		for (var entry : CabinCorridorLayout.blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), corridor).entrySet()) {
			pocket.setBlock(entry.getKey(), entry.getValue(), CabinCorridorSnapshot.FLAGS);
		}
	}

	static Map<BlockPos, BlockPos> expansionDestinations(CabinRecord cabin) {
		var result = new LinkedHashMap<BlockPos, BlockPos>();
		for (CabinCorridor corridor : cabin.progression().corridors()) {
			BlockPos delta = corridor.expansionOffset();
			for (BlockPos source : CabinCorridorLayout.volume(cabin.cellIndex(), cabin.progression().generalSize(), corridor)) {
				BlockPos previous = result.put(source, source.offset(delta));
				if (previous != null && !previous.equals(source.offset(delta))) throw new IllegalStateException("Moving wings overlap");
			}
		}
		for (CabinRoom room : cabin.progression().rooms()) {
			room.space().ifPresent(space -> {
				for (BlockPos source : space.volume(cabin.cellIndex())) {
					BlockPos destination = source.offset(space.corridor().expansionOffset());
					BlockPos previous = result.put(source, destination);
					if (previous != null && !previous.equals(destination)) throw new IllegalStateException("Moving room wings overlap");
				}
			});
		}
		return Map.copyOf(result);
	}

	public static boolean isPaused(ServerLevel level, BlockPos pos) {
		if (!level.dimension().equals(PocketDimension.LEVEL_KEY)) return false;
		CabinRecord cabin = CabinRegistry.get(level.getServer()).findByCell(PocketDimension.cellIndexAt(pos).orElse(-1L)).orElse(null);
		if (cabin == null || !hasPending(level.getServer(), cabin.uuid())) return false;
		Path path = journal(level.getServer(), cabin.uuid());
		Set<BlockPos> paused = PAUSED.computeIfAbsent(path, ignored -> {
			CompoundTag tag = read(level.getServer(), cabin.uuid());
			if (tag.contains("snapshot")) {
				var result = new HashSet<BlockPos>();
				CabinCorridorSnapshot.load(tag.getCompoundOrEmpty("snapshot")).destinations().forEach((source, destination) -> {
					result.add(source); result.add(destination);
				});
				return Set.copyOf(result);
			}
			return CabinCorridorLayout.volume(cabin.cellIndex(), cabin.progression().generalSize(), CabinCorridor.valueOf(tag.getStringOr("corridor", "")));
		});
		return paused.contains(pos);
	}

	static CabinUpgradeService.Outcome validateExpansion(ServerLevel pocket, CabinRecord cabin) {
		if (hasPending(pocket.getServer(), cabin.uuid())) return CabinUpgradeService.Outcome.failure("Corridor recovery is in progress.");
		if (cabin.progression().corridors().isEmpty()) return CabinUpgradeService.Outcome.success("");
		if (cabin.progression().rooms().stream().anyMatch(room -> room.space().isEmpty())) {
			return CabinUpgradeService.Outcome.failure("A legacy disconnected room has no safe relocation geometry.");
		}
		for (var player : pocket.getServer().getPlayerList().getPlayers()) {
			if (CabinReconciliation.isOccupant(player, cabin) && !PocketDimension.isWithinUsable(
				cabin.cellIndex(), cabin.progression().generalSize(), player.blockPosition())) {
				String wing = cabin.progression().corridors().stream().filter(c -> CabinCorridorLayout.volume(
					cabin.cellIndex(), cabin.progression().generalSize(), c).contains(player.blockPosition()))
					.map(c -> c.name().toLowerCase(Locale.ROOT)).findFirst().orElse("room");
				return CabinUpgradeService.Outcome.failure("Return players from the " + wing + " wing to the main room before expanding.");
			}
		}
		try {
			cabin.progression().withGeneralSize(cabin.progression().generalSize() + CabinProgression.GENERAL_SIZE_STEP);
			var destinations = expansionDestinations(cabin);
			loadEntities(pocket, destinations);
			CabinCorridorSnapshot.validate(pocket, destinations, structuralBlocks(cabin));
			return CabinUpgradeService.Outcome.success("");
		} catch (IllegalStateException | IllegalArgumentException exception) {
			return CabinUpgradeService.Outcome.failure(exception.getMessage());
		}
	}

	static void moveForExpansion(ServerLevel pocket, CabinRecord cabin, int targetSize) {
		if (cabin.progression().corridors().isEmpty()) return;
		MinecraftServer server = pocket.getServer();
		CompoundTag tag;
		if (hasPending(server, cabin.uuid())) {
			tag = read(server, cabin.uuid());
			if (tag.getIntOr("target_size", 0) != targetSize || !tag.contains("snapshot")) {
				throw new IllegalStateException("A different corridor operation is pending");
			}
		} else {
			for (var player : server.getPlayerList().getPlayers()) {
				if (CabinReconciliation.isOccupant(player, cabin) && !(player.containerMenu instanceof CabinUpgradeMenu)
					&& player.containerMenu != player.inventoryMenu) player.closeContainer();
			}
			var destinations = expansionDestinations(cabin);
			loadEntities(pocket, destinations);
			var snapshot = CabinCorridorSnapshot.capture(pocket, destinations, structuralBlocks(cabin));
			tag = new CompoundTag();
			tag.putString("cabin", cabin.uuid().toString());
			tag.putInt("source_size", cabin.progression().generalSize());
			tag.putInt("target_size", targetSize);
			tag.put("snapshot", snapshot.save());
			var home = CabinRespawnData.get(server).find(cabin.owner());
			home.filter(h -> destinations.containsKey(h.bedPosition())).ifPresent(h ->
				CabinCorridorSnapshot.putPosition(tag, "home_bed", destinations.get(h.bedPosition())));
			write(server, cabin.uuid(), tag);
		}
		applyMove(pocket, cabin, tag);
	}

	private static void loadEntities(ServerLevel pocket, Map<BlockPos, BlockPos> destinations) {
		var chunks = new HashSet<ChunkPos>();
		for (var entry : destinations.entrySet()) {
			chunks.add(new ChunkPos(entry.getKey().getX() >> 4, entry.getKey().getZ() >> 4));
			chunks.add(new ChunkPos(entry.getValue().getX() >> 4, entry.getValue().getZ() >> 4));
		}
		for (ChunkPos chunk : chunks) {
			pocket.getChunk(chunk.x(), chunk.z());
			pocket.waitForEntities(chunk, 0);
		}
	}

	private static Set<net.minecraft.world.level.block.Block> structuralBlocks(CabinRecord cabin) {
		return new HashSet<>(List.of(cabin.palette().floor().planksBlock(), cabin.palette().walls().planksBlock(),
			cabin.palette().walls().structuralWoodBlock(), cabin.palette().roof().planksBlock()));
	}

	private static void applyMove(ServerLevel pocket, CabinRecord cabin, CompoundTag tag) {
		var snapshot = CabinCorridorSnapshot.load(tag.getCompoundOrEmpty("snapshot"));
		loadEntities(pocket, snapshot.destinations());
		applyWorldEffect(() -> snapshot.apply(pocket));
		if (tag.contains("home_bed")) {
			CabinRespawnData.get(pocket.getServer()).bind(cabin.owner(), cabin.uuid(), CabinCorridorSnapshot.position(tag, "home_bed"));
		}
	}

	static void finish(MinecraftServer server, UUID cabinId) {
		if (!hasPending(server, cabinId)) return;
		// Flush chunks, entities, player data, and registry before discarding authoritative contents.
		server.saveEverything(false, true, false);
		try {
			Files.delete(journal(server, cabinId));
			try (var directory = FileChannel.open(journal(server, cabinId).getParent(), StandardOpenOption.READ)) { directory.force(true); }
		}
		catch (IOException exception) { throw new IllegalStateException("Cannot finish corridor recovery", exception); }
		PAUSED.remove(journal(server, cabinId));
	}

	static void reconcileAll(MinecraftServer server) {
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) return;
		for (CabinRecord cabin : CabinRegistry.get(server).cabins()) {
			if (!hasPending(server, cabin.uuid())) continue;
			CompoundTag tag = read(server, cabin.uuid());
			if (!tag.getStringOr("cabin", "").equals(cabin.uuid().toString())) throw new IllegalStateException("Corridor journal belongs to another cabin");
			if (tag.contains("corridor")) {
				CabinCorridor corridor = CabinCorridor.valueOf(tag.getStringOr("corridor", ""));
				applyInstall(pocket, cabin, corridor);
				server.saveEverything(false, true, false);
				CabinRegistry.get(server).completeCorridor(cabin.uuid(), corridor);
				CabinRegistry.flush(server);
				finish(server, cabin.uuid());
			} else if (cabin.progression().generalSize() == tag.getIntOr("target_size", 0)) {
				// Registry committed before a crash, but the final world flush may not have finished.
				applyMove(pocket, cabin, tag);
				CabinRecord source = withSourceSize(cabin, tag.getIntOr("source_size", 0));
				applyWorldEffect(() -> {
					PocketDimension.applyGeneralSpaceExpansion(pocket, source, cabin.progression().generalSize());
					new CabinWindowWorld(server, pocket).applyRelayout(source, cabin.progression().generalSize());
					if (cabin.upgrades().storage().level() > 0) CabinStorage.placeControl(pocket, cabin, cabin.progression().generalSize());
				});
				finish(server, cabin.uuid());
			}
			// An uncommitted general expansion is replayed by CabinUpgradeService from this journal.
		}
	}

	private static CabinRecord withSourceSize(CabinRecord cabin, int sourceSize) {
		return new CabinRecord(cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(), cabin.lastExterior(),
			cabin.interiorGenerated(), cabin.packedItemGeneration(), cabin.exteriorCleanupPending(), cabin.palette(),
			cabin.lastDeploymentItemId(), cabin.deploymentItemDeliveryPending(), cabin.entryPermission(), cabin.trustedPlayers(),
			cabin.progression().withGeneralSize(sourceSize), cabin.upgrades());
	}

	private static Path journal(MinecraftServer server, UUID cabinId) {
		return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(PortablePocketCabin.MOD_ID)
			.resolve("corridor_operations").resolve(cabinId + ".nbt");
	}

	private static CompoundTag read(MinecraftServer server, UUID cabinId) {
		try { return Objects.requireNonNull(NbtIo.read(journal(server, cabinId))); }
		catch (IOException exception) { throw new IllegalStateException("Cannot read corridor recovery", exception); }
	}

	private static void write(MinecraftServer server, UUID cabinId, CompoundTag tag) {
		Path path = journal(server, cabinId), temporary = path.resolveSibling(path.getFileName() + ".tmp");
		try {
			Files.createDirectories(path.getParent());
			NbtIo.write(tag, temporary);
			try (var file = FileChannel.open(temporary, StandardOpenOption.WRITE)) { file.force(true); }
			Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			try (var directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
		} catch (IOException exception) { throw new IllegalStateException("Cannot persist corridor operation", exception); }
	}
}
