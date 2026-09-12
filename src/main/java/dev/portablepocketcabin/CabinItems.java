package dev.portablepocketcabin;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
	private static final ResourceKey<Item> PACKED_CABIN_KEY = ResourceKey.create(
		Registries.ITEM, PortablePocketCabin.id("packed_cabin")
	);

	static final Item PACKED_CABIN = Registry.register(
		BuiltInRegistries.ITEM,
		PACKED_CABIN_KEY,
		new PackedCabinItem(new Item.Properties().setId(PACKED_CABIN_KEY).stacksTo(1).fireResistant())
	);

	record Binding(UUID cabinId, long generation, boolean pending) {
	}

	private CabinItems() {
	}

	static void register() {
		// Triggers static registration during mod initialization.
	}

	static ItemStack createBound(CabinRecord cabin, long generation, boolean pending) {
		ItemStack stack = new ItemStack(PACKED_CABIN);
		CompoundTag tag = new CompoundTag();
		tag.putString(CABIN_UUID, cabin.uuid().toString());
		tag.putLong(GENERATION, generation);
		tag.putBoolean(PENDING, pending);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(pending ? "Cabin (packing…)" : "Packed Cabin")
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
			long generation = tag.getLongOr(GENERATION, -1L);
			if (generation < 0) {
				return Optional.empty();
			}
			return Optional.of(new Binding(cabinId, generation, tag.getBooleanOr(PENDING, false)));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	static boolean isUnbound(ItemStack stack) {
		return stack.is(PACKED_CABIN) && binding(stack).isEmpty();
	}

	static int findUnbound(Inventory inventory) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (isUnbound(inventory.getItem(slot))) {
				return slot;
			}
		}
		return Inventory.NOT_FOUND_INDEX;
	}

	static int findValid(Inventory inventory, CabinRecord cabin) {
		return find(inventory, cabin.uuid(), cabin.packedItemGeneration(), null);
	}

	static int findPending(Inventory inventory, CabinRecord cabin, long generation) {
		return find(inventory, cabin.uuid(), generation, true);
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

	static boolean give(Inventory inventory, ItemStack stack) {
		return inventory.add(stack);
	}

	static void activatePending(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(PENDING, false));
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Packed Cabin").withStyle(ChatFormatting.GOLD));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
	}

	private static int find(Inventory inventory, UUID cabinId, long generation, Boolean pending) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			Binding binding = binding(inventory.getItem(slot)).orElse(null);
			if (binding != null && binding.cabinId().equals(cabinId)
				&& binding.generation() == generation && (pending == null || binding.pending() == pending)) {
				return slot;
			}
		}
		return Inventory.NOT_FOUND_INDEX;
	}
}
