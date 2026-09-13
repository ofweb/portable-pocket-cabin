package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record CabinRecord(
	UUID uuid,
	UUID owner,
	long cellIndex,
	CabinLifecycle lifecycle,
	Optional<CabinExterior> exterior,
	Optional<CabinExterior> lastExterior,
	boolean interiorGenerated,
	long packedItemGeneration,
	boolean exteriorCleanupPending,
	CabinPalette palette,
	Optional<UUID> lastDeploymentItemId,
	boolean deploymentItemDeliveryPending,
	CabinEntryPermission entryPermission,
	List<UUID> trustedPlayers,
	CabinProgression progression
) {
	public static final Codec<CabinRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(CabinRecord::uuid),
		UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(CabinRecord::owner),
		Codec.LONG.fieldOf("cell_index").forGetter(CabinRecord::cellIndex),
		CabinLifecycle.CODEC.fieldOf("lifecycle").forGetter(CabinRecord::lifecycle),
		CabinExterior.CODEC.optionalFieldOf("exterior").forGetter(CabinRecord::exterior),
		CabinExterior.CODEC.optionalFieldOf("last_exterior").forGetter(CabinRecord::lastExterior),
		Codec.BOOL.optionalFieldOf("interior_generated", false).forGetter(CabinRecord::interiorGenerated),
		Codec.LONG.optionalFieldOf("packed_item_generation", 0L).forGetter(CabinRecord::packedItemGeneration),
		Codec.BOOL.optionalFieldOf("exterior_cleanup_pending", false).forGetter(CabinRecord::exteriorCleanupPending),
		CabinPalette.CODEC.fieldOf("palette").forGetter(CabinRecord::palette),
		UUIDUtil.STRING_CODEC.optionalFieldOf("last_deployment_item_uuid")
			.forGetter(CabinRecord::lastDeploymentItemId),
		Codec.BOOL.optionalFieldOf("deployment_item_delivery_pending", false)
			.forGetter(CabinRecord::deploymentItemDeliveryPending),
		CabinEntryPermission.CODEC.optionalFieldOf("entry_permission", CabinEntryPermission.OWNER_ONLY)
			.forGetter(CabinRecord::entryPermission),
		UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("trusted_players", List.of())
			.forGetter(CabinRecord::trustedPlayers),
		CabinProgression.CODEC.optionalFieldOf("progression", CabinProgression.INITIAL)
			.forGetter(CabinRecord::progression)
	).apply(instance, CabinRecord::new));

	public CabinRecord(UUID uuid, UUID owner, long cellIndex, CabinLifecycle lifecycle) {
		this(uuid, owner, cellIndex, lifecycle, Optional.empty(), Optional.empty(), false, 0L, false,
			CabinPalette.DEFAULT, Optional.empty(), false, CabinEntryPermission.OWNER_ONLY, List.of(),
			CabinProgression.INITIAL);
	}

	public CabinRecord(
		UUID uuid, UUID owner, long cellIndex, CabinLifecycle lifecycle,
		Optional<CabinExterior> exterior, boolean interiorGenerated
	) {
		this(uuid, owner, cellIndex, lifecycle, exterior, Optional.empty(), interiorGenerated, 0L, false,
			CabinPalette.DEFAULT, Optional.empty(), false, CabinEntryPermission.OWNER_ONLY, List.of(),
			CabinProgression.INITIAL);
	}

	public CabinRecord(
		UUID uuid, UUID owner, long cellIndex, CabinLifecycle lifecycle,
		Optional<CabinExterior> exterior, Optional<CabinExterior> lastExterior,
		boolean interiorGenerated, long packedItemGeneration
	) {
		this(uuid, owner, cellIndex, lifecycle, exterior, lastExterior,
			interiorGenerated, packedItemGeneration, false, CabinPalette.DEFAULT, Optional.empty(), false,
			CabinEntryPermission.OWNER_ONLY, List.of(), CabinProgression.INITIAL);
	}

	public CabinRecord(
		UUID uuid, UUID owner, long cellIndex, CabinLifecycle lifecycle,
		Optional<CabinExterior> exterior, Optional<CabinExterior> lastExterior,
		boolean interiorGenerated, long packedItemGeneration, boolean exteriorCleanupPending
	) {
		this(uuid, owner, cellIndex, lifecycle, exterior, lastExterior, interiorGenerated,
			packedItemGeneration, exteriorCleanupPending, CabinPalette.DEFAULT, Optional.empty(), false,
			CabinEntryPermission.OWNER_ONLY, List.of(), CabinProgression.INITIAL);
	}

	public CabinRecord {
		Objects.requireNonNull(uuid, "uuid");
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(lifecycle, "lifecycle");
		Objects.requireNonNull(exterior, "exterior");
		Objects.requireNonNull(lastExterior, "lastExterior");
		Objects.requireNonNull(palette, "palette");
		Objects.requireNonNull(lastDeploymentItemId, "lastDeploymentItemId");
		Objects.requireNonNull(entryPermission, "entryPermission");
		Objects.requireNonNull(trustedPlayers, "trustedPlayers");
		Objects.requireNonNull(progression, "progression");
		trustedPlayers = List.copyOf(new LinkedHashSet<>(trustedPlayers));
		if (cellIndex < 0) {
			throw new IllegalArgumentException("Cabin cell index must be non-negative");
		}
		if ((lifecycle == CabinLifecycle.DEPLOYING
			|| lifecycle == CabinLifecycle.DEPLOYED
			|| lifecycle == CabinLifecycle.PACKING) && exterior.isEmpty()) {
			throw new IllegalArgumentException("An active cabin must have an exterior location");
		}
		if (packedItemGeneration < 0) {
			throw new IllegalArgumentException("Packed item generation must be non-negative");
		}
		if (exteriorCleanupPending && lifecycle != CabinLifecycle.PACKED) {
			throw new IllegalArgumentException("Exterior cleanup can only be pending for a packed cabin");
		}
	}

	boolean canEnter(UUID playerId) {
		return owner.equals(playerId)
			|| entryPermission == CabinEntryPermission.TRUSTED_PLAYERS && trustedPlayers.contains(playerId);
	}
}
