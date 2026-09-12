package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

final class CabinRespawnData extends SavedData {
	private record StoredData(List<CabinHomeBinding> bindings) {
		private static final Codec<StoredData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			CabinHomeBinding.CODEC.listOf().optionalFieldOf("bindings", List.of())
				.forGetter(StoredData::bindings)
		).apply(instance, StoredData::new));
	}

	static final Codec<CabinRespawnData> CODEC = StoredData.CODEC.xmap(
		data -> new CabinRespawnData(data.bindings()),
		data -> new StoredData(new ArrayList<>(data.byPlayer.values()))
	);
	private static final SavedDataType<CabinRespawnData> TYPE = new SavedDataType<>(
		PortablePocketCabin.id("cabin_homes"),
		CabinRespawnData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<UUID, CabinHomeBinding> byPlayer = new LinkedHashMap<>();

	CabinRespawnData() {
	}

	private CabinRespawnData(List<CabinHomeBinding> bindings) {
		for (CabinHomeBinding binding : bindings) {
			byPlayer.put(binding.playerId(), binding);
		}
	}

	static CabinRespawnData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	synchronized Optional<CabinHomeBinding> find(UUID playerId) {
		return Optional.ofNullable(byPlayer.get(playerId));
	}

	synchronized void bind(UUID playerId, UUID cabinId, net.minecraft.core.BlockPos bedPosition) {
		byPlayer.put(playerId, new CabinHomeBinding(playerId, cabinId, bedPosition));
		setDirty();
	}
}
