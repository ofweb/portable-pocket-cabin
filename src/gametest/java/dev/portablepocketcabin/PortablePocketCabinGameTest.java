package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PortablePocketCabinGameTest {
	@GameTest
	public void cabinBooksDescribeTheirOwnUpgradesAndStackByType(GameTestHelper helper) {
		var types = List.of(CabinItems.STORAGE_BOOK, CabinItems.CRAFTING_BOOK, CabinItems.GREENHOUSE_BOOK);
		for (var item : types) {
			var book = new ItemStack(item, 64);
			helper.assertTrue(book.getMaxStackSize() == 64 && book.hasFoil(), "Cabin books must stack and show an enchantment glint");
			helper.assertTrue(book.get(DataComponents.LORE).lines().size() >= 3, "Hover text must describe upgrades and installation");
			helper.assertTrue(ItemStack.isSameItemSameComponents(book, new ItemStack(item)), "Copies of the same type must merge");
			var definition = CabinBookItem.from(book);
			helper.assertTrue(definition != null && !definition.revealed(CabinUpgradeState.EMPTY), "Books must start uninstalled");
			var revealed = definition.reveal(CabinUpgradeState.EMPTY);
			helper.assertTrue(definition.revealed(revealed) && revealed.storage().level() == 0
				&& revealed.crafting().level() == 0 && revealed.greenhouse().level() == 0, "Reveals must not purchase capabilities");
			var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
			var restored = CabinUpgradeState.CODEC.parse(ops, CabinUpgradeState.CODEC.encodeStart(ops, revealed).getOrThrow()).getOrThrow();
			helper.assertTrue(definition.revealed(restored), "Installed books must survive persistence");
			for (var other : types) if (other != item) {
				helper.assertTrue(!ItemStack.isSameItemSameComponents(book, new ItemStack(other)), "Different book types must not merge");
				helper.assertTrue(!CabinBookItem.from(new ItemStack(other)).revealed(revealed), "Reveals must remain specific to each type");
			}
		}
		helper.assertTrue(CabinBookItem.from(new ItemStack(Items.BOOK)) == null
			&& CabinBookItem.from(new ItemStack(Items.ENCHANTED_BOOK)) == null, "Vanilla books are not cabin books");
		helper.succeed();
	}

	@GameTest
	public void storageMergesStacksAndPreservesVariants(GameTestHelper helper) {
		var storage = CabinStorageState.EMPTY.reveal().upgrade(1);
		storage = storage.deposit(new ItemStack(Items.COPPER_INGOT, 40), 40).state();
		storage = storage.deposit(new ItemStack(Items.COPPER_INGOT, 40), 40).state();
		helper.assertTrue(storage.used() == 2 && storage.entries().size() == 1
			&& storage.entries().getFirst().getCount() == 80, "Grouping must retain stack-slot capacity");
		ItemStack named = new ItemStack(Items.COPPER_INGOT, 3);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
		storage = storage.deposit(named, 3).state();
		helper.assertTrue(storage.entries().size() == 2 && storage.used() == 3, "Named stacks must remain distinct");
		var taken = storage.withdraw(named, 1);
		helper.assertTrue(taken.moved().getCount() == 1 && ItemStack.isSameItemSameComponents(taken.moved(), named),
			"Withdrawals must preserve components");
		helper.assertTrue(taken.state().entries().stream().anyMatch(s -> s.getCount() == 80), "Other variants must stay untouched");
		var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		var restored = CabinStorageState.CODEC.parse(ops,
			CabinStorageState.CODEC.encodeStart(ops, taken.state()).getOrThrow()).getOrThrow();
		helper.assertTrue(restored.used() == 3 && restored.entries().size() == 2, "Storage must survive persistence");
		helper.succeed();
	}

	@GameTest
	public void storageOnlyMovesItemsThatFit(GameTestHelper helper) {
		var storage = CabinStorageState.EMPTY.reveal().upgrade(1);
		for (int i = 0; i < 53; i++) storage = storage.deposit(new ItemStack(Items.IRON_SWORD), 1).state();
		storage = storage.deposit(new ItemStack(Items.COPPER_INGOT, 60), 60).state();
		var deposit = storage.deposit(new ItemStack(Items.COPPER_INGOT, 12), 12);
		helper.assertTrue(deposit.moved().getCount() == 4 && deposit.state().used() == 54,
			"A full inventory must accept only compatible partial-stack space");
		helper.assertTrue(deposit.state().deposit(new ItemStack(Items.DIAMOND, 2), 2).moved().isEmpty(),
			"A full inventory must reject a new variant");
		var withdrawal = deposit.state().withdraw(new ItemStack(Items.COPPER_INGOT), 200);
		helper.assertTrue(withdrawal.moved().getCount() == 64 && withdrawal.state().used() == 53,
			"Withdrawal must stop at one normal stack");
		helper.succeed();
	}

	@GameTest
	public void storageFundingIsAtomicAndOwnerInitiated(GameTestHelper helper) {
		var registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		var cabin = deployRegistryCabin(registry, owner, 0);
		var storage = CabinStorageState.EMPTY.reveal().upgrade(1)
			.deposit(new ItemStack(Items.COPPER_INGOT, 4), 4).state();
		ItemStack named = new ItemStack(Items.AMETHYST_BLOCK, 4);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
		storage = storage.deposit(named, 4).state();
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(storage));
		var target = CabinUpgradeState.Target.storage(2);
		var requirements = CabinUpgradeCatalog.storageRequirements(2);
		var definitions = CabinUpgradeDefinitions.current();
		var attunement = new WorldAttunement(1, CabinPalette.DEFAULT.walls().profileId());
		var refused = CabinStorageFunding.fill(registry, cabin.uuid(), UUID.randomUUID(), target,
			requirements, attunement, definitions);
		helper.assertTrue(!refused.success() && registry.find(cabin.uuid()).orElseThrow().upgrades().storage().used() == 2,
			"Guests must not move or inspect storage");
		var changed = new ArrayList<>(requirements);
		changed.set(0, new CabinUpgradeState.Requirement(requirements.getFirst().itemId(), 9));
		helper.assertTrue(!CabinStorageFunding.fill(registry, cabin.uuid(), owner, target, changed, attunement, definitions).success(),
			"Changed requirements must move nothing");
		helper.assertTrue(CabinStorageFunding.fill(registry, cabin.uuid(), owner, target, requirements, attunement, definitions).success(),
			"Partial funding must succeed");
		var updated = registry.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(updated.upgrades().fund(target).orElseThrow().fundedCount(
			BuiltInRegistries.ITEM.getKey(Items.COPPER_INGOT)) == 4, "The fund must receive only missing materials");
		helper.assertTrue(updated.upgrades().storage().stacks().size() == 1
			&& updated.upgrades().storage().stacks().getFirst().has(DataComponents.CUSTOM_NAME),
			"Automatic funding must leave custom stacks in storage");
		helper.assertTrue(!CabinStorageFunding.fill(registry, cabin.uuid(), owner, target, requirements, attunement, definitions).success(),
			"Repeated funding without eligible materials must make no change");
		helper.succeed();
	}

	@GameTest
	public void storageLevelsUseRecoverableUpgradeInstallation(GameTestHelper helper) {
		var registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		var cabin = deployRegistryCabin(registry, owner, 0);
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(CabinStorageState.EMPTY.reveal()));
		var definitions = CabinUpgradeDefinitions.current();
		var attunement = new WorldAttunement(1, CabinPalette.DEFAULT.walls().profileId());
		var effect = new TestExpansionEffect(true, false);
		for (int level = 1; level <= 6; level++) {
			cabin = registry.find(cabin.uuid()).orElseThrow();
			var target = CabinUpgradeState.Target.storage(level);
			for (var requirement : CabinUpgradeCatalog.storageRequirements(level)) {
				ItemStack material = new ItemStack(BuiltInRegistries.ITEM.getOptional(requirement.itemId()).orElseThrow(), requirement.count());
				helper.assertTrue(CabinUpgradeService.deposit(registry, cabin.uuid(), owner, target, material,
					attunement, definitions, effect).success(), "Storage level must accept its cost");
			}
			long revision = registry.find(cabin.uuid()).orElseThrow().upgrades().fundRevision();
			var interrupted = new CabinUpgradeService.UpgradeEffect() {
				public CabinUpgradeService.Outcome validate(CabinRecord c, CabinUpgradeCatalog.Offer o) {
					return CabinUpgradeService.Outcome.success("");
				}
				public void apply(CabinRecord c, CabinUpgradeState.Installation i) { throw new IllegalStateException("Interrupted"); }
				public void refresh(CabinRecord c) { }
			};
			helper.assertTrue(!CabinUpgradeService.install(registry, cabin.uuid(), owner, target, revision,
				attunement, definitions, interrupted, () -> {}).success(), "Interrupted install must remain pending");
			helper.assertTrue(registry.find(cabin.uuid()).orElseThrow().upgrades().storage().level() == level - 1,
				"Interrupted installation must retain capacity");
			CabinUpgradeService.reconcileInstallation(registry, cabin.uuid(), effect, () -> {});
			CabinUpgradeService.reconcileInstallation(registry, cabin.uuid(), effect, () -> {});
			helper.assertTrue(registry.find(cabin.uuid()).orElseThrow().upgrades().storage().capacity() == CabinStorageState.CAPACITIES[level],
				"Recovery must install exactly one level");
		}
		var groups = CabinUpgradeCatalog.groups(registry.find(cabin.uuid()).orElseThrow(), attunement, definitions);
		helper.assertTrue(groups.getLast().panels().getFirst().complete(), "Maximum storage must show fully upgraded");
		helper.succeed();
	}


	@GameTest
	public void pocketDimensionTypeIsRegistered(GameTestHelper helper) {
		var dimensionTypes = helper.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);

		helper.assertTrue(dimensionTypes.containsKey(PocketDimension.TYPE_KEY),
			"Pocket dimension type must be registered");
		helper.assertTrue(
			PocketDimension.LEVEL_KEY.identifier().equals(PortablePocketCabin.id("pocket_home")),
			"Pocket dimension key must remain stable for save compatibility"
		);
		helper.succeed();
	}

	@GameTest
	public void registryEnforcesOneCabinPerPlayer(GameTestHelper helper) {
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		CabinRecord first = registry.create(owner);

		helper.assertTrue(first.cellIndex() == 0, "First cabin must receive cell zero");
		helper.assertTrue(first.lifecycle() == CabinLifecycle.PACKED, "Development cabins start packed");
		try {
			registry.create(owner);
			helper.fail("A player must not be able to own two cabins");
		} catch (IllegalStateException expected) {
			helper.succeed();
		}
	}

	@GameTest
	public void registrySurvivesSaveReload(GameTestHelper helper) {
		CabinRegistry original = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		CabinRecord cabin = original.create(owner);

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinRecord restoredCabin = restored.find(cabin.uuid()).orElseThrow();

		helper.assertTrue(restoredCabin.equals(cabin), "Cabin record must survive save/reload");
		helper.assertTrue(restored.findByOwner(owner).orElseThrow().uuid().equals(cabin.uuid()),
			"Owner index must survive save/reload");
		helper.succeed();
	}

	@GameTest
	public void anchoredRegistrySchemasAreRejectedWithoutMutation(GameTestHelper helper) {
		for (int version = 0; version <= 7; version++) {
			CabinRegistry registry = new CabinRegistry();
			registry.create(UUID.randomUUID());
			var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
			encoded.asCompound().orElseThrow().putInt("schema_version", version);
			var unchanged = encoded.copy();
			helper.assertTrue(CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).error().isPresent(),
				"Anchored geometry must not be interpreted as centered geometry");
			helper.assertTrue(encoded.equals(unchanged), "Schema rejection must not alter saved data");
		}
		helper.succeed();
	}

	@GameTest
	public void currentSchemaPreservesWindowsFundsAndActiveInstallation(GameTestHelper helper) {
		CabinRegistry registry = new CabinRegistry();
		CabinRecord cabin = registry.create(UUID.randomUUID());
		CabinWindowState.Identity identity = new CabinWindowState.Identity(CabinWindowState.Wall.RIGHT, 1);
		CabinWindowState windows = CabinWindowState.EMPTY.install(
			identity, 0, List.of(new ItemStack(Items.GLASS_PANE, 3))
		);
		CabinUpgradeState.Target target = CabinUpgradeState.Target.window(identity);
		CabinUpgradeState.Requirement requirement = new CabinUpgradeState.Requirement(
			Items.AMETHYST_SHARD.builtInRegistryHolder().key().identifier(), 2
		);
		CabinUpgradeState.Fund fund = new CabinUpgradeState.Fund(
			target, List.of(requirement), List.of(new ItemStack(Items.AMETHYST_SHARD, 2))
		);
		CabinUpgradeState expected = new CabinUpgradeState(
			List.of(fund),
			Optional.of(CabinUpgradeState.Installation.window(UUID.randomUUID(), target, 1)),
			4L,
			windows
		);
		registry.updateUpgradeState(cabin.uuid(), expected);
		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();

		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinUpgradeState actual = restored.find(cabin.uuid()).orElseThrow().upgrades();
		assertUpgradeState(helper, expected, actual);
		helper.assertTrue(actual.reversal().isEmpty(),
			"Reload must not invent a window reversal journal");
		helper.succeed();
	}

	@GameTest
	public void upgradeStateSurvivesCodecAndCabinLifecycleChanges(GameTestHelper helper) {
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID trusted = UUID.randomUUID();
		CabinRecord cabin = registry.create(owner);
		ItemStack namedGlass = new ItemStack(Items.GLASS_PANE, 7);
		namedGlass.set(DataComponents.CUSTOM_NAME, Component.literal("Committed panes"));
		CabinUpgradeState state = new CabinUpgradeState(
			List.of(new CabinUpgradeState.Fund(
				CabinUpgradeState.Target.generalSpace(5),
				List.of(
					new CabinUpgradeState.Requirement(Items.GLASS_PANE.builtInRegistryHolder().key().identifier(), 12),
					new CabinUpgradeState.Requirement(Items.AMETHYST_SHARD.builtInRegistryHolder().key().identifier(), 2)
				),
				List.of(namedGlass)
			), new CabinUpgradeState.Fund(
				CabinUpgradeState.Target.generalSpace(7),
				List.of(new CabinUpgradeState.Requirement(
					Items.AMETHYST_SHARD.builtInRegistryHolder().key().identifier(), 4
				)),
				List.of(new ItemStack(Items.AMETHYST_SHARD, 1))
			)),
			Optional.empty(),
			7L
		);
		registry.updateUpgradeState(cabin.uuid(), state);

		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(10, 100, 10), Direction.NORTH);
		registry.trust(cabin.uuid(), owner, trusted);
		assertUpgradeState(helper, state, registry.find(cabin.uuid()).orElseThrow().upgrades());
		registry.setEntryPermission(cabin.uuid(), owner, CabinEntryPermission.TRUSTED_PLAYERS);
		assertUpgradeState(helper, state, registry.find(cabin.uuid()).orElseThrow().upgrades());
		registry.beginDeployment(cabin.uuid(), owner, exterior);
		registry.markInteriorGenerated(cabin.uuid());
		registry.finishDeployment(cabin.uuid());
		assertUpgradeState(helper, state, registry.find(cabin.uuid()).orElseThrow().upgrades());
		registry.beginPacking(cabin.uuid(), owner);
		registry.finishPacking(cabin.uuid());
		registry.markExteriorCleanupComplete(cabin.uuid());
		assertUpgradeState(helper, state, registry.find(cabin.uuid()).orElseThrow().upgrades());

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		assertUpgradeState(helper, state, restored.find(cabin.uuid()).orElseThrow().upgrades());
		helper.succeed();
	}

	@GameTest
	public void legacyTrackedFundCodecPreservesExactStacks(GameTestHelper helper) {
		CabinRegistry registry = new CabinRegistry();
		CabinRecord cabin = registry.create(UUID.randomUUID());
		ItemStack named = new ItemStack(Items.GLASS_PANE, 7);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Legacy panes"));
		CabinUpgradeState.Fund fund = new CabinUpgradeState.Fund(
			CabinUpgradeState.Target.generalSpace(5),
			List.of(new CabinUpgradeState.Requirement(
				Items.GLASS_PANE.builtInRegistryHolder().key().identifier(), 12
			)),
			List.of(named)
		);
		registry.updateUpgradeState(cabin.uuid(), new CabinUpgradeState(List.of(fund), Optional.empty(), 1L));
		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		var cabinTag = encoded.asCompound().orElseThrow().getListOrEmpty("cabins")
			.getCompoundOrEmpty(0).getCompoundOrEmpty("upgrades");
		cabinTag.remove("funds");
		var tracked = new net.minecraft.nbt.CompoundTag();
		tracked.put("target", CabinUpgradeState.Target.CODEC.encodeStart(NbtOps.INSTANCE, fund.target()).getOrThrow());
		tracked.put("requirements", CabinUpgradeState.Requirement.CODEC.listOf()
			.encodeStart(NbtOps.INSTANCE, fund.requirements()).getOrThrow());
		tracked.put("fund", ItemStack.CODEC.listOf().encodeStart(NbtOps.INSTANCE, fund.stacks()).getOrThrow());
		cabinTag.put("tracked", tracked);

		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinUpgradeState.Fund migrated = restored.find(cabin.uuid()).orElseThrow().upgrades()
			.fund(fund.target()).orElseThrow();
		helper.assertTrue(migrated.stacks().size() == 1
			&& migrated.stacks().getFirst().getCount() == 7
			&& Component.literal("Legacy panes").equals(
				migrated.stacks().getFirst().get(DataComponents.CUSTOM_NAME)),
			"Legacy fund codec stacks and components must survive in the matching target fund");
		helper.succeed();
	}

	@GameTest
	public void windowStatePreservesStableIdentityTiersAndExactReceipts(GameTestHelper helper) {
		ItemStack named = new ItemStack(Items.GLASS_PANE, 16);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Paid window panes"));
		CabinWindowState.Identity identity = new CabinWindowState.Identity(CabinWindowState.Wall.REAR, 1);
		CabinWindowState state = CabinWindowState.EMPTY
			.install(identity, 0, List.of(named))
			.install(identity, 1, List.of(new ItemStack(Items.AMETHYST_SHARD, 1)));
		var encoded = CabinWindowState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
		CabinWindowState restored = CabinWindowState.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinWindowState.Window window = restored.window(identity).orElseThrow();

		helper.assertTrue(window.tier() == 2 && window.receipts().size() == 2,
			"A cabin window must retain its stable identity and one receipt per paid tier");
		helper.assertTrue(window.receipts().getFirst().stacks().getFirst().getCount() == 16
			&& Component.literal("Paid window panes").equals(
				window.receipts().getFirst().stacks().getFirst().get(DataComponents.CUSTOM_NAME)),
			"Window receipts must preserve exact contributed stacks and components");
		helper.succeed();
	}

	@GameTest
	public void windowDowngradeAndRemovalReturnOnlyTheirExactReceipts(GameTestHelper helper) {
		CabinWindowState.Identity identity = new CabinWindowState.Identity(CabinWindowState.Wall.REAR, 0);
		ItemStack base = new ItemStack(Items.GLASS_PANE, 6);
		base.set(DataComponents.CUSTOM_NAME, Component.literal("Base receipt"));
		ItemStack second = new ItemStack(Items.AMETHYST_SHARD, 2);
		second.set(DataComponents.CUSTOM_NAME, Component.literal("Latest receipt"));
		CabinWindowState source = CabinWindowState.EMPTY
			.install(identity, 0, List.of(base))
			.install(identity, 1, List.of(second));

		CabinWindowState.ReversalResult downgraded = source.downgrade(identity, 2);
		helper.assertTrue(source.tier(identity) == 2 && downgraded.state().tier(identity) == 1,
			"Downgrade must return a new state without mutating its source");
		helper.assertTrue(downgraded.refundStacks().size() == 1
			&& ItemStack.matches(downgraded.refundStacks().getFirst(), second),
			"Downgrade must refund only the exact latest tier receipt");

		CabinWindowState.ReversalResult removed = source.remove(identity, 2);
		helper.assertTrue(removed.state().tier(identity) == 0 && removed.refundStacks().size() == 2
			&& ItemStack.matches(removed.refundStacks().get(0), base)
			&& ItemStack.matches(removed.refundStacks().get(1), second),
			"Removal must refund every exact receipt in tier order");
		CabinWindowState.Identity grandfatheredIdentity = new CabinWindowState.Identity(
			CabinWindowState.Wall.LEFT, 0
		);
		helper.assertTrue(CabinWindowState.grandfathered()
			.remove(grandfatheredIdentity, 1).refundStacks().isEmpty(),
			"Removing a grandfathered base window must not invent a refund");
		helper.succeed();
	}

	@GameTest
	public void windowGeometryReservesWallCentersThroughEveryExpansion(GameTestHelper helper) {
		long cell = 12;
		BlockPos center = PocketDimension.cellCenter(cell);
		var first = new CabinWindowState.Identity(CabinWindowState.Wall.LEFT, 0);
		var second = new CabinWindowState.Identity(CabinWindowState.Wall.LEFT, 1);
		var one = stateWithWindows(windowAtTier(first, 1));
		var pair = stateWithWindows(windowAtTier(first, 1), windowAtTier(second, 1));
		helper.assertTrue(!CabinWindowLayout.current(cell, 3, one).valid(),
			"A 3x3 room cannot fit a window beside its reserved passage and corner frame");
		var minimum = CabinWindowLayout.current(cell, 5, pair);
		helper.assertTrue(minimum.valid() && minimum.positions().size() == 4
			&& !minimum.positions().contains(center.offset(-3, 1, 0)),
			"Two base windows must fit at 5x5 while leaving the wall center solid");
		for (int size = 5; size <= 21; size += 2) {
			var bounds = PocketDimension.bounds(size);
			for (CabinWindowState.Wall wall : CabinWindowState.Wall.values()) {
				for (int tier = 1; tier <= 6; tier++) {
					var left = new CabinWindowState.Identity(wall, 0);
					var right = new CabinWindowState.Identity(wall, 1);
					var state = stateWithWindows(windowAtTier(left, tier), windowAtTier(right, tier));
					var layout = CabinWindowLayout.current(cell, size, state);
					if (!layout.valid()) {
						continue;
					}
					for (BlockPos pos : layout.positions()) {
						int horizontal = wall == CabinWindowState.Wall.REAR
							? pos.getX() - center.getX() : pos.getZ() - center.getZ();
						helper.assertTrue(horizontal != 0 && PocketDimension.isInteriorShell(cell, size, pos),
							"Every fitting pane must stay in the shell and away from the reserved center");
					}
					var remaining = CabinWindowLayout.current(cell, size, state.remove(left, tier).state());
					helper.assertTrue(remaining.positions().equals(layout.windows().get(1).positions()),
						"Removing one window must keep the other identity on its own side");
					var structural = PocketDimension.shellBlocks(cell, size, CabinPalette.DEFAULT).entrySet()
						.stream().filter(entry -> entry.getValue().is(CabinPalette.DEFAULT.walls().structuralWoodBlock()))
						.map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
					helper.assertTrue(java.util.Collections.disjoint(structural, layout.positions()),
						"Windows must preserve the corner frames");
				}
			}
		}
		var maximum = stateWithWindows(windowAtTier(first, 6), windowAtTier(second, 6));
		helper.assertTrue(CabinWindowLayout.current(cell, 21, maximum).positions().size() == 144
			&& !CabinWindowLayout.current(cell, 19, maximum).valid(),
			"Two maximum windows fit at 21x21, but not at 19x19");
		helper.succeed();
	}

	@GameTest
	public void windowPurchaseConsumesOnlyItsFundAndStoresExactReceipt(GameTestHelper helper) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(71L);
		CabinUpgradeDefinitions.Definitions definitions = testUpgradeDefinitions(attunement, 8, 4);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 45);
		cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 3, 5, 5);
		CabinWindowState.Identity identity = new CabinWindowState.Identity(CabinWindowState.Wall.REAR, 0);
		CabinUpgradeState.Target target = CabinUpgradeState.Target.window(identity);
		CabinUpgradeCatalog.Offer offer = CabinUpgradeCatalog.offer(cabin, target, attunement, definitions)
			.orElseThrow();
		TestExpansionEffect clear = new TestExpansionEffect(true, false);

		for (CabinUpgradeState.Requirement requirement : offer.requirements()) {
			ItemStack contribution = new ItemStack(
				BuiltInRegistries.ITEM.getOptional(requirement.itemId()).orElseThrow(), requirement.count()
			);
			if (contribution.is(Items.GLASS_PANE)) {
				contribution.set(DataComponents.CUSTOM_NAME, Component.literal("Exact paid panes"));
			}
			helper.assertTrue(CabinUpgradeService.deposit(
				registry, cabin.uuid(), owner, target, contribution, attunement, definitions, clear
			).success(), "Every exact base-window requirement must be fundable");
		}
		long revision = registry.find(cabin.uuid()).orElseThrow().upgrades().fundRevision();
		helper.assertTrue(CabinUpgradeService.install(
			registry, cabin.uuid(), owner, target, revision, attunement, definitions, clear, () -> { }
		).success(), "A complete valid base-window fund must install");

		CabinRecord installed = registry.find(cabin.uuid()).orElseThrow();
		CabinWindowState.Window window = installed.upgrades().windows().window(identity).orElseThrow();
		helper.assertTrue(window.tier() == 1 && installed.upgrades().fund(target).isEmpty(),
			"Window installation must advance exactly one tier and consume only its fund");
		helper.assertTrue(window.receipts().getFirst().stacks().stream().anyMatch(stack ->
			stack.is(Items.GLASS_PANE) && Component.literal("Exact paid panes").equals(
				stack.get(DataComponents.CUSTOM_NAME)
		)), "The installed tier must retain the exact component-bearing paid stack");
		CabinUpgradeCatalog.Offer second = CabinUpgradeCatalog.offer(
			installed,
			CabinUpgradeState.Target.window(new CabinWindowState.Identity(CabinWindowState.Wall.REAR, 1)),
			attunement,
			definitions
		).orElseThrow();
		helper.assertTrue(!second.locked(), "Installing a wall's first window must unlock its second window");
		helper.succeed();
	}

	@GameTest
	public void windowWorldEffectRejectsObstructionsAndMigratesLegacyPanels(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long cell = 90;
		PocketDimension.ensureCabinInterior(level, cell, CabinPalette.DEFAULT, 5);
		CabinRecord empty = cabinWithWindows(cell, 5, CabinWindowState.EMPTY);
		CabinWindowState.Identity rear = new CabinWindowState.Identity(CabinWindowState.Wall.REAR, 0);
		CabinWindowWorld world = new CabinWindowWorld(level.getServer(), level);
		CabinWindowLayout.Result target = CabinWindowLayout.installing(cell, 5, CabinWindowState.EMPTY, rear, 1);
		BlockPos pane = target.positions().iterator().next();

		helper.assertTrue(world.validateInstall(empty, rear, 1).success(),
			"A palette wall with no attachments must accept a fitting window");
		level.setBlockAndUpdate(pane, Blocks.CHEST.defaultBlockState());
		helper.assertTrue(!world.validateInstall(empty, rear, 1).success(),
			"A non-managed block in the footprint must obstruct installation");
		level.setBlockAndUpdate(pane, CabinPalette.DEFAULT.walls().planksBlock().defaultBlockState());
		BlockPos attached = pane.south();
		level.setBlockAndUpdate(attached, Blocks.WALL_TORCH.defaultBlockState());
		helper.assertTrue(!world.validateInstall(empty, rear, 1).success(),
			"A wall attachment that could be displaced must obstruct installation");
		level.setBlockAndUpdate(attached, Blocks.AIR.defaultBlockState());
		world.applyInstall(empty, rear, 1);
		helper.assertTrue(target.positions().stream().allMatch(position ->
			CabinWindows.isManagedWindowBlock(level.getBlockState(position).getBlock())
		), "A validated base installation must project functional panes across its full footprint");

		long legacyCell = 91;
		PocketDimension.ensureCabinInterior(level, legacyCell, CabinPalette.DEFAULT, 5);
		PocketDimension.InteriorBounds bounds = PocketDimension.bounds(5);
		BlockPos legacyCenter = PocketDimension.cellCenter(legacyCell);
		int legacyZ = 1;
		for (int x : new int[] {bounds.shellMinimumX(), bounds.shellMaximumX()}) {
			for (int y = 1; y <= 2; y++) {
				level.setBlockAndUpdate(
					legacyCenter.offset(x, y, legacyZ), Blocks.STAINED_GLASS.blue().defaultBlockState()
				);
			}
		}
		CabinRecord grandfathered = cabinWithWindows(legacyCell, 5, CabinWindowState.grandfathered());
		helper.assertTrue(new CabinWindowWorld(level.getServer(), level).reconcileProjection(grandfathered),
			"Legacy automatic panels must reconcile into centered state-derived panes");
		CabinWindowLayout.Result migrated = CabinWindowLayout.current(
			legacyCell, 5, CabinWindowState.grandfathered()
		);
		helper.assertTrue(migrated.positions().stream().allMatch(position ->
			CabinWindows.isManagedWindowBlock(level.getBlockState(position).getBlock())
		), "Every grandfathered identity must receive its centered pane projection");
		helper.assertTrue(level.getBlockState(
		legacyCenter.offset(bounds.shellMinimumX(), 1, legacyZ)
		).is(CabinPalette.DEFAULT.walls().planksBlock()),
			"The superseded legacy panel position must return to its palette wall block");
		helper.succeed();
	}

	@GameTest
	public void windowRemovalKeepsTheRemainingIdentityBesideItsPassage(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long cell = 92;
		int size = 5;
		PocketDimension.ensureCabinInterior(level, cell, CabinPalette.DEFAULT, size);
		CabinWindowState.Identity first = new CabinWindowState.Identity(CabinWindowState.Wall.REAR, 0);
		CabinWindowState.Identity second = new CabinWindowState.Identity(CabinWindowState.Wall.REAR, 1);
		CabinWindowState windows = stateWithWindows(windowAtTier(first, 1), windowAtTier(second, 1));
		CabinRecord cabin = cabinWithWindows(cell, size, windows);
		CabinWindowWorld world = new CabinWindowWorld(level.getServer(), level);
		helper.assertTrue(world.reconcileProjection(cabin),
			"The test cabin must project both persisted windows before removal");
		CabinWindowState resulting = windows.remove(first, 1).state();
		CabinWindowLayout.Result oldLayout = CabinWindowLayout.current(cell, size, windows);
		CabinWindowLayout.Result newLayout = CabinWindowLayout.current(cell, size, resulting);

		helper.assertTrue(world.validateChange(cabin, resulting).success(),
			"Removing slot one must leave slot two in place");
		world.applyChange(cabin, resulting);
		helper.assertTrue(newLayout.positions().stream().allMatch(position ->
			CabinWindows.isManagedWindowBlock(level.getBlockState(position).getBlock())),
			"The remaining slot-two identity must keep its own pane footprint");
		helper.assertTrue(oldLayout.positions().stream()
			.filter(position -> !newLayout.positions().contains(position))
			.allMatch(position -> level.getBlockState(position).is(CabinPalette.DEFAULT.walls().planksBlock())),
			"Every vacated pane must return to the cabin palette wall");

		world.applyChange(cabin, windows);
		BlockPos obstructed = oldLayout.positions().iterator().next();
		level.setBlockAndUpdate(obstructed, Blocks.CHEST.defaultBlockState());
		helper.assertTrue(!world.validateChange(cabin, resulting).success()
			&& level.getBlockState(obstructed).is(Blocks.CHEST),
			"A non-managed current pane must reject removal without mutating the obstruction");
		helper.succeed();
	}

	@GameTest
	public void fixedHeightRegistryIsRejectedWithoutMutation(GameTestHelper helper) {
		var root = new net.minecraft.nbt.CompoundTag();
		var legacyData = new net.minecraft.nbt.CompoundTag();
		legacyData.putInt("schema_version", 2);
		legacyData.putLong("next_cell_index", 3L);
		root.put("data", legacyData);
		var unchanged = root.copy();
		try {
			CabinRegistry.requireSupportedSchema(root, java.nio.file.Path.of("fixed-height-cabins.dat"));
			helper.fail("A fixed-height registry must fail before it can be loaded or replaced");
		} catch (IllegalStateException expected) {
			helper.assertTrue(expected.getMessage().contains("just fresh-world"),
				"Fixed-height-world failure must explain the fresh-world recovery command");
			helper.assertTrue(root.equals(unchanged),
				"Rejecting a fixed-height registry must not mutate its NBT data");
		}
		var migratableRoot = new net.minecraft.nbt.CompoundTag();
		var migratableData = new net.minecraft.nbt.CompoundTag();
		migratableData.putInt("schema_version", 8);
		migratableRoot.put("data", migratableData);
		CabinRegistry.requireSupportedSchema(migratableRoot, java.nio.file.Path.of("schema-three-cabins.dat"));
		var futureRoot = new net.minecraft.nbt.CompoundTag();
		var futureData = new net.minecraft.nbt.CompoundTag();
		futureData.putInt("schema_version", 10);
		futureRoot.put("data", futureData);
		try {
			CabinRegistry.requireSupportedSchema(futureRoot, java.nio.file.Path.of("future-cabins.dat"));
			helper.fail("A future registry schema must be rejected instead of guessed");
		} catch (IllegalStateException expected) {
			helper.assertTrue(expected.getMessage().contains("Unsupported cabin registry schema version 10"),
				"Future-schema rejection must identify the unsupported version");
		}

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, new CabinRegistry()).getOrThrow();
		helper.assertTrue(encoded.asCompound().orElseThrow().getIntOr("schema_version", 0) == 9,
			"Registries with corridor state must publish explicit schema version 9");
		helper.succeed();
	}

	@GameTest
	public void upgradeMenuUsesVirtualFundSlotsNotBlockStorage(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CabinUpgradeMenu menu = new CabinUpgradeMenu(41, player.getInventory(), UUID.randomUUID());

		for (int slot = CabinUpgradeMenu.FIRST_REQUIREMENT_SLOT;
			 slot < CabinUpgradeMenu.BOOK_SLOT; slot++) {
			helper.assertTrue(menu.getSlot(slot).isFake(),
				"Upgrade material controls must be synchronized virtual slots, not block storage slots");
		}
		helper.succeed();
	}

	@GameTest
	public void shiftClickingPlayerInventoryNeverAutoFundsAnUpgrade(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getInventory().setItem(9, new ItemStack(Items.GLASS_PANE, 12));
		CabinUpgradeMenu menu = new CabinUpgradeMenu(42, player.getInventory(), UUID.randomUUID());

		ItemStack moved = menu.quickMoveStack(player, CabinUpgradeMenu.FIRST_PLAYER_SLOT);
		int panes = player.getInventory().getNonEquipmentItems().stream()
			.filter(stack -> stack.is(Items.GLASS_PANE))
			.mapToInt(ItemStack::getCount)
			.sum();
		helper.assertTrue(!moved.isEmpty() && panes == 12,
			"Shift-clicking a player slot must only use ordinary inventory movement");
		helper.succeed();
	}

	@GameTest
	public void interiorControllerAlwaysOpensMenuAndNeverPurchasesDirectly(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CabinRegistry registry = new CabinRegistry();
		CabinRecord cabin = deployRegistryCabin(registry, player.getUUID(), 34);
		player.getInventory().setItem(0, new ItemStack(Items.OBSIDIAN, 64));

		player.setShiftKeyDown(false);
		helper.assertTrue(CabinUpgrades.useController(player, cabin, registry)
			== net.minecraft.world.InteractionResult.SUCCESS_SERVER
			&& player.containerMenu instanceof CabinUpgradeMenu,
			"Normal-use of the interior controller must open Cabin Upgrades");
		player.closeContainer();
		player.setShiftKeyDown(true);
		helper.assertTrue(CabinUpgrades.useController(player, cabin, registry)
			== net.minecraft.world.InteractionResult.SUCCESS_SERVER
			&& player.containerMenu instanceof CabinUpgradeMenu,
			"Sneak-use of the interior controller must open the same interface");
		CabinRecord unchanged = registry.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(unchanged.progression().generalSize() == CabinProgression.INITIAL_GENERAL_SIZE
			&& unchanged.upgrades().equals(CabinUpgradeState.EMPTY)
			&& player.getInventory().getItem(0).getCount() == 64,
			"Opening either way must not scan inventory, fund, or install an upgrade");
		player.closeContainer();
		helper.succeed();
	}

	@GameTest
	public void allocationDoesNotCollideAfterReload(GameTestHelper helper) {
		CabinRegistry original = new CabinRegistry();
		CabinRecord first = original.create(UUID.randomUUID());
		CabinRecord second = original.create(UUID.randomUUID());

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinRecord third = restored.create(UUID.randomUUID());

		helper.assertTrue(first.cellIndex() == 0, "First allocation must use cell zero");
		helper.assertTrue(second.cellIndex() == 1, "Second allocation must use cell one");
		helper.assertTrue(third.cellIndex() == 2, "Reloaded allocation must not reuse an occupied cell");
		helper.assertTrue(!first.uuid().equals(second.uuid()) && !second.uuid().equals(third.uuid()),
			"Cabin UUIDs must be unique");
		helper.succeed();
	}

	@GameTest
	public void worldAttunementIsSharedAndNeverRerollsOnReload(GameTestHelper helper) {
		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		WorldAttunement firstResolution = definitions.resolve(123456789L);
		CabinRegistry registry = new CabinRegistry();
		WorldAttunement firstPlayer = registry.resolveWorldAttunement(firstResolution);
		WorldAttunement secondPlayer = registry.resolveWorldAttunement(definitions.resolve(-987654321L));

		helper.assertTrue(firstPlayer.equals(secondPlayer),
			"Every player and cabin in one save must share one persisted world attunement");
		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		helper.assertTrue(restored.worldAttunement().orElseThrow().equals(firstResolution),
			"Restart must preserve the exact resolved materials and definition version");
		helper.succeed();
	}

	@GameTest
	public void expansionCostsFollowSavedPaletteAcrossWorldsAndReload(GameTestHelper helper) {
		var definitions = CabinUpgradeDefinitions.current();
		var spruce = CabinMaterialProfiles.woodProfile(PortablePocketCabin.id("vanilla/wood/spruce"))
			.orElseThrow().selection();
		var birch = CabinMaterialProfiles.woodProfile(PortablePocketCabin.id("vanilla/wood/birch"))
			.orElseThrow().selection();
		var palette = new CabinPalette(spruce, birch, spruce, CabinPalette.DEFAULT.door());
		var registry = new CabinRegistry();
		var cabin = registry.create(UUID.randomUUID(), palette);
		var firstWorld = new WorldAttunement(1, PortablePocketCabin.id("vanilla/wood/oak"));
		var unavailableWorld = new WorldAttunement(99, PortablePocketCabin.id("missing/wood"));
		var requirements = CabinUpgradeCatalog.next(cabin, firstWorld, definitions).orElseThrow().requirements();
		helper.assertTrue(requirements.contains(new CabinUpgradeState.Requirement(spruce.planks(), 8))
			&& requirements.contains(new CabinUpgradeState.Requirement(birch.planks(), 4))
			&& requirements.size() == 4,
			"A twelve-plank expansion must combine eight spruce floor/roof planks and four birch wall planks");
		helper.assertTrue(requirements.equals(CabinUpgradeCatalog.next(cabin, unavailableWorld, definitions)
			.orElseThrow().requirements()), "World materials and definition version must not change expansion costs");
		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		var restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		helper.assertTrue(requirements.equals(CabinUpgradeCatalog.next(restored.find(cabin.uuid()).orElseThrow(),
			unavailableWorld, definitions).orElseThrow().requirements()), "Reload must preserve palette-based costs");
		helper.succeed();
	}

	@GameTest
	public void palettePlankCostsKeepTheirTotalAndConsolidateMatchingItems(GameTestHelper helper) {
		var spruce = CabinMaterialProfiles.woodProfile(PortablePocketCabin.id("vanilla/wood/spruce"))
			.orElseThrow().selection();
		var birch = CabinMaterialProfiles.woodProfile(PortablePocketCabin.id("vanilla/wood/birch"))
			.orElseThrow().selection();
		var palette = new CabinPalette(spruce, birch, CabinPalette.DEFAULT.roof(), CabinPalette.DEFAULT.door());
		var definitions = CabinUpgradeDefinitions.current();
		for (int size = 5; size <= 21; size += 2) {
			int expected = 8 + ((size - 3) / 2) * 4;
			var requirements = CabinUpgradeCatalog.resolve(definitions.expansion(size).ingredients(), palette);
			int total = requirements.stream().filter(value -> List.of(spruce.planks(), birch.planks(),
				CabinPalette.DEFAULT.roof().planks()).contains(value.itemId())).mapToInt(
				CabinUpgradeState.Requirement::count).sum();
			helper.assertTrue(total == expected, "Splitting must preserve each level's fixed plank total");
		}
		var ingredients = List.of(new CabinUpgradeDefinitions.Ingredient(null, true, 16));
		var split = CabinUpgradeCatalog.resolve(ingredients, palette);
		helper.assertTrue(split.contains(new CabinUpgradeState.Requirement(spruce.planks(), 6))
			&& split.contains(new CabinUpgradeState.Requirement(birch.planks(), 5))
			&& split.contains(new CabinUpgradeState.Requirement(CabinPalette.DEFAULT.roof().planks(), 5)),
			"Remainders must go to floor, then walls, then roof");
		helper.assertTrue(CabinUpgradeCatalog.resolve(ingredients, CabinPalette.DEFAULT).equals(List.of(
			new CabinUpgradeState.Requirement(CabinPalette.DEFAULT.floor().planks(), 16))),
			"Matching floor, wall, and roof woods must remain one requirement");
		helper.succeed();
	}

	@GameTest
	public void obsoleteWorldMaterialsDoNotBlockMenuOrConsumeOldFunds(GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		var registry = CabinRegistry.get(helper.getLevel().getServer());
		var owner = player.getUUID();
		var cabin = deployRegistryCabin(registry, owner, 94);
		var target = CabinUpgradeState.Target.generalSpace(5);
		var obsolete = new WorldAttunement(99, PortablePocketCabin.id("missing/wood"));
		var legacyRegistry = new CabinRegistry();
		legacyRegistry.resolveWorldAttunement(obsolete);
		helper.assertTrue(CabinUpgradeCatalog.resolveAttunement(legacyRegistry, helper.getLevel(),
			CabinUpgradeDefinitions.current()).equals(obsolete),
			"Unavailable legacy world materials must not block upgrade access");
		var legacyFund = new CabinUpgradeState.Fund(target, List.of(
			new CabinUpgradeState.Requirement(Items.SPRUCE_PLANKS.builtInRegistryHolder().key().identifier(), 12)
		), List.of(new ItemStack(Items.SPRUCE_PLANKS, 3)));
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(legacyFund));
		cabin = registry.find(cabin.uuid()).orElseThrow();
		var definitions = CabinUpgradeDefinitions.current();
		helper.assertTrue(CabinUpgradeCatalog.isStale(legacyFund, cabin, obsolete, definitions),
			"A world-selected fund must be stale when it differs from the saved palette");
		var offered = new ItemStack(Items.OAK_PLANKS, 12);
		helper.assertTrue(!CabinUpgradeService.deposit(registry, cabin.uuid(), owner, target, offered,
			obsolete, definitions, new TestExpansionEffect(true, false)).success() && offered.getCount() == 12,
			"Stale funds must reject new contributions without consuming them");
		helper.assertTrue(!CabinUpgradeService.install(registry, cabin.uuid(), owner, target,
			cabin.upgrades().fundRevision(), obsolete, definitions, new TestExpansionEffect(true, false),
			() -> { }).success(), "Stale funds must not install using old material requirements");
		var withdrawn = CabinUpgradeService.withdraw(registry, cabin.uuid(), owner, target,
			Items.SPRUCE_PLANKS.builtInRegistryHolder().key().identifier(), 3);
		helper.assertTrue(withdrawn.success() && withdrawn.stack().is(Items.SPRUCE_PLANKS)
			&& withdrawn.stack().getCount() == 3, "Old world-selected materials must remain withdrawable");
		var menu = new CabinUpgradeMenu(95, player.getInventory(), cabin.uuid());
		helper.assertTrue(menu.attunedStack().isEmpty() && menu.requirementCount() == 3,
			"The menu must show palette costs without advertising world-selected wood");
		helper.assertTrue(java.util.stream.IntStream.range(0, menu.requirementCount()).anyMatch(index ->
			menu.requirementStack(index).is(Items.OAK_PLANKS) && menu.requiredCount(index) == 12),
			"After withdrawal the menu must show the cabin's twelve oak planks");
		helper.succeed();
	}

	@GameTest
	public void paletteCostsLoadWithoutWoodPoolAndAcceptLegacyPlankSlots(GameTestHelper helper) throws Exception {
		try (var stream = PortablePocketCabinGameTest.class.getResourceAsStream(
			"/data/portable_pocket_cabin/portable_pocket_cabin/progression/default.json")) {
			var json = net.minecraft.util.GsonHelper.parse(new java.io.InputStreamReader(stream,
				java.nio.charset.StandardCharsets.UTF_8));
			json.remove("wood_pool");
			var definitions = CabinUpgradeDefinitions.parse(json);
			var legacy = CabinUpgradeDefinitions.parse(net.minecraft.util.GsonHelper.parse(
				json.toString().replace("palette_slot", "attuned_slot")));
			helper.assertTrue(definitions.expansions().equals(legacy.expansions()),
				"Legacy plank slots must load as palette slots without requiring a world wood pool");
			var requirements = CabinUpgradeCatalog.resolve(definitions.expansion(5).ingredients(),
				CabinPalette.DEFAULT);
			helper.assertTrue(requirements.contains(new CabinUpgradeState.Requirement(
				CabinPalette.DEFAULT.floor().planks(), 12)), "Loaded palette slots must resolve to saved wood");
		}
		helper.succeed();
	}

	@GameTest
	public void generalSpaceUpgradeLadderReachesTwentyOneByTwentyOne(GameTestHelper helper) {
		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		helper.assertTrue(definitions.maximumGeneralSize() == 21,
			"General-space progression must end at 21x21");
		for (int size = 5; size <= 21; size += 2) {
			helper.assertTrue(definitions.expansion(size) != null,
				"Each of the nine odd-size expansions must have an upgrade definition");
		}
		helper.succeed();
	}

	@GameTest
	public void upgradeCatalogConsolidatesRequirementsAndFundsOnlyOnDeposit(GameTestHelper helper) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(17L);
		CabinUpgradeDefinitions.Definitions definitions = testUpgradeDefinitions(attunement, 8, 4);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID visitor = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 30);

		CabinUpgradeCatalog.Offer offer = CabinUpgradeCatalog.next(cabin, attunement, definitions).orElseThrow();
		List<CabinUpgradeCatalog.Group> groups = CabinUpgradeCatalog.groups(cabin, attunement, definitions);
		helper.assertTrue(groups.size() == 1 && groups.getFirst().id().equals(CabinUpgradeCatalog.CABIN_GROUP)
			&& groups.getFirst().panels().size() == 7
			&& groups.getFirst().panels().getFirst().equals(offer),
			"The Cabin group must expose general space followed by all six stable window panels");
		helper.assertTrue(groups.getFirst().panels().stream().skip(1)
			.map(CabinUpgradeCatalog.Offer::target).distinct().count() == 6,
			"Every eligible wall and slot must have one distinct stable window target");
		helper.assertTrue(offer.target().equals(CabinUpgradeState.Target.generalSpace(5)),
			"The catalog must expose only the next general-space size");
		helper.assertTrue(offer.requirements().size() == 2
			&& offer.requirements().stream().filter(requirement -> requirement.itemId().equals(
				Items.GLASS_PANE.builtInRegistryHolder().key().identifier()
			)).findFirst().orElseThrow().count() == 12,
			"Duplicate resolved item requirements must be consolidated");
		helper.assertTrue(cabin.upgrades().funds().isEmpty(),
			"Displaying an offer must not create persistent tracking or an empty fund");
		ItemStack visitorPanes = new ItemStack(Items.GLASS_PANE, 1);
		helper.assertTrue(!CabinUpgradeService.deposit(
			registry, cabin.uuid(), visitor, offer.target(), visitorPanes, attunement, definitions,
			new TestExpansionEffect(true, false)
		).success(), "A visitor must not create an upgrade fund");
		ItemStack ownerPanes = new ItemStack(Items.GLASS_PANE, 1);
		helper.assertTrue(CabinUpgradeService.deposit(
			registry, cabin.uuid(), owner, offer.target(), ownerPanes, attunement, definitions,
			new TestExpansionEffect(true, false)
		).success(), "The first deliberate deposit must create the target fund");

		CabinUpgradeDefinitions.Definitions changed = testUpgradeDefinitions(attunement, 9, 4);
		CabinRecord funded = registry.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(CabinUpgradeCatalog.isStale(funded.upgrades().fund(offer.target()).orElseThrow(), funded,
			attunement, changed), "A changed resolved requirement must make the target fund stale");
		helper.succeed();
	}

	@GameTest
	public void upgradePanelSelectionRetainsStableTargetsAndFallsBackDeterministically(GameTestHelper helper) {
		CabinUpgradeCatalog.Group cabin = syntheticGroup(
			"cabin", "Cabin", Items.OAK_DOOR,
			syntheticOffer(5, 2), syntheticOffer(7, 13)
		);
		CabinUpgradeCatalog.Group room = syntheticGroup(
			"stable", "Stable", Items.HAY_BLOCK,
			syntheticOffer(9, 2)
		);
		List<CabinUpgradeCatalog.Group> groups = List.of(cabin, room);

		CabinUpgradeSelection.Selected initial = CabinUpgradeSelection.resolve(
			groups, null, CabinUpgradeState.Target.generalSpace(7)
		).orElseThrow();
		helper.assertTrue(initial.groupIndex() == 0 && initial.panelIndex() == 1,
			"A refresh must retain the selected stable target while it remains visible");

		CabinUpgradeSelection.Selected next = CabinUpgradeSelection.cyclePanel(groups, initial, 1)
			.orElseThrow();
		helper.assertTrue(next.offer().target().equals(CabinUpgradeState.Target.generalSpace(5)),
			"Next-panel navigation must wrap within the active category");

		CabinUpgradeSelection.Selected selectedRoom = CabinUpgradeSelection.selectGroup(groups, 1)
			.orElseThrow();
		helper.assertTrue(selectedRoom.group().id().equals(PortablePocketCabin.id("stable"))
			&& selectedRoom.panelIndex() == 0,
			"Selecting a side tab must show the first panel in that category");

		List<CabinUpgradeCatalog.Group> refreshed = List.of(room);
		CabinUpgradeSelection.Selected fallback = CabinUpgradeSelection.resolve(
			refreshed, cabin.id(), initial.offer().target()
		).orElseThrow();
		helper.assertTrue(fallback.groupIndex() == 0 && fallback.panelIndex() == 0
			&& fallback.group().id().equals(room.id()),
			"A vanished target and category must fall back to the first visible panel");
		helper.assertTrue(CabinUpgradeSelection.selectGroup(groups, -1).isEmpty()
			&& CabinUpgradeSelection.selectGroup(groups, groups.size()).isEmpty(),
			"Forged category indexes must be rejected");
		helper.succeed();
	}

	@GameTest
	public void upgradeMenuSupportsThirteenRequirementsWithinSixteenSlotBoundary(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CabinUpgradeMenu menu = new CabinUpgradeMenu(43, player.getInventory(), UUID.randomUUID());
		CabinUpgradeCatalog.Offer thirteen = syntheticOffer(5, 13);
		CabinUpgradeCatalog.Offer sixteen = syntheticOffer(7, 16);
		CabinUpgradeCatalog.Offer seventeen = syntheticOffer(9, 17);

		helper.assertTrue(CabinUpgradeMenu.MAX_REQUIREMENTS == 16,
			"A window base purchase must fit thirteen exact requirements without paging");
		helper.assertTrue(CabinUpgradeSelection.isPresentable(thirteen, CabinUpgradeMenu.MAX_REQUIREMENTS)
			&& CabinUpgradeSelection.isPresentable(sixteen, CabinUpgradeMenu.MAX_REQUIREMENTS)
			&& !CabinUpgradeSelection.isPresentable(seventeen, CabinUpgradeMenu.MAX_REQUIREMENTS),
			"The panel boundary must support sixteen requirements and fail closed above it");
		for (int index = 0; index < CabinUpgradeMenu.MAX_REQUIREMENTS; index++) {
			var slot = menu.getSlot(CabinUpgradeMenu.FIRST_REQUIREMENT_SLOT + index);
			helper.assertTrue(slot.x == CabinUpgradeLayout.requirementX(index)
				&& slot.y == CabinUpgradeLayout.requirementY(index),
				"Requirement slots must occupy two deterministic rows of eight");
		}
		helper.assertTrue(CabinUpgradeLayout.requirementX(7) + 17 < CabinUpgradeLayout.SCREEN_WIDTH
			&& CabinUpgradeLayout.requirementY(15) + 26 <= CabinUpgradeLayout.STATUS_Y
			&& CabinUpgradeLayout.ACTION_Y + CabinUpgradeLayout.ACTION_HEIGHT
				< CabinUpgradeLayout.INVENTORY_LABEL_Y
			&& CabinUpgradeLayout.tabY(CabinUpgradeMenu.MAX_GROUPS - 1)
				+ CabinUpgradeLayout.TAB_SIZE <= CabinUpgradeLayout.SCREEN_HEIGHT,
			"Requirements, actions, and category controls must fit without overlapping the inventory");
		var firstInventorySlot = menu.getSlot(CabinUpgradeMenu.FIRST_PLAYER_SLOT);
		var firstHotbarSlot = menu.getSlot(CabinUpgradeMenu.FIRST_PLAYER_SLOT + 27);
		helper.assertTrue(firstInventorySlot.x == CabinUpgradeLayout.INVENTORY_X
			&& firstInventorySlot.y == CabinUpgradeLayout.INVENTORY_Y
			&& firstHotbarSlot.x == firstInventorySlot.x
			&& firstHotbarSlot.y == firstInventorySlot.y + 58,
			"Player slots must preserve vanilla inventory and hotbar spacing");
		helper.succeed();
	}

	@GameTest
	public void upgradeContributionsAreDeliberatePermissionedAndCapped(GameTestHelper helper) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(23L);
		CabinUpgradeDefinitions.Definitions definitions = testUpgradeDefinitions(attunement, 8, 4);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID trusted = UUID.randomUUID();
		UUID outsider = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 31);
		registry.trust(cabin.uuid(), owner, trusted);
		registry.setEntryPermission(cabin.uuid(), owner, CabinEntryPermission.TRUSTED_PLAYERS);
		CabinUpgradeState.Target target = CabinUpgradeState.Target.generalSpace(5);
		TestExpansionEffect clear = new TestExpansionEffect(true, false);

		ItemStack namedPanes = new ItemStack(Items.GLASS_PANE, 10);
		namedPanes.set(DataComponents.CUSTOM_NAME, Component.literal("Neighbour contribution"));
		CabinUpgradeService.Contribution first = CabinUpgradeService.deposit(
			registry, cabin.uuid(), trusted, target, namedPanes, attunement, definitions, clear
		);
		helper.assertTrue(first.success() && first.accepted() == 10 && namedPanes.isEmpty(),
			"A trusted player must deliberately contribute an offered required stack");

		ItemStack excess = new ItemStack(Items.GLASS_PANE, 8);
		CabinUpgradeService.Contribution capped = CabinUpgradeService.deposit(
			registry, cabin.uuid(), owner, target, excess, attunement, definitions, clear
		);
		helper.assertTrue(capped.success() && capped.accepted() == 2 && excess.getCount() == 6,
			"Only the outstanding quantity may enter the fund");
		ItemStack wrong = new ItemStack(Items.DIRT, 3);
		helper.assertTrue(!CabinUpgradeService.deposit(
			registry, cabin.uuid(), owner, target, wrong, attunement, definitions, clear
		).success() && wrong.getCount() == 3, "Wrong items must remain with the contributor");
		ItemStack blocked = new ItemStack(Items.AMETHYST_SHARD, 4);
		helper.assertTrue(!CabinUpgradeService.deposit(
			registry, cabin.uuid(), outsider, target, blocked, attunement, definitions, clear
		).success() && blocked.getCount() == 4, "Untrusted players cannot contribute");

		CabinUpgradeState.Fund fund = registry.find(cabin.uuid()).orElseThrow()
			.upgrades().fund(target).orElseThrow();
		helper.assertTrue(fund.fundedCount(Items.GLASS_PANE.builtInRegistryHolder().key().identifier()) == 12,
			"Concurrent contributions must never exceed the resolved requirement");
		helper.assertTrue(fund.stacks().stream().anyMatch(stack ->
			Component.literal("Neighbour contribution").equals(stack.get(DataComponents.CUSTOM_NAME))),
			"The fund must preserve contributed stack components");
		helper.succeed();
	}

	@GameTest
	public void upgradeFundAggregatesMoreThanOneStackAndRejectsObstructedDeposits(GameTestHelper helper) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(27L);
		CabinUpgradeDefinitions.Definitions definitions = testUpgradeDefinitions(attunement, 64, 12);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 35);
		CabinUpgradeState.Target target = CabinUpgradeState.Target.generalSpace(5);

		ItemStack blocked = new ItemStack(Items.GLASS_PANE, 8);
		helper.assertTrue(!CabinUpgradeService.deposit(
			registry, cabin.uuid(), owner, target, blocked, attunement, definitions,
			new TestExpansionEffect(false, false)
		).success() && blocked.getCount() == 8 && cabin.upgrades().funds().isEmpty(),
			"An obstructed upgrade must reject deposits without consuming the offered stack");

		TestExpansionEffect clear = new TestExpansionEffect(true, false);
		ItemStack first = new ItemStack(Items.GLASS_PANE, 64);
		ItemStack second = new ItemStack(Items.GLASS_PANE, 12);
		CabinUpgradeService.deposit(registry, cabin.uuid(), owner, target, first, attunement, definitions, clear);
		CabinUpgradeService.deposit(registry, cabin.uuid(), owner, target, second, attunement, definitions, clear);
		CabinUpgradeState.Fund fund = registry.find(cabin.uuid()).orElseThrow().upgrades()
			.fund(target).orElseThrow();
		helper.assertTrue(fund.fundedCount(Items.GLASS_PANE.builtInRegistryHolder().key().identifier()) == 76
			&& fund.stacks().size() == 2,
			"One virtual requirement icon must aggregate a capped queue larger than one item stack");
		helper.succeed();
	}

	@GameTest
	public void installationRejectsAStaleFundRevision(GameTestHelper helper) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(30L);
		CabinUpgradeDefinitions.Definitions definitions = testUpgradeDefinitions(attunement, 8, 4);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 36);
		fundTestUpgrade(registry, cabin.uuid(), owner, attunement, definitions);
		CabinUpgradeState.Target target = CabinUpgradeState.Target.generalSpace(5);
		long armedRevision = registry.find(cabin.uuid()).orElseThrow().upgrades().fundRevision();
		var paneId = Items.GLASS_PANE.builtInRegistryHolder().key().identifier();
		CabinUpgradeService.withdraw(registry, cabin.uuid(), owner, target, paneId, 1);

		CabinUpgradeService.Outcome result = CabinUpgradeService.install(
			registry, cabin.uuid(), owner, target, armedRevision, attunement, definitions,
			new TestExpansionEffect(true, false), () -> { }
		);
		helper.assertTrue(!result.success()
			&& registry.find(cabin.uuid()).orElseThrow().progression().generalSize() == 3,
			"A confirmation captured before a fund mutation must never install the upgrade");
		helper.succeed();
	}

	@GameTest
	public void ownerAndTrustedResidentsCanWithdrawExactFundStacks(GameTestHelper helper) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(29L);
		CabinUpgradeDefinitions.Definitions definitions = testUpgradeDefinitions(attunement, 8, 4);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID trusted = UUID.randomUUID();
		UUID visitor = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 32);
		registry.trust(cabin.uuid(), owner, trusted);
		registry.setEntryPermission(cabin.uuid(), owner, CabinEntryPermission.TRUSTED_PLAYERS);
		CabinUpgradeState.Target target = CabinUpgradeState.Target.generalSpace(5);
		ItemStack named = new ItemStack(Items.GLASS_PANE, 6);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Shared fund"));
		CabinUpgradeService.deposit(registry, cabin.uuid(), owner, target, named, attunement, definitions,
			new TestExpansionEffect(true, false));

		var itemId = Items.GLASS_PANE.builtInRegistryHolder().key().identifier();
		helper.assertTrue(!CabinUpgradeService.withdraw(
			registry, cabin.uuid(), visitor, target, itemId, 2
		).success(), "A guest must not withdraw upgrade materials");
		CabinUpgradeService.Withdrawal withdrawn = CabinUpgradeService.withdraw(
			registry, cabin.uuid(), trusted, target, itemId, 2
		);
		helper.assertTrue(withdrawn.success() && withdrawn.stack().getCount() == 2
			&& Component.literal("Shared fund").equals(withdrawn.stack().get(DataComponents.CUSTOM_NAME)),
			"A trusted resident must withdraw the oldest exact contributed stack");
		CabinUpgradeState.Fund remaining = registry.find(cabin.uuid()).orElseThrow().upgrades()
			.fund(target).orElseThrow();
		helper.assertTrue(remaining.fundedCount(itemId) == 4,
			"Withdrawing must leave the other target fund contents intact");
		helper.succeed();
	}

	@GameTest
	public void installationIsOwnerCommittedValidatedAndRestartRecoverable(GameTestHelper helper) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(31L);
		CabinUpgradeDefinitions.Definitions definitions = testUpgradeDefinitions(attunement, 8, 4);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID visitor = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 33);
		fundTestUpgrade(registry, cabin.uuid(), owner, attunement, definitions);
		CabinUpgradeState.Target target = CabinUpgradeState.Target.generalSpace(5);
		long revision = registry.find(cabin.uuid()).orElseThrow().upgrades().fundRevision();

		TestExpansionEffect blocked = new TestExpansionEffect(false, false);
		helper.assertTrue(!CabinUpgradeService.install(
			registry, cabin.uuid(), visitor, target, revision, attunement, definitions, blocked, () -> { }
		).success(), "A non-owner must not install a funded upgrade");
		helper.assertTrue(!CabinUpgradeService.install(
			registry, cabin.uuid(), owner, target, revision, attunement, definitions, blocked, () -> { }
		).success(), "An obstructed expansion must fail validation");
		helper.assertTrue(registry.find(cabin.uuid()).orElseThrow().upgrades().fund(target).isPresent(),
			"Failed installation checks must retain the complete fund");

		TestExpansionEffect interrupted = new TestExpansionEffect(true, true);
		int[] flushes = {0};
		helper.assertTrue(!CabinUpgradeService.install(
			registry, cabin.uuid(), owner, target, revision, attunement, definitions, interrupted,
			() -> flushes[0]++
		).success(), "A world-effect interruption must remain recoverable");
		helper.assertTrue(flushes[0] == 1
			&& registry.find(cabin.uuid()).orElseThrow().upgrades().installation().isPresent(),
			"Installation intent must be flushed before applying its world effect");

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		TestExpansionEffect resumed = new TestExpansionEffect(true, false);
		CabinUpgradeService.reconcileInstallation(restored, cabin.uuid(), resumed, () -> flushes[0]++);
		CabinRecord installed = restored.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(installed.progression().generalSize() == 5
			&& installed.upgrades().funds().isEmpty() && resumed.applications == 1,
			"Restart reconciliation must apply once and consume only the installed target fund");
		helper.succeed();
	}

	@GameTest
	public void windowReversalIsOwnerOnlyInvalidatesOnlyChangedFundsAndRecoversBothPhases(
		GameTestHelper helper
	) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(73L);
		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID trusted = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 46);
		registry.expandGeneralSpace(cabin.uuid(), owner, 3, 5, 21);
		cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 5, 7, 21);
		registry.trust(cabin.uuid(), owner, trusted);
		registry.setEntryPermission(cabin.uuid(), owner, CabinEntryPermission.TRUSTED_PLAYERS);
		CabinWindowState.Identity identity = new CabinWindowState.Identity(CabinWindowState.Wall.REAR, 0);
		CabinUpgradeState.Target target = CabinUpgradeState.Target.window(identity);
		ItemStack baseReceipt = new ItemStack(Items.GLASS_PANE, 3);
		ItemStack latestReceipt = new ItemStack(Items.AMETHYST_SHARD, 2);
		latestReceipt.set(DataComponents.CUSTOM_NAME, Component.literal("Tier two refund"));
		CabinWindowState windows = CabinWindowState.EMPTY
			.install(identity, 0, List.of(baseReceipt))
			.install(identity, 1, List.of(latestReceipt));
		registry.updateUpgradeState(cabin.uuid(), CabinUpgradeState.EMPTY.withWindows(windows));
		cabin = registry.find(cabin.uuid()).orElseThrow();

		CabinUpgradeCatalog.Offer nextWindow = CabinUpgradeCatalog.offer(
			cabin, target, attunement, definitions
		).orElseThrow();
		CabinUpgradeState.Target generalTarget = CabinUpgradeState.Target.generalSpace(9);
		CabinUpgradeCatalog.Offer general = CabinUpgradeCatalog.offer(
			cabin, generalTarget, attunement, definitions
		).orElseThrow();
		CabinUpgradeState.Fund invalidated = partialFund(nextWindow, 1);
		CabinUpgradeState.Fund unaffected = partialFund(general, 1);
		registry.updateUpgradeState(cabin.uuid(), new CabinUpgradeState(
			List.of(invalidated, unaffected), Optional.empty(), 9L, windows
		));
		long revision = 9L;
		TestReversalEffect interrupted = new TestReversalEffect(true, true);

		helper.assertTrue(!CabinWindowReversalService.reverse(
			registry, cabin.uuid(), trusted, target, CabinUpgradeState.ReversalAction.DOWNGRADE,
			revision, attunement, definitions, interrupted, (value, reversal) -> true, () -> { }
		).success(), "Trusted residents must not receive owner-only window reversal authority");
		helper.assertTrue(!CabinWindowReversalService.reverse(
			registry, cabin.uuid(), owner, target, CabinUpgradeState.ReversalAction.DOWNGRADE,
			revision - 1, attunement, definitions, interrupted, (value, reversal) -> true, () -> { }
		).success(), "A stale confirmation revision must not begin a window reversal");
		CabinWindowReversalService.Preview preview = CabinWindowReversalService.preview(
			registry.find(cabin.uuid()).orElseThrow(), target, CabinUpgradeState.ReversalAction.DOWNGRADE,
			attunement, definitions, interrupted
		);
		helper.assertTrue(preview.available() && preview.invalidatedFunds().equals(List.of(invalidated)),
			"Downgrade must invalidate the changed window fund while preserving an unaffected target fund");

		int[] flushes = {0};
		helper.assertTrue(!CabinWindowReversalService.reverse(
			registry, cabin.uuid(), owner, target, CabinUpgradeState.ReversalAction.DOWNGRADE,
			revision, attunement, definitions, interrupted, (value, reversal) -> true,
			() -> flushes[0]++
		).success(), "An interrupted world change must remain journaled for recovery");
		CabinUpgradeState pending = registry.find(cabin.uuid()).orElseThrow().upgrades();
		helper.assertTrue(flushes[0] == 1
			&& pending.reversal().orElseThrow().phase() == CabinUpgradeState.ReversalPhase.WORLD_PENDING,
			"World-pending intent must flush before any window blocks change");
		ItemStack blockedDeposit = new ItemStack(
			BuiltInRegistries.ITEM.getOptional(general.requirements().getFirst().itemId()).orElseThrow()
		);
		helper.assertTrue(!CabinUpgradeService.deposit(
			registry, cabin.uuid(), trusted, generalTarget, blockedDeposit, attunement, definitions,
			new TestExpansionEffect(true, false)
		).success() && blockedDeposit.getCount() == 1,
			"Either persisted operation journal must lock every cabin upgrade fund mutation");

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		TestReversalEffect resumed = new TestReversalEffect(true, false);
		List<ItemStack> pendingPayload = new ArrayList<>();
		helper.assertTrue(!CabinWindowReversalService.resume(
			restored, cabin.uuid(), resumed, (value, reversal) -> {
				pendingPayload.addAll(reversal.payloadStacks());
				return false;
			}, () -> flushes[0]++
		).success(), "Failed refund materialisation must retain an ejection-pending journal");
		CabinUpgradeState changed = restored.find(cabin.uuid()).orElseThrow().upgrades();
		helper.assertTrue(changed.windows().tier(identity) == 1
			&& changed.reversal().orElseThrow().phase() == CabinUpgradeState.ReversalPhase.EJECTION_PENDING
			&& changed.fund(target).isEmpty() && changed.fund(generalTarget).isPresent()
			&& resumed.applications == 1,
			"World recovery must commit the downgrade and only remove its recorded invalidated fund");
		helper.assertTrue(pendingPayload.size() == 2
			&& ItemStack.matches(pendingPayload.getFirst(), latestReceipt)
			&& ItemStack.matches(pendingPayload.get(1), invalidated.stacks().getFirst()),
			"The pending payload must preserve the latest receipt separately from invalidated fund stacks");

		helper.assertTrue(CabinWindowReversalService.resume(
			restored, cabin.uuid(), resumed, (value, reversal) -> true, () -> flushes[0]++
		).success(), "A later ejection retry must complete the persisted reversal");
		CabinUpgradeState completed = restored.find(cabin.uuid()).orElseThrow().upgrades();
		helper.assertTrue(completed.reversal().isEmpty() && completed.fund(generalTarget).isPresent()
			&& resumed.applications == 1,
			"Ejection-phase recovery must not reapply the already committed window change");
		helper.succeed();
	}

	@GameTest
	public void roomCellsArePermanentAndCannotCollide(GameTestHelper helper) {
		CabinRegistry registry = new CabinRegistry();
		UUID firstOwner = UUID.randomUUID();
		UUID secondOwner = UUID.randomUUID();
		CabinRecord first = registry.create(firstOwner);
		CabinRecord second = registry.create(secondOwner);
		CabinRoom firstRoom = registry.allocateRoom(
			first.uuid(), firstOwner, PortablePocketCabin.id("test_annex")
		);
		CabinRoom secondRoom = registry.allocateRoom(
			second.uuid(), secondOwner, PortablePocketCabin.id("test_annex")
		);

		helper.assertTrue(first.cellIndex() == 0 && second.cellIndex() == 1
			&& firstRoom.cellIndex() == 2 && secondRoom.cellIndex() == 3,
			"General rooms and disconnected room cells must share one collision-free allocator");
		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		helper.assertTrue(restored.nextCellIndex() == 4
			&& restored.findByCell(firstRoom.cellIndex()).orElseThrow().uuid().equals(first.uuid()),
			"Room identity, ownership, and allocator position must survive restart");
		helper.succeed();
	}

	@GameTest
	public void deploymentLifecycleAndExteriorSurviveSaveReload(GameTestHelper helper) {
		CabinRegistry original = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		CabinRecord cabin = original.create(owner);
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(20, 70, -12), Direction.WEST);

		CabinRecord deploying = original.beginDeployment(cabin.uuid(), owner, exterior);
		helper.assertTrue(deploying.lifecycle() == CabinLifecycle.DEPLOYING,
			"Deployment must be journalled before structure placement");
		helper.assertTrue(deploying.exterior().orElseThrow().equals(exterior),
			"The journalled transition must include its dimension-qualified exterior");

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinRecord restoredDeploying = restored.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(restoredDeploying.lifecycle() == CabinLifecycle.DEPLOYING,
			"A deployment journal must survive save/reload");
		helper.assertTrue(restoredDeploying.exterior().orElseThrow().equals(exterior),
			"Exterior location and facing must survive save/reload");

		restored.markInteriorGenerated(cabin.uuid());
		CabinRecord deployed = restored.finishDeployment(cabin.uuid());
		helper.assertTrue(deployed.lifecycle() == CabinLifecycle.DEPLOYED && deployed.interiorGenerated(),
			"A generated interior must be recorded before deployment commits");
		helper.succeed();
	}

	@GameTest
	public void onlyOwnerCanBeginDeployment(GameTestHelper helper) {
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		CabinRecord cabin = registry.create(owner);
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, Direction.SOUTH);

		try {
			registry.beginDeployment(cabin.uuid(), UUID.randomUUID(), exterior);
			helper.fail("A non-owner must not be able to deploy a cabin");
		} catch (IllegalStateException expected) {
			helper.assertTrue(registry.find(cabin.uuid()).orElseThrow().lifecycle() == CabinLifecycle.PACKED,
				"A rejected deployment must leave the cabin packed");
			helper.succeed();
		}
	}

	@GameTest
	public void exteriorUsesStableExactOwnedMask(GameTestHelper helper) {
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(4, 80, 9), Direction.NORTH);
		var blocks = ExteriorCabin.blocks(exterior);

		helper.assertTrue(blocks.keySet().stream().map(pos -> new BlockPos(pos.getX(), 0, pos.getZ()))
			.collect(java.util.stream.Collectors.toSet()).size() == 26,
			"The cabin must keep its original 5x5 footprint plus one entrance step");
		helper.assertTrue(blocks.get(ExteriorCabin.controller(exterior)).is(Blocks.LODESTONE),
			"The fixed exterior must have a distinct lodestone controller");
		helper.assertTrue(blocks.get(ExteriorCabin.doorLower(exterior)).is(Blocks.IRON_DOOR)
			&& blocks.get(ExteriorCabin.doorUpper(exterior)).is(Blocks.IRON_DOOR),
			"The fixed exterior must have a two-block entrance");
		helper.assertTrue(!ExteriorCabin.owns(exterior, ExteriorCabin.outsideDestination(exterior)),
			"The protected structure mask must not claim nearby player space");
		for (int lateral = -3; lateral <= 3; lateral++) {
			for (int depth = -2; depth <= 6; depth++) {
				for (int y = -1; y <= ExteriorCabin.RIDGE_Y + 1; y++) {
					BlockPos pos = ExteriorCabin.local(exterior, lateral, depth, y);
					helper.assertTrue(ExteriorCabin.owns(exterior, pos) == blocks.containsKey(pos),
						"Protection lookup must exactly match the placed block mask at " + pos);
				}
			}
		}
		helper.assertTrue(ExteriorCabin.touchesChunk(
			exterior, ChunkPos.containing(ExteriorCabin.controller(exterior))
		), "Chunk-load reconciliation must recognize a chunk containing the exterior");
		helper.assertTrue(!ExteriorCabin.touchesChunk(
			exterior, new ChunkPos(10_000, 10_000)
		), "Chunk-load reconciliation must ignore unrelated chunks");
		helper.succeed();
	}

	@GameTest
	public void progressionInteriorStartsThreeByThreeAroundItsCenter(GameTestHelper helper) {
		long cell = 7;
		BlockPos center = PocketDimension.cellCenter(cell);
		var bounds = PocketDimension.bounds(3);
		helper.assertTrue(bounds.minimumX() == -1 && bounds.maximumX() == 1
			&& bounds.minimumZ() == -1 && bounds.maximumZ() == 1,
			"A new cabin must have a centered 3x3 usable floor");
		helper.assertTrue(PocketDimension.isWithinUsable(cell, 3, center.offset(1, 2, 1))
			&& !PocketDimension.isWithinUsable(cell, 3, center.offset(1, 3, 1)),
			"A new cabin must have exactly two clear blocks of height");
		helper.assertTrue(PocketDimension.isInteriorShell(cell, center)
			&& PocketDimension.isInteriorShell(cell, center.offset(2, 1, 0)),
			"The floor and walls must be protected");
		helper.assertTrue(PocketDimension.interiorEntrance(cell, 3).equals(center.offset(0, 1, 1))
			&& PocketDimension.isInteriorExit(cell, 3, center.offset(0, 1, 2)),
			"The entrance must be centered on the south wall");
		helper.succeed();
	}

	@GameTest
	public void ceilingHeightGrowsWithEachExpansionAndCapsAtTen(GameTestHelper helper) {
		long cell = 8;
		BlockPos center = PocketDimension.cellCenter(cell);
		for (int size = 3; size <= 21; size += 2) {
			int height = Math.min(10, 2 + (size - 3) / 2);
			var shell = PocketDimension.shellBlocks(cell, size, CabinPalette.DEFAULT);
			helper.assertTrue(PocketDimension.clearInteriorHeight(size) == height
				&& shell.containsKey(center.offset(0, height + 1, 0))
				&& !shell.containsKey(center.offset(0, height + 2, 0)),
				"Each size must derive one ceiling, with ten clear blocks from 19x19 onward");
		}
		helper.succeed();
	}

	@GameTest
	public void expansionGrowsEverySideAndMovesTheSouthEntrance(GameTestHelper helper) {
		long cell = 9;
		for (int size = 3; size < 21; size += 2) {
			var oldBounds = PocketDimension.bounds(size);
			var next = PocketDimension.bounds(size + 2);
			helper.assertTrue(next.minimumX() == oldBounds.minimumX() - 1
				&& next.maximumX() == oldBounds.maximumX() + 1
				&& next.minimumZ() == oldBounds.minimumZ() - 1
				&& next.maximumZ() == oldBounds.maximumZ() + 1,
				"Each expansion must add one block on all four sides");
			BlockPos oldDoor = PocketDimension.interiorExitDoorLower(cell, size);
			BlockPos newDoor = PocketDimension.interiorExitDoorLower(cell, size + 2);
			helper.assertTrue(newDoor.equals(oldDoor.south())
				&& PocketDimension.isInteriorExit(cell, size + 2, newDoor)
				&& !PocketDimension.isInteriorExit(cell, size + 2, oldDoor),
				"Exit detection must follow the south wall and stop recognizing the old door");
			helper.assertTrue(PocketDimension.interiorController(cell, size + 2)
				.equals(PocketDimension.interiorController(cell, size).south()),
				"The upgrade controller must follow the exit wall");
		}
		helper.succeed();
	}

	@GameTest
	public void generalExpansionPreservesPlayerBlocksAndRejectsObstructions(GameTestHelper helper) {
		long cell = 50;
		BlockPos center = PocketDimension.cellCenter(cell);
		BlockPos playerBlock = center.above();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos obstruction = center.above().relative(direction, 3);
			helper.assertTrue(!PocketDimension.validateExpansion(cell, 3, 5, obstruction::equals).valid(),
				"An obstruction on any new wall must reject expansion");
		}
		BlockPos ceilingObstruction = center.offset(0, 4, 0);
		helper.assertTrue(!PocketDimension.validateExpansion(cell, 3, 5, ceilingObstruction::equals).valid(),
			"An obstruction above the old ceiling must reject expansion");
		helper.assertTrue(PocketDimension.validateExpansion(cell, 3, 5, playerBlock::equals).valid(),
			"Player blocks already inside the main room must not obstruct expansion");
		helper.assertTrue(PocketDimension.isWithinUsable(cell, 3, playerBlock)
			&& PocketDimension.isWithinUsable(cell, 5, playerBlock)
			&& PocketDimension.isWithinUsable(cell, 5, center.offset(-2, 3, -2)),
			"Player coordinates stay usable while new rows and the old ceiling become usable");
		helper.assertTrue(!PocketDimension.validateExpansion(cell, 3, 7, pos -> false).valid()
			&& !PocketDimension.validateExpansion(cell, 21, 23, pos -> false).valid()
			&& !PocketDimension.validateExpansion(cell, 4, 6, pos -> false).valid(),
			"Expansion must reject skipped steps, even sizes, and sizes above the cap");
		helper.succeed();
	}

	@GameTest
	public void allNineExpansionsPreserveContentsAndMoveManagedFixtures(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long cell = 99;
		BlockPos center = PocketDimension.cellCenter(cell);
		// The headless test server omits custom dimensions; reproduce the pocket's empty volume here.
		for (int x = -11; x <= 11; x++) {
			for (int z = -11; z <= 11; z++) {
				for (int y = 0; y <= 11; y++) {
					level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(),
						net.minecraft.world.level.block.Block.UPDATE_SKIP_ALL_SIDEEFFECTS
							| net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				}
			}
		}
		BlockPos chestPos = center.above();
		BlockPos decoration = center.offset(-1, 1, -1);
		PocketDimension.ensureCabinInterior(level, cell);
		CabinRecord cabin = cabinWithWindows(cell, CabinWindowState.EMPTY);
		level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(decoration, Blocks.DIAMOND_BLOCK.defaultBlockState());
		var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chestPos);
		ItemStack named = new ItemStack(Items.EMERALD, 19);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Stay at the center"));
		chest.setItem(0, named);
		CabinWindowWorld windows = new CabinWindowWorld(level.getServer(), level);
		for (int size = 3; size < 21; size += 2) {
			int next = size + 2;
			BlockPos oldDoor = PocketDimension.interiorExitDoorLower(cell, size);
			BlockPos oldController = PocketDimension.interiorController(cell, size);
			var geometry = PocketDimension.validateExpansion(level, cell, size, next);
			var relayout = windows.validateRelayout(cabin, next);
			helper.assertTrue(geometry.valid() && relayout.success(),
				"Expansion to " + next + " must accept clear space: " + geometry.message() + "; " + relayout.message());
			PocketDimension.applyGeneralSpaceExpansion(level, cabin, next);
			windows.applyRelayout(cabin, next);
			// Replaying an interrupted world effect before its registry commit must preserve contents.
			PocketDimension.applyGeneralSpaceExpansion(level, cabin, next);
			windows.applyRelayout(cabin, next);
			helper.assertTrue(level.getBlockEntity(chestPos) == chest && ItemStack.matches(chest.getItem(0), named)
				&& level.getBlockState(decoration).is(Blocks.DIAMOND_BLOCK),
				"Expansion and recovery must keep placed blocks and exact chest contents at their coordinates");
			helper.assertTrue(level.getBlockState(oldDoor).isAir() && level.getBlockState(oldDoor.above()).isAir()
				&& level.getBlockState(oldController).isAir(),
				"Moved doors and controllers must leave no fixtures inside the expanded main room");
			for (BlockPos door : List.of(PocketDimension.interiorExitDoorLower(cell, next),
				PocketDimension.interiorExitDoorUpper(cell, next))) {
				helper.assertTrue(level.getBlockState(door).is(cabin.palette().door().doorBlock()),
					"Both door halves must remain on the new south wall");
			}
			helper.assertTrue(level.getBlockState(PocketDimension.interiorController(cell, next)).is(Blocks.LODESTONE),
				"The upgrade controller must remain beside the moved door");
			var state = cabin.upgrades().windows();
			if (size == 3) {
				state = state.install(new CabinWindowState.Identity(CabinWindowState.Wall.LEFT, 0), 0, List.of());
			}
			cabin = cabinWithWindows(cell, next, state);
			windows.reconcileProjection(cabin);
			for (BlockPos pane : CabinWindowLayout.current(cell, next, state).positions()) {
				helper.assertTrue(CabinWindows.isManagedWindowBlock(level.getBlockState(pane).getBlock()),
					"Purchased windows must move with the wall through every expansion");
			}
		}
		helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
			new net.minecraft.world.phys.AABB(center).inflate(15)).isEmpty(),
			"Moving generated doors must not create free door-item drops");
		helper.succeed();
	}

	@GameTest
	public void legacyDebugPlatformDoesNotBlockFirstExpansion(GameTestHelper helper) {
		long cell = 61;
		ServerLevel level = helper.getLevel();
		PocketDimension.ensureDebugMarker(level, cell);
		PocketDimension.ensureCabinInterior(level, cell, CabinPalette.DEFAULT, 3);

		PocketDimension.ExpansionCheck check = PocketDimension.validateExpansion(level, cell, 3, 5);
		helper.assertTrue(check.valid(),
			"The old command-created debug platform must not leave a rim in the first expansion footprint");
		BlockPos isolatedSmoothStone = PocketDimension.cellCenter(cell).offset(-3, 0, 0);
		level.setBlockAndUpdate(isolatedSmoothStone, Blocks.SMOOTH_STONE.defaultBlockState());
		helper.assertTrue(PocketDimension.removeLegacyDebugPlatformResidue(level, cell, 3) == 0
			&& level.getBlockState(isolatedSmoothStone).is(Blocks.SMOOTH_STONE),
			"Cleanup must preserve smooth stone that does not form the complete legacy debug rim");
		helper.succeed();
	}

	@GameTest
	public void upgradeFundEjectionPositionIsInsideTheCabin(GameTestHelper helper) {
		long cell = 62;
		CabinRecord cabin = new CabinRecord(UUID.randomUUID(), UUID.randomUUID(), cell, CabinLifecycle.PACKED);
		BlockPos controller = PocketDimension.interiorController(cell);
		BlockPos drop = CabinFundEjection.dropPosition(cabin);

		helper.assertTrue(controller.distManhattan(drop) == 1,
			"Refunded materials must appear beside the interior controller");
		helper.assertTrue(PocketDimension.isWithinUsable(cell, CabinProgression.INITIAL_GENERAL_SIZE, drop),
			"Refunded materials must appear on the room side of the controller wall");
		helper.succeed();
	}

	@GameTest
	public void markedRefundEjectionFillsOnlyMissingIndexesAndRejectsMismatches(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long cell = 63;
		PocketDimension.ensureCabinInterior(level, cell, CabinPalette.DEFAULT, 3);
		CabinRecord cabin = cabinWithWindows(cell, CabinWindowState.EMPTY);
		BlockPos drop = CabinFundEjection.dropPosition(cabin);
		level.setBlockAndUpdate(drop, Blocks.CHEST.defaultBlockState());
		UUID operation = UUID.randomUUID();
		ItemStack first = new ItemStack(Items.GLASS_PANE, 3);
		first.set(DataComponents.CUSTOM_NAME, Component.literal("Indexed refund"));
		ItemStack second = new ItemStack(Items.AMETHYST_SHARD, 2);

		helper.assertTrue(CabinFundEjection.ejectMarkedInCabinLevel(level, cabin, operation, List.of(first)),
			"A refund must materialise at the fixed drop position even when its block is occupied");
		helper.assertTrue(CabinFundEjection.ejectMarkedInCabinLevel(
			level, cabin, operation, List.of(first, second)
		) && CabinFundEjection.ejectMarkedInCabinLevel(level, cabin, operation, List.of(first, second)),
			"Recovery must create a missing indexed entry and accept an already complete batch");
		String markerPrefix = PortablePocketCabin.MOD_ID + ".refund." + operation + ".";
		List<net.minecraft.world.entity.item.ItemEntity> entities = level.getEntitiesOfClass(
			net.minecraft.world.entity.item.ItemEntity.class,
			new net.minecraft.world.phys.AABB(drop).inflate(2.0),
			entity -> entity.entityTags().stream().anyMatch(tag -> tag.startsWith(markerPrefix))
		);
		helper.assertTrue(entities.size() == 2,
			"Retrying a partial or complete marked batch must leave exactly one entity per payload index");
		entities.stream().filter(entity -> entity.entityTags().contains(markerPrefix + "0"))
			.findFirst().orElseThrow().setItem(new ItemStack(Items.DIRT));
		try {
			CabinFundEjection.ejectMarkedInCabinLevel(level, cabin, operation, List.of(first, second));
			helper.fail("A mismatched persisted refund index must not be accepted");
		} catch (IllegalStateException expected) {
			helper.assertTrue(expected.getMessage().contains("index 0") && entities.size() == 2,
				"A mismatched persisted index must report its index without spawning replacements");
		}
		helper.succeed();
	}

	@GameTest
	public void maximumRoomEnvelopesRemainIsolatedAcrossCabins(GameTestHelper helper) {
		var first = PocketDimension.shellBlocks(0, CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE,
			CabinPalette.DEFAULT).keySet();
		var second = PocketDimension.shellBlocks(1, CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE,
			CabinPalette.DEFAULT).keySet();
		helper.assertTrue(java.util.Collections.disjoint(first, second),
			"Declared maximum general rooms must never overlap an adjacent allocated cell");
		helper.succeed();
	}

	@GameTest
	public void packingPreservesInteriorIdentityAndAdvancesItemGeneration(GameTestHelper helper) {
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		CabinRecord cabin = registry.create(owner);
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(8, 72, 8), Direction.SOUTH);

		registry.beginDeployment(cabin.uuid(), owner, exterior);
		registry.markInteriorGenerated(cabin.uuid());
		registry.finishDeployment(cabin.uuid());
		CabinRecord packing = registry.beginPacking(cabin.uuid(), owner);
		helper.assertTrue(packing.deploymentItemDeliveryPending() && packing.lastDeploymentItemId().isPresent(),
			"Packing must journal its reserved physical item identity before inventory mutation");
		CabinRecord packed = registry.finishPacking(cabin.uuid());

		helper.assertTrue(packed.lifecycle() == CabinLifecycle.PACKED && packed.exterior().isEmpty(),
			"Finished packing must disable the active exterior");
		helper.assertTrue(packed.lastExterior().orElseThrow().equals(exterior),
			"Packing must retain the last valid campsite");
		helper.assertTrue(packed.interiorGenerated() && packed.cellIndex() == cabin.cellIndex(),
			"Packing must not change interior identity");
		helper.assertTrue(packed.packedItemGeneration() == 1,
			"Every completed packing operation must advance the packed item generation");
		helper.assertTrue(packed.exteriorCleanupPending(),
			"Packing must journal physical exterior cleanup before removing blocks");
		helper.assertTrue(packed.deploymentItemDeliveryPending(),
			"A completed pack must remain owed until its reserved item is activated");
		helper.assertTrue(!registry.resolveDeploymentItemDelivery(cabin.uuid()).deploymentItemDeliveryPending(),
			"Activating the packed item must resolve the persisted delivery obligation");
		helper.succeed();
	}

	@GameTest
	public void boundCabinItemCarriesUuidAndGeneration(GameTestHelper helper) {
		CabinRecord cabin = new CabinRecord(UUID.randomUUID(), UUID.randomUUID(), 3, CabinLifecycle.PACKED);
		var stack = CabinItems.createBound(cabin, 7, false);
		CabinItems.Binding binding = CabinItems.binding(stack).orElseThrow();

		helper.assertTrue(binding.cabinId().equals(cabin.uuid()),
			"A packed item must identify its authoritative cabin record");
		helper.assertTrue(binding.generation() == 7 && !binding.pending(),
			"A packed item must preserve its generation and pending status");
		helper.succeed();
	}

	@GameTest
	public void craftedCabinKitStartsUnbound(GameTestHelper helper) {
		ItemStack kit = CabinItems.createUnbound(CabinPalette.DEFAULT);
		helper.assertTrue(CabinItems.isUnbound(kit) && CabinItems.binding(kit).isEmpty(),
			"A crafted cabin kit must not claim an interior before its first successful deployment");
		var recipeKey = net.minecraft.resources.ResourceKey.create(
			Registries.RECIPE, PortablePocketCabin.id("cabin_kit")
		);
		helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(recipeKey).isPresent(),
			"The survival cabin-kit recipe must be loaded");
		helper.succeed();
	}

	@GameTest
	public void componentRecipesAndMaterialProfilesLoad(GameTestHelper helper) {
		var manager = helper.getLevel().getServer().getRecipeManager();
		for (String recipe : List.of(
			"dimensional_logic_core", "dimensional_anchor", "dimensional_folding_core",
			"dimensional_foundation", "cabin_kit"
		)) {
			var key = net.minecraft.resources.ResourceKey.create(
				Registries.RECIPE, PortablePocketCabin.id(recipe)
			);
			helper.assertTrue(manager.byKey(key).isPresent(), "Recipe must load: " + recipe);
		}
		helper.assertTrue(CabinMaterialProfiles.woodProfileCount() == 12,
			"Every vanilla wood family must have one unambiguous loaded profile");
		helper.assertTrue(CabinMaterialProfiles.doorProfileCount() == 21,
			"Every vanilla wood, iron, and copper door variant must have a loaded profile");
		helper.succeed();
	}

	@GameTest
	public void cabinKitRecipeCapturesIndependentPaletteRoles(GameTestHelper helper) {
		CabinKitRecipe recipe = new CabinKitRecipe();
		var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(
			new ItemStack(Items.SPRUCE_PLANKS), new ItemStack(Items.SPRUCE_PLANKS),
			new ItemStack(Items.SPRUCE_PLANKS), new ItemStack(Items.BIRCH_LOG),
			new ItemStack(Items.COPPER_DOOR.asList().getFirst()), new ItemStack(Items.BIRCH_LOG),
			new ItemStack(Items.CHERRY_PLANKS), new ItemStack(CabinItems.DIMENSIONAL_FOUNDATION),
			new ItemStack(Items.CHERRY_PLANKS)
		));
		helper.assertTrue(recipe.matches(input, helper.getLevel()),
			"The custom recipe must accept independent supported roof, wall, floor, and door roles");
		CabinItems.Unbound first = CabinItems.unbound(recipe.assemble(input)).orElseThrow();
		CabinItems.Unbound second = CabinItems.unbound(recipe.assemble(input)).orElseThrow();
		helper.assertTrue(first.palette().roof().planksBlock() == Blocks.SPRUCE_PLANKS
			&& first.palette().walls().structuralWoodBlock() == Blocks.BIRCH_LOG
			&& first.palette().floor().planksBlock() == Blocks.CHERRY_PLANKS
			&& first.palette().door().doorBlock() == Blocks.COPPER_DOOR.asList().getFirst(),
			"The crafted Kit must persist the exact recipe-selected material palette");
		helper.assertTrue(!first.itemInstanceId().equals(second.itemInstanceId()),
			"Every physical crafting result must receive an immutable unique Kit identity");
		var mismatched = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(
			new ItemStack(Items.SPRUCE_PLANKS), new ItemStack(Items.OAK_PLANKS),
			new ItemStack(Items.SPRUCE_PLANKS), new ItemStack(Items.BIRCH_LOG),
			new ItemStack(Items.COPPER_DOOR.asList().getFirst()), new ItemStack(Items.BIRCH_LOG),
			new ItemStack(Items.CHERRY_PLANKS), new ItemStack(CabinItems.DIMENSIONAL_FOUNDATION),
			new ItemStack(Items.CHERRY_PLANKS)
		));
		helper.assertTrue(!recipe.matches(mismatched, helper.getLevel()),
			"Mismatched ingredients within one palette role must be rejected");
		helper.succeed();
	}

	@GameTest
	public void paletteAndDeploymentItemObligationSurviveRecovery(GameTestHelper helper) {
		CabinPalette palette = new CabinPalette(
			CabinMaterialProfiles.matchPlanks(new ItemStack(Items.CHERRY_PLANKS)).orElseThrow(),
			CabinMaterialProfiles.matchStructuralWood(new ItemStack(Items.SPRUCE_LOG)).orElseThrow(),
			CabinMaterialProfiles.matchPlanks(new ItemStack(Items.BAMBOO_PLANKS)).orElseThrow(),
			CabinMaterialProfiles.matchDoor(new ItemStack(Items.IRON_DOOR)).orElseThrow()
		);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		CabinRecord cabin = registry.create(owner, palette);
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, Direction.NORTH);
		CabinRecord deploying = registry.beginDeployment(cabin.uuid(), owner, exterior, itemId);
		helper.assertTrue(deploying.deploymentItemDeliveryPending()
			&& deploying.lastDeploymentItemId().orElseThrow().equals(itemId),
			"Deployment must journal the physical source item obligation before world mutation");
		CabinRecord rolledBack = registry.rollbackDeployment(cabin.uuid());
		helper.assertTrue(rolledBack.lifecycle() == CabinLifecycle.PACKED
			&& rolledBack.deploymentItemDeliveryPending() && rolledBack.palette().equals(palette),
			"Rollback must preserve the palette and keep delivery owed");

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		CabinRecord restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow()
			.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(restored.palette().equals(palette)
			&& restored.lastDeploymentItemId().orElseThrow().equals(itemId)
			&& restored.deploymentItemDeliveryPending(),
			"Palette and unresolved item delivery must survive save/reload");
		helper.succeed();
	}

	@GameTest
	public void logCabinRotatesCoursesAndKeepsFloorSpace(GameTestHelper helper) {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			var exterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, facing);
			var blocks = ExteriorCabin.blocks(exterior);
			for (int lateral : new int[] {-2, 2}) {
				for (int depth : new int[] {0, 4}) {
					helper.assertTrue(blocks.get(ExteriorCabin.local(exterior, lateral, depth, 4))
						.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y,
						"Each corner must keep one vertical post");
				}
				helper.assertTrue(blocks.get(ExteriorCabin.local(exterior, lateral, 1, 3))
					.getValue(BlockStateProperties.AXIS) == facing.getAxis(),
					"Side wall logs must run front-to-back");
			}
			helper.assertTrue(blocks.get(ExteriorCabin.local(exterior, 1, 4, 3))
				.getValue(BlockStateProperties.AXIS) == facing.getClockWise().getAxis(),
				"Front and rear logs must run across the wall");
			for (int x = -1; x <= 1; x++) {
				for (int z = 1; z <= 3; z++) {
					for (int y = 1; y <= 4; y++) {
						helper.assertTrue(!blocks.containsKey(ExteriorCabin.local(exterior, x, z, y)),
							"The existing exterior room must keep its full clear height and floor area");
					}
				}
			}
			for (int depth = 0; depth <= 4; depth++) {
				helper.assertTrue(blocks.get(ExteriorCabin.local(exterior, -2, depth, 5))
					.getValue(net.minecraft.world.level.block.StairBlock.FACING) == facing.getClockWise(),
					"Left roof stairs must rise toward the ridge");
				helper.assertTrue(blocks.get(ExteriorCabin.local(exterior, 2, depth, 5))
					.getValue(net.minecraft.world.level.block.StairBlock.FACING) == facing.getCounterClockWise(),
					"Right roof stairs must rise toward the ridge");
				helper.assertTrue(blocks.get(ExteriorCabin.local(exterior, 0, depth, 7)).is(Blocks.OAK_SLAB),
					"A slab ridge must run front-to-back");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void obstructedLegacyRoofStaysValidAndPacksWithoutRemovingObstruction(GameTestHelper helper) {
		var level = helper.getLevel();
		var exterior = new CabinExterior(level.dimension(), helper.absolutePos(new BlockPos(5, 12, 5)), Direction.NORTH);
		var palette = CabinPalette.DEFAULT;
		ExteriorCabin.legacyBlocks(exterior, palette).forEach(level::setBlockAndUpdate);
		var obstruction = ExteriorCabin.local(exterior, 0, 2, ExteriorCabin.RIDGE_Y);
		level.setBlockAndUpdate(obstruction, Blocks.DIAMOND_BLOCK.defaultBlockState());
		helper.assertTrue(!ExteriorCabin.upgradeLegacyExterior(level, exterior, palette)
			&& ExteriorCabin.projectionValid(level, exterior, palette),
			"A blocked taller roof must retain a valid legacy cabin without altering its walls");
		ExteriorCabin.removeProjection(level, exterior, palette);
		helper.assertTrue(level.getBlockState(obstruction).is(Blocks.DIAMOND_BLOCK),
			"Packing must preserve unrelated blocks above a legacy cabin");
		helper.assertTrue(ExteriorCabin.legacyBlocks(exterior, palette).keySet().stream().allMatch(level::isEmptyBlock),
			"Packing must remove all of the old shell, including its flat roof");
		helper.succeed();
	}

	@GameTest
	public void packingExteriorDoesNotDropItems(GameTestHelper helper) {
		var level = helper.getLevel();
		var bounds = new ArrayList<net.minecraft.world.phys.AABB>();
		int index = 0;
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (boolean legacy : new boolean[] {false, true}) {
				var exterior = new CabinExterior(level.dimension(),
					helper.absolutePos(new BlockPos(5 + index++ * 16, 30, 5)), facing);
				var projection = legacy ? ExteriorCabin.legacyBlocks(exterior, CabinPalette.DEFAULT)
					: ExteriorCabin.blocks(exterior);
				var area = new net.minecraft.world.phys.AABB(exterior.anchor()).inflate(10.0);
				bounds.add(area);
				helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area).isEmpty(),
					"The packing test area must start without dropped items");
				projection.forEach(level::setBlockAndUpdate);
				helper.assertTrue(ExteriorCabin.projectionValid(level, exterior),
					"The cabin must have a complete exterior before packing");
				ExteriorCabin.removeProjection(level, exterior);
				helper.assertTrue(projection.keySet().stream().allMatch(level::isEmptyBlock),
					"Packing must remove the complete exterior");
				helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area).isEmpty(),
					"Packing must not drop exterior items for " + facing + " (legacy=" + legacy + ")");
			}
		}
		helper.runAfterDelay(5, () -> {
			for (var area : bounds) {
				helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area).isEmpty(),
					"Packing must not leave delayed item drops on the ground");
			}
			helper.succeed();
		});
	}

	@GameTest
	public void newRoofNeedsClearanceAndPacksCompletely(GameTestHelper helper) {
		var level = helper.getLevel();
		var exterior = new CabinExterior(level.dimension(), helper.absolutePos(new BlockPos(5, 12, 5)), Direction.NORTH);
		for (int x = -2; x <= 2; x++) {
			for (int z = 0; z <= 4; z++) {
				level.setBlockAndUpdate(ExteriorCabin.local(exterior, x, z, -1), Blocks.STONE.defaultBlockState());
			}
		}
		level.setBlockAndUpdate(ExteriorCabin.frontStep(exterior).below(), Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(ExteriorCabin.outsideDestination(exterior).below(), Blocks.STONE.defaultBlockState());
		helper.assertTrue(ExteriorCabin.validate(level, exterior).valid(), "The unchanged footprint must accept clear ground");
		var ridge = ExteriorCabin.local(exterior, 0, 2, ExteriorCabin.RIDGE_Y);
		level.setBlockAndUpdate(ridge, Blocks.STONE.defaultBlockState());
		helper.assertTrue(!ExteriorCabin.validate(level, exterior).valid(), "The taller ridge must reject overhead obstruction");
		level.setBlockAndUpdate(ridge, Blocks.AIR.defaultBlockState());
		ExteriorCabin.place(level, exterior);
		helper.assertTrue(ExteriorCabin.projectionValid(level, exterior), "The placed gable roof must be a valid projection");
		ExteriorCabin.removeProjection(level, exterior);
		helper.assertTrue(ExteriorCabin.blocks(exterior).keySet().stream().allMatch(level::isEmptyBlock),
			"Packing must remove the complete stair and slab roof");
		helper.succeed();
	}

	@GameTest
	public void paletteDrivesExteriorBlocks(GameTestHelper helper) {
		CabinPalette palette = new CabinPalette(
			CabinMaterialProfiles.matchPlanks(new ItemStack(Items.CHERRY_PLANKS)).orElseThrow(),
			CabinMaterialProfiles.matchStructuralWood(new ItemStack(Items.SPRUCE_LOG)).orElseThrow(),
			CabinMaterialProfiles.matchPlanks(new ItemStack(Items.BAMBOO_PLANKS)).orElseThrow(),
			CabinMaterialProfiles.matchDoor(new ItemStack(Items.OAK_DOOR)).orElseThrow()
		);
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(10, 70, 10), Direction.SOUTH);
		var blocks = ExteriorCabin.blocks(exterior, palette);
		helper.assertTrue(blocks.get(exterior.anchor()).is(Blocks.CHERRY_PLANKS),
			"Exterior walking surfaces must use the selected floor planks");
		helper.assertTrue(blocks.get(ExteriorCabin.local(exterior, -2, 0, 1)).is(Blocks.SPRUCE_LOG),
			"Exterior framing must use the selected structural wood");
		helper.assertTrue(blocks.get(ExteriorCabin.local(exterior, 0, 2, ExteriorCabin.RIDGE_Y))
			.is(Blocks.BAMBOO_SLAB), "Exterior roof must use the selected roof family");
		helper.assertTrue(blocks.get(ExteriorCabin.doorLower(exterior)).is(Blocks.OAK_DOOR),
			"Exterior door must use the exact selected door variant");
		helper.succeed();
	}

	@GameTest
	public void structuralCornerFramesWrapBothJoiningWalls(GameTestHelper helper) {
		CabinPalette palette = CabinPalette.DEFAULT;
		var frame = palette.walls().structuralWoodBlock();
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			CabinExterior exterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, facing);
			var exteriorBlocks = ExteriorCabin.blocks(exterior, palette);
			for (int lateral : new int[] {-ExteriorCabin.CORE_RADIUS, ExteriorCabin.CORE_RADIUS}) {
				for (int depth : new int[] {0, ExteriorCabin.CORE_DEPTH}) {
					int inwardLateral = lateral < 0 ? lateral + 1 : lateral - 1;
					int inwardDepth = depth == 0 ? depth + 1 : depth - 1;
					for (BlockPos position : List.of(
						ExteriorCabin.local(exterior, lateral, depth, 1),
						ExteriorCabin.local(exterior, inwardLateral, depth, 1),
						ExteriorCabin.local(exterior, lateral, inwardDepth, 1)
					)) {
						if (!position.equals(ExteriorCabin.controller(exterior))) {
							helper.assertTrue(exteriorBlocks.get(position).is(frame),
								"Exterior corners and adjacent wall courses must use structural wood");
						}
					}
				}
			}
		}

		long cell = 63;
		var bounds = PocketDimension.bounds(CabinProgression.INITIAL_GENERAL_SIZE);
		var interiorBlocks = PocketDimension.shellBlocks(
			cell, CabinProgression.INITIAL_GENERAL_SIZE, palette
		);
		BlockPos center = PocketDimension.cellCenter(cell);
		for (int x : new int[] {bounds.shellMinimumX(), bounds.shellMaximumX()}) {
			for (int z : new int[] {bounds.shellMinimumZ(), bounds.shellMaximumZ()}) {
				int inwardX = x == bounds.shellMinimumX() ? x + 1 : x - 1;
				int inwardZ = z == bounds.shellMinimumZ() ? z + 1 : z - 1;
				for (BlockPos position : List.of(
					center.offset(x, 1, z), center.offset(inwardX, 1, z), center.offset(x, 1, inwardZ)
				)) {
					if (!position.equals(PocketDimension.interiorController(cell))) {
						helper.assertTrue(interiorBlocks.get(position).is(frame),
							"Each interior corner must wrap one Structural Wood column onto both walls");
					}
				}
			}
		}

		long expandedCell = 64;
		var expandedBounds = PocketDimension.bounds(7);
		var expanded = PocketDimension.shellBlocks(expandedCell, 7, palette);
		BlockPos expandedCenter = PocketDimension.cellCenter(expandedCell);
		for (int x = expandedBounds.shellMinimumX(); x <= expandedBounds.shellMaximumX(); x++) {
			boolean shouldFrame = x == 0 || x - expandedBounds.shellMinimumX() < 2
				|| expandedBounds.shellMaximumX() - x < 2;
			helper.assertTrue(expanded.get(expandedCenter.offset(x, 1, expandedBounds.shellMinimumZ()))
				.is(shouldFrame ? frame : palette.walls().planksBlock()),
				"Expanded front and rear frames must use their own wall axis");
		}
		helper.succeed();
	}

	@GameTest
	public void recognizableLegacyCornerFramesMigrateWithoutRebuilding(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CabinPalette palette = CabinPalette.DEFAULT;
		CabinExterior exterior = new CabinExterior(
			level.dimension(), helper.absolutePos(new BlockPos(5, 12, 5)), Direction.EAST
		);
		for (var entry : ExteriorCabin.legacyBlocks(exterior, palette).entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		BlockPos exteriorNewFrame = ExteriorCabin.local(exterior, -1, 0, 1);
		helper.assertTrue(ExteriorCabin.projectionValid(level, exterior, palette),
			"A complete legacy exterior must remain valid during migration");
		helper.assertTrue(ExteriorCabin.upgradeLegacyExterior(level, exterior, palette)
			&& level.getBlockState(exteriorNewFrame).is(palette.walls().structuralWoodBlock()),
			"Recognizable exterior wall planks must be upgraded to the new frame");
		helper.assertTrue(!ExteriorCabin.upgradeLegacyExterior(level, exterior, palette),
			"Repeating an exterior frame migration must not change an already-current structure");

		CabinExterior partiallyMigrated = new CabinExterior(
			level.dimension(), helper.absolutePos(new BlockPos(20, 12, 5)), Direction.SOUTH
		);
		for (var entry : ExteriorCabin.legacyBlocks(partiallyMigrated, palette).entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		BlockPos alreadyMigrated = ExteriorCabin.local(partiallyMigrated, -1, 0, 1);
		BlockPos remainingLegacyFrame = ExteriorCabin.local(
			partiallyMigrated, 1, ExteriorCabin.CORE_DEPTH, 1
		);
		level.setBlockAndUpdate(alreadyMigrated,
			ExteriorCabin.blocks(partiallyMigrated, palette).get(alreadyMigrated));
		helper.assertTrue(ExteriorCabin.projectionValid(level, partiallyMigrated, palette)
			&& ExteriorCabin.upgradeLegacyExterior(level, partiallyMigrated, palette)
			&& level.getBlockState(remainingLegacyFrame).is(palette.walls().structuralWoodBlock()),
			"A partially migrated exterior must remain valid and finish migrating");

		long interiorCell = 65;
		CabinExterior recordExterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, Direction.NORTH);
		CabinRecord cabin = new CabinRecord(
			UUID.randomUUID(), UUID.randomUUID(), interiorCell, CabinLifecycle.DEPLOYED,
			Optional.of(recordExterior), true
		);
		for (var entry : PocketDimension.legacyShellBlocks(interiorCell, 3, palette).entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		var bounds = PocketDimension.bounds(3);
		BlockPos center = PocketDimension.cellCenter(interiorCell);
		BlockPos interiorNewFrame = center.offset(
			bounds.shellMinimumX() + 1, 1, bounds.shellMinimumZ()
		);
		helper.assertTrue(PocketDimension.upgradeLegacyCornerFrames(level, cabin)
			&& level.getBlockState(interiorNewFrame).is(palette.walls().structuralWoodBlock()),
			"Recognizable interior wall planks must be upgraded to the new frame");
		helper.assertTrue(!PocketDimension.upgradeLegacyCornerFrames(level, cabin),
			"Repeating an interior frame migration must not change an already-current structure");

		long damagedCell = 66;
		CabinRecord damaged = new CabinRecord(
			UUID.randomUUID(), UUID.randomUUID(), damagedCell, CabinLifecycle.DEPLOYED,
			Optional.of(recordExterior), true
		);
		for (var entry : PocketDimension.legacyShellBlocks(damagedCell, 3, palette).entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		BlockPos damagedCenter = PocketDimension.cellCenter(damagedCell);
		BlockPos unrelatedDamage = damagedCenter.offset(0, 1, bounds.shellMinimumZ());
		BlockPos untouchedCandidate = damagedCenter.offset(
			bounds.shellMinimumX() + 1, 1, bounds.shellMinimumZ()
		);
		level.setBlockAndUpdate(unrelatedDamage, Blocks.AIR.defaultBlockState());
		helper.assertTrue(!PocketDimension.upgradeLegacyCornerFrames(level, damaged)
			&& level.getBlockState(untouchedCandidate).is(palette.walls().planksBlock()),
			"An unrecognizable damaged interior must not be partly rebuilt by frame migration");
		helper.succeed();
	}

	@GameTest
	public void clickedSurfaceControlsStairAndDoorOrientation(GameTestHelper helper) {
		BlockPos support = new BlockPos(40, 70, -20);
		for (Direction playerFacing : Direction.Plane.HORIZONTAL) {
			CabinExterior exterior = ExteriorCabin.exteriorFor(Level.OVERWORLD, support, playerFacing);
			helper.assertTrue(ExteriorCabin.frontStep(exterior).equals(support.above()),
				"The cabin-owned front stair must be directly above the clicked support block");
			helper.assertTrue(exterior.facing() == playerFacing.getOpposite(),
				"The cabin door must face outward toward the player at preview time");
		}
		helper.succeed();
	}

	@GameTest
	public void placementRejectsLavaBelowOwnedSurface(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CabinExterior exterior = new CabinExterior(
			level.dimension(), helper.absolutePos(new BlockPos(8, 3, 8)), Direction.NORTH
		);
		for (int lateral = -ExteriorCabin.CORE_RADIUS; lateral <= ExteriorCabin.CORE_RADIUS; lateral++) {
			for (int depth = 0; depth <= ExteriorCabin.CORE_DEPTH; depth++) {
				level.setBlockAndUpdate(ExteriorCabin.local(exterior, lateral, depth, 0).below(),
					Blocks.STONE.defaultBlockState());
			}
		}
		level.setBlockAndUpdate(ExteriorCabin.frontStep(exterior).below(), Blocks.LAVA.defaultBlockState());
		ExteriorCabin.PlacementCheck check = ExteriorCabin.validate(level, exterior);
		helper.assertTrue(!check.valid() && check.message().contains("lava"),
			"A cabin must never deploy with an owned floor or stair supported by lava");
		helper.succeed();
	}

	@GameTest
	public void mvpSupportsVanillaExteriorDimensionsAndWindowProfiles(GameTestHelper helper) {
		var unsupported = net.minecraft.resources.ResourceKey.create(
			Registries.DIMENSION, PortablePocketCabin.id("unsupported")
		);
		helper.assertTrue(ExteriorCabin.isSupportedDimension(Level.OVERWORLD)
			&& ExteriorCabin.isSupportedDimension(Level.NETHER)
			&& ExteriorCabin.isSupportedDimension(Level.END)
			&& !ExteriorCabin.isSupportedDimension(unsupported),
			"Placement must allow exactly the three vanilla exterior dimensions");

		helper.assertTrue(CabinWindows.profile(Level.OVERWORLD, 0, false, false, CabinLifecycle.DEPLOYED)
			== CabinWindows.Profile.DAWN, "Dawn must have a distinct window profile");
		helper.assertTrue(CabinWindows.profile(Level.OVERWORLD, 6_000, false, false, CabinLifecycle.DEPLOYED)
			== CabinWindows.Profile.DAY, "Day must have a distinct window profile");
		helper.assertTrue(CabinWindows.profile(Level.OVERWORLD, 13_000, false, false, CabinLifecycle.DEPLOYED)
			== CabinWindows.Profile.SUNSET, "Sunset must have a distinct window profile");
		helper.assertTrue(CabinWindows.profile(Level.OVERWORLD, 18_000, false, false, CabinLifecycle.DEPLOYED)
			== CabinWindows.Profile.NIGHT, "Night must have a distinct window profile");
		helper.assertTrue(CabinWindows.profile(Level.OVERWORLD, 6_000, true, false, CabinLifecycle.DEPLOYED)
			== CabinWindows.Profile.RAIN, "Rain must override the clear-sky time profile");
		helper.assertTrue(CabinWindows.profile(Level.OVERWORLD, 6_000, true, true, CabinLifecycle.DEPLOYED)
			== CabinWindows.Profile.THUNDER, "Thunder must override the rain profile");
		helper.assertTrue(CabinWindows.profile(Level.NETHER, 6_000, true, true, CabinLifecycle.DEPLOYED)
			== CabinWindows.Profile.NETHER, "The Nether must use its static ambience");
		helper.assertTrue(CabinWindows.profile(Level.END, 6_000, true, true, CabinLifecycle.DEPLOYED)
			== CabinWindows.Profile.END, "The End must use its static ambience");
		helper.assertTrue(CabinWindows.profile(Level.OVERWORLD, 6_000, false, false, CabinLifecycle.PACKED)
			== CabinWindows.Profile.INACTIVE, "Packed cabins must close their fake windows");

		var blocks = CabinWindows.blocks(7, 5, CabinWindows.Profile.DAY);
		BlockPos cabinCenter = PocketDimension.cellCenter(7);
		helper.assertTrue(blocks.size() == 4,
			"Purchased tier-one side windows must project four panes");
		for (BlockPos position : blocks.keySet()) {
			helper.assertTrue(PocketDimension.isInteriorShell(7, 5, position),
				"Every fake-window block must remain part of the protected interior shell");
			helper.assertTrue(position.getY() - cabinCenter.getY() >= 1
				&& position.getY() - cabinCenter.getY()
					<= PocketDimension.clearInteriorHeight(5),
				"Starting-cabin windows must stay below the lowered ceiling");
		}
		for (int size = CabinProgression.INITIAL_GENERAL_SIZE;
			 size <= CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE; size += 2) {
			int checkedSize = size;
			var structuralFrame = PocketDimension.shellBlocks(7, checkedSize, CabinPalette.DEFAULT)
				.entrySet().stream()
				.filter(entry -> entry.getValue().is(
					CabinPalette.DEFAULT.walls().structuralWoodBlock()
				))
				.map(java.util.Map.Entry::getKey)
				.collect(java.util.stream.Collectors.toSet());
			helper.assertTrue(java.util.Collections.disjoint(
				structuralFrame,
				CabinWindows.blocks(7, checkedSize, CabinWindows.Profile.DAY).keySet()
			), "Automatic windows must stay between the inner edges of the corner frames");
		}
		helper.succeed();
	}

	@GameTest
	public void reconciliationCoversEveryLifecycleTransition(GameTestHelper helper) {
		UUID cabinId = UUID.randomUUID();
		UUID owner = UUID.randomUUID();
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, Direction.NORTH);
		CabinRecord packed = new CabinRecord(cabinId, owner, 0, CabinLifecycle.PACKED);
		CabinRecord packedCleanupPending = new CabinRecord(
			cabinId, owner, 0, CabinLifecycle.PACKED,
			Optional.empty(), Optional.of(exterior), true, 2, true
		);
		CabinRecord deployingReady = record(cabinId, owner, CabinLifecycle.DEPLOYING, exterior, true);
		CabinRecord deployingIncomplete = record(cabinId, owner, CabinLifecycle.DEPLOYING, exterior, false);
		CabinRecord deployed = record(cabinId, owner, CabinLifecycle.DEPLOYED, exterior, true);
		CabinRecord packing = record(cabinId, owner, CabinLifecycle.PACKING, exterior, true);
		CabinRecord orphaned = new CabinRecord(
			cabinId, owner, 0, CabinLifecycle.ORPHANED,
			Optional.empty(), Optional.of(exterior), true, 2
		);

		helper.assertTrue(CabinReconciliation.plan(packed, false) == CabinReconciliation.Action.NONE
			&& CabinReconciliation.plan(packedCleanupPending, false) == CabinReconciliation.Action.ENSURE_PACKED,
			"PACKED reconciliation must remove stale physical projections");
		helper.assertTrue(CabinReconciliation.plan(deployingReady, true)
			== CabinReconciliation.Action.FINISH_DEPLOYMENT,
			"A complete interrupted deployment must commit idempotently");
		helper.assertTrue(CabinReconciliation.plan(deployingIncomplete, false)
			== CabinReconciliation.Action.ROLL_BACK_DEPLOYMENT,
			"An incomplete interrupted deployment must roll back");
		helper.assertTrue(CabinReconciliation.plan(deployed, true) == CabinReconciliation.Action.NONE
			&& CabinReconciliation.plan(deployed, false) == CabinReconciliation.Action.ORPHAN,
			"DEPLOYED reconciliation must distinguish a valid exterior from a missing one");
		helper.assertTrue(CabinReconciliation.plan(packing, true) == CabinReconciliation.Action.ABORT_PACKING
			&& CabinReconciliation.plan(packing, false) == CabinReconciliation.Action.ORPHAN,
			"Interrupted packing must restore a valid exterior or orphan a missing one");
		helper.assertTrue(CabinReconciliation.plan(orphaned, false) == CabinReconciliation.Action.NONE,
			"ORPHANED reconciliation must remain stable");
		helper.succeed();
	}

	@GameTest
	public void safeDestinationRejectsCollisionAndHazards(GameTestHelper helper) {
		BlockPos relativeFeet = new BlockPos(1, 2, 1);
		BlockPos absoluteFeet = helper.absolutePos(relativeFeet);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.setBlock(relativeFeet.below(), Blocks.STONE);
		helper.setBlock(relativeFeet, Blocks.AIR);
		helper.setBlock(relativeFeet.above(), Blocks.AIR);

		helper.assertTrue(SafeDestinationResolver.isSafe(helper.getLevel(), absoluteFeet, player),
			"A clear two-block space over solid ground must be safe");
		helper.assertTrue(SafeDestinationResolver.resolveExact(
			helper.getLevel(), absoluteFeet, player
		).isPresent(), "The bounded resolver must accept a safe destination in an already-loaded chunk");
		helper.setBlock(relativeFeet, Blocks.FIRE);
		helper.assertTrue(!SafeDestinationResolver.isSafe(helper.getLevel(), absoluteFeet, player),
			"Fire must invalidate a destination");
		helper.setBlock(relativeFeet, Blocks.AIR);
		helper.setBlock(relativeFeet.above(), Blocks.STONE);
		helper.assertTrue(!SafeDestinationResolver.isSafe(helper.getLevel(), absoluteFeet, player),
			"A colliding head block must invalidate a destination");
		BlockPos farAway = new BlockPos(1_600_000, 64, 1_600_000);
		ChunkPos farChunk = ChunkPos.containing(farAway);
		helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(farChunk.x(), farChunk.z()) == null,
			"The deadline test requires an initially unloaded destination chunk");

		var destination = SafeDestinationResolver.searchUntil(
			helper.getLevel(), farAway, player, 0, System.nanoTime() - 1L
		);
		helper.assertTrue(destination.isEmpty(),
			"An expired destination search must stop without synchronously loading another chunk");
		helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(farChunk.x(), farChunk.z()) == null,
			"An expired destination search must leave the destination chunk unloaded");
		helper.succeed();
	}

	@GameTest
	public void pocketCoordinatesResolveToPermanentCell(GameTestHelper helper) {
		long cell = 1_237;
		BlockPos center = PocketDimension.cellCenter(cell);
		helper.assertTrue(PocketDimension.cellIndexAt(center.offset(200, 30, -200)).orElseThrow() == cell,
			"Any position within a cell's isolation region must resolve to its permanent cell");
		helper.assertTrue(PocketDimension.cellIndexAt(new BlockPos(-10_000, 64, -10_000)).isEmpty(),
			"Coordinates outside the allocated cell grid must not resolve to a cabin");
		helper.succeed();
	}

	@GameTest
	public void trustedEntryIsPersistedAndOwnerControlled(GameTestHelper helper) {
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID trustedPlayer = UUID.randomUUID();
		UUID stranger = UUID.randomUUID();
		CabinRecord cabin = registry.create(owner);

		helper.assertTrue(cabin.canEnter(owner), "A cabin owner must always retain entry permission");
		helper.assertTrue(!cabin.canEnter(trustedPlayer),
			"A private cabin must reject players even when they will later be trusted");
		registry.trust(cabin.uuid(), owner, trustedPlayer);
		CabinRecord opened = registry.setEntryPermission(
			cabin.uuid(), owner, CabinEntryPermission.TRUSTED_PLAYERS
		);
		helper.assertTrue(opened.canEnter(owner) && opened.canEnter(trustedPlayer),
			"Trusted entry mode must admit the owner and explicitly trusted players");
		helper.assertTrue(!opened.canEnter(stranger),
			"Trusted entry mode must continue to reject strangers");

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinRecord restoredCabin = restored.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(restoredCabin.entryPermission() == CabinEntryPermission.TRUSTED_PLAYERS
			&& restoredCabin.trustedPlayers().equals(java.util.List.of(trustedPlayer)),
			"Entry mode and trusted players must survive save/reload");

		try {
			restored.untrust(cabin.uuid(), stranger, trustedPlayer);
			helper.fail("A non-owner must not be able to change trusted players");
		} catch (IllegalStateException expected) {
			helper.succeed();
		}
	}

	@GameTest
	public void lifecycleTransitionsPreserveAccessAndRejectRaces(GameTestHelper helper) {
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID trustedPlayer = UUID.randomUUID();
		CabinRecord cabin = registry.create(owner);
		registry.trust(cabin.uuid(), owner, trustedPlayer);
		registry.setEntryPermission(cabin.uuid(), owner, CabinEntryPermission.TRUSTED_PLAYERS);
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(12, 72, 12), Direction.NORTH);

		registry.beginDeployment(cabin.uuid(), owner, exterior);
		try {
			registry.beginDeployment(cabin.uuid(), owner, exterior);
			helper.fail("Only one simultaneous deployment attempt may journal a transition");
		} catch (IllegalStateException expected) {
			// The first transition owns the deployment journal.
		}
		registry.markInteriorGenerated(cabin.uuid());
		CabinRecord deployed = registry.finishDeployment(cabin.uuid());
		helper.assertTrue(deployed.canEnter(trustedPlayer),
			"Deployment must preserve trusted-player access");

		registry.beginPacking(cabin.uuid(), owner);
		try {
			registry.beginPacking(cabin.uuid(), owner);
			helper.fail("Only one simultaneous packing attempt may journal a transition");
		} catch (IllegalStateException expected) {
			// The first transition owns the packing journal.
		}
		CabinRecord packed = registry.finishPacking(cabin.uuid());
		helper.assertTrue(packed.canEnter(trustedPlayer)
			&& packed.entryPermission() == CabinEntryPermission.TRUSTED_PLAYERS,
			"Packing must preserve access settings without making the inactive entrance usable");
		helper.succeed();
	}

	@GameTest
	public void offlineOccupantsRequireRecoveryWhenCabinIsInactive(GameTestHelper helper) {
		UUID cabinId = UUID.randomUUID();
		UUID owner = UUID.randomUUID();
		long cell = 42;
		BlockPos inside = PocketDimension.cellCenter(cell).above();
		CabinRecord packed = new CabinRecord(cabinId, owner, cell, CabinLifecycle.PACKED);
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, Direction.SOUTH);
		CabinRecord deployed = new CabinRecord(
			cabinId, owner, cell, CabinLifecycle.DEPLOYED,
			Optional.of(exterior), Optional.of(exterior), true, 0
		);
		CabinRecord redeployed = new CabinRecord(
			cabinId, owner, cell, CabinLifecycle.DEPLOYED,
			Optional.of(exterior), Optional.of(exterior), true, 1
		);
		CabinOccupancyData.Stay originalStay = new CabinOccupancyData.Stay(owner, cabinId, 0);

		helper.assertTrue(CabinEvents.requiresLoginEvacuation(inside, packed, originalStay),
			"A player logging into a packed cabin cell must be evacuated");
		helper.assertTrue(!CabinEvents.requiresLoginEvacuation(inside, deployed, originalStay),
			"A player may remain in a currently deployed cabin");
		helper.assertTrue(CabinEvents.requiresLoginEvacuation(inside, deployed, null),
			"A legacy or untracked occupant must be recovered conservatively");
		helper.assertTrue(CabinEvents.requiresLoginEvacuation(inside, redeployed, originalStay),
			"A player who logged out before a pack and redeploy must be evacuated to the new exterior");
		helper.assertTrue(!CabinEvents.requiresLoginEvacuation(
			PocketDimension.cellCenter(cell + 1), packed, originalStay
		),
			"Recovery must not confuse adjacent permanent cabin cells");
		helper.succeed();
	}

	@GameTest
	public void offlineOccupancyGenerationSurvivesSaveReload(GameTestHelper helper) {
		UUID playerId = UUID.randomUUID();
		CabinRecord cabin = new CabinRecord(UUID.randomUUID(), UUID.randomUUID(), 9, CabinLifecycle.PACKED);
		CabinOccupancyData original = new CabinOccupancyData();
		original.enter(playerId, cabin);

		var encoded = CabinOccupancyData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
		CabinOccupancyData restored = CabinOccupancyData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinOccupancyData.Stay stay = restored.find(playerId).orElseThrow();

		helper.assertTrue(stay.cabinId().equals(cabin.uuid())
			&& stay.packedItemGeneration() == cabin.packedItemGeneration(),
			"Offline cabin-entry generation must survive save/reload");
		restored.clear(playerId);
		helper.assertTrue(restored.find(playerId).isEmpty(),
			"Evacuating an offline occupant must clear its persisted cabin stay");
		helper.succeed();
	}

	@GameTest
	public void simulationTicketsCoverOnlyTheBoundedInterior(GameTestHelper helper) {
		long cell = 19;
		BlockPos center = PocketDimension.cellCenter(cell);
		var chunks = CabinSimulation.chunksForCell(cell, CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE);
		var bounds = PocketDimension.bounds(CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE);
		var expected = new HashSet<ChunkPos>();
		int minimumChunkX = Math.floorDiv(center.getX() + bounds.shellMinimumX(), 16);
		int maximumChunkX = Math.floorDiv(center.getX() + bounds.shellMaximumX(), 16);
		int minimumChunkZ = Math.floorDiv(center.getZ() + bounds.shellMinimumZ(), 16);
		int maximumChunkZ = Math.floorDiv(center.getZ() + bounds.shellMaximumZ(), 16);
		for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
			for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
				expected.add(new ChunkPos(chunkX, chunkZ));
			}
		}
		helper.assertTrue(new HashSet<>(chunks).equals(expected),
			"Simulation tickets must cover the declared finite expansion envelope");

		UUID owner = UUID.randomUUID();
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, Direction.NORTH);
		CabinRecord deployed = new CabinRecord(
			UUID.randomUUID(), owner, cell, CabinLifecycle.DEPLOYED,
			Optional.of(exterior), Optional.of(exterior), true, 0
		);
		CabinRecord packed = new CabinRecord(deployed.uuid(), owner, cell, CabinLifecycle.PACKED);
		helper.assertTrue(CabinSimulation.shouldSimulate(deployed),
			"A generated deployed interior must remain simulated");
		helper.assertTrue(!CabinSimulation.shouldSimulate(packed),
			"A packed interior must not retain a simulation ticket");
		helper.succeed();
	}

	@GameTest(maxTicks = 260)
	public void vanillaInteriorFixturesTickNormally(GameTestHelper helper) {
		BlockPos crop = new BlockPos(1, 2, 1);
		BlockPos farmland = crop.below();
		BlockPos furnacePos = new BlockPos(3, 2, 1);
		BlockPos chestPos = new BlockPos(1, 2, 3);
		BlockPos waterPos = new BlockPos(3, 2, 3);

		helper.setBlock(farmland, Blocks.FARMLAND.defaultBlockState()
			.setValue(BlockStateProperties.MOISTURE, 7));
		helper.setBlock(crop, Blocks.WHEAT.defaultBlockState());
		helper.setBlock(crop.offset(0, 0, 1), Blocks.SEA_LANTERN);
		helper.setBlock(furnacePos, Blocks.FURNACE);
		helper.setBlock(chestPos, Blocks.CHEST);
		helper.setBlock(waterPos.below(), Blocks.STONE);
		helper.setBlock(waterPos, Blocks.WATER);

		FurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos, FurnaceBlockEntity.class);
		furnace.setItem(0, new ItemStack(Items.RAW_IRON));
		furnace.setItem(1, new ItemStack(Items.COAL));
		ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
		chest.setItem(0, new ItemStack(Items.DIAMOND, 3));

		helper.onEachTick(() -> {
			if (helper.getBlockState(crop).is(Blocks.WHEAT)) {
				helper.randomTick(crop);
			}
		});
		helper.succeedWhen(() -> {
			int age = helper.getBlockState(crop).getValue(CropBlock.AGE);
			helper.assertTrue(age > 0, "A lit, hydrated crop must receive random ticks");
			helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT),
				"A vanilla furnace must complete recipes in a simulated interior");
			helper.assertTrue(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 3,
				"Vanilla storage contents must remain intact while the interior simulates");
			helper.assertTrue(helper.getBlockState(waterPos).is(Blocks.WATER),
				"A vanilla water source must remain usable in the interior");
		});
	}

	private static CabinRecord record(
		UUID cabinId, UUID owner, CabinLifecycle lifecycle, CabinExterior exterior, boolean generated
	) {
		return new CabinRecord(
			cabinId, owner, 0, lifecycle,
			Optional.of(exterior), Optional.of(exterior), generated, 2
		);
	}

	private static CabinWindowState stateWithWindows(CabinWindowState.Window... windows) {
		return stateWithWindows(List.of(windows));
	}

	private static CabinWindowState stateWithWindows(List<CabinWindowState.Window> windows) {
		return new CabinWindowState(windows);
	}

	private static CabinWindowState.Window windowAtTier(CabinWindowState.Identity identity, int tier) {
		List<CabinWindowState.Receipt> receipts = new ArrayList<>();
		for (int current = 1; current <= tier; current++) {
			receipts.add(new CabinWindowState.Receipt(
				current, List.of(new ItemStack(Items.GLASS_PANE, 1))
			));
		}
		return new CabinWindowState.Window(identity, tier, receipts);
	}

	private static CabinRecord cabinWithWindows(long cellIndex, CabinWindowState windows) {
		return cabinWithWindows(cellIndex, CabinProgression.INITIAL_GENERAL_SIZE, windows);
	}

	private static CabinRecord cabinWithWindows(long cellIndex, int generalSize, CabinWindowState windows) {
		return new CabinRecord(
			UUID.randomUUID(), UUID.randomUUID(), cellIndex, CabinLifecycle.PACKED,
			Optional.empty(), Optional.empty(), true, 0L, false, CabinPalette.DEFAULT,
			Optional.empty(), false, CabinEntryPermission.OWNER_ONLY, List.of(),
			new CabinProgression(generalSize, List.of()),
			CabinUpgradeState.EMPTY.withWindows(windows)
		);
	}

	@GameTest
	public void cabinHomeBindingSurvivesSaveReloadAndLatestBedWins(GameTestHelper helper) {
		UUID owner = UUID.randomUUID();
		UUID cabinId = UUID.randomUUID();
		BlockPos firstBed = PocketDimension.cellCenter(3).offset(2, 1, 2);
		BlockPos latestBed = firstBed.offset(4, 0, -1);
		CabinRespawnData original = new CabinRespawnData();
		original.bind(owner, cabinId, firstBed);
		original.bind(owner, cabinId, latestBed);

		var encoded = CabinRespawnData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
		CabinRespawnData restored = CabinRespawnData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinHomeBinding binding = restored.find(owner).orElseThrow();

		helper.assertTrue(binding.cabinId().equals(cabinId) && binding.bedPosition().equals(latestBed),
			"The latest cabin bed binding must replace and persist over the previous bed");
		helper.succeed();
	}

	@GameTest
	public void onlyOwnerCanBindACabinBed(GameTestHelper helper) {
		UUID owner = UUID.randomUUID();
		UUID visitor = UUID.randomUUID();
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, Direction.NORTH);
		CabinRecord cabin = new CabinRecord(
			UUID.randomUUID(), owner, 2, CabinLifecycle.DEPLOYED,
			Optional.of(exterior), Optional.of(exterior), true, 0
		);
		CabinRespawnData data = new CabinRespawnData();
		BlockPos bed = PocketDimension.cellCenter(2).above();

		helper.assertTrue(!CabinRespawning.bindOwner(data, visitor, cabin, bed),
			"A trusted visitor sleeping in a cabin must not gain a cabin-home binding");
		helper.assertTrue(data.find(visitor).isEmpty(),
			"A rejected visitor binding must not replace that player's home");
		helper.assertTrue(CabinRespawning.bindOwner(data, owner, cabin, bed)
			&& data.find(owner).orElseThrow().cabinId().equals(cabin.uuid()),
			"A deployed cabin owner must be able to bind its bed");
		helper.succeed();
	}

	@GameTest
	public void nearDeathSearchIsBoundedAndNeverTargetsPocketDimension(GameTestHelper helper) {
		int minimum = 128;
		int maximum = 256;
		BlockPos origin = new BlockPos(40, 70, -90);
		var candidates = CabinRespawning.candidateOffsets(UUID.randomUUID(), minimum, maximum);

		helper.assertTrue(candidates.size() == CabinRespawning.MAX_CANDIDATE_REGIONS,
			"Near-death respawning must inspect at most sixteen candidate regions");
		for (BlockPos offset : candidates) {
			helper.assertTrue(CabinRespawning.withinHorizontalBounds(
				origin, origin.offset(offset), minimum, maximum
			), "Every candidate region must be inside the configured distance annulus");
		}
		helper.assertTrue(CabinRespawning.allowsNearDeathSearch(Level.OVERWORLD)
			&& CabinRespawning.allowsNearDeathSearch(Level.NETHER)
			&& CabinRespawning.allowsNearDeathSearch(Level.END),
			"Near-death respawning must support all three vanilla dimensions");
		helper.assertTrue(!CabinRespawning.allowsNearDeathSearch(PocketDimension.LEVEL_KEY),
			"Near-death respawning must never select the pocket-home dimension");
		helper.succeed();
	}

	@GameTest
	public void packingDuringDeathScreenInvalidatesInteriorRespawn(GameTestHelper helper) {
		UUID owner = UUID.randomUUID();
		CabinRegistry registry = new CabinRegistry();
		CabinRecord cabin = registry.create(owner);
		CabinExterior exterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, Direction.SOUTH);
		registry.beginDeployment(cabin.uuid(), owner, exterior);
		registry.markInteriorGenerated(cabin.uuid());
		CabinRecord deployed = registry.finishDeployment(cabin.uuid());
		CabinHomeBinding binding = new CabinHomeBinding(
			owner, cabin.uuid(), PocketDimension.cellCenter(cabin.cellIndex()).above()
		);

		helper.assertTrue(CabinRespawning.canUseInterior(binding, deployed),
			"A deployed bound cabin may initially select its bedside destination");
		CabinRecord packing = registry.beginPacking(cabin.uuid(), owner);
		helper.assertTrue(!CabinRespawning.canUseInterior(binding, packing),
			"Starting packing while the death screen is open must invalidate a bedside respawn");
		CabinRecord packed = registry.finishPacking(cabin.uuid());
		helper.assertTrue(!CabinRespawning.canUseInterior(binding, packed),
			"A completed packing race must continue through outside fallback destinations");
		helper.assertTrue(CabinRespawning.shouldSearchNearDeath(packed),
			"A packed cabin must use the near-death search before its last campsite");
		CabinRecord orphaned = registry.markOrphaned(cabin.uuid());
		helper.assertTrue(CabinRespawning.shouldSearchNearDeath(orphaned)
			&& !CabinRespawning.canUseInterior(binding, orphaned),
			"An orphaned cabin must use outside respawn fallbacks and never its interior");
		helper.succeed();
	}

	@GameTest
	public void commandOutputMakesCabinUuidCopyable(GameTestHelper helper) {
		UUID cabinId = UUID.randomUUID();
		CabinRecord cabin = new CabinRecord(cabinId, UUID.randomUUID(), 4, CabinLifecycle.PACKED);
		Component formatted = CabinCommands.format(cabin);
		ClickEvent clickEvent = formatted.getSiblings().getFirst().getStyle().getClickEvent();

		helper.assertTrue(
			clickEvent instanceof ClickEvent.CopyToClipboard copy
				&& copy.value().equals(cabinId.toString()),
			"Clicking a formatted cabin UUID must copy it to the clipboard"
		);
		helper.succeed();
	}

	private static void assertUpgradeState(
		GameTestHelper helper, CabinUpgradeState expected, CabinUpgradeState actual
	) {
		var expectedTag = CabinUpgradeState.CODEC.encodeStart(NbtOps.INSTANCE, expected).getOrThrow();
		var actualTag = CabinUpgradeState.CODEC.encodeStart(NbtOps.INSTANCE, actual).getOrThrow();
		helper.assertTrue(actualTag.equals(expectedTag),
			"Cabin target funds and contributed stack data must survive every transition");
	}

	private static CabinUpgradeDefinitions.Definitions testUpgradeDefinitions(
		WorldAttunement attunement, int firstPaneCount, int secondPaneCount
	) {
		return new CabinUpgradeDefinitions.Definitions(
			attunement.definitionVersion(), 5, List.of(attunement.woodProfile()),
			List.of(new CabinUpgradeDefinitions.Expansion(5, List.of(
				new CabinUpgradeDefinitions.Ingredient(
					Items.GLASS_PANE.builtInRegistryHolder().key().identifier(), false, firstPaneCount
				),
				new CabinUpgradeDefinitions.Ingredient(
					Items.GLASS_PANE.builtInRegistryHolder().key().identifier(), false, secondPaneCount
				),
				new CabinUpgradeDefinitions.Ingredient(
					Items.AMETHYST_SHARD.builtInRegistryHolder().key().identifier(), false, 4
				)
			)))
		);
	}

	private static CabinUpgradeCatalog.Group syntheticGroup(
		String id, String title, net.minecraft.world.item.Item icon,
		CabinUpgradeCatalog.Offer... offers
	) {
		return new CabinUpgradeCatalog.Group(
			PortablePocketCabin.id(id), title,
			icon.builtInRegistryHolder().key().identifier(), List.of(offers)
		);
	}

	private static CabinUpgradeCatalog.Offer syntheticOffer(int targetSize, int requirementCount) {
		List<net.minecraft.resources.Identifier> items = List.of(
			net.minecraft.resources.Identifier.parse("minecraft:glass_pane"),
			net.minecraft.resources.Identifier.parse("minecraft:amethyst_shard"),
			net.minecraft.resources.Identifier.parse("minecraft:yellow_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:orange_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:white_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:light_blue_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:magenta_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:blue_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:light_gray_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:cyan_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:gray_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:red_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:purple_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:lime_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:pink_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:brown_dye"),
			net.minecraft.resources.Identifier.parse("minecraft:black_dye")
		);
		List<CabinUpgradeState.Requirement> requirements = items.subList(0, requirementCount).stream()
			.map(item -> new CabinUpgradeState.Requirement(item, 1))
			.toList();
		return new CabinUpgradeCatalog.Offer(
			CabinUpgradeState.Target.generalSpace(targetSize),
			"Synthetic " + targetSize,
			"Synthetic effect " + targetSize,
			Items.AMETHYST_BLOCK.builtInRegistryHolder().key().identifier(),
			targetSize - CabinProgression.GENERAL_SIZE_STEP, targetSize, requirements
		);
	}

	private static CabinRecord deployRegistryCabin(CabinRegistry registry, UUID owner, int exteriorX) {
		CabinRecord cabin = registry.create(owner);
		CabinExterior exterior = new CabinExterior(
			Level.OVERWORLD, new BlockPos(exteriorX, 100, 0), Direction.NORTH
		);
		registry.beginDeployment(cabin.uuid(), owner, exterior);
		registry.markInteriorGenerated(cabin.uuid());
		return registry.finishDeployment(cabin.uuid());
	}

	private static void fundTestUpgrade(
		CabinRegistry registry,
		UUID cabinId,
		UUID owner,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		CabinUpgradeState.Target target = CabinUpgradeState.Target.generalSpace(5);
		TestExpansionEffect clear = new TestExpansionEffect(true, false);
		ItemStack panes = new ItemStack(Items.GLASS_PANE, 12);
		ItemStack shards = new ItemStack(Items.AMETHYST_SHARD, 4);
		CabinUpgradeService.deposit(
			registry, cabinId, owner, target, panes, attunement, definitions, clear
		);
		CabinUpgradeService.deposit(
			registry, cabinId, owner, target, shards, attunement, definitions, clear
		);
	}

	private static CabinUpgradeState.Fund partialFund(CabinUpgradeCatalog.Offer offer, int count) {
		CabinUpgradeState.Requirement requirement = offer.requirements().getFirst();
		return new CabinUpgradeState.Fund(
			offer.target(), offer.requirements(), List.of(new ItemStack(
				BuiltInRegistries.ITEM.getOptional(requirement.itemId()).orElseThrow(),
				Math.min(count, requirement.count())
			))
		);
	}

	private static final class TestReversalEffect implements CabinWindowReversalService.Effect {
		private final boolean valid;
		private final boolean interrupt;
		private int applications;

		private TestReversalEffect(boolean valid, boolean interrupt) {
			this.valid = valid;
			this.interrupt = interrupt;
		}

		@Override
		public CabinUpgradeService.Outcome validate(CabinRecord cabin, CabinWindowState resultingState) {
			return valid
				? CabinUpgradeService.Outcome.success("Window wall is clear")
				: CabinUpgradeService.Outcome.failure("Window wall is obstructed");
		}

		@Override
		public void apply(CabinRecord cabin, CabinWindowState resultingState) {
			applications++;
			if (interrupt) {
				throw new IllegalStateException("Simulated reversal interruption");
			}
		}

		@Override
		public void refresh(CabinRecord cabin) {
		}
	}

	private static final class TestExpansionEffect implements CabinUpgradeService.UpgradeEffect {
		private final boolean valid;
		private final boolean interrupt;
		private int applications;

		private TestExpansionEffect(boolean valid, boolean interrupt) {
			this.valid = valid;
			this.interrupt = interrupt;
		}

		@Override
		public CabinUpgradeService.Outcome validate(CabinRecord cabin, CabinUpgradeCatalog.Offer offer) {
			return valid
				? CabinUpgradeService.Outcome.success("Expansion volume is clear.")
				: CabinUpgradeService.Outcome.failure("Expansion is obstructed.");
		}

		@Override
		public void apply(CabinRecord cabin, CabinUpgradeState.Installation installation) {
			applications++;
			if (interrupt) {
				throw new IllegalStateException("Simulated interruption");
			}
		}

		@Override
		public void refresh(CabinRecord cabin) {
		}
	}
}
