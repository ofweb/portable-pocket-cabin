package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The prawns a trap has caught, and the game time it last counted from, so it catches up after being unloaded. */
final class PrawnTrapBlockEntity extends CookingStationBlockEntity {
	/** About one prawn every Minecraft hour. */
	static final int TICKS_PER_PRAWN = 1000;
	static final int MAX_PRAWNS = 8;
	private static final long NEVER = Long.MIN_VALUE;
	private long lastTime = NEVER;

	PrawnTrapBlockEntity(BlockPos pos, BlockState state) {
		super(Butchery.PRAWN_TRAP_ENTITY, pos, state, 1);
	}

	void tick(ServerLevel level, BlockState state) {
		long now = level.getGameTime();
		if (lastTime == NEVER || !PrawnTrapBlock.catches(level, worldPosition, state)) {
			lastTime = now;
			return;
		}
		long caught = (now - lastTime) / TICKS_PER_PRAWN;
		if (caught <= 0) return;
		lastTime += caught * TICKS_PER_PRAWN;
		ItemStack prawns = items.get(0);
		int room = MAX_PRAWNS - prawns.getCount();
		if (room <= 0) return;
		int added = (int) Math.min(room, caught);
		if (prawns.isEmpty()) items.set(0, new ItemStack(Butchery.PRAWNS, added));
		else prawns.grow(added);
		setChanged();
	}

	/** Moves the trap's clock back, as if time had passed while its chunk was unloaded. For GameTests. */
	void rewind(long ticks) {
		lastTime -= ticks;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lastTime = input.getLongOr("last_time", NEVER);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("last_time", lastTime);
	}
}
