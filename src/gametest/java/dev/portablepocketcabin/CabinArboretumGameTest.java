package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** The arboretum grove's rules that need no world (B-0023): regions, sizes, grid, costs, state and tree types. */
public final class CabinArboretumGameTest {
	@GameTest
	public void regionsMapEachCabinToItsOwnPlaceInsideTheBorder(GameTestHelper helper) {
		long last = (long) CabinArboretum.REGION_COLUMNS * CabinArboretum.REGION_COLUMNS - 1;
		for (long cell : new long[]{0, 1, 1799, 1800, 123_456, last}) {
			var anchor = CabinArboretum.anchor(cell);
			helper.assertTrue(CabinArboretum.regionAt(anchor).orElse(-1) == cell, "The anchor maps back to its cabin " + cell);
			helper.assertTrue(CabinArboretum.regionAt(anchor.offset(16_383, 0, -16_383)).orElse(-1) == cell, "The whole region belongs to it");
			helper.assertTrue(Math.abs(anchor.getX()) + 16_384 < 29_999_984 && Math.abs(anchor.getZ()) + 16_384 < 29_999_984,
				"Every region lies inside the world border");
		}
		helper.assertTrue(CabinArboretum.regionAt(CabinArboretum.anchor(0).offset(16_384, 0, 0)).orElse(-1) == 1, "Neighbouring regions meet halfway");
		helper.assertTrue(CabinArboretum.regionAt(new BlockPos(29_999_000, 64, 0)).isEmpty(), "The border strip belongs to no cabin");
		helper.succeed();
	}

	@GameTest
	public void groveSizesKeepTheirGridAndHeights(GameTestHelper helper) {
		int[] spots = {0, 4, 20, 42};
		var previous = new HashSet<BlockPos>();
		for (int level = 1; level <= 3; level++) {
			var grid = CabinArboretum.grid(level);
			helper.assertTrue(grid.size() == spots[level], "Size " + level + " has " + spots[level] + " spots");
			helper.assertTrue(grid.containsAll(previous), "Each size keeps the earlier spots");
			int half = CabinArboretum.half(level);
			for (var spot : grid) {
				helper.assertTrue(Math.abs(spot.getX()) + 1 <= half && spot.getZ() + 1 <= CabinArboretum.SIZES[level] - 1,
					"A spot keeps a floor block before the walls, even for a 2x2 tree");
				helper.assertTrue(CabinArboretum.inside(level, spot) && !CabinArboretum.isShell(level, spot), "Spots stand inside the grove");
			}
			previous.addAll(grid);
			helper.assertTrue(CabinArboretum.HEIGHTS[level] == new int[]{0, 9, 12, 15}[level], "Clear heights are 9, 12 and 15");
			helper.assertTrue(CabinArboretum.isShell(level, new BlockPos(0, CabinArboretum.HEIGHTS[level] + 1, 3))
				&& CabinArboretum.inside(level, new BlockPos(0, CabinArboretum.HEIGHTS[level], 3)), "The roof sits right above the clear height");
			helper.assertTrue(CabinArboretum.isShell(level, CabinArboretum.EXIT) && CabinArboretum.inside(level, CabinArboretum.BOARD)
				&& CabinArboretum.inside(level, CabinArboretum.ARRIVAL), "The exit is in the north wall; the board and arrival are inside");
			for (var torch : CabinArboretum.torches(level).entrySet()) {
				helper.assertTrue(CabinArboretum.inside(level, torch.getKey()), "Torches hang inside");
				helper.assertTrue(CabinArboretum.isShell(level, torch.getKey().relative(torch.getValue().getOpposite())), "Each torch is on a wall");
				helper.assertTrue(!grid.contains(torch.getKey().atY(1)), "No torch hangs over a spot");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void purchasesCostSaplingsLogsAndGlass(GameTestHelper helper) {
		var registry = new CabinRegistry();
		var owner = UUID.randomUUID();
		var cabin = registry.create(owner);
		registry.beginDeployment(cabin.uuid(), owner, new CabinExterior(Level.OVERWORLD, new BlockPos(0, 100, 0), Direction.NORTH));
		registry.markInteriorGenerated(cabin.uuid());
		registry.finishDeployment(cabin.uuid());
		var attunement = new WorldAttunement(1, CabinPalette.DEFAULT.walls().profileId());
		cabin = registry.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(CabinUpgradeCatalog.groups(cabin, attunement, CabinUpgradeDefinitions.current()).stream()
			.noneMatch(g -> g.title().equals("Arboretum")), "The arboretum stays hidden until its book is installed");
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withArboretum(CabinArboretumState.EMPTY.reveal()));
		var offer = offer(cabin, attunement);
		helper.assertTrue(!offer.prerequisite().isEmpty(), "The arboretum needs a 5x5 main room");
		cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 3, 5, 21);
		int[][] costs = {{}, {16, 32}, {32, 48}, {48, 64}};
		var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		for (int level = 1; level <= 3; level++) {
			offer = offer(cabin, attunement);
			helper.assertTrue(offer.prerequisite().isEmpty() && offer.target().equals(CabinUpgradeState.Target.arboretum(level)), "Only the next size is offered");
			var requirements = CabinUpgradeCatalog.arboretumRequirements(cabin, level);
			helper.assertTrue(requirements.size() == 6 && requirements.get(0).itemId().equals(BuiltInRegistries.ITEM.getKey(Items.OAK_LOG))
				&& requirements.get(0).count() == costs[level][0] && requirements.get(1).count() == costs[level][1]
				&& requirements.stream().skip(2).allMatch(r -> r.count() == 1), "Logs, glass and four different saplings");
			var stacks = requirements.stream().map(r -> new ItemStack(BuiltInRegistries.ITEM.getOptional(r.itemId()).orElseThrow(), r.count())).toList();
			var target = offer.target();
			var install = new CabinUpgradeState.Installation(UUID.randomUUID(), target, level - 1, level);
			registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(new CabinUpgradeState.Fund(target, requirements, stacks)).withInstallation(install));
			registry = CabinRegistry.CODEC.parse(ops, CabinRegistry.CODEC.encodeStart(ops, registry).getOrThrow()).getOrThrow();
			cabin = registry.completeUpgradeInstallation(cabin.uuid(), install.operationId());
			helper.assertTrue(cabin.upgrades().arboretum().level() == level && cabin.progression().corridors().contains(CabinCorridor.WEST),
				"A saved installation commits once and brings the west corridor");
		}
		helper.assertTrue(offer(cabin, attunement).complete(), "The biggest grove shows as fully grown");
		helper.assertTrue(CabinArboretum.door(cabin).subtract(PocketDimension.cellCenter(cabin.cellIndex())).equals(new BlockPos(-31, 1, 2)),
			"The door sits in the west corridor's south wall, past the greenhouse");
		cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 5, 7, 21);
		helper.assertTrue(CabinArboretum.door(cabin).subtract(PocketDimension.cellCenter(cabin.cellIndex())).equals(new BlockPos(-32, 1, 2)),
			"The door moves west with the main room's wall");
		helper.succeed();
	}

	@GameTest
	public void stateKeepsTaughtAndSelectedTypesWithinTheLimit(GameTestHelper helper) {
		Identifier oak = CabinArboretumPlanting.id(Items.OAK_SAPLING), birch = CabinArboretumPlanting.id(Items.BIRCH_SAPLING);
		var state = CabinArboretumState.EMPTY.reveal().upgrade(1).teach(oak).teach(birch);
		state = state.select(List.of(oak));
		boolean refused = false;
		try { state.select(List.of(oak, birch)); } catch (IllegalArgumentException e) { refused = true; }
		helper.assertTrue(refused, "A 7x7 grove plants one type");
		refused = false;
		try { CabinArboretumState.EMPTY.reveal().upgrade(1).select(List.of(oak)); } catch (IllegalArgumentException e) { refused = true; }
		helper.assertTrue(refused, "Only taught types can be planted");
		state = state.upgrade(2).select(List.of(oak, birch));
		var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		var restored = CabinArboretumState.CODEC.parse(ops, CabinArboretumState.CODEC.encodeStart(ops, state).getOrThrow()).getOrThrow();
		helper.assertTrue(restored.equals(state), "The arboretum state saves and loads");
		var legacy = CabinUpgradeState.CODEC.parse(ops, CabinUpgradeState.CODEC.encodeStart(ops, CabinUpgradeState.EMPTY).getOrThrow()).getOrThrow();
		helper.assertTrue(legacy.arboretum().equals(CabinArboretumState.EMPTY), "Saves from before the arboretum load with none");
		helper.succeed();
	}

	@GameTest
	public void everyTreeTypeIsASaplingAndDarkOaksNeedFour(GameTestHelper helper) {
		var types = CabinArboretumPlanting.types();
		helper.assertTrue(types.containsAll(List.of(Items.OAK_SAPLING, Items.CHERRY_SAPLING, Items.MANGROVE_PROPAGULE, Items.PALE_OAK_SAPLING)),
			"The vanilla trees can be taught");
		helper.assertTrue(types.stream().anyMatch(item -> BuiltInRegistries.ITEM.getKey(item).getPath().equals("nutmeg_sapling"))
			&& types.stream().anyMatch(item -> BuiltInRegistries.ITEM.getKey(item).getPath().equals("apple_sapling")),
			"The mod's fruit and spice trees can be taught");
		helper.assertTrue(types.stream().allMatch(item -> item instanceof net.minecraft.world.item.BlockItem block
			&& block.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock), "Every type plants a sapling");
		helper.assertTrue(new HashSet<>(types).size() == types.size(), "No type is listed twice");
		helper.assertTrue(CabinArboretumPlanting.needsFour(Items.DARK_OAK_SAPLING) && CabinArboretumPlanting.needsFour(Items.PALE_OAK_SAPLING)
			&& !CabinArboretumPlanting.needsFour(Items.OAK_SAPLING), "Dark and pale oak grow from four saplings");
		helper.succeed();
	}

	private static CabinUpgradeCatalog.Offer offer(CabinRecord cabin, WorldAttunement attunement) {
		return CabinUpgradeCatalog.groups(cabin, attunement, CabinUpgradeDefinitions.current()).stream()
			.filter(g -> g.title().equals("Arboretum")).findFirst().orElseThrow().panels().getFirst();
	}
}
