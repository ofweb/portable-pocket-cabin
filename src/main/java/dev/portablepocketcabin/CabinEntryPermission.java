package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

enum CabinEntryPermission {
	OWNER_ONLY("owner_only"),
	TRUSTED_PLAYERS("trusted");

	static final Codec<CabinEntryPermission> CODEC = Codec.STRING.comapFlatMap(
		value -> {
			for (CabinEntryPermission permission : values()) {
				if (permission.serializedName.equals(value)) {
					return DataResult.success(permission);
				}
			}
			return DataResult.error(() -> "Unknown cabin entry permission: " + value);
		},
		CabinEntryPermission::serializedName
	);

	private final String serializedName;

	CabinEntryPermission(String serializedName) {
		this.serializedName = serializedName;
	}

	String serializedName() {
		return serializedName;
	}
}
