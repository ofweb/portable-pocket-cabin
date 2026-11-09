package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Public entry points for the B-0054 mixins into the package-private {@link RichSoil}. */
public final class RichSoilAccess {
	private RichSoilAccess() {
	}

	/** What a full composter gives. */
	public static Item compost() {
		return RichSoil.COMPOST;
	}

	/** Turns rich farmland that dries out or is trampled into rich soil. Returns whether it did. */
	public static boolean toRichSoil(BlockState farmland, Level level, BlockPos pos) {
		return RichSoil.toRichSoil(farmland, level, pos);
	}
}
