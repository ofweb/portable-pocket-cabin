package dev.portablepocketcabin;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.UUID;

/** Opens the protected, server-authoritative upgrade interface from the interior controller. */
final class CabinUpgrades {
	private CabinUpgrades() {
	}

	static InteractionResult useController(ServerPlayer player, CabinRecord observed) {
		return useController(player, observed, CabinRegistry.get(player.level().getServer()));
	}

	static InteractionResult useController(
		ServerPlayer player, CabinRecord observed, CabinRegistry registry
	) {
		CabinRecord cabin = registry.find(observed.uuid()).orElse(null);
		if (cabin == null || cabin.lifecycle() != CabinLifecycle.DEPLOYED || !cabin.interiorGenerated()) {
			player.sendSystemMessage(Component.literal("That cabin controller is not active."));
			return InteractionResult.FAIL;
		}
		if (!cabin.canEnter(player.getUUID())) {
			player.sendSystemMessage(Component.literal("You do not have permission to inspect this cabin."));
			return InteractionResult.FAIL;
		}
		PocketDimension.removeLegacyDebugPlatformResidue(
			player.level(), cabin.cellIndex(), cabin.progression().generalSize()
		);
		PocketDimension.upgradeLegacyCornerFrames(player.level(), cabin);

		UUID cabinId = cabin.uuid();
		return player.openMenu(new ExtendedMenuProvider<UUID>() {
			@Override
			public UUID getScreenOpeningData(ServerPlayer serverPlayer) {
				return cabinId;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("container.portable_pocket_cabin.cabin_upgrades");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
				return new CabinUpgradeMenu(containerId, inventory, cabinId);
			}
		}).isPresent() ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
	}
}
