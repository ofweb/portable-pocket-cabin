package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class CabinOccupancyData extends SavedData {
	record Stay(UUID playerId, UUID cabinId, long packedItemGeneration, int generalSize) {
		Stay(UUID playerId, UUID cabinId, long packedItemGeneration) {
			this(playerId, cabinId, packedItemGeneration, CabinProgression.INITIAL_GENERAL_SIZE);
		}
		private static final Codec<Stay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.STRING_CODEC.fieldOf("player_id").forGetter(Stay::playerId),
			UUIDUtil.STRING_CODEC.fieldOf("cabin_id").forGetter(Stay::cabinId),
			Codec.LONG.fieldOf("packed_item_generation").forGetter(Stay::packedItemGeneration),
			Codec.INT.optionalFieldOf("general_size", CabinProgression.INITIAL_GENERAL_SIZE).forGetter(Stay::generalSize)
		).apply(instance, Stay::new));
	}

	private record StoredData(List<Stay> stays) {
		private static final Codec<StoredData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Stay.CODEC.listOf().optionalFieldOf("stays", List.of()).forGetter(StoredData::stays)
		).apply(instance, StoredData::new));
	}

	static final Codec<CabinOccupancyData> CODEC = StoredData.CODEC.xmap(
		data -> new CabinOccupancyData(data.stays()),
		data -> new StoredData(new ArrayList<>(data.byPlayer.values()))
	);
	private static final SavedDataType<CabinOccupancyData> TYPE = new SavedDataType<>(
		PortablePocketCabin.id("cabin_occupancy"),
		CabinOccupancyData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<UUID, Stay> byPlayer = new LinkedHashMap<>();

	CabinOccupancyData() {
	}

	private CabinOccupancyData(List<Stay> stays) {
		for (Stay stay : stays) {
			byPlayer.put(stay.playerId(), stay);
		}
	}

	static CabinOccupancyData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	synchronized Optional<Stay> find(UUID playerId) {
		return Optional.ofNullable(byPlayer.get(playerId));
	}

	synchronized void enter(UUID playerId, CabinRecord cabin) {
		byPlayer.put(playerId, new Stay(playerId, cabin.uuid(), cabin.packedItemGeneration(), cabin.progression().generalSize()));
		setDirty();
	}

	synchronized void clear(UUID playerId) {
		if (byPlayer.remove(playerId) != null) {
			setDirty();
		}
	}
}
