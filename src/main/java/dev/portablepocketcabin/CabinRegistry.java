package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CabinRegistry extends SavedData {
	private record RegistryData(long nextCellIndex, List<CabinRecord> cabins) {
		private static final Codec<RegistryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.optionalFieldOf("next_cell_index", 0L).forGetter(RegistryData::nextCellIndex),
			CabinRecord.CODEC.listOf().optionalFieldOf("cabins", List.of()).forGetter(RegistryData::cabins)
		).apply(instance, RegistryData::new));
	}

	public static final Codec<CabinRegistry> CODEC = RegistryData.CODEC.comapFlatMap(
		CabinRegistry::decode,
		CabinRegistry::encode
	);
	public static final SavedDataType<CabinRegistry> TYPE = new SavedDataType<>(
		PortablePocketCabin.id("cabins"),
		CabinRegistry::new,
		CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<UUID, CabinRecord> byId = new LinkedHashMap<>();
	private final Map<UUID, UUID> byOwner = new LinkedHashMap<>();
	private final Map<Long, UUID> byCell = new LinkedHashMap<>();
	private long nextCellIndex;

	CabinRegistry() {
	}

	private CabinRegistry(long nextCellIndex, List<CabinRecord> cabins) {
		this.nextCellIndex = nextCellIndex;
		for (CabinRecord cabin : cabins) {
			byId.put(cabin.uuid(), cabin);
			byOwner.put(cabin.owner(), cabin.uuid());
			byCell.put(cabin.cellIndex(), cabin.uuid());
		}
	}

	public static CabinRegistry get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public static void flush(MinecraftServer server) {
		server.overworld().getDataStorage().saveAndJoin();
	}

	public synchronized CabinRecord create(UUID owner) {
		CabinRecord existing = findByOwner(owner).orElse(null);
		if (existing != null) {
			throw new IllegalStateException("Player already owns cabin " + existing.uuid());
		}
		if (nextCellIndex > PocketDimension.MAX_CELL_INDEX) {
			throw new IllegalStateException("Pocket-home cell grid is exhausted");
		}

		UUID cabinId;
		do {
			cabinId = UUID.randomUUID();
		} while (byId.containsKey(cabinId));

		CabinRecord cabin = new CabinRecord(cabinId, owner, nextCellIndex, CabinLifecycle.PACKED);
		nextCellIndex++;
		byId.put(cabin.uuid(), cabin);
		byOwner.put(cabin.owner(), cabin.uuid());
		byCell.put(cabin.cellIndex(), cabin.uuid());
		setDirty();
		return cabin;
	}

	public synchronized Optional<CabinRecord> find(UUID cabinId) {
		return Optional.ofNullable(byId.get(cabinId));
	}

	public synchronized Optional<CabinRecord> findByOwner(UUID owner) {
		UUID cabinId = byOwner.get(owner);
		return cabinId == null ? Optional.empty() : Optional.ofNullable(byId.get(cabinId));
	}

	public synchronized Optional<CabinRecord> findByCell(long cellIndex) {
		UUID cabinId = byCell.get(cellIndex);
		return cabinId == null ? Optional.empty() : Optional.ofNullable(byId.get(cabinId));
	}

	public synchronized CabinRecord beginDeployment(UUID cabinId, UUID owner, CabinExterior exterior) {
		CabinRecord cabin = byId.get(cabinId);
		if (cabin == null) {
			throw new IllegalStateException("No cabin record exists for " + cabinId);
		}
		if (!cabin.owner().equals(owner)) {
			throw new IllegalStateException("Only the cabin owner may deploy it");
		}
		if (cabin.lifecycle() != CabinLifecycle.PACKED) {
			throw new IllegalStateException("Cabin must be PACKED before deployment; current state is "
				+ cabin.lifecycle());
		}
		if (cabin.exteriorCleanupPending()) {
			throw new IllegalStateException("Cabin exterior cleanup must finish before redeployment");
		}

		CabinRecord deploying = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.DEPLOYING,
			Optional.of(exterior), cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false
		);
		replace(deploying);
		return deploying;
	}

	public synchronized CabinRecord markInteriorGenerated(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.interiorGenerated()) {
			return cabin;
		}
		CabinRecord generated = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(),
			cabin.lastExterior(), true, cabin.packedItemGeneration(), cabin.exteriorCleanupPending()
		);
		replace(generated);
		return generated;
	}

	public synchronized CabinRecord finishDeployment(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYING) {
			throw new IllegalStateException("Cabin must be DEPLOYING before it can become DEPLOYED");
		}
		if (!cabin.interiorGenerated()) {
			throw new IllegalStateException("Cabin interior must be generated before deployment can finish");
		}
		CabinRecord deployed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.DEPLOYED,
			cabin.exterior(), cabin.exterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false
		);
		replace(deployed);
		return deployed;
	}

	public synchronized CabinRecord beginPacking(UUID cabinId, UUID owner) {
		CabinRecord cabin = require(cabinId);
		if (!cabin.owner().equals(owner)) {
			throw new IllegalStateException("Only the cabin owner may pack it");
		}
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYED || cabin.exterior().isEmpty()) {
			throw new IllegalStateException("Cabin must be DEPLOYED before packing");
		}
		CabinRecord packing = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.PACKING,
			cabin.exterior(), cabin.exterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false
		);
		replace(packing);
		return packing;
	}

	public synchronized CabinRecord abortPacking(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() != CabinLifecycle.PACKING) {
			return cabin;
		}
		CabinRecord deployed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.DEPLOYED,
			cabin.exterior(), cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false
		);
		replace(deployed);
		return deployed;
	}

	public synchronized CabinRecord finishPacking(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() != CabinLifecycle.PACKING) {
			throw new IllegalStateException("Cabin must be PACKING before it can become PACKED");
		}
		if (cabin.packedItemGeneration() == Long.MAX_VALUE) {
			throw new IllegalStateException("Packed item generation is exhausted");
		}
		CabinRecord packed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.PACKED,
			Optional.empty(), cabin.exterior(), cabin.interiorGenerated(), cabin.packedItemGeneration() + 1, true
		);
		replace(packed);
		return packed;
	}

	public synchronized CabinRecord markExteriorCleanupComplete(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (!cabin.exteriorCleanupPending()) {
			return cabin;
		}
		CabinRecord cleaned = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(),
			cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false
		);
		replace(cleaned);
		return cleaned;
	}

	public synchronized CabinRecord rollbackDeployment(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYING) {
			return cabin;
		}
		CabinRecord packed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.PACKED,
			Optional.empty(), cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false
		);
		replace(packed);
		return packed;
	}

	public synchronized CabinRecord markOrphaned(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		Optional<CabinExterior> lastExterior = cabin.exterior().isPresent() ? cabin.exterior() : cabin.lastExterior();
		CabinRecord orphaned = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.ORPHANED,
			Optional.empty(), lastExterior, cabin.interiorGenerated(), cabin.packedItemGeneration(), false
		);
		replace(orphaned);
		return orphaned;
	}

	public synchronized CabinRecord recoverPacked(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() == CabinLifecycle.DEPLOYED && cabin.exterior().isPresent()) {
			throw new IllegalStateException("Cannot recover an item while a valid deployed exterior is registered");
		}
		if (cabin.packedItemGeneration() == Long.MAX_VALUE) {
			throw new IllegalStateException("Packed item generation is exhausted");
		}
		Optional<CabinExterior> lastExterior = cabin.exterior().isPresent() ? cabin.exterior() : cabin.lastExterior();
		CabinRecord packed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.PACKED,
			Optional.empty(), lastExterior, cabin.interiorGenerated(), cabin.packedItemGeneration() + 1, false
		);
		replace(packed);
		return packed;
	}

	public synchronized List<CabinRecord> cabins() {
		return byId.values().stream()
			.sorted(Comparator.comparingLong(CabinRecord::cellIndex))
			.toList();
	}

	public synchronized int size() {
		return byId.size();
	}

	public synchronized long nextCellIndex() {
		return nextCellIndex;
	}

	private CabinRecord require(UUID cabinId) {
		CabinRecord cabin = byId.get(cabinId);
		if (cabin == null) {
			throw new IllegalStateException("No cabin record exists for " + cabinId);
		}
		return cabin;
	}

	private void replace(CabinRecord cabin) {
		byId.put(cabin.uuid(), cabin);
		setDirty();
	}

	private synchronized RegistryData encode() {
		return new RegistryData(nextCellIndex, new ArrayList<>(byId.values()));
	}

	private static DataResult<CabinRegistry> decode(RegistryData data) {
		if (data.nextCellIndex() < 0) {
			return DataResult.error(() -> "Cabin next cell index must be non-negative");
		}

		Map<UUID, CabinRecord> ids = new LinkedHashMap<>();
		Map<UUID, CabinRecord> owners = new LinkedHashMap<>();
		Map<Long, CabinRecord> cells = new LinkedHashMap<>();
		long repairedNextCellIndex = data.nextCellIndex();
		for (CabinRecord cabin : data.cabins()) {
			if (ids.putIfAbsent(cabin.uuid(), cabin) != null) {
				return DataResult.error(() -> "Duplicate cabin UUID in saved registry: " + cabin.uuid());
			}
			if (owners.putIfAbsent(cabin.owner(), cabin) != null) {
				return DataResult.error(() -> "Owner has multiple cabins in saved registry: " + cabin.owner());
			}
			if (cells.putIfAbsent(cabin.cellIndex(), cabin) != null) {
				return DataResult.error(() -> "Duplicate cabin cell in saved registry: " + cabin.cellIndex());
			}
			if (cabin.cellIndex() > PocketDimension.MAX_CELL_INDEX) {
				return DataResult.error(() -> "Cabin cell is outside the supported grid: " + cabin.cellIndex());
			}
			repairedNextCellIndex = Math.max(repairedNextCellIndex, cabin.cellIndex() + 1);
		}

		if (repairedNextCellIndex > PocketDimension.MAX_CELL_INDEX + 1) {
			return DataResult.error(() -> "Cabin next cell index is outside the supported grid");
		}
		return DataResult.success(new CabinRegistry(repairedNextCellIndex, data.cabins()));
	}
}
