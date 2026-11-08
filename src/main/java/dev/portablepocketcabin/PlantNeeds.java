package dev.portablepocketcabin;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What a cooking plant needs to grow, loaded from {@code data/<namespace>/cooking_plant/<plant>.json} so datapacks can
 * change it. A plant with a {@code spread_chance} spreads to suitable spots beside it once grown. A plant whose needs are unmet survives but does not grow, as vanilla crops in the dark do.
 */
record PlantNeeds(List<String> soil, int light, Water water, Support support, float growthChance, boolean shade, float spreadChance) {
	enum Water implements StringRepresentable {
		/** No water needed. */
		NONE,
		/** Water within four blocks, as for farmland. */
		NEARBY,
		/** The plant stands in a water source, as paddy rice does. */
		STANDING,
		/** Flowing water beside the plant, as for wasabi. */
		RUNNING;

		static final Codec<Water> CODEC = StringRepresentable.fromEnum(Water::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	enum Support implements StringRepresentable {
		NONE, TRELLIS, LOG;

		static final Codec<Support> CODEC = StringRepresentable.fromEnum(Support::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	static final PlantNeeds DEFAULT = new PlantNeeds(List.of("minecraft:farmland"), 9, Water.NONE, Support.NONE, 0.25F, false, 0F);
	static final Codec<PlantNeeds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.listOf().optionalFieldOf("soil", DEFAULT.soil()).forGetter(PlantNeeds::soil),
		Codec.intRange(0, 15).optionalFieldOf("light", DEFAULT.light()).forGetter(PlantNeeds::light),
		Water.CODEC.optionalFieldOf("water", DEFAULT.water()).forGetter(PlantNeeds::water),
		Support.CODEC.optionalFieldOf("support", DEFAULT.support()).forGetter(PlantNeeds::support),
		Codec.floatRange(0, 1).optionalFieldOf("growth_chance", DEFAULT.growthChance()).forGetter(PlantNeeds::growthChance),
		Codec.BOOL.optionalFieldOf("shade", false).forGetter(PlantNeeds::shade),
		Codec.floatRange(0, 1).optionalFieldOf("spread_chance", 0F).forGetter(PlantNeeds::spreadChance)
	).apply(instance, PlantNeeds::new));

	private static Map<Identifier, PlantNeeds> current = Map.of();
	/** The growing needs the server last sent, for the cookbook's Garden pages. */
	static Map<Identifier, PlantNeeds> client = Map.of();

	/** Sends every plant's growing needs to a client, on joining and after each datapack reload. */
	record Sync(Map<Identifier, PlantNeeds> needs) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		static final Type<Sync> TYPE = new Type<>(PortablePocketCabin.id("plant_needs"));
		static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Sync> CODEC =
			net.minecraft.network.codec.ByteBufCodecs.<net.minecraft.network.RegistryFriendlyByteBuf, Identifier, PlantNeeds, Map<Identifier, PlantNeeds>>map(
				HashMap::new, Identifier.STREAM_CODEC, net.minecraft.network.codec.ByteBufCodecs.fromCodec(PlantNeeds.CODEC)).map(Sync::new, Sync::needs);

		@Override
		public Type<Sync> type() {
			return TYPE;
		}
	}

	static void register() {
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(Sync.TYPE, Sync.CODEC);
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.getPlayer(), new Sync(current)));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new Sync(current)));
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(PortablePocketCabin.id("cooking_plants"),
			new SimpleReloadListener<Map<Identifier, PlantNeeds>>() {
				@Override
				protected Map<Identifier, PlantNeeds> prepare(PreparableReloadListener.SharedState state) {
					Map<Identifier, PlantNeeds> loaded = new HashMap<>();
					for (var entry : state.resourceManager().listResources("cooking_plant", id -> id.getPath().endsWith(".json")).entrySet()) {
						String path = entry.getKey().getPath();
						Identifier plant = Identifier.fromNamespaceAndPath(entry.getKey().getNamespace(),
							path.substring("cooking_plant/".length(), path.length() - ".json".length()));
						try (var reader = entry.getValue().openAsReader()) {
							loaded.put(plant, CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow());
						} catch (Exception exception) {
							PortablePocketCabin.LOGGER.error("Could not read growing needs {}", entry.getKey(), exception);
						}
					}
					return loaded;
				}

				@Override
				protected void apply(Map<Identifier, PlantNeeds> prepared, PreparableReloadListener.SharedState state) {
					current = Map.copyOf(prepared);
				}
			});
	}

	/** The growing needs for a plant block, or the defaults when no data names it. */
	static PlantNeeds of(net.minecraft.world.level.block.Block block) {
		return current.getOrDefault(BuiltInRegistries.BLOCK.getKey(block), DEFAULT);
	}

	/** Replaces a plant's needs until the next reload. For GameTests. */
	static void override(net.minecraft.world.level.block.Block block, PlantNeeds needs) {
		Map<Identifier, PlantNeeds> changed = new HashMap<>(current);
		changed.put(BuiltInRegistries.BLOCK.getKey(block), needs);
		current = Map.copyOf(changed);
	}

	/** Whether the block a plant stands on is one of its soils. */
	boolean soilMatches(BlockState below) {
		for (String entry : soil) {
			if (entry.startsWith("#")) {
				if (below.is(TagKey.create(Registries.BLOCK, Identifier.parse(entry.substring(1))))) return true;
			} else if (BuiltInRegistries.BLOCK.getKey(below.getBlock()).equals(Identifier.parse(entry))) {
				return true;
			}
		}
		return false;
	}

	/** Whether the plant at {@code pos} can grow now: soil, light, water, and shade. Support is checked by the block. */
	boolean canGrow(LevelReader level, BlockPos pos) {
		if (!soilMatches(level.getBlockState(pos.below()))) return false;
		if (level.getRawBrightness(pos, 0) < light) return false;
		if (shade && !roofed(level, pos)) return false;
		return switch (water) {
			case NONE -> true;
			case NEARBY -> waterNearby(level, pos.below());
			case STANDING -> level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource();
			case RUNNING -> runningWaterBeside(level, pos);
		};
	}

	/** Whether a block stands somewhere above the plant, shading it from the open sky. */
	static boolean roofed(LevelReader level, BlockPos pos) {
		// Only blocks that stop skylight shade a plant: glass lets the light through, as a greenhouse roof should.
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		BlockPos.MutableBlockPos above = pos.mutable();
		for (int y = pos.getY() + 1; y < top; y++) {
			if (!level.getBlockState(above.setY(y)).propagatesSkylightDown()) return true;
		}
		return false;
	}

	private static boolean waterNearby(LevelReader level, BlockPos pos) {
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
			if (level.getFluidState(near).is(FluidTags.WATER)) return true;
		}
		return false;
	}

	private static boolean runningWaterBeside(LevelReader level, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			var fluid = level.getFluidState(pos.relative(direction));
			if (fluid.is(FluidTags.WATER) && !fluid.isSource()) return true;
		}
		return false;
	}
}
