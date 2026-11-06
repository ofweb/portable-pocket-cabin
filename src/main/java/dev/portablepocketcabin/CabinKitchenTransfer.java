package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * A receipt for one transfer between central storage, kitchen stations, and a player's inventory (ADR-0012).
 * It is written with the new storage contents before anything else changes, and removed once the stations and
 * the player are saved. Recovery completes it when the stations carry its operation id, and undoes it otherwise.
 */
record CabinKitchenTransfer(UUID operation, UUID player, List<Change> stations, List<ItemStack> inventoryBefore,
	List<ItemStack> inventoryAfter, List<ItemStack> withdrawn, List<ItemStack> deposited) {
	private static final Codec<List<ItemStack>> STACKS = ItemStack.OPTIONAL_CODEC.listOf();

	/** One station's full contents before and after the transfer. */
	record Change(BlockPos pos, List<ItemStack> before, List<ItemStack> after) {
		static final Codec<Change> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(Change::pos),
			STACKS.fieldOf("before").forGetter(Change::before),
			STACKS.fieldOf("after").forGetter(Change::after)
		).apply(instance, Change::new));

		Change {
			pos = pos.immutable();
			before = copies(before);
			after = copies(after);
			if (before.size() != after.size()) throw new IllegalArgumentException("A station change must keep its slot count");
		}
	}

	static final Codec<CabinKitchenTransfer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.STRING_CODEC.fieldOf("operation").forGetter(CabinKitchenTransfer::operation),
		UUIDUtil.STRING_CODEC.fieldOf("player").forGetter(CabinKitchenTransfer::player),
		Change.CODEC.listOf().fieldOf("stations").forGetter(CabinKitchenTransfer::stations),
		STACKS.fieldOf("inventory_before").forGetter(CabinKitchenTransfer::inventoryBefore),
		STACKS.fieldOf("inventory_after").forGetter(CabinKitchenTransfer::inventoryAfter),
		STACKS.fieldOf("withdrawn").forGetter(CabinKitchenTransfer::withdrawn),
		STACKS.fieldOf("deposited").forGetter(CabinKitchenTransfer::deposited)
	).apply(instance, CabinKitchenTransfer::new));

	CabinKitchenTransfer {
		stations = List.copyOf(stations);
		inventoryBefore = copies(inventoryBefore);
		inventoryAfter = copies(inventoryAfter);
		withdrawn = copies(withdrawn).stream().filter(stack -> !stack.isEmpty()).toList();
		deposited = copies(deposited).stream().filter(stack -> !stack.isEmpty()).toList();
		if (inventoryBefore.size() != inventoryAfter.size()) throw new IllegalArgumentException("A transfer must keep the inventory size");
	}

	static List<ItemStack> copies(List<ItemStack> stacks) {
		return stacks.stream().map(ItemStack::copy).toList();
	}
}
