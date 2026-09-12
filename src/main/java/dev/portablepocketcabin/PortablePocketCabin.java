package dev.portablepocketcabin;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PortablePocketCabin implements ModInitializer {
	public static final String MOD_ID = "portable_pocket_cabin";
	public static final String VERSION = "0.1.0-alpha.1";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(CabinCommands::register);
		CabinEvents.register();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			CabinRegistry registry = CabinRegistry.get(server);
			LOGGER.info("Portable Pocket Cabin {} ready; pocket dimension loaded={}", VERSION,
				server.getLevel(PocketDimension.LEVEL_KEY) != null);
			LOGGER.info("Cabin registry loaded; cabins={}, next cell={}", registry.size(), registry.nextCellIndex());
			DedicatedServerStartupCheck.onServerStarted(server);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
