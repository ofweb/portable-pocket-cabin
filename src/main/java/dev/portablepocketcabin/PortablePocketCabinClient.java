package dev.portablepocketcabin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class PortablePocketCabinClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CabinStationClient.register();
		MenuScreens.register(CabinUpgradeMenu.TYPE, CabinUpgradeScreen::new);
		MenuScreens.register(CabinEnchantingMenu.TYPE, CabinEnchantingScreen::new);
		MenuScreens.register(CabinStorageMenu.TYPE, CabinStorageScreen::new);
		for (CookingMenu.Kind kind : CookingMenu.Kind.values()) MenuScreens.register(CookingMenu.type(kind), CookingScreen::new);
		BlockEntityRenderers.register(CookingBlocks.SKILLET_ENTITY, CookingItemsRenderer.skillet());
		CookbookItem.opener = CookbookScreen::open;
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(CookbookAvailability.Reply.TYPE,
			(reply, context) -> CookbookScreen.available = reply.recipes());
		BlockEntityRenderers.register(CookingBlocks.CUTTING_BOARD_ENTITY, CookingItemsRenderer.cuttingBoard());
	}
}
