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
		place(pocket, cellIndex, Profile.INACTIVE);
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
			place(pocket, cabin.cellIndex(), profile);
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
		Map<BlockPos, Block> result = new LinkedHashMap<>();
		BlockPos center = PocketDimension.cellCenter(cellIndex);
		int index = 0;
		for (int baseX : new int[] {-7, 5}) {
			for (int y = 2; y <= 4; y++) {
				for (int x = baseX; x < baseX + 3; x++) {
					result.put(center.offset(x, y, -PocketDimension.INTERIOR_SHELL_RADIUS),
						block(profile, index++));
				}
			}
		}
		return Map.copyOf(result);
	}

	private static Profile profile(MinecraftServer server, CabinRecord cabin) {
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

	private static void place(ServerLevel pocket, long cellIndex, Profile profile) {
		for (Map.Entry<BlockPos, Block> entry : blocks(cellIndex, profile).entrySet()) {
			if (!pocket.getBlockState(entry.getKey()).is(entry.getValue())) {
				pocket.setBlockAndUpdate(entry.getKey(), entry.getValue().defaultBlockState());
			}
		}
	}

	private static Block block(Profile profile, int index) {
		return switch (profile) {
			case DAWN -> index % 3 == 0 ? Blocks.STAINED_GLASS.yellow() : Blocks.STAINED_GLASS.orange();
			case DAY -> index % 5 == 0 ? Blocks.STAINED_GLASS.white() : Blocks.STAINED_GLASS.lightBlue();
			case SUNSET -> index % 3 == 0 ? Blocks.STAINED_GLASS.magenta() : Blocks.STAINED_GLASS.orange();
			case NIGHT -> index % 7 == 0 ? Blocks.STAINED_GLASS.white() : Blocks.STAINED_GLASS.blue();
			case RAIN -> index % 3 == 0 ? Blocks.STAINED_GLASS.lightGray() : Blocks.STAINED_GLASS.cyan();
			case THUNDER -> index % 7 == 0 ? Blocks.STAINED_GLASS.yellow() : Blocks.STAINED_GLASS.gray();
			case NETHER -> index % 3 == 0 ? Blocks.STAINED_GLASS.orange() : Blocks.STAINED_GLASS.red();
			case END -> index % 3 == 0 ? Blocks.STAINED_GLASS.magenta() : Blocks.STAINED_GLASS.purple();
			case INACTIVE -> index % 2 == 0 ? Blocks.DARK_OAK_PLANKS : Blocks.DARK_OAK_LOG;
		};
	}
}
