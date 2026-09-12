package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

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
				seedRegistry(server, registry);
				PortablePocketCabin.LOGGER.info("DEDICATED_SERVER_STARTUP_TEST_PASSED");
			} else if ("verify".equals(mode)) {
				verifyReloadedRegistry(server, registry);
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

	private static void seedRegistry(MinecraftServer server, CabinRegistry registry) {
		CabinRecord first = registry.create(FIRST_OWNER);
		if (first.cellIndex() != 0 || registry.size() != 1 || registry.nextCellIndex() != 1) {
			throw new IllegalStateException("Fresh registry did not allocate the first cabin at cell zero");
		}

		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(0, 200, 0), Direction.NORTH);
		registry.beginDeployment(first.uuid(), FIRST_OWNER, exterior);
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			throw new IllegalStateException("Pocket dimension disappeared during deployment test");
		}
		PocketDimension.ensureCabinInterior(pocket, first.cellIndex());
		registry.markInteriorGenerated(first.uuid());
		ExteriorCabin.place(server.overworld(), exterior);
		CabinRecord deployed = registry.finishDeployment(first.uuid());
		if (deployed.lifecycle() != CabinLifecycle.DEPLOYED || !deployed.interiorGenerated()) {
			throw new IllegalStateException("Startup test cabin did not complete deployment");
		}
		registry.beginPacking(first.uuid(), FIRST_OWNER);

		CabinRecord interruptedDeployment = registry.create(SECOND_OWNER);
		CabinExterior unusedExterior = new CabinExterior(Level.OVERWORLD, new BlockPos(40, 200, 0), Direction.NORTH);
		registry.beginDeployment(interruptedDeployment.uuid(), SECOND_OWNER, unusedExterior);
	}

	private static void verifyReloadedRegistry(MinecraftServer server, CabinRegistry registry) {
		CabinRecord first = registry.findByOwner(FIRST_OWNER)
			.orElseThrow(() -> new IllegalStateException("Seeded cabin was not persisted across restart"));
		if (first.cellIndex() != 0 || registry.size() != 2 || registry.nextCellIndex() != 2
			|| first.lifecycle() != CabinLifecycle.DEPLOYED || !first.interiorGenerated()
			|| first.exterior().isEmpty()) {
			throw new IllegalStateException("Reloaded registry does not match its persisted state");
		}
		CabinRecord rolledBack = registry.findByOwner(SECOND_OWNER)
			.orElseThrow(() -> new IllegalStateException("Interrupted deployment record disappeared"));
		if (rolledBack.lifecycle() != CabinLifecycle.PACKED || rolledBack.cellIndex() != 1) {
			throw new IllegalStateException("Interrupted deployment was not rolled back to PACKED");
		}
		CabinExterior exterior = first.exterior().get();
		if (!server.overworld().getBlockState(ExteriorCabin.controller(exterior)).is(Blocks.LODESTONE)) {
			throw new IllegalStateException("Deployed exterior controller did not persist across restart");
		}
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null
			|| !pocket.getBlockState(PocketDimension.interiorExitDoorLower(first.cellIndex())).is(Blocks.IRON_DOOR)) {
			throw new IllegalStateException("Generated cabin interior did not persist across restart");
		}

		registry.beginPacking(first.uuid(), FIRST_OWNER);
		CabinRecord packed = registry.finishPacking(first.uuid());
		ExteriorCabin.removeProjection(server.overworld(), exterior);
		registry.markExteriorCleanupComplete(first.uuid());
		if (packed.lifecycle() != CabinLifecycle.PACKED || packed.packedItemGeneration() != 1
			|| !pocket.getBlockState(PocketDimension.interiorExitDoorLower(first.cellIndex())).is(Blocks.IRON_DOOR)) {
			throw new IllegalStateException("Packing changed the persistent interior or item generation incorrectly");
		}

		CabinExterior redeployedExterior = new CabinExterior(
			Level.OVERWORLD, new BlockPos(20, 200, 0), Direction.EAST
		);
		registry.beginDeployment(first.uuid(), FIRST_OWNER, redeployedExterior);
		ExteriorCabin.place(server.overworld(), redeployedExterior);
		CabinRecord redeployed = registry.finishDeployment(first.uuid());
		if (redeployed.lifecycle() != CabinLifecycle.DEPLOYED
			|| !ExteriorCabin.projectionValid(server.overworld(), redeployedExterior)) {
			throw new IllegalStateException("Packed cabin could not be redeployed");
		}

		CabinRecord third = registry.create(UUID.fromString("00000000-0000-0000-0000-000000000003"));
		if (third.cellIndex() != 2 || first.uuid().equals(third.uuid()) || registry.size() != 3) {
			throw new IllegalStateException("Post-restart allocation collided with the persisted cabin");
		}
	}
}
