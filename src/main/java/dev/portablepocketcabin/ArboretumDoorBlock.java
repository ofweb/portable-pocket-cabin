package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A door between the cabin and its arboretum grove (B-0023). It never opens: using it walks the player through. The
 * cabin's door leads into the grove, and the grove's exit door leads back to the corridor.
 */
final class ArboretumDoorBlock extends DoorBlock {
	private final boolean exit;
	private final MapCodec<ArboretumDoorBlock> codec;

	ArboretumDoorBlock(boolean exit, Properties properties) {
		super(BlockSetType.SPRUCE, properties);
		this.exit = exit;
		this.codec = simpleCodec(p -> new ArboretumDoorBlock(exit, p));
	}

	@Override
	public MapCodec<? extends DoorBlock> codec() { return codec; }

	/** Walks the player through, from either half of the door. */
	InteractionResult use(ServerPlayer player, BlockPos pos, BlockState state) {
		BlockPos lower = state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? pos : pos.below();
		return exit ? CabinArboretum.leave(player, lower) : CabinArboretum.enter(player, lower);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) return use(serverPlayer, pos, state);
		return InteractionResult.SUCCESS;
	}

	/** Redstone does not open it. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean movedByPiston) { }
}
