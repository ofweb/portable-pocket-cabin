package dev.portablepocketcabin;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.UUID;

/** Synchronized, non-storage menu for one cabin's tracked upgrade. */
final class CabinUpgradeMenu extends AbstractContainerMenu {
	static final int BUTTON_TRACK = 1;
	static final int BUTTON_STOP_TRACKING = 2;
	static final int BUTTON_INSTALL = 3;
	static final int MAX_REQUIREMENTS = 64;
	static final int DEPOSIT_SLOT = 0;
	static final int FIRST_REQUIREMENT_SLOT = 1;
	static final int ATTUNED_SLOT = FIRST_REQUIREMENT_SLOT + MAX_REQUIREMENTS;
	static final int STATUS_SLOT = ATTUNED_SLOT + 1;
	static final int FIRST_PLAYER_SLOT = STATUS_SLOT + 1;

	private static final int DATA_CURRENT_SIZE = 0;
	private static final int DATA_MAXIMUM_SIZE = 1;
	private static final int DATA_TARGET_SIZE = 2;
	private static final int DATA_FLAGS = 3;
	private static final int DATA_REQUIREMENT_COUNT = 4;
	private static final int DATA_REQUIREMENTS = 5;
	private static final int DATA_COUNT = DATA_REQUIREMENTS + MAX_REQUIREMENTS * 2;

	private static final int FLAG_OWNER = 1;
	private static final int FLAG_CONTRIBUTOR = 1 << 1;
	private static final int FLAG_TRACKED = 1 << 2;
	private static final int FLAG_STALE = 1 << 3;
	private static final int FLAG_COMPLETE = 1 << 4;
	private static final int FLAG_ACTIVE = 1 << 5;
	private static final int FLAG_MAXIMUM = 1 << 6;
	private static final int FLAG_INSTALLING = 1 << 7;
	private static final int FLAG_BLOCKED = 1 << 8;

	static final MenuType<CabinUpgradeMenu> TYPE = Registry.register(
		BuiltInRegistries.MENU,
		PortablePocketCabin.id("cabin_upgrades"),
		new ExtendedMenuType<>(CabinUpgradeMenu::new, UUIDUtil.STREAM_CODEC)
	);

	private final UUID cabinId;
	private final SimpleContainer display = new SimpleContainer(FIRST_PLAYER_SLOT);
	private final SimpleContainerData data = new SimpleContainerData(DATA_COUNT);
	private final ServerPlayer serverPlayer;
	private String actionMessage = "";

	CabinUpgradeMenu(int containerId, Inventory inventory, UUID cabinId) {
		super(TYPE, containerId);
		this.cabinId = cabinId;
		this.serverPlayer = inventory.player instanceof ServerPlayer player ? player : null;

		addSlot(new DisplaySlot(display, DEPOSIT_SLOT, 14, 112));
		for (int index = 0; index < MAX_REQUIREMENTS; index++) {
			addSlot(new DisplaySlot(display, FIRST_REQUIREMENT_SLOT + index, -1000, -1000));
		}
		addSlot(new DisplaySlot(display, ATTUNED_SLOT, -1000, -1000));
		addSlot(new DisplaySlot(display, STATUS_SLOT, -1000, -1000));
		addStandardInventorySlots(inventory, 44, 192);
		addDataSlots(data);
		if (serverPlayer != null) {
			refreshFromServer();
		}
	}

	static void register() {
		// Loading the class registers TYPE.
	}

	UUID cabinId() {
		return cabinId;
	}

	int currentSize() {
		return data.get(DATA_CURRENT_SIZE);
	}

	int maximumSize() {
		return data.get(DATA_MAXIMUM_SIZE);
	}

	int targetSize() {
		return data.get(DATA_TARGET_SIZE);
	}

	int requirementCount() {
		return data.get(DATA_REQUIREMENT_COUNT);
	}

	int requiredCount(int index) {
		return data.get(DATA_REQUIREMENTS + index * 2);
	}

	int fundedCount(int index) {
		return data.get(DATA_REQUIREMENTS + index * 2 + 1);
	}

	ItemStack requirementStack(int index) {
		return display.getItem(FIRST_REQUIREMENT_SLOT + index);
	}

	ItemStack attunedStack() {
		return display.getItem(ATTUNED_SLOT);
	}

	Component statusMessage() {
		ItemStack stack = display.getItem(STATUS_SLOT);
		return stack.isEmpty() ? Component.empty() : stack.getHoverName();
	}

	boolean isOwner() {
		return flag(FLAG_OWNER);
	}

	boolean canContribute() {
		return flag(FLAG_CONTRIBUTOR) && flag(FLAG_TRACKED) && !flag(FLAG_STALE)
			&& !flag(FLAG_INSTALLING);
	}

	boolean isTracked() {
		return flag(FLAG_TRACKED);
	}

	boolean isStale() {
		return flag(FLAG_STALE);
	}

	boolean isComplete() {
		return flag(FLAG_COMPLETE);
	}

	boolean isAtMaximum() {
		return flag(FLAG_MAXIMUM);
	}

	boolean installationInProgress() {
		return flag(FLAG_INSTALLING);
	}

	boolean isBlocked() {
		return flag(FLAG_BLOCKED);
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (!(player instanceof ServerPlayer actor) || actor != serverPlayer || !stillValid(player)) {
			return false;
		}
		CabinRegistry registry = CabinRegistry.get(actor.level().getServer());
		CabinUpgradeService.Outcome outcome;
		if (button == BUTTON_STOP_TRACKING) {
			outcome = CabinUpgradeService.stopTracking(
				registry, cabinId, actor.getUUID(),
				(cabin, stacks) -> CabinFundEjection.eject(actor.level(), cabin, stacks)
			);
		} else {
			CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
			WorldAttunement attunement;
			try {
				attunement = CabinUpgradeCatalog.resolveAttunement(registry, actor.level(), definitions);
			} catch (IllegalStateException exception) {
				setActionMessage(exception.getMessage());
				return false;
			}
			if (button == BUTTON_TRACK) {
				CabinRecord cabin = registry.find(cabinId).orElse(null);
				CabinUpgradeCatalog.Offer offer = cabin == null ? null
					: CabinUpgradeCatalog.next(cabin, attunement, definitions).orElse(null);
				outcome = offer == null
					? CabinUpgradeService.Outcome.failure("No general-space upgrade is currently available.")
					: CabinUpgradeService.track(
						registry, cabinId, actor.getUUID(), offer.target(), attunement, definitions
					);
			} else if (button == BUTTON_INSTALL) {
				outcome = CabinUpgradeService.install(
					registry, cabinId, actor.getUUID(), attunement, definitions,
					new CabinGeneralSpaceEffect(actor.level().getServer(), actor.level()),
					() -> CabinRegistry.flush(actor.level().getServer())
				);
			} else {
				return false;
			}
		}
		if ((button == BUTTON_TRACK || button == BUTTON_STOP_TRACKING) && outcome.success()) {
			CabinRegistry.flush(actor.level().getServer());
		}
		setActionMessage(outcome.message());
		actor.sendSystemMessage(Component.literal(outcome.message()));
		refreshFromServer();
		broadcastChanges();
		return outcome.success();
	}

	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (slotId == DEPOSIT_SLOT) {
			if (input == ContainerInput.PICKUP && serverPlayer != null && player == serverPlayer) {
				contribute(getCarried());
			}
			return;
		}
		super.clicked(slotId, button, input, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotId) {
		if (serverPlayer == null || player != serverPlayer || slotId < FIRST_PLAYER_SLOT
			|| slotId >= slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(slotId);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack original = slot.getItem().copy();
		int accepted = contribute(slot.getItem());
		if (accepted <= 0) {
			return ItemStack.EMPTY;
		}
		slot.setChanged();
		return original;
	}

	@Override
	public void broadcastChanges() {
		if (serverPlayer != null) {
			refreshFromServer();
		}
		super.broadcastChanges();
	}

	@Override
	public boolean stillValid(Player player) {
		if (serverPlayer == null) {
			return true;
		}
		if (player != serverPlayer || !player.level().dimension().equals(PocketDimension.LEVEL_KEY)) {
			return false;
		}
		CabinRecord cabin = CabinRegistry.get(serverPlayer.level().getServer()).find(cabinId).orElse(null);
		if (cabin == null || cabin.lifecycle() != CabinLifecycle.DEPLOYED || !cabin.interiorGenerated()
			|| !cabin.canEnter(player.getUUID())) {
			return false;
		}
		var controller = PocketDimension.interiorController(cabin.cellIndex());
		return player.distanceToSqr(
			controller.getX() + 0.5, controller.getY() + 0.5, controller.getZ() + 0.5
		) <= 64.0 && serverPlayer.level().getBlockState(controller).is(Blocks.LODESTONE);
	}

	private int contribute(ItemStack offered) {
		if (offered.isEmpty() || !stillValid(serverPlayer)) {
			return 0;
		}
		CabinRegistry registry = CabinRegistry.get(serverPlayer.level().getServer());
		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		try {
			WorldAttunement attunement = CabinUpgradeCatalog.resolveAttunement(
				registry, serverPlayer.level(), definitions
			);
			CabinUpgradeService.Contribution result = CabinUpgradeService.contribute(
				registry, cabinId, serverPlayer.getUUID(), offered, attunement, definitions
			);
			setActionMessage(result.message());
			if (result.success()) {
				CabinRegistry.flush(serverPlayer.level().getServer());
			}
			refreshFromServer();
			broadcastChanges();
			return result.accepted();
		} catch (IllegalStateException exception) {
			setActionMessage(exception.getMessage());
			return 0;
		}
	}

	private void refreshFromServer() {
		CabinRegistry registry = CabinRegistry.get(serverPlayer.level().getServer());
		CabinRecord cabin = registry.find(cabinId).orElse(null);
		String contextualMessage = "";
		clearDisplay();
		if (cabin == null) {
			setFlagData(0);
			writeStatus("That cabin no longer exists.");
			return;
		}

		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		data.set(DATA_CURRENT_SIZE, cabin.progression().generalSize());
		data.set(DATA_MAXIMUM_SIZE, definitions.maximumGeneralSize());
		int flags = FLAG_ACTIVE;
		if (cabin.owner().equals(serverPlayer.getUUID())) {
			flags |= FLAG_OWNER | FLAG_CONTRIBUTOR;
		} else if (cabin.trustedPlayers().contains(serverPlayer.getUUID()) && cabin.canEnter(serverPlayer.getUUID())) {
			flags |= FLAG_CONTRIBUTOR;
		}
		CabinUpgradeState.TrackedUpgrade tracked = cabin.upgrades().tracked().orElse(null);
		registry.worldAttunement()
			.flatMap(value -> CabinMaterialProfiles.woodProfile(value.woodProfile()))
			.ifPresent(profile -> display.setItem(ATTUNED_SLOT, new ItemStack(profile.planksIngredient())));

		try {
			WorldAttunement attunement = CabinUpgradeCatalog.resolveAttunement(
				registry, serverPlayer.level(), definitions
			);
			CabinMaterialProfiles.woodProfile(attunement.woodProfile()).ifPresent(profile ->
				display.setItem(ATTUNED_SLOT, new ItemStack(profile.planksIngredient()))
			);
			List<CabinUpgradeState.Requirement> requirements;
			if (tracked != null) {
				flags |= FLAG_TRACKED;
				data.set(DATA_TARGET_SIZE, tracked.target().generalSpaceSize());
				requirements = tracked.requirements();
				if (CabinUpgradeCatalog.isStale(tracked, cabin, attunement, definitions)) {
					flags |= FLAG_STALE;
					contextualMessage = "Tracked requirements changed; stop tracking to recover the fund.";
				}
				if (tracked.isComplete()) {
					flags |= FLAG_COMPLETE;
				}
				if (cabin.upgrades().installation().isPresent()) {
					flags |= FLAG_INSTALLING;
				}
				CabinUpgradeService.Outcome check = new CabinGeneralSpaceEffect(
					serverPlayer.level().getServer(), serverPlayer.level()
				).validate(cabin, tracked.target().generalSpaceSize());
				if (!check.success()) {
					flags |= FLAG_BLOCKED;
					contextualMessage = check.message();
				}
				writeRequirements(requirements, tracked);
			} else {
				CabinUpgradeCatalog.Offer offer = CabinUpgradeCatalog.next(cabin, attunement, definitions)
					.orElse(null);
				if (offer == null) {
					flags |= FLAG_MAXIMUM;
					data.set(DATA_TARGET_SIZE, 0);
					writeRequirements(List.of(), null);
				} else {
					data.set(DATA_TARGET_SIZE, offer.targetSize());
					writeRequirements(offer.requirements(), null);
					CabinUpgradeService.Outcome check = new CabinGeneralSpaceEffect(
						serverPlayer.level().getServer(), serverPlayer.level()
					).validate(cabin, offer.targetSize());
					if (!check.success()) {
						flags |= FLAG_BLOCKED;
						contextualMessage = check.message();
					}
				}
			}
		} catch (IllegalStateException exception) {
			contextualMessage = exception.getMessage();
			if (tracked == null) {
				writeRequirements(List.of(), null);
			} else {
				flags |= FLAG_TRACKED | FLAG_STALE;
				if (tracked.isComplete()) {
					flags |= FLAG_COMPLETE;
				}
				if (cabin.upgrades().installation().isPresent()) {
					flags |= FLAG_INSTALLING;
				}
				data.set(DATA_TARGET_SIZE, tracked.target().generalSpaceSize());
				writeRequirements(tracked.requirements(), tracked);
			}
		}
		setFlagData(flags);
		writeStatus(contextualMessage.isEmpty() ? actionMessage : contextualMessage);
	}

	private void writeRequirements(
		List<CabinUpgradeState.Requirement> requirements,
		CabinUpgradeState.TrackedUpgrade tracked
	) {
		int count = Math.min(requirements.size(), MAX_REQUIREMENTS);
		data.set(DATA_REQUIREMENT_COUNT, count);
		for (int index = 0; index < MAX_REQUIREMENTS; index++) {
			data.set(DATA_REQUIREMENTS + index * 2, 0);
			data.set(DATA_REQUIREMENTS + index * 2 + 1, 0);
			if (index >= count) {
				continue;
			}
			CabinUpgradeState.Requirement requirement = requirements.get(index);
			var item = BuiltInRegistries.ITEM.getOptional(requirement.itemId()).orElse(null);
			if (item != null) {
				display.setItem(FIRST_REQUIREMENT_SLOT + index, new ItemStack(item));
			}
			data.set(DATA_REQUIREMENTS + index * 2, requirement.count());
			data.set(DATA_REQUIREMENTS + index * 2 + 1,
				tracked == null ? 0 : tracked.fundedCount(requirement.itemId()));
		}
	}

	private void clearDisplay() {
		for (int index = FIRST_REQUIREMENT_SLOT; index < FIRST_PLAYER_SLOT; index++) {
			display.setItem(index, ItemStack.EMPTY);
		}
	}

	private void setActionMessage(String message) {
		actionMessage = message == null ? "" : message;
		writeStatus(actionMessage);
	}

	private void writeStatus(String message) {
		if (message == null || message.isEmpty()) {
			display.setItem(STATUS_SLOT, ItemStack.EMPTY);
			return;
		}
		ItemStack status = new ItemStack(Items.PAPER);
		status.set(DataComponents.CUSTOM_NAME, Component.literal(message));
		display.setItem(STATUS_SLOT, status);
	}

	private void setFlagData(int flags) {
		data.set(DATA_FLAGS, flags);
	}

	private boolean flag(int flag) {
		return (data.get(DATA_FLAGS) & flag) != 0;
	}

	private static final class DisplaySlot extends Slot {
		private DisplaySlot(SimpleContainer container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public boolean isFake() {
			return true;
		}
	}
}
