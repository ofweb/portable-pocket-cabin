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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * A climbing crop on a trellis: cucumber, grapes, hops, tomatoes, and more. It is planted only by using its seeds on
 * a trellis. Picking a ripe vine gives its fruit and sets it back two stages, so it fruits again without replanting.
 */
final class TrellisCropBlock extends FieldCropBlock {
	static final int PICKED_BACK = 2;
	private final Supplier<Item> produce;
	private final MapCodec<TrellisCropBlock> codec;

	TrellisCropBlock(Properties properties, Supplier<? extends ItemLike> seeds, Supplier<Item> produce) {
		super(properties, seeds);
		this.produce = produce;
		this.codec = simpleCodec(p -> new TrellisCropBlock(p, seeds, produce));
	}

	@Override
	public MapCodec<? extends CropBlock> codec() {
		return codec;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return TrellisBlock.SHAPE;
	}

	/** A vine is never placed directly: its seeds are used on a trellis instead. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!isMaxAge(state)) return InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			int count = 1 + server.getRandom().nextInt(2);
			popResource(level, pos, new ItemStack(produce.get(), count));
			level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.setBlock(pos, getStateForAge(getMaxAge() - PICKED_BACK), Block.UPDATE_CLIENTS);
		}
		return InteractionResult.SUCCESS;
	}
}
