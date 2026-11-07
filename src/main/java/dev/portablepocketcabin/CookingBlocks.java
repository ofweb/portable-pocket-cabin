package dev.portablepocketcabin;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Set;
import java.util.function.Function;

final class CookingBlocks {
	static final Block STOVE = register("stove", CookingStationBlock.Stove::new, BlockBehaviour.Properties.of()
		.strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()
		.lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0));
	static final Block OVEN = register("oven", CookingStationBlock.Oven::new, BlockBehaviour.Properties.of()
		.strength(3.5F).sound(SoundType.STONE).requiresCorrectToolForDrops()
		.lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0));
	static final Block COOKING_POT = register("cooking_pot", CookingStationBlock.Pot::new, BlockBehaviour.Properties.of()
		.strength(2.0F).sound(SoundType.COPPER).noOcclusion());
	static final Block SKILLET = register("skillet", CookingStationBlock.Skillet::new, BlockBehaviour.Properties.of()
		.strength(1.0F).sound(SoundType.METAL).noOcclusion());

	static final Block CUTTING_BOARD = register("cutting_board", CookingStationBlock.CuttingBoard::new, BlockBehaviour.Properties.of()
		.strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());

	static final Block PREP_COUNTER = register("prep_counter", CookingStationBlock.PrepCounter::new, BlockBehaviour.Properties.of()
		.strength(2.5F).sound(SoundType.WOOD).ignitedByLava());
	static final Block STRAINER = register("strainer", CookingStationBlock.Strainer::new, BlockBehaviour.Properties.of()
		.strength(1.0F).sound(SoundType.METAL).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));

	static final Block GRILL = register("grill", CookingStationBlock.Grill::new, BlockBehaviour.Properties.of()
		.strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()
		.lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0));
	static final Block WOK = register("wok", CookingStationBlock.Wok::new, BlockBehaviour.Properties.of()
		.strength(1.0F).sound(SoundType.METAL).noOcclusion());

	static final Block AEBLESKIVE_PAN = register("aebleskive_pan", CookingStationBlock.AebleskivePan::new, BlockBehaviour.Properties.of()
		.strength(1.5F).sound(SoundType.METAL).noOcclusion());

	static final Block PRESS = register("press", CookingStationBlock.Press::new, BlockBehaviour.Properties.of()
		.strength(2.5F).sound(SoundType.WOOD).ignitedByLava());

	static final Block MILL = register("mill", properties -> new ProcessStationBlock(properties,
		ProcessStationBlock.Config.of(CookingMenu.Kind.MILL, () -> CookingBlocks.MILL_ENTITY)),
		BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
	static final Block MIXING_BOWL = register("mixing_bowl", properties -> new ProcessStationBlock.CounterTopStation(properties,
		ProcessStationBlock.Config.of(CookingMenu.Kind.MIXING_BOWL, () -> CookingBlocks.MIXING_BOWL_ENTITY)
			.shape(Block.box(3, 0, 3, 13, 6, 13)).tool(() -> CookingItems.SPOON, SoundEvents.WOOD_HIT)),
		BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion()
			.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
	static final Block MORTAR = register("mortar", properties -> new ProcessStationBlock.CounterTopStation(properties,
		ProcessStationBlock.Config.of(CookingMenu.Kind.MORTAR, () -> CookingBlocks.MORTAR_ENTITY)
			.shape(Block.box(4, 0, 4, 12, 6, 12)).tool(() -> CookingItems.PESTLE, SoundEvents.STONE_HIT)),
		BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.STONE).noOcclusion()
			.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
	static final Block BUTTER_CHURN = register("butter_churn", properties -> new ProcessStationBlock(properties,
		ProcessStationBlock.Config.of(CookingMenu.Kind.CHURN, () -> CookingBlocks.BUTTER_CHURN_ENTITY)
			.shape(Block.box(3, 0, 3, 13, 16, 13)).handWork(SoundEvents.WOOD_HIT)),
		BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
	static final Block PROOFING_BASKET = register("proofing_basket", properties -> new ProcessStationBlock(properties,
		ProcessStationBlock.Config.of(CookingMenu.Kind.PROOFING, () -> CookingBlocks.PROOFING_BASKET_ENTITY).shape(Block.box(2, 0, 2, 14, 5, 14))),
		BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.BAMBOO_WOOD).noOcclusion().ignitedByLava());
	static final Block DAIRY_PAN = register("dairy_pan", properties -> new ProcessStationBlock(properties,
		ProcessStationBlock.Config.of(CookingMenu.Kind.DAIRY_PAN, () -> CookingBlocks.DAIRY_PAN_ENTITY).shape(Block.box(1, 0, 1, 15, 4, 15))),
		BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.COPPER).noOcclusion());
	static final Block CROCK = register("fermentation_crock", properties -> new ProcessStationBlock(properties,
		ProcessStationBlock.Config.of(CookingMenu.Kind.CROCK, () -> CookingBlocks.CROCK_ENTITY).shape(Block.box(2, 0, 2, 14, 14, 14))),
		BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.DECORATED_POT).noOcclusion());
	static final Block BRINE_BARREL = register("brine_barrel", properties -> new ProcessStationBlock(properties,
		ProcessStationBlock.Config.of(CookingMenu.Kind.BRINE, () -> CookingBlocks.BRINE_BARREL_ENTITY)),
		BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).ignitedByLava());
	static final Block AGEING_SHELF = register("ageing_shelf", properties -> new ProcessStationBlock.SpotStation(properties,
		() -> CookingBlocks.AGEING_SHELF_ENTITY, SpotStationBlockEntity::ageingShelf, Block.box(0, 0, 0, 16, 10, 16)),
		BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
	static final Block DRYING_RACK = register("drying_rack", properties -> new ProcessStationBlock.SpotStation(properties,
		() -> CookingBlocks.DRYING_RACK_ENTITY, SpotStationBlockEntity::dryingRack, Block.box(0, 0, 0, 16, 13, 16)),
		BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
	static final Block CHEESE_PRESS = register("cheese_press", properties -> new ProcessStationBlock(properties,
		ProcessStationBlock.Config.of(CookingMenu.Kind.CHEESE_PRESS, () -> CookingBlocks.CHEESE_PRESS_ENTITY)),
		BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).ignitedByLava());

	/** Rock salt ore, in dripstone caves and badlands stone and terracotta. */
	static final Block ROCK_SALT_ORE = register("rock_salt_ore", Block::new, BlockBehaviour.Properties.of()
		.strength(3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());

	/** Cardoon, a wild thistle whose flowers steep into vegetable rennet. Its item replants it. */
	static final Block CARDOON = register("cardoon", CardoonBlock::new, BlockBehaviour.Properties.of()
		.noCollision().instabreak().randomTicks().sound(SoundType.GRASS).noOcclusion()
		.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));

	static final BlockEntityType<StoveBlockEntity> STOVE_ENTITY = registerEntity("stove",
		new BlockEntityType<>(StoveBlockEntity::new, Set.of(STOVE)));
	static final BlockEntityType<OvenBlockEntity> OVEN_ENTITY = registerEntity("oven",
		new BlockEntityType<>(OvenBlockEntity::new, Set.of(OVEN)));
	static final BlockEntityType<CookingPotBlockEntity> COOKING_POT_ENTITY = registerEntity("cooking_pot",
		new BlockEntityType<>(CookingPotBlockEntity::new, Set.of(COOKING_POT)));
	static final BlockEntityType<SkilletBlockEntity> SKILLET_ENTITY = registerEntity("skillet",
		new BlockEntityType<>(SkilletBlockEntity::new, Set.of(SKILLET)));
	static final BlockEntityType<ProcessStationBlockEntity> PREP_COUNTER_ENTITY = registerEntity("prep_counter",
		new BlockEntityType<>(ProcessStationBlockEntity::counter, Set.of(PREP_COUNTER)));
	static final BlockEntityType<ProcessStationBlockEntity> STRAINER_ENTITY = registerEntity("strainer",
		new BlockEntityType<>(ProcessStationBlockEntity::strainer, Set.of(STRAINER)));
	static final BlockEntityType<GrillBlockEntity> GRILL_ENTITY = registerEntity("grill",
		new BlockEntityType<>(GrillBlockEntity::new, Set.of(GRILL)));
	static final BlockEntityType<ProcessStationBlockEntity> WOK_ENTITY = registerEntity("wok",
		new BlockEntityType<>(ProcessStationBlockEntity::wok, Set.of(WOK)));
	static final BlockEntityType<AebleskivePanBlockEntity> AEBLESKIVE_PAN_ENTITY = registerEntity("aebleskive_pan",
		new BlockEntityType<>(AebleskivePanBlockEntity::new, Set.of(AEBLESKIVE_PAN)));
	static final BlockEntityType<ProcessStationBlockEntity> PRESS_ENTITY = registerEntity("press",
		new BlockEntityType<>(ProcessStationBlockEntity::press, Set.of(PRESS)));
	static final BlockEntityType<ProcessStationBlockEntity> PROOFING_BASKET_ENTITY = process("proofing_basket", PROOFING_BASKET, CookingMenu.Kind.PROOFING);
	static final BlockEntityType<ProcessStationBlockEntity> DAIRY_PAN_ENTITY = process("dairy_pan", DAIRY_PAN, CookingMenu.Kind.DAIRY_PAN);
	static final BlockEntityType<ProcessStationBlockEntity> CROCK_ENTITY = process("fermentation_crock", CROCK, CookingMenu.Kind.CROCK);
	static final BlockEntityType<ProcessStationBlockEntity> BRINE_BARREL_ENTITY = process("brine_barrel", BRINE_BARREL, CookingMenu.Kind.BRINE);
	static final BlockEntityType<SpotStationBlockEntity> AGEING_SHELF_ENTITY = registerEntity("ageing_shelf",
		new BlockEntityType<>(SpotStationBlockEntity::ageingShelf, Set.of(AGEING_SHELF)));
	static final BlockEntityType<SpotStationBlockEntity> DRYING_RACK_ENTITY = registerEntity("drying_rack",
		new BlockEntityType<>(SpotStationBlockEntity::dryingRack, Set.of(DRYING_RACK)));
	static final BlockEntityType<ProcessStationBlockEntity> MILL_ENTITY = process("mill", MILL, CookingMenu.Kind.MILL);
	static final BlockEntityType<ProcessStationBlockEntity> MIXING_BOWL_ENTITY = process("mixing_bowl", MIXING_BOWL, CookingMenu.Kind.MIXING_BOWL);
	static final BlockEntityType<ProcessStationBlockEntity> MORTAR_ENTITY = process("mortar", MORTAR, CookingMenu.Kind.MORTAR);
	static final BlockEntityType<ProcessStationBlockEntity> BUTTER_CHURN_ENTITY = process("butter_churn", BUTTER_CHURN, CookingMenu.Kind.CHURN);
	static final BlockEntityType<ProcessStationBlockEntity> CHEESE_PRESS_ENTITY = process("cheese_press", CHEESE_PRESS, CookingMenu.Kind.CHEESE_PRESS);
	static final BlockEntityType<CuttingBoardBlockEntity> CUTTING_BOARD_ENTITY = registerEntity("cutting_board",
		new BlockEntityType<>(CuttingBoardBlockEntity::new, Set.of(CUTTING_BOARD)));

	private CookingBlocks() {
	}

	static void register() {
		CookingMenu.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			entries.accept(STOVE);
			entries.accept(OVEN);
			entries.accept(COOKING_POT);
			entries.accept(SKILLET);
			entries.accept(CUTTING_BOARD);
			entries.accept(PREP_COUNTER);
			entries.accept(STRAINER);
			entries.accept(GRILL);
			entries.accept(WOK);
			entries.accept(AEBLESKIVE_PAN);
			entries.accept(PRESS);
			entries.accept(MILL);
			entries.accept(MIXING_BOWL);
			entries.accept(MORTAR);
			entries.accept(BUTTER_CHURN);
			entries.accept(CHEESE_PRESS);
			entries.accept(PROOFING_BASKET);
			entries.accept(DAIRY_PAN);
			entries.accept(CROCK);
			entries.accept(BRINE_BARREL);
			entries.accept(AGEING_SHELF);
			entries.accept(DRYING_RACK);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			entries.accept(ROCK_SALT_ORE);
			entries.accept(CARDOON);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(CookingItems.KITCHEN_KNIFE);
			entries.accept(CookingItems.COOKBOOK);
			entries.accept(CookingItems.SPOON);
			entries.accept(CookingItems.PESTLE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			for (Item item : new Item[] {CookingItems.CHOPPED_CARROT, CookingItems.DICED_POTATO, CookingItems.CHOPPED_BEETROOT,
				CookingItems.CHOPPED_APPLE, CookingItems.SLICED_BROWN_MUSHROOMS, CookingItems.SLICED_RED_MUSHROOMS,
				CookingItems.SAUTEED_MUSHROOMS, CookingItems.FRIED_EGG, CookingItems.VEGETABLE_SOUP, CookingItems.APPLE_COMPOTE,
				CookingItems.GRATED_CARROT, CookingItems.APPLE_JUICE, CookingItems.SWEET_BERRY_JUICE, CookingItems.MELON_JUICE,
				CookingItems.CARROT_JUICE, CookingItems.APPLE_POMACE, CookingItems.PULP, CookingItems.PRESS_CAKE,
				CookingItems.SUNFLOWER_SEEDS, CookingItems.TOASTED_PUMPKIN_SEEDS, CookingItems.SUNFLOWER_OIL, CookingItems.PUMPKIN_SEED_OIL,
				CookingItems.FRIED_POTATOES, CookingItems.STIR_FRIED_VEGETABLES, CookingItems.UNSTRAINED_BERRY_CORDIAL,
				CookingItems.SWEET_BERRY_CORDIAL, CookingItems.APPLE_SYRUP, CookingItems.ROCK_SALT, CookingItems.SALT,
				CookingItems.COARSE_SALT, CookingItems.WET_SALT, CookingItems.SEAWATER, CookingItems.NIGARI, CookingItems.CANE_JUICE,
				CookingItems.CANE_SUGAR_PASTE, CookingItems.BROWN_SUGAR, CookingItems.PANELA, CookingItems.SIMPLE_SYRUP, CookingItems.CARAMEL,
				CookingItems.CREAM, CookingItems.SKIMMED_MILK_BUCKET, CookingItems.WARM_MILK_BUCKET, CookingItems.RAW_BUTTER,
				CookingItems.SALTED_BUTTER, CookingItems.BUTTERMILK, CookingItems.WHIPPED_CREAM, CookingItems.YOGURT, CookingItems.CREME_FRAICHE,
				CookingItems.WHEAT_FLOUR, CookingItems.SOURDOUGH_STARTER, CookingItems.YEAST_CULTURE, CookingItems.YEAST, CookingItems.BREAD_DOUGH,
				CookingItems.PROOFED_BREAD_DOUGH, CookingItems.SHAPED_ROLLS, CookingItems.BREAD_ROLL, CookingItems.SHORTCRUST_DOUGH,
				CookingItems.SHORTCRUST_PASTRY, CookingItems.UNBAKED_PUMPKIN_PIE, CookingItems.COOKIE_DOUGH, CookingItems.CAKE_BATTER,
				CookingItems.SWEET_YEAST_DOUGH, CookingItems.PROOFED_SWEET_DOUGH, CookingItems.CARDOON_FLOWER, CookingItems.VEGETABLE_RENNET,
				CookingItems.CURDS, CookingItems.WHEY, CookingItems.FRESH_CHEESE, CookingItems.PRESSED_CHEESE, CookingItems.SOURED_MILK_BUCKET,
				CookingItems.QUARK, CookingItems.DRAINED_CREAM, CookingItems.CREAM_CHEESE, CookingItems.BRINED_CHEESE, CookingItems.AGED_HARD_CHEESE,
				CookingItems.HARD_CHEESE_WEDGE, CookingItems.SOFT_CHEESE_CURDS, CookingItems.AGED_SOFT_CHEESE, CookingItems.CIDER,
				CookingItems.CIDER_VINEGAR, CookingItems.BOILED_BEETROOT, CookingItems.SLICED_BEETROOT, CookingItems.PICKLED_BEETROOT,
				CookingItems.PICKLED_CARROTS, CookingItems.DRIED_APPLE, CookingItems.DRIED_SWEET_BERRIES, CookingItems.SALTED_COD, CookingItems.SALT_COD,
				CookingItems.BEEF_STRIPS, CookingItems.CURED_BEEF_STRIPS, CookingItems.JERKY, CookingItems.CURED_SALMON, CookingItems.SMOKED_SALMON,
				CookingItems.BRINED_PORK, CookingItems.SMOKED_PORK, CookingItems.ROUX, CookingItems.SWEET_BERRY_JAM, CookingItems.CARAMEL_SAUCE,
				CookingItems.FRIED_DOUGHNUT, CookingItems.BERLINER}) {
				entries.accept(item);
			}
		});
	}

	/** The block entity type of a station whose slots follow its screen layout. */
	private static BlockEntityType<ProcessStationBlockEntity> process(String name, Block block, CookingMenu.Kind kind) {
		BlockEntityType<ProcessStationBlockEntity>[] type = new BlockEntityType[1];
		type[0] = registerEntity(name, new BlockEntityType<>((pos, state) -> new ProcessStationBlockEntity(type[0], pos, state, kind), Set.of(block)));
		return type[0];
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, PortablePocketCabin.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, PortablePocketCabin.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	private static <T extends BlockEntityType<?>> T registerEntity(String name, T type) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, PortablePocketCabin.id(name), type);
	}
}
