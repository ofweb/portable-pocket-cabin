package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.UUID;
import java.util.Optional;

/** A permanent allocation in the pocket dimension belonging to one cabin. */
public record CabinRoom(UUID uuid, Identifier type, long cellIndex, Optional<CabinRoomSpace> space) {
	public static final Codec<CabinRoom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(CabinRoom::uuid),
		Identifier.CODEC.fieldOf("type").forGetter(CabinRoom::type),
		Codec.LONG.fieldOf("cell_index").forGetter(CabinRoom::cellIndex),
		CabinRoomSpace.CODEC.optionalFieldOf("space").forGetter(CabinRoom::space)
	).apply(instance, CabinRoom::new));

	public CabinRoom(UUID uuid, Identifier type, long cellIndex) {
		this(uuid, type, cellIndex, Optional.empty());
	}

	CabinRoom shifted(int expansions) {
		return new CabinRoom(uuid, type, cellIndex, space.map(value -> value.shifted(expansions)));
	}

	public CabinRoom {
		Objects.requireNonNull(uuid, "uuid");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(space, "space");
		if (cellIndex < 0 || cellIndex > PocketDimension.MAX_CELL_INDEX) {
			throw new IllegalArgumentException("Room cell index is outside the supported grid: " + cellIndex);
		}
	}
}
