package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/** A stove burns fuel to heat the pot or skillet above it. */
final class StoveBlockEntity extends CookingStationBlockEntity implements WorldlyContainer {
	static final int FUEL_SLOT = 0;
	private static final int[] SLOTS = {FUEL_SLOT};
	private final CookingBurner burner = new CookingBurner();
	final ContainerData data = new CookingMenu.Data() {
		@Override
		public int get(int index) {
			return switch (index) {
				case CookingMenu.DATA_HEAT -> burner.heat().map(heat -> heat.ordinal() + 1).orElse(0);
				case CookingMenu.DATA_LIT_TIME -> burner.litTime();
				case CookingMenu.DATA_LIT_DURATION -> burner.litDuration();
				default -> 0;
			};
		}
	};

	StoveBlockEntity(BlockPos pos, BlockState state) {
		super(CookingBlocks.STOVE_ENTITY, pos, state, 1);
	}

	Optional<CookingHeat> heat() {
		return burner.heat();
	}

	CookingBurner burner() {
		return burner;
	}

	boolean extinguish() {
		if (!burner.extinguish()) return false;
		updateLit();
		saveChanged();
		return true;
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, StoveBlockEntity stove) {
		boolean wanted = level.getBlockEntity(pos.above()) instanceof CookingVessel vessel && vessel.wantsHeat(level);
		if (stove.burner.tick(wanted, stove, FUEL_SLOT, level.fuelValues())) {
			stove.updateLit();
			stove.saveChanged();
		}
	}

	private void updateLit() {
		if (level != null && getBlockState().getValue(BlockStateProperties.LIT) != burner.burning()) {
			level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, burner.burning()), 3);
		}
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return level != null && level.fuelValues().isFuel(stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return canPlaceItem(slot, stack);
	}

	/** Only spent fuel, such as the empty bucket a lava bucket leaves, comes back out. */
	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return level != null && !level.fuelValues().isFuel(stack);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		burner.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		burner.load(input);
	}
}
