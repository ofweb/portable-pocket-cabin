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

/** A cooking pot simmers, boils, and steams on a stove or campfire. A water bucket fills it for three recipes. */
final class CookingPotBlockEntity extends CookingStationBlockEntity implements CookingVessel, WorldlyContainer {
	static final int CONTAINER_SLOT = 6;
	static final int OUTPUT_SLOT = 7;
	static final int BYPRODUCT_SLOT = 8;
	private static final int[] TOP = {0, 1, 2, 3, 4, 5};
	private static final int[] SIDE = {CONTAINER_SLOT};
	private static final int[] BOTTOM = {OUTPUT_SLOT, BYPRODUCT_SLOT};
	private final CookingProcess process = new CookingProcess(CookingStation.POT, CONTAINER_SLOT, OUTPUT_SLOT, BYPRODUCT_SLOT);
	private int water;
	private CookingHeat lastHeat;
	final ContainerData data = new CookingMenu.Data() {
		@Override
		public int get(int index) {
			return switch (index) {
				case CookingMenu.DATA_PROGRESS -> process.progress();
				case CookingMenu.DATA_TIME -> process.time();
				case CookingMenu.DATA_WATER -> water;
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
		return water;
	}

	int progress() {
		return process.progress();
	}

	/** Fills the pot from a water bucket. Returns false when the pot is already full. */
	boolean fillWater() {
		if (water >= CookingStation.MAX_WATER) return false;
		water = CookingStation.MAX_WATER;
		process.markDirty();
		setChanged();
		return true;
	}

	@Override
	public boolean wantsHeat(ServerLevel level) {
		return process.wantsHeat(level, this, water);
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, CookingPotBlockEntity pot) {
		var heat = CookingVessel.heatAt(level, pos);
		pot.lastHeat = heat.orElse(null);
		int before = pot.process.progress();
		int used = pot.process.tick(level, pot, pot.water, heat);
		if (used > 0) {
			pot.water -= used;
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
		output.putInt("water", water);
		process.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getIntOr("water", 0);
		process.load(input);
	}
}
