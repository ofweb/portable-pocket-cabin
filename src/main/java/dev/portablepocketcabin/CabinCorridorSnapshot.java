package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

import java.util.*;

/** Authoritative source contents and absolute destinations; replay never captures a partial move. */
final class CabinCorridorSnapshot {
	static final int FLAGS = Block.UPDATE_SKIP_ALL_SIDEEFFECTS | Block.UPDATE_CLIENTS;
	// These records contain local state; external coordinate records need a relocation adapter.
	private static final Set<String> LOCAL_BLOCK_ENTITIES = Set.of("furnace", "chest", "trapped_chest", "ender_chest",
		"jukebox", "dispenser", "dropper", "sign", "hanging_sign", "brewing_stand", "enchanting_table", "hopper",
		"comparator", "banner", "skull", "bed", "shulker_box", "smoker", "blast_furnace", "lectern", "bell",
		"campfire", "chiseled_bookshelf", "decorated_pot", "crafter", "beacon");
	private static final Set<String> LOCAL_ENTITIES = Set.of("cow", "mooshroom", "sheep", "pig", "chicken", "goat",
		"horse", "donkey", "mule", "llama", "trader_llama", "rabbit", "cat", "wolf", "parrot", "ocelot",
		"item", "experience_orb", "item_frame", "glow_item_frame", "painting", "armor_stand",
		"minecart", "chest_minecart", "hopper_minecart", "furnace_minecart", "tnt_minecart");
	record Cell(BlockPos source, BlockPos destination, BlockState state, CompoundTag data) { }
	private final List<Cell> cells;
	private final List<CompoundTag> ticks;
	private final List<CompoundTag> entities;
	private final long capturedTime;

	private CabinCorridorSnapshot(List<Cell> cells, List<CompoundTag> ticks, List<CompoundTag> entities, long capturedTime) {
		this.cells = List.copyOf(cells);
		this.ticks = List.copyOf(ticks);
		this.entities = List.copyOf(entities);
		this.capturedTime = capturedTime;
	}

	static CabinCorridorSnapshot capture(ServerLevel level, Map<BlockPos, BlockPos> destinations) {
		return capture(level, destinations, Set.of());
	}

	static CabinCorridorSnapshot capture(ServerLevel level, Map<BlockPos, BlockPos> destinations, Set<Block> structuralBlocks) {
		validate(level, destinations, structuralBlocks);
		var cells = new ArrayList<Cell>();
		for (var entry : destinations.entrySet()) {
			BlockEntity be = level.getBlockEntity(entry.getKey());
			cells.add(new Cell(entry.getKey(), entry.getValue(), level.getBlockState(entry.getKey()),
				be == null ? null : be.saveWithFullMetadata(level.registryAccess()).copy()));
		}
		var ticks = new ArrayList<CompoundTag>();
		var chunks = new HashSet<net.minecraft.world.level.ChunkPos>();
		for (BlockPos pos : destinations.keySet()) chunks.add(new net.minecraft.world.level.ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
		for (var chunkPos : chunks) {
			var chunk = level.getChunk(chunkPos.x(), chunkPos.z());
			((LevelChunkTicks<Block>) chunk.getBlockTicks()).getAll().filter(t -> destinations.containsKey(t.pos()))
				.forEach(t -> ticks.add(tick("block", BuiltInRegistries.BLOCK.getKey(t.type()), destinations.get(t.pos()),
					t.triggerTick(), t.priority(), t.subTickOrder())));
			((LevelChunkTicks<net.minecraft.world.level.material.Fluid>) chunk.getFluidTicks()).getAll()
				.filter(t -> destinations.containsKey(t.pos())).forEach(t -> ticks.add(tick("fluid",
					BuiltInRegistries.FLUID.getKey(t.type()), destinations.get(t.pos()), t.triggerTick(), t.priority(), t.subTickOrder())));
		}
		var entities = new ArrayList<CompoundTag>();
		for (Entity entity : level.getAllEntities()) {
			BlockPos source = entity.blockPosition();
			if (!destinations.containsKey(source)) continue;
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			if (!entity.saveAsPassenger(output)) throw new IllegalStateException("Cannot save " + entity.getName().getString());
			CompoundTag body = new CompoundTag();
			CompoundTag data = output.buildResult();
			data.remove("Passengers");
			body.put("data", data);
			body.putString("id", entity.getUUID().toString());
			BlockPos delta = destinations.get(source).subtract(source);
			body.putDouble("x", entity.getX() + delta.getX());
			body.putDouble("y", entity.getY() + delta.getY());
			body.putDouble("z", entity.getZ() + delta.getZ());
			if (entity.getVehicle() != null) body.putString("vehicle", entity.getVehicle().getUUID().toString());
			entities.add(body);
		}
		return new CabinCorridorSnapshot(cells, ticks, entities, level.getGameTime());
	}

	static void validate(ServerLevel level, Map<BlockPos, BlockPos> destinations) {
		validate(level, destinations, Set.of());
	}

	static void validate(ServerLevel level, Map<BlockPos, BlockPos> destinations, Set<Block> structuralBlocks) {
		for (var entry : destinations.entrySet()) {
			BlockPos source = entry.getKey(), dest = entry.getValue();
			if (!destinations.containsKey(dest) && !level.getBlockState(dest).isAir()) {
				throw new IllegalStateException("Expansion is obstructed at " + dest.toShortString());
			}
			BlockState state = level.getBlockState(source);
			BlockEntity blockEntity = level.getBlockEntity(source);
			if (blockEntity != null && !LOCAL_BLOCK_ENTITIES.contains(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).getPath())) {
				throw new IllegalStateException("Clear unsupported block entity at " + source.toShortString());
			}
			if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("minecraft")
				&& (!structuralBlocks.contains(state.getBlock()) || state.hasBlockEntity())
				|| state.is(Blocks.MOVING_PISTON)
				|| (state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON)) && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED)) {
				throw new IllegalStateException("Clear unsupported moving contents at " + source.toShortString());
			}
			BlockPos other = null;
			if (state.hasProperty(DoublePlantBlock.HALF)) {
				other = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? source.above() : source.below();
			} else if (state.getBlock() instanceof BedBlock) {
				var facing = state.getValue(BedBlock.FACING);
				other = source.relative(state.getValue(BedBlock.PART) == BedPart.FOOT ? facing : facing.getOpposite());
			} else if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
				other = source.relative(ChestBlock.getConnectedDirection(state));
			}
			if (other != null && (!destinations.containsKey(other)
				|| !destinations.get(other).subtract(other).equals(dest.subtract(source)))) {
				throw new IllegalStateException("Expansion would split an object at " + source.toShortString());
			}
		}
		for (Entity entity : level.getAllEntities()) {
			if (!destinations.containsKey(entity.blockPosition())) continue;
			String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
			if (entity instanceof Player || !BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("minecraft")
				|| !LOCAL_ENTITIES.contains(type) && !type.endsWith("_boat") && !type.endsWith("_raft")
				|| entity instanceof Leashable leashable && leashable.isLeashed()) {
				throw new IllegalStateException("Clear unsupported moving entity at " + entity.blockPosition().toShortString());
			}
			if (entity.getVehicle() != null && !sameMovement(destinations, entity.blockPosition(), entity.getVehicle().blockPosition())) {
				throw new IllegalStateException("A vehicle crosses the moving passage boundary");
			}
			if (entity.getPassengers().stream().anyMatch(passenger -> !sameMovement(destinations, entity.blockPosition(), passenger.blockPosition()))) {
				throw new IllegalStateException("A passenger crosses the moving passage boundary");
			}
		}
	}

	private static boolean sameMovement(Map<BlockPos, BlockPos> destinations, BlockPos first, BlockPos second) {
		return destinations.containsKey(second) && destinations.get(first).subtract(first).equals(destinations.get(second).subtract(second));
	}

	void apply(ServerLevel level) {
		var union = new LinkedHashSet<BlockPos>();
		for (Cell cell : cells) { union.add(cell.source()); union.add(cell.destination()); }
		for (BlockPos pos : union) {
			var box = BoundingBox.fromCorners(pos, pos);
			level.getBlockTicks().clearArea(box);
			level.getFluidTicks().clearArea(box);
			level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), FLAGS);
		}
		for (BlockPos pos : union) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
		for (Cell cell : cells) {
			level.setBlock(cell.destination(), cell.state(), FLAGS);
			if (cell.data() != null) {
				BlockEntity be = level.getBlockEntity(cell.destination());
				if (be == null) throw new IllegalStateException("Missing moved block entity at " + cell.destination());
				CompoundTag data = cell.data().copy();
				data.putInt("x", cell.destination().getX());
				data.putInt("y", cell.destination().getY());
				data.putInt("z", cell.destination().getZ());
				be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
				be.setChanged();
			}
		}
		for (CompoundTag tick : ticks) {
			var id = Identifier.parse(tick.getStringOr("type", ""));
			var pos = position(tick, "pos");
			long time = tick.getLongOr("time", 0) + Math.max(0, level.getGameTime() - capturedTime), order = tick.getLongOr("order", 0);
			var priority = TickPriority.values()[tick.getIntOr("priority", 0)];
			if (tick.getStringOr("kind", "").equals("block")) {
				level.getBlockTicks().schedule(new ScheduledTick<>(BuiltInRegistries.BLOCK.getValue(id), pos, time, priority, order));
			} else level.getFluidTicks().schedule(new ScheduledTick<>(BuiltInRegistries.FLUID.getValue(id), pos, time, priority, order));
		}
		for (CompoundTag body : entities) {
			UUID id = UUID.fromString(body.getStringOr("id", ""));
			Entity entity = level.getEntityInAnyDimension(id);
			CompoundTag data = body.getCompoundOrEmpty("data");
			var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data);
			if (entity == null) {
				var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(data.getStringOr("id", "")));
				entity = EntityType.create(type, input, level, EntitySpawnReason.COMMAND).orElseThrow();
				if (!level.addWithUUID(entity)) throw new IllegalStateException("Cannot restore moved entity " + id);
			} else entity.load(input);
			entity.stopRiding();
			entity.teleportTo(level, body.getDoubleOr("x", 0), body.getDoubleOr("y", 0), body.getDoubleOr("z", 0),
				Set.of(), entity.getYRot(), entity.getXRot(), false);
		}
		for (CompoundTag body : entities) {
			if (!body.contains("vehicle")) continue;
			Entity passenger = level.getEntityInAnyDimension(UUID.fromString(body.getStringOr("id", "")));
			Entity vehicle = level.getEntityInAnyDimension(UUID.fromString(body.getStringOr("vehicle", "")));
			if (passenger == null || vehicle == null || !passenger.startRiding(vehicle, true, false)) {
				throw new IllegalStateException("Cannot restore moved passenger");
			}
		}
	}

	Map<BlockPos, BlockPos> destinations() {
		var result = new LinkedHashMap<BlockPos, BlockPos>();
		for (Cell cell : cells) result.put(cell.source(), cell.destination());
		return Map.copyOf(result);
	}

	CompoundTag save() {
		var root = new CompoundTag();
		root.putLong("captured_time", capturedTime);
		var list = new ListTag();
		for (Cell cell : cells) {
			var tag = new CompoundTag();
			putPosition(tag, "source", cell.source());
			putPosition(tag, "destination", cell.destination());
			tag.put("state", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, cell.state()).getOrThrow());
			if (cell.data() != null) tag.put("data", cell.data().copy());
			list.add(tag);
		}
		root.put("cells", list);
		var tickList = new ListTag(); ticks.forEach(t -> tickList.add(t.copy())); root.put("ticks", tickList);
		var bodies = new ListTag(); entities.forEach(e -> bodies.add(e.copy())); root.put("entities", bodies);
		return root;
	}

	static CabinCorridorSnapshot load(CompoundTag root) {
		var cells = new ArrayList<Cell>();
		for (Tag value : root.getListOrEmpty("cells")) {
			var tag = (CompoundTag) value;
			cells.add(new Cell(position(tag, "source"), position(tag, "destination"),
				BlockState.CODEC.parse(NbtOps.INSTANCE, tag.get("state")).getOrThrow(),
				tag.getCompound("data").map(CompoundTag::copy).orElse(null)));
		}
		var ticks = new ArrayList<CompoundTag>(); for (Tag tag : root.getListOrEmpty("ticks")) ticks.add(((CompoundTag) tag).copy());
		var entities = new ArrayList<CompoundTag>(); for (Tag tag : root.getListOrEmpty("entities")) entities.add(((CompoundTag) tag).copy());
		return new CabinCorridorSnapshot(cells, ticks, entities, root.getLongOr("captured_time", 0));
	}

	static void putPosition(CompoundTag tag, String name, BlockPos pos) {
		tag.putIntArray(name, new int[]{pos.getX(), pos.getY(), pos.getZ()});
	}

	static BlockPos position(CompoundTag tag, String name) {
		int[] pos = tag.getIntArray(name).orElseThrow();
		if (pos.length != 3) throw new IllegalStateException("Invalid saved position");
		return new BlockPos(pos[0], pos[1], pos[2]);
	}

	private static CompoundTag tick(String kind, Identifier type, BlockPos pos, long time, TickPriority priority, long order) {
		var tag = new CompoundTag();
		tag.putString("kind", kind); tag.putString("type", type.toString()); putPosition(tag, "pos", pos);
		tag.putLong("time", time); tag.putInt("priority", priority.ordinal()); tag.putLong("order", order);
		return tag;
	}
}
