package dev.portablepocketcabin;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Captures reproducible upgrade states through the real server-synchronized menu. */
public final class PortablePocketCabinClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(960, 720);
		context.runOnClient(client -> client.options.guiScale().set(3));
		context.getInput().setCursorPos(0, 0);

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			UUID cabinId = world.getServer().computeOnServer(server -> {
				var player = world.getConnection().getServerPlayer();
				var registry = CabinRegistry.get(server);
				registry.resolveWorldAttunement(CabinUpgradeDefinitions.current().resolve(0L));
				var cabin = registry.create(player.getUUID());
				registry.beginDeployment(cabin.uuid(), player.getUUID(), new CabinExterior(
					Level.OVERWORLD, new BlockPos(0, 100, 0), Direction.NORTH
				));
				var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
				if (pocket == null) {
					throw new AssertionError("Pocket dimension did not load");
				}
				PocketDimension.ensureCabinInterior(pocket, cabin.cellIndex());
				registry.markInteriorGenerated(cabin.uuid());
				registry.finishDeployment(cabin.uuid());
				BlockPos entrance = PocketDimension.interiorEntrance(cabin.cellIndex());
				if (!player.teleportTo(pocket, entrance.getX() + 0.5, entrance.getY(),
					entrance.getZ() + 0.5, Set.of(), 180, 0, false)) {
					throw new AssertionError("Could not enter the test cabin");
				}
				return cabin.uuid();
			});
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> client.player != null && client.level != null
				&& client.level.dimension().equals(PocketDimension.LEVEL_KEY));
			world.getConnection().waitForChunksRender();

			open(context, world, cabinId);
			context.runOnClient(client -> {
				var menu = (CabinUpgradeMenu) client.player.containerMenu;
				if (menu.requirementCount() == 0 || menu.isComplete() || !menu.isAvailable()) {
					throw new AssertionError("Expected an available, unfunded expansion");
				}
			});
			capture(context, "cabin-upgrades-missing-materials");

			fund(world, cabinId, false);
			open(context, world, cabinId);
			context.runOnClient(client -> {
				var menu = (CabinUpgradeMenu) client.player.containerMenu;
				if (menu.fundedCount(0) != 1 || menu.isComplete()) {
					throw new AssertionError("Expected a partially funded expansion");
				}
			});
			capture(context, "cabin-upgrades-partially-funded");

			fund(world, cabinId, true);
			open(context, world, cabinId);
			context.runOnClient(client -> {
				var menu = (CabinUpgradeMenu) client.player.containerMenu;
				if (!menu.isComplete() || !menu.isAvailable() || !menu.isOwner()) {
					throw new AssertionError("Expected an expansion ready to install");
				}
			});
			capture(context, "cabin-upgrades-ready");
			context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(
				client.player.containerMenu.containerId, CabinUpgradeMenu.BUTTON_INSTALL
			));
			world.getConnection().waitForServerboundPackets();
			context.waitTick();
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> client.player.containerMenu instanceof CabinUpgradeMenu menu && menu.isArmed());
			capture(context, "cabin-upgrades-confirmation");
		}
	}

	private static void open(ClientGameTestContext context, TestSingleplayerContext world, UUID cabinId) {
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			player.closeContainer();
			CabinUpgrades.useController(player, CabinRegistry.get(server).find(cabinId).orElseThrow());
		});
		world.getConnection().waitForClientboundPackets();
		context.waitForScreen(CabinUpgradeScreen.class);
		context.waitTick();
		world.getConnection().waitForClientboundPackets();
	}

	private static void fund(TestSingleplayerContext world, UUID cabinId, boolean complete) {
		world.getServer().runOnServer(server -> {
			var registry = CabinRegistry.get(server);
			var cabin = registry.find(cabinId).orElseThrow();
			var offer = CabinUpgradeCatalog.next(cabin, registry.worldAttunement().orElseThrow(),
				CabinUpgradeDefinitions.current()).orElseThrow();
			List<ItemStack> stacks = complete ? offer.requirements().stream()
				.map(requirement -> new ItemStack(BuiltInRegistries.ITEM.getOptional(requirement.itemId())
					.orElseThrow(), requirement.count())).toList()
				: List.of(new ItemStack(BuiltInRegistries.ITEM.getOptional(offer.requirements().getFirst().itemId())
					.orElseThrow(), 1));
			registry.updateUpgradeState(cabinId, cabin.upgrades().withFund(
				new CabinUpgradeState.Fund(offer.target(), offer.requirements(), stacks)
			));
		});
	}

	private static void capture(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}
}
