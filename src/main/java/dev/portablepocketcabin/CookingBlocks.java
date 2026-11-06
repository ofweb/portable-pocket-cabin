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

	static final BlockEntityType<StoveBlockEntity> STOVE_ENTITY = registerEntity("stove",
		new BlockEntityType<>(StoveBlockEntity::new, Set.of(STOVE)));
	static final BlockEntityType<OvenBlockEntity> OVEN_ENTITY = registerEntity("oven",
		new BlockEntityType<>(OvenBlockEntity::new, Set.of(OVEN)));
	static final BlockEntityType<CookingPotBlockEntity> COOKING_POT_ENTITY = registerEntity("cooking_pot",
		new BlockEntityType<>(CookingPotBlockEntity::new, Set.of(COOKING_POT)));
	static final BlockEntityType<SkilletBlockEntity> SKILLET_ENTITY = registerEntity("skillet",
		new BlockEntityType<>(SkilletBlockEntity::new, Set.of(SKILLET)));

	private CookingBlocks() {
	}

	static void register() {
		CookingMenu.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			entries.accept(STOVE);
			entries.accept(OVEN);
			entries.accept(COOKING_POT);
			entries.accept(SKILLET);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(CookingItems.KITCHEN_KNIFE);
			entries.accept(CookingItems.COOKBOOK);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			for (Item item : new Item[] {CookingItems.CHOPPED_CARROT, CookingItems.DICED_POTATO, CookingItems.CHOPPED_BEETROOT,
				CookingItems.CHOPPED_APPLE, CookingItems.SLICED_BROWN_MUSHROOMS, CookingItems.SLICED_RED_MUSHROOMS,
				CookingItems.SAUTEED_MUSHROOMS, CookingItems.FRIED_EGG, CookingItems.VEGETABLE_SOUP, CookingItems.APPLE_COMPOTE}) {
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
