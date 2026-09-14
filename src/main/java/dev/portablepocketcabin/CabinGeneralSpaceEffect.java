package dev.portablepocketcabin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Production adapter for validation, replay-safe world mutation and window refresh. */
final class CabinGeneralSpaceEffect implements CabinUpgradeService.ExpansionEffect {
	private final MinecraftServer server;
	private final ServerLevel pocket;

	CabinGeneralSpaceEffect(MinecraftServer server, ServerLevel pocket) {
		this.server = server;
		this.pocket = pocket;
	}

	@Override
	public CabinUpgradeService.Outcome validate(CabinRecord cabin, int targetSize) {
		PocketDimension.ExpansionCheck check = PocketDimension.validateExpansion(
			pocket, cabin.cellIndex(), cabin.progression().generalSize(), targetSize
		);
		return check.valid()
			? CabinUpgradeService.Outcome.success(check.message())
			: CabinUpgradeService.Outcome.failure(check.message());
	}

	@Override
	public void apply(CabinRecord cabin, int targetSize) {
		PocketDimension.applyGeneralSpaceExpansion(pocket, cabin, targetSize);
	}

	@Override
	public void refresh(CabinRecord cabin) {
		CabinWindows.refresh(server, cabin);
	}
}
