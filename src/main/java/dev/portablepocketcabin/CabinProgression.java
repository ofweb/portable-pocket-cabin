package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Persisted, player-owned progression state. Balancing definitions live elsewhere. */
public record CabinProgression(int generalSize, List<CabinRoom> rooms) {
	public static final int INITIAL_GENERAL_SIZE = 4;
	public static final int ABSOLUTE_MAX_GENERAL_SIZE = 32;
	public static final CabinProgression INITIAL = new CabinProgression(INITIAL_GENERAL_SIZE, List.of());
	public static final Codec<CabinProgression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.optionalFieldOf("general_size", INITIAL_GENERAL_SIZE).forGetter(CabinProgression::generalSize),
		CabinRoom.CODEC.listOf().optionalFieldOf("rooms", List.of()).forGetter(CabinProgression::rooms)
	).apply(instance, CabinProgression::new));

	public CabinProgression {
		Objects.requireNonNull(rooms, "rooms");
		rooms = List.copyOf(rooms);
		if (generalSize < INITIAL_GENERAL_SIZE || generalSize > ABSOLUTE_MAX_GENERAL_SIZE) {
			throw new IllegalArgumentException("General cabin size must be between "
				+ INITIAL_GENERAL_SIZE + " and " + ABSOLUTE_MAX_GENERAL_SIZE);
		}
		var ids = new HashSet<>();
		var cells = new HashSet<>();
		for (CabinRoom room : rooms) {
			if (!ids.add(room.uuid())) {
				throw new IllegalArgumentException("Duplicate room UUID in cabin progression: " + room.uuid());
			}
			if (!cells.add(room.cellIndex())) {
				throw new IllegalArgumentException("Duplicate room cell in cabin progression: " + room.cellIndex());
			}
		}
	}

	CabinProgression withGeneralSize(int size) {
		return new CabinProgression(size, rooms);
	}

	CabinProgression withRoom(CabinRoom room) {
		var updated = new java.util.ArrayList<>(rooms);
		updated.add(room);
		return new CabinProgression(generalSize, updated);
	}
}
