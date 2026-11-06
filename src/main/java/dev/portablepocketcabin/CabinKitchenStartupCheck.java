package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;

import java.util.UUID;

/** Installs a kitchen, stops before the commit, and checks on restart that recovery finishes it once. */
final class CabinKitchenStartupCheck {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000b41");
	private static final UUID COOK = UUID.fromString("00000000-0000-0000-0000-000000000b42");

	private CabinKitchenStartupCheck() {
	}

	static void onServerStarted(MinecraftServer server) {
		String mode = System.getProperty("portable-pocket-cabin.kitchen-test", "");
		if (mode.isEmpty()) return;
		server.schedule(new TickTask(server.getTickCount() + 10, () -> {
			try {
				if (mode.equals("seed")) seed(server);
				else verify(server);
			} catch (Exception exception) {
				PortablePocketCabin.LOGGER.error("Kitchen integration check failed", exception);
			} finally {
				server.halt(false);
			}
		}));
	}

	private static void seed(MinecraftServer server) {
		var registry = CabinRegistry.get(server);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		var cabin = registry.create(OWNER);
		var exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(0, 200, 0), Direction.NORTH);
		registry.beginDeployment(cabin.uuid(), OWNER, exterior);
		ExteriorCabin.place(server.overworld(), exterior);
		PocketDimension.ensureCabinInterior(pocket, cabin.cellIndex(), cabin.palette(), 3);
		registry.markInteriorGenerated(cabin.uuid());
		registry.finishDeployment(cabin.uuid());
		cabin = registry.find(cabin.uuid()).orElseThrow();
		require(!CabinKitchen.validate(pocket, cabin).success(), "A 3x3 main room cannot take a kitchen");
		PocketDimension.applyGeneralSpaceExpansion(pocket, cabin, 5);
		cabin = registry.expandGeneralSpace(cabin.uuid(), OWNER, 3, 5, 21);
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withKitchen(CabinKitchenState.EMPTY.reveal()));
		BlockPos center = PocketDimension.cellCenter(cabin.cellIndex());
		BlockPos obstruction = center.offset(CabinKitchen.space(cabin).minimum()).offset(2, 1, 2);
		pocket.setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
		require(!CabinKitchen.validate(pocket, cabin).success(), "A block in the kitchen's space blocks installation");
		pocket.setBlockAndUpdate(obstruction, Blocks.AIR.defaultBlockState());
		require(CabinKitchen.validate(pocket, cabin).success(), "The kitchen fits south of the east corridor");

		var target = CabinUpgradeState.Target.kitchen();
		var requirements = CabinUpgradeCatalog.kitchenRequirements(cabin);
		var stacks = requirements.stream().map(r -> new ItemStack(BuiltInRegistries.ITEM.getOptional(r.itemId()).orElseThrow(), r.count())).toList();
		var operation = new CabinUpgradeState.Installation(UUID.randomUUID(), target, 0, 1);
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(new CabinUpgradeState.Fund(target, requirements, stacks)).withInstallation(operation));
		CabinRegistry.flush(server);
		CabinKitchen.install(pocket, cabin);
		require(CabinKitchen.paused(registry.find(cabin.uuid()).orElseThrow(), center.offset(CabinKitchen.space(cabin).entrance())),
			"The kitchen pauses while it installs");
		// A transfer whose receipt was written but whose oven never saved it: storage already gave 3 wheat. The kitchen
		// is paused until its installation commits, so this oven stands in the main room; recovery reads any station.
		BlockPos oven = center.offset(1, 1, 1);
		CabinCorridors.applyWorldEffect(() -> pocket.setBlockAndUpdate(oven, CookingBlocks.OVEN.defaultBlockState()));
		var empty = ((CookingStationBlockEntity) pocket.getBlockEntity(oven)).contents();
		var filled = new java.util.ArrayList<>(empty);
		filled.set(0, new ItemStack(net.minecraft.world.item.Items.WHEAT, 3));
		int inventory = net.fabricmc.fabric.api.entity.FakePlayer.get(pocket).getInventory().getContainerSize();
		var none = java.util.Collections.nCopies(inventory, ItemStack.EMPTY);
		var receipt = new CabinKitchenTransfer(UUID.randomUUID(), COOK, java.util.List.of(new CabinKitchenTransfer.Change(oven, empty, filled)),
			none, none, java.util.List.of(new ItemStack(net.minecraft.world.item.Items.WHEAT, 3)), java.util.List.of());
		cabin = registry.find(cabin.uuid()).orElseThrow();
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades()
			.withStorage(new CabinStorageState(true, 1, java.util.List.of(new ItemStack(net.minecraft.world.item.Items.WHEAT, 2))))
			.withKitchen(cabin.upgrades().kitchen().withPending(receipt)));
		CabinRegistry.flush(server);
		// Stop before the commit, as a crash would.
		server.saveEverything(false, true, false);
		PortablePocketCabin.LOGGER.info("KITCHEN_INSTALLATION_TEST_PASSED");
	}

	private static void verify(MinecraftServer server) {
		var registry = CabinRegistry.get(server);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		var cabin = registry.findByOwner(OWNER).orElseThrow();
		require(cabin.upgrades().kitchen().level() == 1 && cabin.upgrades().installation().isEmpty()
			&& cabin.progression().rooms().stream().filter(room -> room.type().equals(CabinKitchen.TYPE)).count() == 1,
			"The interrupted kitchen installation completes once");
		require(cabin.progression().corridors().contains(CabinCorridor.EAST), "The east corridor is recorded");
		var receipt = cabin.upgrades().kitchen().pendingFor(COOK).orElseThrow();
		require(receipt.applied().equals(java.util.Optional.of(false)), "Startup recovery undoes a transfer the oven never saved");
		require(cabin.upgrades().storage().stacks().stream().filter(stack -> stack.is(net.minecraft.world.item.Items.WHEAT))
			.mapToInt(ItemStack::getCount).sum() == 5, "Storage gets its wheat back exactly once");
		require(((CookingStationBlockEntity) pocket.getBlockEntity(receipt.stations().getFirst().pos())).isEmpty(), "The oven stays empty");
		var space = CabinKitchen.space(cabin);
		BlockPos center = PocketDimension.cellCenter(cabin.cellIndex());
		BlockPos entrance = center.offset(space.entrance());
		require(pocket.getBlockState(entrance).isAir() && pocket.getBlockState(entrance.above()).isAir(), "The entrance is open");
		require(pocket.getBlockState(center.offset(space.minimum()).offset(0, 2, 3)).is(cabin.palette().walls().planksBlock()),
			"The walls use the cabin's wall wood");
		require(CabinProtection.isProtected(pocket, center.offset(space.minimum()).offset(0, 2, 3)), "The shell is protected");
		for (var lamp : CabinKitchen.lamps(cabin).keySet()) {
			require(pocket.getBlockState(lamp).is(Blocks.LANTERN) && pocket.getBlockState(lamp).getValue(LanternBlock.HANGING),
				"The lamps hang from the ceiling");
			require(!CabinProtection.isProtected(pocket, lamp), "Lamps are ordinary furnishings");
		}
		BlockPos floor = center.offset(space.minimum()).offset(2, 1, 4);
		require(!CabinProtection.isProtected(pocket, floor), "The household can furnish the floor");
		CabinCorridors.applyWorldEffect(() -> pocket.setBlockAndUpdate(floor, CookingBlocks.STOVE.defaultBlockState()));
		require(CabinKitchen.contains(cabin, floor), "A stove on the kitchen floor is a kitchen station");
		registry.beginPacking(cabin.uuid(), OWNER);
		cabin = registry.finishPacking(cabin.uuid());
		registry.markExteriorCleanupComplete(cabin.uuid());
		registry.beginDeployment(cabin.uuid(), OWNER, cabin.lastExterior().orElseThrow());
		registry.finishDeployment(cabin.uuid());
		require(pocket.getBlockState(floor).is(CookingBlocks.STOVE) && registry.findByOwner(OWNER).orElseThrow().upgrades().kitchen().level() == 1,
			"Packing and redeployment keep the kitchen and its stations");
		PortablePocketCabin.LOGGER.info("KITCHEN_RECOVERY_TEST_PASSED");
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException(message);
	}
}
