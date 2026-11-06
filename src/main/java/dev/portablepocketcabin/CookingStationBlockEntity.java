package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A cooking station that holds items in slots. Inside a cabin, only the household may use it. */
abstract class CookingStationBlockEntity extends BlockEntity implements Container {
	protected final NonNullList<ItemStack> items;
	/** The last storage transfer applied to this station, which kitchen recovery reads (ADR-0012). */
	private UUID lastTransfer;

	CookingStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
		super(type, pos, state);
		items = NonNullList.withSize(size, ItemStack.EMPTY);
	}

	Optional<UUID> lastTransfer() {
		return Optional.ofNullable(lastTransfer);
	}

	/** Sets every slot for a confirmed storage transfer and records its operation id. */
	void applyTransfer(UUID operation, List<ItemStack> contents) {
		if (contents.size() != items.size()) throw new IllegalArgumentException("Transfer contents do not match the station");
		for (int slot = 0; slot < items.size(); slot++) items.set(slot, contents.get(slot).copy());
		lastTransfer = operation;
		setChanged();
	}

	/** Every slot's contents, copied. */
	List<ItemStack> contents() {
		return items.stream().map(ItemStack::copy).toList();
	}

	/** Called whenever slot contents change, so a station can look up its recipe again. */
	protected void contentsChanged() {
	}

	static boolean mayUse(BlockEntity station, Player player) {
		return !(station.getLevel() instanceof ServerLevel level)
			|| CabinCorridors.mayChange(level, player.getUUID(), station.getBlockPos());
	}

	/** The first {@code count} slots, for checking a station's layout against a recipe. */
	List<ItemStack> getItems(int count) {
		return List.copyOf(items.subList(0, count));
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack removed = ContainerHelper.removeItem(items, slot, count);
		if (!removed.isEmpty()) setChanged();
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack removed = ContainerHelper.takeItem(items, slot);
		contentsChanged();
		return removed;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public void setChanged() {
		contentsChanged();
		super.setChanged();
	}

	/** Marks progress or fuel for saving without treating the slots as changed. */
	protected void saveChanged() {
		super.setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player) && mayUse(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.storeNullable("last_transfer", UUIDUtil.STRING_CODEC, lastTransfer);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		lastTransfer = input.read("last_transfer", UUIDUtil.STRING_CODEC).orElse(null);
		contentsChanged();
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) Containers.dropContents(level, pos, this);
	}
}
