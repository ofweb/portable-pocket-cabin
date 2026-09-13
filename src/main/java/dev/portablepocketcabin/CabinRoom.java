package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.UUID;

/** A permanent allocation in the pocket dimension belonging to one cabin. */
public record CabinRoom(UUID uuid, Identifier type, long cellIndex) {
	public static final Codec<CabinRoom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(CabinRoom::uuid),
		Identifier.CODEC.fieldOf("type").forGetter(CabinRoom::type),
		Codec.LONG.fieldOf("cell_index").forGetter(CabinRoom::cellIndex)
	).apply(instance, CabinRoom::new));

	public CabinRoom {
		Objects.requireNonNull(uuid, "uuid");
		Objects.requireNonNull(type, "type");
		if (cellIndex < 0 || cellIndex > PocketDimension.MAX_CELL_INDEX) {
			throw new IllegalArgumentException("Room cell index is outside the supported grid: " + cellIndex);
		}
	}
}
