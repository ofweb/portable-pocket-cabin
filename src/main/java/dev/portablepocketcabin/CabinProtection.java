package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public final class CabinProtection {
	private CabinProtection() {
	}

	public static boolean isProtected(ServerLevel level, BlockPos pos) {
		CabinRegistry registry = CabinRegistry.get(level.getServer());
		for (CabinRecord cabin : registry.cabins()) {
			if (level.dimension().equals(PocketDimension.LEVEL_KEY)
				&& (PocketDimension.isInteriorShell(
					cabin.cellIndex(), cabin.progression().generalSize(), pos
				) && !CabinCorridorLayout.isEntrance(cabin, pos)
					|| CabinEnchanting.stations(cabin, cabin.upgrades().enchanting().level()).entrySet().stream()
					.anyMatch(entry -> entry.getKey().equals(pos) && !entry.getValue().is(net.minecraft.world.level.block.Blocks.ANVIL))
					|| CabinCrafting.stations(cabin, cabin.upgrades().crafting().level()).containsKey(pos)
					|| CabinCorridorLayout.isShell(cabin, pos) || cabin.progression().rooms().stream()
					.anyMatch(room -> room.space().map(space -> space.shell(cabin.cellIndex(), pos))
						.orElseGet(() -> PocketDimension.isInteriorShell(room.cellIndex(), pos))))) {
				return true;
			}
			if (cabin.exterior().isPresent()
				&& cabin.exterior().get().dimension().equals(level.dimension())
				&& (cabin.lifecycle() == CabinLifecycle.DEPLOYING
					|| cabin.lifecycle() == CabinLifecycle.DEPLOYED
					|| cabin.lifecycle() == CabinLifecycle.PACKING)
				&& (ExteriorCabin.owns(cabin.exterior().get(), pos)
					|| ExteriorCabin.ownsLegacyRoof(level, cabin.exterior().get(), cabin.palette(), pos))) {
				return true;
			}
		}
		return false;
	}

	static Optional<CabinRecord> findExteriorEntrance(ServerLevel level, BlockPos pos) {
		return CabinRegistry.get(level.getServer()).cabins().stream()
			.filter(cabin -> cabin.exterior().isPresent())
			.filter(cabin -> cabin.exterior().get().dimension().equals(level.dimension()))
			.filter(cabin -> ExteriorCabin.isEntrance(cabin.exterior().get(), pos))
			.findFirst();
	}

	static Optional<CabinRecord> findInteriorExit(ServerLevel level, BlockPos pos) {
		if (!level.dimension().equals(PocketDimension.LEVEL_KEY)) {
			return Optional.empty();
		}
		return CabinRegistry.get(level.getServer()).cabins().stream()
			.filter(cabin -> PocketDimension.isInteriorExit(cabin.cellIndex(), cabin.progression().generalSize(), pos))
			.findFirst();
	}

	static Optional<CabinRecord> findInteriorController(ServerLevel level, BlockPos pos) {
		if (!level.dimension().equals(PocketDimension.LEVEL_KEY)) {
			return Optional.empty();
		}
		return CabinRegistry.get(level.getServer()).cabins().stream()
			.filter(cabin -> PocketDimension.isInteriorController(cabin.cellIndex(), cabin.progression().generalSize(), pos))
			.findFirst();
	}
}
