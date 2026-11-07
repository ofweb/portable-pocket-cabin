package dev.portablepocketcabin;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
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

	@GameTest
	public void kitchenFillsFromStorageFirstWithFuelAndOverflow(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = new BlockPos(1, 2, 1);
		helper.setBlock(base, CookingBlocks.STOVE);
		helper.setBlock(base.above(), CookingBlocks.COOKING_POT);
		var stove = (StoveBlockEntity) level.getBlockEntity(helper.absolutePos(base));
		var pot = (CookingPotBlockEntity) level.getBlockEntity(helper.absolutePos(base.above()));
		pot.setItem(0, new ItemStack(Items.STICK));
		pot.fillWater();
		var soup = recipe(helper, "cooking/pot/vegetable_soup");
		var storage = new CabinStorageState(true, 1, List.of(new ItemStack(CookingItems.CHOPPED_CARROT, 2), new ItemStack(CookingItems.DICED_POTATO),
			new ItemStack(CookingItems.CHOPPED_BEETROOT), new ItemStack(Items.BOWL, 3), new ItemStack(Items.STICK, 8), new ItemStack(Items.OAK_PLANKS, 4)));
		var inventory = FakePlayer.get(level).getInventory();
		inventory.clearContent();
		inventory.setItem(0, new ItemStack(CookingItems.CHOPPED_CARROT));
		inventory.setItem(1, new ItemStack(Items.BOWL, 5));

		var plan = CookingFill.plan(pot, CookingStation.POT, soup, inventory, storage, stove, StoveBlockEntity.FUEL_SLOT).orElseThrow();
		helper.assertTrue(plan.station().get(0).is(CookingItems.CHOPPED_CARROT) && plan.station().get(CookingPotBlockEntity.CONTAINER_SLOT).getCount() == 3,
			"The pot gets the soup's vegetables and bowls");
		helper.assertTrue(count(plan.inventory(), CookingItems.CHOPPED_CARROT) == 1 && count(plan.inventory(), Items.BOWL) == 5,
			"Storage supplies the ingredients before the inventory");
		helper.assertTrue(count(plan.inventory(), Items.STICK) == 1, "The displaced stick goes to the inventory");
		helper.assertTrue(plan.heater().orElseThrow().get(StoveBlockEntity.FUEL_SLOT).is(Items.OAK_PLANKS)
			&& plan.heater().get().get(StoveBlockEntity.FUEL_SLOT).getCount() == 1, "One plank, not the bowls or sticks, covers one batch");
		var after = plan.storage().orElseThrow().stacks();
		helper.assertTrue(count(after, CookingItems.CHOPPED_CARROT) == 1 && count(after, Items.OAK_PLANKS) == 3 && count(after, Items.BOWL) == 0,
			"Storage gives up exactly what the fill uses");
		helper.assertTrue(count(plan.withdrawn(), Items.OAK_PLANKS) == 1 && count(plan.withdrawn(), Items.BOWL) == 3, "The plan records what storage gives");

		for (int slot = 0; slot < 36; slot++) inventory.setItem(slot, new ItemStack(Items.STONE, 64));
		plan = CookingFill.plan(pot, CookingStation.POT, soup, inventory, storage, stove, StoveBlockEntity.FUEL_SLOT).orElseThrow();
		helper.assertTrue(count(plan.storage().orElseThrow().stacks(), Items.STICK) == 9 && count(plan.deposited(), Items.STICK) == 1,
			"With a full inventory, the displaced stick goes to storage");
		helper.assertTrue(CookingFill.plan(pot, CookingStation.POT, soup, inventory, null, null, -1).isEmpty(),
			"Outside the kitchen, a full inventory still refuses the fill");

		stove.setItem(StoveBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
		inventory.clearContent();
		plan = CookingFill.plan(pot, CookingStation.POT, soup, inventory, storage, stove, StoveBlockEntity.FUEL_SLOT).orElseThrow();
		helper.assertTrue(plan.heater().isEmpty() && count(plan.storage().orElseThrow().stacks(), Items.OAK_PLANKS) == 4,
			"A stove that holds fuel gets none");
		inventory.clearContent();
		helper.succeed();
	}

	@GameTest
	public void kitchenFillsAPressFromStorage(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = new BlockPos(1, 2, 1);
		helper.setBlock(base, CookingBlocks.PRESS);
		var press = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(base));
		var juice = recipe(helper, "cooking/press/apple_juice");
		var storage = new CabinStorageState(true, 1, List.of(new ItemStack(CookingItems.CHOPPED_APPLE, 4), new ItemStack(Items.GLASS_BOTTLE, 2)));
		var inventory = FakePlayer.get(level).getInventory();
		inventory.clearContent();
		var plan = CookingFill.plan(press, CookingStation.PRESS, juice, inventory, storage, null, -1).orElseThrow();
		var kind = CookingMenu.Kind.PRESS;
		helper.assertTrue(plan.station().get(0).is(CookingItems.CHOPPED_APPLE) && plan.station().get(0).getCount() == 4
			&& plan.station().get(kind.containerSlot).is(Items.GLASS_BOTTLE), "The press gets the apples and a bottle from storage");
		helper.assertTrue(count(plan.withdrawn(), CookingItems.CHOPPED_APPLE) == 4 && count(plan.withdrawn(), Items.GLASS_BOTTLE) == 1,
			"Storage gives up what one pressing uses");
		helper.succeed();
	}

	@GameTest
	public void kitchenFillsPhase3StationsFromStorage(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = new BlockPos(1, 2, 1);
		helper.setBlock(base, CookingBlocks.CROCK);
		helper.setBlock(base.east(2), CookingBlocks.PREP_COUNTER);
		helper.setBlock(base.east(2).above(), CookingBlocks.MIXING_BOWL);
		var crock = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(base));
		var bowl = (ProcessStationBlockEntity) level.getBlockEntity(helper.absolutePos(base.east(2).above()));
		var inventory = FakePlayer.get(level).getInventory();
		inventory.clearContent();
		var storage = new CabinStorageState(true, 1, List.of(new ItemStack(CookingItems.APPLE_JUICE, 8), new ItemStack(CookingItems.RAW_BUTTER),
			new ItemStack(CookingItems.SALT, 4)));
		var cider = CookingFill.plan(crock, CookingStation.CROCK, recipe(helper, "cooking/crock/cider"), inventory, storage, null, -1).orElseThrow();
		helper.assertTrue(cider.station().get(0).is(CookingItems.APPLE_JUICE) && cider.station().get(0).getCount() == 4,
			"The crock fills with apple juice from storage");
		var butter = CookingFill.plan(bowl, CookingStation.MIXING_BOWL, recipe(helper, "cooking/mixing_bowl/salted_butter"), inventory, storage, null, -1)
			.orElseThrow();
		helper.assertTrue(count(butter.withdrawn(), CookingItems.RAW_BUTTER) == 1 && count(butter.withdrawn(), CookingItems.SALT) == 1,
			"The mixing bowl takes butter and salt from storage");
		helper.succeed();
	}

	@GameTest
	public void kitchenCommitWritesAReceiptTheStationsConfirm(GameTestHelper helper) {
		var level = helper.getLevel();
		var server = level.getServer();
		var registry = CabinRegistry.get(server);
		var cabin = registry.create(UUID.randomUUID());
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades()
			.withStorage(new CabinStorageState(true, 1, List.of(new ItemStack(Items.WHEAT, 5))))
			.withKitchen(CabinKitchenState.EMPTY.reveal()));
		helper.setBlock(new BlockPos(1, 2, 1), CookingBlocks.OVEN);
		var oven = (OvenBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 1)));
		var player = FakePlayer.get(level);
		player.getInventory().clearContent();
		var plan = CookingFill.plan(oven, CookingStation.OVEN, recipe(helper, "portable_pocket_cabin_gametest:legacy_bread"), player.getInventory(),
			cabin.upgrades().storage(), null, -1).orElseThrow();
		boolean confirmed = CabinKitchenStorage.commit(player, cabin, List.of(new CabinKitchenStorage.StationAfter(oven, plan.station())),
			plan.inventory(), plan.storage().orElseThrow(), plan.withdrawn(), plan.deposited());
		var saved = registry.find(cabin.uuid()).orElseThrow();
		helper.assertTrue(oven.getItem(0).is(Items.WHEAT) && oven.getItem(0).getCount() == 3, "The oven holds the bread's wheat");
		helper.assertTrue(count(saved.upgrades().storage().stacks(), Items.WHEAT) == 2, "Storage keeps the rest");
		var receipt = saved.upgrades().kitchen().pending().stream().findFirst();
		helper.assertTrue(confirmed ? receipt.isEmpty() : receipt.isPresent() && oven.lastTransfer().equals(Optional.of(receipt.get().operation()))
			&& receipt.get().stations().getFirst().after().get(0).getCount() == 3 && count(receipt.get().withdrawn(), Items.WHEAT) == 3,
			"A confirmed transfer clears its receipt; an unconfirmed one keeps a receipt the oven carries");
		helper.succeed();
	}

	@GameTest
	public void kitchenRecoveryCompletesOrUndoesEachReceiptOnce(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		// Server GameTests run without the pocket dimension, so the stations stand in the test level.
		var pocket = helper.getLevel();
		var registry = CabinRegistry.get(server);
		var cabin = registry.create(UUID.randomUUID());
		BlockPos applied = helper.absolutePos(new BlockPos(1, 2, 1));
		BlockPos undone = applied.east(2);
		for (BlockPos pos : List.of(applied, undone)) {
			pocket.getChunkAt(pos);
			pocket.setBlockAndUpdate(pos, CookingBlocks.OVEN.defaultBlockState());
		}
		var appliedOven = (OvenBlockEntity) pocket.getBlockEntity(applied);
		var undoneOven = (OvenBlockEntity) pocket.getBlockEntity(undone);
		var before = appliedOven.contents();
		var after = new java.util.ArrayList<>(before);
		after.set(0, new ItemStack(Items.WHEAT, 3));
		int inventorySize = FakePlayer.get(pocket).getInventory().getContainerSize();
		var inventoryBefore = java.util.Collections.nCopies(inventorySize, ItemStack.EMPTY);
		var inventoryAfter = new java.util.ArrayList<>(inventoryBefore);
		inventoryAfter.set(0, new ItemStack(Items.STICK));
		UUID firstPlayer = UUID.randomUUID();
		var first = new CabinKitchenTransfer(UUID.randomUUID(), firstPlayer, List.of(new CabinKitchenTransfer.Change(applied, before, after)),
			inventoryBefore, inventoryAfter, List.of(new ItemStack(Items.WHEAT, 3)), List.of());
		var second = new CabinKitchenTransfer(UUID.randomUUID(), UUID.randomUUID(), List.of(new CabinKitchenTransfer.Change(undone, before, after)),
			inventoryBefore, inventoryAfter, List.of(new ItemStack(Items.WHEAT, 3)), List.of(new ItemStack(Items.STICK)));
		appliedOven.applyTransfer(first.operation(), after);
		// Storage as both transfers left it: 8 wheat less 3 and 3, plus the deposited stick.
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades()
			.withStorage(new CabinStorageState(true, 1, List.of(new ItemStack(Items.WHEAT, 2), new ItemStack(Items.STICK))))
			.withKitchen(CabinKitchenState.EMPTY.reveal().withPending(first).withPending(second)));

		CabinKitchenStorage.recoverAll(pocket);
		var recovered = registry.find(cabin.uuid()).orElseThrow();
		var storage = recovered.upgrades().storage().stacks();
		helper.assertTrue(count(storage, Items.WHEAT) == 5 && count(storage, Items.STICK) == 0,
			"The undone transfer returns its wheat and takes back its deposit; the completed one keeps its withdrawal");
		helper.assertTrue(appliedOven.getItem(0).getCount() == 3 && undoneOven.isEmpty(), "Each oven keeps the state its receipt settled on");
		helper.assertTrue(recovered.upgrades().kitchen().pending().stream().allMatch(receipt -> receipt.applied().isPresent())
			&& recovered.upgrades().kitchen().pendingFor(firstPlayer).orElseThrow().applied().orElseThrow()
			&& !recovered.upgrades().kitchen().pendingFor(second.player()).orElseThrow().applied().orElseThrow(),
			"Both receipts record their outcome and wait for their players");
		CabinKitchenStorage.recoverAll(pocket);
		helper.assertTrue(count(registry.find(cabin.uuid()).orElseThrow().upgrades().storage().stacks(), Items.WHEAT) == 5,
			"Recovering again changes nothing");

		var player = FakePlayer.get(pocket, new com.mojang.authlib.GameProfile(firstPlayer, "kitchen_cook"));
		player.getInventory().clearContent();
		CabinKitchenStorage.recover(player, pocket);
		helper.assertTrue(player.getInventory().getItem(0).is(Items.STICK), "Join recovery gives the player the completed transfer's inventory");
		// A fake player's save cannot be confirmed, so the receipt must stay for the next join; the client run
		// checks that a real player's confirmed save removes it.
		helper.assertTrue(registry.find(cabin.uuid()).orElseThrow().upgrades().kitchen().pendingFor(firstPlayer)
			.flatMap(CabinKitchenTransfer::applied).orElse(false), "An unconfirmed save keeps the settled receipt for the next join");

		var full = new java.util.ArrayList<ItemStack>();
		for (int i = 0; i < CabinStorageState.CAPACITIES[1]; i++) full.add(new ItemStack(Items.STONE, 64));
		var third = new CabinKitchenTransfer(UUID.randomUUID(), UUID.randomUUID(), List.of(new CabinKitchenTransfer.Change(undone, before, after)),
			inventoryBefore, inventoryAfter, List.of(new ItemStack(Items.WHEAT, 3)), List.of());
		var crowded = registry.create(UUID.randomUUID());
		registry.updateUpgradeState(crowded.uuid(), crowded.upgrades().withStorage(new CabinStorageState(true, 1, full))
			.withKitchen(CabinKitchenState.EMPTY.reveal().withPending(third)));
		CabinKitchenStorage.resolve(pocket, crowded.uuid(), third);
		var dropped = pocket.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(undone).inflate(2));
		helper.assertTrue(dropped.stream().filter(item -> item.getItem().is(Items.WHEAT)).mapToInt(item -> item.getItem().getCount()).sum() == 3,
			"Wheat that full storage cannot take back drops at the station");
		dropped.forEach(net.minecraft.world.entity.Entity::discard);
		helper.succeed();
	}

	private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
		return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	private static CookingRecipe recipe(GameTestHelper helper, String path) {
		return (CookingRecipe) helper.getLevel().getServer().getRecipeManager()
			.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
				path.contains(":") ? net.minecraft.resources.Identifier.parse(path) : PortablePocketCabin.id(path)))
			.orElseThrow().value();
	}

	private static CabinUpgradeCatalog.Offer kitchenOffer(CabinRecord cabin, WorldAttunement attunement,
		CabinUpgradeDefinitions.Definitions definitions) {
		var group = CabinUpgradeCatalog.groups(cabin, attunement, definitions).stream().filter(g -> g.title().equals("Kitchen")).findFirst().orElseThrow();
		return group.panels().getFirst();
	}
}
