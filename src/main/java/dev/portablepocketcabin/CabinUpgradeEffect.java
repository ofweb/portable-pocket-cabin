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
		if (offer.target().isStorage()) return CabinUpgradeService.Outcome.success("");
		if (offer.target().isGeneralSpace()) {
			var corridorCheck = CabinCorridors.validateExpansion(pocket, cabin);
			if (!corridorCheck.success()) return corridorCheck;
			var moving = CabinCorridors.expansionDestinations(cabin).keySet();
			PocketDimension.ExpansionCheck expansion = PocketDimension.validateExpansion(
				cabin.cellIndex(), cabin.progression().generalSize(), offer.targetSize(),
				pos -> !moving.contains(pos) && !pocket.getBlockState(pos).isAir()
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
		if (installation.target().isStorage()) {
			CabinStorage.placeControl(pocket, cabin, cabin.progression().generalSize());
			return;
		}
		if (installation.target().isGeneralSpace()) {
			CabinCorridors.moveForExpansion(pocket, cabin, installation.targetState());
			CabinCorridors.applyWorldEffect(() -> {
				PocketDimension.applyGeneralSpaceExpansion(pocket, cabin, installation.targetState());
				windows.applyRelayout(cabin, installation.targetState());
				if (cabin.upgrades().storage().level() > 0) CabinStorage.placeControl(pocket, cabin, installation.targetState());
			});
			return;
		}
		windows.applyInstall(cabin, installation.target().windowIdentity(), installation.targetState());
	}

	@Override
	public void refresh(CabinRecord cabin) {
		CabinCorridors.finish(server, cabin.uuid());
		CabinWindows.refresh(server, cabin);
	}
}
