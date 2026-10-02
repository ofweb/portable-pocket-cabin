package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Maintains the deliberately stylised, block-based view of conditions outside a cabin. */
final class CabinWindows {
	enum Profile {
		DAWN,
		DAY,
		SUNSET,
		NIGHT,
		RAIN,
		THUNDER,
		NETHER,
		END,
		INACTIVE
	}

	private static final int UPDATE_INTERVAL_TICKS = 20;
	private static final Map<MinecraftServer, Map<UUID, Profile>> LAST_PROFILES = new IdentityHashMap<>();

	private CabinWindows() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % UPDATE_INTERVAL_TICKS == 0) {
				updateDeployed(server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(LAST_PROFILES::remove);
	}

	static void initializeInactive(ServerLevel pocket, long cellIndex) {
		initializeInactive(pocket, cellIndex, CabinProgression.INITIAL_GENERAL_SIZE);
	}

	static void initializeInactive(ServerLevel pocket, long cellIndex, int generalSize) {
		place(pocket, cellIndex, generalSize, CabinWindowState.grandfathered(), Profile.INACTIVE);
	}

	static void update(MinecraftServer server, CabinRecord cabin) {
		if (!cabin.interiorGenerated()) {
			return;
		}
		Profile profile = profile(server, cabin);
		Map<UUID, Profile> profiles = LAST_PROFILES.computeIfAbsent(server, ignored -> new LinkedHashMap<>());
		if (profile == profiles.put(cabin.uuid(), profile)) {
			return;
		}
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket != null) {
			place(pocket, cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows(), profile);
		}
	}

	static Profile profile(
		ResourceKey<Level> dimension, long dayTime, boolean raining, boolean thundering,
		CabinLifecycle lifecycle
	) {
		if (lifecycle != CabinLifecycle.DEPLOYED) {
			return Profile.INACTIVE;
		}
		if (dimension.equals(Level.NETHER)) {
			return Profile.NETHER;
		}
		if (dimension.equals(Level.END)) {
			return Profile.END;
		}
		if (thundering) {
			return Profile.THUNDER;
		}
		if (raining) {
			return Profile.RAIN;
		}
		long time = Math.floorMod(dayTime, 24_000L);
		if (time >= 23_000L || time < 1_000L) {
			return Profile.DAWN;
		}
		if (time < 12_000L) {
			return Profile.DAY;
		}
		if (time < 14_000L) {
			return Profile.SUNSET;
		}
		return Profile.NIGHT;
	}

	static Map<BlockPos, Block> blocks(long cellIndex, Profile profile) {
		return blocks(
			cellIndex, CabinProgression.INITIAL_GENERAL_SIZE, CabinWindowState.EMPTY, profile
		);
	}

	static Map<BlockPos, Block> blocks(long cellIndex, int generalSize, Profile profile) {
		return blocks(cellIndex, generalSize,
			generalSize >= 5 ? CabinWindowState.grandfathered() : CabinWindowState.EMPTY, profile);
	}

	static Map<BlockPos, Block> blocks(
		long cellIndex, int generalSize, CabinWindowState windows, Profile profile
	) {
		Map<BlockPos, Block> result = new LinkedHashMap<>();
		CabinWindowLayout.Result layout = CabinWindowLayout.current(cellIndex, generalSize, windows);
		if (!layout.valid()) {
			throw new IllegalStateException(layout.message());
		}
		int index = 0;
		for (CabinWindowLayout.Footprint window : layout.windows()) {
			for (BlockPos position : window.positions().stream().sorted(
				java.util.Comparator.comparingInt((BlockPos position) -> position.getY())
					.thenComparingInt(BlockPos::getX)
					.thenComparingInt(BlockPos::getZ)
			).toList()) {
				result.put(position, block(profile, index++));
			}
		}
		return Map.copyOf(result);
	}

	static void refresh(MinecraftServer server, CabinRecord cabin) {
		ServerLevel pocket = server.getLevel(PocketDimension.LEVEL_KEY);
		if (pocket != null && cabin.interiorGenerated()) {
			Profile profile = profile(server, cabin);
			place(pocket, cabin.cellIndex(), cabin.progression().generalSize(), cabin.upgrades().windows(), profile);
			LAST_PROFILES.computeIfAbsent(server, ignored -> new LinkedHashMap<>())
				.put(cabin.uuid(), profile);
		}
	}

	static Profile profile(MinecraftServer server, CabinRecord cabin) {
		if (cabin.lifecycle() != CabinLifecycle.DEPLOYED || cabin.exterior().isEmpty()) {
			return Profile.INACTIVE;
		}
		CabinExterior exterior = cabin.exterior().get();
		ServerLevel exteriorLevel = server.getLevel(exterior.dimension());
		if (exteriorLevel == null) {
			return Profile.INACTIVE;
		}
		return profile(exterior.dimension(), exteriorLevel.getOverworldClockTime(),
			exteriorLevel.isRaining(), exteriorLevel.isThundering(), cabin.lifecycle());
	}

	private static void updateDeployed(MinecraftServer server) {
		for (CabinRecord cabin : CabinRegistry.get(server).cabins()) {
			if (cabin.lifecycle() == CabinLifecycle.DEPLOYED && cabin.interiorGenerated()) {
				update(server, cabin);
			}
		}
	}

	private static void place(
		ServerLevel pocket, long cellIndex, int generalSize, CabinWindowState windows, Profile profile
	) {
		for (Map.Entry<BlockPos, Block> entry : blocks(cellIndex, generalSize, windows, profile).entrySet()) {
			if (isManagedWindowBlock(pocket.getBlockState(entry.getKey()).getBlock())
				&& !pocket.getBlockState(entry.getKey()).is(entry.getValue())) {
				pocket.setBlockAndUpdate(entry.getKey(), entry.getValue().defaultBlockState());
			}
		}
	}

	static boolean isManagedWindowBlock(Block block) {
		return Blocks.STAINED_GLASS_PANE.asList().contains(block)
			|| Blocks.STAINED_GLASS.asList().contains(block);
	}

	static Block block(Profile profile, int index) {
		return switch (profile) {
			case DAWN -> index % 3 == 0 ? Blocks.STAINED_GLASS_PANE.yellow() : Blocks.STAINED_GLASS_PANE.orange();
			case DAY -> index % 5 == 0 ? Blocks.STAINED_GLASS_PANE.white() : Blocks.STAINED_GLASS_PANE.lightBlue();
			case SUNSET -> index % 3 == 0 ? Blocks.STAINED_GLASS_PANE.magenta() : Blocks.STAINED_GLASS_PANE.orange();
			case NIGHT -> index % 7 == 0 ? Blocks.STAINED_GLASS_PANE.white() : Blocks.STAINED_GLASS_PANE.blue();
			case RAIN -> index % 3 == 0 ? Blocks.STAINED_GLASS_PANE.lightGray() : Blocks.STAINED_GLASS_PANE.cyan();
			case THUNDER -> index % 7 == 0 ? Blocks.STAINED_GLASS_PANE.yellow() : Blocks.STAINED_GLASS_PANE.gray();
			case NETHER -> index % 3 == 0 ? Blocks.STAINED_GLASS_PANE.orange() : Blocks.STAINED_GLASS_PANE.red();
			case END -> index % 3 == 0 ? Blocks.STAINED_GLASS_PANE.magenta() : Blocks.STAINED_GLASS_PANE.purple();
			case INACTIVE -> index % 2 == 0 ? Blocks.STAINED_GLASS_PANE.black() : Blocks.STAINED_GLASS_PANE.gray();
		};
	}
}
