package dev.portablepocketcabin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** A cabin book describes the upgrades it reveals through the shared installer. */
final class CabinBookItem extends Item {

	enum Type { STORAGE, CRAFTING, GREENHOUSE, ENCHANTING }

	private final Type type;
	CabinBookItem(Properties properties, Type type) {
		super(properties.stacksTo(64)
			.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
			.component(DataComponents.LORE, new ItemLore(tooltip(type))));
		this.type = type;
	}

	static CabinBookItem from(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof CabinBookItem book ? book : null;
	}

	List<Component> upgrades() {
		return upgrades(type);
	}

	boolean revealed(CabinUpgradeState state) {
		return switch (type) {
			case ENCHANTING -> state.enchanting().revealed();
			case STORAGE -> state.storage().revealed();
			case CRAFTING -> state.crafting().revealed();
			case GREENHOUSE -> state.greenhouse().revealed();
		};
	}

	CabinUpgradeState reveal(CabinUpgradeState state) {
		return switch (type) {
			case ENCHANTING -> state.withEnchanting(state.enchanting().reveal());
			case STORAGE -> state.withStorage(state.storage().reveal());
			case CRAFTING -> state.withCrafting(state.crafting().reveal());
			case GREENHOUSE -> state.withGreenhouse(state.greenhouse().reveal());
		};
	}

	private static List<Component> tooltip(Type type) {
		var lines = new ArrayList<Component>();
		lines.add(Component.translatable("book.portable_pocket_cabin.reveals"));
		lines.addAll(upgrades(type));
		lines.add(Component.translatable("book.portable_pocket_cabin.install_hint"));
		return List.copyOf(lines);
	}

	private static List<Component> upgrades(Type type) {
		return switch (type) {
			case ENCHANTING -> java.util.stream.IntStream.rangeClosed(1, 5)
				.<Component>mapToObj(level -> Component.translatable("book.portable_pocket_cabin.enchanting_" + level)).toList();
			case STORAGE -> List.of(Component.translatable("book.portable_pocket_cabin.storage"));
			case CRAFTING -> List.of(
				Component.translatable("book.portable_pocket_cabin.crafting"),
				Component.translatable("book.portable_pocket_cabin.stonecutting"),
				Component.translatable("book.portable_pocket_cabin.smithing"));
			case GREENHOUSE -> List.of(Component.translatable("book.portable_pocket_cabin.greenhouse"));
		};
	}
}
