package dev.portablepocketcabin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** A furnace-style screen: slots, a progress arrow, a flame for the current heat, and a status line. */
final class CookingScreen extends AbstractContainerScreen<CookingMenu> {
	private static final Identifier PANEL = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
	private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
	private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");

	CookingScreen(CookingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		inventoryLabelY = CookingMenu.INVENTORY_Y - 11;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int[] dx = {0, 4, imageWidth - 4}, dy = {0, 4, imageHeight - 4};
		int[] sx = {0, 4, 172}, sy = {0, 4, 162};
		int[] widths = {4, imageWidth - 8, 4}, heights = {4, imageHeight - 8, 4};
		for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++)
			graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, leftPos + dx[x], topPos + dy[y], sx[x], sy[y],
				widths[x], heights[y], x == 1 ? 1 : 4, y == 1 ? 1 : 4, 256, 256);
		for (int[] slot : menu.kind().slots) slot(graphics, slot[0], slot[1]);
		for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
			slot(graphics, 8 + col * 18, CookingMenu.INVENTORY_Y + row * 18);
		for (int col = 0; col < 9; col++) slot(graphics, 8 + col * 18, CookingMenu.INVENTORY_Y + 58);
		if (menu.kind() == CookingMenu.Kind.STOVE) {
			flame(graphics, 81, 36);
			return;
		}
		int time = menu.data(CookingMenu.DATA_TIME);
		int progress = menu.data(CookingMenu.DATA_PROGRESS);
		if (time > 0 && progress > 0) {
			int width = Math.round(24F * progress / time);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, leftPos + 92, topPos + 26, width, 16);
		}
		if (menu.kind() == CookingMenu.Kind.OVEN) {
			flame(graphics, 97, 46);
		} else if (menu.data(CookingMenu.DATA_HEAT) > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, leftPos + 97, topPos + 46, 14, 14);
		}
	}

	/** A flame that shrinks as the burning fuel runs down, like a furnace's. */
	private void flame(GuiGraphicsExtractor graphics, int x, int y) {
		int duration = menu.data(CookingMenu.DATA_LIT_DURATION);
		int left = menu.data(CookingMenu.DATA_LIT_TIME);
		if (duration <= 0 || left <= 0) return;
		int height = Math.max(1, Math.round(14F * left / duration));
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, 14, 14, 0, 14 - height, leftPos + x, topPos + y + 14 - height, 14, height);
	}

	private void slot(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, leftPos + x - 1, topPos + y - 1, 18, 18);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, 8, 6, 0xff404040, false);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xff404040, false);
		Component status = status();
		graphics.text(font, status, imageWidth - 8 - font.width(status), 6, 0xff404040, false);
		if (menu.kind() == CookingMenu.Kind.POT) {
			graphics.text(font, Component.translatable("screen.portable_pocket_cabin.cooking.water",
				menu.data(CookingMenu.DATA_WATER), CookingStation.MAX_WATER), 68, 61, 0xff404040, false);
		}
	}

	private static Component heat(int level) {
		return Component.translatable("screen.portable_pocket_cabin.cooking.heat." + CookingHeat.values()[level - 1].getSerializedName());
	}

	private Component status() {
		int heat = menu.data(CookingMenu.DATA_HEAT);
		if (menu.kind() == CookingMenu.Kind.STOVE) {
			return heat == 0 ? Component.translatable("screen.portable_pocket_cabin.cooking.out")
				: Component.translatable("screen.portable_pocket_cabin.cooking.burning", heat(heat));
		}
		int needed = menu.data(CookingMenu.DATA_NEEDED_HEAT);
		if (needed == 0) return Component.translatable("screen.portable_pocket_cabin.cooking.idle");
		if (heat != needed) return Component.translatable("screen.portable_pocket_cabin.cooking.needs", heat(needed));
		return Component.translatable("screen.portable_pocket_cabin.cooking.cooking", heat(heat));
	}
}
