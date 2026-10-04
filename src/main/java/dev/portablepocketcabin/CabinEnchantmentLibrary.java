package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.ArrayList;
import java.util.List;

final class CabinEnchantmentLibrary {
	private CabinEnchantmentLibrary() { }

	record Source(boolean storage, int slot, ItemStack stack) {
		Source { stack = stack.copy(); }
		@Override public ItemStack stack() { return stack.copy(); }
		String label() { return storage ? "Storage" : "Inventory"; }
	}
	record Cost(Item item, int storage, int inventory) {
		int total() { return storage + inventory; }
		String description() { return total() + " " + new ItemStack(item).getHoverName().getString() + " (storage " + storage + ", inventory " + inventory + ")"; }
	}
	record Plan(Source source, Identifier enchantment, int level, boolean learning,
		List<ItemStack> beforeInventory, CabinStorageState beforeStorage, CabinEnchantingState beforeLibrary,
		List<ItemStack> afterInventory, CabinStorageState afterStorage, CabinEnchantingState afterLibrary,
		ItemStack result, List<Cost> costs, String reason) {
		boolean ready() { return reason.isEmpty(); }
	}

	static ItemEnchantments enchantments(ItemStack stack) { return EnchantmentHelper.getEnchantmentsForCrafting(stack); }
	static boolean vanilla(ItemStack stack) {
		return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft")
			&& enchantments(stack).keySet().stream().allMatch(holder -> holder.unwrapKey()
				.map(key -> key.identifier().getNamespace().equals("minecraft")).orElse(false));
	}
	static List<Source> sources(CabinRecord cabin, List<ItemStack> inventory, boolean learning, RegistryAccess access) {
		var sources = new ArrayList<Source>();
		var storage = cabin.upgrades().storage().stacks();
		for (int i = 0; i < storage.size(); i++) if (candidate(storage.get(i), learning, access)) sources.add(new Source(true, i, storage.get(i)));
		for (int i = 0; i < inventory.size(); i++) if (candidate(inventory.get(i), learning, access)) sources.add(new Source(false, i, inventory.get(i)));
		return List.copyOf(sources);
	}
	private static boolean candidate(ItemStack stack, boolean learning, RegistryAccess access) {
		if (!vanilla(stack)) return false;
		return learning ? !enchantments(stack).isEmpty() : stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)
			|| access.lookupOrThrow(Registries.ENCHANTMENT).stream().anyMatch(enchantment -> enchantment.isSupportedItem(stack));
	}
	static List<ItemStack> inventory(ServerPlayer player) {
		var result = new ArrayList<ItemStack>();
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) result.add(player.getInventory().getItem(i).copy());
		return List.copyOf(result);
	}

	static boolean valid(ServerPlayer player, CabinRecord cabin) {
		if (!CabinStorage.mayUse(cabin, player.getUUID()) || cabin.upgrades().enchanting().level() == 0
			|| cabin.upgrades().operationInProgress() || !player.level().dimension().equals(PocketDimension.LEVEL_KEY)) return false;
		var space = CabinEnchanting.space(cabin);
		BlockPos relative = player.blockPosition().subtract(PocketDimension.cellCenter(cabin.cellIndex()));
		BlockPos table = table(cabin);
		return relative.getX() > space.minimum().getX() && relative.getX() < space.maximum().getX()
			&& relative.getZ() > space.minimum().getZ() && relative.getZ() < space.maximum().getZ()
			&& relative.getY() > space.minimum().getY() && relative.getY() < space.maximum().getY()
			&& player.level().getBlockState(table).is(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE)
			&& player.distanceToSqr(table.getX() + .5, table.getY() + .5, table.getZ() + .5) <= 64;
	}
	static BlockPos table(CabinRecord cabin) {
		return PocketDimension.cellCenter(cabin.cellIndex()).offset(CabinEnchanting.space(cabin).minimum()).offset(3, 1, 3);
	}

	static Plan preview(CabinRecord cabin, List<ItemStack> inventory, Source source,
		Identifier enchantmentId, int level, boolean learning, RegistryAccess access) {
		var library = cabin.upgrades().enchanting();
		var storage = cabin.upgrades().storage();
		var afterInventory = new ArrayList<>(inventory.stream().map(ItemStack::copy).toList());
		var afterStorage = new ArrayList<>(storage.stacks());
		var costs = new ArrayList<Cost>();
		var result = ItemStack.EMPTY;
		String reason = "";
		var holder = access.lookupOrThrow(Registries.ENCHANTMENT).get(enchantmentId).orElse(null);
		ItemStack selected = source.stack();
		var sourceStacks = source.storage() ? afterStorage : afterInventory;
		if (library.level() == 0) reason = "Install the enchanting room first.";
		else if (source.slot() < 0 || source.slot() >= sourceStacks.size() || !ItemStack.matches(sourceStacks.get(source.slot()), selected))
			reason = "The selected item changed; select it again.";
		else if (!vanilla(selected) || holder == null || !enchantmentId.getNamespace().equals("minecraft"))
			reason = "This item or enchantment has no supported vanilla profile.";
		else if (level < 1 || level > 255) reason = "Invalid enchantment level.";
		else if (learning) {
			int sourceLevel = enchantments(selected).getLevel(holder);
			if (sourceLevel != level) reason = "The sacrifice no longer has that enchantment level.";
			else if (!selected.is(Items.ENCHANTED_BOOK) && !holder.value().isSupportedItem(selected)) reason = "Unsupported enchanted equipment.";
			else if (level <= library.known().getOrDefault(enchantmentId, 0)) reason = "That level is already known; nothing is consumed.";
			else {
				sourceStacks.get(source.slot()).shrink(1);
				int books = level - library.known().getOrDefault(enchantmentId, 0);
				if (!fill(Items.BOOK, books, afterStorage, afterInventory, costs)) reason = "Not enough ordinary books.";
				else library = library.learn(enchantmentId, level);
			}
		} else {
			var existing = enchantments(selected);
			if (level > library.known().getOrDefault(enchantmentId, 0) || level > holder.value().getMaxLevel() || level > library.limit())
				reason = "That level exceeds cabin knowledge, the normal maximum, or the room limit.";
			else if (!selected.is(Items.BOOK) && !selected.is(Items.ENCHANTED_BOOK) && !holder.value().isSupportedItem(selected))
				reason = "That enchantment cannot be applied to this item.";
			else if (existing.keySet().stream().anyMatch(other -> !other.equals(holder) && !Enchantment.areCompatible(holder, other)))
				reason = "That enchantment conflicts with an existing enchantment.";
			else if (existing.getLevel(holder) >= level) reason = "The item already has that level or higher; nothing is consumed.";
			else {
				result = selected.is(Items.BOOK) ? selected.transmuteCopy(Items.ENCHANTED_BOOK, 1) : selected.copyWithCount(1);
				var updated = new ItemEnchantments.Mutable(existing);
				updated.set(holder, level);
				result.set(result.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, updated.toImmutable());
				sourceStacks.get(source.slot()).shrink(1);
				int shards = level * (1 + existing.size());
				if (!fill(Items.AMETHYST_SHARD, shards, afterStorage, afterInventory, costs)) reason = "Not enough ordinary amethyst shards.";
				else if (!fill(Items.LAPIS_LAZULI, 2 * shards, afterStorage, afterInventory, costs)) reason = "Not enough ordinary lapis lazuli.";
				else if (!returnResult(source, result, afterStorage, afterInventory, storage.capacity())) reason = "No room for the result in the selected source.";
			}
		}
		afterStorage.removeIf(ItemStack::isEmpty);
		return new Plan(source, enchantmentId, level, learning, inventory.stream().map(ItemStack::copy).toList(), storage,
			cabin.upgrades().enchanting(), List.copyOf(afterInventory), storage.withStacks(afterStorage), library, result, List.copyOf(costs), reason);
	}

	private static boolean fill(Item item, int count, List<ItemStack> storage, List<ItemStack> inventory, List<Cost> costs) {
		int fromStorage = take(item, count, storage, false);
		int fromInventory = take(item, count - fromStorage, inventory, true);
		costs.add(new Cost(item, fromStorage, fromInventory));
		return fromStorage + fromInventory == count;
	}
	private static int take(Item item, int count, List<ItemStack> stacks, boolean playerInventory) {
		int left = count;
		for (int i = 0; i < stacks.size() && left > 0; i++) {
			if (playerInventory && i >= 36 && i != net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND) continue;
			ItemStack stack = stacks.get(i);
			if (!stack.is(item) || !CabinStation.ordinary(stack)) continue;
			int used = Math.min(left, stack.getCount()); stack.shrink(used); left -= used;
		}
		return count - left;
	}
	private static boolean returnResult(Source source, ItemStack result, List<ItemStack> storage, List<ItemStack> inventory, int capacity) {
		List<ItemStack> stacks = source.storage() ? storage : inventory;
		if (stacks.get(source.slot()).isEmpty()) { stacks.set(source.slot(), result.copy()); return true; }
		for (int i = 0; i < stacks.size(); i++) {
			if (!source.storage() && i >= 36 && i != source.slot()) continue;
			var stack = stacks.get(i);
			if (ItemStack.isSameItemSameComponents(stack, result) && stack.getCount() < stack.getMaxStackSize()) { stack.grow(1); return true; }
		}
		for (int i = 0; i < stacks.size(); i++) {
			if (!source.storage() && i >= 36) continue;
			if (stacks.get(i).isEmpty()) { stacks.set(i, result.copy()); return true; }
		}
		if (source.storage() && stacks.size() < capacity) { stacks.add(result.copy()); return true; }
		return false;
	}

	static String confirm(ServerPlayer player, java.util.UUID cabinId, Plan plan) {
		var server = player.level().getServer();
		var registry = CabinRegistry.get(server);
		synchronized (registry) {
			var cabin = registry.find(cabinId).orElse(null);
			if (!valid(player, cabin)) return "The table or household access is no longer available.";
			if (plan == null || !plan.ready()) return plan == null ? "Select an enchantment first." : plan.reason();
			if (!ItemStack.listMatches(inventory(player), plan.beforeInventory())
				|| !cabin.upgrades().enchanting().equals(plan.beforeLibrary())
				|| !ItemStack.listMatches(cabin.upgrades().storage().stacks(), plan.beforeStorage().stacks())
				|| cabin.upgrades().storage().level() != plan.beforeStorage().level()
				|| !cabin.upgrades().storage().sessions().equals(plan.beforeStorage().sessions()))
				return "Items or cabin knowledge changed; select the enchantment again.";
			var checked = preview(cabin, inventory(player), plan.source(), plan.enchantment(), plan.level(), plan.learning(), player.registryAccess());
			if (!checked.ready()) return checked.reason();
			var receipt = CabinStorage.receipt(player, player.containerMenu.getCarried());
			var session = new CabinStorageState.Session(player.getUUID(), checked.afterInventory(), receipt.cursor(), receipt.operation(), receipt.delivery());
			registry.updateUpgradeState(cabinId, cabin.upgrades().withEnchanting(checked.afterLibrary()).withStorage(checked.afterStorage().withSession(session)));
			CabinRegistry.flush(server);
			player.containerMenu.setCarried(ItemStack.EMPTY);
			CabinStorage.recover(player);
			return checked.learning() ? "Enchantment learned. The sacrifice was destroyed." : "Enchantment applied.";
		}
	}
}
