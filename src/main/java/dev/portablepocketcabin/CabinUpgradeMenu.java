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
	static final int BUTTON_PREVIOUS_PANEL = 2;
	static final int BUTTON_NEXT_PANEL = 3;
	static final int BUTTON_DOWNGRADE = 4;
	static final int BUTTON_REMOVE = 5;
	static final int BUTTON_GROUP_BASE = 100;
	static final int MAX_REQUIREMENTS = 16;
	static final int REQUIREMENTS_PER_ROW = CabinUpgradeLayout.REQUIREMENT_COLUMNS;
	static final int MAX_GROUPS = 8;
	static final int FIRST_REQUIREMENT_SLOT = 0;
	static final int ATTUNED_SLOT = MAX_REQUIREMENTS;
	static final int STATUS_SLOT = ATTUNED_SLOT + 1;
	static final int PANEL_SLOT = STATUS_SLOT + 1;
	static final int EFFECT_SLOT = PANEL_SLOT + 1;
	static final int DOWNGRADE_STATUS_SLOT = EFFECT_SLOT + 1;
	static final int REMOVE_STATUS_SLOT = DOWNGRADE_STATUS_SLOT + 1;
	static final int FIRST_GROUP_SLOT = REMOVE_STATUS_SLOT + 1;
	static final int FIRST_PLAYER_SLOT = FIRST_GROUP_SLOT + MAX_GROUPS;
	static final int PLAYER_INVENTORY_SLOTS = 36;

	private static final int DATA_FLAGS = 0;
	private static final int DATA_REQUIREMENT_COUNT = 1;
	private static final int DATA_GROUP_COUNT = 2;
	private static final int DATA_SELECTED_GROUP = 3;
	private static final int DATA_PANEL_COUNT = 4;
	private static final int DATA_SELECTED_PANEL = 5;
	private static final int DATA_REQUIREMENTS = 6;
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
	private static final int FLAG_OVERSIZED = 1 << 9;
	private static final int FLAG_CAN_DOWNGRADE = 1 << 10;
	private static final int FLAG_CAN_REMOVE = 1 << 11;
	private static final int FLAG_ARMED_DOWNGRADE = 1 << 12;
	private static final int FLAG_ARMED_REMOVE = 1 << 13;
	private static final int FLAG_HAS_WINDOW = 1 << 14;
	private static final int FLAG_HAS_DOWNGRADE = 1 << 15;
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
	private Identifier selectedGroupId;
	private CabinUpgradeSelection.Selected selection;
	private CabinUpgradeState.Target armedTarget;
	private CabinUpgradeState.ReversalAction armedReversalAction;
	private long armedRevision = -1L;
	private long armedUntil = -1L;
	private String actionMessage = "";

	CabinUpgradeMenu(int containerId, Inventory inventory, UUID cabinId) {
		super(TYPE, containerId);
		this.cabinId = cabinId;
		this.playerInventory = inventory;
		this.serverPlayer = inventory.player instanceof ServerPlayer player ? player : null;

		for (int index = 0; index < MAX_REQUIREMENTS; index++) {
			addSlot(new DisplaySlot(
				display, index,
				CabinUpgradeLayout.requirementX(index),
				CabinUpgradeLayout.requirementY(index)
			));
		}
		addSlot(new DisplaySlot(display, ATTUNED_SLOT, -1000, -1000));
		addSlot(new DisplaySlot(display, STATUS_SLOT, -1000, -1000));
		addSlot(new DisplaySlot(display, PANEL_SLOT, -1000, -1000));
		addSlot(new DisplaySlot(display, EFFECT_SLOT, -1000, -1000));
		addSlot(new DisplaySlot(display, DOWNGRADE_STATUS_SLOT, -1000, -1000));
		addSlot(new DisplaySlot(display, REMOVE_STATUS_SLOT, -1000, -1000));
		for (int index = 0; index < MAX_GROUPS; index++) {
			addSlot(new DisplaySlot(display, FIRST_GROUP_SLOT + index, -1000, -1000));
		}
		addStandardInventorySlots(inventory, CabinUpgradeLayout.INVENTORY_X, CabinUpgradeLayout.INVENTORY_Y);
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

	int requirementCount() {
		return data.get(DATA_REQUIREMENT_COUNT);
	}

	int groupCount() {
		return data.get(DATA_GROUP_COUNT);
	}

	int selectedGroupIndex() {
		return data.get(DATA_SELECTED_GROUP);
	}

	int panelCount() {
		return data.get(DATA_PANEL_COUNT);
	}

	int selectedPanelIndex() {
		return data.get(DATA_SELECTED_PANEL);
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

	ItemStack groupStack(int index) {
		return index < 0 || index >= MAX_GROUPS
			? ItemStack.EMPTY : display.getItem(FIRST_GROUP_SLOT + index);
	}

	ItemStack panelStack() {
		return display.getItem(PANEL_SLOT);
	}

	Component panelTitle() {
		ItemStack stack = panelStack();
		return stack.isEmpty() ? Component.empty() : stack.getHoverName();
	}

	Component panelEffect() {
		ItemStack stack = display.getItem(EFFECT_SLOT);
		return stack.isEmpty() ? Component.empty() : stack.getHoverName();
	}

	Component statusMessage() {
		ItemStack stack = display.getItem(STATUS_SLOT);
		return stack.isEmpty() ? Component.empty() : stack.getHoverName();
	}

	Component downgradeStatusMessage() {
		ItemStack stack = display.getItem(DOWNGRADE_STATUS_SLOT);
		return stack.isEmpty() ? Component.empty() : stack.getHoverName();
	}

	Component removeStatusMessage() {
		ItemStack stack = display.getItem(REMOVE_STATUS_SLOT);
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

	boolean canDowngrade() {
		return flag(FLAG_CAN_DOWNGRADE);
	}

	boolean canRemove() {
		return flag(FLAG_CAN_REMOVE);
	}

	boolean isDowngradeArmed() {
		return flag(FLAG_ARMED_DOWNGRADE);
	}

	boolean isRemoveArmed() {
		return flag(FLAG_ARMED_REMOVE);
	}

	boolean hasInstalledWindow() {
		return flag(FLAG_HAS_WINDOW);
	}

	boolean hasDowngradeAction() {
		return flag(FLAG_HAS_DOWNGRADE);
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (!(player instanceof ServerPlayer actor) || actor != serverPlayer || !stillValid(player)) {
			return false;
		}
		if (button == BUTTON_DOWNGRADE || button == BUTTON_REMOVE) {
			return reverseWindow(actor, button);
		}
		if (button != BUTTON_INSTALL) {
			return navigate(actor, button);
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
		if (!target.equals(armedTarget) || armedReversalAction != null
			|| revision != armedRevision || now > armedUntil) {
			armedTarget = target;
			armedReversalAction = null;
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
				new CabinUpgradeEffect(actor.level().getServer(), actor.level()),
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

	private boolean reverseWindow(ServerPlayer actor, int button) {
		CabinRegistry registry = CabinRegistry.get(actor.level().getServer());
		CabinRecord cabin = registry.find(cabinId).orElse(null);
		if (cabin == null || target == null || !target.isWindow()
			|| !cabin.owner().equals(actor.getUUID())) {
			return false;
		}
		if (cabin.upgrades().operationInProgress()) {
			setActionMessage("A cabin upgrade operation is in progress.");
			refreshFromServer();
			broadcastChanges();
			return false;
		}
		CabinUpgradeState.ReversalAction action = button == BUTTON_DOWNGRADE
			? CabinUpgradeState.ReversalAction.DOWNGRADE : CabinUpgradeState.ReversalAction.REMOVE;
		try {
			CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
			WorldAttunement attunement = CabinUpgradeCatalog.resolveAttunement(
				registry, actor.level(), definitions
			);
			CabinWindowReversalEffect effect = new CabinWindowReversalEffect(
				actor.level().getServer(), actor.level()
			);
			CabinWindowReversalService.Preview preview = CabinWindowReversalService.preview(
				cabin, target, action, attunement, definitions, effect
			);
			if (!preview.available()) {
				setActionMessage(preview.message());
				refreshFromServer();
				broadcastChanges();
				return false;
			}
			long revision = cabin.upgrades().fundRevision();
			long now = actor.level().getGameTime();
			if (!target.equals(armedTarget) || action != armedReversalAction
				|| revision != armedRevision || now > armedUntil) {
				armedTarget = target;
				armedReversalAction = action;
				armedRevision = revision;
				armedUntil = now + CONFIRMATION_TICKS;
				String warning = preview.invalidatedFunds().isEmpty() ? "" : " Funded materials for "
					+ invalidatedTitles(cabin, preview.invalidatedFunds(), attunement, definitions)
					+ " will also be dropped.";
				setActionMessage("Click " + action.name().toLowerCase(java.util.Locale.ROOT)
					+ " again to confirm." + warning);
				refreshFromServer();
				broadcastChanges();
				return true;
			}

			CabinWindowReversalService.Ejector ejector = (value, reversal) ->
				CabinFundEjection.ejectMarked(
					actor.level(), value, reversal.operationId(), reversal.payloadStacks()
				);
			CabinUpgradeService.Outcome outcome = CabinWindowReversalService.reverse(
				registry, cabinId, actor.getUUID(), target, action, armedRevision, attunement,
				definitions, effect, ejector, () -> CabinRegistry.flush(actor.level().getServer())
			);
			clearArming();
			setActionMessage(outcome.message());
			actor.sendSystemMessage(Component.literal(outcome.message()));
			refreshFromServer();
			broadcastChanges();
			return outcome.success();
		} catch (IllegalStateException exception) {
			clearArming();
			setActionMessage(exception.getMessage());
			refreshFromServer();
			broadcastChanges();
			return false;
		}
	}

	private static String invalidatedTitles(
		CabinRecord cabin,
		List<CabinUpgradeState.Fund> funds,
		WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		return funds.stream().map(fund -> CabinUpgradeCatalog.offer(
			cabin, fund.target(), attunement, definitions
		).map(CabinUpgradeCatalog.Offer::title).orElse(fund.target().key()))
			.collect(java.util.stream.Collectors.joining(", "));
	}

	private boolean navigate(ServerPlayer actor, int button) {
		CabinRegistry registry = CabinRegistry.get(actor.level().getServer());
		CabinRecord cabin = registry.find(cabinId).orElse(null);
		if (cabin == null) {
			return false;
		}
		try {
			CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
			WorldAttunement attunement = CabinUpgradeCatalog.resolveAttunement(
				registry, actor.level(), definitions
			);
			List<CabinUpgradeCatalog.Group> groups = CabinUpgradeCatalog.groups(
				cabin, attunement, definitions
			);
			if (groups.size() > MAX_GROUPS) {
				return false;
			}
			CabinUpgradeSelection.Selected current = CabinUpgradeSelection.resolve(
				groups, selectedGroupId, target
			).orElse(null);
			CabinUpgradeSelection.Selected requested;
			if (button == BUTTON_PREVIOUS_PANEL) {
				requested = CabinUpgradeSelection.cyclePanel(groups, current, -1).orElse(null);
			} else if (button == BUTTON_NEXT_PANEL) {
				requested = CabinUpgradeSelection.cyclePanel(groups, current, 1).orElse(null);
			} else if (button >= BUTTON_GROUP_BASE && button < BUTTON_GROUP_BASE + MAX_GROUPS) {
				requested = CabinUpgradeSelection.selectGroup(groups, button - BUTTON_GROUP_BASE).orElse(null);
			} else {
				return false;
			}
			if (requested == null) {
				return false;
			}
			applySelection(requested);
			refreshFromServer();
			broadcastChanges();
			return true;
		} catch (IllegalStateException exception) {
			setActionMessage(exception.getMessage());
			return false;
		}
	}

	private void applySelection(CabinUpgradeSelection.Selected selected) {
		if (target == null || !target.equals(selected.offer().target())) {
			clearArming();
			actionMessage = "";
		}
		selection = selected;
		selectedGroupId = selected.group().id();
		target = selected.offer().target();
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
					new CabinUpgradeEffect(serverPlayer.level().getServer(), serverPlayer.level())
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
		resetPresentationData();
		if (cabin == null) {
			target = null;
			selection = null;
			selectedGroupId = null;
			setFlagData(0);
			writeStatus("That cabin no longer exists.");
			return;
		}

		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		int flags = 0;
		if (cabin.owner().equals(serverPlayer.getUUID())) {
			flags |= FLAG_OWNER | FLAG_CONTRIBUTOR;
		} else if (cabin.trustedPlayers().contains(serverPlayer.getUUID())
			&& cabin.canEnter(serverPlayer.getUUID())) {
			flags |= FLAG_CONTRIBUTOR;
		}
		if (cabin.upgrades().operationInProgress()) {
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
			if (groups.size() > MAX_GROUPS) {
				target = null;
				selection = null;
				clearArming();
				flags |= FLAG_BLOCKED;
				contextualMessage = "This cabin has too many upgrade categories for the interface.";
				writeRequirements(List.of(), null);
				setFlagData(flags);
				writeStatus(contextualMessage);
				return;
			}
			writeGroups(groups);
			CabinUpgradeState.Target previousTarget = target;
			selection = CabinUpgradeSelection.resolve(groups, selectedGroupId, target).orElse(null);
			CabinUpgradeCatalog.Offer offer = selection == null ? null : selection.offer();
			if (offer == null) {
				target = null;
				selectedGroupId = null;
				flags |= FLAG_MAXIMUM;
				writeRequirements(List.of(), null);
			} else {
				applySelection(selection);
				if (previousTarget != null && !previousTarget.equals(target)) {
					clearArming();
				}
				data.set(DATA_SELECTED_GROUP, selection.groupIndex());
				data.set(DATA_PANEL_COUNT, selection.group().panels().size());
				data.set(DATA_SELECTED_PANEL, selection.panelIndex());
				writePanel(offer);
				CabinUpgradeState.Fund fund = cabin.upgrades().fund(target).orElse(null);
				List<CabinUpgradeState.Requirement> displayedRequirements = fund == null
					? offer.requirements() : fund.requirements();
					if (offer.complete()) {
						flags |= FLAG_MAXIMUM;
						contextualMessage = "This upgrade is fully installed.";
						writeRequirements(List.of(), null);
					} else if (offer.locked()) {
						flags |= FLAG_BLOCKED;
						contextualMessage = offer.prerequisite();
						writeRequirements(offer.requirements(), fund);
					} else if (displayedRequirements.size() > MAX_REQUIREMENTS) {
					flags |= FLAG_BLOCKED | FLAG_OVERSIZED;
					contextualMessage = "This upgrade requires " + displayedRequirements.size()
						+ " material types; the interface supports at most " + MAX_REQUIREMENTS + ".";
					writeRequirements(List.of(), null);
				} else if (fund != null && CabinUpgradeCatalog.isStale(fund, cabin, attunement, definitions)) {
					flags |= FLAG_STALE;
					contextualMessage = "Funded requirements no longer match the loaded definition.";
					writeRequirements(fund.requirements(), fund);
				} else {
					CabinUpgradeService.Outcome validation = new CabinUpgradeEffect(
						serverPlayer.level().getServer(), serverPlayer.level()
					).validate(cabin, offer);
					if (!validation.success()) {
						flags |= FLAG_BLOCKED;
						contextualMessage = validation.message();
					} else if ((flags & FLAG_INSTALLING) == 0) {
						flags |= FLAG_AVAILABLE;
					}
					if (fund != null && fund.isComplete()) {
						flags |= FLAG_COMPLETE;
					}
					writeRequirements(displayedRequirements, fund);
				}
				if (target.isWindow()) {
					int installedTier = cabin.upgrades().windows().tier(target.windowIdentity());
					if (installedTier > 0) {
						flags |= FLAG_HAS_WINDOW;
					}
					if (installedTier > 1) {
						flags |= FLAG_HAS_DOWNGRADE;
					}
					if ((flags & FLAG_INSTALLING) != 0) {
						writeActionStatus(DOWNGRADE_STATUS_SLOT, "A cabin upgrade operation is in progress.");
						writeActionStatus(REMOVE_STATUS_SLOT, "A cabin upgrade operation is in progress.");
					} else {
						CabinWindowReversalEffect reversalEffect = new CabinWindowReversalEffect(
							serverPlayer.level().getServer(), serverPlayer.level()
						);
						CabinWindowReversalService.Preview downgrade = CabinWindowReversalService.preview(
							cabin, target, CabinUpgradeState.ReversalAction.DOWNGRADE,
							attunement, definitions, reversalEffect
						);
						CabinWindowReversalService.Preview remove = CabinWindowReversalService.preview(
							cabin, target, CabinUpgradeState.ReversalAction.REMOVE,
							attunement, definitions, reversalEffect
						);
						writeActionStatus(DOWNGRADE_STATUS_SLOT, downgrade.message());
						writeActionStatus(REMOVE_STATUS_SLOT, remove.message());
						if (downgrade.available()) {
							flags |= FLAG_CAN_DOWNGRADE;
						}
						if (remove.available()) {
							flags |= FLAG_CAN_REMOVE;
						}
					}
				}
			}
		} catch (IllegalStateException exception) {
			target = null;
			selection = null;
			flags |= FLAG_STALE;
			contextualMessage = exception.getMessage();
			writeRequirements(List.of(), null);
		}
		if (armedTarget != null && target != null && armedTarget.equals(target)
			&& armedRevision == cabin.upgrades().fundRevision()
			&& serverPlayer.level().getGameTime() <= armedUntil) {
			if (armedReversalAction == CabinUpgradeState.ReversalAction.DOWNGRADE) {
				flags |= FLAG_ARMED_DOWNGRADE;
			} else if (armedReversalAction == CabinUpgradeState.ReversalAction.REMOVE) {
				flags |= FLAG_ARMED_REMOVE;
			} else {
				flags |= FLAG_ARMED;
			}
		} else {
			clearArming();
		}
		setFlagData(flags);
		writeStatus(contextualMessage.isEmpty() ? actionMessage : contextualMessage);
	}

	private void writeGroups(List<CabinUpgradeCatalog.Group> groups) {
		data.set(DATA_GROUP_COUNT, groups.size());
		for (int index = 0; index < groups.size(); index++) {
			CabinUpgradeCatalog.Group group = groups.get(index);
			display.setItem(FIRST_GROUP_SLOT + index, namedStack(group.iconItem(), group.title()));
		}
	}

	private void writePanel(CabinUpgradeCatalog.Offer offer) {
		display.setItem(PANEL_SLOT, namedStack(offer.iconItem(), offer.title()));
		display.setItem(EFFECT_SLOT, namedStack(
			Items.PAPER.builtInRegistryHolder().key().identifier(), offer.effect()
		));
	}

	private static ItemStack namedStack(Identifier itemId, String name) {
		var item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.BARRIER);
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
		return stack;
	}

	private void resetPresentationData() {
		data.set(DATA_REQUIREMENT_COUNT, 0);
		data.set(DATA_GROUP_COUNT, 0);
		data.set(DATA_SELECTED_GROUP, 0);
		data.set(DATA_PANEL_COUNT, 0);
		data.set(DATA_SELECTED_PANEL, 0);
		for (int index = 0; index < MAX_REQUIREMENTS; index++) {
			data.set(DATA_REQUIREMENTS + index * 2, 0);
			data.set(DATA_REQUIREMENTS + index * 2 + 1, 0);
		}
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

	private void writeActionStatus(int slot, String message) {
		if (message == null || message.isEmpty()) {
			display.setItem(slot, ItemStack.EMPTY);
			return;
		}
		ItemStack status = new ItemStack(Items.PAPER);
		status.set(DataComponents.CUSTOM_NAME, Component.literal(message));
		display.setItem(slot, status);
	}

	private void clearArming() {
		armedTarget = null;
		armedReversalAction = null;
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
