package dev.portablepocketcabin;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Synchronized menu whose requirement icons transact with durable, target-keyed funds. */
final class CabinUpgradeMenu extends AbstractContainerMenu {
	static final int BUTTON_INSTALL = 1;
	static final int MAX_REQUIREMENTS = 8;
	static final int FIRST_REQUIREMENT_SLOT = 0;
	static final int ATTUNED_SLOT = MAX_REQUIREMENTS;
	static final int STATUS_SLOT = ATTUNED_SLOT + 1;
	static final int FIRST_PLAYER_SLOT = STATUS_SLOT + 1;
	static final int PLAYER_INVENTORY_SLOTS = 36;

	private static final int DATA_CURRENT_SIZE = 0;
	private static final int DATA_MAXIMUM_SIZE = 1;
	private static final int DATA_TARGET_SIZE = 2;
	private static final int DATA_FLAGS = 3;
	private static final int DATA_REQUIREMENT_COUNT = 4;
	private static final int DATA_REQUIREMENTS = 5;
	private static final int DATA_COUNT = DATA_REQUIREMENTS + MAX_REQUIREMENTS * 2;

	private static final int FLAG_OWNER = 1;
	private static final int FLAG_CONTRIBUTOR = 1 << 1;
	private static final int FLAG_AVAILABLE = 1 << 2;
	private static final int FLAG_STALE = 1 << 3;
	private static final int FLAG_COMPLETE = 1 << 4;
	private static final int FLAG_MAXIMUM = 1 << 5;
	private static final int FLAG_INSTALLING = 1 << 6;
	private static final int FLAG_BLOCKED = 1 << 7;
	private static final int FLAG_ARMED = 1 << 8;
	private static final long CONFIRMATION_TICKS = 100L;

	static final MenuType<CabinUpgradeMenu> TYPE = Registry.register(
		BuiltInRegistries.MENU,
		PortablePocketCabin.id("cabin_upgrades"),
		new ExtendedMenuType<>(CabinUpgradeMenu::new, UUIDUtil.STREAM_CODEC)
	);

	private final UUID cabinId;
	private final SimpleContainer display = new SimpleContainer(FIRST_PLAYER_SLOT);
	private final SimpleContainerData data = new SimpleContainerData(DATA_COUNT);
	private final ServerPlayer serverPlayer;
	private final Inventory playerInventory;
	private final Set<Integer> quickFundSlots = new LinkedHashSet<>();
	private CabinUpgradeState.Target target;
	private CabinUpgradeState.Target armedTarget;
	private long armedRevision = -1L;
	private long armedUntil = -1L;
	private String actionMessage = "";

	CabinUpgradeMenu(int containerId, Inventory inventory, UUID cabinId) {
		super(TYPE, containerId);
		this.cabinId = cabinId;
		this.playerInventory = inventory;
		this.serverPlayer = inventory.player instanceof ServerPlayer player ? player : null;

		for (int index = 0; index < MAX_REQUIREMENTS; index++) {
			addSlot(new DisplaySlot(display, index, 55 + index * 22, 56));
		}
		addSlot(new DisplaySlot(display, ATTUNED_SLOT, -1000, -1000));
		addSlot(new DisplaySlot(display, STATUS_SLOT, -1000, -1000));
		addStandardInventorySlots(inventory, 44, 136);
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

	boolean canUseFund() {
		return flag(FLAG_CONTRIBUTOR);
	}

	boolean isAvailable() {
		return flag(FLAG_AVAILABLE);
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

	boolean isArmed() {
		return flag(FLAG_ARMED);
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (button != BUTTON_INSTALL || !(player instanceof ServerPlayer actor)
			|| actor != serverPlayer || !stillValid(player)) {
			return false;
		}
		CabinRegistry registry = CabinRegistry.get(actor.level().getServer());
		CabinRecord cabin = registry.find(cabinId).orElse(null);
		if (cabin == null || target == null || !cabin.owner().equals(actor.getUUID())) {
			return false;
		}
		if (!isComplete() || !isAvailable() || installationInProgress()) {
			setActionMessage(statusMessage().getString().isEmpty()
				? "That upgrade is not ready to install." : statusMessage().getString());
			refreshFromServer();
			broadcastChanges();
			return false;
		}
		long revision = cabin.upgrades().fundRevision();
		long now = actor.level().getGameTime();
		if (!target.equals(armedTarget) || revision != armedRevision || now > armedUntil) {
			armedTarget = target;
			armedRevision = revision;
			armedUntil = now + CONFIRMATION_TICKS;
			setActionMessage("Click install again to confirm.");
			refreshFromServer();
			broadcastChanges();
			return true;
		}

		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		CabinUpgradeService.Outcome outcome;
		try {
			WorldAttunement attunement = CabinUpgradeCatalog.resolveAttunement(
				registry, actor.level(), definitions
			);
			outcome = CabinUpgradeService.install(
				registry, cabinId, actor.getUUID(), target, armedRevision, attunement, definitions,
				new CabinGeneralSpaceEffect(actor.level().getServer(), actor.level()),
				() -> CabinRegistry.flush(actor.level().getServer())
			);
		} catch (IllegalStateException exception) {
			outcome = CabinUpgradeService.Outcome.failure(exception.getMessage());
		}
		clearArming();
		setActionMessage(outcome.message());
		actor.sendSystemMessage(Component.literal(outcome.message()));
		refreshFromServer();
		broadcastChanges();
		return outcome.success();
	}

	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (isRequirementSlot(slotId)) {
			if (serverPlayer == null || player != serverPlayer || !stillValid(player)) {
				return;
			}
			if (input == ContainerInput.PICKUP) {
				handleRequirementPickup(slotId, button);
			} else if (input == ContainerInput.QUICK_MOVE) {
				fundFromInventory(slotId);
			} else if (input == ContainerInput.QUICK_CRAFT) {
				handleRequirementDrag(slotId, button);
			}
			return;
		}
		if (input == ContainerInput.QUICK_CRAFT && serverPlayer != null && player == serverPlayer) {
			handleRequirementDrag(slotId, button);
		}
		super.clicked(slotId, button, input, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotId) {
		if (serverPlayer == null || player != serverPlayer || slotId < FIRST_PLAYER_SLOT
			|| slotId >= FIRST_PLAYER_SLOT + PLAYER_INVENTORY_SLOTS) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(slotId);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack original = slot.getItem().copy();
		int mainEnd = FIRST_PLAYER_SLOT + 27;
		int playerEnd = FIRST_PLAYER_SLOT + PLAYER_INVENTORY_SLOTS;
		boolean moved = slotId < mainEnd
			? moveItemStackTo(slot.getItem(), mainEnd, playerEnd, false)
			: moveItemStackTo(slot.getItem(), FIRST_PLAYER_SLOT, mainEnd, false);
		if (!moved) {
			return ItemStack.EMPTY;
		}
		if (slot.getItem().isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
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

	@Override
	public boolean canDragTo(Slot slot) {
		return slot instanceof DisplaySlot displaySlot && displaySlot.isRequirement()
			&& canUseFund() && isAvailable();
	}

	private void handleRequirementPickup(int slotId, int button) {
		ItemStack icon = display.getItem(slotId);
		if (icon.isEmpty() || target == null || !canUseFund()) {
			return;
		}
		ItemStack carried = getCarried();
		if (!carried.isEmpty()) {
			if (!ItemStack.isSameItem(carried, icon) || !isAvailable()) {
				return;
			}
			int limit = button == 1 ? 1 : carried.getCount();
			ItemStack offered = carried.copyWithCount(limit);
			int accepted = deposit(offered);
			if (accepted > 0) {
				carried.shrink(accepted);
			}
			return;
		}
		Identifier itemId = BuiltInRegistries.ITEM.getKey(icon.getItem());
		CabinRegistry registry = CabinRegistry.get(serverPlayer.level().getServer());
		int requested = button == 1 ? halfOfFirstStoredStack(registry, itemId) : icon.getMaxStackSize();
		CabinUpgradeService.Withdrawal result = CabinUpgradeService.withdraw(
			registry, cabinId, serverPlayer.getUUID(),
			target, itemId, requested
		);
		setActionMessage(result.message());
		if (result.success()) {
			setCarried(result.stack());
			clearArming();
			CabinRegistry.flush(serverPlayer.level().getServer());
		}
		refreshFromServer();
		broadcastChanges();
	}

	private int halfOfFirstStoredStack(CabinRegistry registry, Identifier itemId) {
		if (target == null) {
			return 0;
		}
		return registry.find(cabinId)
			.flatMap(cabin -> cabin.upgrades().fund(target))
			.flatMap(fund -> fund.stacks().stream()
				.filter(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId))
				.findFirst())
			.map(stack -> (stack.getCount() + 1) / 2)
			.orElse(0);
	}

	private void fundFromInventory(int slotId) {
		ItemStack icon = display.getItem(slotId);
		if (icon.isEmpty() || !isAvailable() || !canUseFund()) {
			return;
		}
		for (int index = 0; index < playerInventory.getContainerSize(); index++) {
			ItemStack stack = playerInventory.getItem(index);
			if (!stack.isEmpty() && ItemStack.isSameItem(stack, icon) && stack.getComponentsPatch().isEmpty()) {
				deposit(stack);
				if (fundedCount(slotId) >= requiredCount(slotId)) {
					break;
				}
			}
		}
		playerInventory.setChanged();
	}

	private void handleRequirementDrag(int slotId, int button) {
		int header = getQuickcraftHeader(button);
		if (header == QUICKCRAFT_HEADER_START) {
			quickFundSlots.clear();
		} else if (header == QUICKCRAFT_HEADER_CONTINUE && isRequirementSlot(slotId)) {
			quickFundSlots.add(slotId);
		} else if (header == QUICKCRAFT_HEADER_END) {
			ItemStack carried = getCarried();
			int type = getQuickcraftType(button);
			for (int fundSlot : quickFundSlots) {
				ItemStack icon = display.getItem(fundSlot);
				if (carried.isEmpty() || !ItemStack.isSameItem(carried, icon)) {
					continue;
				}
				int limit = type == QUICKCRAFT_TYPE_CHARITABLE ? 1 : carried.getCount();
				ItemStack offered = carried.copyWithCount(limit);
				int accepted = deposit(offered);
				carried.shrink(accepted);
			}
			quickFundSlots.clear();
		}
	}

	private int deposit(ItemStack offered) {
		if (offered.isEmpty() || target == null || !stillValid(serverPlayer)) {
			return 0;
		}
		CabinRegistry registry = CabinRegistry.get(serverPlayer.level().getServer());
		try {
			CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
			WorldAttunement attunement = CabinUpgradeCatalog.resolveAttunement(
				registry, serverPlayer.level(), definitions
			);
			CabinUpgradeService.Contribution result = CabinUpgradeService.deposit(
				registry, cabinId, serverPlayer.getUUID(), target, offered, attunement, definitions,
				new CabinGeneralSpaceEffect(serverPlayer.level().getServer(), serverPlayer.level())
			);
			setActionMessage(result.message());
			if (result.success()) {
				clearArming();
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
		clearDisplay();
		if (cabin == null) {
			target = null;
			setFlagData(0);
			writeStatus("That cabin no longer exists.");
			return;
		}

		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		data.set(DATA_CURRENT_SIZE, cabin.progression().generalSize());
		data.set(DATA_MAXIMUM_SIZE, definitions.maximumGeneralSize());
		int flags = 0;
		if (cabin.owner().equals(serverPlayer.getUUID())) {
			flags |= FLAG_OWNER | FLAG_CONTRIBUTOR;
		} else if (cabin.trustedPlayers().contains(serverPlayer.getUUID())
			&& cabin.canEnter(serverPlayer.getUUID())) {
			flags |= FLAG_CONTRIBUTOR;
		}
		if (cabin.upgrades().installation().isPresent()) {
			flags |= FLAG_INSTALLING;
		}
		String contextualMessage = "";
		try {
			WorldAttunement attunement = CabinUpgradeCatalog.resolveAttunement(
				registry, serverPlayer.level(), definitions
			);
			CabinMaterialProfiles.woodProfile(attunement.woodProfile()).ifPresent(profile ->
				display.setItem(ATTUNED_SLOT, new ItemStack(profile.planksIngredient()))
			);
			List<CabinUpgradeCatalog.Group> groups = CabinUpgradeCatalog.groups(cabin, attunement, definitions);
			CabinUpgradeCatalog.Offer offer = groups.isEmpty()
				? null : groups.getFirst().panels().getFirst();
			if (offer == null) {
				target = null;
				flags |= FLAG_MAXIMUM;
				data.set(DATA_TARGET_SIZE, 0);
				writeRequirements(List.of(), null);
			} else {
				target = offer.target();
				data.set(DATA_TARGET_SIZE, offer.targetSize());
				CabinUpgradeState.Fund fund = cabin.upgrades().fund(target).orElse(null);
				if (fund != null && CabinUpgradeCatalog.isStale(fund, cabin, attunement, definitions)) {
					flags |= FLAG_STALE;
					contextualMessage = "Funded requirements no longer match the loaded definition.";
				}
				CabinUpgradeService.Outcome validation = new CabinGeneralSpaceEffect(
					serverPlayer.level().getServer(), serverPlayer.level()
				).validate(cabin, offer.targetSize());
				if (!validation.success()) {
					flags |= FLAG_BLOCKED;
					contextualMessage = validation.message();
				} else if ((flags & FLAG_STALE) == 0 && (flags & FLAG_INSTALLING) == 0) {
					flags |= FLAG_AVAILABLE;
				}
				if (fund != null && fund.isComplete()) {
					flags |= FLAG_COMPLETE;
				}
				writeRequirements(fund == null ? offer.requirements() : fund.requirements(), fund);
			}
		} catch (IllegalStateException exception) {
			target = null;
			flags |= FLAG_STALE;
			contextualMessage = exception.getMessage();
			writeRequirements(List.of(), null);
		}
		if (armedTarget != null && target != null && armedTarget.equals(target)
			&& armedRevision == cabin.upgrades().fundRevision()
			&& serverPlayer.level().getGameTime() <= armedUntil) {
			flags |= FLAG_ARMED;
		} else {
			clearArming();
		}
		setFlagData(flags);
		writeStatus(contextualMessage.isEmpty() ? actionMessage : contextualMessage);
	}

	private void writeRequirements(
		List<CabinUpgradeState.Requirement> requirements, CabinUpgradeState.Fund fund
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
				display.setItem(index, new ItemStack(item));
			}
			data.set(DATA_REQUIREMENTS + index * 2, requirement.count());
			data.set(DATA_REQUIREMENTS + index * 2 + 1,
				fund == null ? 0 : fund.fundedCount(requirement.itemId()));
		}
	}

	private void clearDisplay() {
		for (int index = 0; index < FIRST_PLAYER_SLOT; index++) {
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

	private void clearArming() {
		armedTarget = null;
		armedRevision = -1L;
		armedUntil = -1L;
	}

	private void setFlagData(int flags) {
		data.set(DATA_FLAGS, flags);
	}

	private boolean flag(int flag) {
		return (data.get(DATA_FLAGS) & flag) != 0;
	}

	private static boolean isRequirementSlot(int slotId) {
		return slotId >= FIRST_REQUIREMENT_SLOT && slotId < FIRST_REQUIREMENT_SLOT + MAX_REQUIREMENTS;
	}

	private final class DisplaySlot extends Slot {
		private final int displayIndex;

		private DisplaySlot(SimpleContainer container, int slot, int x, int y) {
			super(container, slot, x, y);
			displayIndex = slot;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return isRequirement() && canUseFund() && isAvailable()
				&& ItemStack.isSameItem(stack, display.getItem(displayIndex));
		}

		@Override
		public boolean mayPickup(Player player) {
			return isRequirement() && canUseFund() && fundedCount(displayIndex) > 0;
		}

		@Override
		public boolean isFake() {
			return true;
		}

		private boolean isRequirement() {
			return isRequirementSlot(displayIndex);
		}
	}
}
