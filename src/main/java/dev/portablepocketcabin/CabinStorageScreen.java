package dev.portablepocketcabin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class CabinStorageScreen extends AbstractContainerScreen<CabinStorageMenu> {
	private EditBox search;
	private List<CreativeModeTab> categories = List.of();
	private final Map<Item, Integer> order = new HashMap<>();
	private final Set<Item> categorised = new HashSet<>();
	private final List<Integer> filtered = new ArrayList<>();
	private int selected;
	private int row;
	private boolean scrolling;

	CabinStorageScreen(CabinStorageMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, CabinStorageLayout.WIDTH, CabinStorageLayout.HEIGHT);
	}
	@Override protected void init() {
		super.init();
		CreativeModeTabs.tryRebuildTabContents(minecraft.level.enabledFeatures(), false, minecraft.level.registryAccess());
		categories = CreativeModeTabs.tabs().stream().filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY).toList();
		order.clear();
		categorised.clear();
		for (var tab : categories) for (var stack : tab.getDisplayItems()) {
			order.putIfAbsent(stack.getItem(), order.size());
			categorised.add(stack.getItem());
		}
		search = addRenderableWidget(new EditBox(font, leftPos + 9, topPos + 5, 160, 14, Component.literal("Search")));
		search.setMaxLength(80);
		search.setHint(Component.literal("Search"));
		search.setResponder(value -> { row = 0; rebuild(); });
		for (int i = 0; i < categories.size() + 3; i++) {
			final int index = i;
			addRenderableWidget(new Tab(index, tabName(index), tabIcon(index)));
		}
		selected = 0;
		menu.inventoryTab = false;
		rebuild();
	}
	private Component tabName(int index) {
		if (index == 0) return Component.literal("Search");
		if (index <= categories.size()) return categories.get(index - 1).getDisplayName();
		return Component.literal(index == categories.size() + 1 ? "Miscellaneous" : "Inventory");
	}
	private ItemStack tabIcon(int index) {
		if (index == 0) return new ItemStack(Items.COMPASS);
		if (index <= categories.size()) return categories.get(index - 1).getIconItem();
		return new ItemStack(index == categories.size() + 1 ? Items.CHEST : Items.PLAYER_HEAD);
	}
	void select(int index) {
		selected = index;
		row = 0;
		menu.inventoryTab = index == categories.size() + 2;
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.inventoryTab ? 1 : 0);
		search.visible = index == 0;
		rebuild();
	}
	void searchFor(String value) { select(0); search.setValue(value); }

	private void rebuild() {
		filtered.clear();
		for (int i = 0; i < menu.entryCount(); i++) {
			ItemStack stack = menu.slots.get(i).getItem();
			if (stack.isEmpty()) continue;
			String query = search.getValue().toLowerCase(Locale.ROOT);
			boolean matches = selected == 0 ? query.isEmpty()
				|| net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(query)
				|| getTooltipFromContainerItem(stack).stream().anyMatch(line -> line.getString().toLowerCase(Locale.ROOT).contains(query))
				: selected <= categories.size() ? categories.get(selected - 1).getDisplayItems().stream()
					.anyMatch(value -> value.getItem() == stack.getItem())
				: !categorised.contains(stack.getItem());
			if (matches) filtered.add(i);
		}
		filtered.sort(Comparator.comparingInt(index -> order.getOrDefault(menu.slots.get(index).getItem().getItem(), Integer.MAX_VALUE)));
		row = Math.min(row, maxRow());
	}
	private int maxRow() { return Math.max(0, (filtered.size() + 8) / 9 - 5); }
	@Override protected void containerTick() { super.containerTick(); rebuild(); }
	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		background(graphics);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("hud/experience_bar_background"), leftPos + 9, topPos + 21, 162, 5);
		int filled = menu.capacity() == 0 ? 0 : 162 * menu.used() / menu.capacity();
		if (filled > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("hud/experience_bar_progress"),
			162, 5, 0, 0, leftPos + 9, topPos + 21, filled, 5);
		if (!menu.inventoryTab) {
			for (int i = 0; i < 45; i++) {
				int x = leftPos + 9 + i % 9 * 18;
				int y = topPos + 28 + i / 9 * 18;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("container/slot"), x - 1, y - 1, 18, 18);
				int entry = row * 9 + i;
				if (entry < filtered.size()) {
					ItemStack stack = menu.slots.get(filtered.get(entry)).getItem();
					graphics.fakeItem(stack, x, y);
					int quantity = menu.quantity(filtered.get(entry));
					graphics.itemDecorations(font, stack, x, y, quantity == 1 ? null
						: quantity >= 1000 ? quantity / 1000 + "k" : Integer.toString(quantity));
				}
			}
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("container/creative_inventory/scroller"),
				leftPos + 176, topPos + 28 + (maxRow() == 0 ? 0 : 73 * row / maxRow()), 12, 15);
		} else {
			for (int i = 0; i < 5; i++) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("container/slot"),
				leftPos + (i == 4 ? 116 : 8 + i * 18), topPos + 39, 18, 18);
			for (int i = 0; i < 27; i++) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("container/slot"),
				leftPos + 8 + i % 9 * 18, topPos + 67 + i / 9 * 18, 18, 18);
		}
		for (int i = 0; i < 9; i++) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("container/slot"),
			leftPos + 8 + i * 18, topPos + 125, 18, 18);
	}
	@Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (selected != 0) graphics.text(font, tabName(selected), 9, 7, 0xff404040, false);
	}
	@Override protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (isHovering(9, 21, 162, 5, mouseX, mouseY))
			graphics.setTooltipForNextFrame(Component.literal(menu.used() + " / " + menu.capacity() + " stack slots"), mouseX, mouseY);
		int index = gridIndex(mouseX, mouseY);
		if (index >= 0 && index < filtered.size()) {
			var tooltip = new ArrayList<>(getTooltipFromContainerItem(menu.slots.get(filtered.get(index)).getItem()));
			tooltip.add(Component.literal("Stored: " + menu.quantity(filtered.get(index))));
			graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}
	private int gridIndex(double x, double y) {
		if (menu.inventoryTab || !isHovering(9, 28, 162, 90, x, y)) return -1;
		return row * 9 + (int)(x - leftPos - 9) / 18 + (int)(y - topPos - 28) / 18 * 9;
	}
	@Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int index = gridIndex(event.x(), event.y());
		if (index >= 0 && (event.button() == 0 || event.button() == 1)) {
			if (index >= filtered.size() && menu.getCarried().isEmpty()) return true;
			int slot = index < filtered.size() ? filtered.get(index) : 0;
			minecraft.gameMode.handleContainerInput(menu.containerId, slot, event.button(),
				event.hasShiftDown() ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP, minecraft.player);
			return true;
		}
		if (!menu.inventoryTab && isHovering(176, 28, 12, 90, event.x(), event.y())) {
			scrolling = true;
			scrollTo(event.y());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
	private void scrollTo(double y) {
		row = Math.clamp((int)Math.round((y - topPos - 35) / 73 * maxRow()), 0, maxRow());
	}
	@Override public boolean mouseDragged(MouseButtonEvent event, double x, double y) {
		if (scrolling) { scrollTo(event.y()); return true; }
		return super.mouseDragged(event, x, y);
	}
	@Override public boolean mouseReleased(MouseButtonEvent event) {
		scrolling = false;
		return super.mouseReleased(event);
	}

	@Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		if (!menu.inventoryTab) { row = Math.clamp(row - (int)vertical, 0, maxRow()); return true; }
		return super.mouseScrolled(x, y, horizontal, vertical);
	}
	@Override public boolean keyPressed(KeyEvent event) {
		if (search.visible && search.isFocused() && event.key() != 256) return search.keyPressed(event);
		return super.keyPressed(event);
	}
	private void background(GuiGraphicsExtractor graphics) {
		int[] dx = {0, 4, imageWidth - 4}, dy = {0, 4, imageHeight - 4};
		int[] sx = {0, 4, 172}, sy = {0, 4, 162};
		int[] widths = {4, imageWidth - 8, 4}, heights = {4, imageHeight - 8, 4};
		for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++)
			graphics.blit(RenderPipelines.GUI_TEXTURED, sprite("textures/gui/container/inventory.png"),
				leftPos + dx[x], topPos + dy[y], sx[x], sy[y], widths[x], heights[y],
				x == 1 ? 1 : 4, y == 1 ? 1 : 4, 256, 256);
	}

	private static Identifier sprite(String name) { return Identifier.withDefaultNamespace(name); }
	private final class Tab extends AbstractButton {
		private final int index;
		private final ItemStack icon;
		Tab(int index, Component name, ItemStack icon) {
			super(leftPos + index % 7 * 28, topPos + (index < 7 ? -28 : imageHeight), 28, 28, name);
			this.index = index;
			this.icon = icon;
			setTooltip(Tooltip.create(name));
		}
		@Override public void onPress(net.minecraft.client.input.InputWithModifiers input) { select(index); }
		@Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			String side = index < 7 ? "top" : "bottom";
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("container/creative_inventory/tab_" + side + "_"
				+ (selected == index ? "selected" : "unselected") + "_" + (index % 7 + 1)), getX(), getY(), 28, 28);
			graphics.fakeItem(icon, getX() + 6, getY() + 6);
		}
		@Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
	}
}
