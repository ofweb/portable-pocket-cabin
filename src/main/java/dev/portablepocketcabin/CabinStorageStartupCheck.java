package dev.portablepocketcabin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.Set;

final class CabinStorageStartupCheck {
	private CabinStorageStartupCheck() { }
	static void run(net.minecraft.server.MinecraftServer server, CabinRecord cabin) throws Exception {
		var registry = CabinRegistry.get(server);
		var original = cabin.upgrades();
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
			new com.mojang.authlib.GameProfile(cabin.owner(), "storage-test"), false);
		var player = new ServerPlayer(server, server.overworld(), cookie.gameProfile(), cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		try {
			shiftWithdrawal(server, player, registry, cabin);
			player.getInventory().clearContent();
			bookCustody(server, player, registry, cabin);
			staleFill(server, player, registry, cabin);
			PortablePocketCabin.LOGGER.info("STORAGE_ACCEPTANCE_TESTS_PASSED");
		} finally {
			registry.updateUpgradeState(cabin.uuid(), original);
			server.getPlayerList().remove(player);
			channel.close();
		}
	}
	private static void check(boolean condition, String message) {
		if (!condition) throw new IllegalStateException(message);
	}
	private static void shiftWithdrawal(net.minecraft.server.MinecraftServer server, ServerPlayer player, CabinRegistry registry, CabinRecord cabin) throws Exception {
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);

		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(CabinStorageState.EMPTY.reveal().upgrade(1)
			.deposit(new ItemStack(Items.DIAMOND, 2), 2).state()));
		cabin = registry.find(cabin.uuid()).orElseThrow();
		CabinStorage.placeControl(pocket, cabin, cabin.progression().generalSize());
		var control = CabinStorage.control(cabin);
		player.teleportTo(pocket, control.getX() + .5, control.getY(), control.getZ() + 1.5, Set.of(), 180, 0, false);
		for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
		player.getInventory().setItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND, new ItemStack(Items.DIAMOND, 63));
		var menu = new CabinStorageMenu(51, player.getInventory(), cabin.uuid());
		menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
		check(player.getInventory().getItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND).getCount() == 64
			&& registry.find(cabin.uuid()).orElseThrow().upgrades().storage().entries().getFirst().getCount() == 1,
			"Shift withdrawal must fill matching offhand space and keep the remainder in storage");
		player.getInventory().setItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
		menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
		check(registry.find(cabin.uuid()).orElseThrow().upgrades().storage().entries().getFirst().getCount() == 1,
			"An empty offhand is not an automatic insertion destination");
		menu.removed(player);
	}

	private static void bookCustody(net.minecraft.server.MinecraftServer server, ServerPlayer player, CabinRegistry registry, CabinRecord cabin) throws Exception {
		registry.updateUpgradeState(cabin.uuid(), CabinUpgradeState.EMPTY);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);

		var control = PocketDimension.interiorController(cabin.cellIndex(), cabin.progression().generalSize());
		player.teleportTo(pocket, control.getX() + .5, control.getY(), control.getZ() - 1.5, Set.of(), 0, 0, false);
		var menu = new CabinUpgradeMenu(53, player.getInventory(), cabin.uuid());
		check(menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_BOOKS), "The owner must select Books");
		menu.setCarried(new ItemStack(CabinItems.STORAGE_BOOK, 2));
		menu.clicked(CabinUpgradeMenu.BOOK_SLOT, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
		menu.setCarried(new ItemStack(Items.DIAMOND, 7));
		check(menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_INSTALL_BOOK), "The owner must install one storage book");
		var receipt = registry.find(cabin.uuid()).orElseThrow().upgrades().storage().sessions().getFirst();
		check(receipt.cursor().is(Items.DIAMOND) && receipt.cursor().getCount() == 7,
			"Book installation custody must retain unrelated held items until they are saved");
		var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		var restored = CabinStorageState.Session.CODEC.parse(ops,
			CabinStorageState.Session.CODEC.encodeStart(ops, receipt).getOrThrow()).getOrThrow();
		var installed = registry.find(cabin.uuid()).orElseThrow();
		registry.updateUpgradeState(cabin.uuid(), installed.upgrades().withStorage(installed.upgrades().storage().withSession(restored)));
		menu.setCarried(ItemStack.EMPTY);
		player.getInventory().setItem(0, new ItemStack(CabinItems.STORAGE_BOOK, 2));
		CabinStorage.recover(player);
		CabinStorage.recover(player);
		check(player.getInventory().getItem(0).getCount() == 1
			&& player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum() == 7,
			"Restart recovery must consume one book and return held items exactly once");
	}

	private static void staleFill(net.minecraft.server.MinecraftServer server, ServerPlayer player, CabinRegistry registry, CabinRecord cabin) throws Exception {
		registry.updateUpgradeState(cabin.uuid(), CabinUpgradeState.EMPTY);
		cabin = registry.find(cabin.uuid()).orElseThrow();
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);

		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(CabinStorageState.EMPTY.reveal().upgrade(1)
			.deposit(new ItemStack(Items.AMETHYST_BLOCK, 4), 4).state()));
		var control = PocketDimension.interiorController(cabin.cellIndex(), cabin.progression().generalSize());
		player.teleportTo(pocket, control.getX() + .5, control.getY(), control.getZ() - 1.5, Set.of(), 0, 0, false);
		var menu = new CabinUpgradeMenu(54, player.getInventory(), cabin.uuid());
		int displayedFill = menu.fillStorageButton();
		var field = CabinUpgradeDefinitions.class.getDeclaredField("current");
		field.setAccessible(true);
		var original = CabinUpgradeDefinitions.current();
		var expansions = new ArrayList<>(original.expansions());
		int expansionIndex = 0;
		while (expansions.get(expansionIndex).targetSize() <= cabin.progression().generalSize()) expansionIndex++;
		var first = expansions.get(expansionIndex);
		var ingredients = new ArrayList<>(first.ingredients());
		var ingredient = ingredients.getFirst();
		ingredients.set(0, new CabinUpgradeDefinitions.Ingredient(ingredient.itemId(), ingredient.palettePlanks(), ingredient.count() + 1));
		expansions.set(expansionIndex, new CabinUpgradeDefinitions.Expansion(first.targetSize(), ingredients));
		try {
			field.set(null, new CabinUpgradeDefinitions.Definitions(original.definitionVersion(), original.maximumGeneralSize(),
				original.woodPool(), expansions, original.windowBase(), original.windowTiers()));
			menu.broadcastChanges();
			check(!menu.clickMenuButton(player, displayedFill)
				&& registry.find(cabin.uuid()).orElseThrow().upgrades().funds().isEmpty()
				&& registry.find(cabin.uuid()).orElseThrow().upgrades().storage().stacks().getFirst().getCount() == 4,
				"A stale Fill packet must not fund the refreshed cost");
			check(menu.clickMenuButton(player, menu.fillStorageButton()), "A new Fill action must use the refreshed cost");
			int beforeNavigation = menu.fillStorageButton();
			menu.clickMenuButton(player, CabinUpgradeMenu.BUTTON_GROUP_BASE + 1);
			check(!menu.clickMenuButton(player, beforeNavigation), "Changing the target must invalidate an in-flight Fill action");
		} finally { field.set(null, original); }
	}

}
