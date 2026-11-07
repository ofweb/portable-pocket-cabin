package dev.portablepocketcabin;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * How one animal or fish is dressed and broken down, loaded from {@code data/<namespace>/carcass/<animal>.json} so
 * datapacks can change it. The order of {@code parts} is the order the butcher block takes them.
 */
record CarcassData(Item carcass, Dressing dressing, boolean scald, List<ItemStackTemplate> dressingGives, Optional<Item> dressed,
	List<ItemStackTemplate> parts) {
	/** How the hanging hook dresses a carcass. Fish need no dressing: they go straight to the butcher block. */
	enum Dressing implements StringRepresentable {
		/** The knife takes the hide or fleece off. */
		SKIN,
		/** An empty hand pulls the feathers out of a scalded bird. */
		PLUCK,
		/** The knife scrapes the bristles off a scalded pig, which keeps its skin. */
		SCRAPE,
		NONE;

		static final Codec<Dressing> CODEC = StringRepresentable.fromEnum(Dressing::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	static final Codec<CarcassData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		BuiltInRegistries.ITEM.byNameCodec().fieldOf("carcass").forGetter(CarcassData::carcass),
		Dressing.CODEC.optionalFieldOf("dressing", Dressing.NONE).forGetter(CarcassData::dressing),
		Codec.BOOL.optionalFieldOf("scald", false).forGetter(CarcassData::scald),
		ItemStackTemplate.CODEC.listOf().optionalFieldOf("dressing_gives", List.of()).forGetter(CarcassData::dressingGives),
		BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("dressed").forGetter(CarcassData::dressed),
		ItemStackTemplate.CODEC.listOf().optionalFieldOf("parts", List.of()).forGetter(CarcassData::parts)
	).apply(instance, CarcassData::new));

	private static Map<Identifier, CarcassData> current = Map.of();
	/** The carcass data the server last sent, for the cookbook's Butchery pages. */
	static Map<Identifier, CarcassData> client = Map.of();

	/** Sends every carcass's data to a client, on joining and after each datapack reload. */
	record Sync(Map<Identifier, CarcassData> carcasses) implements CustomPacketPayload {
		static final Type<Sync> TYPE = new Type<>(PortablePocketCabin.id("carcass_data"));
		static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = ByteBufCodecs.<RegistryFriendlyByteBuf, Identifier, CarcassData,
			Map<Identifier, CarcassData>>map(LinkedHashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.fromCodecWithRegistries(CarcassData.CODEC))
			.map(Sync::new, Sync::carcasses);

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
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(PortablePocketCabin.id("carcasses"),
			new SimpleReloadListener<Map<Identifier, CarcassData>>() {
				@Override
				protected Map<Identifier, CarcassData> prepare(PreparableReloadListener.SharedState state) {
					Map<Identifier, CarcassData> loaded = new HashMap<>();
					for (var entry : state.resourceManager().listResources("carcass", id -> id.getPath().endsWith(".json")).entrySet()) {
						String path = entry.getKey().getPath();
						Identifier animal = Identifier.fromNamespaceAndPath(entry.getKey().getNamespace(),
							path.substring("carcass/".length(), path.length() - ".json".length()));
						try (var reader = entry.getValue().openAsReader()) {
							loaded.put(animal, CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow());
						} catch (Exception exception) {
							PortablePocketCabin.LOGGER.error("Could not read carcass data {}", entry.getKey(), exception);
						}
					}
					return loaded;
				}

				@Override
				protected void apply(Map<Identifier, CarcassData> prepared, PreparableReloadListener.SharedState state) {
					current = Map.copyOf(prepared);
				}
			});
	}

	/** Every loaded carcass, on the server. */
	static Map<Identifier, CarcassData> all() {
		return current;
	}

	/** The data for an undressed carcass, or a fish, by its item. */
	static Optional<CarcassData> forCarcass(ItemStack stack) {
		return current.values().stream().filter(data -> stack.is(data.carcass())).findFirst();
	}

	/** The data for a carcass ready for the butcher block: a dressed carcass, or a fish, which needs no dressing. */
	static Optional<CarcassData> forBlock(ItemStack stack) {
		return current.values().stream()
			.filter(data -> data.dressed().map(stack::is).orElse(data.dressing() == Dressing.NONE && stack.is(data.carcass())))
			.findFirst();
	}

	/** Replaces a carcass's data until the next reload. For GameTests. */
	static void override(Identifier animal, CarcassData data) {
		Map<Identifier, CarcassData> changed = new HashMap<>(current);
		changed.put(animal, data);
		current = Map.copyOf(changed);
	}
}
