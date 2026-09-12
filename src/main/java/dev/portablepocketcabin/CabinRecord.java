package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record CabinRecord(
	UUID uuid,
	UUID owner,
	long cellIndex,
	CabinLifecycle lifecycle,
	Optional<CabinExterior> exterior,
	boolean interiorGenerated
) {
	public static final Codec<CabinRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(CabinRecord::uuid),
		UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(CabinRecord::owner),
		Codec.LONG.fieldOf("cell_index").forGetter(CabinRecord::cellIndex),
		CabinLifecycle.CODEC.fieldOf("lifecycle").forGetter(CabinRecord::lifecycle),
		CabinExterior.CODEC.optionalFieldOf("exterior").forGetter(CabinRecord::exterior),
		Codec.BOOL.optionalFieldOf("interior_generated", false).forGetter(CabinRecord::interiorGenerated)
	).apply(instance, CabinRecord::new));

	public CabinRecord(UUID uuid, UUID owner, long cellIndex, CabinLifecycle lifecycle) {
		this(uuid, owner, cellIndex, lifecycle, Optional.empty(), false);
	}

	public CabinRecord {
		Objects.requireNonNull(uuid, "uuid");
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(lifecycle, "lifecycle");
		Objects.requireNonNull(exterior, "exterior");
		if (cellIndex < 0) {
			throw new IllegalArgumentException("Cabin cell index must be non-negative");
		}
		if ((lifecycle == CabinLifecycle.DEPLOYING || lifecycle == CabinLifecycle.DEPLOYED)
			&& exterior.isEmpty()) {
			throw new IllegalArgumentException("An active cabin must have an exterior location");
		}
	}
}
