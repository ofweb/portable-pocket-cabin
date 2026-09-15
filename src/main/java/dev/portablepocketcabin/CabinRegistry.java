package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class CabinRegistry extends SavedData {
	private static final int SCHEMA_VERSION = 5;
	private static final int OLDEST_MIGRATABLE_SCHEMA_VERSION = 3;

	private record RegistryData(
		int schemaVersion, long nextCellIndex, List<CabinRecord> cabins,
		Optional<WorldAttunement> worldAttunement
	) {
		private static final Codec<RegistryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("schema_version")
				.xmap(value -> value.orElse(SCHEMA_VERSION), Optional::of)
				.forGetter(RegistryData::schemaVersion),
			Codec.LONG.optionalFieldOf("next_cell_index", 0L).forGetter(RegistryData::nextCellIndex),
			CabinRecord.CODEC.listOf().optionalFieldOf("cabins", List.of()).forGetter(RegistryData::cabins),
			WorldAttunement.CODEC.optionalFieldOf("world_attunement").forGetter(RegistryData::worldAttunement)
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
	private long revision;
	private Optional<WorldAttunement> worldAttunement = Optional.empty();
	private static final Set<MinecraftServer> VALIDATED_SERVERS =
		Collections.newSetFromMap(new WeakHashMap<>());

	CabinRegistry() {
	}

	private CabinRegistry(
		long nextCellIndex, List<CabinRecord> cabins, Optional<WorldAttunement> worldAttunement
	) {
		this.nextCellIndex = nextCellIndex;
		this.worldAttunement = worldAttunement;
		for (CabinRecord cabin : cabins) {
			byId.put(cabin.uuid(), cabin);
			byOwner.put(cabin.owner(), cabin.uuid());
			byCell.put(cabin.cellIndex(), cabin.uuid());
			for (CabinRoom room : cabin.progression().rooms()) {
				byCell.put(room.cellIndex(), cabin.uuid());
			}
		}
	}

	public static CabinRegistry get(MinecraftServer server) {
		validateWorldSchema(server);
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private static synchronized void validateWorldSchema(MinecraftServer server) {
		if (VALIDATED_SERVERS.contains(server)) {
			return;
		}
		Path levelFolder = DimensionType.getStorageFolder(Level.OVERWORLD, server.getWorldPath(LevelResource.ROOT));
		Path registryFile = levelFolder.resolve("data").resolve(PortablePocketCabin.MOD_ID).resolve("cabins.dat");
		if (Files.isRegularFile(registryFile)) {
			try {
				var root = NbtIo.readCompressed(registryFile, NbtAccounter.defaultQuota());
				requireSupportedSchema(root, registryFile);
			} catch (IOException exception) {
				throw new IllegalStateException("Could not inspect cabin registry " + registryFile, exception);
			}
		}
		VALIDATED_SERVERS.add(server);
	}

	static void requireSupportedSchema(net.minecraft.nbt.CompoundTag root, Path registryFile) {
		var data = root.getCompoundOrEmpty("data");
		int version = data.getIntOr("schema_version", 0);
		if (!isSupportedSchema(version)) {
			throw new IllegalStateException("Unsupported cabin registry schema version " + version
				+ " in " + registryFile + ". Back up this world and run `just fresh-world` before using this version."
			);
		}
	}

	public static void flush(MinecraftServer server) {
		server.overworld().getDataStorage().saveAndJoin();
	}

	public synchronized CabinRecord create(UUID owner) {
		return create(owner, CabinPalette.DEFAULT);
	}

	public synchronized CabinRecord create(UUID owner, CabinPalette palette) {
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

		CabinRecord cabin = new CabinRecord(
			cabinId, owner, nextCellIndex, CabinLifecycle.PACKED,
			Optional.empty(), Optional.empty(), false, 0L, false, palette, Optional.empty(), false,
			CabinEntryPermission.OWNER_ONLY, List.of(), CabinProgression.INITIAL, CabinUpgradeState.EMPTY
		);
		nextCellIndex++;
		byId.put(cabin.uuid(), cabin);
		byOwner.put(cabin.owner(), cabin.uuid());
		byCell.put(cabin.cellIndex(), cabin.uuid());
		revision++;
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

	public synchronized CabinRecord beginDeployment(UUID cabinId, UUID owner, CabinExterior exterior) {
		return beginDeployment(cabinId, owner, exterior, UUID.randomUUID());
	}

	public synchronized CabinRecord beginDeployment(
		UUID cabinId, UUID owner, CabinExterior exterior, UUID itemInstanceId
	) {
		CabinRecord cabin = byId.get(cabinId);
		if (cabin == null) {
			throw new IllegalStateException("No cabin record exists for " + cabinId);
		}
		if (!cabin.owner().equals(owner)) {
			throw new IllegalStateException("Only the cabin owner may deploy it");
		}
		if (cabin.lifecycle() != CabinLifecycle.PACKED) {
			throw new IllegalStateException("Cabin must be PACKED before deployment; current state is "
				+ cabin.lifecycle());
		}
		if (cabin.exteriorCleanupPending()) {
			throw new IllegalStateException("Cabin exterior cleanup must finish before redeployment");
		}

		CabinRecord deploying = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.DEPLOYING,
			Optional.of(exterior), cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false,
			cabin.palette(), Optional.of(itemInstanceId), true,
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(deploying);
		return deploying;
	}

	public synchronized CabinRecord markInteriorGenerated(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.interiorGenerated()) {
			return cabin;
		}
		CabinRecord generated = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(),
			cabin.lastExterior(), true, cabin.packedItemGeneration(), cabin.exteriorCleanupPending(),
			cabin.palette(), cabin.lastDeploymentItemId(), cabin.deploymentItemDeliveryPending(),
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(generated);
		return generated;
	}

	public synchronized CabinRecord finishDeployment(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYING) {
			throw new IllegalStateException("Cabin must be DEPLOYING before it can become DEPLOYED");
		}
		if (!cabin.interiorGenerated()) {
			throw new IllegalStateException("Cabin interior must be generated before deployment can finish");
		}
		CabinRecord deployed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.DEPLOYED,
			cabin.exterior(), cabin.exterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false,
			cabin.palette(), cabin.lastDeploymentItemId(), false,
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(deployed);
		return deployed;
	}

	public synchronized CabinRecord beginPacking(UUID cabinId, UUID owner) {
		return beginPacking(cabinId, owner, UUID.randomUUID());
	}

	public synchronized CabinRecord beginPacking(UUID cabinId, UUID owner, UUID itemInstanceId) {
		CabinRecord cabin = require(cabinId);
		if (!cabin.owner().equals(owner)) {
			throw new IllegalStateException("Only the cabin owner may pack it");
		}
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYED || cabin.exterior().isEmpty()) {
			throw new IllegalStateException("Cabin must be DEPLOYED before packing");
		}
		CabinRecord packing = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.PACKING,
			cabin.exterior(), cabin.exterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false,
			cabin.palette(), Optional.of(itemInstanceId), true,
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(packing);
		return packing;
	}

	public synchronized CabinRecord abortPacking(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() != CabinLifecycle.PACKING) {
			return cabin;
		}
		CabinRecord deployed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.DEPLOYED,
			cabin.exterior(), cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false,
			cabin.palette(), cabin.lastDeploymentItemId(), false,
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(deployed);
		return deployed;
	}

	public synchronized CabinRecord finishPacking(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() != CabinLifecycle.PACKING) {
			throw new IllegalStateException("Cabin must be PACKING before it can become PACKED");
		}
		if (cabin.packedItemGeneration() == Long.MAX_VALUE) {
			throw new IllegalStateException("Packed item generation is exhausted");
		}
		CabinRecord packed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.PACKED,
			Optional.empty(), cabin.exterior(), cabin.interiorGenerated(), cabin.packedItemGeneration() + 1, true,
			cabin.palette(), cabin.lastDeploymentItemId(), true,
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(packed);
		return packed;
	}

	public synchronized CabinRecord markExteriorCleanupComplete(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (!cabin.exteriorCleanupPending()) {
			return cabin;
		}
		CabinRecord cleaned = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(),
			cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false,
			cabin.palette(), cabin.lastDeploymentItemId(), cabin.deploymentItemDeliveryPending(),
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(cleaned);
		return cleaned;
	}

	public synchronized CabinRecord rollbackDeployment(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYING) {
			return cabin;
		}
		CabinRecord packed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.PACKED,
			Optional.empty(), cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(), false,
			cabin.palette(), cabin.lastDeploymentItemId(), true,
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(packed);
		return packed;
	}

	public synchronized CabinRecord resolveDeploymentItemDelivery(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (!cabin.deploymentItemDeliveryPending()) {
			return cabin;
		}
		CabinRecord resolved = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(),
			cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(),
			cabin.exteriorCleanupPending(), cabin.palette(), cabin.lastDeploymentItemId(), false,
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(resolved);
		return resolved;
	}

	public synchronized CabinRecord markOrphaned(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		Optional<CabinExterior> lastExterior = cabin.exterior().isPresent() ? cabin.exterior() : cabin.lastExterior();
		CabinRecord orphaned = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.ORPHANED,
			Optional.empty(), lastExterior, cabin.interiorGenerated(), cabin.packedItemGeneration(), false,
			cabin.palette(), cabin.lastDeploymentItemId(), cabin.deploymentItemDeliveryPending(),
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(orphaned);
		return orphaned;
	}

	public synchronized CabinRecord recoverPacked(UUID cabinId) {
		CabinRecord cabin = require(cabinId);
		if (cabin.lifecycle() == CabinLifecycle.DEPLOYED && cabin.exterior().isPresent()) {
			throw new IllegalStateException("Cannot recover an item while a valid deployed exterior is registered");
		}
		if (cabin.packedItemGeneration() == Long.MAX_VALUE) {
			throw new IllegalStateException("Packed item generation is exhausted");
		}
		Optional<CabinExterior> lastExterior = cabin.exterior().isPresent() ? cabin.exterior() : cabin.lastExterior();
		CabinRecord packed = new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), CabinLifecycle.PACKED,
			Optional.empty(), lastExterior, cabin.interiorGenerated(), cabin.packedItemGeneration() + 1, false,
			cabin.palette(), Optional.empty(), false,
			cabin.entryPermission(), cabin.trustedPlayers(), cabin.progression(), cabin.upgrades()
		);
		replace(packed);
		return packed;
	}

	public synchronized CabinRecord setEntryPermission(
		UUID cabinId, UUID owner, CabinEntryPermission permission
	) {
		CabinRecord cabin = requireOwned(cabinId, owner);
		CabinRecord updated = copyAccess(cabin, permission, cabin.trustedPlayers());
		replace(updated);
		return updated;
	}

	public synchronized CabinRecord trust(UUID cabinId, UUID owner, UUID playerId) {
		CabinRecord cabin = requireOwned(cabinId, owner);
		if (playerId.equals(owner)) {
			throw new IllegalStateException("The owner already has permanent cabin access");
		}
		List<UUID> trusted = new ArrayList<>(cabin.trustedPlayers());
		if (!trusted.contains(playerId)) {
			trusted.add(playerId);
		}
		CabinRecord updated = copyAccess(cabin, cabin.entryPermission(), trusted);
		replace(updated);
		return updated;
	}

	public synchronized CabinRecord untrust(UUID cabinId, UUID owner, UUID playerId) {
		CabinRecord cabin = requireOwned(cabinId, owner);
		List<UUID> trusted = new ArrayList<>(cabin.trustedPlayers());
		trusted.remove(playerId);
		CabinRecord updated = copyAccess(cabin, cabin.entryPermission(), trusted);
		replace(updated);
		return updated;
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

	public synchronized Optional<WorldAttunement> worldAttunement() {
		return worldAttunement;
	}

	public synchronized WorldAttunement resolveWorldAttunement(WorldAttunement resolution) {
		if (worldAttunement.isPresent()) {
			return worldAttunement.get();
		}
		worldAttunement = Optional.of(resolution);
		revision++;
		setDirty();
		return resolution;
	}

	public synchronized CabinRecord expandGeneralSpace(
		UUID cabinId, UUID owner, int expectedSize, int targetSize, int configuredMaximum
	) {
		CabinRecord cabin = requireOwned(cabinId, owner);
		if (cabin.progression().generalSize() != expectedSize || targetSize != expectedSize + 1) {
			throw new IllegalStateException("Cabin size changed before the upgrade could commit");
		}
		if (targetSize > configuredMaximum) {
			throw new IllegalStateException("Cabin has reached the configured general-space limit");
		}
		CabinRecord updated = copyProgression(cabin, cabin.progression().withGeneralSize(targetSize));
		replace(updated);
		return updated;
	}

	synchronized CabinRecord updateUpgradeState(UUID cabinId, CabinUpgradeState upgrades) {
		CabinRecord cabin = require(cabinId);
		CabinRecord updated = copyUpgrades(cabin, upgrades);
		replace(updated);
		return updated;
	}

	synchronized CabinRecord completeUpgradeInstallation(UUID cabinId, UUID operationId) {
		CabinRecord cabin = require(cabinId);
		CabinUpgradeState.Installation installation = cabin.upgrades().installation()
			.orElseThrow(() -> new IllegalStateException("No upgrade installation is in progress"));
		if (!installation.operationId().equals(operationId)) {
			throw new IllegalStateException("Upgrade installation operation changed before commit");
		}
		int targetSize = installation.target().generalSpaceSize();
		int currentSize = cabin.progression().generalSize();
		if (currentSize != installation.expectedSize() && currentSize != targetSize) {
			throw new IllegalStateException("Cabin size changed during upgrade installation");
		}
		CabinProgression progression = currentSize == targetSize
			? cabin.progression() : cabin.progression().withGeneralSize(targetSize);
		CabinRecord updated = copyProgressionAndUpgrades(
			cabin, progression, cabin.upgrades().completeInstallation(installation.target())
		);
		replace(updated);
		return updated;
	}

	public synchronized CabinRoom allocateRoom(UUID cabinId, UUID owner, net.minecraft.resources.Identifier type) {
		CabinRecord cabin = requireOwned(cabinId, owner);
		if (nextCellIndex > PocketDimension.MAX_CELL_INDEX) {
			throw new IllegalStateException("Pocket-home cell grid is exhausted");
		}
		CabinRoom room = new CabinRoom(UUID.randomUUID(), type, nextCellIndex++);
		CabinRecord updated = copyProgression(cabin, cabin.progression().withRoom(room));
		byCell.put(room.cellIndex(), cabin.uuid());
		replace(updated);
		return room;
	}

	synchronized long revision() {
		return revision;
	}

	private CabinRecord require(UUID cabinId) {
		CabinRecord cabin = byId.get(cabinId);
		if (cabin == null) {
			throw new IllegalStateException("No cabin record exists for " + cabinId);
		}
		return cabin;
	}

	private CabinRecord requireOwned(UUID cabinId, UUID owner) {
		CabinRecord cabin = require(cabinId);
		if (!cabin.owner().equals(owner)) {
			throw new IllegalStateException("Only the cabin owner may change its access settings");
		}
		return cabin;
	}

	private static CabinRecord copyAccess(
		CabinRecord cabin, CabinEntryPermission permission, List<UUID> trustedPlayers
	) {
		return new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(),
			cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(),
			cabin.exteriorCleanupPending(), cabin.palette(), cabin.lastDeploymentItemId(),
			cabin.deploymentItemDeliveryPending(), permission, trustedPlayers, cabin.progression(), cabin.upgrades()
		);
	}

	private static CabinRecord copyProgression(CabinRecord cabin, CabinProgression progression) {
		return new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(),
			cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(),
			cabin.exteriorCleanupPending(), cabin.palette(), cabin.lastDeploymentItemId(),
			cabin.deploymentItemDeliveryPending(), cabin.entryPermission(), cabin.trustedPlayers(), progression,
			cabin.upgrades()
		);
	}

	private static CabinRecord copyUpgrades(CabinRecord cabin, CabinUpgradeState upgrades) {
		return copyProgressionAndUpgrades(cabin, cabin.progression(), upgrades);
	}

	private static CabinRecord copyProgressionAndUpgrades(
		CabinRecord cabin, CabinProgression progression, CabinUpgradeState upgrades
	) {
		return new CabinRecord(
			cabin.uuid(), cabin.owner(), cabin.cellIndex(), cabin.lifecycle(), cabin.exterior(),
			cabin.lastExterior(), cabin.interiorGenerated(), cabin.packedItemGeneration(),
			cabin.exteriorCleanupPending(), cabin.palette(), cabin.lastDeploymentItemId(),
			cabin.deploymentItemDeliveryPending(), cabin.entryPermission(), cabin.trustedPlayers(),
			progression, upgrades
		);
	}

	private void replace(CabinRecord cabin) {
		byId.put(cabin.uuid(), cabin);
		revision++;
		setDirty();
	}

	private synchronized RegistryData encode() {
		return new RegistryData(SCHEMA_VERSION, nextCellIndex, new ArrayList<>(byId.values()), worldAttunement);
	}

	private static DataResult<CabinRegistry> decode(RegistryData data) {
		if (!isSupportedSchema(data.schemaVersion())) {
			return DataResult.error(() -> "Unsupported cabin registry schema version " + data.schemaVersion()
				+ ". Back up this world and run `just fresh-world` before using this version.");
		}
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
			for (CabinRoom room : cabin.progression().rooms()) {
				if (cells.putIfAbsent(room.cellIndex(), cabin) != null) {
					return DataResult.error(() -> "Duplicate allocated room cell in saved registry: "
						+ room.cellIndex());
				}
				repairedNextCellIndex = Math.max(repairedNextCellIndex, room.cellIndex() + 1);
			}
		}

		if (repairedNextCellIndex > PocketDimension.MAX_CELL_INDEX + 1) {
			return DataResult.error(() -> "Cabin next cell index is outside the supported grid");
		}
		CabinRegistry registry = new CabinRegistry(
			repairedNextCellIndex, data.cabins(), data.worldAttunement()
		);
		if (data.schemaVersion() != SCHEMA_VERSION) {
			registry.setDirty();
		}
		return DataResult.success(registry);
	}

	private static boolean isSupportedSchema(int version) {
		return version >= OLDEST_MIGRATABLE_SCHEMA_VERSION && version <= SCHEMA_VERSION;
	}
}
