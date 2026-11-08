package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A seaweed foraged underwater, such as wakame, laver, or sea lettuce. It stands in water on a solid floor like
 * seagrass, and breaking it gives the seaweed, which plants it again in water. It doesn't grow tall with bone meal.
 */
final class SeaPlantBlock extends SeagrassBlock {
	static final MapCodec<SeaPlantBlock> CODEC = simpleCodec(SeaPlantBlock::new);
	static final MapCodec<SeaPlantBlock> SHELLFISH_CODEC = simpleCodec(SeaPlantBlock::shellfish);
	/** Shellfish beds, mussels and oysters, are gathered by hand: using one gives its shellfish and leaves water. */
	private final boolean gatheredByHand;

	SeaPlantBlock(Properties properties) {
		this(properties, false);
	}

	private SeaPlantBlock(Properties properties, boolean gatheredByHand) {
		super(properties);
		this.gatheredByHand = gatheredByHand;
	}

	static SeaPlantBlock shellfish(Properties properties) {
		return new SeaPlantBlock(properties, true);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public MapCodec<SeagrassBlock> codec() {
		return (MapCodec) (gatheredByHand ? SHELLFISH_CODEC : CODEC);
	}

	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
		net.minecraft.world.phys.BlockHitResult hit) {
		if (!gatheredByHand) return super.useWithoutItem(state, level, pos, player, hit);
		if (level instanceof ServerLevel server) {
			popResource(level, pos, new net.minecraft.world.item.ItemStack(asItem(), 1 + server.getRandom().nextInt(2)));
			level.setBlock(pos, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
			level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_FILL_FISH, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return false;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return false;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
	}
}
