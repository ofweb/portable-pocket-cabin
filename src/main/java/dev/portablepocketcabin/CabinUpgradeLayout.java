package dev.portablepocketcabin;

/** Client-neutral geometry contract for the compact cabin-upgrade container. */
final class CabinUpgradeLayout {
	static final int SCREEN_WIDTH = 248;
	static final int SCREEN_HEIGHT = 248;
	static final int PANEL_ICON_X = 216;
	static final int PANEL_ICON_Y = 27;
	static final int PANEL_TITLE_X = 12;
	static final int PANEL_TITLE_Y = 28;
	static final int PANEL_TITLE_WIDTH = 156;
	static final int PANEL_PAGE_X = 178;
	static final int PANEL_PAGE_Y = 7;
	static final int PANEL_PAGE_WIDTH = 32;
	static final int STATUS_X = 12;
	static final int STATUS_Y = 116;
	static final int STATUS_WIDTH = 224;
	static final int EFFECT_X = 12;
	static final int EFFECT_Y = 43;
	static final int EFFECT_WIDTH = 224;
	static final int REMOVE_X = 66;
	static final int DOWNGRADE_X = 8;
	static final int INSTALL_X = 182;
	static final int ACTION_Y = 128;
	static final int ACTION_WIDTH = 58;
	static final int ACTION_HEIGHT = 20;
	static final int REQUIREMENT_COLUMNS = 8;
	static final int REQUIREMENT_X = 12;
	static final int REQUIREMENT_Y = 54;
	static final int REQUIREMENT_X_STEP = 28;
	static final int REQUIREMENT_Y_STEP = 30;
	static final int TAB_X = -23;
	static final int TAB_Y = 20;
	static final int TAB_SIZE = 22;
	static final int TAB_STEP = 23;
	static final int PANEL_NAV_X = 154;
	static final int PANEL_NAV_Y = 4;
	static final int NEXT_PANEL_X = 218;
	static final int PANEL_NAV_WIDTH = 18;
	static final int PANEL_NAV_HEIGHT = 16;
	static final int RESULT_ARROW_X = 182;
	static final int RESULT_ARROW_Y = 27;
	static final int INVENTORY_BACKGROUND_Y = 152;
	static final int INVENTORY_X = 44;
	static final int INVENTORY_Y = 166;
	static final int INVENTORY_LABEL_Y = 154;

	private CabinUpgradeLayout() {
	}

	static int requirementX(int index) {
		return REQUIREMENT_X + index % REQUIREMENT_COLUMNS * REQUIREMENT_X_STEP;
	}

	static int requirementY(int index) {
		return REQUIREMENT_Y + index / REQUIREMENT_COLUMNS * REQUIREMENT_Y_STEP;
	}

	static int tabY(int index) {
		return TAB_Y + index * TAB_STEP;
	}
}
