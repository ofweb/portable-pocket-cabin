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

/** An oven bakes and dries with its own fire. Like a furnace, it lights fuel only when a recipe can progress. */
final class OvenBlockEntity extends FiredStationBlockEntity implements WorldlyContainer {
	static final int FUEL_SLOT = 6;
	static final int OUTPUT_SLOT = 7;
	static final int BYPRODUCT_SLOT = 8;
	private static final int[] TOP = {0, 1, 2, 3, 4, 5};
	private static final int[] SIDE = {FUEL_SLOT};
	private static final int[] BOTTOM = {OUTPUT_SLOT, BYPRODUCT_SLOT, FUEL_SLOT};
	private final CookingProcess process = new CookingProcess(CookingStation.OVEN, -1, OUTPUT_SLOT, BYPRODUCT_SLOT);
	final ContainerData data = new CookingMenu.Data() {
		@Override
		public int get(int index) {
			return switch (index) {
				case CookingMenu.DATA_PROGRESS -> process.progress();
				case CookingMenu.DATA_TIME -> process.time();
				case CookingMenu.DATA_HEAT -> burner.heat().map(heat -> heat.ordinal() + 1).orElse(0);
				case CookingMenu.DATA_NEEDED_HEAT -> process.neededHeat().map(heat -> heat.ordinal() + 1).orElse(0);
				case CookingMenu.DATA_LIT_TIME -> burner.litTime();
				case CookingMenu.DATA_LIT_DURATION -> burner.litDuration();
				default -> 0;
			};
		}
	};

	OvenBlockEntity(BlockPos pos, BlockState state) {
		super(CookingBlocks.OVEN_ENTITY, pos, state, 9, FUEL_SLOT);
	}

	int progress() {
		return process.progress();
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, OvenBlockEntity oven) {
		oven.burn(oven.process.wantsHeat(level, oven, CookingInput.Liquid.NONE));
		int before = oven.process.progress();
		oven.process.tick(level, oven, CookingInput.Liquid.NONE, oven.burner.heat());
		if (before != oven.process.progress()) oven.saveChanged();
	}

	@Override
	protected void contentsChanged() {
		process.markDirty();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < FUEL_SLOT || slot == FUEL_SLOT && isFuel(stack);
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
		return slot == OUTPUT_SLOT || slot == BYPRODUCT_SLOT || slot == FUEL_SLOT && !isFuel(stack);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		process.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		process.load(input);
	}
}
