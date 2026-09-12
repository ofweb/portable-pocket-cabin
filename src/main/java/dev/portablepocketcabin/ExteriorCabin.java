package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
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

	static Map<BlockPos, BlockState> blocks(CabinExterior exterior) {
		Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();

		for (int lateral = -CORE_RADIUS; lateral <= CORE_RADIUS; lateral++) {
			for (int depth = 0; depth <= CORE_DEPTH; depth++) {
				blocks.put(local(exterior, lateral, depth, 0), Blocks.POLISHED_ANDESITE.defaultBlockState());
				blocks.put(local(exterior, lateral, depth, ROOF_Y), Blocks.BRICKS.defaultBlockState());

				if (Math.abs(lateral) == CORE_RADIUS || depth == 0 || depth == CORE_DEPTH) {
					for (int y = 1; y < ROOF_Y; y++) {
						BlockState wall = Math.abs(lateral) == CORE_RADIUS && (depth == 0 || depth == CORE_DEPTH)
							? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
							: Blocks.STONE_BRICKS.defaultBlockState();
						blocks.put(local(exterior, lateral, depth, y), wall);
					}
				}
			}
		}

		BlockState lowerDoor = Blocks.IRON_DOOR.defaultBlockState()
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
		blocks.put(frontStep(exterior), Blocks.STONE_BRICKS.defaultBlockState());
		return Collections.unmodifiableMap(new LinkedHashMap<>(blocks));
	}

	static PlacementCheck validate(ServerLevel level, CabinExterior exterior) {
		if (!exterior.dimension().equals(Level.OVERWORLD)) {
			return new PlacementCheck(false, "Delivery 3 cabins can only be deployed in the Overworld");
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

	static void place(ServerLevel level, CabinExterior exterior) {
		for (Map.Entry<BlockPos, BlockState> entry : blocks(exterior).entrySet()) {
			level.setBlockAndUpdate(entry.getKey(), entry.getValue());
		}
	}

	static boolean projectionValid(ServerLevel level, CabinExterior exterior) {
		for (Map.Entry<BlockPos, BlockState> entry : blocks(exterior).entrySet()) {
			if (!level.getBlockState(entry.getKey()).is(entry.getValue().getBlock())) {
				return false;
			}
		}
		return true;
	}

	static void removeProjection(ServerLevel level, CabinExterior exterior) {
		for (Map.Entry<BlockPos, BlockState> entry : blocks(exterior).entrySet()) {
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
