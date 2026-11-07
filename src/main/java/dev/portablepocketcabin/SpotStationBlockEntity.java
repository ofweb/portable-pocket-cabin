package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/**
 * A time station whose items each age or dry on their own spot: the ageing shelf and the drying rack. Spots count
 * game time, so they catch up after the chunk was unloaded or the cabin packed (ADR-0013). The drying rack checks
 * the sky above it for sun and shade recipes. Hoppers above add items a recipe takes; hoppers below take finished ones.
 */
final class SpotStationBlockEntity extends DisplayStationBlockEntity implements WorldlyContainer {
	static final int SPOTS = 4;
	private static final int[] ALL = {0, 1, 2, 3};
	/** The saved time before a station's first tick. */
	private static final long NEVER = Long.MIN_VALUE;
	private final CookingStation station;
	private final SurfaceCooking cooking;
	private long lastTime = NEVER;

	SpotStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CookingStation station) {
		super(type, pos, state, SPOTS);
		this.station = station;
		this.cooking = new SurfaceCooking(station, SPOTS, false);
	}

	static SpotStationBlockEntity ageingShelf(BlockPos pos, BlockState state) {
		return new SpotStationBlockEntity(CookingBlocks.AGEING_SHELF_ENTITY, pos, state, CookingStation.AGEING);
	}

	static SpotStationBlockEntity dryingRack(BlockPos pos, BlockState state) {
		return new SpotStationBlockEntity(CookingBlocks.DRYING_RACK_ENTITY, pos, state, CookingStation.DRYING);
	}

	int progress(int spot) {
		return cooking.progress(spot);
	}

	/** Puts one item on a free spot if a recipe here takes it. */
	boolean add(ServerLevel level, ItemStack held) {
		updateSky(level);
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

	/** Moves the saved time back, as if the station had been unloaded for {@code ticks}. For GameTests. */
	void rewind(long ticks) {
		if (lastTime != NEVER) lastTime -= ticks;
	}

	private void updateSky(ServerLevel level) {
		if (station == CookingStation.DRYING) cooking.sky(openSky(level, worldPosition));
	}

	/** Whether nothing above the rack blocks the sky: no block higher than the rack itself in the heightmap. */
	static boolean openSky(ServerLevel level, BlockPos pos) {
		return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1;
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SpotStationBlockEntity spots) {
		long now = level.getGameTime();
		long elapsed = spots.lastTime == NEVER ? 1 : now - spots.lastTime;
		spots.lastTime = now;
		spots.updateSky(level);
		var tick = spots.cooking.tick(level, spots.items, Optional.empty(), elapsed);
		if (tick.finished()) spots.changedForClients();
		else if (tick.cooking()) spots.setChanged();
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
	public int[] getSlotsForFace(Direction side) {
		return ALL;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.UP && items.get(slot).isEmpty() && level instanceof ServerLevel server && cooking.cooks(server, stack);
	}

	/** Hoppers below take items no recipe here would change any more. */
	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN && level instanceof ServerLevel server && cooking.recipe(server, items, slot).isEmpty();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return items.get(slot).isEmpty() && level instanceof ServerLevel server && cooking.cooks(server, stack);
	}

	@Override
	public boolean canTakeItem(Container target, int slot, ItemStack stack) {
		return true;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		cooking.save(output);
		output.putLong("last_time", lastTime);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		cooking.load(input);
		lastTime = input.getLongOr("last_time", NEVER);
	}
}
