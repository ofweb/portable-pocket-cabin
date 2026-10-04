package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import java.util.*;

public final class CabinEnchantingGameTest {
	static CabinRecord cabin(CabinRegistry registry) {
		UUID owner = UUID.randomUUID();
		var cabin = registry.create(owner);
		registry.beginDeployment(cabin.uuid(), owner, new CabinExterior(Level.OVERWORLD, new BlockPos(0, 100, 0), Direction.NORTH));
		registry.markInteriorGenerated(cabin.uuid()); registry.finishDeployment(cabin.uuid());
		cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 3, 5, 21);
		return registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withEnchanting(CabinEnchantingState.EMPTY.reveal()));
	}
	static List<ItemStack> inventory(ItemStack... stacks) {
		var inventory = new ArrayList<ItemStack>();
		for (int i = 0; i < 41; i++) inventory.add(i < stacks.length ? stacks[i] : ItemStack.EMPTY);
		return inventory;
	}
	static Identifier id(String value) { return Identifier.withDefaultNamespace(value); }
	static CabinRecord withLibrary(CabinRegistry registry, CabinRecord cabin, int tier, Map<Identifier, Integer> known) {
		return registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withEnchanting(new CabinEnchantingState(true, tier, known)));
	}

	@GameTest public void tierCostsKnowledgeAndPersistence(GameTestHelper helper) {
		var registry = new CabinRegistry();
		var cabin = cabin(registry);
		var book = CabinBookItem.from(new ItemStack(CabinItems.ENCHANTING_BOOK));
		helper.assertTrue(book.upgrades().size() == 5 && book.revealed(cabin.upgrades()), "The room book reveals five purchases");
		var definitions = CabinUpgradeDefinitions.current();
		var attunement = new WorldAttunement(1, CabinPalette.DEFAULT.walls().profileId());
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withEnchanting(cabin.upgrades().enchanting().learn(id("sharpness"), 5)));
		for (int tier = 1; tier <= 5; tier++) {
			var offer = CabinUpgradeCatalog.groups(cabin, attunement, definitions).stream().filter(g -> g.title().equals("Enchanting")).findFirst().orElseThrow();
			helper.assertTrue(offer.panels().size() == 1 && offer.panels().getFirst().target().equals(CabinUpgradeState.Target.enchanting(tier)), "Only the next tier is offered");
			var requirements = CabinUpgradeCatalog.enchantingRequirements(cabin, tier);
			if (tier == 1) helper.assertTrue(requirements.stream().anyMatch(r -> r.itemId().equals(id("oak_log")) && r.count() == 16), "Installation uses saved wall wood");
			var stacks = requirements.stream().map(r -> new ItemStack(BuiltInRegistries.ITEM.getOptional(r.itemId()).orElseThrow(), r.count())).toList();
			var operation = new CabinUpgradeState.Installation(UUID.randomUUID(), CabinUpgradeState.Target.enchanting(tier), tier - 1, tier);
			registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(new CabinUpgradeState.Fund(operation.target(), requirements, stacks)).withInstallation(operation));
			var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
			registry = CabinRegistry.CODEC.parse(ops, CabinRegistry.CODEC.encodeStart(ops, registry).getOrThrow()).getOrThrow();
			cabin = registry.completeUpgradeInstallation(cabin.uuid(), operation.operationId());
			helper.assertTrue(cabin.upgrades().enchanting().known().get(id("sharpness")) == 5, "Room upgrades preserve stronger knowledge");
			helper.assertTrue(cabin.progression().rooms().size() == 1 && cabin.progression().corridors().contains(CabinCorridor.NORTH), "Installation commits exactly one connected room");
		}
		helper.assertTrue(cabin.upgrades().enchanting().limit() == 255, "Final tier removes the room cap");
		var space = CabinEnchanting.space(cabin);
		helper.assertTrue(space.maximum().subtract(space.minimum()).equals(new BlockPos(6, 4, 6)), "Room retains a 5x5 floor and three clear blocks of height");
		helper.assertTrue(space.minimum().getZ() < CabinCrafting.space(cabin).minimum().getZ() - 6, "Enchanting branches beyond crafting");
		helper.assertTrue(CabinEnchanting.stations(cabin, 5).size() == 19, "Five tiers supply three stations and sixteen shelves");
		helper.succeed();
	}

	@GameTest public void learningCostsDeltaAndDoesNotCapKnowledge(GameTestHelper helper) {
		var registry = new CabinRegistry(); var cabin = withLibrary(registry, cabin(registry), 1, Map.of());
		var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
		var looting = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
		var sword = new ItemStack(Items.DIAMOND_SWORD); sword.enchant(sharpness, 3); sword.enchant(looting, 2);
		var inventory = inventory(sword, new ItemStack(Items.BOOK, 4));
		var source = new CabinEnchantmentLibrary.Source(false, 0, sword);
		var plan = CabinEnchantmentLibrary.preview(cabin, inventory, source, id("sharpness"), 3, true, helper.getLevel().registryAccess());
		helper.assertTrue(plan.ready() && plan.afterLibrary().known().get(id("sharpness")) == 3
			&& !plan.afterLibrary().known().containsKey(id("looting")) && plan.afterInventory().get(0).isEmpty()
			&& plan.afterInventory().get(1).getCount() == 1 && plan.result().isEmpty(), "One sacrifice teaches one selected level III for three books, above room cap");
		helper.assertTrue(inventory.get(0).getCount() == 1 && inventory.get(1).getCount() == 4, "Preview leaves inputs unchanged");
		cabin = withLibrary(registry, cabin, 1, Map.of(id("sharpness"), 2));
		plan = CabinEnchantmentLibrary.preview(cabin, inventory, source, id("sharpness"), 3, true, helper.getLevel().registryAccess());
		helper.assertTrue(plan.ready() && plan.afterInventory().get(1).getCount() == 3, "Learning III from II costs one book");
		cabin = withLibrary(registry, cabin, 1, Map.of(id("sharpness"), 3));
		plan = CabinEnchantmentLibrary.preview(cabin, inventory, source, id("sharpness"), 3, true, helper.getLevel().registryAccess());
		helper.assertTrue(!plan.ready(), "Known levels never consume another sacrifice");
		var named = new ItemStack(Items.BOOK, 64); named.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
		cabin = withLibrary(registry, cabin, 1, Map.of());
		plan = CabinEnchantmentLibrary.preview(cabin, inventory(sword, named), source, id("sharpness"), 3, true, helper.getLevel().registryAccess());
		helper.assertTrue(!plan.ready(), "Named books are not automatic ingredients");
		helper.succeed();
	}

	@GameTest public void applicationPreservesDataAndChecksRules(GameTestHelper helper) {
		var registry = new CabinRegistry(); var cabin = withLibrary(registry, cabin(registry), 3, Map.of(id("sharpness"), 5, id("smite"), 5, id("mending"), 1));
		var access = helper.getLevel().registryAccess(); var ench = access.lookupOrThrow(Registries.ENCHANTMENT);
		var sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.set(DataComponents.CUSTOM_NAME, Component.literal("Family sword"));
		sword.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Keep this.")))); sword.setDamageValue(123);
		sword.enchant(ench.getOrThrow(Enchantments.SHARPNESS), 1); sword.enchant(ench.getOrThrow(Enchantments.UNBREAKING), 2);
		var inventory = inventory(sword, new ItemStack(Items.AMETHYST_SHARD, 64), new ItemStack(Items.LAPIS_LAZULI, 64));
		var source = new CabinEnchantmentLibrary.Source(false, 0, sword);
		var plan = CabinEnchantmentLibrary.preview(cabin, inventory, source, id("sharpness"), 3, false, access);
		helper.assertTrue(plan.ready() && plan.costs().getFirst().total() == 9 && plan.costs().get(1).total() == 18, "Raising one of two enchantments pays full level times three");
		var result = plan.result();
		helper.assertTrue(result.getDamageValue() == 123 && result.getHoverName().equals(sword.getHoverName())
			&& result.get(DataComponents.LORE).equals(sword.get(DataComponents.LORE))
			&& EnchantmentHelper.getItemEnchantmentLevel(ench.getOrThrow(Enchantments.UNBREAKING), result) == 2, "Unrelated components and enchantments survive");
		helper.assertTrue(!CabinEnchantmentLibrary.preview(cabin, inventory, source, id("smite"), 2, false, access).ready(), "Exclusive enchantments reject without consumption");
		helper.assertTrue(!CabinEnchantmentLibrary.preview(cabin, inventory, source, id("sharpness"), 4, false, access).ready(), "Room tier caps application");
		cabin = withLibrary(registry, cabin, 5, cabin.upgrades().enchanting().known());
		helper.assertTrue(CabinEnchantmentLibrary.preview(cabin, inventory, source, id("sharpness"), 5, false, access).ready(), "Tier V uses known normal maximum");
		helper.assertTrue(!CabinEnchantmentLibrary.preview(cabin, inventory, source, id("mending"), 2, false, access).ready(), "Normal enchantment maximum remains enforced");
		var pick = new ItemStack(Items.DIAMOND_PICKAXE);
		helper.assertTrue(!CabinEnchantmentLibrary.preview(cabin, inventory(pick), new CabinEnchantmentLibrary.Source(false, 0, pick), id("sharpness"), 1, false, access).ready(), "Item eligibility is enforced");
		helper.succeed();
	}

	@GameTest public void booksStoragePriorityAndResultSpace(GameTestHelper helper) {
		var registry = new CabinRegistry(); var cabin = withLibrary(registry, cabin(registry), 5, Map.of(id("mending"), 1, id("unbreaking"), 3));
		var access = helper.getLevel().registryAccess(); var ench = access.lookupOrThrow(Registries.ENCHANTMENT);
		var books = new ItemStack(Items.BOOK, 2);
		var inventory = inventory(books, new ItemStack(Items.AMETHYST_SHARD, 2), new ItemStack(Items.LAPIS_LAZULI, 2));
		var storage = new CabinStorageState(true, 1, List.of(new ItemStack(Items.AMETHYST_SHARD, 2), new ItemStack(Items.LAPIS_LAZULI, 2)));
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(storage));
		var plan = CabinEnchantmentLibrary.preview(cabin, inventory, new CabinEnchantmentLibrary.Source(false, 0, books), id("mending"), 1, false, access);
		helper.assertTrue(plan.ready() && plan.result().is(Items.ENCHANTED_BOOK)
			&& plan.result().get(DataComponents.STORED_ENCHANTMENTS).getLevel(ench.getOrThrow(Enchantments.MENDING)) == 1
			&& plan.afterInventory().get(0).getCount() == 1 && plan.afterInventory().get(1).getCount() == 2
			&& plan.costs().getFirst().storage() == 1 && plan.costs().get(1).storage() == 2, "Only one book changes; storage fills before inventory");
		var stored = plan.result();
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withStorage(storage.withStacks(List.of(stored, new ItemStack(Items.AMETHYST_SHARD, 4), new ItemStack(Items.LAPIS_LAZULI, 8)))));
		plan = CabinEnchantmentLibrary.preview(cabin, inventory(), new CabinEnchantmentLibrary.Source(true, 0, stored), id("unbreaking"), 2, false, access);
		helper.assertTrue(plan.ready() && plan.costs().getFirst().total() == 4 && plan.afterStorage().stacks().getFirst().is(Items.ENCHANTED_BOOK)
			&& plan.result().get(DataComponents.STORED_ENCHANTMENTS).size() == 2, "Stored book enchantments count and the result returns to storage");
		var full = inventory(books);
		for (int i = 1; i < 36; i++) full.set(i, new ItemStack(Items.STONE, 64));
		plan = CabinEnchantmentLibrary.preview(cabin, full, new CabinEnchantmentLibrary.Source(false, 0, books), id("mending"), 1, false, access);
		helper.assertTrue(!plan.ready() && plan.reason().contains("No room"), "A full inventory rejects a split result before consumption");
		helper.succeed();
	}
	@GameTest public void everyVanillaEnchantmentWorksOnBooks(GameTestHelper helper) {
		var registry = new CabinRegistry(); var cabin = cabin(registry);
		var access = helper.getLevel().registryAccess();
		for (var holder : access.lookupOrThrow(Registries.ENCHANTMENT).listElements().toList()) {
			var key = holder.key().identifier();
			if (!key.getNamespace().equals("minecraft")) continue;
			cabin = withLibrary(registry, cabin, 5, Map.of(key, holder.value().getMaxLevel()));
			var book = new ItemStack(Items.BOOK);
			var plan = CabinEnchantmentLibrary.preview(cabin, inventory(book, new ItemStack(Items.AMETHYST_SHARD, 64), new ItemStack(Items.LAPIS_LAZULI, 64)),
				new CabinEnchantmentLibrary.Source(false, 0, book), key, holder.value().getMaxLevel(), false, access);
			helper.assertTrue(plan.ready(), "Vanilla enchantment is manually available on a book: " + key + " " + plan.reason());
			var other = withLibrary(registry, cabin, 1, Map.of());
			var learned = CabinEnchantmentLibrary.preview(other, inventory(plan.result(), new ItemStack(Items.BOOK, 64)),
				new CabinEnchantmentLibrary.Source(false, 0, plan.result()), key, holder.value().getMaxLevel(), true, access);
			helper.assertTrue(learned.ready() && learned.afterLibrary().known().get(key) == holder.value().getMaxLevel(), "Created books can teach a different library: " + key);
		}
		helper.succeed();
	}

}
