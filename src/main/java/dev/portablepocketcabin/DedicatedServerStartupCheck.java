package dev.portablepocketcabin;

import net.minecraft.server.MinecraftServer;

final class DedicatedServerStartupCheck {
	private static final String PROPERTY = "portable-pocket-cabin.startup-test";

	private DedicatedServerStartupCheck() {
	}

	static void onServerStarted(MinecraftServer server) {
		if (!Boolean.getBoolean(PROPERTY)) {
			return;
		}

		if (server.getLevel(PocketDimension.LEVEL_KEY) == null) {
			PortablePocketCabin.LOGGER.error("Dedicated server startup test failed: pocket dimension is missing");
			return;
		}

		PortablePocketCabin.LOGGER.info("DEDICATED_SERVER_STARTUP_TEST_PASSED");
		server.execute(() -> server.halt(false));
	}
}
