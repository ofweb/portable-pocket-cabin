package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

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
