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
		context.getInput().resizeWindow(1280, 800);
		context.runOnClient(client -> client.options.guiScale().set(3));
		context.getInput().setCursorPos(0, 0);
		captureExterior(context);

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			UUID cabinId = createCabin(world, false);
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> client.player != null && client.level != null
				&& client.level.dimension().equals(PocketDimension.LEVEL_KEY));
			world.getConnection().waitForChunksRender();

			open(context, world, cabinId);
			context.runOnClient(client -> {
				var menu = (CabinUpgradeMenu) client.player.containerMenu;
				if (menu.requirementCount() != 5 || menu.isComplete() || !menu.isAvailable()) {
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

			selectNext(context, world);
			context.runOnClient(client -> {
				var menu = (CabinUpgradeMenu) client.player.containerMenu;
				if (menu.requirementCount() != 13) {
					throw new AssertionError("Expected thirteen window materials");
				}
			});
			capture(context, "cabin-upgrades-window-materials");
			selectNext(context, world);
			context.runOnClient(client -> {
				if (!((CabinUpgradeMenu) client.player.containerMenu).isBlocked()) {
					throw new AssertionError("Expected a prerequisite-blocked second window");
				}
			});
			capture(context, "cabin-upgrades-blocked");
			context.getInput().setCursorPos(340, 380);
			context.waitTick();
			capture(context, "cabin-upgrades-blocked-tooltip");
			context.getInput().setCursorPos(0, 0);

			world.getServer().runOnServer(server -> {
				var registry = CabinRegistry.get(server);
				var cabin = registry.find(cabinId).orElseThrow();
				var identity = new CabinWindowState.Identity(CabinWindowState.Wall.LEFT, 0);
				var windows = cabin.upgrades().windows();
				for (int tier = 0; tier < CabinWindowState.MAX_TIER; tier++) {
					windows = windows.install(identity, tier, List.of());
				}
				registry.updateUpgradeState(cabinId, cabin.upgrades().withWindows(windows));
			});
			open(context, world, cabinId);
			selectNext(context, world);
			context.runOnClient(client -> {
				if (!((CabinUpgradeMenu) client.player.containerMenu).isAtMaximum()) {
					throw new AssertionError("Expected an installed window at maximum tier");
				}
			});
			capture(context, "cabin-upgrades-installed");

			UUID residentCabinId = createCabin(world, true);
			world.getConnection().waitForClientboundPackets();
			world.getConnection().waitForChunksRender();
			fund(world, residentCabinId, true);
			open(context, world, residentCabinId);
			context.runOnClient(client -> {
				var menu = (CabinUpgradeMenu) client.player.containerMenu;
				if (menu.isOwner() || !menu.canUseFund() || !menu.isComplete()) {
					throw new AssertionError("Expected a resident with a complete fund");
				}
			});
			capture(context, "cabin-upgrades-resident");
		}
	}

	private static void captureExterior(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			BlockPos anchor = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				var ground = new BlockPos(0, level.getHeight(
					net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0
				), 0);
				var exterior = new CabinExterior(Level.OVERWORLD, ground, Direction.NORTH);
				if (!ExteriorCabin.validate(level, exterior).valid()) {
					throw new AssertionError("The exterior capture fixture needs clear, level ground");
				}
				var palette = new CabinPalette(CabinPalette.DEFAULT.floor(), CabinPalette.DEFAULT.walls(),
					CabinMaterialProfiles.matchPlanks(new ItemStack(net.minecraft.world.item.Items.SPRUCE_PLANKS))
						.orElseThrow(), CabinPalette.DEFAULT.door());
				ExteriorCabin.place(level, exterior, palette);
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
				world.getConnection().getServerPlayer().setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
				return ground;
			});
			context.runOnClient(client -> {
				client.options.fov().set(70);
				client.options.bobView().set(false);
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			captureExteriorSide(context, world, anchor, "front", 0.5, -6.5, 0);
			captureExteriorSide(context, world, anchor, "rear", 0.5, 11.5, 180);
			captureExteriorSide(context, world, anchor, "left", -8.5, 2.5, -90);
			captureExteriorSide(context, world, anchor, "right", 9.5, 2.5, 90);
			captureExteriorSide(context, world, anchor, "front-left", -6.5, -4.5, -45);
		} finally {
			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
		}
	}

	private static void captureExteriorSide(
		ClientGameTestContext context, TestSingleplayerContext world, BlockPos anchor,
		String side, double x, double z, float yaw
	) {
		world.getServer().runOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			if (!player.teleportTo(server.overworld(), anchor.getX() + x, anchor.getY() + 4,
				anchor.getZ() + z, Set.of(), yaw, 15, false)) {
				throw new AssertionError("Could not position the exterior " + side + " camera");
			}
		});
		world.getConnection().waitForClientboundPackets();
		world.getConnection().waitForChunksRender();
		capture(context, "cabin-exterior-" + side);
	}

	private static UUID createCabin(TestSingleplayerContext world, boolean resident) {
		return world.getServer().computeOnServer(server -> {
			var player = world.getConnection().getServerPlayer();
			var registry = CabinRegistry.get(server);
			registry.resolveWorldAttunement(new WorldAttunement(99, PortablePocketCabin.id("missing/wood")));
			UUID owner = resident ? UUID.fromString("00000000-0000-0000-0000-000000000001") : player.getUUID();
			var palette = resident ? CabinPalette.DEFAULT : new CabinPalette(
				CabinMaterialProfiles.woodProfile(PortablePocketCabin.id("vanilla/wood/spruce"))
					.orElseThrow().selection(),
				CabinMaterialProfiles.woodProfile(PortablePocketCabin.id("vanilla/wood/birch"))
					.orElseThrow().selection(),
				CabinPalette.DEFAULT.roof(), CabinPalette.DEFAULT.door()
			);
			var cabin = registry.create(owner, palette);
			if (resident) {
				registry.trust(cabin.uuid(), owner, player.getUUID());
				registry.setEntryPermission(cabin.uuid(), owner, CabinEntryPermission.TRUSTED_PLAYERS);
			}
			registry.beginDeployment(cabin.uuid(), owner, new CabinExterior(
				Level.OVERWORLD, new BlockPos(0, 100, 0), Direction.NORTH
			));
			var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
			if (pocket == null) {
				throw new AssertionError("Pocket dimension did not load");
			}
			PocketDimension.ensureCabinInterior(pocket, cabin.cellIndex(), palette, cabin.progression().generalSize());
			registry.markInteriorGenerated(cabin.uuid());
			registry.finishDeployment(cabin.uuid());
			BlockPos entrance = PocketDimension.interiorEntrance(cabin.cellIndex());
			if (!player.teleportTo(pocket, entrance.getX() + 0.5, entrance.getY(),
				entrance.getZ() + 0.5, Set.of(), 180, 0, false)) {
				throw new AssertionError("Could not enter the test cabin");
			}
			return cabin.uuid();
		});
	}

	private static void selectNext(ClientGameTestContext context, TestSingleplayerContext world) {
		int previous = context.computeOnClient(client ->
			((CabinUpgradeMenu) client.player.containerMenu).selectedPanelIndex());
		context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(
			client.player.containerMenu.containerId, CabinUpgradeMenu.BUTTON_NEXT_PANEL));
		world.getConnection().waitForServerboundPackets();
		context.waitFor(client -> ((CabinUpgradeMenu) client.player.containerMenu).selectedPanelIndex() != previous);
		world.getConnection().waitForClientboundPackets();
		context.waitTick();
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
		if (!name.endsWith("-tooltip")) {
			context.getInput().setCursorPos(0, 0);
		}
		context.waitTick();
		context.waitTick();
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}
}
