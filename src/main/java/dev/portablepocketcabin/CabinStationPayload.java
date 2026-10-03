package dev.portablepocketcabin;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public record CabinStationPayload(int menuId, List<Entry> entries) implements CustomPacketPayload {
	public record Entry(String title, ItemStack icon, boolean available, boolean selected) { }
	public static final Type<CabinStationPayload> TYPE = new Type<>(PortablePocketCabin.id("station_recipes"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CabinStationPayload> CODEC = new StreamCodec<>() {
		public CabinStationPayload decode(RegistryFriendlyByteBuf buffer) {
			int id = buffer.readVarInt(), size = buffer.readVarInt();
			if (size < 0 || size > 65536) throw new IllegalArgumentException("Invalid station recipe count");
			List<Entry> entries = new ArrayList<>();
			for (int i = 0; i < size; i++) entries.add(new Entry(buffer.readUtf(512), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readBoolean(), buffer.readBoolean()));
			return new CabinStationPayload(id, List.copyOf(entries));
		}
		public void encode(RegistryFriendlyByteBuf buffer, CabinStationPayload payload) {
			buffer.writeVarInt(payload.menuId()); buffer.writeVarInt(payload.entries().size());
			for (Entry entry : payload.entries()) { buffer.writeUtf(entry.title(), 512); ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.icon()); buffer.writeBoolean(entry.available()); buffer.writeBoolean(entry.selected()); }
		}
	};
	public Type<CabinStationPayload> type() { return TYPE; }
	static void register() { PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC); }
	static void send(ServerPlayer player, int id, List<Entry> choices) {
		ServerPlayNetworking.send(player, new CabinStationPayload(id, choices));
	}
}
