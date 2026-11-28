package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.util.*;

/**
 * Dedicated-server checks for the arboretum grove (B-0023): installing every size in the real dimensions, planting,
 * drops, protection, packing and catch-up, and a restart that keeps it all and moves the door with the main room.
 */
final class CabinArboretumStartupCheck {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000002301");
	private static final UUID SLEEP_OWNER = UUID.fromString("00000000-0000-0000-0000-000000002302");
	private static final BlockPos CHEST = new BlockPos(-1, 1, 3);
	private CabinArboretumStartupCheck() { }

	static void onServerStarted(MinecraftServer server) {
		String mode = System.getProperty("portable-pocket-cabin.arboretum-test", "");
		if (mode.isEmpty()) return;
		server.schedule(new TickTask(server.getTickCount() + 10, () -> {
			try {
				if (mode.equals("seed")) seed(server); else verify(server);
			} catch (Exception e) { PortablePocketCabin.LOGGER.error("Arboretum integration check failed", e); }
			finally { server.halt(false); }
		}));
	}

	private static CabinRecord deployed(MinecraftServer server, UUID owner) {
		var registry = CabinRegistry.get(server);
		var cabin = registry.create(owner);
		var exterior = new CabinExterior(Level.OVERWORLD, new BlockPos((int) cabin.cellIndex() * 64, 200, 0), Direction.NORTH);
		registry.beginDeployment(cabin.uuid(), owner, exterior);
		ExteriorCabin.place(server.overworld(), exterior);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		PocketDimension.ensureCabinInterior(pocket, cabin.cellIndex(), cabin.palette(), 3);
		registry.markInteriorGenerated(cabin.uuid());
		registry.finishDeployment(cabin.uuid());
		cabin = registry.find(cabin.uuid()).orElseThrow();
		PocketDimension.applyGeneralSpaceExpansion(pocket, cabin, 5);
		cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 3, 5, 21);
		return registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withArboretum(CabinArboretumState.EMPTY.reveal()));
	}

	/** Funds and records the next size, then runs its world effect twice, as recovery would. */
	private static CabinRecord grow(MinecraftServer server, CabinRecord cabin, int level) {
		var registry = CabinRegistry.get(server);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		require(CabinArboretum.validate(pocket, cabin, level).success(), "Arboretum size " + level + " fits");
		var requirements = CabinUpgradeCatalog.arboretumRequirements(cabin, level);
		var target = CabinUpgradeState.Target.arboretum(level);
		var stacks = requirements.stream().map(r -> new ItemStack(BuiltInRegistries.ITEM.getOptional(r.itemId()).orElseThrow(), r.count())).toList();
		var install = new CabinUpgradeState.Installation(UUID.randomUUID(), target, level - 1, level);
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(new CabinUpgradeState.Fund(target, requirements, stacks)).withInstallation(install));
		CabinRegistry.flush(server);
		CabinArboretum.install(pocket, cabin, install);
		CabinArboretum.install(pocket, cabin, install);
		return registry.completeUpgradeInstallation(cabin.uuid(), install.operationId());
	}

	private static CabinRecord plantOak(MinecraftServer server, CabinRecord cabin, Identifier... more) {
		var registry = CabinRegistry.get(server);
		var state = cabin.upgrades().arboretum().teach(CabinArboretumPlanting.id(Items.OAK_SAPLING));
		var selected = new ArrayList<Identifier>(List.of(CabinArboretumPlanting.id(Items.OAK_SAPLING)));
		for (var id : more) { state = state.teach(id); selected.add(id); }
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withArboretum(state.select(selected)));
		CabinArboretumPlanting.applySelection(CabinArboretum.level(server), cabin);
		return cabin;
	}

	private static void seed(MinecraftServer server) {
		var registry = CabinRegistry.get(server);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		var grove = CabinArboretum.level(server);
		var cabin = deployed(server, OWNER);
		require(CabinArboretum.regionAt(CabinArboretum.anchor(cabin.cellIndex()).offset(9000, 0, -9000)).orElse(-1) == cabin.cellIndex(),
			"Positions anywhere in a region belong to its cabin");

		// The first size: an obstruction in the empty dimension refuses before anything changes.
		BlockPos blocker = CabinArboretum.world(cabin, new BlockPos(4, 5, 3));
		grove.setBlock(blocker, Blocks.STONE.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		require(!CabinArboretum.validate(pocket, cabin, 1).success(), "An obstruction refuses the purchase");
		grove.setBlock(blocker, Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		cabin = grow(server, cabin, 1);
		require(cabin.upgrades().arboretum().level() == 1 && cabin.progression().corridors().contains(CabinCorridor.WEST),
			"The first size installs the grove and the west corridor");
		require(pocket.getBlockState(CabinArboretum.door(cabin)).is(ArboretumBlocks.ARBORETUM_DOOR)
			&& pocket.getBlockState(CabinArboretum.door(cabin).above()).is(ArboretumBlocks.ARBORETUM_DOOR), "The corridor has the arboretum door");
		require(grove.getBlockState(CabinArboretum.world(cabin, CabinArboretum.EXIT)).is(ArboretumBlocks.GROVE_DOOR)
			&& grove.getBlockState(CabinArboretum.world(cabin, CabinArboretum.BOARD)).is(ArboretumBlocks.PLANTING_BOARD), "The grove has its exit door and board");
		require(grove.getBlockState(CabinArboretum.world(cabin, new BlockPos(0, 10, 3))).is(Blocks.GLASS)
			&& grove.getBlockState(CabinArboretum.world(cabin, new BlockPos(0, 9, 3))).isAir(), "Nine blocks of clear height under a glass roof");
		require(CabinArboretum.spots(cabin).size() == 4, "The first grove has four spots");
		require(CabinProtection.isProtected(grove, CabinArboretum.world(cabin, new BlockPos(4, 2, 3)))
			&& !CabinProtection.isProtected(grove, CabinArboretum.world(cabin, new BlockPos(0, 0, 3))), "Walls are protected; the grass is not");
		BlockPos chest = CabinArboretum.world(cabin, CHEST);
		grove.setBlock(chest, Blocks.CHEST.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		((ChestBlockEntity) grove.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 5));
		require(CabinCorridors.mayChange(grove, OWNER, chest) && !CabinCorridors.mayChange(grove, UUID.randomUUID(), chest), "Guests cannot change the grove");
		cabin = plantOak(server, cabin);
		for (var spot : CabinArboretum.spots(cabin)) require(grove.getBlockState(spot).is(Blocks.OAK_SAPLING), "Every spot gets the selected type");

		// Saplings: none from the grove's leaves or spots, but leaves elsewhere still give them.
		BlockPos leaf = CabinArboretum.world(cabin, new BlockPos(0, 6, 3));
		int groveSaplings = 0, wildSaplings = 0;
		BlockPos wild = new BlockPos(0, 220, 0);
		for (int i = 0; i < 400; i++) {
			groveSaplings += count(Block.getDrops(Blocks.OAK_LEAVES.defaultBlockState(), grove, leaf, null), Items.OAK_SAPLING);
			wildSaplings += count(Block.getDrops(Blocks.OAK_LEAVES.defaultBlockState(), server.overworld(), wild, null), Items.OAK_SAPLING);
		}
		require(groveSaplings == 0 && wildSaplings > 0, "Grove leaves give no saplings; others still do");
		BlockPos spot = CabinArboretum.spots(cabin).getFirst();
		require(count(Block.getDrops(Blocks.OAK_SAPLING.defaultBlockState(), grove, spot, null), Items.OAK_SAPLING) == 0,
			"A spot's sapling gives nothing back");
		grove.setBlock(spot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		require(CabinArboretumPlanting.plant(grove, cabin, spot, grove.getRandom()) && grove.getBlockState(spot).is(Blocks.OAK_SAPLING),
			"A felled spot is planted again");

		cabin = grow(server, cabin, 2);
		require(CabinArboretum.spots(cabin).size() == 20 && grove.getBlockState(spot).is(Blocks.OAK_SAPLING)
			&& ((ChestBlockEntity) grove.getBlockEntity(chest)).getItem(0).getCount() == 5, "The bigger grove keeps its spots and contents");
		require(grove.getBlockState(CabinArboretum.world(cabin, new BlockPos(0, 13, 10))).is(Blocks.GLASS), "Twelve blocks of clear height");
		require(grove.getBlockState(CabinArboretum.world(cabin, new BlockPos(4, 2, 3))).isAir()
			&& grove.getBlockState(CabinArboretum.world(cabin, new BlockPos(4, 0, 3))).is(Blocks.GRASS_BLOCK), "The old wall became open grove");
		cabin = plantOak(server, cabin, CabinArboretumPlanting.id(Items.DARK_OAK_SAPLING));
		for (var each : CabinArboretum.spots(cabin)) require(CabinArboretumPlanting.typeOf(grove.getBlockState(each)).isPresent(), "New spots are planted");
		boolean square = false;
		for (var each : CabinArboretum.spots(cabin))
			if (grove.getBlockState(each).is(Blocks.DARK_OAK_SAPLING))
				square |= CabinArboretumPlanting.cells(each, true).stream().allMatch(cell -> grove.getBlockState(cell).is(Blocks.DARK_OAK_SAPLING));
		require(square, "Dark oak is planted as a 2x2 square");
		// Branching cherries and broad dark oaks grow right up to the walls, but never through them.
		Identifier cherry = CabinArboretumPlanting.id(Items.CHERRY_SAPLING), darkOak = CabinArboretumPlanting.id(Items.DARK_OAK_SAPLING);
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withArboretum(cabin.upgrades().arboretum().teach(cherry).select(List.of(cherry, darkOak))));
		CabinArboretumPlanting.applySelection(grove, cabin);
		var random = net.minecraft.util.RandomSource.create(7);
		int grown = 0;
		for (int attempt = 0; attempt < 60; attempt++)
			for (var each : CabinArboretum.spots(cabin)) {
				var state = grove.getBlockState(each);
				if (state.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock sapling) sapling.advanceTree(grove, each, state, random);
			}
		for (var each : CabinArboretum.spots(cabin)) if (grove.getBlockState(each).is(net.minecraft.tags.BlockTags.LOGS)) grown++;
		require(grown >= 10, "Most saplings grow into trees in the 19x19 grove: " + grown);
		int half = CabinArboretum.half(2);
		for (int x = -half - 4; x <= half + 4; x++) for (int z = -4; z <= CabinArboretum.SIZES[2] + 4; z++) for (int y = -3; y <= CabinArboretum.HEIGHTS[2] + 12; y++) {
			var r = new BlockPos(x, y, z);
			if (!CabinArboretum.inVolume(2, r)) require(grove.getBlockState(CabinArboretum.world(cabin, r)).isAir(), "Trees stay inside the grove, but one reached " + r);
		}

		cabin = grow(server, cabin, 3);
		require(CabinArboretum.spots(cabin).size() == 42 && grove.getBlockState(CabinArboretum.world(cabin, new BlockPos(0, 16, 20))).is(Blocks.GLASS),
			"The biggest grove has 42 spots and fifteen blocks of clear height");
		for (var entry : CabinArboretum.torches(2).entrySet())
			if (!CabinArboretum.torches(3).containsKey(entry.getKey()))
				require(!grove.getBlockState(CabinArboretum.world(cabin, entry.getKey())).is(Blocks.WALL_TORCH), "Old wall torches come down");

		// A second cabin is packed with saplings planted and one spot felled; time passes while it sleeps.
		var sleeping = deployed(server, SLEEP_OWNER);
		sleeping = grow(server, sleeping, 1);
		sleeping = plantOak(server, sleeping);
		grove.setBlock(CabinArboretum.spots(sleeping).getFirst(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		registry.beginPacking(sleeping.uuid(), SLEEP_OWNER);
		sleeping = registry.finishPacking(sleeping.uuid());
		CabinSimulation.sync(server);
		var growth = CabinGreenhouseGrowth.get(server);
		require(growth.groveSleeping(sleeping.uuid()) && !CabinSimulation.isGroveTicketed(server, sleeping.uuid()), "A packed grove sleeps");
		require(CabinSimulation.isGroveTicketed(server, cabin.uuid()), "A deployed grove stays simulated");
		growth.advance(4096L * 8192);
		CabinRegistry.flush(server);
		server.saveEverything(false, true, false);
		PortablePocketCabin.LOGGER.info("ARBORETUM_INSTALLATION_TEST_PASSED");
	}

	private static void verify(MinecraftServer server) {
		var registry = CabinRegistry.get(server);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		var grove = CabinArboretum.level(server);
		var cabin = registry.findByOwner(OWNER).orElseThrow();
		require(cabin.upgrades().arboretum().level() == 3 && cabin.upgrades().arboretum().selected().size() == 2, "Restart keeps the grove's size and types");
		var found = CabinArboretum.cabinAt(server, CabinArboretum.world(cabin, CHEST)).map(CabinRecord::uuid);
		require(found.isPresent() && found.get().equals(cabin.uuid()), "The region still finds its cabin");
		require(((ChestBlockEntity) grove.getBlockEntity(CabinArboretum.world(cabin, CHEST))).getItem(0).getCount() == 5, "Restart keeps the grove's contents");

		// Expanding the main room moves the door with the west wing.
		BlockPos oldDoor = CabinArboretum.door(cabin);
		var install = CabinUpgradeState.Installation.generalSpace(UUID.randomUUID(), CabinUpgradeState.Target.generalSpace(7), 5);
		var requirement = List.of(new CabinUpgradeState.Requirement(net.minecraft.resources.Identifier.withDefaultNamespace("diamond"), 1));
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(new CabinUpgradeState.Fund(install.target(), requirement,
			List.of(new ItemStack(Items.DIAMOND)))).withInstallation(install));
		new CabinUpgradeEffect(server, pocket).apply(cabin, install);
		cabin = registry.completeUpgradeInstallation(cabin.uuid(), install.operationId());
		new CabinUpgradeEffect(server, pocket).refresh(cabin);
		require(!oldDoor.equals(CabinArboretum.door(cabin)) && pocket.getBlockState(CabinArboretum.door(cabin)).is(ArboretumBlocks.ARBORETUM_DOOR)
			&& !pocket.getBlockState(oldDoor).is(ArboretumBlocks.ARBORETUM_DOOR), "The door moves with the west wing");

		// The packed grove wakes on redeployment: the felled spot is replanted and saplings have grown into trees.
		var sleeping = registry.findByOwner(SLEEP_OWNER).orElseThrow();
		require(CabinGreenhouseGrowth.get(server).groveSleeping(sleeping.uuid()), "The grove's sleep survives restart");
		registry.markExteriorCleanupComplete(sleeping.uuid());
		var exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(320, 200, 0), Direction.NORTH);
		registry.beginDeployment(sleeping.uuid(), SLEEP_OWNER, exterior);
		ExteriorCabin.place(server.overworld(), exterior);
		registry.finishDeployment(sleeping.uuid());
		sleeping = registry.find(sleeping.uuid()).orElseThrow();
		CabinSimulation.sync(server);
		require(!CabinGreenhouseGrowth.get(server).groveSleeping(sleeping.uuid()) && !CabinGreenhouseGrowth.hasGrovePending(server, sleeping.uuid()),
			"Catch-up consumes the elapsed time");
		int trees = 0;
		for (var spot : CabinArboretum.spots(sleeping)) {
			require(!grove.getBlockState(spot).isAir(), "Every spot holds a tree or sapling again");
			if (grove.getBlockState(spot).is(Blocks.OAK_LOG)) trees++;
		}
		require(trees > 0, "Saplings grow into trees while the cabin was packed");
		var before = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
		for (var pos : CabinArboretum.growingSpace(sleeping)) before.put(pos, grove.getBlockState(pos));
		CabinSimulation.sync(server);
		CabinGreenhouseGrowth.sync(server);
		for (var entry : before.entrySet()) require(grove.getBlockState(entry.getKey()).equals(entry.getValue()), "Catch-up never runs twice");
		server.saveEverything(false, true, false);
		PortablePocketCabin.LOGGER.info("ARBORETUM_RECOVERY_TEST_PASSED");
	}

	private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
		return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException(message);
	}
}
