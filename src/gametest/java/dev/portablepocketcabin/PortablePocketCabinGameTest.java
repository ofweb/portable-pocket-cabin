package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

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
		helper.succeed();
	}

	@GameTest
	public void interiorGeometryProvidesTwentyOneByTwentyOneUsableRoom(GameTestHelper helper) {
		long cell = 7;
		BlockPos center = PocketDimension.cellCenter(cell);
		BlockPos innerCorner = center.offset(
			PocketDimension.INTERIOR_USABLE_RADIUS, 1, PocketDimension.INTERIOR_USABLE_RADIUS
		);
		BlockPos wall = center.offset(PocketDimension.INTERIOR_SHELL_RADIUS, 1, 0);

		helper.assertTrue(PocketDimension.INTERIOR_USABLE_RADIUS * 2 + 1 == 21,
			"A base cabin interior must expose a 21x21 usable footprint");
		helper.assertTrue(!PocketDimension.isInteriorShell(cell, innerCorner),
			"The 21x21 inner footprint must remain usable");
		helper.assertTrue(PocketDimension.isInteriorShell(cell, wall)
			&& PocketDimension.isInteriorShell(cell, center),
			"The room wall and floor must belong to the protected shell");
		helper.assertTrue(PocketDimension.isInteriorExit(cell, PocketDimension.interiorExitDoorLower(cell)),
			"Every interior must have a stable exit-door coordinate");
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
		registry.beginPacking(cabin.uuid(), owner);
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
		helper.setBlock(relativeFeet, Blocks.FIRE);
		helper.assertTrue(!SafeDestinationResolver.isSafe(helper.getLevel(), absoluteFeet, player),
			"Fire must invalidate a destination");
		helper.setBlock(relativeFeet, Blocks.AIR);
		helper.setBlock(relativeFeet.above(), Blocks.STONE);
		helper.assertTrue(!SafeDestinationResolver.isSafe(helper.getLevel(), absoluteFeet, player),
			"A colliding head block must invalidate a destination");
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

		helper.assertTrue(CabinEvents.requiresLoginEvacuation(inside, packed),
			"A player logging into a packed cabin cell must be evacuated");
		helper.assertTrue(!CabinEvents.requiresLoginEvacuation(inside, deployed),
			"A player may remain in a currently deployed cabin");
		helper.assertTrue(!CabinEvents.requiresLoginEvacuation(PocketDimension.cellCenter(cell + 1), packed),
			"Recovery must not confuse adjacent permanent cabin cells");
		helper.succeed();
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
}
