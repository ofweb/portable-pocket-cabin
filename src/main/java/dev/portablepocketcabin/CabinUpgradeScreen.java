package dev.portablepocketcabin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Compact first-pass upgrade overview; all decisions remain in the server menu and service. */
final class CabinUpgradeScreen extends AbstractContainerScreen<CabinUpgradeMenu> {
	private static final int REQUIREMENTS_PER_PAGE = 8;

	private Button trackButton;
	private Button stopButton;
	private Button installButton;
	private boolean confirmingStop;
	private int requirementPage;

	CabinUpgradeScreen(CabinUpgradeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 248, 276);
		titleLabelX = 10;
		titleLabelY = 8;
		inventoryLabelX = 44;
		inventoryLabelY = 182;
	}

	@Override
	protected void init() {
		super.init();
		trackButton = addRenderableWidget(Button.builder(
			Component.translatable("screen.portable_pocket_cabin.track"),
			button -> sendButton(CabinUpgradeMenu.BUTTON_TRACK)
		).bounds(leftPos + 43, topPos + 121, 62, 20).build());
		stopButton = addRenderableWidget(Button.builder(
			Component.translatable("screen.portable_pocket_cabin.stop_tracking"),
			button -> {
				if (!confirmingStop) {
					confirmingStop = true;
					return;
				}
				sendButton(CabinUpgradeMenu.BUTTON_STOP_TRACKING);
				confirmingStop = false;
			}
		).bounds(leftPos + 43, topPos + 121, 92, 20).build());
		installButton = addRenderableWidget(Button.builder(
			Component.translatable("screen.portable_pocket_cabin.install"),
			button -> sendButton(CabinUpgradeMenu.BUTTON_INSTALL)
		).bounds(leftPos + 140, topPos + 121, 64, 20).build());
		refreshButtons();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		requirementPage = Math.min(requirementPage, maximumRequirementPage());
		if (!menu.isTracked()) {
			confirmingStop = false;
		}
		refreshButtons();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xf0181b20);
		graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xff8f9aa8);
		graphics.fill(leftPos + 11, topPos + 109, leftPos + 35, topPos + 137, 0xff264f32);
		graphics.outline(leftPos + 11, topPos + 109, 24, 28, 0xff72d68a);
		graphics.fill(leftPos + 39, topPos + 188, leftPos + 209, topPos + 272, 0xff101216);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, 0xfff1f3f5, false);
		graphics.text(font, Component.translatable(
			"screen.portable_pocket_cabin.size", menu.currentSize(), menu.currentSize(),
			menu.maximumSize(), menu.maximumSize()
		), 10, 22, 0xffcbd2d9, false);

		ItemStack attuned = menu.attunedStack();
		Component wood = attuned.isEmpty()
			? Component.translatable("screen.portable_pocket_cabin.unavailable") : attuned.getHoverName();
		graphics.text(font, Component.translatable("screen.portable_pocket_cabin.attuned_wood", wood),
			10, 34, 0xffcbd2d9, false);
		if (menu.isAtMaximum()) {
			graphics.text(font, Component.translatable("screen.portable_pocket_cabin.maximum"),
				10, 47, 0xffffd27a, false);
		} else {
			graphics.text(font, Component.translatable(
				"screen.portable_pocket_cabin.effect", menu.targetSize(), menu.targetSize()
			), 10, 47, 0xffa9d8ff, false);
		}

		int firstRequirement = requirementPage * REQUIREMENTS_PER_PAGE;
		int lastRequirement = Math.min(menu.requirementCount(), firstRequirement + REQUIREMENTS_PER_PAGE);
		for (int index = firstRequirement; index < lastRequirement; index++) {
			int visibleIndex = index - firstRequirement;
			int x = 14 + (visibleIndex % 4) * 56;
			int y = 65 + (visibleIndex / 4) * 24;
			ItemStack stack = menu.requirementStack(index);
			graphics.fakeItem(stack, x, y);
			graphics.text(font, menu.fundedCount(index) + "/" + menu.requiredCount(index),
				x + 18, y + 5, menu.fundedCount(index) >= menu.requiredCount(index)
					? 0xff72d68a : 0xffe4e7eb, false);
		}
		if (maximumRequirementPage() > 0) {
			graphics.text(font, Component.translatable(
				"screen.portable_pocket_cabin.requirement_page",
				requirementPage + 1, maximumRequirementPage() + 1
			), 188, 98, 0xff8f9aa8, false);
		}
		graphics.text(font, Component.translatable("screen.portable_pocket_cabin.deposit_hint"),
			12, 98, menu.canContribute() ? 0xff72d68a : 0xff8f9aa8, false);
		if (menu.isStale()) {
			graphics.text(font, Component.translatable("screen.portable_pocket_cabin.stale"),
				10, 144, 0xffff8c82, false);
		} else if (!menu.statusMessage().getString().isEmpty()) {
			graphics.textWithWordWrap(font, menu.statusMessage(), 10, 144, 225, 0xffd9dde2, false);
		}
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffcbd2d9, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		int index = hoveredRequirement(mouseX, mouseY);
		if (index >= 0) {
			ItemStack stack = menu.requirementStack(index);
			if (!stack.isEmpty()) {
				graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX >= leftPos + 10 && mouseX < leftPos + 238
			&& mouseY >= topPos + 60 && mouseY < topPos + 91 && maximumRequirementPage() > 0) {
			int direction = scrollY < 0.0 ? 1 : scrollY > 0.0 ? -1 : 0;
			requirementPage = Math.max(0, Math.min(maximumRequirementPage(), requirementPage + direction));
			return direction != 0;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void refreshButtons() {
		if (trackButton == null) {
			return;
		}
		trackButton.visible = menu.isOwner() && !menu.isTracked() && !menu.isAtMaximum();
		trackButton.active = trackButton.visible;
		stopButton.visible = menu.isOwner() && menu.isTracked();
		stopButton.active = stopButton.visible && !menu.installationInProgress();
		stopButton.setMessage(Component.translatable(confirmingStop
			? "screen.portable_pocket_cabin.confirm_eject"
			: "screen.portable_pocket_cabin.stop_tracking"));
		installButton.visible = menu.isOwner() && menu.isTracked();
		installButton.active = installButton.visible && menu.isComplete()
			&& !menu.isStale() && !menu.isBlocked() && !menu.installationInProgress();
	}

	private void sendButton(int button) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
		}
	}

	private int maximumRequirementPage() {
		return Math.max(0, (menu.requirementCount() - 1) / REQUIREMENTS_PER_PAGE);
	}

	private int hoveredRequirement(int mouseX, int mouseY) {
		int relativeX = mouseX - leftPos;
		int relativeY = mouseY - topPos;
		for (int visibleIndex = 0; visibleIndex < REQUIREMENTS_PER_PAGE; visibleIndex++) {
			int index = requirementPage * REQUIREMENTS_PER_PAGE + visibleIndex;
			if (index >= menu.requirementCount()) {
				break;
			}
			int x = 14 + (visibleIndex % 4) * 56;
			int y = 65 + (visibleIndex / 4) * 24;
			if (relativeX >= x && relativeX < x + 16 && relativeY >= y && relativeY < y + 16) {
				return index;
			}
		}
		return -1;
	}
}
