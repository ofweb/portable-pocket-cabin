package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

/** The sapling of a fruit tree. It grows into the tree's configured feature, {@code portable_pocket_cabin:<tree>}. */
final class FruitSaplingBlock extends SaplingBlock {
	private final MapCodec<FruitSaplingBlock> codec;

	FruitSaplingBlock(TreeGrower grower, Properties properties) {
		super(grower, properties);
		this.codec = simpleCodec(p -> new FruitSaplingBlock(grower, p));
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
