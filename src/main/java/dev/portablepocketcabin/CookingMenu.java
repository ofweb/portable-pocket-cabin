package dev.portablepocketcabin;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumMap;
import java.util.Map;

/** One furnace-style menu for every container cooking station, with a slot layout per station. */
final class CookingMenu extends AbstractContainerMenu {
	static final int DATA_PROGRESS = 0;
	static final int DATA_TIME = 1;
	static final int DATA_WATER = 2;
	static final int DATA_HEAT = 3;
	static final int DATA_NEEDED_HEAT = 4;
	static final int DATA_LIT_TIME = 5;
	static final int DATA_LIT_DURATION = 6;
	static final int DATA_COUNT = 7;
	static final int INVENTORY_Y = 84;

	/** Read-only menu data that a station computes on demand. */
	abstract static class Data implements ContainerData {
		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	}

	enum Kind {
		STOVE(1, new int[][] {{80, 53}}),
		POT(9, new int[][] {{30, 17}, {48, 17}, {66, 17}, {30, 35}, {48, 35}, {66, 35}, {48, 56}, {124, 26}, {124, 53}}),
		OVEN(9, new int[][] {{30, 17}, {48, 17}, {66, 17}, {30, 35}, {48, 35}, {66, 35}, {48, 56}, {124, 26}, {124, 53}});

		final int size;
		final int[][] slots;

		Kind(int size, int[][] slots) {
			this.size = size;
			this.slots = slots;
		}

		boolean takeOnly(int slot) {
			return this != STOVE && (slot == CookingPotBlockEntity.OUTPUT_SLOT || slot == CookingPotBlockEntity.BYPRODUCT_SLOT);
		}
	}

	private static final Map<Kind, MenuType<CookingMenu>> TYPES = new EnumMap<>(Kind.class);
	private final Kind kind;
	private final Container container;
	private final ContainerData data;
	private final BlockPos pos;

	private CookingMenu(Kind kind, int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
		super(TYPES.get(kind), containerId);
		this.kind = kind;
		this.container = container;
		this.data = data;
		this.pos = pos;
		for (int slot = 0; slot < kind.size; slot++) {
			int index = slot;
			addSlot(new Slot(container, slot, kind.slots[slot][0], kind.slots[slot][1]) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return !kind.takeOnly(index) && container.canPlaceItem(index, stack);
				}
			});
		}
		addStandardInventorySlots(inventory, 8, INVENTORY_Y);
		addDataSlots(data);
	}

	static void register() {
		for (Kind kind : Kind.values()) {
			TYPES.put(kind, Registry.register(BuiltInRegistries.MENU, PortablePocketCabin.id(kind.name().toLowerCase()),
				new ExtendedMenuType<>((id, inventory, pos) -> new CookingMenu(kind, id, inventory,
					new SimpleContainer(kind.size), new SimpleContainerData(DATA_COUNT), pos), BlockPos.STREAM_CODEC)));
		}
	}

	static MenuType<CookingMenu> type(Kind kind) {
		return TYPES.get(kind);
	}

	static CookingMenu server(Kind kind, int containerId, Inventory inventory, CookingStationBlockEntity station, BlockPos pos) {
		ContainerData data = switch (station) {
			case StoveBlockEntity stove -> stove.data;
			case CookingPotBlockEntity pot -> pot.data;
			case OvenBlockEntity oven -> oven.data;
			default -> new SimpleContainerData(DATA_COUNT);
		};
		return new CookingMenu(kind, containerId, inventory, station, data, pos);
	}

	Kind kind() {
		return kind;
	}

	BlockPos pos() {
		return pos;
	}

	int data(int index) {
		return data.get(index);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		int stationSlots = kind.size;
		int end = stationSlots + 36;
		if (index < stationSlots) {
			boolean moved = moveItemStackTo(stack, stationSlots, end, true);
			if (kind.takeOnly(index) && !stack.isEmpty() && player instanceof ServerPlayer serverPlayer
				&& container instanceof CookingStationBlockEntity station) {
				// In the kitchen, output that does not fit in the inventory goes to central storage.
				slot.setChanged();
				if (CabinKitchenStorage.overflow(serverPlayer, station, index) || moved) return original;
				return ItemStack.EMPTY;
			}
			if (!moved) return ItemStack.EMPTY;
		} else if (kind == Kind.STOVE) {
			if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
		} else if (kind == Kind.OVEN) {
			boolean fuel = player.level().fuelValues().isFuel(stack);
			if (!(fuel && moveItemStackTo(stack, OvenBlockEntity.FUEL_SLOT, OvenBlockEntity.FUEL_SLOT + 1, false))
				&& !moveItemStackTo(stack, 0, CookingStation.OVEN.ingredientSlots(), false)) return ItemStack.EMPTY;
		} else if (stack.is(Items.BOWL)) {
			if (!moveItemStackTo(stack, CookingPotBlockEntity.CONTAINER_SLOT, CookingPotBlockEntity.CONTAINER_SLOT + 1, false)
				&& !moveItemStackTo(stack, 0, CookingStation.POT.ingredientSlots(), false)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, CookingStation.POT.ingredientSlots(), false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return original;
	}
}
