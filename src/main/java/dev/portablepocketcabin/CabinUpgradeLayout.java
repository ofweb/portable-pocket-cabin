package dev.portablepocketcabin;

/** Client-neutral geometry contract for the compact cabin-upgrade container. */
final class CabinUpgradeLayout {
	static final int SCREEN_WIDTH = 248;
	static final int SCREEN_HEIGHT = 220;
	static final int PANEL_X = 38;
	static final int PANEL_Y = 20;
	static final int PANEL_WIDTH = 202;
	static final int PANEL_HEIGHT = 96;
	static final int REQUIREMENT_COLUMNS = 8;
	static final int REQUIREMENT_X = 55;
	static final int REQUIREMENT_Y = 50;
	static final int REQUIREMENT_X_STEP = 22;
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
