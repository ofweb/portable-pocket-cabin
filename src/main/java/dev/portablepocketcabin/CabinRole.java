package dev.portablepocketcabin;

enum CabinRole {
	OWNER("owner"),
	RESIDENT("resident"),
	GUEST("guest");

	private final String displayName;

	CabinRole(String displayName) {
		this.displayName = displayName;
	}

	String displayName() {
		return displayName;
	}
}
