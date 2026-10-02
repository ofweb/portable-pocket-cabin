package dev.portablepocketcabin;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class PortablePocketCabinClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(CabinUpgradeMenu.TYPE, CabinUpgradeScreen::new);
		MenuScreens.register(CabinStorageMenu.TYPE, CabinStorageScreen::new);
	}
}
