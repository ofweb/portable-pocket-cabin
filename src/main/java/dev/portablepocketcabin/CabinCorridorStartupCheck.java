package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.*;

/** Runs only in disposable Gradle test worlds; exercises recovery across separate JVMs. */
final class CabinCorridorStartupCheck {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000501");
	private static final UUID COMMITTED_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000502");
	private static final UUID ANIMAL = UUID.fromString("00000000-0000-0000-0000-000000000503");
	private CabinCorridorStartupCheck() { }

	static void onServerStarted(MinecraftServer server) {
		String mode = System.getProperty("portable-pocket-cabin.corridor-test", "");
		if (mode.isEmpty()) return;
		server.schedule(new TickTask(server.getTickCount() + 10, () -> {
			try {
				if (mode.equals("seed")) seed(server);
				else if (mode.equals("verify")) verify(server);
				else throw new IllegalStateException("Unknown corridor test mode");
			} catch (Exception exception) {
				PortablePocketCabin.LOGGER.error("Corridor integration test failed", exception);
			} finally { server.halt(false); }
		}));
	}

	private static CabinRecord deployed(MinecraftServer server, UUID owner) {
		CabinRegistry registry = CabinRegistry.get(server);
		CabinRecord cabin = registry.create(owner);
		var exterior = new CabinExterior(Level.OVERWORLD, new BlockPos((int) cabin.cellIndex() * 64, 200, 0), Direction.NORTH);
		registry.beginDeployment(cabin.uuid(), owner, exterior);
		ExteriorCabin.place(server.overworld(), exterior);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		PocketDimension.ensureCabinInterior(pocket, cabin.cellIndex(), cabin.palette(), 3);
		registry.markInteriorGenerated(cabin.uuid()); registry.finishDeployment(cabin.uuid());
		require(!CabinCorridors.installForRoom(pocket, cabin.uuid(), owner, CabinCorridor.NORTH).success(), "3x3 interiors cannot install passages");
		PocketDimension.applyGeneralSpaceExpansion(pocket, registry.find(cabin.uuid()).orElseThrow(), 5);
		registry.expandGeneralSpace(cabin.uuid(), owner, 3, 5, 21);
		return registry.find(cabin.uuid()).orElseThrow();
	}

	private static void seed(MinecraftServer server) {
		CabinRegistry registry = CabinRegistry.get(server);
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		CabinRecord cabin = deployed(server, OWNER);
		BlockPos center = PocketDimension.cellCenter(cabin.cellIndex());
		require(!CabinCorridors.installForRoom(pocket, cabin.uuid(), UUID.randomUUID(), CabinCorridor.WEST).success(), "Guests cannot install corridors");
		BlockPos obstruction = center.offset(8, 1, 0);
		pocket.setBlockAndUpdate(obstruction, Blocks.DIAMOND_BLOCK.defaultBlockState());
		require(!CabinCorridors.installForRoom(pocket, cabin.uuid(), OWNER, CabinCorridor.EAST).success(), "Full-corridor validation must detect far obstructions");
		require(registry.find(cabin.uuid()).orElseThrow().equals(cabin) && pocket.getBlockState(obstruction).is(Blocks.DIAMOND_BLOCK), "Failed installation changes nothing");
		pocket.setBlockAndUpdate(obstruction, Blocks.AIR.defaultBlockState());
		for (CabinCorridor corridor : CabinCorridor.values()) require(CabinCorridors.installForRoom(pocket, cabin.uuid(), OWNER, corridor).success(), "Each complete corridor installs");
		cabin = registry.find(cabin.uuid()).orElseThrow();
		require(cabin.progression().rooms().isEmpty() && cabin.upgrades().funds().isEmpty(), "Free corridors add no room or upgrade fund");
		chest(pocket, center.offset(-38, 1, -25), Items.DIAMOND, 11);
		chest(pocket, center.offset(0, 1, -8), Items.EMERALD, 17);
		chest(pocket, center.offset(6, 1, 0), Items.GOLD_INGOT, 7);
		require(CabinCorridors.installForRoom(pocket, cabin.uuid(), OWNER, CabinCorridor.WEST).success(), "Later room installations reuse the complete corridor");
		assertChest(pocket, center.offset(-38, 1, -25), Items.DIAMOND, 11);
		require(CabinProtection.isProtected(pocket, center.offset(-38, 0, -25)), "Corridor floor is protected");
		require(!CabinProtection.isProtected(pocket, center.offset(-38, 1, -25)), "Owners can clear player furniture");
		UUID resident = UUID.randomUUID(), guest = UUID.randomUUID();
		BlockPos furniture = center.offset(-38, 1, -25);
		registry.trust(cabin.uuid(), OWNER, resident);
		registry.setEntryPermission(cabin.uuid(), OWNER, CabinEntryPermission.TRUSTED_PLAYERS);
		require(CabinCorridors.mayChange(pocket, OWNER, furniture), "Owners can change corridor contents");
		require(CabinCorridors.mayChange(pocket, resident, furniture), "Residents can change corridor contents");
		require(!CabinCorridors.mayChange(pocket, guest, furniture), "Guests cannot change corridor state");
		registry.untrust(cabin.uuid(), OWNER, resident);
		require(!CabinCorridors.mayChange(pocket, resident, furniture), "Revoked roles take effect immediately");
		cabin = registry.find(cabin.uuid()).orElseThrow();
		var restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow()).getOrThrow();
		require(restored.find(cabin.uuid()).orElseThrow().progression().corridors().size() == 3, "All corridors persist");
		var space = new CabinRoomSpace(CabinCorridor.NORTH, new BlockPos(-8, 0, -10), new BlockPos(-2, 4, -4), new BlockPos(-2, 1, -7));
		for (BlockPos pos : space.volume(cabin.cellIndex())) {
			pocket.setBlock(pos, space.shell(cabin.cellIndex(), pos) ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		}
		registry.registerConnectedRoom(cabin.uuid(), OWNER, PortablePocketCabin.id("test_room"), space);
		chest(pocket, center.offset(-6, 1, -8), Items.IRON_INGOT, 13);
		BlockPos bed = center.offset(-5, 1, -5);
		pocket.setBlock(bed.west(), Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT), CabinCorridorSnapshot.FLAGS);
		pocket.setBlock(bed, Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.HEAD), CabinCorridorSnapshot.FLAGS);
		CabinRespawnData.get(server).bind(OWNER, cabin.uuid(), bed);
		var cow = EntityTypes.COW.create(pocket, EntitySpawnReason.COMMAND);
		cow.setUUID(ANIMAL); cow.setNoAi(true); cow.setCustomName(Component.literal("Corridor resident"));
		cow.setPos(center.getX() - 37.5, center.getY() + 1, center.getZ() - 23.5); pocket.addFreshEntity(cow);
		cabin = registry.find(cabin.uuid()).orElseThrow();
		CabinSimulation.sync(server);
		CabinOccupancyData.get(server).enter(OWNER, cabin);
		require(CabinCorridorLayout.chunks(cabin).stream().allMatch(chunk -> pocket.getChunkSource().hasChunk(chunk.x(), chunk.z())), "All moving spaces load without visits");
		var installed = pendingExpansion(server, registry, cabin);
		new CabinUpgradeEffect(server, pocket).apply(cabin, installed);
		require(CabinCorridors.hasPending(server, cabin.uuid()), "Source snapshot remains authoritative before commit");
		require(pocket.getBlockEntity(center.offset(-39, 1, -25)) == null, "Moving inventories are isolated");
		// Save a partial target, then exit before the registry consumes its retained fund.
		CabinCorridors.applyWorldEffect(() -> pocket.setBlock(center.offset(-39, 1, -25), Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS));
		CabinRecord second = deployed(server, COMMITTED_OWNER);
		CabinCorridors.installForRoom(pocket, second.uuid(), COMMITTED_OWNER, CabinCorridor.EAST);
		second = registry.find(second.uuid()).orElseThrow();
		BlockPos secondCenter = PocketDimension.cellCenter(second.cellIndex());
		chest(pocket, secondCenter.offset(6, 1, 0), Items.LAPIS_LAZULI, 19);
		var secondInstall = pendingExpansion(server, registry, second);
		new CabinUpgradeEffect(server, pocket).apply(second, secondInstall);
		registry.completeUpgradeInstallation(second.uuid(), secondInstall.operationId());
		CabinRegistry.flush(server);
		server.saveEverything(false, true, false);
		PortablePocketCabin.LOGGER.info("CORRIDOR_INSTALLATION_TEST_PASSED");
	}

	private static CabinUpgradeState.Installation pendingExpansion(MinecraftServer server, CabinRegistry registry, CabinRecord cabin) {
		var target = CabinUpgradeState.Target.generalSpace(7);
		var fund = new CabinUpgradeState.Fund(target, List.of(new CabinUpgradeState.Requirement(net.minecraft.resources.Identifier.withDefaultNamespace("diamond"), 1)), List.of(new ItemStack(Items.DIAMOND)));
		var installation = CabinUpgradeState.Installation.generalSpace(UUID.randomUUID(), target, 5);
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(fund).withInstallation(installation));
		CabinRegistry.flush(server);
		return installation;
	}


	private static void verify(MinecraftServer server) {
		var registry = CabinRegistry.get(server);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		CabinRecord cabin = registry.findByOwner(OWNER).orElseThrow();
		require(cabin.progression().generalSize() == 7 && !cabin.upgrades().operationInProgress() && cabin.upgrades().funds().isEmpty(), "Recovery completes and consumes one retained fund");
		require(!CabinCorridors.hasPending(server, cabin.uuid()), "Completion discards the snapshot only after saving");
		BlockPos center = PocketDimension.cellCenter(cabin.cellIndex());
		var stay = CabinOccupancyData.get(server).find(OWNER).orElseThrow();
		require(stay.generalSize() == 5 && CabinEvents.requiresMainRoomReturn(center.offset(-38, 1, -25), cabin, stay, false),
			"Players disconnected in moved wings must return to the main room after restart");
		require(!CabinEvents.requiresMainRoomReturn(center.above(), cabin, stay, false), "Main-room reconnects keep their position");
		assertChest(pocket, center.offset(-39, 1, -25), Items.DIAMOND, 11);
		assertChest(pocket, center.offset(0, 1, -9), Items.EMERALD, 17);
		assertChest(pocket, center.offset(7, 1, 0), Items.GOLD_INGOT, 7);
		assertChest(pocket, center.offset(-6, 1, -9), Items.IRON_INGOT, 13);
		CabinReconciliation.reconcileAll(server); CabinUpgradeService.reconcileAll(server); CabinCorridors.reconcileAll(server);
		assertChest(pocket, center.offset(-39, 1, -25), Items.DIAMOND, 11);
		require(pocket.getBlockState(center.offset(-38, 1, -25)).isAir(), "Old inventory positions remain empty");
		for (CabinCorridor corridor : CabinCorridor.values()) {
			BlockPos entrance = CabinCorridorLayout.entrance(cabin.cellIndex(), 7, corridor);
			require(pocket.getBlockState(entrance).isAir() && pocket.getBlockState(entrance.above()).isAir(), "Every expanded opening stays 1x2");
		}
		require(CabinRespawnData.get(server).find(OWNER).orElseThrow().bedPosition().equals(center.offset(-5, 1, -6)), "Moved beds retain cabin-home identity");
		var animal = pocket.getEntityInAnyDimension(ANIMAL);
		require(animal != null && Math.abs(animal.getX() - (center.getX() - 38.5)) < 0.01, "The same animal survives relocation and restart");
		CabinRecord second = registry.findByOwner(COMMITTED_OWNER).orElseThrow();
		assertChest(pocket, PocketDimension.cellCenter(second.cellIndex()).offset(7, 1, 0), Items.LAPIS_LAZULI, 19);
		require(pocket.getBlockState(PocketDimension.cellCenter(second.cellIndex()).offset(3, 0, 0)).is(second.palette().floor().planksBlock()), "Committed recovery repairs the vacated main-room floor");
		registry.beginPacking(cabin.uuid(), OWNER); registry.finishPacking(cabin.uuid());
		assertChest(pocket, center.offset(-39, 1, -25), Items.DIAMOND, 11);
		require(registry.find(cabin.uuid()).orElseThrow().progression().corridors().size() == 3, "Packing preserves installed corridors");
		PortablePocketCabin.LOGGER.info("CORRIDOR_RECOVERY_TEST_PASSED");
	}

	private static void chest(ServerLevel pocket, BlockPos pos, net.minecraft.world.item.Item item, int count) {
		pocket.setBlock(pos, Blocks.CHEST.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		((ChestBlockEntity) pocket.getBlockEntity(pos)).setItem(0, new ItemStack(item, count));
	}
	private static void assertChest(ServerLevel pocket, BlockPos pos, net.minecraft.world.item.Item item, int count) {
		var chest = (ChestBlockEntity) pocket.getBlockEntity(pos);
		require(chest != null && chest.getItem(0).is(item) && chest.getItem(0).getCount() == count, "Changed inventory at " + pos);
	}
	private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
