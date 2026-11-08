package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cardoon, a wild thistle of meadows and savannas. It grows through four stages. A perennial crown: using a mature plant
 * cuts its purple flowers, which steep into vegetable rennet, and sets it back to flower again. Breaking a mature plant
 * divides it into two cardoons to replant, with its flowers.
 */
final class CardoonBlock extends VegetationBlock implements BonemealableBlock {
	static final MapCodec<CardoonBlock> CODEC = simpleCodec(CardoonBlock::new);
	static final int MAX_AGE = 3;
	static final IntegerProperty AGE = BlockStateProperties.AGE_3;
	private static final VoxelShape[] SHAPES = {
		Block.box(4, 0, 4, 12, 6, 12), Block.box(3, 0, 3, 13, 10, 13), Block.box(2, 0, 2, 14, 14, 14), Block.box(2, 0, 2, 14, 16, 14)
	};

	CardoonBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AGE, 0));
	}

	@Override
	protected MapCodec<? extends VegetationBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(AGE)];
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(AGE) < MAX_AGE;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(AGE) < MAX_AGE && random.nextInt(5) == 0 && level.getRawBrightness(pos.above(), 0) >= 9) {
			level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
		net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
		if (state.getValue(AGE) < MAX_AGE) return net.minecraft.world.InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			popResource(level, pos, new net.minecraft.world.item.ItemStack(CookingItems.CARDOON_FLOWER, 1 + server.getRandom().nextInt(2)));
			level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CROP_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
			level.setBlock(pos, state.setValue(AGE, MAX_AGE - 1), Block.UPDATE_CLIENTS);
			RichSoil.spend(level, pos.below());
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return state.getValue(AGE) < MAX_AGE;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), Block.UPDATE_CLIENTS);
	}
}
