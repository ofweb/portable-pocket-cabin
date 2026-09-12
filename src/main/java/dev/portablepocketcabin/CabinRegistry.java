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
