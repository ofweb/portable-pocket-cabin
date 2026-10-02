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
		captureStorage(context);

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

	private static void captureStorage(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			UUID cabinId = createCabin(world, false);
			world.getServer().runOnServer(server -> {
				var player = world.getConnection().getServerPlayer();
				var registry = CabinRegistry.get(server);
				var cabin = registry.find(cabinId).orElseThrow();
				player.getInventory().setItem(0, new ItemStack(CabinItems.STORAGE_BOOK, 2));
				CabinUpgrades.useController(player, cabin);
				var upgrades = (CabinUpgradeMenu) player.containerMenu;
				check(upgrades.clickMenuButton(player, CabinUpgradeMenu.BUTTON_STORAGE_BOOK), "Book installation must show a preview");
				check(!registry.find(cabinId).orElseThrow().upgrades().storage().revealed()
					&& player.getInventory().getItem(0).getCount() == 2, "Preview must not consume the book or reveal upgrades");
				check(upgrades.clickMenuButton(player, CabinUpgradeMenu.BUTTON_STORAGE_BOOK), "Owner confirmation must install the book");
				check(registry.find(cabinId).orElseThrow().upgrades().storage().revealed()
					&& player.getInventory().getItem(0).getCount() == 1, "Installation must consume exactly one book");
				check(!upgrades.clickMenuButton(player, CabinUpgradeMenu.BUTTON_STORAGE_BOOK)
					&& player.getInventory().getItem(0).getCount() == 1, "Repeated book installation must make no change");
				upgrades.clickMenuButton(player, CabinUpgradeMenu.BUTTON_GROUP_BASE + 1);
			});
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> client.player.containerMenu instanceof CabinUpgradeMenu menu && menu.selectedGroupIndex() == 1);
			capture(context, "cabin-storage-upgrade-installation");
			world.getServer().runOnServer(server -> {
				var player = world.getConnection().getServerPlayer();
				var registry = CabinRegistry.get(server);
				var cabin = registry.find(cabinId).orElseThrow();
				var storage = CabinStorageState.EMPTY.reveal().upgrade(1).upgrade(2);
				for (var item : BuiltInRegistries.ITEM) {
					if (item == net.minecraft.world.item.Items.AIR) continue;
					storage = storage.deposit(new ItemStack(item, Math.min(64, item.getDefaultMaxStackSize())), 64).state();
					if (storage.used() >= 70) break;
				}
				storage = storage.deposit(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 64), 64).state();
				storage = storage.deposit(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 64), 64).state();
				ItemStack named = new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3);
				named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Family gems"));
				storage = storage.deposit(named, 3).state();
				registry.updateUpgradeState(cabinId, cabin.upgrades().withStorage(storage));
				cabin = registry.find(cabinId).orElseThrow();
				CabinStorage.placeControl(player.level(), cabin, cabin.progression().generalSize());
				var control = CabinStorage.control(cabin);
				player.teleportTo(player.level(), control.getX() + .5, control.getY(), control.getZ() + 1.5,
					Set.of(), 180, 0, false);
				CabinStorage.open(player, cabin);
			});
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> client.gui.screen() instanceof CabinStorageScreen && client.player.containerMenu instanceof CabinStorageMenu menu
				&& menu.capacity() == 108 && menu.entryCount() > 45);
			capture(context, "cabin-storage-search");
			context.runOnClient(client -> ((CabinStorageScreen) client.gui.screen()).mouseScrolled(0, 0, 0, -3));
			capture(context, "cabin-storage-scrolled");
			context.runOnClient(client -> ((CabinStorageScreen) client.gui.screen()).select(1));
			capture(context, "cabin-storage-category");
			context.runOnClient(client -> ((CabinStorageScreen) client.gui.screen()).select(
				net.minecraft.world.item.CreativeModeTabs.tabs().stream().filter(tab -> tab.getType()
					== net.minecraft.world.item.CreativeModeTab.Type.CATEGORY).toList().size() + 2));
			world.getConnection().waitForServerboundPackets();
			capture(context, "cabin-storage-inventory");
			context.runOnClient(client -> ((CabinStorageScreen) client.gui.screen()).searchFor("diamond"));
			world.getConnection().waitForServerboundPackets();
			capture(context, "cabin-storage-search-variants");
			context.getInput().setCursorPos(450, 279);
			capture(context, "cabin-storage-variant-tooltip");
			context.getInput().setCursorPos(435, 240);
			capture(context, "cabin-storage-capacity-tooltip");
			context.getInput().setCursorPos(0, 0);
			context.runOnClient(client -> {
				var menu = (CabinStorageMenu) client.player.containerMenu;
				int entry = -1;
				for (int i = 0; i < menu.entryCount(); i++) if (menu.slots.get(i).getItem().is(net.minecraft.world.item.Items.DIAMOND)
					&& menu.quantity(i) == 128) entry = i;
				check(entry >= 0, "The browser must synchronize aggregate quantities beyond a normal stack");
				client.gameMode.handleContainerInput(menu.containerId, entry, 1,
					net.minecraft.world.inventory.ContainerInput.PICKUP, client.player);
			});
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> client.player.containerMenu.getCarried().getCount() == 1);
			world.getServer().runOnServer(server -> {
				var menu = world.getConnection().getServerPlayer().containerMenu;
				check(menu.getCarried().is(net.minecraft.world.item.Items.DIAMOND), "Client clicks must withdraw the selected server stack");
			});
			world.getServer().runOnServer(server -> {
				var player = world.getConnection().getServerPlayer();
				player.closeContainer();
				var registry = CabinRegistry.get(server);
				var cabin = registry.find(cabinId).orElseThrow();
				var storage = cabin.upgrades().storage();
				while (storage.level() < 6) storage = storage.upgrade(storage.level() + 1);
				registry.updateUpgradeState(cabinId, cabin.upgrades().withStorage(storage));
				var controller = PocketDimension.interiorController(cabin.cellIndex(), cabin.progression().generalSize());
				player.teleportTo(player.level(), controller.getX() + .5, controller.getY(), controller.getZ() - 1.5,
					Set.of(), 0, 0, false);
				CabinUpgrades.useController(player, registry.find(cabinId).orElseThrow());
				((CabinUpgradeMenu) player.containerMenu).clickMenuButton(player, CabinUpgradeMenu.BUTTON_GROUP_BASE + 1);
			});
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> client.player.containerMenu instanceof CabinUpgradeMenu menu && menu.selectedGroupIndex() == 1 && menu.isAtMaximum());
			capture(context, "cabin-storage-fully-upgraded");
			world.getServer().runOnServer(server -> {
				var player = world.getConnection().getServerPlayer();
				player.closeContainer();
				storageMenuMovesPartialStacksAndRecoversCursor(server, player);
				storageControlMovesThroughExpansionWithoutTakingChestItems(server);
			});
		}
	}

	private static void storageMenuMovesPartialStacksAndRecoversCursor(net.minecraft.server.MinecraftServer server, net.minecraft.server.level.ServerPlayer player) {
		var registry = CabinRegistry.get(server);
		UUID owner = UUID.randomUUID();
		var cabin = deployRegistryCabin(registry, owner, 7000);
		registry.trust(cabin.uuid(), owner, player.getUUID());
		registry.setEntryPermission(cabin.uuid(), owner, CabinEntryPermission.TRUSTED_PLAYERS);
		cabin = registry.find(cabin.uuid()).orElseThrow();
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		PocketDimension.ensureCabinInterior(pocket, cabin.cellIndex(), cabin.palette(), cabin.progression().generalSize());
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(CabinStorageState.EMPTY.reveal().upgrade(1)));
		cabin = registry.find(cabin.uuid()).orElseThrow();
		CabinStorage.placeControl(pocket, cabin, cabin.progression().generalSize());
		var control = CabinStorage.control(cabin);
		player.teleportTo(pocket, control.getX() + .5, control.getY(), control.getZ() + 1.5, Set.of(), 180, 0, false);
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) player.getInventory().setItem(i, ItemStack.EMPTY);
		var menu = new CabinStorageMenu(9, player.getInventory(), cabin.uuid());
		player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 40));
		menu.quickMoveStack(player, CabinStorageMenu.ENTRY_SLOTS + 27);
		check(player.getInventory().getItem(0).isEmpty(), "Shift-click must deposit the source stack");
		menu.setCarried(new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 30));
		menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
		check(menu.getCarried().isEmpty() && registry.find(cabin.uuid()).orElseThrow().upgrades().storage().used() == 2,
			"Cursor deposits must merge stacks at normal limits");
		menu.clicked(0, 1, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
		check(menu.getCarried().getCount() == 1, "Right-click must withdraw one item");
		menu.clicked(0, 1, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
		menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
		check(menu.getCarried().getCount() == 64, "Left-click must withdraw at most one normal stack");
		var ops = server.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
		var restored = CabinRegistry.CODEC.parse(ops, CabinRegistry.CODEC.encodeStart(ops, registry).getOrThrow()).getOrThrow();
		var session = restored.find(cabin.uuid()).orElseThrow().upgrades().storage().sessions().getFirst();
		check(session.cursor().getCount() == 64, "A restart must retain custody of the cursor");
		var beforeRemoteChange = registry.find(cabin.uuid()).orElseThrow().upgrades().storage();
		var remotelyChanged = beforeRemoteChange.withdraw(new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT), 6).state()
			.deposit(new ItemStack(net.minecraft.world.item.Items.EMERALD, 3), 3).state();
		var upgrades = registry.find(cabin.uuid()).orElseThrow().upgrades();
		registry.updateUpgradeState(cabin.uuid(), upgrades.withStorage(remotelyChanged));
		menu.broadcastChanges();
		menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
		check(player.getInventory().getItem(0).isEmpty(), "A stale entry click must not withdraw a different item");
		upgrades = registry.find(cabin.uuid()).orElseThrow().upgrades();
		registry.updateUpgradeState(cabin.uuid(), upgrades.withStorage(beforeRemoteChange));
		menu.broadcastChanges();
		registry.untrust(cabin.uuid(), owner, player.getUUID());
		menu.clicked(0, 1, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
		check(menu.getCarried().getCount() == 64 && !menu.stillValid(player), "Access removal must stop transfers");
		menu.removed(player);
		check(player.getInventory().getItem(0).getCount() == 64,
			"Closing must return the recoverable cursor to player inventory");
		CabinStorage.recover(player);
		check(player.getInventory().getItem(0).getCount() == 64
			&& registry.find(cabin.uuid()).orElseThrow().upgrades().storage().sessions().isEmpty(),
			"Recovery must deliver cursor items once");
		check(registry.find(cabin.uuid()).orElseThrow().upgrades().storage().entries().getFirst().getCount() == 6,
			"Withdrawing must retain the exact storage remainder");
		for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 64));
		var receipt = CabinStorage.receipt(player, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 64));
		var retained = registry.find(cabin.uuid()).orElseThrow().upgrades();
		registry.updateUpgradeState(cabin.uuid(), retained.withStorage(retained.storage().withSession(receipt)));
		boolean creative = player.getAbilities().instabuild;
		player.getAbilities().instabuild = true;
		CabinStorage.recover(player);
		var delivery = pocket.getEntity(receipt.operation());
		check(delivery instanceof net.minecraft.world.entity.item.ItemEntity item && item.getItem().getCount() == 64,
			"A full inventory must recover cursor overflow without discarding it in Creative mode");
		retained = registry.find(cabin.uuid()).orElseThrow().upgrades();
		registry.updateUpgradeState(cabin.uuid(), retained.withStorage(retained.storage().withSession(receipt)));
		var pendingDrop = (net.minecraft.world.entity.item.ItemEntity) delivery;
		pendingDrop.addTag(PortablePocketCabin.MOD_ID + ".storage_delivery");
		pendingDrop.setNoPickUpDelay();
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(pendingDrop, pocket);
		pendingDrop.playerTouch(player);
		check(pendingDrop.isAlive() && pendingDrop.hasPickUpDelay(), "An uncommitted delivery must remain uncollectable after reload");
		pendingDrop.setPos(pendingDrop.getX() + 5, pendingDrop.getY(), pendingDrop.getZ());
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server);
		check(pendingDrop.getX() == receipt.delivery().position().getX() + .5, "An uncommitted delivery must stay at its saved position");
		CabinStorage.recover(player);
		check(pocket.getEntity(receipt.operation()) == delivery, "Interrupted overflow delivery must reuse its existing item entity");
		player.getAbilities().instabuild = creative;
	}

	private static void storageControlMovesThroughExpansionWithoutTakingChestItems(net.minecraft.server.MinecraftServer server) {
		var registry = CabinRegistry.get(server);
		var cabin = deployRegistryCabin(registry, UUID.randomUUID(), 8000);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		PocketDimension.ensureCabinInterior(pocket, cabin.cellIndex(), cabin.palette(), cabin.progression().generalSize());
		var storage = CabinStorageState.EMPTY.reveal().upgrade(1).deposit(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3), 3).state();
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(storage));
		cabin = registry.find(cabin.uuid()).orElseThrow();
		CabinStorage.placeControl(pocket, cabin, cabin.progression().generalSize());
		var oldControl = CabinStorage.control(cabin);
		var chestPosition = PocketDimension.cellCenter(cabin.cellIndex()).above();
		pocket.setBlockAndUpdate(chestPosition, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
		var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) pocket.getBlockEntity(chestPosition);
		chest.setItem(0, new ItemStack(net.minecraft.world.item.Items.EMERALD, 7));
		var effect = new CabinUpgradeEffect(server, pocket);
		effect.apply(cabin, CabinUpgradeState.Installation.generalSpace(UUID.randomUUID(), CabinUpgradeState.Target.generalSpace(5), 3));
		registry.expandGeneralSpace(cabin.uuid(), cabin.owner(), 3, 5, 21);
		cabin = registry.find(cabin.uuid()).orElseThrow();
		check(pocket.getBlockState(oldControl).isAir()
			&& pocket.getBlockState(CabinStorage.control(cabin)).is(net.minecraft.world.level.block.Blocks.CHISELED_BOOKSHELF)
			&& CabinProtection.isProtected(pocket, CabinStorage.control(cabin)), "Expansion must move the protected storage control");
		check(chest.getItem(0).getCount() == 7 && cabin.upgrades().storage().entries().getFirst().getCount() == 3,
			"Storage must never inspect or move placed inventories");
		var ops = server.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
		registry.beginPacking(cabin.uuid(), cabin.owner());
		registry.finishPacking(cabin.uuid());
		var restored = CabinRegistry.CODEC.parse(ops, CabinRegistry.CODEC.encodeStart(ops, registry).getOrThrow()).getOrThrow();
		check(restored.find(cabin.uuid()).orElseThrow().upgrades().storage().entries().getFirst().getCount() == 3,
			"Packing and restart must preserve storage");
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
	private static CabinRecord deployRegistryCabin(CabinRegistry registry, UUID owner, int x) {
		var cabin = registry.create(owner);
		registry.beginDeployment(cabin.uuid(), owner, new CabinExterior(Level.OVERWORLD, new BlockPos(x, 100, 0), Direction.NORTH));
		registry.markInteriorGenerated(cabin.uuid());
		return registry.finishDeployment(cabin.uuid());
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
