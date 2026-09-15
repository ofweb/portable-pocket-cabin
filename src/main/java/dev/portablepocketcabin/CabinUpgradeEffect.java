package dev.portablepocketcabin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Dispatches code-defined upgrade targets to their deterministic pocket-world effects. */
final class CabinUpgradeEffect implements CabinUpgradeService.UpgradeEffect {
	private final MinecraftServer server;
	private final ServerLevel pocket;
	private final CabinWindowWorld windows;

	CabinUpgradeEffect(MinecraftServer server, ServerLevel pocket) {
		this.server = server;
		this.pocket = pocket;
		this.windows = new CabinWindowWorld(server, pocket);
	}

	@Override
	public CabinUpgradeService.Outcome validate(CabinRecord cabin, CabinUpgradeCatalog.Offer offer) {
		if (offer.target().isGeneralSpace()) {
			PocketDimension.ExpansionCheck expansion = PocketDimension.validateExpansion(
				pocket, cabin.cellIndex(), cabin.progression().generalSize(), offer.targetSize()
			);
			if (!expansion.valid()) {
				return CabinUpgradeService.Outcome.failure(expansion.message());
			}
			return windows.validateRelayout(cabin, offer.targetSize());
		}
		return windows.validateInstall(cabin, offer.target().windowIdentity(), offer.targetSize());
	}

	@Override
	public void apply(CabinRecord cabin, CabinUpgradeState.Installation installation) {
		if (installation.target().isGeneralSpace()) {
			PocketDimension.applyGeneralSpaceExpansion(pocket, cabin, installation.targetState());
			windows.applyRelayout(cabin, installation.targetState());
			return;
		}
		windows.applyInstall(cabin, installation.target().windowIdentity(), installation.targetState());
	}

	@Override
	public void refresh(CabinRecord cabin) {
		CabinWindows.refresh(server, cabin);
	}
}
