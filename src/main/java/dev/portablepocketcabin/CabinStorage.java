package dev.portablepocketcabin;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

final class CabinStorage {
	private static final String DELIVERY_TAG = PortablePocketCabin.MOD_ID + ".storage_delivery";
	private CabinStorage() { }
	static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof net.minecraft.world.entity.item.ItemEntity item) || !item.entityTags().contains(DELIVERY_TAG)) return;
			boolean pending = CabinRegistry.get(level.getServer()).cabins().stream()
				.flatMap(cabin -> cabin.upgrades().storage().sessions().stream())
				.anyMatch(session -> session.operation().equals(item.getUUID()));
			if (pending) hold(item);
			else release(item);
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var cabin : CabinRegistry.get(server).cabins()) for (var session : cabin.upgrades().storage().sessions()) {
				var level = server.getLevel(session.delivery().dimension());
				if (level == null) continue;
				var entity = level.getEntity(session.operation());
				if (!(entity instanceof net.minecraft.world.entity.item.ItemEntity item) || !item.entityTags().contains(DELIVERY_TAG)) continue;
				var position = session.delivery().position();
				item.setPos(position.getX() + .5, position.getY() + .5, position.getZ() + .5);
				item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
				hold(item);
			}
		});
	}
	private static void hold(net.minecraft.world.entity.item.ItemEntity item) {
		item.addTag(DELIVERY_TAG);
		item.setNeverPickUp();
		item.setUnlimitedLifetime();
		item.setNoGravity(true);
		item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
	}
	private static void release(net.minecraft.world.entity.item.ItemEntity item) {
		item.removeTag(DELIVERY_TAG);
		item.setNoGravity(false);
		item.setDefaultPickUpDelay();
		item.setExtendedLifetime();
	}
	static BlockPos control(CabinRecord cabin) {
		return control(cabin.cellIndex(), cabin.progression().generalSize());
	}
	static BlockPos control(long cell, int size) {
		BlockPos lodestone = PocketDimension.interiorController(cell, size);
		BlockPos center = PocketDimension.cellCenter(cell);
		return new BlockPos(center.getX(), lodestone.getY(),
			center.getZ() * 2 - lodestone.getZ());
	}
	static void placeControl(ServerLevel pocket, CabinRecord cabin, int size) {
		pocket.setBlockAndUpdate(control(cabin.cellIndex(), size), Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(
			net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH));
	}
	static boolean mayUse(CabinRecord cabin, UUID actor) {
		return cabin != null && cabin.lifecycle() == CabinLifecycle.DEPLOYED && cabin.interiorGenerated()
			&& (cabin.owner().equals(actor) || cabin.trustedPlayers().contains(actor) && cabin.canEnter(actor));
	}
	static CabinStorageState.Session receipt(ServerPlayer player, ItemStack cursor) {
		var inventory = new java.util.ArrayList<ItemStack>();
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) inventory.add(player.getInventory().getItem(i).copy());
		return new CabinStorageState.Session(player.getUUID(), inventory, cursor, UUID.randomUUID(),
			new CabinStorageState.Delivery(player.level().dimension(), player.blockPosition()));
	}
	static void recover(ServerPlayer player) {
		var registry = CabinRegistry.get(player.level().getServer());
		synchronized (registry) {
			for (var cabin : registry.cabins()) {
				var session = cabin.upgrades().storage().sessions().stream()
					.filter(value -> value.player().equals(player.getUUID())).findFirst().orElse(null);
				if (session == null) continue;
				var items = session.inventory();
				if (items.size() != player.getInventory().getContainerSize())
					throw new IllegalStateException("Storage recovery inventory size changed");
				for (int i = 0; i < items.size(); i++) player.getInventory().setItem(i, items.get(i));
				ItemStack cursor = session.cursor();
				if (!cursor.isEmpty()) {
					returnToInventory(player.getInventory(), cursor);
					if (!cursor.isEmpty()) {
						var delivery = session.delivery();
						var level = player.level().getServer().getLevel(delivery.dimension());
						if (level == null) throw new IllegalStateException("Storage recovery dimension is unavailable");
						level.getChunkAt(delivery.position());
						var existing = level.getEntity(session.operation());
						if (existing == null) {
							var position = delivery.position();
							var drop = new net.minecraft.world.entity.item.ItemEntity(level, position.getX() + .5,
								position.getY() + .5, position.getZ() + .5, cursor.copy());
							drop.setUUID(session.operation());
							hold(drop);
							if (!level.addFreshEntity(drop)) throw new IllegalStateException("Storage recovery delivery failed");
						} else if (!(existing instanceof net.minecraft.world.entity.item.ItemEntity drop)
							|| !ItemStack.matches(drop.getItem(), cursor)) {
							throw new IllegalStateException("Storage recovery delivery changed");
						}
						var pendingDrop = (net.minecraft.world.entity.item.ItemEntity) level.getEntity(session.operation());
						hold(pendingDrop);
						player.level().getServer().saveAllChunks(true, true, true);
					}
				}
				player.getInventory().setChanged();
				player.level().getServer().getPlayerList().saveAll();
				registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(
					cabin.upgrades().storage().withoutSession(player.getUUID())));
				CabinRegistry.flush(player.level().getServer());
				var level = player.level().getServer().getLevel(session.delivery().dimension());
				if (level != null && level.getEntity(session.operation()) instanceof net.minecraft.world.entity.item.ItemEntity item)
					release(item);
			}
		}
	}

	private static void returnToInventory(Inventory inventory, ItemStack cursor) {
		for (int i = 0; i < 37; i++) {
			ItemStack stack = inventory.getItem(i == 36 ? Inventory.SLOT_OFFHAND : i);
			if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, cursor)) {
				int count = Math.min(cursor.getCount(), stack.getMaxStackSize() - stack.getCount());
				stack.grow(count);
				cursor.shrink(count);
			}
		}
		for (int i = 0; i < 36 && !cursor.isEmpty(); i++) {
			if (!inventory.getItem(i).isEmpty()) continue;
			int count = Math.min(cursor.getCount(), cursor.getMaxStackSize());
			inventory.setItem(i, cursor.copyWithCount(count));
			cursor.shrink(count);
		}
	}

	static InteractionResult open(ServerPlayer player, CabinRecord cabin) {
		if (!mayUse(cabin, player.getUUID())) {
			player.sendSystemMessage(Component.literal("Only the cabin owner and residents may use storage."));
			return InteractionResult.FAIL;
		}
		return player.openMenu(new ExtendedMenuProvider<UUID>() {
			public UUID getScreenOpeningData(ServerPlayer player) { return cabin.uuid(); }
			public Component getDisplayName() { return Component.literal("Central Storage"); }
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
				return new CabinStorageMenu(id, inventory, cabin.uuid());
			}
		}).isPresent() ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
	}
}
