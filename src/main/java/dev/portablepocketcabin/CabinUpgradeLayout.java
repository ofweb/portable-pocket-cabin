package dev.portablepocketcabin;

/** Client-neutral geometry contract for the compact cabin-upgrade container. */
final class CabinUpgradeLayout {
	static final int SCREEN_WIDTH = 248;
	static final int SCREEN_HEIGHT = 220;
	static final int PANEL_X = 4;
	static final int PANEL_Y = 20;
	static final int PANEL_WIDTH = 240;
	static final int PANEL_HEIGHT = 100;
	static final int PANEL_ICON_X = 12;
	static final int PANEL_ICON_Y = 25;
	static final int PANEL_TITLE_X = 32;
	static final int PANEL_TITLE_Y = 28;
	static final int PANEL_TITLE_WIDTH = 76;
	static final int PANEL_PAGE_X = 112;
	static final int PANEL_PAGE_Y = 28;
	static final int PANEL_PAGE_WIDTH = 29;
	static final int STATUS_X = 145;
	static final int STATUS_Y = 25;
	static final int EFFECT_X = 13;
	static final int EFFECT_Y = 44;
	static final int EFFECT_WIDTH = 222;
	static final int REMOVE_X = 168;
	static final int DOWNGRADE_X = 190;
	static final int INSTALL_X = 212;
	static final int ACTION_Y = 23;
	static final int ACTION_WIDTH = 19;
	static final int ACTION_HEIGHT = 18;
	static final int REQUIREMENT_COLUMNS = 8;
	static final int REQUIREMENT_X = 12;
	static final int REQUIREMENT_Y = 56;
	static final int REQUIREMENT_X_STEP = 28;
	static final int REQUIREMENT_Y_STEP = 30;
	static final int TAB_X = -21;
	static final int TAB_Y = 20;
	static final int TAB_SIZE = 22;
	static final int TAB_STEP = 23;
	static final int PANEL_NAV_X = SCREEN_WIDTH - 1;
	static final int PANEL_NAV_Y = PANEL_Y;

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
