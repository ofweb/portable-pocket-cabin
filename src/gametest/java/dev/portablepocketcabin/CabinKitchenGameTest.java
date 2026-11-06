package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class CabinKitchenGameTest {
	@GameTest
	public void kitchenPurchaseCommitsOneRoomOnTheEastCorridor(GameTestHelper helper) {
		var registry = new CabinRegistry();
		UUID owner = UUID.randomUUID();
		var cabin = registry.create(owner);
		registry.beginDeployment(cabin.uuid(), owner, new CabinExterior(Level.OVERWORLD, new BlockPos(0, 100, 0), Direction.NORTH));
		registry.markInteriorGenerated(cabin.uuid());
		registry.finishDeployment(cabin.uuid());
		var definitions = CabinUpgradeDefinitions.current();
		var attunement = new WorldAttunement(1, CabinPalette.DEFAULT.walls().profileId());
		cabin = registry.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(CabinUpgradeCatalog.groups(cabin, attunement, definitions).stream().noneMatch(g -> g.title().equals("Kitchen")),
			"The kitchen stays hidden until its book is installed");
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withKitchen(CabinKitchenState.EMPTY.reveal()));
		var offer = kitchenOffer(cabin, attunement, definitions);
		helper.assertTrue(!offer.prerequisite().isEmpty(), "The kitchen needs a 5x5 main room");
		cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 3, 5, 21);
		offer = kitchenOffer(cabin, attunement, definitions);
		helper.assertTrue(offer.prerequisite().isEmpty() && offer.target().equals(CabinUpgradeState.Target.kitchen()),
			"A 5x5 main room unlocks the kitchen");
		var requirements = CabinUpgradeCatalog.kitchenRequirements(cabin);
		helper.assertTrue(requirements.stream().anyMatch(r -> r.itemId().equals(BuiltInRegistries.ITEM.getKey(Items.OAK_LOG)) && r.count() == 16)
			&& requirements.size() == 3, "The kitchen costs saved wall logs, copper, and amethyst");

		var target = CabinUpgradeState.Target.kitchen();
		var stacks = requirements.stream().map(r -> new ItemStack(BuiltInRegistries.ITEM.getOptional(r.itemId()).orElseThrow(), r.count())).toList();
		var operation = new CabinUpgradeState.Installation(UUID.randomUUID(), target, 0, 1);
		registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(new CabinUpgradeState.Fund(target, requirements, stacks)).withInstallation(operation));
		var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		registry = CabinRegistry.CODEC.parse(ops, CabinRegistry.CODEC.encodeStart(ops, registry).getOrThrow()).getOrThrow();
		cabin = registry.completeUpgradeInstallation(cabin.uuid(), operation.operationId());
		helper.assertTrue(cabin.upgrades().kitchen().level() == 1 && cabin.upgrades().fund(target).isEmpty()
			&& cabin.progression().rooms().stream().filter(room -> room.type().equals(CabinKitchen.TYPE)).count() == 1
			&& cabin.progression().corridors().contains(CabinCorridor.EAST), "Recovery commits one kitchen room and the east corridor");
		helper.assertTrue(kitchenOffer(cabin, attunement, definitions).complete(), "An installed kitchen shows as installed");

		var space = CabinKitchen.space(cabin);
		helper.assertTrue(space.maximum().getX() - space.minimum().getX() - 1 == 5 && space.maximum().getZ() - space.minimum().getZ() - 1 == 5
			&& space.maximum().getY() - space.minimum().getY() - 1 == 3, "The kitchen has a 5x5 floor and three blocks of clearance");
		helper.assertTrue(space.minimum().equals(new BlockPos(5, 0, 2)) && space.entrance().equals(new BlockPos(8, 1, 2)),
			"The kitchen sits south of the east corridor with its entrance centered on the north wall");
		BlockPos center = PocketDimension.cellCenter(cabin.cellIndex());
		helper.assertTrue(CabinKitchen.contains(cabin, center.offset(6, 1, 3)) && CabinKitchen.contains(cabin, center.offset(10, 3, 7)),
			"The floor and clearance belong to the kitchen");
		helper.assertTrue(!CabinKitchen.contains(cabin, center.offset(5, 1, 3)) && !CabinKitchen.contains(cabin, center.offset(6, 0, 3))
			&& !CabinKitchen.contains(cabin, center.offset(0, 1, 0)), "Walls, floor blocks, and the main room do not");
		var installed = cabin;
		helper.assertTrue(CabinKitchen.lamps(installed).keySet().stream().allMatch(pos -> CabinKitchen.contains(installed, pos)),
			"The lamps hang inside the kitchen");

		cabin = registry.expandGeneralSpace(cabin.uuid(), owner, 5, 7, 21);
		helper.assertTrue(CabinKitchen.space(cabin).minimum().equals(new BlockPos(6, 0, 2)),
			"The kitchen moves east with the main room's east wall");
		helper.succeed();
	}

	private static CabinUpgradeCatalog.Offer kitchenOffer(CabinRecord cabin, WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions) {
		var group = CabinUpgradeCatalog.groups(cabin, attunement, definitions).stream().filter(g -> g.title().equals("Kitchen")).findFirst().orElseThrow();
		return group.panels().getFirst();
	}
}
