package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PortablePocketCabinGameTest {
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
	public void schemaThreeMigratesToEmptySchemaFourUpgradeState(GameTestHelper helper) {
		CabinRegistry original = new CabinRegistry();
		CabinRecord cabin = original.create(UUID.randomUUID());
		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
		encoded.asCompound().orElseThrow().putInt("schema_version", 3);

		CabinRegistry restored = CabinRegistry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		CabinRecord restoredCabin = restored.find(cabin.uuid()).orElseThrow();
		var rewritten = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, restored).getOrThrow();

		helper.assertTrue(restoredCabin.upgrades().equals(CabinUpgradeState.EMPTY),
			"Schema 3 cabins must migrate with no tracked upgrade or fund");
		helper.assertTrue(rewritten.asCompound().orElseThrow().getIntOr("schema_version", 0) == 4,
			"Migrated registries must rewrite as schema 4");
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
			Optional.of(new CabinUpgradeState.TrackedUpgrade(
				CabinUpgradeState.Target.generalSpace(5),
				List.of(
					new CabinUpgradeState.Requirement(Items.GLASS_PANE.builtInRegistryHolder().key().identifier(), 12),
					new CabinUpgradeState.Requirement(Items.AMETHYST_SHARD.builtInRegistryHolder().key().identifier(), 2)
				),
				List.of(namedGlass)
			)),
			Optional.empty()
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
		migratableData.putInt("schema_version", 3);
		migratableRoot.put("data", migratableData);
		CabinRegistry.requireSupportedSchema(migratableRoot, java.nio.file.Path.of("schema-three-cabins.dat"));

		var encoded = CabinRegistry.CODEC.encodeStart(NbtOps.INSTANCE, new CabinRegistry()).getOrThrow();
		helper.assertTrue(encoded.asCompound().orElseThrow().getIntOr("schema_version", 0) == 4,
			"Upgrade-fund registries must publish explicit schema version 4");
		helper.succeed();
	}

	@GameTest
	public void upgradeMenuHasNoWithdrawableFundSlots(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CabinUpgradeMenu menu = new CabinUpgradeMenu(41, player.getInventory(), UUID.randomUUID());

		for (int slot = CabinUpgradeMenu.DEPOSIT_SLOT;
			 slot < CabinUpgradeMenu.FIRST_PLAYER_SLOT; slot++) {
			helper.assertTrue(!menu.getSlot(slot).mayPickup(player)
				&& !menu.getSlot(slot).mayPlace(new ItemStack(Items.DIAMOND)),
				"Upgrade display and deposit controls must never expose withdrawable storage slots");
		}
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
	public void generalSpaceUpgradeLadderReachesTwentyOneByTwentyOne(GameTestHelper helper) {
		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		helper.assertTrue(definitions.maximumGeneralSize() == 21,
			"General-space progression must end at 21x21");
		for (int size = 5; size <= 21; size++) {
			helper.assertTrue(definitions.expansion(size) != null,
				"Every one-block expansion through 21x21 must have an upgrade definition");
		}
		helper.succeed();
	}

	@GameTest
	public void upgradeCatalogConsolidatesRequirementsAndTrackingIsOwnerControlled(GameTestHelper helper) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(17L);
		CabinUpgradeDefinitions.Definitions definitions = testUpgradeDefinitions(attunement, 8, 4);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID visitor = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 30);

		CabinUpgradeCatalog.Offer offer = CabinUpgradeCatalog.next(cabin, attunement, definitions).orElseThrow();
		helper.assertTrue(offer.target().equals(CabinUpgradeState.Target.generalSpace(5)),
			"The catalog must expose only the next general-space size");
		helper.assertTrue(offer.requirements().size() == 2
			&& offer.requirements().stream().filter(requirement -> requirement.itemId().equals(
				Items.GLASS_PANE.builtInRegistryHolder().key().identifier()
			)).findFirst().orElseThrow().count() == 12,
			"Duplicate resolved item requirements must be consolidated");
		helper.assertTrue(!CabinUpgradeService.track(registry, cabin.uuid(), visitor, offer.target(), attunement,
			definitions).success(), "A visitor must not track an upgrade");
		helper.assertTrue(CabinUpgradeService.track(registry, cabin.uuid(), owner, offer.target(), attunement,
			definitions).success(), "The owner must be able to track the next expansion");
		helper.assertTrue(!CabinUpgradeService.track(registry, cabin.uuid(), owner, offer.target(), attunement,
			definitions).success(), "Tracking must never replace an existing target implicitly");

		CabinUpgradeDefinitions.Definitions changed = testUpgradeDefinitions(attunement, 9, 4);
		CabinRecord tracked = registry.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(CabinUpgradeCatalog.isStale(tracked.upgrades().tracked().orElseThrow(), tracked,
			attunement, changed), "A changed resolved requirement must make the tracked upgrade stale");
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
		CabinUpgradeService.track(registry, cabin.uuid(), owner, target, attunement, definitions);

		ItemStack namedPanes = new ItemStack(Items.GLASS_PANE, 10);
		namedPanes.set(DataComponents.CUSTOM_NAME, Component.literal("Neighbour contribution"));
		CabinUpgradeService.Contribution first = CabinUpgradeService.contribute(
			registry, cabin.uuid(), trusted, namedPanes, attunement, definitions
		);
		helper.assertTrue(first.success() && first.accepted() == 10 && namedPanes.isEmpty(),
			"A trusted player must deliberately contribute an offered required stack");

		ItemStack excess = new ItemStack(Items.GLASS_PANE, 8);
		CabinUpgradeService.Contribution capped = CabinUpgradeService.contribute(
			registry, cabin.uuid(), owner, excess, attunement, definitions
		);
		helper.assertTrue(capped.success() && capped.accepted() == 2 && excess.getCount() == 6,
			"Only the outstanding quantity may enter the fund");
		ItemStack wrong = new ItemStack(Items.DIRT, 3);
		helper.assertTrue(!CabinUpgradeService.contribute(
			registry, cabin.uuid(), owner, wrong, attunement, definitions
		).success() && wrong.getCount() == 3, "Wrong items must remain with the contributor");
		ItemStack blocked = new ItemStack(Items.AMETHYST_SHARD, 4);
		helper.assertTrue(!CabinUpgradeService.contribute(
			registry, cabin.uuid(), outsider, blocked, attunement, definitions
		).success() && blocked.getCount() == 4, "Untrusted players cannot contribute");

		CabinUpgradeState.TrackedUpgrade tracked = registry.find(cabin.uuid()).orElseThrow()
			.upgrades().tracked().orElseThrow();
		helper.assertTrue(tracked.fundedCount(Items.GLASS_PANE.builtInRegistryHolder().key().identifier()) == 12,
			"Concurrent contributions must never exceed the resolved requirement");
		helper.assertTrue(tracked.fund().stream().anyMatch(stack ->
			Component.literal("Neighbour contribution").equals(stack.get(DataComponents.CUSTOM_NAME))),
			"The fund must preserve contributed stack components");
		helper.succeed();
	}

	@GameTest
	public void stoppingTrackingIsOwnerOnlyAndEjectsTheWholeFund(GameTestHelper helper) {
		WorldAttunement attunement = CabinUpgradeDefinitions.current().resolve(29L);
		CabinUpgradeDefinitions.Definitions definitions = testUpgradeDefinitions(attunement, 8, 4);
		CabinRegistry registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		UUID visitor = UUID.randomUUID();
		CabinRecord cabin = deployRegistryCabin(registry, owner, 32);
		CabinUpgradeService.track(registry, cabin.uuid(), owner,
			CabinUpgradeState.Target.generalSpace(5), attunement, definitions);
		ItemStack named = new ItemStack(Items.GLASS_PANE, 6);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Shared fund"));
		CabinUpgradeService.contribute(registry, cabin.uuid(), owner, named, attunement, definitions);

		int[] ejections = {0};
		helper.assertTrue(!CabinUpgradeService.stopTracking(
			registry, cabin.uuid(), visitor, (ignored, stacks) -> {
				ejections[0]++;
				return true;
			}
		).success() && ejections[0] == 0, "A non-owner must not eject the fund");
		helper.assertTrue(!CabinUpgradeService.stopTracking(
			registry, cabin.uuid(), owner, (ignored, stacks) -> false
		).success() && registry.find(cabin.uuid()).orElseThrow().upgrades().tracked().isPresent(),
			"A failed ejection must leave tracking and the fund intact");

		List<ItemStack> ejected = new java.util.ArrayList<>();
		helper.assertTrue(CabinUpgradeService.stopTracking(
			registry, cabin.uuid(), owner, (ignored, stacks) -> {
				ejected.addAll(stacks);
				return true;
			}
		).success(), "The owner must be able to confirm whole-fund ejection");
		helper.assertTrue(registry.find(cabin.uuid()).orElseThrow().upgrades().equals(CabinUpgradeState.EMPTY)
			&& ejected.size() == 1 && ejected.getFirst().getCount() == 6
			&& Component.literal("Shared fund").equals(ejected.getFirst().get(DataComponents.CUSTOM_NAME)),
			"Successful untracking must eject exact stacks and clear the tracked upgrade");
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

		TestExpansionEffect blocked = new TestExpansionEffect(false, false);
		helper.assertTrue(!CabinUpgradeService.install(
			registry, cabin.uuid(), visitor, attunement, definitions, blocked, () -> { }
		).success(), "A non-owner must not install a funded upgrade");
		helper.assertTrue(!CabinUpgradeService.install(
			registry, cabin.uuid(), owner, attunement, definitions, blocked, () -> { }
		).success(), "An obstructed expansion must fail validation");
		helper.assertTrue(registry.find(cabin.uuid()).orElseThrow().upgrades().tracked().isPresent(),
			"Failed installation checks must retain the complete fund");

		TestExpansionEffect interrupted = new TestExpansionEffect(true, true);
		int[] flushes = {0};
		helper.assertTrue(!CabinUpgradeService.install(
			registry, cabin.uuid(), owner, attunement, definitions, interrupted, () -> flushes[0]++
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
			&& installed.upgrades().equals(CabinUpgradeState.EMPTY) && resumed.applications == 1,
			"Restart reconciliation must apply once, consume the fund and clear tracking");
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

		helper.assertTrue(blocks.size() == 115,
			"The fixed cabin must own exactly its 5x5 shell plus one front step");
		helper.assertTrue(blocks.get(ExteriorCabin.controller(exterior)).is(Blocks.LODESTONE),
			"The fixed exterior must have a distinct lodestone controller");
		helper.assertTrue(blocks.get(ExteriorCabin.doorLower(exterior)).is(Blocks.IRON_DOOR)
			&& blocks.get(ExteriorCabin.doorUpper(exterior)).is(Blocks.IRON_DOOR),
			"The fixed exterior must have a two-block entrance");
		helper.assertTrue(!ExteriorCabin.owns(exterior, ExteriorCabin.outsideDestination(exterior)),
			"The protected structure mask must not claim nearby player space");
		for (int lateral = -3; lateral <= 3; lateral++) {
			for (int depth = -2; depth <= 6; depth++) {
				for (int y = -1; y <= 6; y++) {
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
	public void progressionInteriorStartsFourByFourWithAnchoredEntrance(GameTestHelper helper) {
		long cell = 7;
		BlockPos center = PocketDimension.cellCenter(cell);
		var bounds = PocketDimension.bounds(CabinProgression.INITIAL_GENERAL_SIZE);
		BlockPos innerCorner = center.offset(bounds.maximumX(), 1, bounds.maximumZ());
		BlockPos wall = center.offset(bounds.shellMaximumX(), 1, 0);

		helper.assertTrue(bounds.maximumX() - bounds.minimumX() + 1 == 4
			&& bounds.maximumZ() - bounds.minimumZ() + 1 == 4,
			"A new progression cabin must expose exactly a 4x4 usable footprint");
		helper.assertTrue(!PocketDimension.isInteriorShell(cell, innerCorner),
			"The 4x4 inner footprint must remain usable");
		helper.assertTrue(PocketDimension.isInteriorShell(cell, wall)
			&& PocketDimension.isInteriorShell(cell, center),
			"The room wall and floor must belong to the protected shell");
		helper.assertTrue(PocketDimension.isInteriorExit(cell, PocketDimension.interiorExitDoorLower(cell)),
			"Every interior must have a stable exit-door coordinate");
		helper.assertTrue(PocketDimension.interiorExitDoorLower(cell).getZ()
			== PocketDimension.cellCenter(cell).getZ() + PocketDimension.INTERIOR_FRONT_WALL_Z,
			"The entrance wall must remain anchored while the rear and sides expand");
		helper.succeed();
	}

	@GameTest
	public void ceilingHeightGrowsEverySecondSizeStepAndCapsAtTen(GameTestHelper helper) {
		long cell = 8;
		BlockPos center = PocketDimension.cellCenter(cell);
		var sizeFour = PocketDimension.shellBlocks(cell, 4, CabinPalette.DEFAULT);
		var sizeFive = PocketDimension.shellBlocks(cell, 5, CabinPalette.DEFAULT);
		var sizeSix = PocketDimension.shellBlocks(cell, 6, CabinPalette.DEFAULT);
		var sizeTwenty = PocketDimension.shellBlocks(cell, 20, CabinPalette.DEFAULT);
		var maximum = PocketDimension.shellBlocks(
			cell, CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE, CabinPalette.DEFAULT
		);

		helper.assertTrue(sizeFour.containsKey(center.offset(0, 3, 0))
			&& sizeFive.containsKey(center.offset(0, 3, 0)),
			"Sizes four and five must have two clear blocks below the ceiling");
		helper.assertTrue(sizeSix.containsKey(center.offset(0, 4, 0)),
			"Size six must gain its first block of clear height");
		helper.assertTrue(sizeTwenty.containsKey(center.offset(0, 11, 0))
			&& maximum.containsKey(center.offset(0, 11, 0)),
			"Clear interior height must cap at ten blocks from size twenty onward");
		helper.assertTrue(!sizeFour.containsKey(center.offset(0, 4, 0))
			&& !maximum.containsKey(center.offset(0, 12, 0)),
			"Cabin shells must not retain a ceiling above their size-derived height");
		helper.succeed();
	}

	@GameTest
	public void expansionPatternGrowsOneBlockWithoutMovingTheEntrance(GameTestHelper helper) {
		var four = PocketDimension.bounds(4);
		var five = PocketDimension.bounds(5);
		var six = PocketDimension.bounds(6);
		helper.assertTrue(four.maximumZ() == five.maximumZ() && five.maximumZ() == six.maximumZ(),
			"Every expansion must keep the entrance side anchored");
		helper.assertTrue(five.minimumZ() == four.minimumZ() - 1 && six.minimumZ() == five.minimumZ() - 1,
			"Every expansion must add exactly one row at the rear");
		helper.assertTrue(five.minimumX() == four.minimumX() - 1 && five.maximumX() == four.maximumX(),
			"The first lateral expansion must grow left deterministically");
		helper.assertTrue(six.minimumX() == five.minimumX() && six.maximumX() == five.maximumX() + 1,
			"The next lateral expansion must grow right deterministically");
		helper.succeed();
	}

	@GameTest
	public void generalExpansionPreservesPlayerBlocksAndRejectsObstructions(GameTestHelper helper) {
		long cell = 50;
		BlockPos center = PocketDimension.cellCenter(cell);
		BlockPos playerBlock = center.above();
		BlockPos obstruction = center.offset(0, 1, PocketDimension.bounds(5).shellMinimumZ());
		BlockPos raisedCeilingObstruction = center.offset(
			0, PocketDimension.clearInteriorHeight(6) + 1, 0
		);

		PocketDimension.ExpansionCheck blocked = PocketDimension.validateExpansion(
			cell, 4, 5, obstruction::equals
		);
		helper.assertTrue(!blocked.valid(),
			"An obstruction in the target shell must reject expansion before mutation");
		PocketDimension.ExpansionCheck verticallyBlocked = PocketDimension.validateExpansion(
			cell, 5, 6, raisedCeilingObstruction::equals
		);
		helper.assertTrue(!verticallyBlocked.valid(),
			"An obstruction above the old ceiling must reject a height-growing expansion");
		helper.assertTrue(PocketDimension.isWithinUsable(cell, 4, playerBlock)
			&& PocketDimension.isWithinUsable(cell, 5, playerBlock),
			"Existing player blocks must remain in the unchanged usable-volume intersection");
		helper.assertTrue(!PocketDimension.isWithinUsable(cell, 5, center.offset(0, 3, 0))
			&& PocketDimension.isWithinUsable(cell, 6, center.offset(0, 3, 0)),
			"A height-growing expansion must expose the old ceiling layer as usable space");
		helper.assertTrue(PocketDimension.isWithinUsable(cell, 5, center.offset(-2, 1, -2)),
			"A successful 4x4 to 5x5 expansion must expose the deterministic new row and column");
		helper.succeed();
	}

	@GameTest
	public void legacyDebugPlatformDoesNotBlockFirstExpansion(GameTestHelper helper) {
		long cell = 61;
		ServerLevel level = helper.getLevel();
		PocketDimension.ensureDebugMarker(level, cell);
		PocketDimension.ensureCabinInterior(level, cell, CabinPalette.DEFAULT, 4);

		PocketDimension.ExpansionCheck check = PocketDimension.validateExpansion(level, cell, 4, 5);
		helper.assertTrue(check.valid(),
			"The old command-created debug platform must not leave a rim in the first expansion footprint");
		BlockPos isolatedSmoothStone = PocketDimension.cellCenter(cell).offset(-3, 0, 0);
		level.setBlockAndUpdate(isolatedSmoothStone, Blocks.SMOOTH_STONE.defaultBlockState());
		helper.assertTrue(PocketDimension.removeLegacyDebugPlatformResidue(level, cell, 4) == 0
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
		helper.assertTrue(blocks.get(ExteriorCabin.local(exterior, 0, 2, ExteriorCabin.ROOF_Y))
			.is(Blocks.BAMBOO_PLANKS), "Exterior roof must use the selected roof family");
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
								"Each exterior corner must wrap one Structural Wood column onto both walls");
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
		var expandedBounds = PocketDimension.bounds(6);
		var expanded = PocketDimension.shellBlocks(expandedCell, 6, palette);
		BlockPos expandedCenter = PocketDimension.cellCenter(expandedCell);
		for (int x = expandedBounds.shellMinimumX(); x <= expandedBounds.shellMaximumX(); x++) {
			boolean shouldFrame = x - expandedBounds.shellMinimumX() < 2
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
			level.dimension(), helper.absolutePos(new BlockPos(5, 2, 5)), Direction.EAST
		);
		for (var entry : ExteriorCabin.legacyBlocks(exterior, palette).entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		BlockPos exteriorNewFrame = ExteriorCabin.local(exterior, -1, 0, 1);
		helper.assertTrue(ExteriorCabin.projectionValid(level, exterior, palette),
			"A complete legacy exterior must remain valid during migration");
		helper.assertTrue(ExteriorCabin.upgradeLegacyCornerFrames(level, exterior, palette)
			&& level.getBlockState(exteriorNewFrame).is(palette.walls().structuralWoodBlock()),
			"Recognizable exterior wall planks must be upgraded to the new frame");
		helper.assertTrue(!ExteriorCabin.upgradeLegacyCornerFrames(level, exterior, palette),
			"Repeating an exterior frame migration must not change an already-current structure");

		CabinExterior partiallyMigrated = new CabinExterior(
			level.dimension(), helper.absolutePos(new BlockPos(20, 2, 5)), Direction.SOUTH
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
			&& ExteriorCabin.upgradeLegacyCornerFrames(level, partiallyMigrated, palette)
			&& level.getBlockState(remainingLegacyFrame).is(palette.walls().structuralWoodBlock()),
			"A partially migrated exterior must remain valid and finish migrating");

		long interiorCell = 65;
		CabinExterior recordExterior = new CabinExterior(Level.OVERWORLD, BlockPos.ZERO, Direction.NORTH);
		CabinRecord cabin = new CabinRecord(
			UUID.randomUUID(), UUID.randomUUID(), interiorCell, CabinLifecycle.DEPLOYED,
			Optional.of(recordExterior), true
		);
		for (var entry : PocketDimension.legacyShellBlocks(interiorCell, 4, palette).entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
		var bounds = PocketDimension.bounds(4);
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
		for (var entry : PocketDimension.legacyShellBlocks(damagedCell, 4, palette).entrySet()) {
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

		var blocks = CabinWindows.blocks(7, CabinWindows.Profile.DAY);
		BlockPos cabinCenter = PocketDimension.cellCenter(7);
		helper.assertTrue(blocks.size() == 4,
			"The compact interior must retain two visible fake-window panels");
		for (BlockPos position : blocks.keySet()) {
			helper.assertTrue(PocketDimension.isInteriorShell(7, position),
				"Every fake-window block must remain part of the protected interior shell");
			helper.assertTrue(position.getY() - cabinCenter.getY() >= 1
				&& position.getY() - cabinCenter.getY()
					<= PocketDimension.clearInteriorHeight(CabinProgression.INITIAL_GENERAL_SIZE),
				"Starting-cabin windows must stay below the lowered ceiling");
		}
		for (int size = CabinProgression.INITIAL_GENERAL_SIZE;
			 size <= CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE; size++) {
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
			"Cabin upgrade tracking and contributed stack data must survive every transition");
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
		CabinUpgradeService.track(registry, cabinId, owner,
			CabinUpgradeState.Target.generalSpace(5), attunement, definitions);
		ItemStack panes = new ItemStack(Items.GLASS_PANE, 12);
		ItemStack shards = new ItemStack(Items.AMETHYST_SHARD, 4);
		CabinUpgradeService.contribute(registry, cabinId, owner, panes, attunement, definitions);
		CabinUpgradeService.contribute(registry, cabinId, owner, shards, attunement, definitions);
	}

	private static final class TestExpansionEffect implements CabinUpgradeService.ExpansionEffect {
		private final boolean valid;
		private final boolean interrupt;
		private int applications;

		private TestExpansionEffect(boolean valid, boolean interrupt) {
			this.valid = valid;
			this.interrupt = interrupt;
		}

		@Override
		public CabinUpgradeService.Outcome validate(CabinRecord cabin, int targetSize) {
			return valid
				? CabinUpgradeService.Outcome.success("Expansion volume is clear.")
				: CabinUpgradeService.Outcome.failure("Expansion is obstructed.");
		}

		@Override
		public void apply(CabinRecord cabin, int targetSize) {
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
