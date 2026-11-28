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

/** The planting board's screen (B-0023): a page of tree types, each with its sapling, and a Teach button. */
final class CabinArboretumScreen extends AbstractContainerScreen<CabinArboretumMenu> {
	private static final int WIDTH = 236, HEIGHT = 240, ROWS = 8, ROW_HEIGHT = 20, LIST_X = 28, LIST_Y = 36, LIST_WIDTH = 200;
	private final Button[] rows = new Button[ROWS];
	private Button teach, previous, next;
	private int page;

	CabinArboretumScreen(CabinArboretumMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
	}

	@Override protected void init() {
		super.init();
		previous = button("<", WIDTH - 66, 14, 28, () -> { page--; update(); });
		next = button(">", WIDTH - 36, 14, 28, () -> { page++; update(); });
		for (int i = 0; i < ROWS; i++) {
			final int index = i;
			rows[i] = button("", LIST_X, LIST_Y + i * ROW_HEIGHT, LIST_WIDTH,
				() -> action(CabinArboretumMenu.CHOICE_BASE + page * ROWS + index));
		}
		teach = button("Teach", 8, HEIGHT - 26, WIDTH - 16, () -> action(CabinArboretumMenu.TEACH));
		update();
	}

	private Button button(String label, int x, int y, int width, Runnable action) {
		return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(leftPos + x, topPos + y, width, 18).build());
	}

	private void action(int button) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button); }

	private List<CabinStationPayload.Entry> entries() {
		var entries = CabinStationClient.entries(menu.containerId);
		return entries == null ? List.of() : entries;
	}

	private void update() {
		var entries = entries();
		int pages = Math.max(1, (entries.size() + ROWS - 1) / ROWS);
		page = Math.clamp(page, 0, pages - 1);
		previous.active = page > 0;
		next.active = page < pages - 1;
		teach.active = menu.household() && menu.teachable();
		for (int i = 0; i < ROWS; i++) {
			int index = page * ROWS + i;
			rows[i].visible = index < entries.size();
			if (index >= entries.size()) continue;
			var entry = entries.get(index);
			rows[i].setMessage(Component.literal(font.plainSubstrByWidth(entry.title(), LIST_WIDTH - 12)));
			rows[i].active = menu.household();
			rows[i].setTooltip(Tooltip.create(Component.literal(entry.title())));
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
		var entries = entries();
		for (int i = 0; i < ROWS; i++) {
			int index = page * ROWS + i;
			if (index >= entries.size()) break;
			var entry = entries.get(index);
			int x = leftPos + 8, y = topPos + LIST_Y + i * ROW_HEIGHT;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("container/slot"), x - 1, y, 18, 18);
			if (entry.selected()) graphics.fill(x, y + 1, x + 16, y + 17, entry.available() ? 0x8040a040 : 0x80c0a020);
			graphics.fakeItem(entry.icon(), x, y + 1);
		}
	}

	@Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, 8, 6, 0xff404040, false);
		String limit = menu.level() == 0 ? "" : "Planting " + menu.selectedCount() + " of " + menu.limit();
		graphics.text(font, limit, 8, 19, 0xff404040, false);
		graphics.text(font, font.plainSubstrByWidth(menu.status(), WIDTH - 16), 8, HEIGHT - 38, 0xff404040, false);
	}

	@Override protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (isHovering(8, HEIGHT - 38, WIDTH - 16, 9, mouseX, mouseY))
			graphics.setTooltipForNextFrame(Component.literal(menu.status()), mouseX, mouseY);
	}
}
