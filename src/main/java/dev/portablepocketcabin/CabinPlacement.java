package dev.portablepocketcabin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class CabinPlacement {
	private static final long PREVIEW_LIFETIME_TICKS = 30L * 20L;
	private static final Map<UUID, Preview> PREVIEWS = new ConcurrentHashMap<>();

	private record Preview(UUID cabinId, CabinExterior exterior, long expiresAt) {
	}

	private CabinPlacement() {
	}

	static int preview(ServerPlayer player) {
		if (!player.level().dimension().equals(Level.OVERWORLD)) {
			player.sendSystemMessage(Component.literal("Delivery 3 cabins can only be deployed in the Overworld."));
			return 0;
		}

		ServerLevel level = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(level.getServer());
		CabinRecord cabin = registry.findByOwner(player.getUUID()).orElse(null);
		if (cabin == null) {
			player.sendSystemMessage(Component.literal("You do not own a cabin. Use /cabin create first."));
			return 0;
		}
		if (cabin.lifecycle() != CabinLifecycle.PACKED) {
			player.sendSystemMessage(Component.literal("Your cabin cannot be previewed while it is "
				+ cabin.lifecycle() + "."));
			return 0;
		}

		CabinExterior exterior = ExteriorCabin.previewFor(player);
		ExteriorCabin.PlacementCheck check = ExteriorCabin.validate(level, exterior);
		ExteriorCabin.showPreview(level, exterior, check.valid());
		PREVIEWS.put(player.getUUID(), new Preview(
			cabin.uuid(), exterior, level.getGameTime() + PREVIEW_LIFETIME_TICKS
		));

		player.sendSystemMessage(Component.literal(check.message() + " at " + coordinates(exterior)
			+ (check.valid() ? ". Run /cabin deploy within 30 seconds to confirm." : ".")));
		return check.valid() ? 1 : 0;
	}

	static int deploy(ServerPlayer player) {
		ServerLevel exteriorLevel = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(exteriorLevel.getServer());
		CabinRecord cabin = registry.findByOwner(player.getUUID()).orElse(null);
		if (cabin == null) {
			player.sendSystemMessage(Component.literal("You do not own a cabin. Use /cabin create first."));
			return 0;
		}

		Preview preview = PREVIEWS.remove(player.getUUID());
		if (preview == null || !preview.cabinId().equals(cabin.uuid())) {
			player.sendSystemMessage(Component.literal("Run /cabin preview before deploying your cabin."));
			return 0;
		}
		if (!player.level().dimension().equals(preview.exterior().dimension())
			|| player.level().getGameTime() > preview.expiresAt()) {
			player.sendSystemMessage(Component.literal("That cabin preview expired. Run /cabin preview again."));
			return 0;
		}

		ExteriorCabin.PlacementCheck check = ExteriorCabin.validate(exteriorLevel, preview.exterior());
		if (!check.valid()) {
			ExteriorCabin.showPreview(exteriorLevel, preview.exterior(), false);
			player.sendSystemMessage(Component.literal(check.message() + ". Preview the cabin again after clearing it."));
			return 0;
		}

		ServerLevel pocket = exteriorLevel.getServer().getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			player.sendSystemMessage(Component.literal("Pocket dimension is unavailable."));
			return 0;
		}

		try {
			CabinRecord deploying = registry.beginDeployment(cabin.uuid(), player.getUUID(), preview.exterior());
			if (!deploying.interiorGenerated()) {
				PocketDimension.ensureCabinInterior(pocket, deploying.cellIndex());
				registry.markInteriorGenerated(deploying.uuid());
			}
			ExteriorCabin.place(exteriorLevel, preview.exterior());
			registry.finishDeployment(deploying.uuid());
		} catch (IllegalStateException exception) {
			player.sendSystemMessage(Component.literal(exception.getMessage()));
			return 0;
		}

		player.sendSystemMessage(Component.literal("Cabin deployed. Interact with its iron door or lodestone controller to enter."));
		return 1;
	}

	private static String coordinates(CabinExterior exterior) {
		return exterior.anchor().getX() + "," + exterior.anchor().getY() + "," + exterior.anchor().getZ()
			+ " facing " + exterior.facing().getSerializedName();
	}
}
