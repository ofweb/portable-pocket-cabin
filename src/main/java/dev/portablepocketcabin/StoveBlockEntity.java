package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** A stove burns fuel to heat the pot or skillet above it. */
final class StoveBlockEntity extends FiredStationBlockEntity implements WorldlyContainer {
	static final int FUEL_SLOT = 0;
	private static final int[] SLOTS = {FUEL_SLOT};
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
		super(CookingBlocks.STOVE_ENTITY, pos, state, 1, FUEL_SLOT);
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, StoveBlockEntity stove) {
		stove.burn(level.getBlockEntity(pos.above()) instanceof CookingVessel vessel && vessel.wantsHeat(level));
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return isFuel(stack);
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
		return !isFuel(stack);
	}
}
