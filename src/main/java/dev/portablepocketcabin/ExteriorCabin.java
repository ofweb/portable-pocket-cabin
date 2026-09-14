package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class ExteriorCabin {
	static final int CORE_RADIUS = 2;
	static final int CORE_DEPTH = 4;
	static final int ROOF_Y = 5;
	private static final int LEGACY_CORNER_FRAME_DEPTH = 1;
	private static final int CORNER_FRAME_DEPTH = 2;

	record PlacementCheck(boolean valid, String message) {
	}

	private ExteriorCabin() {
	}

	static CabinExterior previewFor(net.minecraft.server.level.ServerPlayer player) {
		Direction playerFacing = player.getDirection();
		Direction doorFacing = playerFacing.getOpposite();
		BlockPos anchor = player.blockPosition().relative(playerFacing, 5);
		return new CabinExterior(player.level().dimension(), anchor, doorFacing);
	}

	static CabinExterior previewFor(
		net.minecraft.server.level.ServerPlayer player, BlockPos supportBlock
	) {
		return exteriorFor(player.level().dimension(), supportBlock, player.getDirection());
	}

	static CabinExterior exteriorFor(
		net.minecraft.resources.ResourceKey<Level> dimension, BlockPos supportBlock, Direction playerFacing
	) {
		Direction doorFacing = playerFacing.getOpposite();
		BlockPos frontStep = supportBlock.above();
		BlockPos anchor = frontStep.relative(doorFacing.getOpposite());
		return new CabinExterior(dimension, anchor, doorFacing);
	}

	static Map<BlockPos, BlockState> blocks(CabinExterior exterior) {
		return blocks(exterior, CabinPalette.DEFAULT);
	}

	static Map<BlockPos, BlockState> blocks(CabinExterior exterior, CabinPalette palette) {
		return blocks(exterior, palette, CORNER_FRAME_DEPTH);
	}

	static Map<BlockPos, BlockState> legacyBlocks(CabinExterior exterior, CabinPalette palette) {
		return blocks(exterior, palette, LEGACY_CORNER_FRAME_DEPTH);
	}

	private static Map<BlockPos, BlockState> blocks(
		CabinExterior exterior, CabinPalette palette, int cornerFrameDepth
	) {
		Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
		BlockState floor = palette.floor().planksBlock().defaultBlockState();
		BlockState roof = palette.roof().planksBlock().defaultBlockState();
		BlockState wall = palette.walls().planksBlock().defaultBlockState();
		BlockState frame = palette.walls().structuralWoodBlock().defaultBlockState();

		for (int lateral = -CORE_RADIUS; lateral <= CORE_RADIUS; lateral++) {
			for (int depth = 0; depth <= CORE_DEPTH; depth++) {
				blocks.put(local(exterior, lateral, depth, 0), floor);
				blocks.put(local(exterior, lateral, depth, ROOF_Y), roof);

				if (Math.abs(lateral) == CORE_RADIUS || depth == 0 || depth == CORE_DEPTH) {
					for (int y = 1; y < ROOF_Y; y++) {
						boolean structural = isStructuralFrame(lateral, depth, cornerFrameDepth);
						blocks.put(local(exterior, lateral, depth, y), structural ? frame : wall);
					}
				}
			}
		}

		BlockState lowerDoor = palette.door().doorBlock().defaultBlockState()
			.setValue(DoorBlock.FACING, exterior.facing())
			.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
			.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
		BlockState upperDoor = lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
		blocks.put(doorLower(exterior), lowerDoor);
		blocks.put(doorUpper(exterior), upperDoor);
		blocks.put(controller(exterior), Blocks.LODESTONE.defaultBlockState());

		blocks.put(local(exterior, -CORE_RADIUS, 2, 2), Blocks.GLASS.defaultBlockState());
		blocks.put(local(exterior, CORE_RADIUS, 2, 2), Blocks.GLASS.defaultBlockState());
		blocks.put(local(exterior, 0, CORE_DEPTH, 2), Blocks.GLASS.defaultBlockState());
		BlockState step = palette.floor().stairsBlock().defaultBlockState()
			.setValue(StairBlock.FACING, exterior.facing().getOpposite());
		blocks.put(frontStep(exterior), step);
		return Collections.unmodifiableMap(new LinkedHashMap<>(blocks));
	}

	static PlacementCheck validate(ServerLevel level, CabinExterior exterior) {
		if (!isSupportedDimension(exterior.dimension()) || !level.dimension().equals(exterior.dimension())) {
			return new PlacementCheck(false, "Cabins can only be deployed in the Overworld, Nether, or End");
		}
		for (BlockPos floor : floorAndStep(exterior)) {
			BlockPos support = floor.below();
			if (level.getFluidState(floor).is(net.minecraft.tags.FluidTags.LAVA)
				|| level.getFluidState(support).is(net.minecraft.tags.FluidTags.LAVA)) {
				return new PlacementCheck(false, "Cabins cannot be deployed on lava");
			}
		}

		for (BlockPos pos : clearance(exterior)) {
			if (level.isOutsideBuildHeight(pos)) {
				return new PlacementCheck(false, "Cabin would extend outside the build height");
			}
			if (!level.getWorldBorder().isWithinBounds(pos)) {
				return new PlacementCheck(false, "Cabin would extend outside the world border");
			}
			if (!level.getBlockState(pos).canBeReplaced()) {
				return new PlacementCheck(false, "Cabin space is blocked at " + shortPos(pos));
			}
		}

		for (BlockPos floor : floorAndStep(exterior)) {
			BlockPos support = floor.below();
			if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) {
				return new PlacementCheck(false, "Cabin needs solid, level ground below " + shortPos(floor));
			}
		}

		BlockPos outside = outsideDestination(exterior);
		BlockPos outsideHead = outside.above();
		BlockPos outsideFloor = outside.below();
		if (!level.getBlockState(outside).canBeReplaced()
			|| !level.getBlockState(outsideHead).canBeReplaced()
			|| !level.getBlockState(outsideFloor).isFaceSturdy(level, outsideFloor, Direction.UP)) {
			return new PlacementCheck(false, "The exterior doorway does not have a safe standing space");
		}

		return new PlacementCheck(true, "Cabin footprint is clear");
	}

	static boolean isSupportedDimension(net.minecraft.resources.ResourceKey<Level> dimension) {
		return dimension.equals(Level.OVERWORLD)
			|| dimension.equals(Level.NETHER)
			|| dimension.equals(Level.END);
	}

	static void place(ServerLevel level, CabinExterior exterior) {
		place(level, exterior, CabinPalette.DEFAULT);
	}

	static void place(ServerLevel level, CabinExterior exterior, CabinPalette palette) {
		for (Map.Entry<BlockPos, BlockState> entry : blocks(exterior, palette).entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
	}

	static boolean projectionValid(ServerLevel level, CabinExterior exterior) {
		return projectionValid(level, exterior, CabinPalette.DEFAULT);
	}

	static boolean projectionValid(ServerLevel level, CabinExterior exterior, CabinPalette palette) {
		return matchesCurrentOrLegacyProjection(level, exterior, palette);
	}

	static boolean upgradeLegacyCornerFrames(
		ServerLevel level, CabinExterior exterior, CabinPalette palette
	) {
		Map<BlockPos, BlockState> legacy = legacyBlocks(exterior, palette);
		Map<BlockPos, BlockState> current = blocks(exterior, palette);
		if (!matchesCurrentOrLegacyProjection(level, legacy, current)) {
			return false;
		}
		boolean changed = false;
		for (Map.Entry<BlockPos, BlockState> entry : current.entrySet()) {
			BlockState previous = legacy.get(entry.getKey());
			if (!previous.is(entry.getValue().getBlock())
				&& level.getBlockState(entry.getKey()).is(previous.getBlock())) {
				level.setBlockAndUpdate(entry.getKey(), entry.getValue());
				changed = true;
			}
		}
		return changed;
	}

	private static boolean matchesCurrentOrLegacyProjection(
		ServerLevel level, CabinExterior exterior, CabinPalette palette
	) {
		return matchesCurrentOrLegacyProjection(
			level, legacyBlocks(exterior, palette), blocks(exterior, palette)
		);
	}

	private static boolean matchesCurrentOrLegacyProjection(
		ServerLevel level, Map<BlockPos, BlockState> legacy, Map<BlockPos, BlockState> current
	) {
		for (Map.Entry<BlockPos, BlockState> entry : current.entrySet()) {
			BlockState actual = level.getBlockState(entry.getKey());
			BlockState previous = legacy.get(entry.getKey());
			if (!actual.is(entry.getValue().getBlock()) && !actual.is(previous.getBlock())) {
				return false;
			}
		}
		return true;
	}

	private static boolean isStructuralFrame(int lateral, int depth, int cornerFrameDepth) {
		boolean sideWall = Math.abs(lateral) == CORE_RADIUS;
		boolean endWall = depth == 0 || depth == CORE_DEPTH;
		return sideWall && nearEdge(depth, 0, CORE_DEPTH, cornerFrameDepth)
			|| endWall && nearEdge(lateral, -CORE_RADIUS, CORE_RADIUS, cornerFrameDepth);
	}

	private static boolean nearEdge(int value, int minimum, int maximum, int depth) {
		return value - minimum < depth || maximum - value < depth;
	}

	static boolean touchesChunk(CabinExterior exterior, ChunkPos chunk) {
		BlockPos anchor = exterior.anchor();
		int maximumHorizontalExtent = CORE_DEPTH;
		return anchor.getX() + maximumHorizontalExtent >= chunk.getMinBlockX()
			&& anchor.getX() - maximumHorizontalExtent <= chunk.getMaxBlockX()
			&& anchor.getZ() + maximumHorizontalExtent >= chunk.getMinBlockZ()
			&& anchor.getZ() - maximumHorizontalExtent <= chunk.getMaxBlockZ();
	}

	static void removeProjection(ServerLevel level, CabinExterior exterior) {
		removeProjection(level, exterior, CabinPalette.DEFAULT);
	}

	static void removeProjection(ServerLevel level, CabinExterior exterior, CabinPalette palette) {
		for (Map.Entry<BlockPos, BlockState> entry : blocks(exterior, palette).entrySet()) {
			if (level.getBlockState(entry.getKey()).is(entry.getValue().getBlock())) {
				level.setBlockAndUpdate(entry.getKey(), Blocks.AIR.defaultBlockState());
			}
		}
	}

	static void showPreview(ServerLevel level, CabinExterior exterior, boolean valid) {
		for (BlockPos pos : footprintOutline(exterior)) {
			level.sendParticles(
				valid ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ELECTRIC_SPARK,
				pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5,
				2, 0.12, 0.05, 0.12, 0.0
			);
		}
	}

	static boolean owns(CabinExterior exterior, BlockPos pos) {
		int dx = pos.getX() - exterior.anchor().getX();
		int dz = pos.getZ() - exterior.anchor().getZ();
		Direction right = exterior.facing().getClockWise();
		Direction inward = exterior.facing().getOpposite();
		int lateral = dx * right.getStepX() + dz * right.getStepZ();
		int depth = dx * inward.getStepX() + dz * inward.getStepZ();
		int y = pos.getY() - exterior.anchor().getY();

		if (lateral == 0 && depth == -1 && y == 0) {
			return true;
		}
		if (Math.abs(lateral) > CORE_RADIUS || depth < 0 || depth > CORE_DEPTH) {
			return false;
		}
		if (y == 0 || y == ROOF_Y) {
			return true;
		}
		return y > 0 && y < ROOF_Y
			&& (Math.abs(lateral) == CORE_RADIUS || depth == 0 || depth == CORE_DEPTH);
	}

	static boolean isEntrance(CabinExterior exterior, BlockPos pos) {
		return pos.equals(doorLower(exterior)) || pos.equals(doorUpper(exterior))
			|| pos.equals(controller(exterior));
	}

	static boolean isController(CabinExterior exterior, BlockPos pos) {
		return pos.equals(controller(exterior));
	}

	static BlockPos doorLower(CabinExterior exterior) {
		return local(exterior, 0, 0, 1);
	}

	static BlockPos doorUpper(CabinExterior exterior) {
		return local(exterior, 0, 0, 2);
	}

	static BlockPos controller(CabinExterior exterior) {
		return local(exterior, 1, 0, 1);
	}

	static BlockPos frontStep(CabinExterior exterior) {
		return local(exterior, 0, -1, 0);
	}

	static BlockPos outsideDestination(CabinExterior exterior) {
		return local(exterior, 0, -2, 0);
	}

	static BlockPos local(CabinExterior exterior, int lateral, int depth, int y) {
		return exterior.anchor()
			.relative(exterior.facing().getClockWise(), lateral)
			.relative(exterior.facing().getOpposite(), depth)
			.above(y);
	}

	private static Set<BlockPos> clearance(CabinExterior exterior) {
		Set<BlockPos> positions = new LinkedHashSet<>();
		for (int lateral = -CORE_RADIUS; lateral <= CORE_RADIUS; lateral++) {
			for (int depth = 0; depth <= CORE_DEPTH; depth++) {
				for (int y = 0; y <= ROOF_Y; y++) {
					positions.add(local(exterior, lateral, depth, y));
				}
			}
		}
		positions.add(frontStep(exterior));
		return positions;
	}

	private static Set<BlockPos> floorAndStep(CabinExterior exterior) {
		Set<BlockPos> positions = new LinkedHashSet<>();
		for (int lateral = -CORE_RADIUS; lateral <= CORE_RADIUS; lateral++) {
			for (int depth = 0; depth <= CORE_DEPTH; depth++) {
				positions.add(local(exterior, lateral, depth, 0));
			}
		}
		positions.add(frontStep(exterior));
		return positions;
	}

	private static Set<BlockPos> footprintOutline(CabinExterior exterior) {
		Set<BlockPos> positions = new LinkedHashSet<>();
		for (int lateral = -CORE_RADIUS; lateral <= CORE_RADIUS; lateral++) {
			positions.add(local(exterior, lateral, 0, 0));
			positions.add(local(exterior, lateral, CORE_DEPTH, 0));
		}
		for (int depth = 1; depth < CORE_DEPTH; depth++) {
			positions.add(local(exterior, -CORE_RADIUS, depth, 0));
			positions.add(local(exterior, CORE_RADIUS, depth, 0));
		}
		positions.add(frontStep(exterior));
		return positions;
	}

	private static String shortPos(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}
}
