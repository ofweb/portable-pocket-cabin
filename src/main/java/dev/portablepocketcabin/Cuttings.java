package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Taking a cutting with shears from a grown bush or vine that is planted from cuttings (B-0054). */
final class Cuttings {
	static final int SET_BACK = 2;

	private Cuttings() {
	}

	/** The cutting that plants this bush or vine. */
	static Item of(Block plant) {
		return CookingPlants.PLANTED_FROM.get(BuiltInRegistries.BLOCK.getKey(plant).getPath());
	}

	/**
	 * Gives one cutting from a plant at least {@code grown} old, sets it back two stages, and wears the shears. A
	 * younger plant gives nothing.
	 */
	static InteractionResult take(BlockState state, Level level, BlockPos pos, Player player, ItemStack shears, InteractionHand hand,
		IntegerProperty age, int grown) {
		int current = state.getValue(age);
		if (current < grown) return InteractionResult.PASS;
		if (level instanceof ServerLevel) {
			Block.popResource(level, pos, new ItemStack(of(state.getBlock())));
			level.setBlock(pos, state.setValue(age, Math.max(0, current - SET_BACK)), Block.UPDATE_CLIENTS);
			level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.2F);
			shears.hurtAndBreak(1, player, hand);
		}
		return InteractionResult.SUCCESS;
	}
}
