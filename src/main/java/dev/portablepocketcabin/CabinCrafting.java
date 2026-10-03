package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

final class CabinCrafting {
	static final Identifier TYPE = PortablePocketCabin.id("crafting_room");
	private static final String DROP_TAG = PortablePocketCabin.MOD_ID + ".crafting_displacement";
	private CabinCrafting() { }

	static CabinRoomSpace space(CabinRecord cabin) {
		return cabin.progression().rooms().stream().filter(room -> room.type().equals(TYPE))
			.findFirst().flatMap(CabinRoom::space).orElseGet(() -> {
				int z = -cabin.progression().generalSize() / 2 - 5;
				return new CabinRoomSpace(CabinCorridor.NORTH, new BlockPos(-8, 0, z - 3),
					new BlockPos(-2, 4, z + 3), new BlockPos(-2, 1, z));
			});
	}

	static Map<BlockPos, BlockState> stations(CabinRecord cabin, int level) {
		if (level == 0) return Map.of();
		BlockPos corner = PocketDimension.cellCenter(cabin.cellIndex()).offset(space(cabin).minimum()).offset(1, 1, 1);
		var result = new LinkedHashMap<BlockPos, BlockState>();
		result.put(corner, Blocks.CRAFTING_TABLE.defaultBlockState());
		result.put(corner.offset(2, 0, 0), Blocks.LOOM.defaultBlockState());
		result.put(corner.offset(4, 0, 0), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
		if (level >= 2) result.put(corner.offset(0, 0, 2), Blocks.STONECUTTER.defaultBlockState());
		if (level >= 3) result.put(corner.offset(0, 0, 4), Blocks.SMITHING_TABLE.defaultBlockState());
		return result;
	}

	static CabinUpgradeService.Outcome validate(ServerLevel pocket, CabinRecord cabin, int target) {
		if (target != cabin.upgrades().crafting().level() + 1)
			return CabinUpgradeService.Outcome.failure("Crafting requires the previous level.");
		if (target != 1) return CabinUpgradeService.Outcome.success("");
		var corridor = CabinCorridors.validateInstall(pocket, cabin, CabinCorridor.NORTH);
		if (!corridor.success()) return corridor;
		var volume = space(cabin).volume(cabin.cellIndex());
		for (CabinRoom room : cabin.progression().rooms())
			if (room.space().isPresent() && !Collections.disjoint(volume, room.space().get().volume(cabin.cellIndex())))
				return CabinUpgradeService.Outcome.failure("Crafting room overlaps an installed room.");
		var shared = CabinCorridorLayout.blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), CabinCorridor.NORTH);
		for (BlockPos pos : volume) {
			BlockState state = pocket.getBlockState(pos);
			if (!state.isAir() && !state.equals(shared.get(pos)))
				return CabinUpgradeService.Outcome.failure("Crafting room installation is obstructed at " + pos.toShortString());
		}
		return CabinUpgradeService.Outcome.success("");
	}

	static boolean paused(CabinRecord cabin, BlockPos pos) {
		var installing = cabin.upgrades().installation().filter(i -> i.target().isCrafting());
		if (installing.isEmpty()) return false;
		return space(cabin).volume(cabin.cellIndex()).contains(pos)
			|| installing.get().expectedState() == 0 && CabinCorridorLayout.volume(cabin.cellIndex(),
				cabin.progression().generalSize(), CabinCorridor.NORTH).contains(pos);
	}

	static void install(ServerLevel pocket, CabinRecord cabin, CabinUpgradeState.Installation installation) {
		CabinCorridors.applyWorldEffect(() -> installWorld(pocket, cabin, installation));
	}

	private static void installWorld(ServerLevel pocket, CabinRecord cabin, CabinUpgradeState.Installation installation) {
		Path path = journal(pocket.getServer(), cabin.uuid());
		CompoundTag receipt;
		if (Files.exists(path)) receipt = read(path);
		else {
			receipt = new CompoundTag();
			receipt.putString("operation", installation.operationId().toString());
			List<ItemStack> drops = new ArrayList<>();
			for (var entry : stations(cabin, installation.targetState()).entrySet()) {
				if (stations(cabin, installation.expectedState()).containsKey(entry.getKey())) continue;
				BlockPos pos = entry.getKey();
				var entity = pocket.getBlockEntity(pos);
				drops.addAll(Block.getDrops(pocket.getBlockState(pos), pocket, pos, entity));
				if (entity instanceof Container container && !(entity instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity))
					for (int slot = 0; slot < container.getContainerSize(); slot++)
						if (!container.getItem(slot).isEmpty()) drops.add(container.getItem(slot).copy());
			}
			ListTag list = new ListTag();
			for (ItemStack stack : drops) {
				CompoundTag item = new CompoundTag();
				item.store("stack", ItemStack.CODEC, pocket.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack);
				list.add(item);
			}
			receipt.put("drops", list);
			write(path, receipt);
		}
		if (!receipt.getStringOr("operation", "").equals(installation.operationId().toString()))
			throw new IllegalStateException("Crafting installation receipt changed");
		CabinCorridors.applyWorldEffect(() -> {
			if (installation.targetState() == 1) {
				if (!cabin.progression().corridors().contains(CabinCorridor.NORTH))
					CabinCorridorLayout.blocks(cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette(), CabinCorridor.NORTH)
						.forEach((pos, state) -> pocket.setBlock(pos, state, CabinCorridorSnapshot.FLAGS));
				var space = space(cabin);
				for (BlockPos pos : space.volume(cabin.cellIndex())) {
					if (!space.shell(cabin.cellIndex(), pos)) continue;
					int y = pos.getY() - PocketDimension.cellCenter(cabin.cellIndex()).getY();
					Block block = y == 0 ? cabin.palette().floor().planksBlock()
						: y == 4 ? cabin.palette().roof().planksBlock() : cabin.palette().walls().planksBlock();
					pocket.setBlock(pos, block.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
				}
				BlockPos entrance = PocketDimension.cellCenter(cabin.cellIndex()).offset(space.entrance());
				pocket.setBlock(entrance, Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
				pocket.setBlock(entrance.above(), Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
			}
			for (var entry : stations(cabin, installation.targetState()).entrySet()) {
				if (stations(cabin, installation.expectedState()).containsKey(entry.getKey())) continue;
				pocket.removeBlockEntity(entry.getKey());
				pocket.setBlock(entry.getKey(), entry.getValue(), CabinCorridorSnapshot.FLAGS);
			}
		});
		var drops = receipt.getListOrEmpty("drops");
		BlockPos delivery = PocketDimension.cellCenter(cabin.cellIndex()).offset(space(cabin).entrance()).west(2);
		pocket.getChunkAt(delivery);
		pocket.waitForEntities(new net.minecraft.world.level.ChunkPos(delivery.getX() >> 4, delivery.getZ() >> 4), 0);
		for (int index = 0; index < drops.size(); index++) {
			UUID id = UUID.nameUUIDFromBytes((installation.operationId() + ":" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			ItemStack stack = drops.getCompoundOrEmpty(index).read("stack", ItemStack.CODEC,
				pocket.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE)).orElseThrow();
			if (pocket.getEntity(id) == null) {
				var item = new ItemEntity(pocket, delivery.getX() + .5, delivery.getY() + .5, delivery.getZ() + .5, stack);
				item.setUUID(id); item.addTag(DROP_TAG); item.setNeverPickUp(); item.setUnlimitedLifetime(); item.setNoGravity(true); item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
				if (!pocket.addFreshEntity(item)) throw new IllegalStateException("Could not deliver displaced items");
			}
		}
		pocket.getServer().saveEverything(false, true, false);
	}

	static void finish(MinecraftServer server, CabinRecord cabin) {
		Path path = journal(server, cabin.uuid());
		if (!Files.exists(path)) return;
		if (cabin.upgrades().installation().isPresent()) return;
		CompoundTag receipt = read(path);
		UUID operation = UUID.fromString(receipt.getStringOr("operation", ""));
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		server.saveEverything(false, true, false);
		try {
			Files.delete(path);
			try (var directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
		} catch (java.io.IOException e) { throw new IllegalStateException(e); }
		for (int index = 0; index < receipt.getListOrEmpty("drops").size(); index++) {
			UUID id = UUID.nameUUIDFromBytes((operation + ":" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			if (pocket.getEntity(id) instanceof ItemEntity item) release(item);
		}
		CabinSimulation.sync(server);
	}

	static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof ItemEntity item) || !item.entityTags().contains(DROP_TAG)) return;
			var cabin = CabinRegistry.get(level.getServer()).findByCell(
				PocketDimension.cellIndexAt(item.blockPosition()).orElse(-1L)).orElse(null);
			boolean pending = cabin != null && Files.exists(journal(level.getServer(), cabin.uuid()));
			if (!pending) release(item);
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var cabin : CabinRegistry.get(server).cabins()) finish(server, cabin);
		});
	}
	private static void release(ItemEntity item) {
		item.removeTag(DROP_TAG); item.setNoGravity(false); item.setDefaultPickUpDelay(); item.setExtendedLifetime();
	}
	private static Path journal(MinecraftServer server, UUID cabin) {
		return server.getWorldPath(LevelResource.ROOT).resolve("data/portable_pocket_cabin/crafting_installations/" + cabin + ".nbt");
	}
	private static CompoundTag read(Path path) {
		try { return Objects.requireNonNull(NbtIo.read(path)); } catch (java.io.IOException e) { throw new IllegalStateException(e); }
	}
	private static void write(Path path, CompoundTag receipt) {
		try {
			Files.createDirectories(path.getParent());
			Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
			NbtIo.write(receipt, temporary);
			try (var file = FileChannel.open(temporary, StandardOpenOption.WRITE)) { file.force(true); }
			Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			try (var directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
		} catch (java.io.IOException e) { throw new IllegalStateException("Cannot persist crafting installation", e); }
	}
}
