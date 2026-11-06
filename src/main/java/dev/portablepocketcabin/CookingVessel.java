package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CampfireBlock;

import java.util.Optional;

/** A pot or skillet that cooks on a heat source below it. */
interface CookingVessel {
	/** Whether the vessel has a recipe that heat would advance, so the stove below should light fuel. */
	boolean wantsHeat(ServerLevel level);

	/** The heat a vessel at {@code pos} receives: a burning stove's fuel heat, or low heat from a lit campfire. */
	static Optional<CookingHeat> heatAt(ServerLevel level, BlockPos pos) {
		BlockPos below = pos.below();
		if (level.getBlockEntity(below) instanceof StoveBlockEntity stove) return stove.heat();
		return CampfireBlock.isLitCampfire(level.getBlockState(below)) ? Optional.of(CookingHeat.LOW) : Optional.empty();
	}
}
