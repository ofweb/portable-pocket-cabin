package dev.portablepocketcabin;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.resources.Identifier;
import java.util.*;

final class CabinEnchantingMenu extends AbstractContainerMenu {
	static final MenuType<CabinEnchantingMenu> TYPE = Registry.register(BuiltInRegistries.MENU,
		PortablePocketCabin.id("enchantment_library"), new ExtendedMenuType<>(CabinEnchantingMenu::new, UUIDUtil.STREAM_CODEC));
	static final int LEARN = 0, APPLY = 1, BACK = 2, CONFIRM = 3, CHOICE_BASE = 100;
	private final UUID cabinId;
	private final ServerPlayer serverPlayer;
	private final SimpleContainer display = new SimpleContainer(3);
	private final SimpleContainerData data = new SimpleContainerData(4);
	private List<CabinEnchantmentLibrary.Source> sources = List.of();
	private List<Choice> choices = List.of();
	private CabinEnchantmentLibrary.Source selected;
	private CabinEnchantmentLibrary.Plan plan;
	private boolean learning = true;
	private String status = "Choose an item to sacrifice.";
	private record Choice(Identifier enchantment, int level, String title) { }

	CabinEnchantingMenu(int id, Inventory inventory, UUID cabinId) {
		super(TYPE, id);
		this.cabinId = cabinId;
		serverPlayer = inventory.player instanceof ServerPlayer player ? player : null;
		addSlot(new DisplaySlot(display, 0, CabinEnchantingLayout.SOURCE_X, CabinEnchantingLayout.PREVIEW_Y));
		addSlot(new DisplaySlot(display, 1, CabinEnchantingLayout.DETAIL_X, CabinEnchantingLayout.PREVIEW_Y));
		addSlot(new DisplaySlot(display, 2, -1000, -1000));
		addStandardInventorySlots(inventory, CabinEnchantingLayout.INVENTORY_X, CabinEnchantingLayout.INVENTORY_Y);
		addDataSlots(data);
		if (serverPlayer != null) reset();
	}
	static void register() { }
	static InteractionResult open(ServerPlayer player, CabinRecord cabin) {
		if (!CabinEnchantmentLibrary.valid(player, cabin)) {
			player.sendSystemMessage(Component.literal("Only owners and residents near the installed table may use the library."));
			return InteractionResult.FAIL;
		}
		return player.openMenu(new ExtendedMenuProvider<UUID>() {
			public UUID getScreenOpeningData(ServerPlayer actor) { return cabin.uuid(); }
			public Component getDisplayName() { return Component.literal("Enchantment Library"); }
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player actor) { return new CabinEnchantingMenu(id, inventory, cabin.uuid()); }
		}).isPresent() ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
	}
	private CabinRecord cabin() { return CabinRegistry.get(serverPlayer.level().getServer()).find(cabinId).orElse(null); }
	boolean learning() { return data.get(0) != 0; }
	boolean choosingItem() { return data.get(1) == 0; }
	boolean ready() { return data.get(2) != 0; }
	int tier() { return data.get(3); }
	String status() { return display.getItem(2).getHoverName().getString(); }
	private void reset() {
		selected = null; plan = null; choices = List.of();
		var cabin = cabin();
		sources = cabin == null ? List.of() : CabinEnchantmentLibrary.sources(cabin, CabinEnchantmentLibrary.inventory(serverPlayer), learning, serverPlayer.registryAccess());
		sync();
	}
	private void select(CabinEnchantmentLibrary.Source source) {
		selected = source; plan = null;
		var result = new ArrayList<Choice>();
		var library = cabin().upgrades().enchanting();
		var enchantments = serverPlayer.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		for (var holder : enchantments.listElements().toList()) {
			var key = holder.key().identifier();
			if (!key.getNamespace().equals("minecraft")) continue;
			int limit = learning ? CabinEnchantmentLibrary.enchantments(source.stack()).getLevel(holder)
				: Math.min(library.limit(), Math.min(holder.value().getMaxLevel(), library.known().getOrDefault(key, 0)));
			for (int level = learning ? limit : 1; level > 0 && level <= limit; level++) {
				String title = Enchantment.getFullname(holder, level).getString() + (holder.is(EnchantmentTags.CURSE) ? " (Curse)" : "");
				result.add(new Choice(key, level, title));
			}
		}
		result.sort(Comparator.comparing(Choice::title)); choices = List.copyOf(result);
		status = choices.isEmpty() ? "No known enchantments available at this tier." : "Choose an enchantment and level.";
		sync();
	}
	private void sync() {
		data.set(0, learning ? 1 : 0); data.set(1, selected == null ? 0 : 1);
		data.set(2, plan != null && plan.ready() ? 1 : 0);
		data.set(3, cabin() == null ? 0 : cabin().upgrades().enchanting().level());
		display.setItem(0, selected == null ? ItemStack.EMPTY : selected.stack());
		var detail = new ItemStack(Items.BOOK);
		detail.set(DataComponents.CUSTOM_NAME, Component.literal(plan == null ? "Select an enchantment" : status));
		var lines = new ArrayList<Component>();
		if (selected != null) lines.add(Component.literal("Source: " + selected.label()));
		if (plan != null) {
			var holder = serverPlayer.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(plan.enchantment()).orElseThrow();
			lines.add(Enchantment.getFullname(holder, plan.level()));
			if (holder.is(EnchantmentTags.CURSE)) lines.add(Component.literal("Curse: applies only when you confirm."));
			if (learning) {
				lines.add(Component.literal("Destroys one source item and ALL its enchantments."));
				lines.add(Component.literal("Ordinary books required: " + Math.max(0, plan.level() - plan.beforeLibrary().known().getOrDefault(plan.enchantment(), 0))));
				lines.add(Component.literal("No enchanted book is returned."));
			} else {
				int amount = plan.level() * (1 + CabinEnchantmentLibrary.enchantments(selected.stack()).size());
				lines.add(Component.literal("Required: " + amount + " amethyst shards, " + (amount * 2) + " lapis lazuli"));
				lines.add(Component.literal("Result returns to " + selected.label().toLowerCase(Locale.ROOT) + "."));
			}
			plan.costs().forEach(cost -> lines.add(Component.literal(cost.description())));
			lines.add(Component.literal(plan.ready() ? "Compatible and ready. Experience stays unchanged." : plan.reason()));
		}
		detail.set(DataComponents.LORE, new ItemLore(lines.stream().<Component>map(line -> line.copy()
			.withStyle(net.minecraft.ChatFormatting.GRAY).withStyle(style -> style.withItalic(false))).toList())); display.setItem(1, detail);
		var message = new ItemStack(Items.PAPER); message.set(DataComponents.CUSTOM_NAME, Component.literal(status)); display.setItem(2, message);
		List<CabinStationPayload.Entry> entries = new ArrayList<>();
		if (selected == null) for (var source : sources) entries.add(new CabinStationPayload.Entry(
			source.label() + ": " + source.stack().getHoverName().getString(), source.stack(), true, false));
		else for (var choice : choices) entries.add(new CabinStationPayload.Entry(choice.title(), new ItemStack(Items.ENCHANTED_BOOK), true,
			plan != null && plan.enchantment().equals(choice.enchantment()) && plan.level() == choice.level()));
		CabinStationPayload.send(serverPlayer, containerId, entries);
	}
	@Override public boolean clickMenuButton(Player actor, int button) {
		if (actor != serverPlayer || !stillValid(actor)) return false;
		if (button == LEARN || button == APPLY) {
			learning = button == LEARN; status = learning ? "Choose an item to sacrifice." : "Choose an item to enchant."; reset();
		} else if (button == BACK) { status = learning ? "Choose an item to sacrifice." : "Choose an item to enchant."; reset(); }
		else if (button == CONFIRM) {
			status = CabinEnchantmentLibrary.confirm(serverPlayer, cabinId, plan); reset();
		} else if (button >= CHOICE_BASE) {
			int index = button - CHOICE_BASE;
			if (selected == null) {
				if (index >= sources.size()) return false;
				select(sources.get(index));
			} else {
				if (index >= choices.size()) return false;
				var choice = choices.get(index);
				plan = CabinEnchantmentLibrary.preview(cabin(), CabinEnchantmentLibrary.inventory(serverPlayer), selected,
					choice.enchantment(), choice.level(), learning, serverPlayer.registryAccess());
				status = plan.ready() ? (learning ? "Confirm sacrifice: " : "Confirm application: ") + choice.title() : plan.reason();
				sync();
			}
		} else return false;
		broadcastChanges(); return true;
	}
	@Override public void clicked(int slot, int button, ContainerInput input, Player actor) {
		if (actor != serverPlayer || !stillValid(actor) || slot < 3 || slot >= slots.size()) return;
		var inventorySlot = slots.get(slot).getContainerSlot();
		var source = sources.stream().filter(value -> !value.storage() && value.slot() == inventorySlot).findFirst().orElse(null);
		if (source != null) { select(source); broadcastChanges(); }
	}
	@Override public ItemStack quickMoveStack(Player actor, int slot) { return ItemStack.EMPTY; }
	@Override public boolean stillValid(Player actor) { return serverPlayer == null || actor == serverPlayer && CabinEnchantmentLibrary.valid(serverPlayer, cabin()); }
	private static final class DisplaySlot extends Slot {
		DisplaySlot(net.minecraft.world.Container container, int slot, int x, int y) { super(container, slot, x, y); }
		@Override public boolean mayPickup(Player player) { return false; }
		@Override public boolean mayPlace(ItemStack stack) { return false; }
	}
}
