package dev.portablepocketcabin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Compact, vanilla-inventory-shaped upgrade screen with icon-first controls. */
final class CabinUpgradeScreen extends AbstractContainerScreen<CabinUpgradeMenu> {
	private Button installButton;
	private Button downgradeButton;
	private Button removeButton;
	private Button previousPanelButton;
	private Button nextPanelButton;
	private final List<CategoryTabButton> categoryButtons = new ArrayList<>();

	CabinUpgradeScreen(CabinUpgradeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, CabinUpgradeLayout.SCREEN_WIDTH, CabinUpgradeLayout.SCREEN_HEIGHT);
		titleLabelX = 8;
		titleLabelY = 7;
		inventoryLabelX = CabinUpgradeLayout.INVENTORY_X;
		inventoryLabelY = CabinUpgradeLayout.INVENTORY_LABEL_Y;
	}

	@Override
	protected void init() {
		super.init();
		previousPanelButton = addRenderableWidget(new ImageButton(
			leftPos + CabinUpgradeLayout.PANEL_NAV_X, topPos + CabinUpgradeLayout.PANEL_NAV_Y,
			CabinUpgradeLayout.PANEL_NAV_WIDTH, CabinUpgradeLayout.PANEL_NAV_HEIGHT, new WidgetSprites(sprite("recipe_book/page_backward"),
				sprite("recipe_book/page_backward_highlighted")),
			button -> sendMenuButton(CabinUpgradeMenu.BUTTON_PREVIOUS_PANEL),
			Component.translatable("screen.portable_pocket_cabin.previous_upgrade")
		));
		previousPanelButton.setTooltip(Tooltip.create(previousPanelButton.getMessage()));
		nextPanelButton = addRenderableWidget(new ImageButton(
			leftPos + CabinUpgradeLayout.NEXT_PANEL_X, topPos + CabinUpgradeLayout.PANEL_NAV_Y,
			CabinUpgradeLayout.PANEL_NAV_WIDTH, CabinUpgradeLayout.PANEL_NAV_HEIGHT, new WidgetSprites(sprite("recipe_book/page_forward"),
				sprite("recipe_book/page_forward_highlighted")),
			button -> sendMenuButton(CabinUpgradeMenu.BUTTON_NEXT_PANEL),
			Component.translatable("screen.portable_pocket_cabin.next_upgrade")
		));
		nextPanelButton.setTooltip(Tooltip.create(nextPanelButton.getMessage()));
		installButton = addRenderableWidget(Button.builder(
			Component.translatable("screen.portable_pocket_cabin.install"), button -> sendInstall()
		).bounds(
			leftPos + CabinUpgradeLayout.INSTALL_X,
			topPos + CabinUpgradeLayout.ACTION_Y,
			CabinUpgradeLayout.ACTION_WIDTH,
			CabinUpgradeLayout.ACTION_HEIGHT
		).build());
		installButton.setTooltip(Tooltip.create(
			Component.translatable("screen.portable_pocket_cabin.install_tooltip")
		));
		downgradeButton = addRenderableWidget(Button.builder(
			Component.translatable("screen.portable_pocket_cabin.downgrade"), button -> sendMenuButton(CabinUpgradeMenu.BUTTON_DOWNGRADE)
		).bounds(
			leftPos + CabinUpgradeLayout.DOWNGRADE_X,
			topPos + CabinUpgradeLayout.ACTION_Y,
			CabinUpgradeLayout.ACTION_WIDTH,
			CabinUpgradeLayout.ACTION_HEIGHT
		).build());
		removeButton = addRenderableWidget(Button.builder(
			Component.translatable("screen.portable_pocket_cabin.remove"), button -> sendMenuButton(CabinUpgradeMenu.BUTTON_REMOVE)
		).bounds(
			leftPos + CabinUpgradeLayout.REMOVE_X,
			topPos + CabinUpgradeLayout.ACTION_Y,
			CabinUpgradeLayout.ACTION_WIDTH,
			CabinUpgradeLayout.ACTION_HEIGHT
		).build());
		categoryButtons.clear();
		for (int index = 0; index < CabinUpgradeMenu.MAX_GROUPS; index++) {
			CategoryTabButton button = addRenderableWidget(new CategoryTabButton(
				leftPos + CabinUpgradeLayout.TAB_X,
				topPos + CabinUpgradeLayout.tabY(index), index
			));
			categoryButtons.add(button);
		}
		refreshButton();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		refreshButton();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		containerBackground(graphics);
		graphics.blit(RenderPipelines.GUI_TEXTURED,
			sprite("textures/gui/container/generic_54.png"),
			leftPos + CabinUpgradeLayout.INVENTORY_X - 1,
			topPos + CabinUpgradeLayout.INVENTORY_BACKGROUND_Y, 7, 126, 162, 90, 256, 256);
		for (int index = 0; index < menu.requirementCount(); index++) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("container/slot"),
				leftPos + CabinUpgradeLayout.requirementX(index) - 1,
				topPos + CabinUpgradeLayout.requirementY(index) - 1, 18, 18);
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("container/slot"),
			leftPos + CabinUpgradeLayout.PANEL_ICON_X - 1,
			topPos + CabinUpgradeLayout.PANEL_ICON_Y - 1, 18, 18);
		graphics.blit(RenderPipelines.GUI_TEXTURED,
			sprite("textures/gui/container/smithing.png"),
			leftPos + CabinUpgradeLayout.RESULT_ARROW_X,
			topPos + CabinUpgradeLayout.RESULT_ARROW_Y, 67, 48, 22, 15, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, 0xff404040, false);
		if (!menu.panelStack().isEmpty()) {
			graphics.fakeItem(
				menu.panelStack(), CabinUpgradeLayout.PANEL_ICON_X, CabinUpgradeLayout.PANEL_ICON_Y
			);
			List<net.minecraft.util.FormattedCharSequence> panelTitle = font.split(
				menu.panelTitle(), CabinUpgradeLayout.PANEL_TITLE_WIDTH
			);
			if (!panelTitle.isEmpty()) {
				graphics.text(font, panelTitle.getFirst(),
					CabinUpgradeLayout.PANEL_TITLE_X, CabinUpgradeLayout.PANEL_TITLE_Y,
					0xff404040, false);
			}
			List<net.minecraft.util.FormattedCharSequence> effect = font.split(
				menu.panelEffect(), CabinUpgradeLayout.EFFECT_WIDTH
			);
			if (!effect.isEmpty()) {
				graphics.text(font, effect.getFirst(),
					CabinUpgradeLayout.EFFECT_X, CabinUpgradeLayout.EFFECT_Y, 0xff606060, false);
			}
			if (menu.panelCount() > 1) {
				Component page = Component.literal(
					(menu.selectedPanelIndex() + 1) + "/" + menu.panelCount()
				);
				graphics.text(font, page,
					CabinUpgradeLayout.PANEL_PAGE_X + CabinUpgradeLayout.PANEL_PAGE_WIDTH
						- font.width(page),
					CabinUpgradeLayout.PANEL_PAGE_Y, 0xff606060, false);
			}
		}

		for (int index = 0; index < menu.requirementCount(); index++) {
			int x = CabinUpgradeLayout.requirementX(index);
			int y = CabinUpgradeLayout.requirementY(index) + 18;
			int funded = menu.fundedCount(index);
			int required = menu.requiredCount(index);
			String progress = funded + "/" + required;
			graphics.text(font, progress, x + 8 - font.width(progress) / 2, y,
				funded >= required ? 0xff207a20 : 0xff404040, false);
		}
		Component status = menu.isArmed() || menu.isDowngradeArmed() || menu.isRemoveArmed()
			? Component.translatable("screen.portable_pocket_cabin.confirm_action") : statusDescription();
		List<net.minecraft.util.FormattedCharSequence> statusLines = font.split(status, CabinUpgradeLayout.STATUS_WIDTH);
		if (!statusLines.isEmpty()) {
			graphics.text(font, statusLines.getFirst(), CabinUpgradeLayout.STATUS_X,
				CabinUpgradeLayout.STATUS_Y, 0xff404040, false);
		}
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xff404040, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		int relativeX = mouseX - leftPos;
		int relativeY = mouseY - topPos;
		if (relativeX >= CabinUpgradeLayout.STATUS_X - 1
			&& relativeX < CabinUpgradeLayout.STATUS_X + CabinUpgradeLayout.STATUS_WIDTH
			&& relativeY >= CabinUpgradeLayout.STATUS_Y - 1
			&& relativeY < CabinUpgradeLayout.STATUS_Y + 10) {
			Component message = menu.statusMessage().getString().isEmpty()
				? statusDescription() : menu.statusMessage();
			graphics.setTooltipForNextFrame(message, mouseX, mouseY);
			return;
		}
		if (relativeY >= CabinUpgradeLayout.PANEL_TITLE_Y - 2 && relativeY < 53) {
			graphics.setComponentTooltipForNextFrame(font,
				List.of(menu.panelTitle(), menu.panelEffect()), mouseX, mouseY);
			return;
		}
		for (int index = 0; index < menu.requirementCount(); index++) {
			int x = CabinUpgradeLayout.requirementX(index) - 1;
			int y = CabinUpgradeLayout.requirementY(index) - 1;
			if (relativeX >= x && relativeX < x + 18 && relativeY >= y && relativeY < y + 18) {
				ItemStack stack = menu.requirementStack(index);
				graphics.setComponentTooltipForNextFrame(font, List.of(
					stack.getHoverName(),
					Component.literal(menu.fundedCount(index) + " / " + menu.requiredCount(index)),
					Component.translatable("screen.portable_pocket_cabin.fund_click"),
					Component.translatable("screen.portable_pocket_cabin.fund_shift_click")
				), mouseX, mouseY);
				return;
			}
		}
	}

	private void refreshButton() {
		if (installButton == null || downgradeButton == null || removeButton == null
			|| previousPanelButton == null || nextPanelButton == null) {
			return;
		}
		installButton.visible = menu.isOwner() && !menu.isAtMaximum();
		installButton.active = installButton.visible && menu.isComplete() && menu.isAvailable()
			&& !menu.isStale() && !menu.isBlocked() && !menu.installationInProgress();
		installButton.setMessage(Component.translatable(menu.isArmed()
			? "screen.portable_pocket_cabin.confirm" : "screen.portable_pocket_cabin.install"));
		downgradeButton.visible = menu.isOwner() && menu.hasDowngradeAction();
		downgradeButton.active = downgradeButton.visible && menu.canDowngrade();
		downgradeButton.setMessage(Component.translatable(menu.isDowngradeArmed()
			? "screen.portable_pocket_cabin.confirm" : "screen.portable_pocket_cabin.downgrade"));
		Component downgradeStatus = menu.downgradeStatusMessage();
		downgradeButton.setTooltip(Tooltip.create(downgradeStatus.getString().isEmpty()
			? Component.translatable("screen.portable_pocket_cabin.downgrade_tooltip")
			: downgradeStatus));
		removeButton.visible = menu.isOwner() && menu.hasInstalledWindow();
		removeButton.active = removeButton.visible && menu.canRemove();
		removeButton.setMessage(Component.translatable(menu.isRemoveArmed()
			? "screen.portable_pocket_cabin.confirm" : "screen.portable_pocket_cabin.remove"));
		Component removeStatus = menu.removeStatusMessage();
		removeButton.setTooltip(Tooltip.create(removeStatus.getString().isEmpty()
			? Component.translatable("screen.portable_pocket_cabin.remove_tooltip")
			: removeStatus));
		boolean severalPanels = menu.panelCount() > 1;
		previousPanelButton.visible = severalPanels;
		nextPanelButton.visible = severalPanels;
		for (int index = 0; index < categoryButtons.size(); index++) {
			CategoryTabButton button = categoryButtons.get(index);
			button.visible = index < menu.groupCount();
			button.active = button.visible;
			ItemStack stack = menu.groupStack(index);
			if (!stack.isEmpty()) {
				button.setMessage(stack.getHoverName());
				button.setTooltip(Tooltip.create(stack.getHoverName()));
			}
		}
	}

	private Component statusDescription() {
		if (menu.isAtMaximum()) {
			return Component.translatable("screen.portable_pocket_cabin.maximum");
		}
		if (menu.installationInProgress()) {
			return Component.translatable("screen.portable_pocket_cabin.installing");
		}
		if (menu.isBlocked() || menu.isStale()) {
			return Component.translatable("screen.portable_pocket_cabin.blocked");
		}
		if (menu.isComplete()) {
			return Component.translatable(menu.isOwner()
				? "screen.portable_pocket_cabin.ready" : "screen.portable_pocket_cabin.ready_owner");
		}
		return Component.translatable("screen.portable_pocket_cabin.missing");
	}

	private void sendInstall() {
		sendMenuButton(CabinUpgradeMenu.BUTTON_INSTALL);
	}

	private void sendMenuButton(int button) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
		}
	}

	private final class CategoryTabButton extends AbstractButton {
		private final int groupIndex;

		private CategoryTabButton(int x, int y, int groupIndex) {
			super(x, y, CabinUpgradeLayout.TAB_SIZE, CabinUpgradeLayout.TAB_SIZE, Component.empty());
			this.groupIndex = groupIndex;
			visible = false;
		}

		@Override
		public void onPress(InputWithModifiers input) {
			sendMenuButton(CabinUpgradeMenu.BUTTON_GROUP_BASE + groupIndex);
		}

		@Override
		protected void extractContents(
			GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
		) {
			boolean selected = groupIndex == menu.selectedGroupIndex();
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
				sprite(selected ? "container/beacon/button_selected"
					: isHoveredOrFocused() ? "container/beacon/button_highlighted" : "container/beacon/button"),
				getX(), getY(), width, height);
			ItemStack icon = menu.groupStack(groupIndex);
			if (!icon.isEmpty()) {
				graphics.fakeItem(icon, getX() + 3, getY() + 3);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	private static Identifier sprite(String path) {
		return Identifier.withDefaultNamespace(path);
	}

	private void containerBackground(GuiGraphicsExtractor graphics) {
		Identifier texture = sprite("textures/gui/container/inventory.png");
		int[] destinationsX = {0, 4, imageWidth - 4};
		int[] destinationsY = {0, 4, imageHeight - 4};
		int[] sourceX = {0, 4, 172};
		int[] sourceY = {0, 4, 162};
		int[] widths = {4, imageWidth - 8, 4};
		int[] heights = {4, imageHeight - 8, 4};
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
					leftPos + destinationsX[column], topPos + destinationsY[row],
					sourceX[column], sourceY[row], widths[column], heights[row],
					column == 1 ? 1 : 4, row == 1 ? 1 : 4, 256, 256);
			}
		}
	}
}
