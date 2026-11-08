package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A seaweed or shellfish bed that grows underwater, such as wakame, laver, sea lettuce, mussels, or oysters. It stands
 * in water on a solid floor like seagrass and grows through four stages. A grown one slowly spreads to water beside it.
 * Breaking a grown seaweed gives two or three, and bone meal helps it grow. Shellfish beds are gathered by hand instead:
 * a grown bed gives two or three and goes back to its second stage, as a picked bush does.
 */
final class SeaPlantBlock extends SeagrassBlock implements CookingPlant {
	static final MapCodec<SeaPlantBlock> CODEC = simpleCodec(SeaPlantBlock::new);
	static final MapCodec<SeaPlantBlock> SHELLFISH_CODEC = simpleCodec(SeaPlantBlock::shellfish);
	static final IntegerProperty AGE = BlockStateProperties.AGE_3;
	static final int MAX_AGE = 3;
	/** Shellfish beds, mussels and oysters, are gathered by hand rather than broken. */
	private final boolean gatheredByHand;

	SeaPlantBlock(Properties properties) {
		this(properties, false);
	}

	private SeaPlantBlock(Properties properties, boolean gatheredByHand) {
		super(properties);
		this.gatheredByHand = gatheredByHand;
		registerDefaultState(stateDefinition.any().setValue(AGE, 0));
	}

	static SeaPlantBlock shellfish(Properties properties) {
		return new SeaPlantBlock(properties, true);
	}

	boolean gatheredByHand() {
		return gatheredByHand;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public MapCodec<SeagrassBlock> codec() {
		return (MapCodec) (gatheredByHand ? SHELLFISH_CODEC : CODEC);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	/** Whether the plant stands in a water source with enough light to grow. Sea plants need no soil, as seagrass. */
	boolean canGrow(LevelReader level, BlockPos pos) {
		var fluid = level.getFluidState(pos);
		return fluid.is(FluidTags.WATER) && fluid.isSource() && level.getRawBrightness(pos, 0) >= needs().light();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!canGrow(level, pos)) return;
		int age = state.getValue(AGE);
		if (age < MAX_AGE) {
			if (random.nextFloat() < needs().growthChance(level, pos)) level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
		} else if (random.nextFloat() < needs().spreadChance()) {
			spread(level, pos, random);
		}
	}

	/** Plants a young one in one of the eight water blocks around, at the same height or one below, if it can live there. */
	void spread(ServerLevel level, BlockPos pos, RandomSource random) {
		int pick = random.nextInt(8);
		int dx = new int[] {-1, 0, 1, -1, 1, -1, 0, 1}[pick];
		int dz = new int[] {-1, -1, -1, 0, 0, 1, 1, 1}[pick];
		for (int dy = 0; dy >= -1; dy--) {
			BlockPos target = pos.offset(dx, dy, dz);
			BlockState there = level.getBlockState(target);
			var fluid = level.getFluidState(target);
			if (!there.is(net.minecraft.world.level.block.Blocks.WATER) || !fluid.isSource()) continue;
			BlockState young = defaultBlockState();
			if (young.canSurvive(level, target)) {
				level.setBlock(target, young, Block.UPDATE_ALL);
				return;
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!gatheredByHand) return super.useWithoutItem(state, level, pos, player, hit);
		if (state.getValue(AGE) < MAX_AGE) return InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			popResource(level, pos, new ItemStack(asItem(), 2 + server.getRandom().nextInt(2)));
			level.setBlock(pos, state.setValue(AGE, 1), Block.UPDATE_CLIENTS);
			level.playSound(null, pos, SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 0.8F, 1.2F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return !gatheredByHand && state.getValue(AGE) < MAX_AGE;
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
