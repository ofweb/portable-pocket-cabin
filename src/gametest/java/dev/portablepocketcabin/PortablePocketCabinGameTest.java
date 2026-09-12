package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;

public final class PortablePocketCabinGameTest {
	@GameTest
	public void pocketDimensionTypeIsRegistered(GameTestHelper helper) {
		var dimensionTypes = helper.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);

		helper.assertTrue(dimensionTypes.containsKey(PocketDimension.TYPE_KEY),
			"Pocket dimension type must be registered");
		helper.assertTrue(
			PocketDimension.LEVEL_KEY.identifier().equals(PortablePocketCabin.id("pocket_home")),
			"Pocket dimension key must remain stable for save compatibility"
		);
		helper.succeed();
	}
}
