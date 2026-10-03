package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

final class CabinGreenhouseGrowth extends SavedData {
	private record Sleep(UUID cabin, long clock) {
		static final Codec<Sleep> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.STRING_CODEC.fieldOf("cabin").forGetter(Sleep::cabin),
			Codec.LONG.fieldOf("clock").forGetter(Sleep::clock)).apply(i, Sleep::new));
	}
	private record State(long clock, List<Sleep> sleeps) {
		static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("clock").forGetter(State::clock),
			Sleep.CODEC.listOf().fieldOf("sleeps").forGetter(State::sleeps)).apply(i, State::new));
	}
	static final Codec<CabinGreenhouseGrowth> CODEC = State.CODEC.xmap(CabinGreenhouseGrowth::new,
		data -> new State(data.clock, data.sleeps.entrySet().stream().map(e -> new Sleep(e.getKey(), e.getValue())).toList()));
	private static final SavedDataType<CabinGreenhouseGrowth> TYPE = new SavedDataType<>(
		PortablePocketCabin.id("greenhouse_growth"), CabinGreenhouseGrowth::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
	private static final Set<String> FARMERS_DELIGHT_CROPS = Set.of("cabbages", "onions", "tomatoes", "tomatoes_on_rope", "rice", "rice_panicles");
	private long clock;
	private final Map<UUID, Long> sleeps = new LinkedHashMap<>();
	private CabinGreenhouseGrowth() { }
	private CabinGreenhouseGrowth(State state) {
		clock = state.clock();
		for (var sleep : state.sleeps()) sleeps.put(sleep.cabin(), sleep.clock());
	}
	static boolean hasPending(MinecraftServer server, UUID cabin) { return Files.exists(journal(server, cabin)); }
	static CabinGreenhouseGrowth get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}
	void advance(long randomSelections) { clock = Math.addExact(clock, randomSelections); setDirty(); }
	boolean sleeping(UUID cabin) { return sleeps.containsKey(cabin); }
	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			var data = get(server);
			var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
			if (pocket == null) return;
			data.advance(pocket.getGameRules().get(GameRules.RANDOM_TICK_SPEED));
			sync(server);
		});
	}
	static void sync(MinecraftServer server) {
		var data = get(server);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) return;
		for (var cabin : CabinRegistry.get(server).cabins()) {
			if (cabin.upgrades().greenhouse().level() == 0) continue;
			boolean asleep = cabin.lifecycle() == CabinLifecycle.PACKED
				&& server.getPlayerList().getPlayers().stream().noneMatch(p -> CabinReconciliation.isOccupant(p, cabin));
			if (asleep && !data.sleeps.containsKey(cabin.uuid())) {
				data.sleeps.put(cabin.uuid(), data.clock); data.setDirty();
			} else if (CabinSimulation.shouldSimulate(cabin) && (data.sleeps.containsKey(cabin.uuid()) || hasPending(server, cabin.uuid()))) {
				resume(pocket, cabin, data);
			}
		}
	}

	private static void resume(ServerLevel level, CabinRecord cabin, CabinGreenhouseGrowth data) {
		Path path = journal(level.getServer(), cabin.uuid());
		CompoundTag receipt = prepare(level, cabin, data);
		var original = new LinkedHashMap<BlockPos, BlockState>();
		for (var tag : receipt.getListOrEmpty("blocks")) {
			var entry = (CompoundTag) tag;
			original.put(CabinCorridorSnapshot.position(entry, "pos"), BlockState.CODEC.parse(NbtOps.INSTANCE, entry.get("state")).getOrThrow());
		}
		CabinCorridors.applyWorldEffect(() -> {
			original.forEach((pos, state) -> {
				if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, CabinCorridorSnapshot.FLAGS);
			});
			waitForLight(level, original.keySet());
			catchUp(level, original.keySet(), receipt.getLongOr("elapsed", 0), receipt.getLongOr("seed", 0));
		});
		// Persist the plants before consuming elapsed time, then persist consumption before deleting the receipt.
		level.getServer().saveEverything(false, true, false);
		data.sleeps.remove(cabin.uuid()); data.setDirty();
		level.getServer().saveEverything(false, true, false);
		try {
			Files.delete(path);
			try (var directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
		} catch (java.io.IOException e) { throw new IllegalStateException("Cannot finish greenhouse catch-up", e); }
	}

	static CompoundTag prepare(ServerLevel level, CabinRecord cabin, CabinGreenhouseGrowth data) {
		Path path = journal(level.getServer(), cabin.uuid());
		CompoundTag receipt;
		if (Files.exists(path)) receipt = read(path);
		else {
			receipt = new CompoundTag();
			receipt.putLong("elapsed", Math.max(0, data.clock - data.sleeps.get(cabin.uuid())));
			receipt.putLong("seed", level.getRandom().nextLong());
			var blocks = new ListTag();
			for (var pos : CabinGreenhouse.space(cabin, cabin.upgrades().greenhouse().level()).volume(cabin.cellIndex())) {
				var entry = new CompoundTag();
				CabinCorridorSnapshot.putPosition(entry, "pos", pos);
				entry.put("state", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, level.getBlockState(pos)).getOrThrow());
				blocks.add(entry);
			}
			receipt.put("blocks", blocks);
			write(path, receipt);
		}
		return receipt;
	}

	static void waitForLight(ServerLevel level, Set<BlockPos> volume) {
		var chunks = volume.stream().map(pos -> new net.minecraft.world.level.ChunkPos(pos.getX() >> 4, pos.getZ() >> 4)).collect(java.util.stream.Collectors.toSet());
		for (var chunk : chunks) {
			level.getChunk(chunk.x(), chunk.z());
			var future = level.getChunkSource().getLightEngine().waitForPendingTasks(chunk.x(), chunk.z());
			level.getServer().managedBlock(future::isDone);
			future.join();
		}
	}

	static void catchUp(ServerLevel level, Set<BlockPos> volume, long elapsed, long seed) {
		var random = RandomSource.create(seed);
		// A block receives randomTickSpeed / 4096 random ticks per server tick on average.
		int rounds = (int) Math.min(8192, elapsed / 4096 + (random.nextDouble() < (elapsed % 4096) / 4096.0 ? 1 : 0));
		for (int round = 0; round < rounds; round++) {
			boolean growing = false;
			for (var pos : volume) {
				var state = level.getBlockState(pos);
				if (!canProgress(level, pos, state) || !state.canSurvive(level, pos) || !state.isRandomlyTicking()) continue;
				if (state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS)) {
					if (level.getBlockState(pos.above()).is(state.getBlock())) continue;
					int height = 1;
					while (height < 3 && level.getBlockState(pos.below(height)).is(state.getBlock())) height++;
					if (height >= 3) continue;
				}
				growing = true;
				state.randomTick(level, pos, random);
			}
			if (!growing) break;
		}
	}

	static boolean canProgress(ServerLevel level, BlockPos pos, BlockState state) {
		var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (id.getNamespace().equals("farmersdelight")) {
			if (id.getPath().equals("budding_tomatoes")) return true;
			if (id.getPath().equals("rice") && level.getBlockState(pos.above()).isAir()) return true;
		}
		return canProgress(state);
	}

	static boolean canProgress(BlockState state) {
		Block block = state.getBlock();
		var id = BuiltInRegistries.BLOCK.getKey(block);
		boolean supported = id.getNamespace().equals("minecraft") && (block instanceof CropBlock || block instanceof StemBlock || block instanceof PitcherCropBlock
			|| block instanceof SweetBerryBushBlock || block instanceof CocoaBlock || block instanceof NetherWartBlock)
			|| id.getNamespace().equals("farmersdelight") && FARMERS_DELIGHT_CROPS.contains(id.getPath());
		if (state.is(Blocks.BAMBOO_SAPLING)) return true;
		if (id.getNamespace().equals("minecraft") && (block instanceof SugarCaneBlock || block instanceof CactusBlock)) return true;
		if (id.getNamespace().equals("minecraft") && block instanceof BambooStalkBlock) return state.getValue(BambooStalkBlock.STAGE) == 0;
		if (id.getNamespace().equals("minecraft") && block instanceof GrowingPlantHeadBlock head) return !head.isMaxAge(state);
		if (!supported) return false;
		for (var property : state.getProperties())
			if (property instanceof IntegerProperty age && age.getName().equals("age"))
				return state.getValue(age) < age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
		return false;
	}
	private static Path journal(MinecraftServer server, UUID cabin) {
		return server.getWorldPath(LevelResource.ROOT).resolve("data/portable_pocket_cabin/greenhouse_growth/" + cabin + ".nbt");
	}
	private static CompoundTag read(Path path) {
		try { return Objects.requireNonNull(NbtIo.read(path)); }
		catch (java.io.IOException e) { throw new IllegalStateException("Cannot read greenhouse catch-up", e); }
	}
	private static void write(Path path, CompoundTag receipt) {
		try {
			Files.createDirectories(path.getParent());
			Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
			NbtIo.write(receipt, temporary);
			try (var file = FileChannel.open(temporary, StandardOpenOption.WRITE)) { file.force(true); }
			Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			try (var directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
		} catch (java.io.IOException e) { throw new IllegalStateException("Cannot persist greenhouse catch-up", e); }
	}
}
