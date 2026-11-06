package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * An æbleskive pan has seven round wells over a heat source. Batter in a well cooks to half done, then waits to
 * be turned with a skewer, as Danes turn æbleskiver with a knitting needle, before it cooks through. A ball that
 * is never turned stays half done and doesn't burn.
 */
final class AebleskivePanBlockEntity extends DisplayStationBlockEntity implements CookingVessel {
	static final int WELLS = 7;
	private final SurfaceCooking cooking = new SurfaceCooking(CookingStation.AEBLESKIVE_PAN, WELLS, true);

	AebleskivePanBlockEntity(BlockPos pos, BlockState state) {
		super(CookingBlocks.AEBLESKIVE_PAN_ENTITY, pos, state, WELLS);
	}

	int progress(int well) {
		return cooking.progress(well);
	}

	boolean needsTurning(int well) {
		return cooking.needsTurning(well);
	}

	/** Pours one batter into a free well if the pan can cook it. */
	boolean add(ServerLevel level, ItemStack held) {
		if (!cooking.add(level, items, held)) return false;
		changedForClients();
		return true;
	}

	/** Turns every half-done ball. Returns whether any turned. */
	boolean turn() {
		if (!cooking.turn()) return false;
		changedForClients();
		return true;
	}

	/** Takes a finished ball first, otherwise the last batter poured, as it is. */
	ItemStack take(ServerLevel level) {
		ItemStack taken = cooking.take(level, items);
		if (!taken.isEmpty()) changedForClients();
		return taken;
	}

	@Override
	public boolean wantsHeat(ServerLevel level) {
		return cooking.anyRecipe(level, items);
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AebleskivePanBlockEntity pan) {
		boolean waiting = false;
		for (int well = 0; well < WELLS; well++) waiting |= pan.cooking.needsTurning(well);
		var tick = pan.cooking.tick(level, pan.items, CookingVessel.heatAt(level, pos));
		boolean nowWaiting = false;
		for (int well = 0; well < WELLS; well++) nowWaiting |= pan.cooking.needsTurning(well);
		// Clients render a ball as ready to turn, so tell them when one reaches half done.
		if (tick.finished() || nowWaiting != waiting) pan.changedForClients();
		else if (tick.cooking()) pan.saveChanged();
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
