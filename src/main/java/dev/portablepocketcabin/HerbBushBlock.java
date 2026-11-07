package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * A perennial plant that regrows after picking, like a sweet berry bush: thyme, strawberries, raspberries, and oyster
 * mushrooms. A two-season bush, the elder, gives one harvest at its flowering stage and another when it fruits.
 * Picking sets it back to its first grown stage.
 */
final class HerbBushBlock extends VegetationBlock implements BonemealableBlock, CookingPlant {
	static final int MAX_AGE = 3;
	static final IntegerProperty AGE = BlockStateProperties.AGE_3;
	private static final VoxelShape YOUNG = Block.box(3, 0, 3, 13, 8, 13);
	private static final VoxelShape GROWN = Block.box(1, 0, 1, 15, 16, 15);
	/** What a picking gives at the flowering stage (age 2), or null; and at the ripe stage (age 3). */
	private final Supplier<Item> flowering;
	private final Supplier<Item> ripe;
	private final MapCodec<HerbBushBlock> codec;

	HerbBushBlock(Properties properties, Supplier<Item> flowering, Supplier<Item> ripe) {
		super(properties);
		this.flowering = flowering;
		this.ripe = ripe;
		this.codec = simpleCodec(p -> new HerbBushBlock(p, flowering, ripe));
		registerDefaultState(stateDefinition.any().setValue(AGE, 0));
	}

	@Override
	protected MapCodec<? extends VegetationBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AGE) < 2 ? YOUNG : GROWN;
	}

	/** Planted on its soil, or found wild on ground that supports vegetation, such as grass and dirt. */
	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return needs().soilMatches(state) || state.is(CookingPlants.WILD_GROUND);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(AGE) < MAX_AGE;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int age = state.getValue(AGE);
		if (age < MAX_AGE && needs().canGrow(level, pos) && random.nextFloat() < needs().growthChance()) {
			level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
		}
	}

	/** What picking the bush gives now, or null when there is nothing to pick. */
	Item harvest(BlockState state) {
		int age = state.getValue(AGE);
		if (age == MAX_AGE) return ripe.get();
		if (age == 2 && flowering != null) return flowering.get();
		return null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		Item harvest = harvest(state);
		if (harvest == null) return InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			popResource(level, pos, new ItemStack(harvest, 1 + server.getRandom().nextInt(2)));
			level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F + server.getRandom().nextFloat() * 0.2F);
			level.setBlock(pos, state.setValue(AGE, 1), Block.UPDATE_CLIENTS);
		}
		return InteractionResult.SUCCESS;
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
