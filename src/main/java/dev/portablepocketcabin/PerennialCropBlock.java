package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/**
 * A perennial crown, asparagus or rhubarb, harvested again and again from one planting. Using a ripe plant cuts one or
 * two of its harvest and sets it back to its first grown stage. Breaking a ripe plant divides it into two crowns.
 */
final class PerennialCropBlock extends FieldCropBlock {
	/** The stage a cut plant goes back to, from which it grows the harvest again. */
	static final int CUT_BACK = 4;
	private final Supplier<Item> harvest;
	private final MapCodec<PerennialCropBlock> codec;

	PerennialCropBlock(Properties properties, Supplier<? extends ItemLike> crowns, Supplier<Item> harvest) {
		super(properties, crowns);
		this.harvest = harvest;
		this.codec = simpleCodec(p -> new PerennialCropBlock(p, crowns, harvest));
	}

	@Override
	public MapCodec<? extends CropBlock> codec() {
		return codec;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!isMaxAge(state)) return InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			popResource(level, pos, new ItemStack(harvest.get(), 1 + server.getRandom().nextInt(2)));
			level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.setBlock(pos, getStateForAge(CUT_BACK), Block.UPDATE_CLIENTS);
			RichSoil.spend(level, pos.below());
		}
		return InteractionResult.SUCCESS;
	}
}
