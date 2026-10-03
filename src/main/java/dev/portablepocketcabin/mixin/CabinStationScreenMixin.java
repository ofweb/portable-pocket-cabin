package dev.portablepocketcabin.mixin;

import dev.portablepocketcabin.CabinStationClient;
import dev.portablepocketcabin.CabinStationPayload;
import dev.portablepocketcabin.CabinStationPanel;
import static dev.portablepocketcabin.CabinStationLayout.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

@Mixin(AbstractContainerScreen.class)
abstract class CabinStationScreenMixin extends Screen implements CabinStationPanel {
	@Shadow protected int leftPos;
	@Shadow protected int topPos;
	@Shadow @Final protected AbstractContainerMenu menu;
	@Shadow @Final protected int imageWidth;
	@Unique private EditBox cabin$search;
	@Unique private int cabin$page;
	@Unique private final List<Integer> cabin$filtered = new ArrayList<>();
	@Unique private List<CabinStationPayload.Entry> cabin$entries;
	@Unique private boolean cabin$overlay;
	@Unique private boolean cabin$panelOpen = true;
	protected CabinStationScreenMixin() { super(Component.empty()); }

	@Unique private boolean cabin$active() {
		return !(menu instanceof CraftingMenu) && CabinStationClient.entries(menu.containerId) != null;
	}
	@Unique private void cabin$update() {
		if (!cabin$active()) return;
		cabin$overlay = width < imageWidth + OFFSET;
		leftPos = cabin$overlay ? (width - imageWidth) / 2 : (width - imageWidth + OFFSET) / 2;
		if (cabin$search == null) {
			cabin$search = addWidget(new EditBox(font, cabin$panelX() + 11, topPos + 15, 125, 14, Component.literal("Search operations")));
			cabin$search.setHint(Component.literal("Search"));
			cabin$search.setResponder(value -> { cabin$page = 0; cabin$filter(); });
		}
		cabin$search.setPosition(cabin$panelX() + 11, topPos + 15);
		cabin$search.visible = !cabin$overlay || cabin$panelOpen;
		cabin$search.active = cabin$search.visible;
		var entries = CabinStationClient.entries(menu.containerId);
		if (entries != cabin$entries) { cabin$entries = entries; cabin$filter(); }
	}
	@Override public boolean cabin$panelOnly() { return cabin$active() && cabin$overlay && cabin$panelOpen; }
	@Unique private int cabin$panelX() { return cabin$overlay ? (width - WIDTH) / 2 : leftPos - OFFSET; }
	@Unique private void cabin$filter() {
		cabin$filtered.clear();
		if (cabin$entries == null) return;
		String query = cabin$search.getValue().toLowerCase(Locale.ROOT);
		for (int i = 0; i < cabin$entries.size(); i++) if (cabin$entries.get(i).title().toLowerCase(Locale.ROOT).contains(query)) cabin$filtered.add(i);
		cabin$page = Math.min(cabin$page, Math.max(0, (cabin$filtered.size() - 1) / PAGE_SIZE));
	}
	@Inject(method = "init", at = @At("RETURN"))
	private void cabin$init(CallbackInfo ci) { cabin$search = null; cabin$update(); }
	@Inject(method = "containerTick", at = @At("HEAD"))
	private void cabin$tick(CallbackInfo ci) { cabin$update(); }
	@Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
	private void cabin$hideContainer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		cabin$update();
		if (cabin$active() && cabin$overlay && cabin$panelOpen) ci.cancel();
	}
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void cabin$panel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		cabin$update();
		if (!cabin$active()) return;
		if (cabin$overlay) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("recipe_book/button"), leftPos - 25, topPos + 5, 20, 18);
			if (!cabin$panelOpen) return;
		}
		int x = cabin$panelX();
		graphics.blit(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("textures/gui/recipe_book.png"), x, topPos, 0, 0, WIDTH, HEIGHT, 256, 256);
		cabin$search.extractRenderState(graphics, mouseX, mouseY, partialTick);
		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = cabin$page * PAGE_SIZE + i;
			if (index >= cabin$filtered.size()) break;
			var entry = cabin$entries.get(cabin$filtered.get(index));
			int bx = x + GRID_X + i % COLUMNS * CELL, by = topPos + GRID_Y + i / COLUMNS * CELL;
			boolean hover = mouseX >= bx && mouseX < bx + CELL && mouseY >= by && mouseY < by + CELL;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(entry.available() ? "recipe_book/slot_craftable" : "recipe_book/slot_uncraftable"), bx, by, CELL, CELL);
			graphics.fakeItem(entry.icon(), bx + 4, by + 4);
			if (entry.selected()) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("container/slot_highlight_front"), bx, by, CELL, CELL);
			if (hover) {
				var tooltip = new ArrayList<>(Screen.getTooltipFromItem(minecraft, entry.icon()));
				tooltip.addFirst(Component.literal(entry.title()));
				tooltip.add(Component.literal(entry.available() ? "Click to fill inputs" : "Missing ordinary ingredients"));
				graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
			}
		}
		if (cabin$page > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("recipe_book/page_backward"), x + 15, topPos + 143, 12, 17);
		if ((cabin$page + 1) * PAGE_SIZE < cabin$filtered.size()) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("recipe_book/page_forward"), x + 119, topPos + 143, 12, 17);
		graphics.text(font, (cabin$page + 1) + " / " + Math.max(1, (cabin$filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE), x + 56, topPos + 148, 0xffffffff, true);
	}
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void cabin$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> ci) {
		if (!cabin$active() || cabin$search == null) return;
		if (cabin$overlay && event.x() >= leftPos - 25 && event.x() < leftPos - 5 && event.y() >= topPos + 5 && event.y() < topPos + 23) {
			cabin$panelOpen = !cabin$panelOpen; cabin$update(); ci.setReturnValue(true); return;
		}
		if (cabin$overlay && !cabin$panelOpen) return;
		int x = cabin$panelX();
		if (cabin$overlay) ci.setReturnValue(true);
		if (event.x() < x || event.x() >= x + WIDTH || event.y() < topPos || event.y() >= topPos + HEIGHT) return;
		if (cabin$search.mouseClicked(event, doubleClick)) { setFocused(cabin$search); ci.setReturnValue(true); return; }
		if (event.y() >= topPos + 143 && event.y() < topPos + 160) {
			if (event.x() >= x + 119 && (cabin$page + 1) * PAGE_SIZE < cabin$filtered.size()) cabin$page++;
			if (event.x() < x + 28 && cabin$page > 0) cabin$page--;
		} else if (event.x() >= x + 11 && event.x() < x + 136 && event.y() >= topPos + 35 && event.y() < topPos + 135) {
			int index = cabin$page * PAGE_SIZE + (int)(event.y() - topPos - 35) / CELL * COLUMNS + (int)(event.x() - x - 11) / CELL;
			if (index < cabin$filtered.size()) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10000 + cabin$filtered.get(index));
		}
		ci.setReturnValue(true);
	}
}
