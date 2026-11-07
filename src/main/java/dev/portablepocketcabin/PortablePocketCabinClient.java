package dev.portablepocketcabin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import java.util.Map;

public final class PortablePocketCabinClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CabinStationClient.register();
		MenuScreens.register(CabinUpgradeMenu.TYPE, CabinUpgradeScreen::new);
		MenuScreens.register(CabinEnchantingMenu.TYPE, CabinEnchantingScreen::new);
		MenuScreens.register(CabinStorageMenu.TYPE, CabinStorageScreen::new);
		for (CookingMenu.Kind kind : CookingMenu.Kind.values()) MenuScreens.register(CookingMenu.type(kind), CookingScreen::new);
		BlockEntityRenderers.register(CookingBlocks.SKILLET_ENTITY, CookingItemsRenderer.skillet());
		BlockEntityRenderers.register(CookingBlocks.GRILL_ENTITY, CookingItemsRenderer.grill());
		BlockEntityRenderers.register(CookingBlocks.AEBLESKIVE_PAN_ENTITY, CookingItemsRenderer.aebleskivePan());
		BlockEntityRenderers.register(CookingBlocks.AGEING_SHELF_ENTITY, CookingItemsRenderer.ageingShelf());
		BlockEntityRenderers.register(CookingBlocks.DRYING_RACK_ENTITY, CookingItemsRenderer.dryingRack());
		CookbookItem.opener = CookbookScreen::open;
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(CookbookAvailability.Reply.TYPE,
			(reply, context) -> {
				if (reply.station()) CookingScreen.fillable = reply.recipes();
				else CookbookScreen.available = reply.recipes();
			});
		BlockEntityRenderers.register(CookingBlocks.CUTTING_BOARD_ENTITY, CookingItemsRenderer.cuttingBoard());
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(PlantNeeds.Sync.TYPE,
			(sync, context) -> PlantNeeds.client = Map.copyOf(sync.needs()));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(CarcassData.Sync.TYPE,
			(sync, context) -> CarcassData.client = Map.copyOf(sync.carcasses()));
		BlockEntityRenderers.register(Butchery.HANGING_HOOK_ENTITY, HangingHookRenderer::new);
		BlockEntityRenderers.register(Butchery.BUTCHER_BLOCK_ENTITY, CookingItemsRenderer.butcherBlock());
		net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Integer quality = stack.get(Seasoning.DISH_QUALITY);
			if (quality != null) {
				lines.add(Math.min(1, lines.size()), net.minecraft.network.chat.Component.translatable("tooltip.portable_pocket_cabin.quality." + quality)
					.withStyle(net.minecraft.ChatFormatting.GOLD));
			}
			Butchery.CarcassState carcass = stack.get(Butchery.CARCASS);
			if (carcass != null) {
				int at = Math.min(1, lines.size());
				if (carcass.fleece().isPresent()) {
					lines.add(at, net.minecraft.network.chat.Component.translatable("tooltip.portable_pocket_cabin.carcass.fleece",
						net.minecraft.network.chat.Component.translatable("color.minecraft." + carcass.fleece().get().getSerializedName()))
						.withStyle(net.minecraft.ChatFormatting.GRAY));
				}
				if (carcass.scalded()) lines.add(at, net.minecraft.network.chat.Component.translatable("tooltip.portable_pocket_cabin.carcass.scalded")
					.withStyle(net.minecraft.ChatFormatting.GRAY));
				if (carcass.bled()) lines.add(at, net.minecraft.network.chat.Component.translatable("tooltip.portable_pocket_cabin.carcass.bled")
					.withStyle(net.minecraft.ChatFormatting.GRAY));
			}
		});
	}
}
