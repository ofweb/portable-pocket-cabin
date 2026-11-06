package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/** A station with its own fire and fuel slot: the stove and the oven. */
abstract class FiredStationBlockEntity extends CookingStationBlockEntity {
	protected final CookingBurner burner = new CookingBurner();
	private final int fuelSlot;

	FiredStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, int fuelSlot) {
		super(type, pos, state, size);
		this.fuelSlot = fuelSlot;
	}

	Optional<CookingHeat> heat() {
		return burner.heat();
	}

	CookingBurner burner() {
		return burner;
	}

	/** Burns down the fire and lights new fuel when {@code wanted}. */
	protected void burn(boolean wanted) {
		if (level != null && burner.tick(wanted, this, fuelSlot, level.fuelValues())) {
			updateLit();
			saveChanged();
		}
	}

	/** Puts the fire out. Returns false when it was not burning. */
	boolean extinguish() {
		if (!burner.extinguish()) return false;
		updateLit();
		saveChanged();
		return true;
	}

	private void updateLit() {
		if (level != null && getBlockState().getValue(BlockStateProperties.LIT) != burner.burning()) {
			level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, burner.burning()), 3);
		}
	}

	protected boolean isFuel(ItemStack stack) {
		return level != null && level.fuelValues().isFuel(stack);
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
