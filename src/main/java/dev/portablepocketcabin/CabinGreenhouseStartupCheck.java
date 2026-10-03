package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.util.*;

final class CabinGreenhouseStartupCheck {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000001901");
	private static final UUID SLEEP_OWNER = UUID.fromString("00000000-0000-0000-0000-000000001902");
	private CabinGreenhouseStartupCheck() { }
	static void onServerStarted(MinecraftServer server) {
		String mode = System.getProperty("portable-pocket-cabin.greenhouse-test", "");
		if (mode.isEmpty()) return;
		server.schedule(new TickTask(server.getTickCount() + 10, () -> {
			try {
				if (mode.equals("seed")) seed(server); else verify(server);
			} catch (Exception e) { PortablePocketCabin.LOGGER.error("Greenhouse integration check failed", e); }
			finally { server.halt(false); }
		}));
	}
	private static CabinRecord deployed(MinecraftServer server, UUID owner) {
		var registry = CabinRegistry.get(server);
		var cabin = registry.create(owner);
		var exterior = new CabinExterior(Level.OVERWORLD, new BlockPos((int)cabin.cellIndex()*64,200,0), Direction.NORTH);
		registry.beginDeployment(cabin.uuid(),owner,exterior);
		ExteriorCabin.place(server.overworld(), exterior);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		PocketDimension.ensureCabinInterior(pocket,cabin.cellIndex(),cabin.palette(),3);
		registry.markInteriorGenerated(cabin.uuid()); registry.finishDeployment(cabin.uuid());
		cabin = registry.find(cabin.uuid()).orElseThrow();
		PocketDimension.applyGeneralSpaceExpansion(pocket,cabin,5);
		cabin = registry.expandGeneralSpace(cabin.uuid(),owner,3,5,21);
		return registry.updateUpgradeState(cabin.uuid(),cabin.upgrades().withGreenhouse(CabinGreenhouseState.EMPTY.reveal()));
	}
	private static CabinUpgradeState.Installation pending(CabinRegistry registry, CabinRecord cabin, int level) {
		var requirements = CabinUpgradeCatalog.greenhouseRequirements(level);
		var target = CabinUpgradeState.Target.greenhouse(level);
		var stacks = requirements.stream().map(r -> new ItemStack(BuiltInRegistries.ITEM.getOptional(r.itemId()).orElseThrow(),r.count())).toList();
		var install = new CabinUpgradeState.Installation(UUID.randomUUID(),target,level-1,level);
		registry.updateUpgradeState(cabin.uuid(),cabin.upgrades().withFund(new CabinUpgradeState.Fund(target,requirements,stacks)).withInstallation(install));
		return install;
	}
	private static BlockPos crop(CabinRecord cabin) {
		return PocketDimension.cellCenter(cabin.cellIndex()).offset(CabinGreenhouse.space(cabin,1).entrance()).south().west();
	}
	private static void seed(MinecraftServer server) {
		var registry = CabinRegistry.get(server);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		var cabin = deployed(server,OWNER);
		BlockPos crop = crop(cabin), chest = crop.east(2).south();
		for (int level=1; level<=4; level++) {
			cabin = registry.find(cabin.uuid()).orElseThrow();
			var bounds = CabinGreenhouse.space(cabin,level);
			BlockPos obstruction = PocketDimension.cellCenter(cabin.cellIndex()).offset(bounds.maximum()).below();
			pocket.setBlock(obstruction,Blocks.DIAMOND_BLOCK.defaultBlockState(),CabinCorridorSnapshot.FLAGS);
			require(!CabinGreenhouse.validate(pocket,cabin,level).success(),"Obstructions refuse before changes or funding consumption");
			pocket.setBlock(obstruction,Blocks.AIR.defaultBlockState(),CabinCorridorSnapshot.FLAGS);
			require(CabinGreenhouse.validate(pocket,cabin,level).success(),"Greenhouse and its west corridor fit");
			var install = pending(registry,cabin,level);
			CabinRegistry.flush(server);
			CabinGreenhouse.install(pocket,cabin,install);
			CabinGreenhouse.install(pocket,cabin,install);
			verifyHydration(pocket,cabin,level);
			if (level==4) break;
			cabin = registry.completeUpgradeInstallation(cabin.uuid(),install.operationId());
			if(level==1) {
				pocket.setBlock(crop.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),CabinCorridorSnapshot.FLAGS);
				pocket.setBlock(crop,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,3),CabinCorridorSnapshot.FLAGS);
				pocket.setBlock(chest,Blocks.CHEST.defaultBlockState(),CabinCorridorSnapshot.FLAGS);
				((ChestBlockEntity)pocket.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,5));
				pocket.setBlock(crop.east().south(2).below(),Blocks.WATER.defaultBlockState(),CabinCorridorSnapshot.FLAGS);
			}
			require(pocket.getBlockState(crop).getValue(CropBlock.AGE)==3,"Plants keep their growth stages");
			require(((ChestBlockEntity)pocket.getBlockEntity(chest)).getItem(0).getCount()==5,"Fixtures retain inventory");
			require(CabinProtection.isProtected(pocket,crop.below(2)) && !CabinProtection.isProtected(pocket,crop.below()),"Foundation protected; gardening layer editable");
			require(CabinCorridors.mayChange(pocket,OWNER,crop) && !CabinCorridors.mayChange(pocket,UUID.randomUUID(),crop),"Guests cannot garden");
			UUID resident = UUID.randomUUID(); registry.trust(cabin.uuid(),OWNER,resident);
			registry.setEntryPermission(cabin.uuid(),OWNER,CabinEntryPermission.TRUSTED_PLAYERS);
			require(CabinCorridors.mayChange(pocket,resident,crop),"Residents can garden");
			registry.untrust(cabin.uuid(),OWNER,resident);
			require(!CabinCorridors.mayChange(pocket,resident,crop),"Revoked roles take effect");
			var guest = new net.minecraft.server.level.ServerPlayer(server, pocket,
				new com.mojang.authlib.GameProfile(resident, "garden_guest"), net.minecraft.server.level.ClientInformation.createDefault());
			FarmlandBlock.turnToDirt(guest, pocket.getBlockState(crop.below()), pocket, crop.below());
			require(pocket.getBlockState(crop.below()).is(Blocks.FARMLAND), "Guest jumps cannot trample beds");
		}
		cabin = registry.findByOwner(OWNER).orElseThrow();
		// Save a partial added bed, leaving the retained installation for restart recovery.
		BlockPos added = PocketDimension.cellCenter(cabin.cellIndex()).offset(CabinGreenhouse.space(cabin,4).entrance()).south(28).west(4);
		CabinCorridors.applyWorldEffect(() -> pocket.setBlock(added,Blocks.AIR.defaultBlockState(),CabinCorridorSnapshot.FLAGS));
		var sleeping = deployed(server,SLEEP_OWNER);
		var install = pending(registry,sleeping,1); CabinGreenhouse.install(pocket,sleeping,install);
		sleeping = registry.completeUpgradeInstallation(sleeping.uuid(),install.operationId());
		BlockPos sleepingCrop = crop(sleeping);
		pocket.setBlock(sleepingCrop.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),CabinCorridorSnapshot.FLAGS);
		pocket.setBlock(sleepingCrop,Blocks.WHEAT.defaultBlockState(),CabinCorridorSnapshot.FLAGS);
		registry.beginPacking(sleeping.uuid(),SLEEP_OWNER); sleeping = registry.finishPacking(sleeping.uuid());
		CabinSimulation.sync(server);
		var growth = CabinGreenhouseGrowth.get(server);
		require(growth.sleeping(sleeping.uuid()),"Empty packed gardens suspend");
		growth.advance(4096L*8192);
		CabinGreenhouseGrowth.prepare(pocket,sleeping,growth);
		// Simulate a crash after part of catch-up reached the world but before elapsed time was consumed.
		CabinCorridors.applyWorldEffect(() -> pocket.setBlock(sleepingCrop,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,2),CabinCorridorSnapshot.FLAGS));
		CabinRegistry.flush(server); server.saveEverything(false,true,false);
		PortablePocketCabin.LOGGER.info("GREENHOUSE_INSTALLATION_TEST_PASSED");
	}
	private static void verify(MinecraftServer server) {
		var registry = CabinRegistry.get(server);
		var pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		var cabin = registry.findByOwner(OWNER).orElseThrow();
		require(cabin.upgrades().greenhouse().level()==4 && !cabin.upgrades().operationInProgress() && cabin.upgrades().funds().isEmpty(),"Interrupted installation commits exactly once");
		BlockPos crop = crop(cabin), chest = crop.east(2).south();
		require(pocket.getBlockState(crop).getValue(CropBlock.AGE)==3 && pocket.getBlockState(crop.below()).getValue(FarmlandBlock.MOISTURE)==7,"Restart preserves old plants and soil");
		require(((ChestBlockEntity)pocket.getBlockEntity(chest)).getItem(0).getCount()==5 && pocket.getBlockState(crop.east().south(2).below()).is(Blocks.WATER),"Restart preserves fixtures and altered paths");
		var volume = CabinGreenhouse.space(cabin,4).volume(cabin.cellIndex());
		CabinGreenhouseGrowth.waitForLight(pocket,volume);
		for (var entry : CabinGreenhouse.defaults(cabin,4).entrySet())
			if(entry.getValue().is(Blocks.FARMLAND)) require(pocket.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,entry.getKey().above())>=9,"Supplied block light covers the widest garden without daylight");
		var install = CabinUpgradeState.Installation.generalSpace(UUID.randomUUID(),CabinUpgradeState.Target.generalSpace(7),5);
		var target = install.target();
		var req = List.of(new CabinUpgradeState.Requirement(net.minecraft.resources.Identifier.withDefaultNamespace("diamond"),1));
		registry.updateUpgradeState(cabin.uuid(),cabin.upgrades().withFund(new CabinUpgradeState.Fund(target,req,List.of(new ItemStack(Items.DIAMOND)))).withInstallation(install));
		new CabinUpgradeEffect(server,pocket).apply(cabin,install);
		cabin = registry.completeUpgradeInstallation(cabin.uuid(),install.operationId());
		new CabinUpgradeEffect(server,pocket).refresh(cabin);
		require(pocket.getBlockState(crop.west()).getValue(CropBlock.AGE)==3 && ((ChestBlockEntity)pocket.getBlockEntity(chest.west())).getItem(0).getCount()==5,"Main-room expansion moves the intact greenhouse");
		var sleeping = registry.findByOwner(SLEEP_OWNER).orElseThrow();
		require(CabinGreenhouseGrowth.get(server).sleeping(sleeping.uuid()),"Sleep clock survives restart");
		require(CabinGreenhouse.paused(sleeping,crop(sleeping)),"Loaded packed plants remain paused");
		registry.markExteriorCleanupComplete(sleeping.uuid());
		var exterior = new CabinExterior(Level.OVERWORLD,new BlockPos(256,200,0),Direction.NORTH);
		registry.beginDeployment(sleeping.uuid(),SLEEP_OWNER,exterior); ExteriorCabin.place(server.overworld(),exterior); registry.finishDeployment(sleeping.uuid());
		CabinSimulation.sync(server);
		require(pocket.getBlockState(crop(sleeping)).getValue(CropBlock.AGE)==7 && !CabinGreenhouseGrowth.get(server).sleeping(sleeping.uuid()),"Interrupted catch-up reaches maturity and consumes elapsed time");
		pocket.setBlock(crop(sleeping),Blocks.WHEAT.defaultBlockState(),CabinCorridorSnapshot.FLAGS);
		CabinSimulation.sync(server); CabinGreenhouseGrowth.sync(server);
		require(pocket.getBlockState(crop(sleeping)).getValue(CropBlock.AGE)==0,"Repeated entry cannot apply the elapsed time twice");
		server.saveEverything(false,true,false);
		PortablePocketCabin.LOGGER.info("GREENHOUSE_RECOVERY_TEST_PASSED");
	}
	private static void verifyHydration(ServerLevel pocket, CabinRecord cabin, int level) {
		CabinCorridors.applyWorldEffect(() -> {
			for (var entry : CabinGreenhouse.defaults(cabin,level).entrySet()) {
				BlockPos pos = entry.getKey();
				var original = pocket.getBlockState(pos);
				if (original.is(Blocks.STONE_BRICK_SLAB)) {
					require(original.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)
						== net.minecraft.world.level.block.state.properties.SlabType.TOP
						&& original.getFluidState().isSource(),"Default paths cover source water with top slabs");
				} else if (original.is(Blocks.FARMLAND)) {
					var dry = original.setValue(FarmlandBlock.MOISTURE,0);
					pocket.setBlock(pos,dry,CabinCorridorSnapshot.FLAGS);
					dry.randomTick(pocket,pos,pocket.getRandom());
					require(pocket.getBlockState(pos).getValue(FarmlandBlock.MOISTURE)==7,
						"Covered paths hydrate every default bed at greenhouse size " + level);
					pocket.setBlock(pos,original,CabinCorridorSnapshot.FLAGS);
				}
			}
		});
	}
	private static void require(boolean condition,String message) {
		if(!condition) throw new IllegalStateException(message);
	}
}
