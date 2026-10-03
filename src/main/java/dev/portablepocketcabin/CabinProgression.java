package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Persisted, player-owned progression state. Balancing definitions live elsewhere. */
public record CabinProgression(int generalSize, List<CabinRoom> rooms, List<CabinCorridor> corridors) {
	public static final int INITIAL_GENERAL_SIZE = 3;
	public static final int GENERAL_SIZE_STEP = 2;
	public static final int ABSOLUTE_MAX_GENERAL_SIZE = 21;
	public static final CabinProgression INITIAL = new CabinProgression(INITIAL_GENERAL_SIZE, List.of());
	public static final Codec<CabinProgression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.optionalFieldOf("general_size", INITIAL_GENERAL_SIZE).forGetter(CabinProgression::generalSize),
		CabinRoom.CODEC.listOf().optionalFieldOf("rooms", List.of()).forGetter(CabinProgression::rooms),
		CabinCorridor.CODEC.listOf().optionalFieldOf("corridors", List.of()).forGetter(CabinProgression::corridors)
	).apply(instance, CabinProgression::new));

	public CabinProgression(int generalSize, List<CabinRoom> rooms) {
		this(generalSize, rooms, List.of());
	}

	public CabinProgression {
		Objects.requireNonNull(rooms, "rooms");
		rooms = List.copyOf(rooms);
		corridors = List.copyOf(corridors);
		if (new HashSet<>(corridors).size() != corridors.size()) {
			throw new IllegalArgumentException("Duplicate corridor in cabin progression");
		}
		if (!corridors.isEmpty() && generalSize < 5) {
			throw new IllegalArgumentException("Corridors require at least 5x5 general space");
		}
		if (!isSupportedGeneralSize(generalSize)) {
			throw new IllegalArgumentException("General cabin size must be odd and between "
				+ INITIAL_GENERAL_SIZE + " and " + ABSOLUTE_MAX_GENERAL_SIZE);
		}
		var ids = new HashSet<>();
		var cells = new HashSet<>();
		var types = new HashSet<>();
		for (CabinRoom room : rooms) {
			if (!types.add(room.type())) throw new IllegalArgumentException("Duplicate room type in cabin progression");
			if (room.space().isPresent() && !corridors.contains(room.space().get().corridor())) {
				throw new IllegalArgumentException("Connected rooms require their installed corridor");
			}
			if (!ids.add(room.uuid())) {
				throw new IllegalArgumentException("Duplicate room UUID in cabin progression: " + room.uuid());
			}
			if (room.space().isEmpty() && !cells.add(room.cellIndex())) {
				throw new IllegalArgumentException("Duplicate room cell in cabin progression: " + room.cellIndex());
			}
		}
	}

	static boolean isSupportedGeneralSize(int size) {
		return size >= INITIAL_GENERAL_SIZE && size <= ABSOLUTE_MAX_GENERAL_SIZE
			&& (size - INITIAL_GENERAL_SIZE) % GENERAL_SIZE_STEP == 0;
	}

	CabinProgression withGeneralSize(int size) {
		int expansions = (size - generalSize) / GENERAL_SIZE_STEP;
		return new CabinProgression(size, rooms.stream().map(room -> room.shifted(expansions)).toList(), corridors);
	}

	CabinProgression withRoom(CabinRoom room) {
		var updated = new java.util.ArrayList<>(rooms);
		updated.add(room);
		return new CabinProgression(generalSize, updated, corridors);
	}

	CabinProgression withCorridor(CabinCorridor corridor) {
		if (corridors.contains(corridor)) return this;
		var updated = new java.util.ArrayList<>(corridors);
		updated.add(corridor);
		return new CabinProgression(generalSize, rooms, updated);
	}
}
