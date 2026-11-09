package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Compost and rich ground (B-0054). The composter makes compost instead of bone meal. Compost spread on farmland makes
 * rich farmland, and on dirt or grass makes rich soil. A plant on rich ground gets an extra random tick each time the
 * ground ticks, so it grows about twice as fast. Each ripe harvest spends one of the ground's four charges; at none left
 * it is plain farmland or dirt again, and more compost tops it up.
 */
final class RichSoil {
	static final int MAX_CHARGE = 4;
	static final IntegerProperty CHARGE = IntegerProperty.create("charge", 1, MAX_CHARGE);

	static final Block RICH_SOIL = CookingBlocks.register("rich_soil", Soil::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.ROOTED_DIRT).randomTicks());
	static final Block RICH_FARMLAND = CookingBlocks.register("rich_farmland", Farmland::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.DIRT).strength(0.6F).sound(SoundType.GRAVEL).randomTicks().isViewBlocking((state, level, pos) -> true)
		.isSuffocating((state, level, pos) -> true));
	static final Item COMPOST = registerItem("compost", Compost::new);

	private RichSoil() {
	}

	private static Item registerItem(String name, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, PortablePocketCabin.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	static void register() {
		// A player breaking a ripe crop on rich ground spends one of its charges.
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel server && ripe(state)) spend(server, pos.below());
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			entries.accept(COMPOST);
			entries.accept(RICH_SOIL);
			entries.accept(RICH_FARMLAND);
		});
	}

	/** Whether a broken plant was ripe: a crop at full age, or a cooking plant at its last stage. */
	static boolean ripe(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
		for (var property : state.getProperties()) {
			if (property instanceof IntegerProperty age && age.getName().equals("age") && state.getBlock() instanceof CookingPlant) {
				return state.getValue(age).equals(age.getPossibleValues().stream().max(Integer::compare).orElse(0));
			}
		}
		return false;
	}

	static boolean isRich(BlockState state) {
		return state.is(RICH_SOIL) || state.is(RICH_FARMLAND);
	}

	/** The plain ground a rich block stands for when a plant checks its soil: farmland or dirt. */
	static BlockState plain(BlockState state) {
		if (state.is(RICH_FARMLAND)) return Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, state.getValue(FarmlandBlock.MOISTURE));
		if (state.is(RICH_SOIL)) return Blocks.DIRT.defaultBlockState();
		return state;
	}

	/** Spends one charge of the rich ground at {@code pos}, if it is rich. With none left it turns plain. */
	static void spend(LevelAccessor level, BlockPos pos) {
		BlockState ground = level.getBlockState(pos);
		if (!isRich(ground)) return;
		int charge = ground.getValue(CHARGE);
		level.setBlock(pos, charge > 1 ? ground.setValue(CHARGE, charge - 1) : plain(ground), Block.UPDATE_ALL);
	}

	/** Gives the plant above rich ground one extra random tick. Its own growing needs still apply. */
	static void feed(ServerLevel level, BlockPos ground, RandomSource random) {
		BlockPos above = ground.above();
		BlockState plant = level.getBlockState(above);
		if (plant.isRandomlyTicking() && !isRich(plant)) plant.randomTick(level, above, random);
	}

	/** Turns rich farmland that dries out or is trampled into rich soil with the same charge. */
	static boolean toRichSoil(BlockState farmland, Level level, BlockPos pos) {
		if (!farmland.is(RICH_FARMLAND)) return false;
		BlockState soil = Block.pushEntitiesUp(farmland, RICH_SOIL.defaultBlockState().setValue(CHARGE, farmland.getValue(CHARGE)), level, pos);
		level.setBlockAndUpdate(pos, soil);
		return true;
	}

	/** Rich soil: dirt fed with compost. A hoe tills it into rich farmland with the same charge. */
	static final class Soil extends Block {
		static final MapCodec<Soil> CODEC = simpleCodec(Soil::new);

		Soil(Properties properties) {
			super(properties);
			registerDefaultState(stateDefinition.any().setValue(CHARGE, MAX_CHARGE));
		}

		@Override
		protected MapCodec<? extends Block> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(CHARGE);
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			feed(level, pos, random);
		}

		@Override
		protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
			if (!stack.is(ItemTags.HOES) || hit.getDirection() == net.minecraft.core.Direction.DOWN || !level.getBlockState(pos.above()).isAir()) {
				return super.useItemOn(stack, state, level, pos, player, hand, hit);
			}
			level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			if (!level.isClientSide()) {
				level.setBlock(pos, RICH_FARMLAND.defaultBlockState().setValue(CHARGE, state.getValue(CHARGE)), Block.UPDATE_ALL_IMMEDIATE);
				stack.hurtAndBreak(1, player, hand);
			}
			return InteractionResult.SUCCESS;
		}
	}

	/** Rich farmland: farmland fed with compost. Dried out or trampled, it becomes rich soil. */
	static final class Farmland extends FarmlandBlock {
		static final MapCodec<FarmlandBlock> CODEC = simpleCodec(Farmland::new);

		Farmland(Properties properties) {
			super(properties);
			registerDefaultState(defaultBlockState().setValue(CHARGE, MAX_CHARGE));
		}

		@Override
		public MapCodec<FarmlandBlock> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(CHARGE);
		}

		@Override
		public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
			BlockState placed = super.getStateForPlacement(context);
			return placed != null && placed.is(Blocks.DIRT) ? RICH_SOIL.defaultBlockState() : placed;
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			super.randomTick(state, level, pos, random);
			if (level.getBlockState(pos).is(this)) feed(level, pos, random);
		}
	}

	/** Compost: spread on farmland, dirt, or grass, it makes the ground rich; on rich ground it tops up the charge. */
	static final class Compost extends Item {
		Compost(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			Level level = context.getLevel();
			BlockPos pos = context.getClickedPos();
			BlockState ground = level.getBlockState(pos);
			BlockState rich;
			if (ground.is(Blocks.FARMLAND)) {
				rich = RICH_FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, ground.getValue(FarmlandBlock.MOISTURE));
			} else if (ground.is(Blocks.DIRT) || ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.COARSE_DIRT)) {
				rich = RICH_SOIL.defaultBlockState();
			} else if (isRich(ground) && ground.getValue(CHARGE) < MAX_CHARGE) {
				rich = ground.setValue(CHARGE, MAX_CHARGE);
			} else {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide()) {
				level.setBlockAndUpdate(pos, rich);
				level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos.above(), 15);
				context.getItemInHand().consume(1, context.getPlayer());
			}
			return InteractionResult.SUCCESS;
		}
	}
}
