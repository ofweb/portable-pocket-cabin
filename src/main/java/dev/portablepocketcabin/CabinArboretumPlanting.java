package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.*;

/**
 * The grove's tree types and its planting (B-0023): which saplings it can learn, how it plants and replants its grid,
 * and why its trees give no saplings.
 */
final class CabinArboretumPlanting {
	/** Saplings needed to teach the grove one tree type. */
	static final int TEACH_COST = 15;
	/** Server ticks a felled spot waits before it is planted again: 30 seconds. */
	static final int REPLANT_DELAY = 600;
	private static final List<Item> VANILLA = List.of(Items.OAK_SAPLING, Items.BIRCH_SAPLING, Items.SPRUCE_SAPLING, Items.JUNGLE_SAPLING,
		Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING, Items.MANGROVE_PROPAGULE, Items.CHERRY_SAPLING, Items.PALE_OAK_SAPLING);
	private static final Set<Item> FOUR = Set.of(Items.DARK_OAK_SAPLING, Items.PALE_OAK_SAPLING);
	private static final Map<MinecraftServer, Map<BlockPos, Long>> EMPTY_SINCE = new IdentityHashMap<>();
	private static List<Item> types;
	private CabinArboretumPlanting() { }

	/** Every tree type the grove can learn: the vanilla trees, then the mod's fruit and spice trees. */
	static List<Item> types() {
		if (types == null) {
			var result = new ArrayList<>(VANILLA);
			CookingPlants.PLANTED_FROM.forEach((plant, item) -> {
				if (plant.endsWith("_leaves") && item instanceof BlockItem) result.add(item);
			});
			types = List.copyOf(result);
		}
		return types;
	}

	static Optional<Item> type(Identifier id) {
		return types().stream().filter(item -> BuiltInRegistries.ITEM.getKey(item).equals(id)).findFirst();
	}

	static Identifier id(Item item) { return BuiltInRegistries.ITEM.getKey(item); }

	/** The grove type a sapling block grows, if it is one. */
	static Optional<Item> typeOf(BlockState state) {
		return types().stream().filter(item -> ((BlockItem) item).getBlock() == state.getBlock()).findFirst();
	}

	static boolean needsFour(Item type) { return FOUR.contains(type); }

	/** The blocks a spot's sapling takes: the spot, and for a 2x2 tree the squares to its east and south. */
	static List<BlockPos> cells(BlockPos spot, boolean four) {
		return four ? List.of(spot, spot.east(), spot.south(), spot.south().east()) : List.of(spot);
	}

	/** Every block a grid sapling may stand on, which players cannot plant on themselves. */
	static boolean isSpotCell(CabinRecord cabin, BlockPos pos) {
		for (var spot : CabinArboretum.spots(cabin)) if (cells(spot, true).contains(pos)) return true;
		return false;
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) tick(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(EMPTY_SINCE::remove);
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!(context.getLevel() instanceof ServerLevel level) || !CabinArboretum.isArboretum(level)) return;
			var state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
			var origin = context.getOptionalParameter(LootContextParams.ORIGIN);
			if (state == null || origin == null) return;
			var cabin = CabinArboretum.cabinAt(level.getServer(), BlockPos.containing(origin)).orElse(null);
			if (cabin == null) return;
			boolean leaves = state.is(BlockTags.LEAVES) || state.getBlock() instanceof FruitingLeavesBlock;
			boolean spot = typeOf(state).isPresent() && isSpotCell(cabin, BlockPos.containing(origin));
			if (leaves || spot) drops.removeIf(stack -> types().contains(stack.getItem()) || stack.is(net.minecraft.tags.ItemTags.SAPLINGS));
		});
	}

	private static void tick(MinecraftServer server) {
		ServerLevel level = server.getLevel(CabinArboretum.LEVEL_KEY);
		if (level == null) return;
		var waiting = EMPTY_SINCE.computeIfAbsent(server, ignored -> new HashMap<>());
		var seen = new HashSet<BlockPos>();
		for (var cabin : CabinRegistry.get(server).cabins()) {
			if (cabin.upgrades().arboretum().level() == 0 || !CabinSimulation.shouldSimulate(cabin)
				|| CabinGreenhouseGrowth.hasGrovePending(server, cabin.uuid())) continue;
			for (var spot : CabinArboretum.spots(cabin)) {
				if (!level.isLoaded(spot)) continue;
				seen.add(spot);
				if (!level.getBlockState(spot).isAir()) { waiting.remove(spot); continue; }
				long since = waiting.computeIfAbsent(spot, ignored -> (long) server.getTickCount());
				if (server.getTickCount() - since >= REPLANT_DELAY && plant(level, cabin, spot, level.getRandom())) waiting.remove(spot);
			}
		}
		waiting.keySet().retainAll(seen);
	}

	/** Plants a random selected type on an empty spot. Returns whether it planted. */
	static boolean plant(ServerLevel level, CabinRecord cabin, BlockPos spot, RandomSource random) {
		var selected = cabin.upgrades().arboretum().selected().stream().map(CabinArboretumPlanting::type)
			.flatMap(Optional::stream).toList();
		if (selected.isEmpty() || !level.getBlockState(spot).isAir()) return false;
		var order = new ArrayList<>(selected);
		Collections.shuffle(order, new Random(random.nextLong()));
		for (var type : order) {
			var cells = cells(spot, needsFour(type));
			BlockState sapling = ((BlockItem) type).getBlock().defaultBlockState();
			if (cells.stream().allMatch(cell -> level.getBlockState(cell).isAir() && sapling.canSurvive(level, cell))) {
				for (var cell : cells) level.setBlock(cell, sapling, Block.UPDATE_ALL);
				return true;
			}
		}
		return false;
	}

	/** Plants every empty spot now. Catch-up uses this once the felled spots' wait has passed. */
	static void plantAll(ServerLevel level, CabinRecord cabin, RandomSource random) {
		for (var spot : CabinArboretum.spots(cabin)) plant(level, cabin, spot, random);
	}

	/**
	 * After the selection changes: saplings of types no longer selected make way for selected ones, and empty spots
	 * are planted at once. Grown trees stay.
	 */
	static void applySelection(ServerLevel level, CabinRecord cabin) {
		var selected = new HashSet<>(cabin.upgrades().arboretum().selected());
		for (var spot : CabinArboretum.spots(cabin)) {
			var type = typeOf(level.getBlockState(spot)).orElse(null);
			if (type != null && !selected.contains(id(type))) {
				for (var cell : cells(spot, needsFour(type)))
					if (level.getBlockState(cell).is(((BlockItem) type).getBlock())) level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			plant(level, cabin, spot, level.getRandom());
		}
	}

	/** How many ordinary saplings of a type the player could give: from installed storage, then the inventory. */
	static int available(CabinRecord cabin, List<ItemStack> inventory, Item type) {
		int count = 0;
		for (var stack : cabin.upgrades().storage().stacks()) if (stack.is(type) && CabinStation.ordinary(stack)) count += stack.getCount();
		for (int i = 0; i < inventory.size(); i++) {
			if (i >= 36 && i != net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND) continue;
			var stack = inventory.get(i);
			if (stack.is(type) && CabinStation.ordinary(stack)) count += stack.getCount();
		}
		return count;
	}
}
