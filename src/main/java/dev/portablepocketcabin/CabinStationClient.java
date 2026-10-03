package dev.portablepocketcabin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;

public final class CabinStationClient {
	private static CabinStationPayload current;
	private CabinStationClient() { }
	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(CabinStationPayload.TYPE, (payload, context) -> current = payload);
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> current = null);
	}
	public static List<CabinStationPayload.Entry> entries(int menu) {
		return current != null && current.menuId() == menu ? current.entries() : null;
	}
}
