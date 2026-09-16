package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Validates and projects cabin-owned window state into the pocket dimension. */
final class CabinWindowWorld {
	private final MinecraftServer server;
	private final ServerLevel pocket;

	CabinWindowWorld(MinecraftServer server, ServerLevel pocket) {
		this.server = server;
		this.pocket = pocket;
	}

	CabinUpgradeService.Outcome validateInstall(
		CabinRecord cabin, CabinWindowState.Identity identity, int targetTier
	) {
		CabinWindowLayout.Result current = CabinWindowLayout.current(
			cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows()
		);
		CabinWindowLayout.Result target = CabinWindowLayout.installing(
			cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows(), identity, targetTier
		);
		if (!target.valid()) {
			return CabinUpgradeService.Outcome.failure(target.message());
		}
		return validateTransition(cabin, current.positions(), target.positions(), false);
	}

	CabinUpgradeService.Outcome validateRelayout(CabinRecord cabin, int targetGeneralSize) {
		CabinWindowLayout.Result current = CabinWindowLayout.current(
			cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows()
		);
		CabinWindowLayout.Result target = CabinWindowLayout.current(
			cabin.cellIndex(), targetGeneralSize, cabin.upgrades().windows()
		);
		if (!target.valid()) {
			return CabinUpgradeService.Outcome.failure(target.message());
		}
		for (BlockPos position : current.positions()) {
			String attachment = attachmentAt(position);
			if (attachment != null) {
				return CabinUpgradeService.Outcome.failure(attachment);
			}
		}
		return CabinUpgradeService.Outcome.success("Installed windows can be safely recentered");
	}

	CabinUpgradeService.Outcome validateChange(CabinRecord cabin, CabinWindowState resultingState) {
		CabinWindowLayout.Result current = CabinWindowLayout.current(
			cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows()
		);
		CabinWindowLayout.Result target = CabinWindowLayout.current(
			cabin.cellIndex(), cabin.progression().generalSize(), resultingState
		);
		if (!target.valid()) {
			return CabinUpgradeService.Outcome.failure(target.message());
		}
		return validateTransition(cabin, current.positions(), target.positions(), false);
	}

	void applyInstall(CabinRecord cabin, CabinWindowState.Identity identity, int targetTier) {
		CabinWindowLayout.Result current = CabinWindowLayout.current(
			cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows()
		);
		CabinWindowLayout.Result target = CabinWindowLayout.installing(
			cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows(), identity, targetTier
		);
		if (!target.valid()) {
			throw new IllegalStateException(target.message());
		}
		applyTransition(cabin, current.positions(), target.positions());
	}

	void applyRelayout(CabinRecord cabin, int targetGeneralSize) {
		CabinWindowLayout.Result target = CabinWindowLayout.current(
			cabin.cellIndex(), targetGeneralSize, cabin.upgrades().windows()
		);
		if (!target.valid()) {
			throw new IllegalStateException(target.message());
		}
		placeProfile(target.positions(), CabinWindows.profile(server, cabin));
	}

	void applyChange(CabinRecord cabin, CabinWindowState resultingState) {
		CabinWindowLayout.Result current = CabinWindowLayout.current(
			cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows()
		);
		CabinWindowLayout.Result target = CabinWindowLayout.current(
			cabin.cellIndex(), cabin.progression().generalSize(), resultingState
		);
		if (!target.valid()) {
			throw new IllegalStateException(target.message());
		}
		applyTransition(cabin, current.positions(), target.positions());
	}

	boolean reconcileProjection(CabinRecord cabin) {
		if (!cabin.interiorGenerated()) {
			return false;
		}
		CabinWindowLayout.Result target = CabinWindowLayout.current(
			cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows()
		);
		if (!target.valid()) {
			return false;
		}
		Set<BlockPos> desired = target.positions();
		boolean alreadyProjected = desired.stream().allMatch(position ->
			CabinWindows.isManagedWindowBlock(pocket.getBlockState(position).getBlock())
		);
		if (!alreadyProjected) {
			CabinUpgradeService.Outcome check = validateTransition(cabin, Set.of(), desired, false);
			if (!check.success()) {
				PortablePocketCabin.LOGGER.warn("Could not reconcile windows for cabin {}: {}", cabin.uuid(), check.message());
				return false;
			}
		}

		boolean changed = clearLegacyAutomaticBlocks(cabin, desired);
		CabinWindows.Profile profile = CabinWindows.profile(server, cabin);
		int index = 0;
		for (BlockPos position : desired) {
			Block block = CabinWindows.block(profile, index++);
			if (!pocket.getBlockState(position).is(block)) {
				pocket.setBlockAndUpdate(position, block.defaultBlockState());
				changed = true;
			}
		}
		return changed;
	}

	private CabinUpgradeService.Outcome validateTransition(
		CabinRecord cabin, Set<BlockPos> current, Set<BlockPos> target, boolean allowAir
	) {
		Set<BlockPos> affected = new LinkedHashSet<>(current);
		affected.addAll(target);
		for (BlockPos position : affected) {
			BlockState state = pocket.getBlockState(position);
			if (current.contains(position)) {
				if (!CabinWindows.isManagedWindowBlock(state.getBlock())) {
					return obstructed(position);
				}
			} else if (!state.is(cabin.palette().walls().planksBlock()) && !(allowAir && state.isAir())) {
				return obstructed(position);
			}
			String attachment = attachmentAt(position);
			if (attachment != null) {
				return CabinUpgradeService.Outcome.failure(attachment);
			}
		}
		return CabinUpgradeService.Outcome.success("Window wall is clear");
	}

	private String attachmentAt(BlockPos wallPosition) {
		for (BlockPos neighbor : neighbors(wallPosition)) {
			Block block = pocket.getBlockState(neighbor).getBlock();
			if (isAttachedBlock(block)) {
				return "Window change would displace an attached block at " + coordinates(neighbor);
			}
		}
		if (!pocket.getEntitiesOfClass(HangingEntity.class, new AABB(wallPosition).inflate(1.0)).isEmpty()) {
			return "Window change would displace a hanging decoration near " + coordinates(wallPosition);
		}
		return null;
	}

	private void applyTransition(CabinRecord cabin, Set<BlockPos> current, Set<BlockPos> target) {
		Set<BlockPos> affected = new LinkedHashSet<>(current);
		affected.addAll(target);
		CabinWindows.Profile profile = CabinWindows.profile(server, cabin);
		int index = 0;
		for (BlockPos position : affected) {
			BlockState state = target.contains(position)
				? CabinWindows.block(profile, index++).defaultBlockState()
				: cabin.palette().walls().planksBlock().defaultBlockState();
			pocket.setBlockAndUpdate(position, state);
		}
	}

	private void placeProfile(Set<BlockPos> positions, CabinWindows.Profile profile) {
		int index = 0;
		for (BlockPos position : positions) {
			pocket.setBlockAndUpdate(position, CabinWindows.block(profile, index++).defaultBlockState());
		}
	}

	private boolean clearLegacyAutomaticBlocks(CabinRecord cabin, Set<BlockPos> desired) {
		boolean changed = false;
		PocketDimension.InteriorBounds bounds = PocketDimension.bounds(cabin.progression().generalSize());
		BlockPos center = PocketDimension.cellCenter(cabin.cellIndex());
		int legacyZ = Math.max(bounds.minimumZ(), bounds.maximumZ() - 1);
		for (int x : new int[] {bounds.shellMinimumX(), bounds.shellMaximumX()}) {
			for (int y = 1; y <= 2; y++) {
				BlockPos position = center.offset(x, y, legacyZ);
				if (!desired.contains(position)
					&& isLegacyWindowBlock(pocket.getBlockState(position).getBlock())) {
					pocket.setBlockAndUpdate(position, cabin.palette().walls().planksBlock().defaultBlockState());
					changed = true;
				}
			}
		}
		return changed;
	}

	private static boolean isLegacyWindowBlock(Block block) {
		return CabinWindows.isManagedWindowBlock(block)
			|| block == Blocks.DARK_OAK_PLANKS || block == Blocks.DARK_OAK_LOG;
	}

	private static Set<BlockPos> neighbors(BlockPos position) {
		Set<BlockPos> result = new LinkedHashSet<>();
		for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
			result.add(position.relative(direction));
		}
		return result;
	}

	private static boolean isAttachedBlock(Block block) {
		return block instanceof WallTorchBlock || block instanceof WallSignBlock
			|| block instanceof WallHangingSignBlock || block instanceof WallBannerBlock
			|| block instanceof WallSkullBlock || block instanceof LadderBlock
			|| block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof TripWireHookBlock;
	}

	private static CabinUpgradeService.Outcome obstructed(BlockPos position) {
		return CabinUpgradeService.Outcome.failure("Window wall is obstructed at " + coordinates(position));
	}

	private static String coordinates(BlockPos position) {
		return position.getX() + ", " + position.getY() + ", " + position.getZ();
	}
}
