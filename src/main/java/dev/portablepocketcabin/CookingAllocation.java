package dev.portablepocketcabin;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Decides which stacks supply which counted ingredients, so that overlapping ingredients, such as a plank tag and
 * oak planks in one recipe, are satisfied whenever any assignment works. It is a small maximum flow from each
 * ingredient's count, through the stacks it accepts, to each stack's size. Earlier stacks are preferred, so callers
 * list the stacks they would rather use first. Named and customised stacks supply nothing.
 */
final class CookingAllocation {
	private CookingAllocation() {
	}

	/** How many items each demand takes from each stack, as {@code amounts[demand][stack]}, or empty when no assignment works. */
	static Optional<int[][]> allocate(List<CookingRecipe.Counted> demands, List<ItemStack> stacks) {
		int demandCount = demands.size();
		int stackCount = stacks.size();
		boolean[][] accepts = new boolean[demandCount][stackCount];
		int[] need = new int[demandCount];
		int[] left = new int[stackCount];
		for (int stack = 0; stack < stackCount; stack++) {
			ItemStack item = stacks.get(stack);
			left[stack] = CabinStation.ordinary(item) ? item.getCount() : 0;
		}
		for (int demand = 0; demand < demandCount; demand++) {
			need[demand] = demands.get(demand).count();
			for (int stack = 0; stack < stackCount; stack++) {
				accepts[demand][stack] = left[stack] > 0 && demands.get(demand).ingredient().test(stacks.get(stack));
			}
		}
		int[][] amounts = new int[demandCount][stackCount];
		for (int demand = 0; demand < demandCount; demand++) {
			while (need[demand] > 0) {
				int moved = augment(demand, need[demand], accepts, amounts, left);
				if (moved == 0) return Optional.empty();
				need[demand] -= moved;
			}
		}
		return Optional.of(amounts);
	}

	/**
	 * Finds one path from the demand to a stack with items left, possibly moving earlier demands to other stacks
	 * they accept, and pushes as much as it can along it. Returns the amount moved, or zero when none can be.
	 */
	private static int augment(int start, int wanted, boolean[][] accepts, int[][] amounts, int[] left) {
		int demands = amounts.length;
		int stacks = left.length;
		int[] stackFrom = new int[stacks];
		int[] demandFrom = new int[demands];
		Arrays.fill(stackFrom, -1);
		Arrays.fill(demandFrom, -1);
		demandFrom[start] = start;
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		queue.add(start);
		int end = -1;
		while (!queue.isEmpty() && end < 0) {
			int demand = queue.poll();
			for (int stack = 0; stack < stacks && end < 0; stack++) {
				if (!accepts[demand][stack] || stackFrom[stack] >= 0) continue;
				stackFrom[stack] = demand;
				if (left[stack] > 0) {
					end = stack;
					break;
				}
				for (int other = 0; other < demands; other++) {
					if (demandFrom[other] < 0 && amounts[other][stack] > 0) {
						demandFrom[other] = stack;
						queue.add(other);
					}
				}
			}
		}
		if (end < 0) return 0;
		int amount = Math.min(wanted, left[end]);
		for (int stack = end, demand = stackFrom[end]; demand != start; ) {
			int previous = demandFrom[demand];
			amount = Math.min(amount, amounts[demand][previous]);
			stack = previous;
			demand = stackFrom[stack];
		}
		left[end] -= amount;
		for (int stack = end, demand = stackFrom[end]; ; ) {
			amounts[demand][stack] += amount;
			if (demand == start) break;
			int previous = demandFrom[demand];
			amounts[demand][previous] -= amount;
			stack = previous;
			demand = stackFrom[stack];
		}
		return amount;
	}

	/** The total each stack gives across all demands. */
	static int[] taken(int[][] amounts, int stacks) {
		int[] taken = new int[stacks];
		for (int[] demand : amounts) for (int stack = 0; stack < stacks; stack++) taken[stack] += demand[stack];
		return taken;
	}
}
