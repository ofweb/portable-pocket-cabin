package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A grill with its own fire. Up to four items or skewers lie on the grate and cook each on its own at the heat
 * of the burning fuel, which the fuel's tag sets, as in the oven. It lights fuel only when something on it can cook.
 */
final class GrillBlockEntity extends FiredStationBlockEntity implements WorldlyContainer {
	static final int SPOTS = 4;
	static final int FUEL_SLOT = 4;
	private static final int[] FUEL = {FUEL_SLOT};
	private final SurfaceCooking cooking = new SurfaceCooking(CookingStation.GRILL, SPOTS, false);

	GrillBlockEntity(BlockPos pos, BlockState state) {
		super(CookingBlocks.GRILL_ENTITY, pos, state, SPOTS + 1, FUEL_SLOT);
	}

	int progress(int spot) {
		return cooking.progress(spot);
	}

	/** Whether the grill cooks the item, so that it goes on the grate rather than into the fuel slot. */
	boolean cooks(ServerLevel level, ItemStack held) {
		return cooking.cooks(level, held);
	}

	/** Puts one item on a free spot of the grate if the grill can cook it. */
	boolean add(ServerLevel level, ItemStack held) {
		if (!cooking.add(level, items, held)) return false;
		changedForClients();
		return true;
	}

	/** Adds fuel to the fuel slot, merging with what is there. Returns whether any went in. */
	boolean addFuel(ItemStack held) {
		if (!isFuel(held)) return false;
		ItemStack fuel = items.get(FUEL_SLOT);
		if (fuel.isEmpty()) {
			items.set(FUEL_SLOT, held.split(held.getCount()));
		} else if (ItemStack.isSameItemSameComponents(fuel, held) && fuel.getCount() < fuel.getMaxStackSize()) {
			int moved = Math.min(held.getCount(), fuel.getMaxStackSize() - fuel.getCount());
			fuel.grow(moved);
			held.shrink(moved);
		} else {
			return false;
		}
		saveChanged();
		return true;
	}

	/** Takes a finished item first, otherwise the last item placed. */
	ItemStack take(ServerLevel level) {
		ItemStack taken = cooking.take(level, items.subList(0, SPOTS));
		if (!taken.isEmpty()) changedForClients();
		return taken;
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GrillBlockEntity grill) {
		var grate = grill.items.subList(0, SPOTS);
		grill.burn(grill.cooking.anyRecipe(level, grate));
		var tick = grill.cooking.tick(level, grate, grill.heat());
		if (tick.finished()) grill.changedForClients();
		else if (tick.cooking()) grill.saveChanged();
	}

	private void changedForClients() {
		saveChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	@Override
	protected void contentsChanged() {
		cooking.invalidate();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == FUEL_SLOT && isFuel(stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return FUEL;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side != Direction.DOWN && canPlaceItem(slot, stack);
	}

	/** Only spent fuel, such as the empty bucket a lava bucket leaves, comes out, through the bottom. */
	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN && !isFuel(stack);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
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
