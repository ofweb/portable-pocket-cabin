package dev.portablepocketcabin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative general-space upgrades exposed by the interior controller. */
final class CabinUpgrades {
	record ResolvedIngredient(Item item, int count) {
	}

	private CabinUpgrades() {
	}

	static InteractionResult useController(ServerPlayer player, CabinRecord observed) {
		ServerLevel pocket = (ServerLevel) player.level();
		CabinRegistry registry = CabinRegistry.get(pocket.getServer());
		CabinRecord cabin = registry.find(observed.uuid()).orElse(null);
		if (cabin == null || cabin.lifecycle() != CabinLifecycle.DEPLOYED || !cabin.interiorGenerated()) {
			player.sendSystemMessage(Component.literal("That cabin controller is not active."));
			return InteractionResult.FAIL;
		}

		CabinUpgradeDefinitions.Definitions definitions = CabinUpgradeDefinitions.current();
		WorldAttunement attunement;
		try {
			attunement = resolveAttunement(registry, pocket, definitions);
		} catch (IllegalStateException exception) {
			player.sendSystemMessage(Component.literal(exception.getMessage()));
			return InteractionResult.FAIL;
		}

		if (!player.isShiftKeyDown()) {
			showStatus(player, cabin, attunement, definitions);
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!cabin.owner().equals(player.getUUID())) {
			player.sendSystemMessage(Component.literal("Only the cabin owner may purchase an expansion."));
			return InteractionResult.FAIL;
		}
		return expand(player, cabin, attunement, definitions);
	}

	private static WorldAttunement resolveAttunement(
		CabinRegistry registry, ServerLevel pocket, CabinUpgradeDefinitions.Definitions definitions
	) {
		WorldAttunement current = registry.worldAttunement().orElse(null);
		if (current != null) {
			if (!definitions.isValid(current)) {
				throw new IllegalStateException("The saved world attunement is invalid under the loaded definitions; "
					+ "an operator must validate the datapacks before upgrades can continue.");
			}
			return current;
		}
		WorldAttunement resolved = registry.resolveWorldAttunement(
			definitions.resolve(pocket.getServer().overworld().getSeed())
		);
		CabinRegistry.flush(pocket.getServer());
		return resolved;
	}

	private static InteractionResult expand(
		ServerPlayer player, CabinRecord cabin, WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		int currentSize = cabin.progression().generalSize();
		if (currentSize >= definitions.maximumGeneralSize()) {
			player.sendSystemMessage(Component.literal("This cabin has reached the " + currentSize + "x"
				+ currentSize + " general-space limit."));
			return InteractionResult.FAIL;
		}
		int targetSize = currentSize + 1;
		CabinUpgradeDefinitions.Expansion expansion = definitions.expansion(targetSize);
		List<ResolvedIngredient> ingredients = resolve(expansion, attunement);
		String missing = missing(player.getInventory(), ingredients);
		if (!player.isCreative() && !missing.isEmpty()) {
			player.sendSystemMessage(Component.literal("Expansion requires: " + describe(ingredients)
				+ ". Missing: " + missing + "."));
			return InteractionResult.FAIL;
		}

		ServerLevel pocket = (ServerLevel) player.level();
		PocketDimension.ExpansionCheck check = PocketDimension.validateExpansion(
			pocket, cabin.cellIndex(), currentSize, targetSize
		);
		if (!check.valid()) {
			player.sendSystemMessage(Component.literal(check.message() + ". No materials were consumed."));
			return InteractionResult.FAIL;
		}

		try {
			PocketDimension.expandGeneralSpace(pocket, cabin, targetSize);
			CabinRecord updated = CabinRegistry.get(pocket.getServer()).expandGeneralSpace(
				cabin.uuid(), player.getUUID(), currentSize, targetSize, definitions.maximumGeneralSize()
			);
			if (!player.isCreative()) {
				consume(player.getInventory(), ingredients);
			}
			CabinRegistry.flush(pocket.getServer());
			CabinWindows.refresh(pocket.getServer(), updated);
		} catch (IllegalStateException exception) {
			player.sendSystemMessage(Component.literal(exception.getMessage()));
			return InteractionResult.FAIL;
		}

		player.sendSystemMessage(Component.literal("Cabin general space expanded to "
			+ targetSize + "x" + targetSize + "."));
		return InteractionResult.SUCCESS_SERVER;
	}

	static List<ResolvedIngredient> resolve(
		CabinUpgradeDefinitions.Expansion expansion, WorldAttunement attunement
	) {
		if (expansion == null) {
			throw new IllegalStateException("No upgrade definition exists for the requested size");
		}
		List<ResolvedIngredient> result = new ArrayList<>();
		for (CabinUpgradeDefinitions.Ingredient ingredient : expansion.ingredients()) {
			result.add(new ResolvedIngredient(ingredient.resolve(attunement), ingredient.count()));
		}
		return List.copyOf(result);
	}

	private static void showStatus(
		ServerPlayer player, CabinRecord cabin, WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions
	) {
		int size = cabin.progression().generalSize();
		String base = "Cabin space: " + size + "x" + size + " / "
			+ definitions.maximumGeneralSize() + "x" + definitions.maximumGeneralSize()
			+ "; world-attuned wood: " + attunement.woodProfile();
		if (size >= definitions.maximumGeneralSize()) {
			player.sendSystemMessage(Component.literal(base + "; maximum reached."));
			return;
		}
		List<ResolvedIngredient> ingredients = resolve(definitions.expansion(size + 1), attunement);
		player.sendSystemMessage(Component.literal(base + "; next expansion: " + describe(ingredients)
			+ ". Sneak-use this controller to purchase."));
	}

	private static String missing(Inventory inventory, List<ResolvedIngredient> requirements) {
		List<String> result = new ArrayList<>();
		for (ResolvedIngredient requirement : requirements) {
			int available = count(inventory, requirement.item());
			if (available < requirement.count()) {
				result.add((requirement.count() - available) + " " + id(requirement.item()));
			}
		}
		return String.join(", ", result);
	}

	private static int count(Inventory inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static void consume(Inventory inventory, List<ResolvedIngredient> requirements) {
		for (ResolvedIngredient requirement : requirements) {
			int remaining = requirement.count();
			for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (!stack.is(requirement.item())) {
					continue;
				}
				int removed = Math.min(remaining, stack.getCount());
				stack.shrink(removed);
				remaining -= removed;
			}
			if (remaining != 0) {
				throw new IllegalStateException("Upgrade inventory changed before materials were consumed");
			}
		}
	}

	private static String describe(List<ResolvedIngredient> ingredients) {
		return ingredients.stream()
			.map(value -> value.count() + " " + id(value.item()))
			.collect(java.util.stream.Collectors.joining(", "));
	}

	private static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}
}
