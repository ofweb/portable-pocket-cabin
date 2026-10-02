package dev.portablepocketcabin;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.List;
import java.util.UUID;

final class CabinStorageMenu extends AbstractContainerMenu {
	static final int ENTRY_SLOTS = 1728;
	static final MenuType<CabinStorageMenu> TYPE = Registry.register(BuiltInRegistries.MENU,
		PortablePocketCabin.id("central_storage"), new ExtendedMenuType<>(CabinStorageMenu::new, UUIDUtil.STREAM_CODEC));
	private final UUID cabinId;
	private final ServerPlayer serverPlayer;
	private final Inventory inventory;
	private final SimpleContainer display = new SimpleContainer(ENTRY_SLOTS);
	private final SimpleContainerData data = new SimpleContainerData(3 + ENTRY_SLOTS * 2);
	private final java.util.ArrayList<ItemStack> identities = new java.util.ArrayList<>();
	private boolean exhausted;
	boolean inventoryTab;

	CabinStorageMenu(int id, Inventory inventory, UUID cabinId) {
		super(TYPE, id);
		this.cabinId = cabinId;
		this.inventory = inventory;
		serverPlayer = inventory.player instanceof ServerPlayer player ? player : null;
		for (int i = 0; i < ENTRY_SLOTS; i++) addSlot(new EntrySlot(i));
		for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
			addSlot(new Slot(inventory, col + row * 9 + 9, 9 + col * 18, 68 + row * 18) {
				@Override public boolean isActive() { return inventoryTab; }
			});
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 9 + col * 18, 126));
		for (int i = 0; i < 5; i++) {
			var original = inventory.player.inventoryMenu.slots.get(i == 4 ? 45 : 5 + i);
			int x = i == 4 ? 117 : 9 + i * 18;
			addSlot(new Slot(original.container, original.getContainerSlot(), x, 40) {
				@Override public boolean isActive() { return inventoryTab; }
				@Override public boolean mayPlace(ItemStack stack) { return original.mayPlace(stack); }
				@Override public boolean mayPickup(Player player) { return original.mayPickup(player); }
				@Override public int getMaxStackSize() { return original.getMaxStackSize(); }
				@Override public net.minecraft.resources.Identifier getNoItemIcon() { return original.getNoItemIcon(); }
				@Override public void setByPlayer(ItemStack stack, ItemStack previous) { original.setByPlayer(stack, previous); }
			});
		}
		addDataSlots(data);
		if (serverPlayer != null) refresh();
	}
	static void register() { }
	int entryCount() { return data.get(0); }
	int quantity(int entry) { return (data.get(3 + entry * 2) & 0xffff) | (data.get(4 + entry * 2) << 16); }
	int used() { return data.get(1); }
	int capacity() { return data.get(2); }

	@Override public boolean clickMenuButton(Player player, int button) {
		if (!stillValid(player) || button < 0 || button > 1) return false;
		inventoryTab = button == 1;
		return true;
	}
	@Override public boolean stillValid(Player player) {
		if (serverPlayer == null) return true;
		CabinRecord cabin = CabinRegistry.get(serverPlayer.level().getServer()).find(cabinId).orElse(null);
		if (exhausted || player != serverPlayer || !player.level().dimension().equals(PocketDimension.LEVEL_KEY)
			|| !CabinStorage.mayUse(cabin, player.getUUID()) || cabin.upgrades().storage().level() == 0
			|| !PocketDimension.isWithinUsable(cabin.cellIndex(), cabin.progression().generalSize(), player.blockPosition())) return false;
		var control = CabinStorage.control(cabin);
		return player.distanceToSqr(control.getX() + .5, control.getY() + .5, control.getZ() + .5) <= 64
			&& player.level().getBlockState(control).is(Blocks.CHISELED_BOOKSHELF);
	}

	@Override public void clicked(int slot, int button, ContainerInput input, Player player) {
		if (serverPlayer == null || !stillValid(player) || input == ContainerInput.THROW
			|| slot < 0 && input == ContainerInput.PICKUP) return;
		if (slot >= 0 && slot < ENTRY_SLOTS) {
			if (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_MOVE) return;
			ItemStack held = getCarried();
			if (!held.isEmpty() && input == ContainerInput.PICKUP) {
				deposit(held, button == 1 ? 1 : held.getCount());
			} else if ((held.isEmpty() || input == ContainerInput.QUICK_MOVE) && slot < entryCount()) {
				withdraw(display.getItem(slot).copy(), button == 1 ? 1 : display.getItem(slot).getMaxStackSize(),
					input == ContainerInput.QUICK_MOVE);
			}
		} else {
			super.clicked(slot, button, input, player);
		}
		checkpoint();
	}

	private void deposit(ItemStack source, int limit) {
		CabinRegistry registry = CabinRegistry.get(serverPlayer.level().getServer());
		synchronized (registry) {
			if (!stillValid(serverPlayer)) return;
			CabinRecord cabin = registry.find(cabinId).orElseThrow();
			if (cabin.upgrades().operationInProgress()) return;
			CabinStorageState.Transfer transfer = cabin.upgrades().storage().deposit(source, limit);
			if (transfer.moved().isEmpty()) return;
			registry.updateUpgradeState(cabinId, cabin.upgrades().withStorage(transfer.state()));
			source.shrink(transfer.moved().getCount());
			inventory.setChanged();
		}
		checkpoint();
		refresh();
	}

	private void withdraw(ItemStack selected, int limit, boolean toInventory) {
		CabinRegistry registry = CabinRegistry.get(serverPlayer.level().getServer());
		synchronized (registry) {
			if (!stillValid(serverPlayer)) return;
			CabinRecord cabin = registry.find(cabinId).orElseThrow();
			if (cabin.upgrades().operationInProgress()) return;
			int fits = toInventory ? inventorySpace(selected) : limit;
			var transfer = cabin.upgrades().storage().withdraw(selected, Math.min(limit, fits));
			if (transfer.moved().isEmpty()) return;
			registry.updateUpgradeState(cabinId, cabin.upgrades().withStorage(transfer.state()));
			if (toInventory) inventory.add(transfer.moved());
			else setCarried(transfer.moved());
		}
		checkpoint();
		refresh();
	}

	private int inventorySpace(ItemStack selected) {
		int count = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) count += selected.getMaxStackSize();
			else if (ItemStack.isSameItemSameComponents(stack, selected)) count += stack.getMaxStackSize() - stack.getCount();
		}
		return count;
	}
	@Override public ItemStack quickMoveStack(Player player, int slot) {
		if (serverPlayer == null || !stillValid(player) || slot < ENTRY_SLOTS || slot >= ENTRY_SLOTS + 41) return ItemStack.EMPTY;
		if (!slots.get(slot).mayPickup(player)) return ItemStack.EMPTY;
		ItemStack source = slots.get(slot).getItem();
		ItemStack original = source.copy();
		deposit(source, source.getCount());
		return source.getCount() == original.getCount() ? ItemStack.EMPTY : original;
	}
	@Override public void broadcastChanges() {
		if (serverPlayer != null) { checkpoint(); refresh(); }
		super.broadcastChanges();
	}
	private void checkpoint() {
		if (serverPlayer == null) return;
		var registry = CabinRegistry.get(serverPlayer.level().getServer());
		synchronized (registry) {
			var cabin = registry.find(cabinId).orElse(null);
			if (cabin == null) return;
			var items = new java.util.ArrayList<ItemStack>();
			for (int i = 0; i < inventory.getContainerSize(); i++) items.add(inventory.getItem(i).copy());
			var previous = cabin.upgrades().storage().sessions().stream()
				.filter(s -> s.player().equals(serverPlayer.getUUID())).findFirst().orElse(null);
			if (previous != null && ItemStack.matches(previous.cursor(), getCarried())
				&& sameItems(previous.inventory(), items)) return;
			registry.updateUpgradeState(cabinId, cabin.upgrades().withStorage(cabin.upgrades().storage().withSession(
				CabinStorage.receipt(serverPlayer, getCarried()))));
			CabinRegistry.flush(serverPlayer.level().getServer());
		}
	}
	private static boolean sameItems(List<ItemStack> first, List<ItemStack> second) {
		if (first.size() != second.size()) return false;
		for (int i = 0; i < first.size(); i++) if (!ItemStack.matches(first.get(i), second.get(i))) return false;
		return true;
	}
	@Override public void removed(Player player) {
		if (serverPlayer == null) return;
		checkpoint();
		setCarried(ItemStack.EMPTY);
		CabinStorage.recover(serverPlayer);
	}

	private void refresh() {
		CabinRecord cabin = CabinRegistry.get(serverPlayer.level().getServer()).find(cabinId).orElse(null);
		List<ItemStack> entries = CabinStorage.mayUse(cabin, serverPlayer.getUUID())
			? cabin.upgrades().storage().entries() : List.of();
		for (ItemStack entry : entries) {
			if (identities.stream().noneMatch(value -> ItemStack.isSameItemSameComponents(value, entry))) {
				if (identities.size() == ENTRY_SLOTS) { exhausted = true; break; }
				identities.add(entry.copyWithCount(1));
			}
		}
		for (int i = 0; i < identities.size(); i++) {
			ItemStack identity = identities.get(i);
			ItemStack entry = entries.stream().filter(value -> ItemStack.isSameItemSameComponents(value, identity))
				.findFirst().orElse(ItemStack.EMPTY);
			display.setItem(i, entry.isEmpty() ? ItemStack.EMPTY : entry.copyWithCount(1));
			data.set(3 + i * 2, entry.getCount() & 0xffff);
			data.set(4 + i * 2, entry.getCount() >>> 16);
		}
		data.set(0, identities.size());
		data.set(1, CabinStorage.mayUse(cabin, serverPlayer.getUUID()) ? cabin.upgrades().storage().used() : 0);
		data.set(2, CabinStorage.mayUse(cabin, serverPlayer.getUUID()) ? cabin.upgrades().storage().capacity() : 0);
	}
	private final class EntrySlot extends Slot {
		EntrySlot(int index) { super(display, index, -1000, -1000); }
		@Override public boolean mayPlace(ItemStack stack) { return false; }
		@Override public boolean mayPickup(Player player) { return false; }
		@Override public boolean isActive() { return false; }
		@Override public boolean isFake() { return true; }
	}
}
