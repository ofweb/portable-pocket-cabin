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
				&& PocketDimension.isInteriorShell(cabin.cellIndex(), pos)) {
				return true;
			}
			if (cabin.exterior().isPresent()
				&& cabin.exterior().get().dimension().equals(level.dimension())
				&& (cabin.lifecycle() == CabinLifecycle.DEPLOYING || cabin.lifecycle() == CabinLifecycle.DEPLOYED)
				&& ExteriorCabin.owns(cabin.exterior().get(), pos)) {
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
			.filter(cabin -> PocketDimension.isInteriorExit(cabin.cellIndex(), pos))
			.findFirst();
	}
}
