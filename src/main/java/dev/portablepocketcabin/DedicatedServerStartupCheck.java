package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.UUID;

final class DedicatedServerStartupCheck {
	private static final String PROPERTY = "portable-pocket-cabin.startup-test";
	private static final UUID FIRST_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID SECOND_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID TRUSTED_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000099");

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
				scheduleCheck(server, () -> finishSeedCheck(server, registry));
			} else if ("verify".equals(mode)) {
				scheduleCheck(server, () -> {
					verifyReloadedRegistry(server, registry);
					PortablePocketCabin.LOGGER.info("DEDICATED_SERVER_RESTART_TEST_PASSED");
				});
			} else {
				throw new IllegalStateException("Unknown startup test mode: " + mode);
			}
		} catch (Exception exception) {
			PortablePocketCabin.LOGGER.error("Dedicated server startup test failed", exception);
			server.execute(() -> server.halt(false));
		}
	}

	private static void scheduleCheck(MinecraftServer server, Runnable check) {
		server.schedule(new TickTask(server.getTickCount() + 5, () -> {
			try {
				check.run();
			} catch (Exception exception) {
				PortablePocketCabin.LOGGER.error("Dedicated server startup test failed", exception);
			} finally {
				server.halt(false);
			}
		}));
	}

	private static void seedRegistry(MinecraftServer server, CabinRegistry registry) {
		CabinRecord first = registry.create(FIRST_OWNER);
		if (first.cellIndex() != 0 || registry.size() != 1 || registry.nextCellIndex() != 1) {
			throw new IllegalStateException("Fresh registry did not allocate the first cabin at cell zero");
		}
		registry.trust(first.uuid(), FIRST_OWNER, TRUSTED_PLAYER);
		registry.setEntryPermission(first.uuid(), FIRST_OWNER, CabinEntryPermission.TRUSTED_PLAYERS);

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
		placeInteriorFixtures(pocket, first.cellIndex());
		seedPartialUpgradeFund(server, registry, deployed);
		seedLegacyCornerFrames(server, registry.find(first.uuid()).orElseThrow());
		CabinSimulation.sync(server);
	}

	private static void finishSeedCheck(MinecraftServer server, CabinRegistry registry) {
		CabinRecord first = registry.findByOwner(FIRST_OWNER)
			.orElseThrow(() -> new IllegalStateException("Seeded cabin disappeared before ticket verification"));
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (!CabinSimulation.isTicketed(server, first.cellIndex())
			|| !simulationIsActive(pocket, first.cellIndex())) {
			throw new IllegalStateException("Deployed cabin did not receive interior simulation tickets");
		}
		registry.beginPacking(first.uuid(), FIRST_OWNER);
		CabinSimulation.sync(server);
		if (CabinSimulation.isTicketed(server, first.cellIndex())) {
			throw new IllegalStateException("Packing cabin retained interior simulation tickets");
		}

		CabinRecord interruptedDeployment = registry.create(SECOND_OWNER);
		CabinExterior unusedExterior = new CabinExterior(Level.OVERWORLD, new BlockPos(40, 200, 0), Direction.NORTH);
		registry.beginDeployment(interruptedDeployment.uuid(), SECOND_OWNER, unusedExterior);
		PortablePocketCabin.LOGGER.info("DEDICATED_SERVER_STARTUP_TEST_PASSED");
	}

	private static void verifyReloadedRegistry(MinecraftServer server, CabinRegistry registry) {
		CabinRecord first = registry.findByOwner(FIRST_OWNER)
			.orElseThrow(() -> new IllegalStateException("Seeded cabin was not persisted across restart"));
		if (first.cellIndex() != 0 || registry.size() != 2 || registry.nextCellIndex() != 2
			|| first.lifecycle() != CabinLifecycle.DEPLOYED || !first.interiorGenerated()
			|| first.exterior().isEmpty() || !first.canEnter(TRUSTED_PLAYER)
			|| first.entryPermission() != CabinEntryPermission.TRUSTED_PLAYERS) {
			throw new IllegalStateException("Reloaded registry does not match its persisted state");
		}
		assertPartialUpgradeFund(first);
		if (!CabinSimulation.isTicketed(server, first.cellIndex())
			|| !simulationIsActive(server.getLevel(PocketDimension.LEVEL_KEY), first.cellIndex())) {
			throw new IllegalStateException("Reconciled deployed cabin did not restore simulation tickets");
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
		assertCornerFramesMigrated(server, pocket, first);
		assertInteriorFixtures(pocket, first.cellIndex());

		registry.beginPacking(first.uuid(), FIRST_OWNER);
		CabinSimulation.sync(server);
		CabinRecord packed = registry.finishPacking(first.uuid());
		CabinSimulation.sync(server);
		ExteriorCabin.removeProjection(server.overworld(), exterior);
		registry.markExteriorCleanupComplete(first.uuid());
		if (packed.lifecycle() != CabinLifecycle.PACKED || packed.packedItemGeneration() != 1
			|| !pocket.getBlockState(PocketDimension.interiorExitDoorLower(first.cellIndex())).is(Blocks.IRON_DOOR)) {
			throw new IllegalStateException("Packing changed the persistent interior or item generation incorrectly");
		}
		assertPartialUpgradeFund(registry.find(first.uuid()).orElseThrow());
		if (CabinSimulation.isTicketed(server, first.cellIndex())) {
			throw new IllegalStateException("Packed cabin retained interior simulation tickets");
		}
		assertInteriorFixtures(pocket, first.cellIndex());

		CabinExterior redeployedExterior = new CabinExterior(
			Level.OVERWORLD, new BlockPos(20, 200, 0), Direction.EAST
		);
		registry.beginDeployment(first.uuid(), FIRST_OWNER, redeployedExterior);
		ExteriorCabin.place(server.overworld(), redeployedExterior);
		CabinRecord redeployed = registry.finishDeployment(first.uuid());
		CabinSimulation.sync(server);
		if (redeployed.lifecycle() != CabinLifecycle.DEPLOYED
			|| !ExteriorCabin.projectionValid(server.overworld(), redeployedExterior)
			|| !CabinSimulation.isTicketed(server, first.cellIndex())) {
			throw new IllegalStateException("Packed cabin could not be redeployed");
		}
		assertPartialUpgradeFund(redeployed);
		assertInteriorFixtures(pocket, first.cellIndex());

		CabinRecord third = registry.create(UUID.fromString("00000000-0000-0000-0000-000000000003"));
		if (third.cellIndex() != 2 || first.uuid().equals(third.uuid()) || registry.size() != 3) {
			throw new IllegalStateException("Post-restart allocation collided with the persisted cabin");
		}
	}

	private static void placeInteriorFixtures(ServerLevel pocket, long cellIndex) {
		BlockPos center = PocketDimension.cellCenter(cellIndex);
		BlockPos chestPos = center.offset(-1, 1, -1);
		BlockPos furnacePos = center.offset(0, 1, -1);
		BlockPos waterPos = center.offset(1, 1, -1);
		BlockPos farmlandPos = center.offset(2, 1, -1);
		BlockPos cropPos = farmlandPos.above();
		BlockPos doubleChestLeft = center.offset(-1, 1, 0);
		BlockPos doubleChestRight = center.offset(0, 1, 0);
		BlockPos bedFoot = center.offset(1, 1, 1);
		BlockPos bedHead = bedFoot.relative(Direction.SOUTH);
		pocket.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
		pocket.setBlockAndUpdate(doubleChestLeft, Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.FACING, Direction.NORTH)
			.setValue(ChestBlock.TYPE, ChestType.LEFT));
		pocket.setBlockAndUpdate(doubleChestRight, Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.FACING, Direction.NORTH)
			.setValue(ChestBlock.TYPE, ChestType.RIGHT));
		pocket.setBlockAndUpdate(furnacePos, Blocks.FURNACE.defaultBlockState());
		pocket.setBlockAndUpdate(waterPos, Blocks.WATER.defaultBlockState());
		pocket.setBlockAndUpdate(farmlandPos, Blocks.FARMLAND.defaultBlockState()
			.setValue(BlockStateProperties.MOISTURE, 7));
		pocket.setBlockAndUpdate(cropPos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));
		pocket.setBlockAndUpdate(bedFoot, Blocks.BED.red().defaultBlockState()
			.setValue(BedBlock.FACING, Direction.SOUTH)
			.setValue(BedBlock.PART, BedPart.FOOT));
		pocket.setBlockAndUpdate(bedHead, Blocks.BED.red().defaultBlockState()
			.setValue(BedBlock.FACING, Direction.SOUTH)
			.setValue(BedBlock.PART, BedPart.HEAD));

		ChestBlockEntity chest = (ChestBlockEntity) pocket.getBlockEntity(chestPos);
		ChestBlockEntity doubleChest = (ChestBlockEntity) pocket.getBlockEntity(doubleChestLeft);
		FurnaceBlockEntity furnace = (FurnaceBlockEntity) pocket.getBlockEntity(furnacePos);
		if (chest == null || doubleChest == null || furnace == null) {
			throw new IllegalStateException("Vanilla interior fixture block entities were not created");
		}
		ItemStack namedDiamonds = new ItemStack(Items.DIAMOND, 3);
		namedDiamonds.set(DataComponents.CUSTOM_NAME, Component.literal("Travelling reserve"));
		chest.setItem(0, namedDiamonds);
		doubleChest.setItem(0, new ItemStack(Items.EMERALD, 5));
		furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 2));
	}

	private static void seedPartialUpgradeFund(
		MinecraftServer server, CabinRegistry registry, CabinRecord cabin
	) {
		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		WorldAttunement attunement = registry.resolveWorldAttunement(
			definitions.resolve(server.overworld().getSeed())
		);
		CabinUpgradeCatalog.Offer offer = CabinUpgradeCatalog.next(cabin, attunement, definitions).orElseThrow();
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			throw new IllegalStateException("Pocket dimension disappeared during upgrade test");
		}
		CabinUpgradeService.UpgradeEffect effect = new CabinUpgradeEffect(server, pocket);
		for (CabinUpgradeState.Requirement requirement : offer.requirements()) {
			var item = BuiltInRegistries.ITEM.getOptional(requirement.itemId()).orElseThrow();
			ItemStack payment = new ItemStack(item, requirement.count());
			CabinUpgradeService.Contribution contribution = CabinUpgradeService.deposit(
				registry, cabin.uuid(), cabin.owner(), offer.target(), payment, attunement, definitions, effect
			);
			if (!contribution.success() || !payment.isEmpty()) {
				throw new IllegalStateException("Could not fully fund the startup installation");
			}
		}
		long revision = registry.find(cabin.uuid()).orElseThrow().upgrades().fundRevision();
		CabinUpgradeService.Outcome installed = CabinUpgradeService.install(
			registry, cabin.uuid(), cabin.owner(), offer.target(), revision, attunement, definitions,
			effect, () -> CabinRegistry.flush(server)
		);
		if (!installed.success()) {
			throw new IllegalStateException(installed.message());
		}

		CabinRecord expanded = registry.find(cabin.uuid()).orElseThrow();
		CabinUpgradeCatalog.Offer next = CabinUpgradeCatalog.next(expanded, attunement, definitions).orElseThrow();
		CabinUpgradeState.Requirement requirement = next.requirements().getFirst();
		var item = BuiltInRegistries.ITEM.getOptional(requirement.itemId()).orElseThrow();
		ItemStack contribution = new ItemStack(item, Math.min(3, requirement.count()));
		contribution.set(DataComponents.CUSTOM_NAME, Component.literal("Persisted upgrade contribution"));
		CabinUpgradeService.Contribution result = CabinUpgradeService.deposit(
			registry, cabin.uuid(), cabin.owner(), next.target(), contribution, attunement, definitions, effect
		);
		if (!result.success()) {
			throw new IllegalStateException(result.message());
		}
	}

	private static void seedLegacyCornerFrames(MinecraftServer server, CabinRecord cabin) {
		CabinExterior exterior = cabin.exterior().orElseThrow();
		ServerLevel exteriorLevel = server.getLevel(exterior.dimension());
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (exteriorLevel == null || pocket == null) {
			throw new IllegalStateException("Cabin levels disappeared while seeding legacy corner frames");
		}
		for (var entry : ExteriorCabin.legacyBlocks(exterior, cabin.palette()).entrySet()) {
			exteriorLevel.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		for (var entry : PocketDimension.legacyShellBlocks(
			cabin.cellIndex(), cabin.progression().generalSize(), cabin.palette()
		).entrySet()) {
			pocket.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		CabinWindows.refresh(server, cabin);
	}

	private static void assertCornerFramesMigrated(
		MinecraftServer server, ServerLevel pocket, CabinRecord cabin
	) {
		CabinExterior exterior = cabin.exterior().orElseThrow();
		ServerLevel exteriorLevel = server.getLevel(exterior.dimension());
		BlockPos exteriorFrame = ExteriorCabin.local(exterior, -1, 0, 1);
		PocketDimension.InteriorBounds bounds = PocketDimension.bounds(
			cabin.progression().generalSize()
		);
		BlockPos interiorFrame = PocketDimension.cellCenter(cabin.cellIndex()).offset(
			bounds.shellMinimumX() + 1, 1, bounds.shellMinimumZ()
		);
		if (exteriorLevel == null
			|| !exteriorLevel.getBlockState(exteriorFrame).is(cabin.palette().walls().structuralWoodBlock())
			|| !pocket.getBlockState(interiorFrame).is(cabin.palette().walls().structuralWoodBlock())) {
			throw new IllegalStateException("Legacy corner frames were not migrated across restart");
		}
	}

	private static void assertPartialUpgradeFund(CabinRecord cabin) {
		CabinUpgradeState.Fund fund = cabin.upgrades().fund(CabinUpgradeState.Target.generalSpace(6))
			.orElseThrow(() -> new IllegalStateException("Partially funded upgrade did not persist"));
		if (cabin.progression().generalSize() != 5
			|| fund.stacks().size() != 1
			|| fund.stacks().getFirst().get(DataComponents.CUSTOM_NAME) == null
			|| !fund.stacks().getFirst().get(DataComponents.CUSTOM_NAME).getString()
				.equals("Persisted upgrade contribution")) {
			throw new IllegalStateException("Persisted upgrade contribution changed across lifecycle or restart");
		}
	}

	private static void assertInteriorFixtures(ServerLevel pocket, long cellIndex) {
		BlockPos center = PocketDimension.cellCenter(cellIndex);
		ChestBlockEntity chest = (ChestBlockEntity) pocket.getBlockEntity(center.offset(-1, 1, -1));
		ChestBlockEntity doubleChest = (ChestBlockEntity) pocket.getBlockEntity(center.offset(-1, 1, 0));
		FurnaceBlockEntity furnace = (FurnaceBlockEntity) pocket.getBlockEntity(center.offset(0, 1, -1));
		if (chest == null || !chest.getItem(0).is(Items.DIAMOND) || chest.getItem(0).getCount() != 3
			|| chest.getItem(0).get(DataComponents.CUSTOM_NAME) == null
			|| doubleChest == null || !doubleChest.getItem(0).is(Items.EMERALD)
			|| doubleChest.getItem(0).getCount() != 5
			|| furnace == null || !furnace.getItem(2).is(Items.IRON_INGOT)
			|| furnace.getItem(2).getCount() != 2
			|| !pocket.getBlockState(center.offset(1, 1, -1)).is(Blocks.WATER)
			|| !pocket.getBlockState(center.offset(2, 1, -1)).is(Blocks.FARMLAND)
			|| !pocket.getBlockState(center.offset(2, 2, -1)).is(Blocks.WHEAT)
			|| pocket.getBlockState(center.offset(2, 2, -1)).getValue(CropBlock.AGE) != 3
			|| !pocket.getBlockState(center.offset(1, 1, 1)).is(Blocks.BED.red())
			|| !pocket.getBlockState(center.offset(1, 1, 2)).is(Blocks.BED.red())) {
			throw new IllegalStateException("Vanilla interior fixtures changed across packing or restart");
		}
	}

	private static boolean simulationIsActive(ServerLevel pocket, long cellIndex) {
		if (pocket == null) {
			return false;
		}
		for (var chunk : CabinSimulation.chunksForCell(cellIndex)) {
			if (!pocket.shouldTickBlocksAt(chunk.pack())) {
				return false;
			}
		}
		return true;
	}
}
