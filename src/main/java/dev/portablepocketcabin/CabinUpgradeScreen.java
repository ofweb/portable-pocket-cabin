package dev.portablepocketcabin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Compact, vanilla-inventory-shaped upgrade screen with icon-first controls. */
final class CabinUpgradeScreen extends AbstractContainerScreen<CabinUpgradeMenu> {
	private static final int PANEL_X = 38;
	private static final int PANEL_Y = 20;
	private static final int PANEL_WIDTH = 202;
	private static final int PANEL_HEIGHT = 96;

	private Button installButton;

	CabinUpgradeScreen(CabinUpgradeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 248, 220);
		titleLabelX = 8;
		titleLabelY = 7;
		inventoryLabelX = 44;
		inventoryLabelY = 126;
	}

	@Override
	protected void init() {
		super.init();
		installButton = addRenderableWidget(Button.builder(
			Component.literal("✓"), button -> sendInstall()
		).bounds(leftPos + 211, topPos + 87, 20, 20).build());
		installButton.setTooltip(Tooltip.create(
			Component.translatable("screen.portable_pocket_cabin.install_tooltip")
		));
		refreshButton();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		refreshButton();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// Vanilla container palette and beveled borders keep the interface visually native.
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
		graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xff373737);
		bevel(graphics, leftPos + PANEL_X, topPos + PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
		bevel(graphics, leftPos + 6, topPos + 20, 26, 26);
		graphics.fakeItem(new ItemStack(Items.OAK_DOOR), leftPos + 11, topPos + 25);
		for (int index = 0; index < menu.requirementCount(); index++) {
			bevel(graphics, leftPos + 54 + index * 22, topPos + 55, 18, 18);
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				bevel(graphics, leftPos + 43 + column * 18, topPos + 135 + row * 18, 18, 18);
			}
		}
		for (int column = 0; column < 9; column++) {
			bevel(graphics, leftPos + 43 + column * 18, topPos + 193, 18, 18);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, 0xff404040, false);
		graphics.text(font, Component.translatable("screen.portable_pocket_cabin.general_space"),
			PANEL_X + 9, PANEL_Y + 8, 0xff404040, false);
		if (menu.isAtMaximum()) {
			graphics.text(font, Component.literal(menu.currentSize() + "×" + menu.currentSize()),
				PANEL_X + 9, PANEL_Y + 23, 0xff606060, false);
		} else {
			graphics.text(font, Component.literal(menu.currentSize() + "×" + menu.currentSize()
				+ "  →  " + menu.targetSize() + "×" + menu.targetSize()),
				PANEL_X + 9, PANEL_Y + 23, 0xff606060, false);
		}

		for (int index = 0; index < menu.requirementCount(); index++) {
			int x = 55 + index * 22;
			int funded = menu.fundedCount(index);
			int required = menu.requiredCount(index);
			graphics.text(font, funded + "/" + required, x - 2, 76,
				funded >= required ? 0xff207a20 : 0xff404040, false);
		}
		graphics.fakeItem(statusIcon(), PANEL_X + PANEL_WIDTH - 29, PANEL_Y + 8);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xff404040, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		int relativeX = mouseX - leftPos;
		int relativeY = mouseY - topPos;
		if (relativeX >= 6 && relativeX < 32 && relativeY >= 20 && relativeY < 46) {
			graphics.setTooltipForNextFrame(
				Component.translatable("screen.portable_pocket_cabin.cabin_tab"), mouseX, mouseY
			);
			return;
		}
		if (relativeX >= PANEL_X + PANEL_WIDTH - 30 && relativeX < PANEL_X + PANEL_WIDTH - 10
			&& relativeY >= PANEL_Y + 7 && relativeY < PANEL_Y + 27) {
			Component message = menu.statusMessage().getString().isEmpty()
				? statusDescription() : menu.statusMessage();
			graphics.setTooltipForNextFrame(message, mouseX, mouseY);
			return;
		}
		for (int index = 0; index < menu.requirementCount(); index++) {
			int x = 54 + index * 22;
			if (relativeX >= x && relativeX < x + 18 && relativeY >= 55 && relativeY < 73) {
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
		if (installButton == null) {
			return;
		}
		installButton.visible = menu.isOwner() && !menu.isAtMaximum();
		installButton.active = installButton.visible && menu.isComplete() && menu.isAvailable()
			&& !menu.isStale() && !menu.isBlocked() && !menu.installationInProgress();
		installButton.setMessage(Component.literal(menu.isArmed() ? "✓✓" : "✓"));
	}

	private ItemStack statusIcon() {
		if (menu.isAtMaximum()) {
			return new ItemStack(Items.NETHER_STAR);
		}
		if (menu.installationInProgress()) {
			return new ItemStack(Items.CLOCK);
		}
		if (menu.isBlocked() || menu.isStale()) {
			return new ItemStack(Items.BARRIER);
		}
		if (menu.isComplete()) {
			return new ItemStack(Items.EMERALD);
		}
		return new ItemStack(Items.REDSTONE);
	}

	private Component statusDescription() {
		if (menu.isAtMaximum()) {
			return Component.translatable("screen.portable_pocket_cabin.maximum");
		}
		if (menu.installationInProgress()) {
			return Component.translatable("screen.portable_pocket_cabin.installing");
		}
		if (menu.isBlocked()) {
			return Component.translatable("screen.portable_pocket_cabin.obstructed");
		}
		if (menu.isComplete()) {
			return Component.translatable("screen.portable_pocket_cabin.ready");
		}
		return Component.translatable("screen.portable_pocket_cabin.missing");
	}

	private void sendInstall() {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CabinUpgradeMenu.BUTTON_INSTALL);
		}
	}

	private static void bevel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, 0xff8b8b8b);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xffffffff);
		graphics.fill(x + 2, y + 2, x + width - 1, y + height - 1, 0xff373737);
		graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xff8b8b8b);
	}
}
