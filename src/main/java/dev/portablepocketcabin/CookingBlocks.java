package dev.portablepocketcabin;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
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
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(CookingItems.KITCHEN_KNIFE);
			entries.accept(CookingItems.COOKBOOK);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			for (Item item : new Item[] {CookingItems.CHOPPED_CARROT, CookingItems.DICED_POTATO, CookingItems.CHOPPED_BEETROOT,
				CookingItems.CHOPPED_APPLE, CookingItems.SLICED_BROWN_MUSHROOMS, CookingItems.SLICED_RED_MUSHROOMS,
				CookingItems.SAUTEED_MUSHROOMS, CookingItems.FRIED_EGG, CookingItems.VEGETABLE_SOUP, CookingItems.APPLE_COMPOTE,
				CookingItems.GRATED_CARROT, CookingItems.APPLE_JUICE, CookingItems.SWEET_BERRY_JUICE, CookingItems.MELON_JUICE,
				CookingItems.CARROT_JUICE, CookingItems.APPLE_POMACE, CookingItems.PULP, CookingItems.PRESS_CAKE,
				CookingItems.SUNFLOWER_SEEDS, CookingItems.TOASTED_PUMPKIN_SEEDS, CookingItems.SUNFLOWER_OIL, CookingItems.PUMPKIN_SEED_OIL,
				CookingItems.FRIED_POTATOES, CookingItems.STIR_FRIED_VEGETABLES}) {
				entries.accept(item);
			}
		});
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
