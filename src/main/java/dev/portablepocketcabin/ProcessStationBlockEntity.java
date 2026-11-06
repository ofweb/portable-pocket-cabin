package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A container station whose slots follow its menu layout: the prep counter's hand work, the strainer, and the wok.
 * Stations without heat progress on recipes that need none. The wok takes heat from below and holds oil. The
 * counter's hand work pauses while another cooking station stands on top of it.
 */
final class ProcessStationBlockEntity extends CookingStationBlockEntity implements WorldlyContainer, CookingVessel {
	private final CookingMenu.Kind kind;
	private final CookingProcess process;
	private final int[] top;
	private final int[] side;
	private final int[] bottom;
	private int oil;
	private CookingHeat lastHeat;
	private boolean paused;
	final ContainerData data = new CookingMenu.Data() {
		@Override
		public int get(int index) {
			return switch (index) {
				case CookingMenu.DATA_PROGRESS -> process.progress();
				case CookingMenu.DATA_TIME -> process.time();
				case CookingMenu.DATA_WATER -> oil;
				case CookingMenu.DATA_LIQUID -> 1;
				case CookingMenu.DATA_HEAT -> lastHeat == null ? 0 : lastHeat.ordinal() + 1;
				case CookingMenu.DATA_NEEDED_HEAT -> process.neededHeat().map(heat -> heat.ordinal() + 1).orElse(0);
				case CookingMenu.DATA_PAUSED -> paused ? 1 : 0;
				default -> 0;
			};
		}
	};

	ProcessStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CookingMenu.Kind kind) {
		super(type, pos, state, kind.size);
		this.kind = kind;
		this.process = new CookingProcess(kind.station, kind.containerSlot, kind.outputSlot, kind.byproductSlot);
		this.top = IntStream.range(0, kind.station.ingredientSlots()).toArray();
		this.side = kind.containerSlot >= 0 ? new int[] {kind.containerSlot} : new int[0];
		this.bottom = new int[] {kind.outputSlot, kind.byproductSlot};
	}

	static ProcessStationBlockEntity counter(BlockPos pos, BlockState state) {
		return new ProcessStationBlockEntity(CookingBlocks.PREP_COUNTER_ENTITY, pos, state, CookingMenu.Kind.COUNTER);
	}

	static ProcessStationBlockEntity strainer(BlockPos pos, BlockState state) {
		return new ProcessStationBlockEntity(CookingBlocks.STRAINER_ENTITY, pos, state, CookingMenu.Kind.STRAINER);
	}

	static ProcessStationBlockEntity wok(BlockPos pos, BlockState state) {
		return new ProcessStationBlockEntity(CookingBlocks.WOK_ENTITY, pos, state, CookingMenu.Kind.WOK);
	}

	CookingMenu.Kind kind() {
		return kind;
	}

	int progress() {
		return process.progress();
	}

	int oil() {
		return oil;
	}

	CookingProcess process() {
		return process;
	}

	private CookingInput.Liquid liquid() {
		return new CookingInput.Liquid(0, oil);
	}

	/** Adds one level of oil. Only stations that hold oil take it, up to three levels. */
	boolean fillOil() {
		if (!kind.station.holdsOil() || oil >= CookingStation.MAX_WATER) return false;
		oil++;
		process.markDirty();
		setChanged();
		return true;
	}

	/** Whether another cooking station stands on this prep counter, which pauses its hand work. */
	boolean covered() {
		return kind == CookingMenu.Kind.COUNTER && level != null
			&& level.getBlockState(worldPosition.above()).getBlock() instanceof CookingStationBlock;
	}

	@Override
	public boolean wantsHeat(ServerLevel level) {
		return kind.station.heated() && process.wantsHeat(level, this, liquid());
	}

	static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ProcessStationBlockEntity station) {
		station.paused = station.covered();
		if (station.paused) return;
		Optional<CookingHeat> heat = station.kind.station.heated() ? CookingVessel.heatAt(level, pos) : Optional.empty();
		station.lastHeat = heat.orElse(null);
		int before = station.process.progress();
		int used = station.process.tick(level, station, station.liquid(), heat);
		if (used > 0) {
			station.oil -= used;
			station.setChanged();
		} else if (before != station.process.progress()) {
			station.saveChanged();
		}
	}

	@Override
	protected void contentsChanged() {
		process.markDirty();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < kind.station.ingredientSlots() || slot == kind.containerSlot;
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return direction == Direction.UP ? top : direction == Direction.DOWN ? bottom : side;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot == kind.outputSlot || slot == kind.byproductSlot;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (kind.station.holdsOil()) output.putInt("oil", oil);
		process.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		oil = input.getIntOr("oil", 0);
		process.load(input);
	}
}
