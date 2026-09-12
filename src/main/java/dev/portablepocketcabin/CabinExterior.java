package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

public record CabinExterior(ResourceKey<Level> dimension, BlockPos anchor, Direction facing) {
	public static final Codec<CabinExterior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(CabinExterior::dimension),
		BlockPos.CODEC.fieldOf("anchor").forGetter(CabinExterior::anchor),
		Direction.CODEC.fieldOf("facing").forGetter(CabinExterior::facing)
	).apply(instance, CabinExterior::new));

	public CabinExterior {
		Objects.requireNonNull(dimension, "dimension");
		Objects.requireNonNull(anchor, "anchor");
		Objects.requireNonNull(facing, "facing");
		if (facing.getAxis().isVertical()) {
			throw new IllegalArgumentException("Cabin exterior facing must be horizontal");
		}
	}
}
