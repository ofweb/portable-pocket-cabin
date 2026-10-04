package dev.portablepocketcabin;

import dev.portablepocketcabin.CabinCrafting;
import dev.portablepocketcabin.CabinEntryPermission;
import dev.portablepocketcabin.CabinExterior;
import dev.portablepocketcabin.CabinGreenhouse;
import dev.portablepocketcabin.CabinItems;
import dev.portablepocketcabin.CabinMaterialProfiles;
import dev.portablepocketcabin.CabinPalette;
import dev.portablepocketcabin.CabinProtection;
import dev.portablepocketcabin.CabinRecord;
import dev.portablepocketcabin.CabinRegistry;
import dev.portablepocketcabin.CabinStation;
import dev.portablepocketcabin.CabinStationClient;
import dev.portablepocketcabin.CabinStorage;
import dev.portablepocketcabin.CabinStorageMenu;
import dev.portablepocketcabin.CabinStorageScreen;
import dev.portablepocketcabin.CabinStorageState;
import dev.portablepocketcabin.CabinUpgradeCatalog;
import dev.portablepocketcabin.CabinUpgradeDefinitions;
import dev.portablepocketcabin.CabinUpgradeEffect;
import dev.portablepocketcabin.CabinUpgradeMenu;
import dev.portablepocketcabin.CabinUpgradeScreen;
import dev.portablepocketcabin.CabinUpgradeState;
import dev.portablepocketcabin.CabinUpgrades;
import dev.portablepocketcabin.CabinWindowState;
import dev.portablepocketcabin.ExteriorCabin;
import dev.portablepocketcabin.PocketDimension;
import dev.portablepocketcabin.PortablePocketCabin;
import dev.portablepocketcabin.WorldAttunement;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Captures reproducible upgrade states through the real server-synchronized menu. */
public final class PortablePocketCabinClientGameTest
    implements FabricClientGameTest
{

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 800);
        context.runOnClient(client -> client.options.guiScale().set(3));
        context.getInput().setCursorPos(0, 0);
        boolean captureAll = "1".equals(System.getenv("PPC_CAPTURE_ALL"));
        if (captureAll || "1".equals(System.getenv("PPC_CAPTURE_CREATIVE"))) {
            VanillaCreativeScreenshots.capture(context);
            if (!captureAll) {
                captureStorage(context);
                return;
            }
        }
        if (captureAll || "1".equals(System.getenv("PPC_CAPTURE_STATIONS"))) {
            VanillaStationScreenshots.capture(context);
            if (!captureAll) return;
        }
        captureBooks(context);
        if (!captureAll && "1".equals(System.getenv("PPC_TEST_BOOKS"))) return;
        captureGreenhouse(context);
        if (!captureAll && "1".equals(System.getenv("PPC_TEST_GREENHOUSE"))) return;
        captureCrafting(context);
        if (!captureAll && "1".equals(System.getenv("PPC_TEST_CRAFTING"))) return;
        captureExterior(context);
        captureStorage(context);

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            UUID cabinId = createCabin(world, false);
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                client ->
                    client.player != null &&
                    client.level != null &&
                    client.level.dimension().equals(PocketDimension.LEVEL_KEY)
            );
            world.getConnection().waitForChunksRender();

            open(context, world, cabinId);
            context.runOnClient(client -> {
                var menu = (CabinUpgradeMenu) client.player.containerMenu;
                if (
                    menu.requirementCount() != 5 ||
                    menu.isComplete() ||
                    !menu.isAvailable()
                ) {
                    throw new AssertionError(
                        "Expected an available, unfunded expansion"
                    );
                }
            });
            capture(context, "cabin-upgrades-missing-materials");

            fund(world, cabinId, false);
            open(context, world, cabinId);
            context.runOnClient(client -> {
                var menu = (CabinUpgradeMenu) client.player.containerMenu;
                if (menu.fundedCount(0) != 1 || menu.isComplete()) {
                    throw new AssertionError(
                        "Expected a partially funded expansion"
                    );
                }
            });
            capture(context, "cabin-upgrades-partially-funded");

            fund(world, cabinId, true);
            open(context, world, cabinId);
            context.runOnClient(client -> {
                var menu = (CabinUpgradeMenu) client.player.containerMenu;
                if (
                    !menu.isComplete() || !menu.isAvailable() || !menu.isOwner()
                ) {
                    throw new AssertionError(
                        "Expected an expansion ready to install"
                    );
                }
            });
            capture(context, "cabin-upgrades-ready");
            context.runOnClient(client ->
                client.gameMode.handleInventoryButtonClick(
                    client.player.containerMenu.containerId,
                    CabinUpgradeMenu.BUTTON_INSTALL
                )
            );
            world.getConnection().waitForServerboundPackets();
            context.waitTick();
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                client ->
                    client.player.containerMenu instanceof
                        CabinUpgradeMenu menu && menu.isArmed()
            );
            capture(context, "cabin-upgrades-confirmation");

            selectNext(context, world);
            context.runOnClient(client -> {
                var menu = (CabinUpgradeMenu) client.player.containerMenu;
                if (menu.requirementCount() != 13) {
                    throw new AssertionError(
                        "Expected thirteen window materials"
                    );
                }
            });
            capture(context, "cabin-upgrades-window-materials");
            selectNext(context, world);
            context.runOnClient(client -> {
                if (
                    !(
                        (CabinUpgradeMenu) client.player.containerMenu
                    ).isBlocked()
                ) {
                    throw new AssertionError(
                        "Expected a prerequisite-blocked second window"
                    );
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
                var identity = new CabinWindowState.Identity(
                    CabinWindowState.Wall.LEFT,
                    0
                );
                var windows = cabin.upgrades().windows();
                for (int tier = 0; tier < CabinWindowState.MAX_TIER; tier++) {
                    windows = windows.install(identity, tier, List.of());
                }
                registry.updateUpgradeState(
                    cabinId,
                    cabin.upgrades().withWindows(windows)
                );
            });
            open(context, world, cabinId);
            selectNext(context, world);
            context.runOnClient(client -> {
                if (
                    !(
                        (CabinUpgradeMenu) client.player.containerMenu
                    ).isAtMaximum()
                ) {
                    throw new AssertionError(
                        "Expected an installed window at maximum tier"
                    );
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
                if (
                    menu.isOwner() || !menu.canUseFund() || !menu.isComplete()
                ) {
                    throw new AssertionError(
                        "Expected a resident with a complete fund"
                    );
                }
            });
            capture(context, "cabin-upgrades-resident");
        }
    }

    private static void captureBooks(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            UUID cabinId = createCabin(world, false);
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(CabinItems.STORAGE_BOOK, 4));
                player.getInventory().setItem(1, new ItemStack(CabinItems.CRAFTING_BOOK, 3));
                player.getInventory().setItem(2, new ItemStack(CabinItems.GREENHOUSE_BOOK));
            });
            open(context, world, cabinId);
            selectBooks(context, world);
            capture(context, "cabin-books-empty");

            // Choose crafting even though storage comes first in inventory order.
            inputBook(context, world, 1);
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var menu = (CabinUpgradeMenu) player.containerMenu;
                var cabin = CabinRegistry.get(server).find(cabinId).orElseThrow();
                check(menu.bookStack().is(CabinItems.CRAFTING_BOOK) && menu.bookStack().getCount() == 3,
                    "Manual placement must choose the exact book stack");
                check(!cabin.upgrades().crafting().revealed() && player.getInventory().getItem(0).getCount() == 4,
                    "Preview must not consume or select a different book");
            });
            capture(context, "cabin-books-crafting-preview");
            double[] hover = context.computeOnClient(client -> {
                var window = client.getWindow();
                return new double[] {
                    ((window.getGuiScaledWidth() - CabinUpgradeLayout.SCREEN_WIDTH) / 2
                        + CabinUpgradeLayout.BOOK_INPUT_X + 8) * window.getGuiScale(),
                    ((window.getGuiScaledHeight() - CabinUpgradeLayout.SCREEN_HEIGHT) / 2
                        + CabinUpgradeLayout.BOOK_INPUT_Y + 8) * window.getGuiScale()
                };
            });
            context.getInput().setCursorPos(hover[0], hover[1]);
            capture(context, "cabin-books-crafting-tooltip");
            context.getInput().setCursorPos(0, 0);
            installBook(context, world);
            context.waitFor(client -> !((CabinUpgradeMenu) client.player.containerMenu).canInstallBook());
            capture(context, "cabin-books-already-installed");
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var menu = (CabinUpgradeMenu) player.containerMenu;
                var cabin = CabinRegistry.get(server).find(cabinId).orElseThrow();
                check(cabin.upgrades().crafting().revealed() && cabin.upgrades().crafting().level() == 0
                    && cabin.progression().rooms().isEmpty() && menu.bookStack().getCount() == 2,
                    "One confirmation must reveal only crafting without purchasing a room");
                check(!menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_INSTALL_BOOK)
                    && menu.bookStack().getCount() == 2, "Duplicate installation must consume nothing");
                menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_GROUP_BASE);
                check(!menu.slots.get(CabinUpgradeMenu.BOOK_SLOT).isActive(), "Input slot must be hidden on upgrade tabs");
                menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_BOOKS);
                check(menu.bookStack().getCount() == 2, "Switching tabs must retain the input stack");
                player.closeContainer();
                check(player.getInventory().countItem(CabinItems.CRAFTING_BOOK) == 2,
                    "Closing must return unused copies");
            });

            for (int inventorySlot : new int[] {0, 2}) {
                open(context, world, cabinId);
                selectBooks(context, world);
                inputBook(context, world, inventorySlot);
                capture(context, inventorySlot == 0 ? "cabin-books-storage-preview" : "cabin-books-greenhouse-preview");
                installBook(context, world);
                world.getServer().runOnServer(server -> world.getConnection().getServerPlayer().closeContainer());
            }
            open(context, world, cabinId);
            selectBooks(context, world);
            capture(context, "cabin-books-all-installed");
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var menu = (CabinUpgradeMenu) player.containerMenu;
                var registry = CabinRegistry.get(server);
                var cabin = registry.find(cabinId).orElseThrow();
                check(menu.booksSelected() && menu.groupCount() == 5 && cabin.upgrades().storage().revealed()
                    && cabin.upgrades().greenhouse().revealed(), "Books tab must remain after all books are installed");
                check(player.getInventory().countItem(CabinItems.STORAGE_BOOK) == 3
                    && player.getInventory().countItem(CabinItems.GREENHOUSE_BOOK) == 0,
                    "Each installation must consume exactly one copy");
                for (var invalid : List.of(net.minecraft.world.item.Items.BOOK,
                    net.minecraft.world.item.Items.ENCHANTED_BOOK, net.minecraft.world.item.Items.DIAMOND)) {
                    menu.setCarried(new ItemStack(invalid));
                    menu.clicked(CabinUpgradeMenu.BOOK_SLOT, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
                    check(menu.bookStack().isEmpty() && menu.getCarried().is(invalid), "Input must reject other items");
                }
                menu.setCarried(ItemStack.EMPTY);
                // Recover the input after losing the menu's temporary container.
                menu.setCarried(new ItemStack(CabinItems.GREENHOUSE_BOOK, 2));
                menu.clicked(CabinUpgradeMenu.BOOK_SLOT, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
                var session = registry.find(cabinId).orElseThrow().upgrades().storage().sessions().getFirst();
                check(session.escrow().size() == 1 && session.escrow().getFirst().stack().getCount() == 2,
                    "The input stack must be checkpointed for recovery");
                menu.getSlot(CabinUpgradeMenu.BOOK_SLOT).set(ItemStack.EMPTY);
                CabinStorage.recover(player);
                check(player.getInventory().countItem(CabinItems.GREENHOUSE_BOOK) == 2,
                    "Recovery must return the exact unconsumed stack after the menu is lost");
                check(previewBook(menu, player, 2), "A recovered stack can return to the input slot");
                registry.beginPacking(cabinId, player.getUUID());
                check(!menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_INSTALL_BOOK)
                    && menu.bookStack().getCount() == 2, "Lifecycle changes must block installation without consumption");
                registry.abortPacking(cabinId);
                for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(net.minecraft.world.item.Items.DIRT, 64));
                int before = player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    player.getBoundingBox().inflate(3), item -> item.getItem().is(CabinItems.GREENHOUSE_BOOK)).size();
                player.closeContainer();
                var drops = player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    player.getBoundingBox().inflate(3), item -> item.getItem().is(CabinItems.GREENHOUSE_BOOK));
                check(drops.size() == before + 1 && drops.stream().anyMatch(item -> item.getItem().getCount() == 2),
                    "Unused books must drop beside the player when inventory is full");
                check(registry.find(cabinId).orElseThrow().upgrades().storage().sessions().isEmpty(),
                    "Closing must clear the custody record after delivery");
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(CabinItems.STORAGE_BOOK));
                CabinUpgrades.useController(player, registry.find(cabinId).orElseThrow());
                menu = (CabinUpgradeMenu) player.containerMenu;
                check(previewBook(menu, player, 0), "A book can be held until the player dies");
                player.setHealth(0);
                player.closeContainer();
                check(player.getInventory().countItem(CabinItems.STORAGE_BOOK) == 0
                    && player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        player.getBoundingBox().inflate(3), item -> item.getItem().is(CabinItems.STORAGE_BOOK)).size() == 1,
                    "Closing after death must drop the book instead of placing it in a dead inventory");
                player.setHealth(20);
            });
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            UUID cabinId = createCabin(world, true);
            open(context, world, cabinId);
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var menu = (CabinUpgradeMenu) player.containerMenu;
                check(!menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_BOOKS), "Residents cannot install books");
                check(!menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_INSTALL_BOOK), "Forged install requests must fail");
                check(!menu.slots.get(CabinUpgradeMenu.BOOK_SLOT).mayPlace(new ItemStack(CabinItems.STORAGE_BOOK)),
                    "Residents cannot deposit into the book slot");
            });
            capture(context, "cabin-books-resident");
        }
    }

    private static void selectBooks(ClientGameTestContext context, TestSingleplayerContext world) {
        context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(
            client.player.containerMenu.containerId, CabinUpgradeMenu.BUTTON_BOOKS));
        world.getConnection().waitForServerboundPackets();
        world.getConnection().waitForClientboundPackets();
        context.waitFor(client -> ((CabinUpgradeMenu) client.player.containerMenu).booksSelected());
    }

    private static void inputBook(ClientGameTestContext context, TestSingleplayerContext world, int inventorySlot) {
        context.runOnClient(client -> {
            var menu = (CabinUpgradeMenu) client.player.containerMenu;
            int slot = menu.slots.stream().filter(value -> value.container == client.player.getInventory()
                && value.getContainerSlot() == inventorySlot).findFirst().orElseThrow().index;
            client.gameMode.handleContainerInput(menu.containerId, slot, 0,
                net.minecraft.world.inventory.ContainerInput.PICKUP, client.player);
            client.gameMode.handleContainerInput(menu.containerId, CabinUpgradeMenu.BOOK_SLOT, 0,
                net.minecraft.world.inventory.ContainerInput.PICKUP, client.player);
        });
        world.getConnection().waitForServerboundPackets();
        world.getConnection().waitForClientboundPackets();
        context.waitFor(client -> ((CabinUpgradeMenu) client.player.containerMenu).canInstallBook());
    }

    private static void installBook(ClientGameTestContext context, TestSingleplayerContext world) {
        context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(
            client.player.containerMenu.containerId, CabinUpgradeMenu.BUTTON_INSTALL_BOOK));
        world.getConnection().waitForServerboundPackets();
        world.getConnection().waitForClientboundPackets();
    }

    private static boolean previewBook(CabinUpgradeMenu menu, net.minecraft.server.level.ServerPlayer player, int inventorySlot) {
        if (!menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_BOOKS)) return false;
        int slot = menu.slots.stream().filter(value -> value.container == player.getInventory()
            && value.getContainerSlot() == inventorySlot).findFirst().orElseThrow().index;
        menu.clicked(slot, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        menu.clicked(CabinUpgradeMenu.BOOK_SLOT, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        return !menu.bookStack().isEmpty();
    }

    private static void captureGreenhouse(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            UUID cabinId = createCabin(world, false);
            world.getServer().runOnServer(server -> {
                var registry = CabinRegistry.get(server);
                var player = world.getConnection().getServerPlayer();
                var cabin = registry.find(cabinId).orElseThrow();
                registry.expandGeneralSpace(cabinId, cabin.owner(), 3, 5, 21);
                cabin = registry.find(cabinId).orElseThrow();
                PocketDimension.ensureCabinInterior(
                    player.level(),
                    cabin.cellIndex(),
                    cabin.palette(),
                    5
                );
                player
                    .getInventory()
                    .setItem(0, new ItemStack(CabinItems.GREENHOUSE_BOOK, 2));
                CabinUpgrades.useController(player, cabin);
                var menu = (CabinUpgradeMenu) player.containerMenu;
                check(
                    previewBook(menu, player, 0),
                    "Book previews all four purchases"
                );
                check(
                    !registry
                        .find(cabinId)
                        .orElseThrow()
                        .upgrades()
                        .greenhouse()
                        .revealed(),
                    "Preview consumes nothing"
                );
            });
            world.getConnection().waitForClientboundPackets();
            context.waitForScreen(CabinUpgradeScreen.class);
            context.waitFor(client -> ((CabinUpgradeMenu) client.player.containerMenu).booksSelected() && ((CabinUpgradeMenu) client.player.containerMenu).canInstallBook());
            capture(context, "greenhouse-book-confirmation");
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var menu = (CabinUpgradeMenu) player.containerMenu;
                check(
                    menu.clickMenuButton(
                        player,
                        CabinUpgradeMenu.BUTTON_INSTALL_BOOK
                    ),
                    "Book confirms"
                );
                var cabin = CabinRegistry.get(server)
                    .find(cabinId)
                    .orElseThrow();
                check(
                    cabin.upgrades().greenhouse().revealed() &&
                        cabin.upgrades().greenhouse().level() == 0 &&
                        menu.bookStack().getCount() == 1,
                    "One book reveals without purchasing"
                );
                menu.clickMenuButton(
                    player,
                    CabinUpgradeMenu.BUTTON_GROUP_BASE + menu.groupCount() - 2
                );
            });
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(3);
            capture(context, "greenhouse-missing-materials");
            for (int level = 1; level <= 4; level++) {
                final int targetLevel = level;
                world.getServer().runOnServer(server -> {
                    var registry = CabinRegistry.get(server);
                    var cabin = registry.find(cabinId).orElseThrow();
                    var target = CabinUpgradeState.Target.greenhouse(
                        targetLevel
                    );
                    var requirements =
                        CabinUpgradeCatalog.greenhouseRequirements(targetLevel);
                    var stacks = requirements
                        .stream()
                        .map(r ->
                            new ItemStack(
                                BuiltInRegistries.ITEM.getOptional(
                                    r.itemId()
                                ).orElseThrow(),
                                r.count()
                            )
                        )
                        .toList();
                    registry.updateUpgradeState(
                        cabinId,
                        cabin
                            .upgrades()
                            .withFund(
                                new CabinUpgradeState.Fund(
                                    target,
                                    requirements,
                                    stacks
                                )
                            )
                    );
                    var menu = (CabinUpgradeMenu) world
                        .getConnection()
                        .getServerPlayer()
                        .containerMenu;
                    menu.broadcastChanges();
                });
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(3);
                if (level == 1 || level == 4) capture(
                    context,
                    "greenhouse-level-" + level + "-ready"
                );
                world.getServer().runOnServer(server -> {
                    var player = world.getConnection().getServerPlayer();
                    var menu = (CabinUpgradeMenu) player.containerMenu;
                    check(
                        menu.clickMenuButton(
                            player,
                            CabinUpgradeMenu.BUTTON_INSTALL
                        ),
                        "Purchase asks for confirmation"
                    );
                    check(
                        menu.clickMenuButton(
                            player,
                            CabinUpgradeMenu.BUTTON_INSTALL
                        ),
                        "Fund purchases the greenhouse"
                    );
                    check(
                        CabinRegistry.get(server)
                            .find(cabinId)
                            .orElseThrow()
                            .upgrades()
                            .greenhouse()
                            .level() == targetLevel,
                        "Manual greenhouse needs no storage or automation"
                    );
                });
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(3);
            }
            capture(context, "greenhouse-fully-upgraded");
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.closeContainer();
                var cabin = CabinRegistry.get(server)
                    .find(cabinId)
                    .orElseThrow();
                var entrance = PocketDimension.cellCenter(
                    cabin.cellIndex()
                ).offset(CabinGreenhouse.space(cabin, 4).entrance());
                player.teleportTo(player.level(), entrance.getX() + .5, entrance.getY(), entrance.getZ() - .5, Set.of(), 0, 0, false);
                player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, 0, 2));
                check(player.getZ() >= entrance.getZ() + 1.4, "Players walk through the 1x2 entrance onto the garden path");
                player.teleportTo(
                    player.level(),
                    entrance.getX() + .5,
                    entrance.getY(),
                    entrance.getZ() + 1.5,
                    Set.of(),
                    0,
                    12,
                    false
                );
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(5);
            capture(context, "greenhouse-garden");
        }
    }

    private static void captureCrafting(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            UUID cabinId = createCabin(world, false);
            world.getServer().runOnServer(server -> {
                var registry = CabinRegistry.get(server);
                var player = world.getConnection().getServerPlayer();
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                var cabin = registry.find(cabinId).orElseThrow();
                registry.expandGeneralSpace(cabinId, cabin.owner(), 3, 5, 21);
                cabin = registry.find(cabinId).orElseThrow();
                PocketDimension.ensureCabinInterior(
                    player.level(),
                    cabin.cellIndex(),
                    cabin.palette(),
                    5
                );
                player
                    .getInventory()
                    .setItem(0, new ItemStack(CabinItems.CRAFTING_BOOK, 2));
                CabinUpgrades.useController(player, cabin);
                var upgrades = (CabinUpgradeMenu) player.containerMenu;
                check(
                    previewBook(upgrades, player, 0),
                    "Crafting book must ask for confirmation"
                );
                check(
                    !registry
                        .find(cabinId)
                        .orElseThrow()
                        .upgrades()
                        .crafting()
                        .revealed(),
                    "Book preview must not reveal or purchase a room"
                );
                check(
                    upgrades.clickMenuButton(
                        player,
                        CabinUpgradeMenu.BUTTON_INSTALL_BOOK
                    ),
                    "Crafting book confirmation must succeed"
                );
                cabin = registry.find(cabinId).orElseThrow();
                check(
                    cabin.upgrades().crafting().revealed() &&
                        cabin.upgrades().crafting().level() == 0 &&
                        cabin.progression().rooms().isEmpty() &&
                        upgrades.bookStack().getCount() == 1,
                    "One book reveals the three levels without purchasing the room"
                );
                player.closeContainer();
                for (int level = 1; level <= 3; level++) {
                    cabin = registry.find(cabinId).orElseThrow();
                    if (level == 2) {
                        var station = CabinCrafting.stations(cabin, 2)
                            .entrySet()
                            .stream()
                            .filter(e ->
                                e
                                    .getValue()
                                    .is(
                                        net.minecraft.world.level.block.Blocks.STONECUTTER
                                    )
                            )
                            .findFirst()
                            .orElseThrow()
                            .getKey();
                        player
                            .level()
                            .setBlockAndUpdate(
                                station,
                                net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState()
                            );
                        (
                            (net.minecraft.world.level.block.entity.ChestBlockEntity) player
                                .level()
                                .getBlockEntity(station)
                        ).setItem(
                            0,
                            new ItemStack(
                                net.minecraft.world.item.Items.DIAMOND,
                                5
                            )
                        );
                        player
                            .level()
                            .setBlockAndUpdate(
                                station.east(),
                                net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState()
                            );
                    }
                    var effect = new CabinUpgradeEffect(server, player.level());
                    var target = CabinUpgradeState.Target.crafting(level);
                    var requirements = CabinUpgradeCatalog.craftingRequirements(
                        cabin,
                        level
                    );
                    var offer = CabinUpgradeCatalog.offer(
                        cabin,
                        target,
                        new WorldAttunement(
                            1,
                            CabinPalette.DEFAULT.walls().profileId()
                        ),
                        CabinUpgradeDefinitions.current()
                    ).orElseThrow();
                    check(
                        effect.validate(cabin, offer).success(),
                        "Room installation must validate"
                    );
                    var stacks = requirements
                        .stream()
                        .map(r ->
                            new ItemStack(
                                BuiltInRegistries.ITEM.getOptional(
                                    r.itemId()
                                ).orElseThrow(),
                                r.count()
                            )
                        )
                        .toList();
                    var installation = new CabinUpgradeState.Installation(
                        UUID.randomUUID(),
                        target,
                        level - 1,
                        level
                    );
                    registry.updateUpgradeState(
                        cabinId,
                        cabin
                            .upgrades()
                            .withFund(
                                new CabinUpgradeState.Fund(
                                    target,
                                    requirements,
                                    stacks
                                )
                            )
                            .withInstallation(installation)
                    );
                    CabinRegistry.flush(server);
                    effect.apply(cabin, installation);
                    effect.apply(cabin, installation);
                    cabin = registry.completeUpgradeInstallation(
                        cabinId,
                        installation.operationId()
                    );
                    CabinRegistry.flush(server);
                    effect.refresh(cabin);
                }
                check(
                    cabin.progression().rooms().size() == 1,
                    "Replay must create only one crafting room"
                );
                var station = CabinCrafting.stations(cabin, 2)
                    .entrySet()
                    .stream()
                    .filter(e ->
                        e
                            .getValue()
                            .is(
                                net.minecraft.world.level.block.Blocks.STONECUTTER
                            )
                    )
                    .findFirst()
                    .orElseThrow()
                    .getKey();
                check(
                    player
                        .level()
                        .getBlockState(station.east())
                        .is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK),
                    "Only the station footprint may be replaced"
                );
                var bounds = new net.minecraft.world.phys.AABB(
                    net.minecraft.world.phys.Vec3.atLowerCornerOf(
                        PocketDimension.cellCenter(cabin.cellIndex()).offset(
                            CabinCrafting.space(cabin).minimum()
                        )
                    ),
                    net.minecraft.world.phys.Vec3.atLowerCornerOf(
                        PocketDimension.cellCenter(cabin.cellIndex()).offset(
                            CabinCrafting.space(cabin).maximum()
                        )
                    )
                ).inflate(1);
                int diamonds = player
                    .level()
                    .getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        bounds
                    )
                    .stream()
                    .filter(e ->
                        e.getItem().is(net.minecraft.world.item.Items.DIAMOND)
                    )
                    .mapToInt(e -> e.getItem().getCount())
                    .sum();
                check(
                    diamonds == 5,
                    "Interrupted station installation must deliver container contents once"
                );
                player.getInventory().clearContent();
                player
                    .getInventory()
                    .setItem(
                        0,
                        CabinCraftingGameTest.named(
                            net.minecraft.world.item.Items.OAK_PLANKS
                        )
                    );
                player
                    .getInventory()
                    .setItem(
                        1,
                        new ItemStack(
                            net.minecraft.world.item.Items.BIRCH_PLANKS,
                            8
                        )
                    );
                var entrance = PocketDimension.cellCenter(cabin.cellIndex())
                    .offset(CabinCrafting.space(cabin).entrance())
                    .west(2);
                player.teleportTo(
                    player.level(),
                    entrance.getX() + .5,
                    entrance.getY(),
                    entrance.getZ() + .5,
                    Set.of(),
                    0,
                    0,
                    false
                );
                var crafting = CabinCrafting.stations(cabin, 3)
                    .entrySet()
                    .stream()
                    .filter(e ->
                        e
                            .getValue()
                            .is(
                                net.minecraft.world.level.block.Blocks.CRAFTING_TABLE
                            )
                    )
                    .findFirst()
                    .orElseThrow()
                    .getKey();
                check(
                    CabinStation.open(player, cabin, crafting) ==
                        net.minecraft.world.InteractionResult.SUCCESS_SERVER,
                    "Manual crafting must open without storage"
                );
                var recipe = server
                    .getRecipeManager()
                    .getRecipes()
                    .stream()
                    .filter(r ->
                        r
                            .id()
                            .identifier()
                            .equals(
                                net.minecraft.resources.Identifier.withDefaultNamespace(
                                    "stick"
                                )
                            )
                    )
                    .findFirst()
                    .orElseThrow();
                var menu =
                    (net.minecraft.world.inventory.CraftingMenu) player.containerMenu;
                menu.handlePlacement(
                    false,
                    false,
                    recipe,
                    player.level(),
                    player.getInventory()
                );
                check(
                    menu
                        .getInputGridSlots()
                        .stream()
                        .filter(net.minecraft.world.inventory.Slot::hasItem)
                        .allMatch(slot ->
                            slot
                                .getItem()
                                .is(net.minecraft.world.item.Items.BIRCH_PLANKS)
                        ),
                    "Without storage fill uses ordinary inventory ingredients"
                );
                player.closeContainer();
                check(
                    player.getInventory().getItem(1).getCount() == 8,
                    "Closing returns inventory ingredients"
                );
                cabin = registry.find(cabinId).orElseThrow();
                registry.updateUpgradeState(
                    cabinId,
                    cabin
                        .upgrades()
                        .withStorage(
                            CabinStorageState.EMPTY.reveal()
                                .upgrade(1)
                                .deposit(
                                    new ItemStack(
                                        net.minecraft.world.item.Items.OAK_PLANKS,
                                        64
                                    ),
                                    64
                                )
                                .state()
                        )
                );
                cabin = registry.find(cabinId).orElseThrow();
                CabinStation.open(player, cabin, crafting);
                menu =
                    (net.minecraft.world.inventory.CraftingMenu) player.containerMenu;
                menu.handlePlacement(
                    false,
                    false,
                    recipe,
                    player.level(),
                    player.getInventory()
                );
                check(
                    menu
                        .getInputGridSlots()
                        .stream()
                        .filter(net.minecraft.world.inventory.Slot::hasItem)
                        .allMatch(
                            slot ->
                                slot
                                    .getItem()
                                    .is(
                                        net.minecraft.world.item.Items.OAK_PLANKS
                                    ) &&
                                !slot
                                    .getItem()
                                    .has(
                                        net.minecraft.core.component.DataComponents.CUSTOM_NAME
                                    )
                        ),
                    "Storage ingredients take priority over inventory"
                );
                menu.setCarried(
                    new ItemStack(net.minecraft.world.item.Items.STICK, 63)
                );
                menu.clicked(
                    0,
                    0,
                    net.minecraft.world.inventory.ContainerInput.PICKUP,
                    player
                );
                check(
                    menu.getCarried().getCount() == 63 &&
                        menu.getResultSlot().getItem().getCount() == 4,
                    "Insufficient cursor space must craft nothing"
                );
                menu.setCarried(ItemStack.EMPTY);
                menu.clicked(
                    0,
                    0,
                    net.minecraft.world.inventory.ContainerInput.PICKUP,
                    player
                );
                check(
                    menu
                        .getCarried()
                        .is(net.minecraft.world.item.Items.STICK) &&
                        menu.getCarried().getCount() == 4 &&
                        menu.getResultSlot().hasItem(),
                    "Normal result click crafts one batch then refills"
                );
                var savedInventory = new java.util.ArrayList<ItemStack>();
                for (int i = 0; i < 36; i++) {
                    savedInventory.add(player.getInventory().getItem(i).copy());
                    if (player.getInventory().getItem(i).isEmpty()) player
                        .getInventory()
                        .setItem(
                            i,
                            new ItemStack(
                                net.minecraft.world.item.Items.DIRT,
                                64
                            )
                        );
                }
                menu.clicked(
                    0,
                    0,
                    net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,
                    player
                );
                check(
                    menu.getResultSlot().hasItem() &&
                        registry
                            .find(cabinId)
                            .orElseThrow()
                            .upgrades()
                            .storage()
                            .entries()
                            .getFirst()
                            .getCount() == 60,
                    "Full inventory must consume no ingredients"
                );
                for (int i = 0; i < 36; i++) player
                    .getInventory()
                    .setItem(i, savedInventory.get(i));
                menu.clicked(
                    0,
                    0,
                    net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,
                    player
                );
                int sticks = 0;
                for (int i = 0; i < 36; i++) if (
                    player
                        .getInventory()
                        .getItem(i)
                        .is(net.minecraft.world.item.Items.STICK)
                ) sticks += player.getInventory().getItem(i).getCount();
                check(sticks == 64, "Shift-click stops after one output stack");
                check(
                    player
                        .getInventory()
                        .getItem(0)
                        .has(
                            net.minecraft.core.component.DataComponents.CUSTOM_NAME
                        ),
                    "Named ingredients stay protected"
                );
            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                client ->
                    client.gui.screen() instanceof
                        net.minecraft.client.gui.screens.inventory.CraftingScreen
            );
            capture(context, "crafting-room-table-refilled");
            for (var block : List.of(
                net.minecraft.world.level.block.Blocks.LOOM,
                net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE,
                net.minecraft.world.level.block.Blocks.STONECUTTER,
                net.minecraft.world.level.block.Blocks.SMITHING_TABLE
            )) {
                world.getServer().runOnServer(server -> {
                    var player = world.getConnection().getServerPlayer();
                    player.closeContainer();
                    var cabin = CabinRegistry.get(server)
                        .find(cabinId)
                        .orElseThrow();
                    check(
                        cabin.upgrades().storage().sessions().isEmpty(),
                        "Closing must settle ingredient custody"
                    );
                    player
                        .getInventory()
                        .setItem(
                            2,
                            new ItemStack(
                                BuiltInRegistries.ITEM.getOptional(
                                    net.minecraft.resources.Identifier.withDefaultNamespace(
                                        "white_banner"
                                    )
                                ).orElseThrow()
                            )
                        );
                    player
                        .getInventory()
                        .setItem(
                            3,
                            new ItemStack(
                                BuiltInRegistries.ITEM.getOptional(
                                    net.minecraft.resources.Identifier.withDefaultNamespace(
                                        "red_dye"
                                    )
                                ).orElseThrow(),
                                8
                            )
                        );
                    player
                        .getInventory()
                        .setItem(
                            4,
                            net.minecraft.world.item.MapItem.create(
                                player.level(),
                                0,
                                0,
                                (byte) 0,
                                true,
                                false
                            )
                        );
                    player
                        .getInventory()
                        .setItem(
                            5,
                            new ItemStack(
                                net.minecraft.world.item.Items.PAPER,
                                8
                            )
                        );
                    player
                        .getInventory()
                        .setItem(
                            6,
                            CabinCraftingGameTest.named(
                                net.minecraft.world.item.Items.DIAMOND_PICKAXE
                            )
                        );
                    player
                        .getInventory()
                        .setItem(
                            7,
                            new ItemStack(
                                net.minecraft.world.item.Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE
                            )
                        );
                    player
                        .getInventory()
                        .setItem(
                            8,
                            new ItemStack(
                                net.minecraft.world.item.Items.NETHERITE_INGOT
                            )
                        );
                    player
                        .getInventory()
                        .setItem(
                            9,
                            new ItemStack(
                                net.minecraft.world.item.Items.STONE,
                                16
                            )
                        );
                    var position = CabinCrafting.stations(cabin, 3)
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().is(block))
                        .findFirst()
                        .orElseThrow()
                        .getKey();
                    CabinStation.open(player, cabin, position);
                });
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(3);
                context.runOnClient(client ->
                    client.gui.toastManager().clear()
                );
                capture(
                    context,
                    "crafting-room-" +
                        BuiltInRegistries.BLOCK.getKey(block).getPath()
                );
                context.runOnClient(client -> {
                    var entries = CabinStationClient.entries(
                        client.player.containerMenu.containerId
                    );
                    int index = -1;
                    for (int i = 0; i < entries.size(); i++) if (
                        entries.get(i).available()
                    ) {
                        index = i;
                        break;
                    }
                    check(
                        index >= 0,
                        "Station panel must offer a fillable operation"
                    );
                    client.gameMode.handleInventoryButtonClick(
                        client.player.containerMenu.containerId,
                        10000 + index
                    );
                });
                world.getConnection().waitForServerboundPackets();
                world.getConnection().waitForClientboundPackets();
                world.getServer().runOnServer(server -> {
                    var player = world.getConnection().getServerPlayer();
                    var menu = player.containerMenu;
                    int result =
                        block == net.minecraft.world.level.block.Blocks.LOOM ||
                        block ==
                            net.minecraft.world.level.block.Blocks.SMITHING_TABLE
                            ? 3
                            : block ==
                                net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE
                              ? 2
                              : 1;
                    check(
                        menu.getSlot(result).hasItem(),
                        "Operation selection must fill inputs and expose a vanilla result"
                    );
                    if (
                        block ==
                        net.minecraft.world.level.block.Blocks.SMITHING_TABLE
                    ) check(
                        menu
                            .getSlot(1)
                            .getItem()
                            .has(
                                net.minecraft.core.component.DataComponents.CUSTOM_NAME
                            ),
                        "Explicit selection must allow a named smithing target"
                    );
                    menu.clicked(
                        result,
                        0,
                        net.minecraft.world.inventory.ContainerInput.PICKUP,
                        player
                    );
                    check(
                        !menu.getCarried().isEmpty(),
                        "Result click must craft the selected operation"
                    );
                    if (
                        block ==
                        net.minecraft.world.level.block.Blocks.SMITHING_TABLE
                    ) check(
                        menu
                            .getCarried()
                            .is(
                                net.minecraft.world.item.Items.NETHERITE_PICKAXE
                            ) &&
                            menu
                                .getCarried()
                                .has(
                                    net.minecraft.core.component.DataComponents.CUSTOM_NAME
                                ),
                        "Smithing must preserve the chosen named target"
                    );
                });
                world.getConnection().waitForClientboundPackets();
                context.runOnClient(client ->
                    client.gui.toastManager().clear()
                );
                capture(
                    context,
                    "crafting-room-" +
                        BuiltInRegistries.BLOCK.getKey(block).getPath() +
                        "-selected"
                );
            }
            context.getInput().resizeWindow(960, 720);
            context.waitTicks(3);
            capture(context, "crafting-room-smithing-narrow");
            context.getInput().resizeWindow(1280, 800);
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var registry = CabinRegistry.get(server);
                var cabin = registry.find(cabinId).orElseThrow();
                registry.beginPacking(cabinId, cabin.owner());
                check(
                    !CabinStation.beforeClick(player.containerMenu, player),
                    "Packing must immediately block station actions"
                );
                player.closeContainer();
                registry.abortPacking(cabinId);
                cabin = registry.find(cabinId).orElseThrow();
                var crafting = CabinCrafting.stations(cabin, 3)
                    .entrySet()
                    .stream()
                    .filter(e ->
                        e
                            .getValue()
                            .is(
                                net.minecraft.world.level.block.Blocks.CRAFTING_TABLE
                            )
                    )
                    .findFirst()
                    .orElseThrow()
                    .getKey();
                CabinStation.open(player, cabin, crafting);
                player.setPos(
                    PocketDimension.cellCenter(cabin.cellIndex()).getX() + .5,
                    PocketDimension.cellCenter(cabin.cellIndex()).getY() + 1,
                    PocketDimension.cellCenter(cabin.cellIndex()).getZ() + .5
                );
                check(
                    !CabinStation.beforeClick(player.containerMenu, player),
                    "Leaving the room must immediately block station actions"
                );
                player.closeContainer();
            });
        }
    }

    private static void captureStorage(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            UUID cabinId = createCabin(world, false);
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var registry = CabinRegistry.get(server);
                var cabin = registry.find(cabinId).orElseThrow();
                player
                    .getInventory()
                    .setItem(0, new ItemStack(CabinItems.STORAGE_BOOK, 2));
                CabinUpgrades.useController(player, cabin);
                var upgrades = (CabinUpgradeMenu) player.containerMenu;
                check(
                    previewBook(upgrades, player, 0),
                    "Book installation must show a preview"
                );
                check(
                    !registry
                        .find(cabinId)
                        .orElseThrow()
                        .upgrades()
                        .storage()
                        .revealed() &&
                        upgrades.bookStack().getCount() == 2,
                    "Preview must not consume the book or reveal upgrades"
                );
                check(
                    upgrades.clickMenuButton(
                        player,
                        CabinUpgradeMenu.BUTTON_INSTALL_BOOK
                    ),
                    "Owner confirmation must install the book"
                );
                check(
                    registry
                        .find(cabinId)
                        .orElseThrow()
                        .upgrades()
                        .storage()
                        .revealed() &&
                        upgrades.bookStack().getCount() == 1,
                    "Installation must consume exactly one book"
                );
                check(
                    !upgrades.clickMenuButton(
                        player,
                        CabinUpgradeMenu.BUTTON_INSTALL_BOOK
                    ) &&
                        upgrades.bookStack().getCount() == 1,
                    "Repeated book installation must make no change"
                );
                upgrades.clickMenuButton(
                    player,
                    CabinUpgradeMenu.BUTTON_GROUP_BASE + 1
                );
            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                client ->
                    client.player.containerMenu instanceof
                        CabinUpgradeMenu menu &&
                    menu.selectedGroupIndex() == 1
            );
            capture(context, "cabin-storage-upgrade-installation");
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var registry = CabinRegistry.get(server);
                var cabin = registry.find(cabinId).orElseThrow();
                var storage = CabinStorageState.EMPTY.reveal()
                    .upgrade(1)
                    .upgrade(2);
                for (var item : BuiltInRegistries.ITEM) {
                    if (item == net.minecraft.world.item.Items.AIR) continue;
                    storage = storage
                        .deposit(
                            new ItemStack(
                                item,
                                Math.min(64, item.getDefaultMaxStackSize())
                            ),
                            64
                        )
                        .state();
                    if (storage.used() >= 70) break;
                }
                storage = storage
                    .deposit(
                        new ItemStack(
                            net.minecraft.world.item.Items.DIAMOND,
                            64
                        ),
                        64
                    )
                    .state();
                storage = storage
                    .deposit(
                        new ItemStack(
                            net.minecraft.world.item.Items.DIAMOND,
                            64
                        ),
                        64
                    )
                    .state();
                ItemStack named = new ItemStack(
                    net.minecraft.world.item.Items.DIAMOND,
                    3
                );
                named.set(
                    net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    net.minecraft.network.chat.Component.literal("Family gems")
                );
                storage = storage.deposit(named, 3).state();
                registry.updateUpgradeState(
                    cabinId,
                    cabin.upgrades().withStorage(storage)
                );
                cabin = registry.find(cabinId).orElseThrow();
                CabinStorage.placeControl(
                    player.level(),
                    cabin,
                    cabin.progression().generalSize()
                );
                var control = CabinStorage.control(cabin);
                player.teleportTo(
                    player.level(),
                    control.getX() + .5,
                    control.getY(),
                    control.getZ() + 1.5,
                    Set.of(),
                    180,
                    0,
                    false
                );
                CabinStorage.open(player, cabin);
            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                client ->
                    client.gui.screen() instanceof CabinStorageScreen &&
                    client.player.containerMenu instanceof
                        CabinStorageMenu menu &&
                    menu.capacity() == 108 &&
                    menu.entryCount() > 45
            );
            capture(context, "cabin-storage-search");
            context.runOnClient(client ->
                ((CabinStorageScreen) client.gui.screen()).mouseScrolled(
                    0,
                    0,
                    0,
                    -3
                )
            );
            capture(context, "cabin-storage-scrolled");
            context.runOnClient(client ->
                ((CabinStorageScreen) client.gui.screen()).select(1)
            );
            capture(context, "cabin-storage-category");
            context.runOnClient(client ->
                ((CabinStorageScreen) client.gui.screen()).select(
                    net.minecraft.world.item.CreativeModeTabs.tabs()
                        .stream()
                        .filter(
                            tab ->
                                tab.getType() ==
                                net.minecraft.world.item.CreativeModeTab.Type.CATEGORY
                        )
                        .toList()
                        .size() + 2
                )
            );
            world.getConnection().waitForServerboundPackets();
            capture(context, "cabin-storage-inventory");
            context.runOnClient(client ->
                ((CabinStorageScreen) client.gui.screen()).searchFor("diamond")
            );
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
                for (int i = 0; i < menu.entryCount(); i++) if (
                    menu.slots
                        .get(i)
                        .getItem()
                        .is(net.minecraft.world.item.Items.DIAMOND) &&
                    menu.quantity(i) == 128
                ) entry = i;
                check(
                    entry >= 0,
                    "The browser must synchronize aggregate quantities beyond a normal stack"
                );
                client.gameMode.handleContainerInput(
                    menu.containerId,
                    entry,
                    1,
                    net.minecraft.world.inventory.ContainerInput.PICKUP,
                    client.player
                );
            });
            world.getConnection().waitForServerboundPackets();
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                client ->
                    client.player.containerMenu.getCarried().getCount() == 1
            );
            world.getServer().runOnServer(server -> {
                var menu = world
                    .getConnection()
                    .getServerPlayer()
                    .containerMenu;
                check(
                    menu
                        .getCarried()
                        .is(net.minecraft.world.item.Items.DIAMOND),
                    "Client clicks must withdraw the selected server stack"
                );
            });
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.closeContainer();
                var registry = CabinRegistry.get(server);
                var cabin = registry.find(cabinId).orElseThrow();
                var storage = cabin.upgrades().storage();
                while (storage.level() < 6)
                    storage = storage.upgrade(storage.level() + 1);
                registry.updateUpgradeState(
                    cabinId,
                    cabin.upgrades().withStorage(storage)
                );
                var controller = PocketDimension.interiorController(
                    cabin.cellIndex(),
                    cabin.progression().generalSize()
                );
                player.teleportTo(
                    player.level(),
                    controller.getX() + .5,
                    controller.getY(),
                    controller.getZ() - 1.5,
                    Set.of(),
                    0,
                    0,
                    false
                );
                CabinUpgrades.useController(
                    player,
                    registry.find(cabinId).orElseThrow()
                );
                ((CabinUpgradeMenu) player.containerMenu).clickMenuButton(
                    player,
                    CabinUpgradeMenu.BUTTON_GROUP_BASE + 1
                );
            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(
                client ->
                    client.player.containerMenu instanceof
                        CabinUpgradeMenu menu &&
                    menu.selectedGroupIndex() == 1 &&
                    menu.isAtMaximum()
            );
            capture(context, "cabin-storage-fully-upgraded");
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                player.closeContainer();
                storageMenuMovesPartialStacksAndRecoversCursor(server, player);
                storageControlMovesThroughExpansionWithoutTakingChestItems(
                    server
                );
            });
        }
    }

    private static void storageMenuMovesPartialStacksAndRecoversCursor(
        net.minecraft.server.MinecraftServer server,
        net.minecraft.server.level.ServerPlayer player
    ) {
        var registry = CabinRegistry.get(server);
        UUID owner = UUID.randomUUID();
        var cabin = deployRegistryCabin(registry, owner, 7000);
        registry.trust(cabin.uuid(), owner, player.getUUID());
        registry.setEntryPermission(
            cabin.uuid(),
            owner,
            CabinEntryPermission.TRUSTED_PLAYERS
        );
        cabin = registry.find(cabin.uuid()).orElseThrow();
        var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
        PocketDimension.ensureCabinInterior(
            pocket,
            cabin.cellIndex(),
            cabin.palette(),
            cabin.progression().generalSize()
        );
        registry.updateUpgradeState(
            cabin.uuid(),
            cabin
                .upgrades()
                .withStorage(CabinStorageState.EMPTY.reveal().upgrade(1))
        );
        cabin = registry.find(cabin.uuid()).orElseThrow();
        CabinStorage.placeControl(
            pocket,
            cabin,
            cabin.progression().generalSize()
        );
        var control = CabinStorage.control(cabin);
        player.teleportTo(
            pocket,
            control.getX() + .5,
            control.getY(),
            control.getZ() + 1.5,
            Set.of(),
            180,
            0,
            false
        );
        for (
            int i = 0;
            i < player.getInventory().getContainerSize();
            i++
        ) player.getInventory().setItem(i, ItemStack.EMPTY);
        var menu = new CabinStorageMenu(9, player.getInventory(), cabin.uuid());
        player
            .getInventory()
            .setItem(
                0,
                new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 40)
            );
        menu.quickMoveStack(player, CabinStorageMenu.ENTRY_SLOTS + 27);
        check(
            player.getInventory().getItem(0).isEmpty(),
            "Shift-click must deposit the source stack"
        );
        menu.setCarried(
            new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 30)
        );
        menu.clicked(
            0,
            0,
            net.minecraft.world.inventory.ContainerInput.PICKUP,
            player
        );
        check(
            menu.getCarried().isEmpty() &&
                registry
                    .find(cabin.uuid())
                    .orElseThrow()
                    .upgrades()
                    .storage()
                    .used() == 2,
            "Cursor deposits must merge stacks at normal limits"
        );
        menu.clicked(
            0,
            1,
            net.minecraft.world.inventory.ContainerInput.PICKUP,
            player
        );
        check(
            menu.getCarried().getCount() == 1,
            "Right-click must withdraw one item"
        );
        menu.clicked(
            0,
            1,
            net.minecraft.world.inventory.ContainerInput.PICKUP,
            player
        );
        menu.clicked(
            0,
            0,
            net.minecraft.world.inventory.ContainerInput.PICKUP,
            player
        );
        check(
            menu.getCarried().getCount() == 64,
            "Left-click must withdraw at most one normal stack"
        );
        var ops = server
            .registryAccess()
            .createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var restored = CabinRegistry.CODEC.parse(
            ops,
            CabinRegistry.CODEC.encodeStart(ops, registry).getOrThrow()
        ).getOrThrow();
        var session = restored
            .find(cabin.uuid())
            .orElseThrow()
            .upgrades()
            .storage()
            .sessions()
            .getFirst();
        check(
            session.cursor().getCount() == 64,
            "A restart must retain custody of the cursor"
        );
        var beforeRemoteChange = registry
            .find(cabin.uuid())
            .orElseThrow()
            .upgrades()
            .storage();
        var remotelyChanged = beforeRemoteChange
            .withdraw(
                new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT),
                6
            )
            .state()
            .deposit(
                new ItemStack(net.minecraft.world.item.Items.EMERALD, 3),
                3
            )
            .state();
        var upgrades = registry.find(cabin.uuid()).orElseThrow().upgrades();
        registry.updateUpgradeState(
            cabin.uuid(),
            upgrades.withStorage(remotelyChanged)
        );
        menu.broadcastChanges();
        menu.clicked(
            0,
            0,
            net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,
            player
        );
        check(
            player.getInventory().getItem(0).isEmpty(),
            "A stale entry click must not withdraw a different item"
        );
        upgrades = registry.find(cabin.uuid()).orElseThrow().upgrades();
        registry.updateUpgradeState(
            cabin.uuid(),
            upgrades.withStorage(beforeRemoteChange)
        );
        menu.broadcastChanges();
        registry.untrust(cabin.uuid(), owner, player.getUUID());
        menu.clicked(
            0,
            1,
            net.minecraft.world.inventory.ContainerInput.PICKUP,
            player
        );
        check(
            menu.getCarried().getCount() == 64 && !menu.stillValid(player),
            "Access removal must stop transfers"
        );
        menu.removed(player);
        check(
            player.getInventory().getItem(0).getCount() == 64,
            "Closing must return the recoverable cursor to player inventory"
        );
        CabinStorage.recover(player);
        check(
            player.getInventory().getItem(0).getCount() == 64 &&
                registry
                    .find(cabin.uuid())
                    .orElseThrow()
                    .upgrades()
                    .storage()
                    .sessions()
                    .isEmpty(),
            "Recovery must deliver cursor items once"
        );
        check(
            registry
                .find(cabin.uuid())
                .orElseThrow()
                .upgrades()
                .storage()
                .entries()
                .getFirst()
                .getCount() == 6,
            "Withdrawing must retain the exact storage remainder"
        );
        for (int i = 0; i < 36; i++) player
            .getInventory()
            .setItem(
                i,
                new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 64)
            );
        var receipt = CabinStorage.receipt(
            player,
            new ItemStack(net.minecraft.world.item.Items.DIAMOND, 64)
        );
        var retained = registry.find(cabin.uuid()).orElseThrow().upgrades();
        registry.updateUpgradeState(
            cabin.uuid(),
            retained.withStorage(retained.storage().withSession(receipt))
        );
        boolean creative = player.getAbilities().instabuild;
        player.getAbilities().instabuild = true;
        CabinStorage.recover(player);
        var delivery = pocket.getEntity(receipt.operation());
        check(
            delivery instanceof
                net.minecraft.world.entity.item.ItemEntity item &&
                item.getItem().getCount() == 64,
            "A full inventory must recover cursor overflow without discarding it in Creative mode"
        );
        retained = registry.find(cabin.uuid()).orElseThrow().upgrades();
        registry.updateUpgradeState(
            cabin.uuid(),
            retained.withStorage(retained.storage().withSession(receipt))
        );
        var pendingDrop = (net.minecraft.world.entity.item.ItemEntity) delivery;
        pendingDrop.addTag(PortablePocketCabin.MOD_ID + ".storage_delivery");
        pendingDrop.setNoPickUpDelay();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(
            pendingDrop,
            pocket
        );
        pendingDrop.playerTouch(player);
        check(
            pendingDrop.isAlive() && pendingDrop.hasPickUpDelay(),
            "An uncommitted delivery must remain uncollectable after reload"
        );
        pendingDrop.setPos(
            pendingDrop.getX() + 5,
            pendingDrop.getY(),
            pendingDrop.getZ()
        );
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(
            server
        );
        check(
            pendingDrop.getX() == receipt.delivery().position().getX() + .5,
            "An uncommitted delivery must stay at its saved position"
        );
        CabinStorage.recover(player);
        check(
            pocket.getEntity(receipt.operation()) == delivery,
            "Interrupted overflow delivery must reuse its existing item entity"
        );
        player.getAbilities().instabuild = creative;
    }

    private static void storageControlMovesThroughExpansionWithoutTakingChestItems(
        net.minecraft.server.MinecraftServer server
    ) {
        var registry = CabinRegistry.get(server);
        var cabin = deployRegistryCabin(registry, UUID.randomUUID(), 8000);
        var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
        PocketDimension.ensureCabinInterior(
            pocket,
            cabin.cellIndex(),
            cabin.palette(),
            cabin.progression().generalSize()
        );
        var storage = CabinStorageState.EMPTY.reveal()
            .upgrade(1)
            .deposit(
                new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3),
                3
            )
            .state();
        registry.updateUpgradeState(
            cabin.uuid(),
            cabin.upgrades().withStorage(storage)
        );
        cabin = registry.find(cabin.uuid()).orElseThrow();
        CabinStorage.placeControl(
            pocket,
            cabin,
            cabin.progression().generalSize()
        );
        var oldControl = CabinStorage.control(cabin);
        var chestPosition = PocketDimension.cellCenter(
            cabin.cellIndex()
        ).above();
        pocket.setBlockAndUpdate(
            chestPosition,
            net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState()
        );
        var chest =
            (net.minecraft.world.level.block.entity.ChestBlockEntity) pocket.getBlockEntity(
                chestPosition
            );
        chest.setItem(
            0,
            new ItemStack(net.minecraft.world.item.Items.EMERALD, 7)
        );
        var effect = new CabinUpgradeEffect(server, pocket);
        effect.apply(
            cabin,
            CabinUpgradeState.Installation.generalSpace(
                UUID.randomUUID(),
                CabinUpgradeState.Target.generalSpace(5),
                3
            )
        );
        registry.expandGeneralSpace(cabin.uuid(), cabin.owner(), 3, 5, 21);
        cabin = registry.find(cabin.uuid()).orElseThrow();
        check(
            pocket.getBlockState(oldControl).isAir() &&
                pocket
                    .getBlockState(CabinStorage.control(cabin))
                    .is(
                        net.minecraft.world.level.block.Blocks.CHISELED_BOOKSHELF
                    ) &&
                CabinProtection.isProtected(
                    pocket,
                    CabinStorage.control(cabin)
                ),
            "Expansion must move the protected storage control"
        );
        check(
            chest.getItem(0).getCount() == 7 &&
                cabin.upgrades().storage().entries().getFirst().getCount() == 3,
            "Storage must never inspect or move placed inventories"
        );
        var ops = server
            .registryAccess()
            .createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        registry.beginPacking(cabin.uuid(), cabin.owner());
        registry.finishPacking(cabin.uuid());
        var restored = CabinRegistry.CODEC.parse(
            ops,
            CabinRegistry.CODEC.encodeStart(ops, registry).getOrThrow()
        ).getOrThrow();
        check(
            restored
                .find(cabin.uuid())
                .orElseThrow()
                .upgrades()
                .storage()
                .entries()
                .getFirst()
                .getCount() == 3,
            "Packing and restart must preserve storage"
        );
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static CabinRecord deployRegistryCabin(
        CabinRegistry registry,
        UUID owner,
        int x
    ) {
        var cabin = registry.create(owner);
        registry.beginDeployment(
            cabin.uuid(),
            owner,
            new CabinExterior(
                Level.OVERWORLD,
                new BlockPos(x, 100, 0),
                Direction.NORTH
            )
        );
        registry.markInteriorGenerated(cabin.uuid());
        return registry.finishDeployment(cabin.uuid());
    }

    private static void captureExterior(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            BlockPos anchor = world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                var ground = new BlockPos(
                    0,
                    level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        0,
                        0
                    ),
                    0
                );
                var exterior = new CabinExterior(
                    Level.OVERWORLD,
                    ground,
                    Direction.NORTH
                );
                if (!ExteriorCabin.validate(level, exterior).valid()) {
                    throw new AssertionError(
                        "The exterior capture fixture needs clear, level ground"
                    );
                }
                var palette = new CabinPalette(
                    CabinPalette.DEFAULT.floor(),
                    CabinPalette.DEFAULT.walls(),
                    CabinMaterialProfiles.matchPlanks(
                        new ItemStack(
                            net.minecraft.world.item.Items.SPRUCE_PLANKS
                        )
                    ).orElseThrow(),
                    CabinPalette.DEFAULT.door()
                );
                ExteriorCabin.place(level, exterior, palette);
                server
                    .getCommands()
                    .performPrefixedCommand(
                        server.createCommandSourceStack(),
                        "time set noon"
                    );
                server
                    .getCommands()
                    .performPrefixedCommand(
                        server.createCommandSourceStack(),
                        "weather clear"
                    );
                world
                    .getConnection()
                    .getServerPlayer()
                    .setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
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
            captureExteriorSide(
                context,
                world,
                anchor,
                "front-left",
                -6.5,
                -4.5,
                -45
            );
        } finally {
            context.runOnClient(client -> {
                if (client.gui.hud.isHidden()) {
                    client.gui.hud.toggle();
                }
            });
        }
    }

    private static void captureExteriorSide(
        ClientGameTestContext context,
        TestSingleplayerContext world,
        BlockPos anchor,
        String side,
        double x,
        double z,
        float yaw
    ) {
        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            if (
                !player.teleportTo(
                    server.overworld(),
                    anchor.getX() + x,
                    anchor.getY() + 4,
                    anchor.getZ() + z,
                    Set.of(),
                    yaw,
                    15,
                    false
                )
            ) {
                throw new AssertionError(
                    "Could not position the exterior " + side + " camera"
                );
            }
        });
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
        capture(context, "cabin-exterior-" + side);
    }

    private static UUID createCabin(
        TestSingleplayerContext world,
        boolean resident
    ) {
        return world.getServer().computeOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            var registry = CabinRegistry.get(server);
            registry.resolveWorldAttunement(
                new WorldAttunement(99, PortablePocketCabin.id("missing/wood"))
            );
            UUID owner = resident
                ? UUID.fromString("00000000-0000-0000-0000-000000000001")
                : player.getUUID();
            var palette = resident
                ? CabinPalette.DEFAULT
                : new CabinPalette(
                      CabinMaterialProfiles.woodProfile(
                          PortablePocketCabin.id("vanilla/wood/spruce")
                      )
                          .orElseThrow()
                          .selection(),
                      CabinMaterialProfiles.woodProfile(
                          PortablePocketCabin.id("vanilla/wood/birch")
                      )
                          .orElseThrow()
                          .selection(),
                      CabinPalette.DEFAULT.roof(),
                      CabinPalette.DEFAULT.door()
                  );
            var cabin = registry.create(owner, palette);
            if (resident) {
                registry.trust(cabin.uuid(), owner, player.getUUID());
                registry.setEntryPermission(
                    cabin.uuid(),
                    owner,
                    CabinEntryPermission.TRUSTED_PLAYERS
                );
            }
            registry.beginDeployment(
                cabin.uuid(),
                owner,
                new CabinExterior(
                    Level.OVERWORLD,
                    new BlockPos(0, 100, 0),
                    Direction.NORTH
                )
            );
            var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
            if (pocket == null) {
                throw new AssertionError("Pocket dimension did not load");
            }
            PocketDimension.ensureCabinInterior(
                pocket,
                cabin.cellIndex(),
                palette,
                cabin.progression().generalSize()
            );
            registry.markInteriorGenerated(cabin.uuid());
            registry.finishDeployment(cabin.uuid());
            BlockPos entrance = PocketDimension.interiorEntrance(
                cabin.cellIndex()
            );
            if (
                !player.teleportTo(
                    pocket,
                    entrance.getX() + 0.5,
                    entrance.getY(),
                    entrance.getZ() + 0.5,
                    Set.of(),
                    180,
                    0,
                    false
                )
            ) {
                throw new AssertionError("Could not enter the test cabin");
            }
            return cabin.uuid();
        });
    }

    private static void selectNext(
        ClientGameTestContext context,
        TestSingleplayerContext world
    ) {
        int previous = context.computeOnClient(client ->
            (
                (CabinUpgradeMenu) client.player.containerMenu
            ).selectedPanelIndex()
        );
        context.runOnClient(client ->
            client.gameMode.handleInventoryButtonClick(
                client.player.containerMenu.containerId,
                CabinUpgradeMenu.BUTTON_NEXT_PANEL
            )
        );
        world.getConnection().waitForServerboundPackets();
        context.waitFor(
            client ->
                (
                    (CabinUpgradeMenu) client.player.containerMenu
                ).selectedPanelIndex() != previous
        );
        world.getConnection().waitForClientboundPackets();
        context.waitTick();
    }

    private static void open(
        ClientGameTestContext context,
        TestSingleplayerContext world,
        UUID cabinId
    ) {
        world.getServer().runOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            player.closeContainer();
            CabinUpgrades.useController(
                player,
                CabinRegistry.get(server).find(cabinId).orElseThrow()
            );
        });
        world.getConnection().waitForClientboundPackets();
        context.waitForScreen(CabinUpgradeScreen.class);
        context.waitTick();
        world.getConnection().waitForClientboundPackets();
    }

    private static void fund(
        TestSingleplayerContext world,
        UUID cabinId,
        boolean complete
    ) {
        world.getServer().runOnServer(server -> {
            var registry = CabinRegistry.get(server);
            var cabin = registry.find(cabinId).orElseThrow();
            var offer = CabinUpgradeCatalog.next(
                cabin,
                registry.worldAttunement().orElseThrow(),
                CabinUpgradeDefinitions.current()
            ).orElseThrow();
            List<ItemStack> stacks = complete
                ? offer
                      .requirements()
                      .stream()
                      .map(requirement ->
                          new ItemStack(
                              BuiltInRegistries.ITEM.getOptional(
                                  requirement.itemId()
                              ).orElseThrow(),
                              requirement.count()
                          )
                      )
                      .toList()
                : List.of(
                      new ItemStack(
                          BuiltInRegistries.ITEM.getOptional(
                              offer.requirements().getFirst().itemId()
                          ).orElseThrow(),
                          1
                      )
                  );
            registry.updateUpgradeState(
                cabinId,
                cabin
                    .upgrades()
                    .withFund(
                        new CabinUpgradeState.Fund(
                            offer.target(),
                            offer.requirements(),
                            stacks
                        )
                    )
            );
        });
    }

    private static void capture(ClientGameTestContext context, String name) {
        if (!name.endsWith("-tooltip")) {
            context.getInput().setCursorPos(0, 0);
        }
        context.waitTick();
        context.waitTick();
        context.takeScreenshot(
            TestScreenshotOptions.of(name).disableCounterPrefix()
        );
    }
}
