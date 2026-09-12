package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Objects;
import java.util.UUID;

public record CabinRecord(UUID uuid, UUID owner, long cellIndex, CabinLifecycle lifecycle) {
	public static final Codec<CabinRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(CabinRecord::uuid),
		UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(CabinRecord::owner),
		Codec.LONG.fieldOf("cell_index").forGetter(CabinRecord::cellIndex),
		CabinLifecycle.CODEC.fieldOf("lifecycle").forGetter(CabinRecord::lifecycle)
	).apply(instance, CabinRecord::new));

	public CabinRecord {
		Objects.requireNonNull(uuid, "uuid");
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(lifecycle, "lifecycle");
		if (cellIndex < 0) {
			throw new IllegalArgumentException("Cabin cell index must be non-negative");
		}
	}
}
