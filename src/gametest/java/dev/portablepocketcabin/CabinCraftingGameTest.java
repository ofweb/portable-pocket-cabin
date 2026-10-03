package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

public final class CabinCraftingGameTest {
	@GameTest
	public void bookOffersCostsAndPersistedLevels(GameTestHelper helper) {
		var registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		var cabin = registry.create(owner);
		registry.beginDeployment(cabin.uuid(), owner, new CabinExterior(Level.OVERWORLD, new BlockPos(0,100,0), Direction.NORTH));
		registry.markInteriorGenerated(cabin.uuid()); registry.finishDeployment(cabin.uuid());
		cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 3, 5, 21);
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withCrafting(CabinCraftingState.EMPTY.reveal()));
		var definitions = CabinUpgradeDefinitions.current();
		var attunement = new WorldAttunement(1, CabinPalette.DEFAULT.walls().profileId());
		for (int level = 1; level <= 3; level++) {
			cabin = registry.find(cabin.uuid()).orElseThrow();
			var target = CabinUpgradeState.Target.crafting(level);
			var group = CabinUpgradeCatalog.groups(cabin, attunement, definitions).stream().filter(g -> g.title().equals("Crafting")).findFirst().orElseThrow();
			helper.assertTrue(group.panels().size() == 1 && group.panels().getFirst().target().equals(target), "Only the next crafting purchase appears");
			var requirements = CabinUpgradeCatalog.craftingRequirements(cabin, level);
			if (level == 1) helper.assertTrue(requirements.stream().anyMatch(r -> r.itemId().equals(BuiltInRegistries.ITEM.getKey(Items.OAK_LOG)) && r.count() == 16), "Room cost uses saved wall logs");
			var stacks = requirements.stream().map(r -> new ItemStack(BuiltInRegistries.ITEM.getOptional(r.itemId()).orElseThrow(), r.count())).toList();
			var operation = new CabinUpgradeState.Installation(UUID.randomUUID(), target, level - 1, level);
			registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(new CabinUpgradeState.Fund(target, requirements, stacks)).withInstallation(operation));
			var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
			registry = CabinRegistry.CODEC.parse(ops, CabinRegistry.CODEC.encodeStart(ops, registry).getOrThrow()).getOrThrow();
			cabin = registry.completeUpgradeInstallation(cabin.uuid(), operation.operationId());
			helper.assertTrue(cabin.upgrades().crafting().level() == level && cabin.progression().rooms().size() == 1
				&& cabin.upgrades().fund(target).isEmpty(), "Recovery commits one room and one level");
		}
		var space = CabinCrafting.space(cabin);
		helper.assertTrue(space.maximum().getX() - space.minimum().getX() - 1 == 5
			&& space.maximum().getZ() - space.minimum().getZ() - 1 == 5, "Usable room remains 5x5");
		helper.assertTrue(CabinCrafting.stations(cabin, 1).size() == 3 && CabinCrafting.stations(cabin, 2).size() == 4
			&& CabinCrafting.stations(cabin, 3).size() == 5, "Levels add exactly their stations");
		helper.assertTrue(!CabinStation.ordinary(named(Items.OAK_PLANKS)) && CabinStation.ordinary(new ItemStack(Items.OAK_PLANKS)), "Automatic filling protects custom stacks");
		var group = CabinUpgradeCatalog.groups(cabin, attunement, definitions).stream().filter(g -> g.title().equals("Crafting")).findFirst().orElseThrow();
		helper.assertTrue(group.panels().getFirst().complete(), "Final level is fully upgraded");
		helper.succeed();
	}

	static ItemStack named(net.minecraft.world.item.Item item) {
		var stack = new ItemStack(item); stack.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake")); return stack;
	}
}
