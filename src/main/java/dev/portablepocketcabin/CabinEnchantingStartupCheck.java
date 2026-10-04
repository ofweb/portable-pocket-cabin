package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.util.*;

final class CabinEnchantingStartupCheck {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000801");
	private CabinEnchantingStartupCheck() { }
	static void onServerStarted(MinecraftServer server) {
		String mode = System.getProperty("portable-pocket-cabin.enchanting-test", "");
		if (mode.isEmpty()) return;
		server.schedule(new TickTask(server.getTickCount() + 10, () -> {
			try { if (mode.equals("seed")) seed(server); else verify(server); }
			catch (Exception exception) { PortablePocketCabin.LOGGER.error("Enchanting integration check failed", exception); }
			finally { server.halt(false); }
		}));
	}
	private static void seed(MinecraftServer server) {
		var registry = CabinRegistry.get(server); var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		var cabin = registry.create(OWNER);
		var exterior = new CabinExterior(Level.OVERWORLD, new BlockPos(0, 200, 0), Direction.NORTH);
		registry.beginDeployment(cabin.uuid(), OWNER, exterior); ExteriorCabin.place(server.overworld(), exterior);
		PocketDimension.ensureCabinInterior(pocket, cabin.cellIndex(), cabin.palette(), 3);
		registry.markInteriorGenerated(cabin.uuid()); registry.finishDeployment(cabin.uuid());
		cabin = registry.find(cabin.uuid()).orElseThrow(); PocketDimension.applyGeneralSpaceExpansion(pocket, cabin, 5);
		cabin = registry.expandGeneralSpace(cabin.uuid(), OWNER, 3, 5, 21);
		cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withEnchanting(CabinEnchantingState.EMPTY.reveal()));
		require(CabinEnchanting.validate(pocket, cabin, 1).success(), "Room fits beyond crafting");
		for (int tier = 1; tier <= 5; tier++) {
			cabin = registry.find(cabin.uuid()).orElseThrow();
			var requirements = CabinUpgradeCatalog.enchantingRequirements(cabin, tier);
			var target = CabinUpgradeState.Target.enchanting(tier);
			var stacks = requirements.stream().map(r -> new ItemStack(BuiltInRegistries.ITEM.getOptional(r.itemId()).orElseThrow(), r.count())).toList();
			var operation = new CabinUpgradeState.Installation(UUID.randomUUID(), target, tier - 1, tier);
			if (tier == 2) {
				var library = cabin.upgrades().enchanting().learn(Identifier.withDefaultNamespace("sharpness"), 5);
				cabin = registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withEnchanting(library));
			}
			if (tier == 5) {
				var shelf = CabinEnchanting.stations(cabin, 5).keySet().stream().filter(pos -> !CabinEnchanting.stations(registry.findByOwner(OWNER).orElseThrow(), 4).containsKey(pos)).findFirst().orElseThrow();
				pocket.setBlockAndUpdate(shelf, Blocks.CHEST.defaultBlockState());
				((ChestBlockEntity)pocket.getBlockEntity(shelf)).setItem(0, new ItemStack(Items.DIAMOND, 7));
			}
			registry.updateUpgradeState(cabin.uuid(), cabin.upgrades().withFund(new CabinUpgradeState.Fund(target, requirements, stacks)).withInstallation(operation));
			CabinRegistry.flush(server); CabinEnchanting.install(pocket, cabin, operation);
			if (tier == 5) break;
			cabin = registry.completeUpgradeInstallation(cabin.uuid(), operation.operationId());
			CabinRegistry.flush(server); CabinEnchanting.finish(server, cabin);
		}
		CabinRegistry.flush(server); server.saveEverything(false, true, false);
		PortablePocketCabin.LOGGER.info("ENCHANTING_INSTALLATION_TEST_PASSED");
	}
	private static void verify(MinecraftServer server) {
		var registry = CabinRegistry.get(server); var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		var cabin = registry.findByOwner(OWNER).orElseThrow();
		require(cabin.upgrades().enchanting().level() == 5 && cabin.upgrades().installation().isEmpty(), "Interrupted upgrade resumes at tier V");
		require(cabin.upgrades().enchanting().known().get(Identifier.withDefaultNamespace("sharpness")) == 5, "Knowledge survives restart");
		var bounds = CabinEnchanting.space(cabin);
		var drops = pocket.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(PocketDimension.cellCenter(cabin.cellIndex()).offset(bounds.minimum())), net.minecraft.world.phys.Vec3.atLowerCornerOf(PocketDimension.cellCenter(cabin.cellIndex()).offset(bounds.maximum()).offset(1, 1, 1))).inflate(8));
		require(drops.stream().filter(item -> item.getItem().is(Items.DIAMOND)).mapToInt(item -> item.getItem().getCount()).sum() == 7,
			"Restart ejects displaced container contents exactly once");
		var table = CabinEnchantmentLibrary.table(cabin);
		CabinCorridors.applyWorldEffect(() -> pocket.setBlockAndUpdate(table.south(), Blocks.GOLD_BLOCK.defaultBlockState()));
		registry.beginPacking(cabin.uuid(), OWNER); cabin = registry.finishPacking(cabin.uuid());
		require(cabin.upgrades().enchanting().level() == 5 && cabin.upgrades().enchanting().known().size() == 1, "Packing preserves knowledge and tier");
		registry.markExteriorCleanupComplete(cabin.uuid());
		registry.beginDeployment(cabin.uuid(), OWNER, cabin.lastExterior().orElseThrow()); registry.finishDeployment(cabin.uuid());
		cabin = registry.findByOwner(OWNER).orElseThrow();
		require(pocket.getBlockState(table.south()).is(Blocks.GOLD_BLOCK), "Redeployment preserves room contents");
		PortablePocketCabin.LOGGER.info("ENCHANTING_RECOVERY_TEST_PASSED");
	}
	private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
