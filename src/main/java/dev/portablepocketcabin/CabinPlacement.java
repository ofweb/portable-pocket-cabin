package dev.portablepocketcabin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class CabinPlacement {
	private static final long PREVIEW_LIFETIME_TICKS = 30L * 20L;
	private static final Map<UUID, Preview> PREVIEWS = new ConcurrentHashMap<>();

	private record Preview(Optional<UUID> cabinId, CabinExterior exterior, long expiresAt) {
	}

	private CabinPlacement() {
	}

	static int preview(ServerPlayer player) {
		if (!ExteriorCabin.isSupportedDimension(player.level().dimension())) {
			player.sendSystemMessage(Component.literal("Cabins can only be deployed in the Overworld, Nether, or End."));
			return 0;
		}

		ServerLevel level = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(level.getServer());
		CabinRecord cabin = registry.findByOwner(player.getUUID()).orElse(null);
		if (cabin != null && cabin.lifecycle() != CabinLifecycle.PACKED) {
			player.sendSystemMessage(Component.literal("Your cabin cannot be previewed while it is "
				+ cabin.lifecycle() + "."));
			return 0;
		}
		if (cabin != null && CabinItems.findValid(player.getInventory(), cabin) < 0) {
			player.sendSystemMessage(Component.literal("You need the current bound packed-cabin item to deploy it."));
			return 0;
		}
		if (cabin == null && CabinItems.findUnbound(player.getInventory()) < 0) {
			player.sendSystemMessage(Component.literal("You need an unbound cabin kit to establish your first cabin."));
			return 0;
		}

		CabinExterior exterior = ExteriorCabin.previewFor(player);
		ExteriorCabin.PlacementCheck check = validate(level, exterior, player);
		ExteriorCabin.showPreview(level, exterior, check.valid());
		PREVIEWS.put(player.getUUID(), new Preview(
			cabin == null ? Optional.empty() : Optional.of(cabin.uuid()),
			exterior, level.getGameTime() + PREVIEW_LIFETIME_TICKS
		));

		player.sendSystemMessage(Component.literal(check.message() + " at " + coordinates(exterior)
			+ (check.valid() ? ". Run /cabin deploy within 30 seconds to confirm." : ".")));
		return check.valid() ? 1 : 0;
	}

	static int deploy(ServerPlayer player) {
		ServerLevel exteriorLevel = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(exteriorLevel.getServer());
		CabinRecord cabin = registry.findByOwner(player.getUUID()).orElse(null);

		Preview preview = PREVIEWS.remove(player.getUUID());
		if (preview == null || preview.cabinId().isPresent()
			&& (cabin == null || !preview.cabinId().get().equals(cabin.uuid()))) {
			player.sendSystemMessage(Component.literal("Run /cabin preview before deploying your cabin."));
			return 0;
		}
		if (preview.cabinId().isEmpty() && cabin != null) {
			player.sendSystemMessage(Component.literal("Cabin ownership changed. Preview the cabin again."));
			return 0;
		}
		if (!player.level().dimension().equals(preview.exterior().dimension())
			|| player.level().getGameTime() > preview.expiresAt()) {
			player.sendSystemMessage(Component.literal("That cabin preview expired. Run /cabin preview again."));
			return 0;
		}

		ExteriorCabin.PlacementCheck check = validate(exteriorLevel, preview.exterior(), player);
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
		int packedItemSlot = cabin == null
			? CabinItems.findUnbound(player.getInventory())
			: CabinItems.findValid(player.getInventory(), cabin);
		if (packedItemSlot < 0) {
			player.sendSystemMessage(Component.literal("The cabin item used for this preview is no longer in your inventory."));
			return 0;
		}

		try {
			if (cabin == null) {
				cabin = registry.create(player.getUUID());
			}
			CabinRecord deploying = registry.beginDeployment(cabin.uuid(), player.getUUID(), preview.exterior());
			CabinRegistry.flush(exteriorLevel.getServer());
			player.getInventory().setItem(packedItemSlot, net.minecraft.world.item.ItemStack.EMPTY);
			if (!deploying.interiorGenerated()) {
				PocketDimension.ensureCabinInterior(pocket, deploying.cellIndex());
				registry.markInteriorGenerated(deploying.uuid());
				CabinRegistry.flush(exteriorLevel.getServer());
			}
			ExteriorCabin.place(exteriorLevel, preview.exterior());
			CabinRecord deployed = registry.finishDeployment(deploying.uuid());
			CabinWindows.update(exteriorLevel.getServer(), deployed);
		} catch (IllegalStateException exception) {
			player.sendSystemMessage(Component.literal(exception.getMessage()));
			return 0;
		}

		player.sendSystemMessage(Component.literal("Cabin deployed. Interact with its iron door or lodestone controller to enter."));
		return 1;
	}

	static int previewOrDeploy(ServerPlayer player) {
		Preview preview = PREVIEWS.get(player.getUUID());
		if (preview != null
			&& preview.exterior().dimension().equals(player.level().dimension())
			&& player.level().getGameTime() <= preview.expiresAt()) {
			return deploy(player);
		}
		return preview(player);
	}

	private static String coordinates(CabinExterior exterior) {
		return exterior.anchor().getX() + "," + exterior.anchor().getY() + "," + exterior.anchor().getZ()
			+ " facing " + exterior.facing().getSerializedName();
	}

	private static ExteriorCabin.PlacementCheck validate(
		ServerLevel level, CabinExterior exterior, ServerPlayer player
	) {
		ExteriorCabin.PlacementCheck structure = ExteriorCabin.validate(level, exterior);
		if (!structure.valid()) {
			return structure;
		}
		if (SafeDestinationResolver.resolveExact(
			level, ExteriorCabin.outsideDestination(exterior), player
		).isEmpty()) {
			return new ExteriorCabin.PlacementCheck(false,
				"The shared safe-destination resolver rejected the exterior doorway");
		}
		return structure;
	}
}
