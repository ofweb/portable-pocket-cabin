package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Central storage at kitchen stations: which stations may use it, and how a transfer commits (ADR-0012). */
final class CabinKitchenStorage {
	private CabinKitchenStorage() {
	}

	/**
	 * The cabin whose storage serves this station for this player: the station is inside that cabin's kitchen, the
	 * player is household, storage is installed, no cabin operation is running, and the player has no unconfirmed transfer.
	 */
	static Optional<CabinRecord> cabinFor(ServerPlayer player, BlockPos station) {
		if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(PocketDimension.LEVEL_KEY)) return Optional.empty();
		long cell = PocketDimension.cellIndexAt(station).orElse(-1L);
		return CabinRegistry.get(level.getServer()).findByCell(cell).filter(cabin -> CabinKitchen.contains(cabin, station)
			&& CabinStorage.mayUse(cabin, player.getUUID()) && cabin.upgrades().storage().level() > 0
			&& !cabin.upgrades().operationInProgress() && cabin.upgrades().kitchen().pendingFor(player.getUUID()).isEmpty());
	}

	/** A planned change to one station's full contents. */
	record StationAfter(CookingStationBlockEntity station, List<ItemStack> contents) {
	}

	/**
	 * Commits a planned transfer: the receipt and new storage contents first, then the stations and inventory, then
	 * the saves that confirm it. Returns false when the confirmation could not be saved; the receipt then stays for recovery.
	 */
	static boolean commit(ServerPlayer player, CabinRecord cabin, List<StationAfter> stations, List<ItemStack> inventoryAfter,
		CabinStorageState storageAfter, List<ItemStack> withdrawn, List<ItemStack> deposited) {
		var server = player.level().getServer();
		var registry = CabinRegistry.get(server);
		Inventory inventory = player.getInventory();
		List<ItemStack> inventoryBefore = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) inventoryBefore.add(inventory.getItem(slot).copy());
		List<CabinKitchenTransfer.Change> changes = stations.stream()
			.map(after -> new CabinKitchenTransfer.Change(after.station().getBlockPos(), after.station().contents(), after.contents())).toList();
		UUID operation = UUID.randomUUID();
		var receipt = new CabinKitchenTransfer(operation, player.getUUID(), changes, inventoryBefore, inventoryAfter, withdrawn, deposited);
		synchronized (registry) {
			var current = registry.find(cabin.uuid()).orElseThrow();
			registry.updateUpgradeState(cabin.uuid(), current.upgrades().withStorage(current.upgrades().storage().withStacks(storageAfter.stacks()))
				.withKitchen(current.upgrades().kitchen().withPending(receipt)));
			CabinRegistry.flush(server);
		}
		for (StationAfter after : stations) after.station().applyTransfer(operation, after.contents());
		for (int slot = 0; slot < inventoryAfter.size(); slot++) inventory.setItem(slot, inventoryAfter.get(slot).copy());
		inventory.setChanged();
		return confirm(player, cabin.uuid(), operation);
	}

	/** Saves the stations' chunks and the player, then removes the receipt. */
	static boolean confirm(ServerPlayer player, UUID cabin, UUID operation) {
		var server = player.level().getServer();
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) return false;
		pocket.save(null, true, false);
		if (!CabinPlayerSave.confirm(player)) return false;
		var registry = CabinRegistry.get(server);
		synchronized (registry) {
			var current = registry.find(cabin).orElseThrow();
			registry.updateUpgradeState(cabin, current.upgrades().withKitchen(current.upgrades().kitchen().withoutPending(operation)));
			CabinRegistry.flush(server);
		}
		return true;
	}

	/**
	 * Moves what is left in a kitchen station's output or byproduct slot into central storage, through a receipt.
	 * Returns whether anything moved.
	 */
	static boolean overflow(ServerPlayer player, CookingStationBlockEntity station, int slot) {
		CabinRecord cabin = cabinFor(player, station.getBlockPos()).orElse(null);
		ItemStack stack = station.getItem(slot);
		if (cabin == null || stack.isEmpty()) return false;
		var transfer = cabin.upgrades().storage().deposit(stack, stack.getCount());
		if (transfer.moved().isEmpty()) return false;
		List<ItemStack> after = new ArrayList<>(station.contents());
		after.get(slot).shrink(transfer.moved().getCount());
		List<ItemStack> inventory = new ArrayList<>();
		for (int index = 0; index < player.getInventory().getContainerSize(); index++) inventory.add(player.getInventory().getItem(index).copy());
		commit(player, cabin, List.of(new StationAfter(station, after)), inventory, transfer.state(), List.of(), List.of(transfer.moved()));
		return true;
	}

	/** Puts the stack into the main inventory copy, merging first. Returns what did not fit. */
	static ItemStack insert(List<ItemStack> main, ItemStack item) {
		ItemStack rest = item.copy();
		for (ItemStack slot : main) {
			if (rest.isEmpty()) break;
			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
				int moved = Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(moved);
				rest.shrink(moved);
			}
		}
		for (int slot = 0; slot < main.size() && !rest.isEmpty(); slot++) {
			if (main.get(slot).isEmpty()) main.set(slot, rest.split(Math.min(rest.getCount(), rest.getMaxStackSize())));
		}
		return rest;
	}
}
