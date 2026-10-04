package dev.portablepocketcabin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

final class CabinEnchantingScreen extends AbstractContainerScreen<CabinEnchantingMenu> {
	private final Button[] rows = new Button[CabinEnchantingLayout.ROWS];
	private Button learn, apply, back, confirm, previous, next;
	private int page;
	private boolean choosingItem = true;
	CabinEnchantingScreen(CabinEnchantingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, CabinEnchantingLayout.WIDTH, CabinEnchantingLayout.HEIGHT);
		inventoryLabelX = CabinEnchantingLayout.INVENTORY_X;
		inventoryLabelY = CabinEnchantingLayout.INVENTORY_Y - 11;
	}
	@Override protected void init() {
		super.init();
		learn = button("Learn", 8, 18, 72, () -> action(CabinEnchantingMenu.LEARN));
		apply = button("Enchant", 84, 18, 72, () -> action(CabinEnchantingMenu.APPLY));
		previous = button("<", 184, 18, 28, () -> { page--; update(); });
		next = button(">", 216, 18, 28, () -> { page++; update(); });
		for (int i = 0; i < rows.length; i++) {
			final int index = i;
			rows[i] = button("", CabinEnchantingLayout.LIST_X, CabinEnchantingLayout.LIST_Y + i * CabinEnchantingLayout.ROW_HEIGHT,
				CabinEnchantingLayout.LIST_WIDTH, () -> action(CabinEnchantingMenu.CHOICE_BASE + page * rows.length + index));
		}
		back = button("Items", 49, 116, 52, () -> action(CabinEnchantingMenu.BACK));
		confirm = button("Confirm", 104, 116, 140, () -> action(CabinEnchantingMenu.CONFIRM));
		update();
	}
	private Button button(String label, int x, int y, int width, Runnable action) {
		return addRenderableWidget(Button.builder(Component.literal(label), button -> action.run()).bounds(leftPos + x, topPos + y, width, 18).build());
	}
	private void action(int button) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button); }
	private List<CabinStationPayload.Entry> entries() {
		var entries = CabinStationClient.entries(menu.containerId); return entries == null ? List.of() : entries;
	}
	private void update() {
		if (choosingItem != menu.choosingItem()) { page = 0; choosingItem = menu.choosingItem(); }
		var entries = entries();
		int pages = Math.max(1, (entries.size() + rows.length - 1) / rows.length);
		page = Math.clamp(page, 0, pages - 1);
		learn.active = !menu.learning(); apply.active = menu.learning();
		previous.active = page > 0; next.active = page < pages - 1;
		back.active = !menu.choosingItem(); confirm.active = menu.ready();
		confirm.setMessage(Component.literal(menu.learning() ? "Confirm sacrifice" : "Confirm enchantment"));
		for (int i = 0; i < rows.length; i++) {
			int index = page * rows.length + i;
			rows[i].visible = index < entries.size();
			if (index < entries.size()) {
				var entry = entries.get(index);
				rows[i].setMessage(Component.literal(font.plainSubstrByWidth((entry.selected() ? "Selected: " : "") + entry.title(), 224)));
				rows[i].active = !entry.selected();
				rows[i].setTooltip(Tooltip.create(Component.literal(entry.title())));
			}
		}
	}
	@Override protected void containerTick() { super.containerTick(); update(); }
	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int[] dx = {0, 4, imageWidth - 4}, dy = {0, 4, imageHeight - 4};
		int[] sx = {0, 4, 172}, sy = {0, 4, 162};
		int[] widths = {4, imageWidth - 8, 4}, heights = {4, imageHeight - 8, 4};
		for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++)
			graphics.blit(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("textures/gui/container/inventory.png"),
				leftPos + dx[x], topPos + dy[y], sx[x], sy[y], widths[x], heights[y], x == 1 ? 1 : 4, y == 1 ? 1 : 4, 256, 256);
		for (int i = 0; i < 2; i++) slot(graphics, CabinEnchantingLayout.SOURCE_X + i * 18, CabinEnchantingLayout.PREVIEW_Y);
		for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
			slot(graphics, CabinEnchantingLayout.INVENTORY_X + col * 18, CabinEnchantingLayout.INVENTORY_Y + row * 18);
		for (int col = 0; col < 9; col++) slot(graphics, CabinEnchantingLayout.INVENTORY_X + col * 18, CabinEnchantingLayout.INVENTORY_Y + 58);
	}
	private void slot(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("container/slot"), leftPos + x - 1, topPos + y - 1, 18, 18);
	}
	@Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, 8, 6, 0xff404040, false);
		graphics.text(font, menu.tier() == 5 ? "No room cap" : "Limit " + menu.tier(), 181, 6, 0xff404040, false);
		graphics.text(font, font.plainSubstrByWidth(menu.status(), 236), 8, 137, 0xff404040, false);
	}
	@Override protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (isHovering(8, 137, 236, 9, mouseX, mouseY)) graphics.setTooltipForNextFrame(Component.literal(menu.status()), mouseX, mouseY);
	}
}
