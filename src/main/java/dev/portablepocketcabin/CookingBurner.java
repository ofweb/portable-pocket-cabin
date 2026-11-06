package dev.portablepocketcabin;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/**
 * The fire of a stove or oven. New fuel lights only when there is work that needs heat, but lit fuel always
 * burns down, so leftover heat at the wrong level clears on its own. A shovel puts the fire out at once.
 */
final class CookingBurner {
	private int litTime;
	private int litDuration;
	private CookingHeat heat;

	boolean burning() {
		return litTime > 0;
	}

	Optional<CookingHeat> heat() {
		return burning() ? Optional.ofNullable(heat) : Optional.empty();
	}

	int litTime() {
		return litTime;
	}

	int litDuration() {
		return litDuration;
	}

	/** Burns down the current fuel and lights the next when needed. Returns whether anything changed. */
	boolean tick(boolean wanted, Container container, int fuelSlot, FuelValues fuelValues) {
		boolean changed = false;
		if (litTime > 0) {
			litTime--;
			if (litTime == 0) heat = null;
			changed = true;
		}
		if (litTime == 0 && wanted) {
			ItemStack fuel = container.getItem(fuelSlot);
			Optional<CookingHeat> level = CookingHeat.ofFuel(fuel, fuelValues);
			int duration = level.isPresent() ? fuelValues.burnDuration(fuel) : 0;
			if (duration > 0) {
				heat = level.get();
				litTime = litDuration = duration;
				ItemStackTemplate remainder = fuel.getCraftingRemainder();
				fuel.shrink(1);
				if (fuel.isEmpty() && remainder != null) container.setItem(fuelSlot, remainder.create());
				changed = true;
			}
		}
		return changed;
	}

	/** Puts the fire out. The burning fuel is lost. */
	boolean extinguish() {
		if (!burning()) return false;
		litTime = 0;
		heat = null;
		return true;
	}

	void save(ValueOutput output) {
		output.putInt("lit_time", litTime);
		output.putInt("lit_duration", litDuration);
		output.storeNullable("heat", CookingHeat.CODEC, heat);
	}

	void load(ValueInput input) {
		litTime = input.getIntOr("lit_time", 0);
		litDuration = input.getIntOr("lit_duration", 0);
		heat = input.read("heat", CookingHeat.CODEC).orElse(null);
	}
}
