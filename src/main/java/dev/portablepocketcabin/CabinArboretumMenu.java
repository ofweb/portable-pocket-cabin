package dev.portablepocketcabin;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/**
 * The planting board's screen (B-0023): teach the grove a tree type with saplings, and choose which taught types it
 * plants. Guests may look but not change anything.
 */
final class CabinArboretumMenu extends AbstractContainerMenu {
	static final MenuType<CabinArboretumMenu> TYPE = Registry.register(BuiltInRegistries.MENU,
		PortablePocketCabin.id("planting_board"), new ExtendedMenuType<>(CabinArboretumMenu::new, UUIDUtil.STREAM_CODEC));
	static final int TEACH = 0, CHOICE_BASE = 100;
	private final UUID cabinId;
	private final ServerPlayer serverPlayer;
	private final SimpleContainer display = new SimpleContainer(1);
	private final SimpleContainerData data = new SimpleContainerData(4);
	private Item pending;
	private String status = "";

	CabinArboretumMenu(int id, Inventory inventory, UUID cabinId) {
		super(TYPE, id);
		this.cabinId = cabinId;
		serverPlayer = inventory.player instanceof ServerPlayer player ? player : null;
		addSlot(new Slot(display, 0, -1000, -1000) {
			@Override public boolean mayPickup(Player player) { return false; }
			@Override public boolean mayPlace(ItemStack stack) { return false; }
		});
		addDataSlots(data);
		if (serverPlayer != null) sync();
	}

	static void register() { }

	static InteractionResult open(ServerPlayer player, BlockPos board) {
		var cabin = CabinArboretum.cabinAt(player.level().getServer(), board).orElse(null);
		if (cabin == null || !CabinArboretum.relative(cabin, board).equals(CabinArboretum.BOARD)) return InteractionResult.FAIL;
		return player.openMenu(new ExtendedMenuProvider<UUID>() {
			public UUID getScreenOpeningData(ServerPlayer actor) { return cabin.uuid(); }
			public Component getDisplayName() { return Component.literal("Planting Board"); }
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player actor) { return new CabinArboretumMenu(id, inventory, cabin.uuid()); }
		}).isPresent() ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
	}

	private CabinRecord cabin() { return CabinRegistry.get(serverPlayer.level().getServer()).find(cabinId).orElse(null); }

	/** Grove size, 0 when gone. */
	int level() { return data.get(0); }
	int limit() { return CabinArboretumState.limit(level()); }
	int selectedCount() { return data.get(1); }
	boolean household() { return data.get(2) != 0; }
	boolean teachable() { return data.get(3) != 0; }
	String status() { return display.getItem(0).getHoverName().getString(); }

	private boolean mayChange(CabinRecord cabin) {
		return cabin != null && cabin.isHousehold(serverPlayer.getUUID()) && cabin.upgrades().arboretum().level() > 0
			&& CabinArboretum.inGrove(serverPlayer, cabin)
			&& !CabinGreenhouseGrowth.hasGrovePending(serverPlayer.level().getServer(), cabinId);
	}

	private String name(Item item) { return item.getName(new ItemStack(item)).getString().replace(" Sapling", "").replace(" Cutting", ""); }

	private void sync() {
		var cabin = cabin();
		var state = cabin == null ? CabinArboretumState.EMPTY : cabin.upgrades().arboretum();
		boolean household = cabin != null && cabin.isHousehold(serverPlayer.getUUID());
		var inventory = CabinEnchantmentLibrary.inventory(serverPlayer);
		data.set(0, state.level());
		data.set(1, state.selected().size());
		data.set(2, household ? 1 : 0);
		data.set(3, pending != null && cabin != null
			&& CabinArboretumPlanting.available(cabin, inventory, pending) >= CabinArboretumPlanting.TEACH_COST ? 1 : 0);
		if (status.isEmpty()) status = !household ? "Only owners and residents can change the grove."
			: "Choose up to " + CabinArboretumState.limit(state.level()) + " taught tree types to plant.";
		var label = new ItemStack(Items.PAPER);
		label.set(DataComponents.CUSTOM_NAME, Component.literal(status));
		display.setItem(0, label);
		var entries = new ArrayList<CabinStationPayload.Entry>();
		for (var type : CabinArboretumPlanting.types()) {
			var id = CabinArboretumPlanting.id(type);
			boolean taught = state.taught().contains(id), selected = state.selected().contains(id);
			String title = taught ? name(type) + (selected ? ": planting" : ": taught")
				: name(type) + ": teach with " + CabinArboretumPlanting.TEACH_COST + ", have "
					+ (cabin == null ? 0 : CabinArboretumPlanting.available(cabin, inventory, type));
			entries.add(new CabinStationPayload.Entry(title, new ItemStack(type), taught, selected || type == pending));
		}
		CabinStationPayload.send(serverPlayer, containerId, entries);
	}

	@Override
	public boolean clickMenuButton(Player actor, int button) {
		if (actor != serverPlayer) return false;
		var cabin = cabin();
		if (!mayChange(cabin)) {
			status = cabin != null && cabin.isHousehold(serverPlayer.getUUID()) ? "The grove cannot change right now."
				: "Only owners and residents can change the grove.";
			sync(); broadcastChanges(); return true;
		}
		if (button == TEACH) status = teach(cabin);
		else if (button >= CHOICE_BASE && button - CHOICE_BASE < CabinArboretumPlanting.types().size()) {
			var type = CabinArboretumPlanting.types().get(button - CHOICE_BASE);
			status = choose(cabin, type);
		} else return false;
		sync(); broadcastChanges(); return true;
	}

	/** Selecting a taught type toggles its planting; selecting an untaught one picks it for teaching. */
	private String choose(CabinRecord cabin, Item type) {
		var state = cabin.upgrades().arboretum();
		var id = CabinArboretumPlanting.id(type);
		if (!state.taught().contains(id)) {
			pending = type;
			int have = CabinArboretumPlanting.available(cabin, CabinEnchantmentLibrary.inventory(serverPlayer), type);
			return have >= CabinArboretumPlanting.TEACH_COST
				? "Teach " + name(type) + ": uses " + CabinArboretumPlanting.TEACH_COST + " saplings. Press Teach."
				: "Teaching " + name(type) + " needs " + CabinArboretumPlanting.TEACH_COST + " saplings; you have " + have + ".";
		}
		pending = null;
		var selected = new ArrayList<>(state.selected());
		if (selected.remove(id)) {
			update(cabin, state.select(selected));
			return name(type) + " is no longer planted. Grown trees stay.";
		}
		if (selected.size() >= CabinArboretumState.limit(state.level()))
			return "This grove plants " + CabinArboretumState.limit(state.level()) + " type" + (state.level() == 1 ? "" : "s") + " at most. Deselect one first.";
		selected.add(id);
		update(cabin, state.select(selected));
		return name(type) + " is now planted.";
	}

	private void update(CabinRecord cabin, CabinArboretumState state) {
		var server = serverPlayer.level().getServer();
		var registry = CabinRegistry.get(server);
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withArboretum(state));
		CabinRegistry.flush(server);
		CabinArboretumPlanting.applySelection(CabinArboretum.level(server), registry.find(cabin.uuid()).orElseThrow());
	}

	/** Consumes the saplings, from installed storage first, then the inventory, and teaches the type for good. */
	private String teach(CabinRecord cabin) {
		if (pending == null) return "Choose a tree type to teach first.";
		var type = pending;
		var id = CabinArboretumPlanting.id(type);
		var server = serverPlayer.level().getServer();
		var registry = CabinRegistry.get(server);
		synchronized (registry) {
			cabin = registry.find(cabinId).orElse(null);
			if (!mayChange(cabin)) return "The grove cannot change right now.";
			var state = cabin.upgrades().arboretum();
			if (state.taught().contains(id)) { pending = null; return name(type) + " is already taught."; }
			var inventory = new ArrayList<>(CabinEnchantmentLibrary.inventory(serverPlayer).stream().map(ItemStack::copy).toList());
			var storage = new ArrayList<>(cabin.upgrades().storage().stacks().stream().map(ItemStack::copy).toList());
			int left = take(type, CabinArboretumPlanting.TEACH_COST, storage, false);
			left = take(type, left, inventory, true);
			if (left > 0) return "Teaching " + name(type) + " needs " + CabinArboretumPlanting.TEACH_COST + " saplings.";
			storage.removeIf(ItemStack::isEmpty);
			var receipt = CabinStorage.receipt(serverPlayer, serverPlayer.containerMenu.getCarried());
			var session = new CabinStorageState.Session(serverPlayer.getUUID(), List.copyOf(inventory), receipt.cursor(), receipt.operation(), receipt.delivery());
			registry.updateUpgradeState(cabinId, cabin.upgrades().withArboretum(state.teach(id))
				.withStorage(cabin.upgrades().storage().withStacks(storage).withSession(session)));
			CabinRegistry.flush(server);
			serverPlayer.containerMenu.setCarried(ItemStack.EMPTY);
			CabinStorage.recover(serverPlayer);
			pending = null;
			return "The grove has learned " + name(type) + ". Choose it to plant it.";
		}
	}

	private static int take(Item type, int count, List<ItemStack> stacks, boolean playerInventory) {
		int left = count;
		for (int i = 0; i < stacks.size() && left > 0; i++) {
			if (playerInventory && i >= 36 && i != Inventory.SLOT_OFFHAND) continue;
			var stack = stacks.get(i);
			if (!stack.is(type) || !CabinStation.ordinary(stack)) continue;
			int used = Math.min(left, stack.getCount()); stack.shrink(used); left -= used;
		}
		return left;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

	@Override
	public boolean stillValid(Player player) {
		if (serverPlayer == null) return true;
		var cabin = cabin();
		return cabin != null && cabin.upgrades().arboretum().level() > 0 && CabinArboretum.inGrove(serverPlayer, cabin)
			&& serverPlayer.blockPosition().closerThan(CabinArboretum.world(cabin, CabinArboretum.BOARD), 8);
	}

	@Override
	public void clicked(int slot, int button, ContainerInput input, Player actor) { }
}
