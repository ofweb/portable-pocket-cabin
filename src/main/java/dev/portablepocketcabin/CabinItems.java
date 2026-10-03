package dev.portablepocketcabin;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.UUID;

final class CabinItems {
	private static final String CABIN_UUID = "portable_pocket_cabin_uuid";
	private static final String GENERATION = "portable_pocket_cabin_generation";
	private static final String PENDING = "portable_pocket_cabin_pending";
	private static final String ITEM_INSTANCE_UUID = "portable_pocket_cabin_item_uuid";
	private static final String PALETTE = "portable_pocket_cabin_palette";

	static final Item GREENHOUSE_BOOK = registerSimple("greenhouse_book");
	static final Item CRAFTING_BOOK = registerSimple("crafting_book");

	static final Item STORAGE_BOOK = registerSimple("storage_book");

	static final Item DIMENSIONAL_LOGIC_CORE = registerSimple("dimensional_logic_core");
	static final Item DIMENSIONAL_ANCHOR = registerSimple("dimensional_anchor");
	static final Item DIMENSIONAL_FOLDING_CORE = registerSimple("dimensional_folding_core");
	static final Item DIMENSIONAL_FOUNDATION = registerSimple("dimensional_foundation");

	private static final ResourceKey<Item> PACKED_CABIN_KEY = ResourceKey.create(
		Registries.ITEM, PortablePocketCabin.id("packed_cabin")
	);
	static final Item PACKED_CABIN = Registry.register(
		BuiltInRegistries.ITEM,
		PACKED_CABIN_KEY,
		new PackedCabinItem(new Item.Properties().setId(PACKED_CABIN_KEY).stacksTo(1).fireResistant())
	);

	record Binding(UUID cabinId, long generation, boolean pending, UUID itemInstanceId, CabinPalette palette) {
	}

	record Unbound(UUID itemInstanceId, CabinPalette palette) {
	}

	private CabinItems() {
	}

	static void register() {
		// Triggers static registration during mod initialization.
	}

	static ItemStack createUnbound(CabinPalette palette) {
		return createUnbound(palette, UUID.randomUUID());
	}

	static ItemStack createUnbound(CabinPalette palette, UUID itemInstanceId) {
		ItemStack stack = new ItemStack(PACKED_CABIN);
		CompoundTag tag = new CompoundTag();
		tag.putString(ITEM_INSTANCE_UUID, itemInstanceId.toString());
		tag.store(PALETTE, CabinPalette.CODEC, NbtOps.INSTANCE, palette);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Cabin Kit").withStyle(ChatFormatting.GOLD));
		return stack;
	}

	static ItemStack createBound(CabinRecord cabin, long generation, boolean pending) {
		return createBound(cabin, generation, pending, UUID.randomUUID());
	}

	static ItemStack createBound(CabinRecord cabin, long generation, boolean pending, UUID itemInstanceId) {
		ItemStack stack = new ItemStack(PACKED_CABIN);
		CompoundTag tag = new CompoundTag();
		tag.putString(CABIN_UUID, cabin.uuid().toString());
		tag.putLong(GENERATION, generation);
		tag.putBoolean(PENDING, pending);
		tag.putString(ITEM_INSTANCE_UUID, itemInstanceId.toString());
		tag.store(PALETTE, CabinPalette.CODEC, NbtOps.INSTANCE, cabin.palette());
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(pending ? "Cabin (transaction pending…)" : "Packed Cabin")
			.withStyle(pending ? ChatFormatting.GRAY : ChatFormatting.GOLD));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, !pending);
		return stack;
	}

	static Optional<Binding> binding(ItemStack stack) {
		if (!stack.is(PACKED_CABIN)) {
			return Optional.empty();
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return Optional.empty();
		}
		CompoundTag tag = data.copyTag();
		try {
			UUID cabinId = UUID.fromString(tag.getStringOr(CABIN_UUID, ""));
			UUID itemId = UUID.fromString(tag.getStringOr(ITEM_INSTANCE_UUID, ""));
			long generation = tag.getLongOr(GENERATION, -1L);
			CabinPalette palette = tag.read(PALETTE, CabinPalette.CODEC, NbtOps.INSTANCE).orElse(null);
			if (generation < 0 || palette == null) {
				return Optional.empty();
			}
			return Optional.of(new Binding(
				cabinId, generation, tag.getBooleanOr(PENDING, false), itemId, palette
			));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	static Optional<Unbound> unbound(ItemStack stack) {
		if (!stack.is(PACKED_CABIN) || binding(stack).isPresent()) {
			return Optional.empty();
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return Optional.empty();
		}
		CompoundTag tag = data.copyTag();
		try {
			UUID itemId = UUID.fromString(tag.getStringOr(ITEM_INSTANCE_UUID, ""));
			return tag.read(PALETTE, CabinPalette.CODEC, NbtOps.INSTANCE)
				.map(palette -> new Unbound(itemId, palette));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	static boolean isUnbound(ItemStack stack) {
		return unbound(stack).isPresent();
	}

	static int findUnbound(Inventory inventory) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (isUnbound(inventory.getItem(slot))) {
				return slot;
			}
		}
		return Inventory.NOT_FOUND_INDEX;
	}

	static int findUnbound(Inventory inventory, UUID itemInstanceId, CabinPalette palette) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			Unbound unbound = unbound(inventory.getItem(slot)).orElse(null);
			if (unbound != null && unbound.itemInstanceId().equals(itemInstanceId)
				&& unbound.palette().equals(palette)) {
				return slot;
			}
		}
		return Inventory.NOT_FOUND_INDEX;
	}

	static int findValid(Inventory inventory, CabinRecord cabin) {
		return find(inventory, cabin.uuid(), cabin.packedItemGeneration(), false, null, cabin.palette());
	}

	static int findPending(Inventory inventory, CabinRecord cabin, long generation) {
		return find(inventory, cabin.uuid(), generation, true, null, cabin.palette());
	}

	static int findByInstance(Inventory inventory, UUID itemInstanceId) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			Binding binding = binding(stack).orElse(null);
			if (binding != null && binding.itemInstanceId().equals(itemInstanceId)) {
				return slot;
			}
			Unbound unbound = unbound(stack).orElse(null);
			if (unbound != null && unbound.itemInstanceId().equals(itemInstanceId)) {
				return slot;
			}
		}
		return Inventory.NOT_FOUND_INDEX;
	}

	static void removePending(Inventory inventory, UUID cabinId, long generation) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			Binding binding = binding(inventory.getItem(slot)).orElse(null);
			if (binding != null && binding.cabinId().equals(cabinId)
				&& binding.generation() == generation && binding.pending()) {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	static int removeByInstance(Inventory inventory, UUID itemInstanceId) {
		int removed = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			Binding binding = binding(stack).orElse(null);
			Unbound unbound = unbound(stack).orElse(null);
			if (binding != null && binding.itemInstanceId().equals(itemInstanceId)
				|| unbound != null && unbound.itemInstanceId().equals(itemInstanceId)) {
				inventory.setItem(slot, ItemStack.EMPTY);
				removed++;
			}
		}
		return removed;
	}

	static boolean give(Inventory inventory, ItemStack stack) {
		return inventory.add(stack);
	}

	static void activatePending(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(PENDING, false));
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Packed Cabin").withStyle(ChatFormatting.GOLD));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
	}

	private static int find(
		Inventory inventory, UUID cabinId, long generation, Boolean pending, UUID itemId, CabinPalette palette
	) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			Binding binding = binding(inventory.getItem(slot)).orElse(null);
			if (binding != null && binding.cabinId().equals(cabinId)
				&& binding.generation() == generation && binding.palette().equals(palette)
				&& (pending == null || binding.pending() == pending)
				&& (itemId == null || binding.itemInstanceId().equals(itemId))) {
				return slot;
			}
		}
		return Inventory.NOT_FOUND_INDEX;
	}

	private static Item registerSimple(String name) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, PortablePocketCabin.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
	}
}
