package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

record CabinStorageState(boolean revealed, int level, List<ItemStack> stacks, List<Session> sessions) {
	static final int[] CAPACITIES = {0, 54, 108, 216, 432, 864, 1728};
	static final CabinStorageState EMPTY = new CabinStorageState(false, 0, List.of());
	static final Codec<CabinStorageState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("revealed", false).forGetter(CabinStorageState::revealed),
		Codec.INT.optionalFieldOf("level", 0).forGetter(CabinStorageState::level),
		ItemStack.CODEC.listOf().optionalFieldOf("stacks", List.of()).forGetter(CabinStorageState::stacks),
		Session.CODEC.listOf().optionalFieldOf("sessions", List.of()).forGetter(CabinStorageState::sessions)
	).apply(instance, CabinStorageState::new));

	CabinStorageState {
		if (level < 0 || level > 6 || level > 0 && !revealed)
			throw new IllegalArgumentException("Invalid storage installation");
		stacks = copies(stacks);
		sessions = List.copyOf(sessions);
		if (sessions.stream().map(Session::player).distinct().count() != sessions.size())
			throw new IllegalArgumentException("Duplicate storage custody session");
		if (stacks.size() > CAPACITIES[level] || stacks.stream().anyMatch(stack ->
			stack.isEmpty() || stack.getCount() > stack.getMaxStackSize()))
			throw new IllegalArgumentException("Invalid storage contents");
	}

	CabinStorageState(boolean revealed, int level, List<ItemStack> stacks) {
		this(revealed, level, stacks, List.of());
	}
	CabinStorageState withStacks(List<ItemStack> values) {
		return new CabinStorageState(revealed, level, values, sessions);
	}
	CabinStorageState withSession(Session value) {
		List<Session> updated = new ArrayList<>(sessions.stream().filter(s -> !s.player().equals(value.player())).toList());
		updated.add(value);
		return new CabinStorageState(revealed, level, stacks, updated);
	}
	CabinStorageState withoutSession(java.util.UUID player) {
		return new CabinStorageState(revealed, level, stacks, sessions.stream().filter(s -> !s.player().equals(player)).toList());
	}
	record Delivery(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
		net.minecraft.core.BlockPos position) {
		static final Codec<Delivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			net.minecraft.resources.ResourceKey.codec(net.minecraft.core.registries.Registries.DIMENSION)
				.fieldOf("dimension").forGetter(Delivery::dimension),
			net.minecraft.core.BlockPos.CODEC.fieldOf("position").forGetter(Delivery::position)
		).apply(instance, Delivery::new));
	}
	record Session(java.util.UUID player, List<ItemStack> inventory, ItemStack cursor, java.util.UUID operation, Delivery delivery) {
		static final Codec<Session> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			net.minecraft.core.UUIDUtil.STRING_CODEC.fieldOf("player").forGetter(Session::player),
			ItemStack.OPTIONAL_CODEC.listOf().fieldOf("inventory").forGetter(Session::inventory),
			ItemStack.OPTIONAL_CODEC.fieldOf("cursor").forGetter(Session::cursor),
			net.minecraft.core.UUIDUtil.STRING_CODEC.fieldOf("operation").forGetter(Session::operation),
			Delivery.CODEC.fieldOf("delivery").forGetter(Session::delivery)
		).apply(instance, Session::new));
		Session { inventory = copies(inventory); cursor = cursor.copy(); }
		@Override public List<ItemStack> inventory() { return copies(inventory); }
		@Override public ItemStack cursor() { return cursor.copy(); }
	}

	@Override public List<ItemStack> stacks() { return copies(stacks); }
	int capacity() { return CAPACITIES[level]; }
	int used() { return stacks.size(); }
	CabinStorageState reveal() { return new CabinStorageState(true, level, stacks, sessions); }
	CabinStorageState upgrade(int target) {
		if (target != level + 1) throw new IllegalArgumentException("Storage requires the previous level");
		return new CabinStorageState(revealed, target, stacks, sessions);
	}

	record Transfer(CabinStorageState state, ItemStack moved) { }

	Transfer deposit(ItemStack offered, int limit) {
		List<ItemStack> result = new ArrayList<>(stacks());
		int remaining = Math.min(Math.max(0, limit), offered.getCount());
		int requested = remaining;
		for (ItemStack stack : result) {
			if (!ItemStack.isSameItemSameComponents(stack, offered)) continue;
			int count = Math.min(remaining, stack.getMaxStackSize() - stack.getCount());
			stack.grow(count);
			remaining -= count;
		}
		while (remaining > 0 && result.size() < capacity()) {
			int count = Math.min(remaining, offered.getMaxStackSize());
			result.add(offered.copyWithCount(count));
			remaining -= count;
		}
		result.sort(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
		return new Transfer(new CabinStorageState(revealed, level, result, sessions),
			requested == remaining ? ItemStack.EMPTY : offered.copyWithCount(requested - remaining));
	}

	Transfer withdraw(ItemStack selected, int limit) {
		List<ItemStack> result = new ArrayList<>(stacks());
		int remaining = Math.min(Math.max(0, limit), selected.getMaxStackSize());
		int requested = remaining;
		for (ItemStack stack : result) {
			if (!ItemStack.isSameItemSameComponents(stack, selected)) continue;
			int count = Math.min(remaining, stack.getCount());
			stack.shrink(count);
			remaining -= count;
		}
		result.removeIf(ItemStack::isEmpty);
		return new Transfer(new CabinStorageState(revealed, level, result, sessions),
			requested == remaining ? ItemStack.EMPTY : selected.copyWithCount(requested - remaining));
	}

	List<ItemStack> entries() {
		List<ItemStack> result = new ArrayList<>();
		for (ItemStack stack : stacks) {
			ItemStack entry = result.stream().filter(value -> ItemStack.isSameItemSameComponents(value, stack))
				.findFirst().orElse(null);
			if (entry == null) result.add(stack.copy());
			else entry.grow(stack.getCount());
		}
		return result;
	}

	private static List<ItemStack> copies(List<ItemStack> values) {
		return values.stream().map(ItemStack::copy).toList();
	}
}
