package dev.portablepocketcabin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** A furnace-style screen: slots, a progress arrow, a flame for the current heat, and a status line. */
final class CookingScreen extends AbstractContainerScreen<CookingMenu> {
	private static final Identifier PANEL = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
	private static final Identifier FURNACE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
	private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
	private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");

	private static final int PANEL_WIDTH = 124;
	private static final int ROW = 18;
	/** The station recipes the server last said the recipe panel can fill. */
	static Set<Identifier> fillable = Set.of();
	private final List<RecipeHolder<CookingRecipe>> recipes = new ArrayList<>();
	private boolean panelOpen;
	private int scroll;

	CookingScreen(CookingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		inventoryLabelY = CookingMenu.INVENTORY_Y - 11;
	}

	private CookingStation station() {
		return menu.kind().station;
	}

	@Override
	protected void init() {
		super.init();
		CookingStation station = station();
		if (station == null) return;
		if (panelOpen) leftPos = Math.max(leftPos, PANEL_WIDTH + 8);
		recipes.clear();
		if (minecraft.level != null) {
			CookbookAvailability.recipes(minecraft.level.recipeAccess().getSynchronizedRecipes().recipes()).stream()
				.filter(holder -> holder.value().station() == station)
				.sorted(Comparator.comparing(holder -> holder.value().result().create().getHoverName().getString()))
				.forEach(recipes::add);
		}
		addRenderableWidget(new ImageButton(leftPos + 7, topPos + 26, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES,
			button -> togglePanel(), Component.translatable("screen.portable_pocket_cabin.cooking.recipes")));
	}

	/** Shows or hides the recipe panel, asking the server which recipes it can fill when it opens. */
	void togglePanel() {
		panelOpen = !panelOpen;
		scroll = 0;
		rebuildWidgets();
		if (panelOpen) ClientPlayNetworking.send(new CookbookAvailability.Request(true));
	}

	private int panelLeft() {
		return leftPos - PANEL_WIDTH - 4;
	}

	private int visibleRows() {
		return (imageHeight - 28) / ROW;
	}

	private void extractPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = panelLeft();
		panel(graphics, x, topPos, PANEL_WIDTH, imageHeight);
		graphics.text(font, Component.translatable("screen.portable_pocket_cabin.cooking.recipes"), x + 8, topPos + 6, 0xff404040, false);
		int y = topPos + 18;
		for (int index = scroll; index < recipes.size() && index < scroll + visibleRows(); index++) {
			var holder = recipes.get(index);
			int rowY = y + (index - scroll) * ROW;
			boolean canFill = fillable.contains(holder.id().identifier());
			boolean hovered = mouseX >= x + 6 && mouseX < x + PANEL_WIDTH - 6 && mouseY >= rowY && mouseY < rowY + ROW;
			if (hovered && canFill) graphics.fill(x + 6, rowY, x + PANEL_WIDTH - 6, rowY + ROW, 0x30000000);
			ItemStack result = holder.value().result().create();
			graphics.item(result, x + 7, rowY + 1);
			String name = result.getHoverName().getString();
			int width = PANEL_WIDTH - 36;
			if (font.width(name) > width) name = font.plainSubstrByWidth(name, width - font.width("…")).stripTrailing() + "…";
			graphics.text(font, name, x + 26, rowY + 5, canFill ? 0xff2a7a2a : 0xff808080, false);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (panelOpen) {
			int x = panelLeft();
			int y = topPos + 18;
			if (event.x() >= x + 6 && event.x() < x + PANEL_WIDTH - 6 && event.y() >= y && event.y() < y + visibleRows() * ROW) {
				int index = scroll + (int) ((event.y() - y) / ROW);
				if (index < recipes.size()) fill(recipes.get(index).id().identifier());
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** Asks the server to fill the station for a recipe the panel shows as fillable. */
	void fill(Identifier recipe) {
		if (fillable.contains(recipe)) ClientPlayNetworking.send(new CookingFill.Request(menu.pos(), recipe));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (panelOpen && mouseX < leftPos) {
			scroll = Math.max(0, Math.min(recipes.size() - visibleRows(), scroll - (int) Math.signum(vertical)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		if (panelOpen) extractPanel(graphics, mouseX, mouseY);
		for (int[] slot : menu.kind().slots) slot(graphics, slot[0], slot[1]);
		for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
			slot(graphics, 8 + col * 18, CookingMenu.INVENTORY_Y + row * 18);
		for (int col = 0; col < 9; col++) slot(graphics, 8 + col * 18, CookingMenu.INVENTORY_Y + 58);
		if (menu.kind() == CookingMenu.Kind.STOVE) {
			emptyFlame(graphics, 81, 36);
			flame(graphics, 81, 36);
			return;
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE, leftPos + 91, topPos + 26, 79, 34, 24, 17, 24, 17, 256, 256);
		if (station() != null && station().heated()) emptyFlame(graphics, 97, 46);
		int time = menu.data(CookingMenu.DATA_TIME);
		int progress = menu.data(CookingMenu.DATA_PROGRESS);
		if (time > 0 && progress > 0) {
			int width = Math.round(24F * progress / time);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, leftPos + 91, topPos + 26, width, 16);
		}
		if (menu.kind() == CookingMenu.Kind.OVEN) {
			flame(graphics, 97, 46);
		} else if (menu.data(CookingMenu.DATA_HEAT) > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, leftPos + 97, topPos + 46, 14, 14);
		}
	}

	/** A plain inventory-style panel of any size, stretched from the corners and edges of the inventory texture. */
	static void panel(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
		int[] dx = {0, 4, width - 4}, dy = {0, 4, height - 4};
		int[] sx = {0, 4, 172}, sy = {0, 4, 162};
		int[] widths = {4, width - 8, 4}, heights = {4, height - 8, 4};
		for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++)
			graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, left + dx[x], top + dy[y], sx[x], sy[y],
				widths[x], heights[y], x == 1 ? 1 : 4, y == 1 ? 1 : 4, 256, 256);
	}

	/** The unlit flame outline from the furnace background. */
	private void emptyFlame(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE, leftPos + x, topPos + y, 56, 36, 14, 14, 14, 14, 256, 256);
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
		if (menu.kind() == CookingMenu.Kind.POT || menu.kind() == CookingMenu.Kind.WOK) {
			graphics.text(font, Component.translatable(menu.data(CookingMenu.DATA_LIQUID) == 1
				? "screen.portable_pocket_cabin.cooking.oil" : "screen.portable_pocket_cabin.cooking.water",
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
		if (menu.data(CookingMenu.DATA_PAUSED) != 0) return Component.translatable("screen.portable_pocket_cabin.cooking.covered");
		int needed = menu.data(CookingMenu.DATA_NEEDED_HEAT);
		if (station() != null && station().timeStation() && menu.data(CookingMenu.DATA_TIME) > 0) {
			int left = (menu.data(CookingMenu.DATA_TIME) - menu.data(CookingMenu.DATA_PROGRESS)) * CookingMenu.TIME_STATION_SCALE;
			return Component.translatable("screen.portable_pocket_cabin.cooking.hours_left",
				Math.max(1, (left + CookingStation.HOUR - 1) / CookingStation.HOUR));
		}
		if (station() != null && !station().heated()) {
			return Component.translatable(menu.data(CookingMenu.DATA_TIME) > 0
				? "screen.portable_pocket_cabin.cooking.working" : "screen.portable_pocket_cabin.cooking.idle");
		}
		if (needed == 0) return Component.translatable("screen.portable_pocket_cabin.cooking.idle");
		if (heat != needed) return Component.translatable("screen.portable_pocket_cabin.cooking.needs", heat(needed));
		return Component.translatable("screen.portable_pocket_cabin.cooking.cooking", heat(heat));
	}
}
