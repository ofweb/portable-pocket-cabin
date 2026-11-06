package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A cooking pot simmers, boils, and steams on a stove or campfire, and deep-fries in oil. It holds water or oil,
 * never both: a water bucket fills it for three recipes, and each oil bottle adds one level of oil.
 */
final class CookingPotBlockEntity extends CookingStationBlockEntity implements CookingVessel, WorldlyContainer {
	static final int CONTAINER_SLOT = 6;
	static final int OUTPUT_SLOT = 7;
	static final int BYPRODUCT_SLOT = 8;
	private static final int[] TOP = {0, 1, 2, 3, 4, 5};
	private static final int[] SIDE = {CONTAINER_SLOT};
	private static final int[] BOTTOM = {OUTPUT_SLOT, BYPRODUCT_SLOT};
	private final CookingProcess process = new CookingProcess(CookingStation.POT, CONTAINER_SLOT, OUTPUT_SLOT, BYPRODUCT_SLOT);
	/** The liquid level, from 0 to 3, and whether that liquid is oil rather than water. */
	private int level;
	private boolean oil;
	private CookingHeat lastHeat;
	final ContainerData data = new CookingMenu.Data() {
		@Override
		public int get(int index) {
			return switch (index) {
				case CookingMenu.DATA_PROGRESS -> process.progress();
				case CookingMenu.DATA_TIME -> process.time();
				case CookingMenu.DATA_WATER -> level;
				case CookingMenu.DATA_LIQUID -> oil ? 1 : 0;
				case CookingMenu.DATA_HEAT -> lastHeat == null ? 0 : lastHeat.ordinal() + 1;
				case CookingMenu.DATA_NEEDED_HEAT -> process.neededHeat().map(heat -> heat.ordinal() + 1).orElse(0);
				default -> 0;
			};
		}
	};

	CookingPotBlockEntity(BlockPos pos, BlockState state) {
		super(CookingBlocks.COOKING_POT_ENTITY, pos, state, 9);
	}

	int water() {
		return oil ? 0 : level;
	}

	int oil() {
		return oil ? level : 0;
	}

	CookingInput.Liquid liquid() {
		return new CookingInput.Liquid(water(), oil());
	}

	int progress() {
		return process.progress();
	}

	CookingProcess process() {
		return process;
	}

	/** Fills the pot from a water bucket. Returns false when it is full or holds oil. */
	boolean fillWater() {
		if (oil && level > 0 || !oil && level >= CookingStation.MAX_WATER) return false;
		oil = false;
		level = CookingStation.MAX_WATER;
		process.markDirty();
		setChanged();
		return true;
	}

	/** Adds one level of oil from an oil bottle. Returns false when it is full or holds water. */
	boolean fillOil() {
		if (!oil && level > 0 || level >= CookingStation.MAX_WATER) return false;
		oil = true;
		level++;
		process.markDirty();
		setChanged();
		return true;
	}

	@Override
	public boolean wantsHeat(ServerLevel level) {
		return process.wantsHeat(level, this, liquid());
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, CookingPotBlockEntity pot) {
		var heat = CookingVessel.heatAt(level, pos);
		pot.lastHeat = heat.orElse(null);
		int before = pot.process.progress();
		int used = pot.process.tick(level, pot, pot.liquid(), heat);
		if (used > 0) {
			pot.level -= used;
			pot.setChanged();
		} else if (before != pot.process.progress()) {
			pot.saveChanged();
		}
	}

	@Override
	protected void contentsChanged() {
		process.markDirty();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < OUTPUT_SLOT;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDE;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT || slot == BYPRODUCT_SLOT;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		// The level keeps B-0045's "water" key, so older pots load as water.
		output.putInt("water", level);
		output.putString("liquid", oil ? "oil" : "water");
		process.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		level = input.getIntOr("water", 0);
		oil = input.getStringOr("liquid", "water").equals("oil");
		process.load(input);
	}
}
