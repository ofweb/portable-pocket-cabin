package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.LinkedHashSet;
import java.util.Set;

/** A room feature supplies its complete shell bounds, relative to the cabin center. */
public record CabinRoomSpace(CabinCorridor corridor, BlockPos minimum, BlockPos maximum, BlockPos entrance) {
	public static final Codec<CabinRoomSpace> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		CabinCorridor.CODEC.fieldOf("corridor").forGetter(CabinRoomSpace::corridor),
		BlockPos.CODEC.fieldOf("minimum").forGetter(CabinRoomSpace::minimum),
		BlockPos.CODEC.fieldOf("maximum").forGetter(CabinRoomSpace::maximum),
		BlockPos.CODEC.fieldOf("entrance").forGetter(CabinRoomSpace::entrance)
	).apply(instance, CabinRoomSpace::new));

	public CabinRoomSpace {
		java.util.Objects.requireNonNull(corridor);
		minimum = minimum.immutable(); maximum = maximum.immutable(); entrance = entrance.immutable();
		if (minimum.getX() > maximum.getX() || minimum.getY() > maximum.getY() || minimum.getZ() > maximum.getZ()
			|| Math.max(Math.abs((long) minimum.getX()), Math.abs((long) maximum.getX())) >= PocketDimension.CELL_SPACING / 2 - 16
			|| Math.max(Math.abs((long) minimum.getZ()), Math.abs((long) maximum.getZ())) >= PocketDimension.CELL_SPACING / 2 - 16
			|| minimum.getY() < -1 || maximum.getY() > 64) {
			throw new IllegalArgumentException("Room bounds must fit the cabin's reserved cell");
		}
		if (((long) maximum.getX() - minimum.getX() + 1) * ((long) maximum.getY() - minimum.getY() + 1)
			* ((long) maximum.getZ() - minimum.getZ() + 1) > 150_000) {
			throw new IllegalArgumentException("Room bounds exceed the supported relocation volume");
		}
		if (entrance.getX() < minimum.getX() || entrance.getX() > maximum.getX()
			|| entrance.getZ() < minimum.getZ() || entrance.getZ() > maximum.getZ()
			|| entrance.getY() <= minimum.getY() || (long) entrance.getY() + 1 >= maximum.getY()
			|| entrance.getX() != minimum.getX() && entrance.getX() != maximum.getX()
				&& entrance.getZ() != minimum.getZ() && entrance.getZ() != maximum.getZ()) {
			throw new IllegalArgumentException("Room entrance must be a 1x2 opening in a side wall");
		}
	}

	CabinRoomSpace shifted(int expansions) {
		BlockPos delta = corridor.expansionOffset().multiply(expansions);
		return new CabinRoomSpace(corridor, minimum.offset(delta), maximum.offset(delta), entrance.offset(delta));
	}

	Set<BlockPos> volume(long cell) {
		var result = new LinkedHashSet<BlockPos>();
		BlockPos center = PocketDimension.cellCenter(cell);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(minimum), center.offset(maximum))) result.add(pos.immutable());
		return Set.copyOf(result);
	}

	boolean contains(long cell, BlockPos pos) {
		BlockPos relative = pos.subtract(PocketDimension.cellCenter(cell));
		return relative.getX() >= minimum.getX() && relative.getX() <= maximum.getX()
			&& relative.getY() >= minimum.getY() && relative.getY() <= maximum.getY()
			&& relative.getZ() >= minimum.getZ() && relative.getZ() <= maximum.getZ();
	}

	boolean shell(long cell, BlockPos pos) {
		BlockPos relative = pos.subtract(PocketDimension.cellCenter(cell));
		if (relative.equals(entrance) || relative.equals(entrance.above())) return false;
		return relative.getX() >= minimum.getX() && relative.getX() <= maximum.getX()
			&& relative.getY() >= minimum.getY() && relative.getY() <= maximum.getY()
			&& relative.getZ() >= minimum.getZ() && relative.getZ() <= maximum.getZ()
			&& (relative.getX() == minimum.getX() || relative.getX() == maximum.getX()
				|| relative.getY() == minimum.getY() || relative.getY() == maximum.getY()
				|| relative.getZ() == minimum.getZ() || relative.getZ() == maximum.getZ());
	}
}
