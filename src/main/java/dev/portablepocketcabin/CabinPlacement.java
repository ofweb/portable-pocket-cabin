package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class CabinPlacement {
	private static final long PREVIEW_LIFETIME_TICKS = 30L * 20L;
	private static final Map<UUID, Preview> PREVIEWS = new ConcurrentHashMap<>();

	private record Preview(
		Optional<UUID> cabinId,
		UUID itemInstanceId,
		CabinPalette palette,
		BlockPos supportBlock,
		CabinExterior exterior,
		long expiresAt
	) {
	}

	private record DeploymentItem(
		Optional<UUID> cabinId, UUID itemInstanceId, CabinPalette palette, int inventorySlot
	) {
	}

	private CabinPlacement() {
	}

	static int preview(ServerPlayer player) {
		DeploymentItem item = resolveAnyInventoryItem(player);
		if (item == null) {
			return 0;
		}
		CabinExterior exterior = ExteriorCabin.previewFor(player);
		BlockPos support = ExteriorCabin.frontStep(exterior).below();
		return previewAt(player, item, support, exterior);
	}

	static int deploy(ServerPlayer player) {
		Preview preview = PREVIEWS.remove(player.getUUID());
		if (preview == null) {
			player.sendSystemMessage(Component.literal("Preview a cabin site before deploying it."));
			return 0;
		}
		return deploy(player, preview);
	}

	static int previewOrDeploy(
		ServerPlayer player, BlockPos supportBlock, Direction clickedFace, ItemStack usedStack
	) {
		if (clickedFace != Direction.UP) {
			player.sendSystemMessage(Component.literal("Use the Cabin Kit on the top of a solid block."));
			return 0;
		}
		ServerLevel level = (ServerLevel) player.level();
		if (!level.getBlockState(supportBlock).isFaceSturdy(level, supportBlock, Direction.UP)) {
			player.sendSystemMessage(Component.literal("The Cabin Kit needs a solid top surface."));
			return 0;
		}

		DeploymentItem item = resolveUsedItem(player, usedStack);
		if (item == null) {
			return 0;
		}
		CabinExterior exterior = ExteriorCabin.previewFor(player, supportBlock);
		Preview previous = PREVIEWS.get(player.getUUID());
		if (previous != null && level.getGameTime() <= previous.expiresAt()
			&& previous.itemInstanceId().equals(item.itemInstanceId())
			&& previous.palette().equals(item.palette())
			&& previous.supportBlock().equals(supportBlock)
			&& previous.exterior().dimension().equals(level.dimension())) {
			PREVIEWS.remove(player.getUUID());
			return deploy(player, previous);
		}
		return previewAt(player, item, supportBlock, exterior);
	}

	static int previewOrDeploy(ServerPlayer player) {
		Preview preview = PREVIEWS.get(player.getUUID());
		if (preview != null && player.level().getGameTime() <= preview.expiresAt()) {
			return deploy(player);
		}
		return preview(player);
	}

	static void clearPreview(UUID playerId) {
		PREVIEWS.remove(playerId);
	}

	private static int previewAt(
		ServerPlayer player, DeploymentItem item, BlockPos supportBlock, CabinExterior exterior
	) {
		if (!ExteriorCabin.isSupportedDimension(player.level().dimension())) {
			player.sendSystemMessage(Component.literal("Cabins can only be deployed in the Overworld, Nether, or End."));
			return 0;
		}
		ServerLevel level = (ServerLevel) player.level();
		ExteriorCabin.PlacementCheck check = validate(level, exterior, player);
		ExteriorCabin.showPreview(level, exterior, check.valid());
		if (!check.valid()) {
			player.sendSystemMessage(Component.literal(check.message() + "."));
			return 0;
		}
		PREVIEWS.put(player.getUUID(), new Preview(
			item.cabinId(), item.itemInstanceId(), item.palette(), supportBlock, exterior,
			level.getGameTime() + PREVIEW_LIFETIME_TICKS
		));
		player.sendSystemMessage(Component.literal("Cabin footprint is clear at " + coordinates(exterior)
			+ ". Use the same top surface again within 30 seconds to deploy."));
		return 1;
	}

	private static int deploy(ServerPlayer player, Preview preview) {
		ServerLevel exteriorLevel = (ServerLevel) player.level();
		if (!player.level().dimension().equals(preview.exterior().dimension())
			|| exteriorLevel.getGameTime() > preview.expiresAt()) {
			player.sendSystemMessage(Component.literal("That cabin preview expired. Select the site again."));
			return 0;
		}

		CabinRegistry registry = CabinRegistry.get(exteriorLevel.getServer());
		CabinRecord cabin = registry.findByOwner(player.getUUID()).orElse(null);
		if (preview.cabinId().isPresent()
			&& (cabin == null || !preview.cabinId().get().equals(cabin.uuid()))
			|| preview.cabinId().isEmpty() && cabin != null) {
			player.sendSystemMessage(Component.literal("Cabin ownership changed. Select the site again."));
			return 0;
		}

		int itemSlot = CabinItems.findByInstance(player.getInventory(), preview.itemInstanceId());
		if (!itemMatchesPreview(player.getInventory(), itemSlot, preview, cabin)) {
			player.sendSystemMessage(Component.literal("The Cabin Kit used for this preview is no longer available."));
			return 0;
		}
		ExteriorCabin.PlacementCheck check = validate(exteriorLevel, preview.exterior(), player);
		if (!check.valid()) {
			ExteriorCabin.showPreview(exteriorLevel, preview.exterior(), false);
			player.sendSystemMessage(Component.literal(check.message() + ". Select the site again after clearing it."));
			return 0;
		}

		ServerLevel pocket = exteriorLevel.getServer().getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			player.sendSystemMessage(Component.literal("Pocket dimension is unavailable."));
			return 0;
		}

		UUID cabinId = cabin == null ? null : cabin.uuid();
		try {
			if (cabin == null) {
				cabin = registry.create(player.getUUID(), preview.palette());
				cabinId = cabin.uuid();
			}
			CabinRecord deploying = registry.beginDeployment(
				cabin.uuid(), player.getUUID(), preview.exterior(), preview.itemInstanceId()
			);
			CabinRegistry.flush(exteriorLevel.getServer());
			player.getInventory().setItem(itemSlot, CabinItems.createBound(
				deploying, deploying.packedItemGeneration(), true, preview.itemInstanceId()
			));
			if (!deploying.interiorGenerated()) {
				PocketDimension.ensureCabinInterior(
					pocket, deploying.cellIndex(), deploying.palette(), deploying.progression().generalSize()
				);
				registry.markInteriorGenerated(deploying.uuid());
				CabinRegistry.flush(exteriorLevel.getServer());
			}
			ExteriorCabin.place(exteriorLevel, preview.exterior(), deploying.palette());
			CabinRecord deployed = registry.finishDeployment(deploying.uuid());
			CabinRegistry.flush(exteriorLevel.getServer());
			CabinItems.removeByInstance(player.getInventory(), preview.itemInstanceId());
			CabinWindows.update(exteriorLevel.getServer(), deployed);
		} catch (IllegalStateException exception) {
			if (cabinId != null && registry.find(cabinId).isPresent()) {
				CabinReconciliation.reconcile(exteriorLevel.getServer(), cabinId);
				CabinReconciliation.reconcileOwnerInventory(player);
			}
			player.sendSystemMessage(Component.literal(exception.getMessage()));
			return 0;
		}

		player.sendSystemMessage(Component.literal(
			"Cabin deployed. Use its door or lodestone to enter. Sneak-use the lodestone twice to pack it."
		));
		return 1;
	}

	private static DeploymentItem resolveAnyInventoryItem(ServerPlayer player) {
		CabinRecord cabin = CabinRegistry.get(player.level().getServer()).findByOwner(player.getUUID()).orElse(null);
		int slot = cabin == null
			? CabinItems.findUnbound(player.getInventory())
			: CabinItems.findValid(player.getInventory(), cabin);
		if (slot < 0) {
			player.sendSystemMessage(Component.literal(cabin == null
				? "You need a crafted Cabin Kit to establish your first cabin."
				: "You need the current Packed Cabin item to deploy it."));
			return null;
		}
		return resolveUsedItem(player, player.getInventory().getItem(slot));
	}

	private static DeploymentItem resolveUsedItem(ServerPlayer player, ItemStack stack) {
		CabinRegistry registry = CabinRegistry.get(player.level().getServer());
		CabinRecord owned = registry.findByOwner(player.getUUID()).orElse(null);
		CabinItems.Unbound unbound = CabinItems.unbound(stack).orElse(null);
		if (unbound != null) {
			if (owned != null) {
				player.sendSystemMessage(Component.literal("You already own a cabin. Give this unused Kit to another player."));
				return null;
			}
			int slot = CabinItems.findUnbound(player.getInventory(), unbound.itemInstanceId(), unbound.palette());
			return new DeploymentItem(Optional.empty(), unbound.itemInstanceId(), unbound.palette(), slot);
		}

		CabinItems.Binding binding = CabinItems.binding(stack).orElse(null);
		if (binding == null || binding.pending()) {
			player.sendSystemMessage(Component.literal("That cabin item is incomplete or reserved by a transaction."));
			return null;
		}
		if (owned == null || !owned.uuid().equals(binding.cabinId())
			|| owned.lifecycle() != CabinLifecycle.PACKED
			|| owned.packedItemGeneration() != binding.generation()
			|| !owned.palette().equals(binding.palette())) {
			player.sendSystemMessage(Component.literal("That Packed Cabin is stale or does not belong to you."));
			return null;
		}
		return new DeploymentItem(
			Optional.of(owned.uuid()), binding.itemInstanceId(), binding.palette(),
			CabinItems.findByInstance(player.getInventory(), binding.itemInstanceId())
		);
	}

	private static boolean itemMatchesPreview(
		Inventory inventory, int slot, Preview preview, CabinRecord cabin
	) {
		if (slot < 0) {
			return false;
		}
		ItemStack stack = inventory.getItem(slot);
		if (cabin == null) {
			CabinItems.Unbound unbound = CabinItems.unbound(stack).orElse(null);
			return unbound != null && unbound.itemInstanceId().equals(preview.itemInstanceId())
				&& unbound.palette().equals(preview.palette());
		}
		CabinItems.Binding binding = CabinItems.binding(stack).orElse(null);
		return binding != null && !binding.pending() && binding.itemInstanceId().equals(preview.itemInstanceId())
			&& binding.cabinId().equals(cabin.uuid())
			&& binding.generation() == cabin.packedItemGeneration()
			&& binding.palette().equals(cabin.palette());
	}

	private static String coordinates(CabinExterior exterior) {
		return ExteriorCabin.frontStep(exterior).getX() + ","
			+ ExteriorCabin.frontStep(exterior).getY() + ","
			+ ExteriorCabin.frontStep(exterior).getZ()
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
