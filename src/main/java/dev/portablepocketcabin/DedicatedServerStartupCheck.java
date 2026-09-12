package dev.portablepocketcabin;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

final class DedicatedServerStartupCheck {
	private static final String PROPERTY = "portable-pocket-cabin.startup-test";
	private static final UUID FIRST_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID SECOND_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private DedicatedServerStartupCheck() {
	}

	static void onServerStarted(MinecraftServer server) {
		String mode = System.getProperty(PROPERTY, "");
		if (mode.isEmpty()) {
			return;
		}

		try {
			if (server.getLevel(PocketDimension.LEVEL_KEY) == null) {
				throw new IllegalStateException("Pocket dimension is missing");
			}

			CabinRegistry registry = CabinRegistry.get(server);
			if ("seed".equals(mode)) {
				seedRegistry(registry);
				PortablePocketCabin.LOGGER.info("DEDICATED_SERVER_STARTUP_TEST_PASSED");
			} else if ("verify".equals(mode)) {
				verifyReloadedRegistry(registry);
				PortablePocketCabin.LOGGER.info("DEDICATED_SERVER_RESTART_TEST_PASSED");
			} else {
				throw new IllegalStateException("Unknown startup test mode: " + mode);
			}
		} catch (Exception exception) {
			PortablePocketCabin.LOGGER.error("Dedicated server startup test failed", exception);
		} finally {
			server.execute(() -> server.halt(false));
		}
	}

	private static void seedRegistry(CabinRegistry registry) {
		CabinRecord first = registry.create(FIRST_OWNER);
		if (first.cellIndex() != 0 || registry.size() != 1 || registry.nextCellIndex() != 1) {
			throw new IllegalStateException("Fresh registry did not allocate the first cabin at cell zero");
		}
	}

	private static void verifyReloadedRegistry(CabinRegistry registry) {
		CabinRecord first = registry.findByOwner(FIRST_OWNER)
			.orElseThrow(() -> new IllegalStateException("Seeded cabin was not persisted across restart"));
		if (first.cellIndex() != 0 || registry.size() != 1 || registry.nextCellIndex() != 1) {
			throw new IllegalStateException("Reloaded registry does not match its persisted state");
		}

		CabinRecord second = registry.create(SECOND_OWNER);
		if (second.cellIndex() != 1 || first.uuid().equals(second.uuid()) || registry.size() != 2) {
			throw new IllegalStateException("Post-restart allocation collided with the persisted cabin");
		}
	}
}
