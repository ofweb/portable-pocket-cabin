package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A skillet holds up to four items, like a campfire, and cooks each one on its own while heated. */
final class SkilletBlockEntity extends DisplayStationBlockEntity implements CookingVessel {
	static final int SLOTS = 4;
	private final SurfaceCooking cooking = new SurfaceCooking(CookingStation.SKILLET, SLOTS, false);

	SkilletBlockEntity(BlockPos pos, BlockState state) {
		super(CookingBlocks.SKILLET_ENTITY, pos, state, SLOTS);
	}

	/** Replaces the recipe lookup, for GameTests. */
	void lookup(CookingStation.Lookup lookup) {
		cooking.lookup(lookup);
	}

	int progress(int slot) {
		return cooking.progress(slot);
	}

	/** Puts one item on a free spot if the skillet can cook it. */
	boolean add(ServerLevel level, ItemStack held) {
		if (!cooking.add(level, items, held)) return false;
		changedForClients();
		return true;
	}

	/** Takes a finished item first, otherwise the last item placed. */
	ItemStack take(ServerLevel level) {
		ItemStack taken = cooking.take(level, items);
		if (!taken.isEmpty()) changedForClients();
		return taken;
	}

	@Override
	public boolean wantsHeat(ServerLevel level) {
		return cooking.anyRecipe(level, items);
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SkilletBlockEntity skillet) {
		var tick = skillet.cooking.tick(level, skillet.items, CookingVessel.heatAt(level, pos));
		if (tick.finished()) skillet.changedForClients();
		else if (tick.cooking()) skillet.saveChanged();
	}

	@Override
	protected void contentsChanged() {
		cooking.invalidate();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		cooking.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		cooking.load(input);
	}
}
