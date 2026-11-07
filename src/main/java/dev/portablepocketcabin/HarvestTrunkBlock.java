package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/**
 * A tree trunk with its own harvest. Cinnamon trunks give bark to an axe and grow it back. Palm trunks fill with sap
 * that a glass bottle taps. {@code ready} is true while there is bark or sap to take.
 */
final class HarvestTrunkBlock extends RotatedPillarBlock implements CookingPlant {
	static final BooleanProperty READY = BooleanProperty.create("ready");

	/** How a trunk is harvested: with an axe that strips the bark, or with a glass bottle that taps the sap. */
	enum Harvest {
		BARK,
		SAP
	}

	private final Harvest harvest;
	private final Supplier<Item> gives;
	private final MapCodec<HarvestTrunkBlock> codec;

	HarvestTrunkBlock(Properties properties, Harvest harvest, Supplier<Item> gives) {
		super(properties);
		this.harvest = harvest;
		this.gives = gives;
		this.codec = simpleCodec(p -> new HarvestTrunkBlock(p, harvest, gives));
		// A grown cinnamon trunk has its bark. A palm trunk fills with sap over time.
		registerDefaultState(defaultBlockState().setValue(READY, harvest == Harvest.BARK));
	}

	@Override
	public MapCodec<? extends RotatedPillarBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(READY);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(READY, harvest == Harvest.BARK);
	}

	Harvest harvest() {
		return harvest;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return !state.getValue(READY);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(READY) && random.nextFloat() < needs().growthChance()) {
			level.setBlock(pos, state.setValue(READY, true), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hit) {
		boolean tool = harvest == Harvest.BARK ? stack.is(ItemTags.AXES) : stack.is(Items.GLASS_BOTTLE);
		if (!tool || !state.getValue(READY)) return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (level instanceof ServerLevel server) {
			ItemStack taken = new ItemStack(gives.get(), harvest == Harvest.BARK ? 1 + server.getRandom().nextInt(2) : 1);
			SoundEvent sound;
			if (harvest == Harvest.BARK) {
				popResource(level, pos, taken);
				stack.hurtAndBreak(1, player, hand);
				sound = SoundEvents.AXE_STRIP;
			} else {
				stack.consume(1, player);
				if (!player.getInventory().add(taken)) player.drop(taken, false);
				sound = SoundEvents.BOTTLE_FILL;
			}
			level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.setBlock(pos, state.setValue(READY, false), Block.UPDATE_CLIENTS);
		}
		return InteractionResult.SUCCESS;
	}
}
