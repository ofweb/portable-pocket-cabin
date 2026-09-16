package dev.portablepocketcabin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Applies the derived world projection for a persisted window reversal. */
final class CabinWindowReversalEffect implements CabinWindowReversalService.Effect {
	private final MinecraftServer server;
	private final CabinWindowWorld windows;

	CabinWindowReversalEffect(MinecraftServer server, ServerLevel pocket) {
		this.server = server;
		this.windows = new CabinWindowWorld(server, pocket);
	}

	@Override
	public CabinUpgradeService.Outcome validate(CabinRecord cabin, CabinWindowState resultingState) {
		return windows.validateChange(cabin, resultingState);
	}

	@Override
	public void apply(CabinRecord cabin, CabinWindowState resultingState) {
		windows.applyChange(cabin, resultingState);
	}

	@Override
	public void refresh(CabinRecord cabin) {
		CabinWindows.refresh(server, cabin);
	}
}
