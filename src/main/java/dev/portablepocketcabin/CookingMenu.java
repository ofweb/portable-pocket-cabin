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
	static final int DATA_LIQUID = 7;
	static final int DATA_PAUSED = 8;
	static final int DATA_COUNT = 9;
	static final int INVENTORY_Y = 84;
	/** Time stations report progress and time in seconds, so that days fit the menu's short data values. */
	static final int TIME_STATION_SCALE = 20;

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

	/**
	 * Each station screen: the recipe type it serves, its slot positions, and which slots hold the container and
	 * the output and byproduct. The stove has no recipes, and the oven's slot 6 is fuel.
	 */
	enum Kind {
		STOVE(null, new int[][] {{80, 53}}, -1, -1, -1),
		POT(CookingStation.POT, new int[][] {{30, 17}, {48, 17}, {66, 17}, {30, 35}, {48, 35}, {66, 35}, {48, 56}, {124, 26}, {124, 53}, {142, 53}}, 6, 7, 8, 9),
		OVEN(CookingStation.OVEN, new int[][] {{30, 17}, {48, 17}, {66, 17}, {30, 35}, {48, 35}, {66, 35}, {48, 56}, {124, 26}, {124, 53}}, -1, 7, 8),
		COUNTER(CookingStation.COUNTER, new int[][] {{30, 17}, {48, 17}, {66, 17}, {30, 35}, {48, 35}, {66, 35}, {124, 26}, {124, 53}}, -1, 6, 7),
		STRAINER(CookingStation.STRAINER, new int[][] {{39, 17}, {57, 17}, {39, 35}, {57, 35}, {48, 56}, {124, 26}, {124, 53}}, 4, 5, 6),
		WOK(CookingStation.WOK, new int[][] {{39, 17}, {57, 17}, {39, 35}, {57, 35}, {48, 56}, {124, 26}, {124, 53}}, 4, 5, 6),
		PRESS(CookingStation.PRESS, new int[][] {{48, 26}, {48, 56}, {124, 26}, {124, 53}}, 1, 2, 3),
		MILL(CookingStation.MILL, new int[][] {{48, 26}, {48, 56}, {124, 26}, {124, 53}}, 1, 2, 3),
		MIXING_BOWL(CookingStation.MIXING_BOWL, new int[][] {{30, 17}, {48, 17}, {66, 17}, {30, 35}, {48, 35}, {66, 35}, {48, 56}, {124, 26}, {124, 53}}, 6, 7, 8),
		MORTAR(CookingStation.MORTAR, new int[][] {{39, 26}, {57, 26}, {124, 26}}, -1, 2),
		CHURN(CookingStation.CHURN, new int[][] {{39, 26}, {57, 26}, {124, 26}, {124, 53}, {142, 53}}, -1, 2, 3, 4),
		CHEESE_PRESS(CookingStation.CHEESE_PRESS, new int[][] {{39, 26}, {57, 26}, {124, 26}, {124, 53}}, -1, 2, 3),
		PROOFING(CookingStation.PROOFING, new int[][] {{48, 26}, {124, 26}}, -1, 1),
		DAIRY_PAN(CookingStation.DAIRY_PAN, new int[][] {{48, 26}, {48, 56}, {124, 26}, {124, 53}}, 1, 2, 3),
		CROCK(CookingStation.CROCK, new int[][] {{39, 17}, {57, 17}, {39, 35}, {57, 35}, {48, 56}, {124, 26}, {124, 53}, {142, 53}}, 4, 5, 6, 7),
		BRINE(CookingStation.BRINE, new int[][] {{39, 17}, {57, 17}, {39, 35}, {57, 35}, {124, 26}, {124, 53}}, -1, 4, 5);

		final CookingStation station;
		final int size;
		final int[][] slots;
		final int containerSlot;
		final int outputSlot;
		/** The first byproduct slot, or -1. Some stations have a second, for recipes that leave two kinds of remainder. */
		final int byproductSlot;
		final int[] byproductSlots;

		Kind(CookingStation station, int[][] slots, int containerSlot, int outputSlot, int... byproductSlots) {
			this.station = station;
			this.size = slots.length;
			this.slots = slots;
			this.containerSlot = containerSlot;
			this.outputSlot = outputSlot;
			this.byproductSlots = byproductSlots;
			this.byproductSlot = byproductSlots.length == 0 ? -1 : byproductSlots[0];
		}

		boolean takeOnly(int slot) {
			if (slot < 0) return false;
			if (slot == outputSlot) return true;
			for (int byproduct : byproductSlots) if (slot == byproduct) return true;
			return false;
		}

		/** The screen for a station's recipes, or null for stations without one, such as the skillet. */
		static Kind of(CookingStation station) {
			for (Kind kind : values()) if (kind.station == station) return kind;
			return null;
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
			case ProcessStationBlockEntity process -> process.data;
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
		} else if (kind.containerSlot >= 0 && (stack.is(Items.BOWL) || stack.is(Items.GLASS_BOTTLE))) {
			if (!moveItemStackTo(stack, kind.containerSlot, kind.containerSlot + 1, false)
				&& !moveItemStackTo(stack, 0, kind.station.ingredientSlots(), false)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, kind.station.ingredientSlots(), false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return original;
	}
}
