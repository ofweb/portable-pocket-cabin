package dev.portablepocketcabin;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import java.util.function.Function;

/** The arboretum's own blocks (B-0023). They are part of the cabin, so they cannot be broken or moved, and have no items. */
final class ArboretumBlocks {
	static final Block ARBORETUM_DOOR = register("arboretum_door", properties -> new ArboretumDoorBlock(false, properties), fixed().sound(SoundType.WOOD).noOcclusion());
	static final Block GROVE_DOOR = register("grove_door", properties -> new ArboretumDoorBlock(true, properties), fixed().sound(SoundType.WOOD).noOcclusion());
	static final Block PLANTING_BOARD = register("planting_board", PlantingBoardBlock::new, fixed().sound(SoundType.WOOD).noOcclusion());

	private ArboretumBlocks() { }

	private static BlockBehaviour.Properties fixed() {
		return BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000.0F).noLootTable().pushReaction(PushReaction.BLOCK);
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, PortablePocketCabin.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	static void register() { }
}
