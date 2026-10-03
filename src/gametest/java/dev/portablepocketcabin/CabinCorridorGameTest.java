package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;

import java.util.*;

public final class CabinCorridorGameTest {
	@GameTest
	public void completeFloorsAndSmallEntrancesStayFixedThroughExpansion(GameTestHelper helper) {
		for (int size = 5; size <= 21; size += 2) {
			BlockPos center = PocketDimension.cellCenter(0);
			int radius = size / 2;
			var west = CabinCorridorLayout.floor(0, size, CabinCorridor.WEST);
			helper.assertTrue(west.size() == 37 * 3 + 29 * 3, "West installs the full run and livestock branch");
			helper.assertTrue(west.contains(center.offset(-radius - 36, 0, -30)), "The livestock end is always present");
			var all = new HashSet<BlockPos>();
			for (CabinCorridor corridor : CabinCorridor.values()) {
				var blocks = CabinCorridorLayout.blocks(0, size, CabinPalette.DEFAULT, corridor);
				BlockPos entrance = CabinCorridorLayout.entrance(0, size, corridor);
				helper.assertTrue(blocks.get(entrance).isAir() && blocks.get(entrance.above()).isAir(), "Entrances are two blocks high");
				helper.assertTrue(!blocks.get(entrance.above(2)).isAir(), "Entrances do not open to corridor height");
				for (BlockPos floor : CabinCorridorLayout.floor(0, size, corridor)) {
					helper.assertTrue(all.add(floor), "Corridor floors never overlap");
					helper.assertTrue(Math.abs(floor.getX() - center.getX()) < 256 && Math.abs(floor.getZ() - center.getZ()) < 256,
						"The complete layout stays in its owning cabin cell");
				}
			}
			var bend = CabinCorridorLayout.blocks(0, size, CabinPalette.DEFAULT, CabinCorridor.WEST);
			for (int x = -radius - 37; x <= -radius - 35; x++) {
				for (int y = 1; y <= 3; y++) helper.assertTrue(!bend.containsKey(center.offset(x, y, -1)), "The whole bend stays open");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void snapshotRefusesSplitObjectsAndReplaysSavedContents(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
		((ChestBlockEntity) level.getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 9));
		var moves = Map.of(pos, pos.east());
		var snapshot = CabinCorridorSnapshot.capture(level, moves);
		var restored = CabinCorridorSnapshot.load(snapshot.save());
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		restored.apply(level); restored.apply(level);
		helper.assertTrue(((ChestBlockEntity) level.getBlockEntity(pos.east())).getItem(0).getCount() == 9, "Serialized interrupted moves preserve chest contents");
		level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		level.setBlock(pos, Blocks.OAK_DOOR.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		boolean refused = false;
		try { CabinCorridorSnapshot.capture(level, moves); } catch (IllegalStateException expected) { refused = expected.getMessage().contains("split an object"); }
		helper.assertTrue(refused, "A move cannot split a door");
		level.setBlock(pos, Blocks.STRUCTURE_BLOCK.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		refused = false;
		try { CabinCorridorSnapshot.capture(level, moves); } catch (IllegalStateException expected) { refused = expected.getMessage().contains("unsupported block entity"); }
		helper.assertTrue(refused && level.getBlockState(pos).is(Blocks.STRUCTURE_BLOCK) && level.getBlockState(pos.east()).isAir(),
			"Unadapted external-coordinate records are rejected without changing either position");
		helper.succeed();
	}

	@GameTest
	public void snapshotPreservesFurnaceContentsAndScheduledTicks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
		BlockPos destination = source.east();
		level.setBlock(source, Blocks.FURNACE.defaultBlockState(), CabinCorridorSnapshot.FLAGS);
		var furnace = (FurnaceBlockEntity) level.getBlockEntity(source);
		furnace.setItem(0, new ItemStack(Items.IRON_ORE, 5));
		furnace.setItem(1, new ItemStack(Items.COAL, 3));
		furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 2));
		level.scheduleTick(source, Blocks.FURNACE, 40);
		level.scheduleTick(source, Fluids.WATER, 60);
		var snapshot = CabinCorridorSnapshot.load(CabinCorridorSnapshot.capture(level, Map.of(source, destination)).save());
		snapshot.apply(level);
		snapshot.apply(level);
		var moved = (FurnaceBlockEntity) level.getBlockEntity(destination);
		helper.assertTrue(moved.getItem(0).getCount() == 5 && moved.getItem(1).getCount() == 3
			&& moved.getItem(2).getCount() == 2, "Furnace input, fuel, and output survive repeated recovery");
		var chunk = level.getChunkAt(destination);
		var blocks = ((LevelChunkTicks<net.minecraft.world.level.block.Block>) chunk.getBlockTicks()).getAll()
			.filter(tick -> tick.pos().equals(destination) && tick.type() == Blocks.FURNACE).toList();
		var fluids = ((LevelChunkTicks<net.minecraft.world.level.material.Fluid>) chunk.getFluidTicks()).getAll()
			.filter(tick -> tick.pos().equals(destination) && tick.type() == Fluids.WATER).toList();
		helper.assertTrue(blocks.size() == 1 && blocks.getFirst().triggerTick() == level.getGameTime() + 40,
			"Recovery preserves one block tick and its remaining delay");
		helper.assertTrue(fluids.size() == 1 && fluids.getFirst().triggerTick() == level.getGameTime() + 60,
			"Recovery preserves one fluid tick and its remaining delay");
		helper.assertTrue(!level.getBlockTicks().hasScheduledTick(source, Blocks.FURNACE)
			&& !level.getFluidTicks().hasScheduledTick(source, Fluids.WATER), "Old tick positions are cleared");
		helper.succeed();
	}

}
