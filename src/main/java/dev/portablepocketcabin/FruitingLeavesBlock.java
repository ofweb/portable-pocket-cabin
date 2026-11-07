package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/**
 * The leaves of a fruit tree. They flower (age 1), then bear fruit (age 2). Using fruiting leaves picks the fruit
 * without breaking the tree, and the leaves flower and fruit again. Leaves still decay like vanilla leaves. A species
 * with a later fruit, the olive, ripens one stage further: green olives at age 2, black olives at age 3.
 */
class FruitingLeavesBlock extends UntintedParticleLeavesBlock implements CookingPlant {
	static final int FRUITING = 2;
	static final IntegerProperty AGE = BlockStateProperties.AGE_2;
	private final Supplier<Item> fruit;
	private final MapCodec<FruitingLeavesBlock> codec;

	FruitingLeavesBlock(Properties properties, Supplier<Item> fruit) {
		super(0.01F, ParticleTypes.CHERRY_LEAVES, properties);
		this.fruit = fruit;
		this.codec = simpleCodec(p -> new FruitingLeavesBlock(p, fruit));
		registerDefaultState(defaultBlockState().setValue(stage(), 0));
	}

	/** The leaves' growth stage. Called while the block is still being built, so overrides return a constant. */
	IntegerProperty stage() {
		return AGE;
	}

	int lastStage() {
		return FRUITING;
	}

	/** What picking gives at a stage, or null before the leaves fruit. */
	Item fruitAt(int stage) {
		return stage >= FRUITING ? fruit.get() : null;
	}

	@Override
	public MapCodec<UntintedParticleLeavesBlock> codec() {
		@SuppressWarnings({"unchecked", "rawtypes"})
		MapCodec<UntintedParticleLeavesBlock> cast = (MapCodec) codec;
		return cast;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(stage());
	}

	Item fruit() {
		return fruit.get();
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return super.isRandomlyTicking(state) || state.getValue(stage()) < lastStage();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.randomTick(state, level, pos, random);
		BlockState now = level.getBlockState(pos);
		if (!now.is(this) || now.getValue(stage()) >= lastStage()) return;
		if (level.getRawBrightness(pos, 0) >= needs().light() && random.nextFloat() < needs().growthChance()) {
			level.setBlock(pos, now.setValue(stage(), now.getValue(stage()) + 1), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		Item picked = fruitAt(state.getValue(stage()));
		if (picked == null) return InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			popResource(level, pos, new ItemStack(picked, 1 + server.getRandom().nextInt(2)));
			level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.setBlock(pos, state.setValue(stage(), 0), Block.UPDATE_CLIENTS);
		}
		return InteractionResult.SUCCESS;
	}

	/** The leaves' state as a tree places them: unflowered and not persistent, so they decay without a trunk. */
	BlockState natural() {
		return defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
	}

	/** Leaves whose fruit ripens one stage further, as olives turn from green to black on the tree. */
	static final class TwoStageFruit extends FruitingLeavesBlock {
		static final IntegerProperty STAGE = BlockStateProperties.AGE_3;
		private final Supplier<Item> ripe;
		private final MapCodec<TwoStageFruit> twoStageCodec;

		TwoStageFruit(Properties properties, Supplier<Item> early, Supplier<Item> ripe) {
			super(properties, early);
			this.ripe = ripe;
			this.twoStageCodec = simpleCodec(p -> new TwoStageFruit(p, early, ripe));
		}

		@Override
		public MapCodec<UntintedParticleLeavesBlock> codec() {
			@SuppressWarnings({"unchecked", "rawtypes"})
			MapCodec<UntintedParticleLeavesBlock> cast = (MapCodec) twoStageCodec;
			return cast;
		}

		@Override
		IntegerProperty stage() {
			return STAGE;
		}

		@Override
		int lastStage() {
			return FRUITING + 1;
		}

		@Override
		Item fruitAt(int stage) {
			return stage > FRUITING ? ripe.get() : super.fruitAt(stage);
		}
	}
}
