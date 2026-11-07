package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

/**
 * The sapling of a fruit tree. It grows into the tree's configured feature, {@code portable_pocket_cabin:<tree>}. A
 * beach tree, the coconut palm, can also be planted on sand.
 */
final class FruitSaplingBlock extends SaplingBlock {
	private final boolean sand;
	private final MapCodec<FruitSaplingBlock> codec;

	FruitSaplingBlock(TreeGrower grower, Properties properties) {
		this(grower, false, properties);
	}

	FruitSaplingBlock(TreeGrower grower, boolean sand, Properties properties) {
		super(grower, properties);
		this.sand = sand;
		this.codec = simpleCodec(p -> new FruitSaplingBlock(grower, sand, p));
	}

	@Override
	protected boolean mayPlaceOn(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
		net.minecraft.core.BlockPos pos) {
		return super.mayPlaceOn(state, level, pos) || sand && state.is(net.minecraft.tags.BlockTags.SAND);
	}

	static TreeGrower grower(String tree) {
		return new TreeGrower(PortablePocketCabin.MOD_ID + ":" + tree, Optional.empty(),
			Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, PortablePocketCabin.id(tree))), Optional.empty());
	}

	@Override
	public MapCodec<? extends SaplingBlock> codec() {
		return codec;
	}
}
