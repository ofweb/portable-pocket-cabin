package dev.portablepocketcabin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Runtime world ownership and loading boundary. B-0023 effects remain unimplemented. */
final class CabinArboretumWorlds {
	private CabinArboretumWorlds() { }

	/** Persist with the installation; generation changes cannot reshape existing woodlands. */
	record Pattern(long seed, int generationVersion) {
		Pattern {
			if (generationVersion < 1) throw new IllegalArgumentException("Invalid woodland generation version");
		}
	}

	record LoadedWorld(UUID cabinId, ServerLevel level) {
		LoadedWorld {
			Objects.requireNonNull(cabinId);
			Objects.requireNonNull(level);
			if (!level.dimension().equals(dimension(cabinId))) {
				throw new IllegalArgumentException("Arboretum world belongs to another cabin");
			}
		}
	}

	enum FailureCode { CABIN_UNAVAILABLE, NOT_INSTALLED, RECOVERY_PENDING, WORLD_UNAVAILABLE, SAVE_FAILED }
	sealed interface LoadResult permits Loaded, LoadFailure { }
	record Loaded(LoadedWorld world) implements LoadResult {
		Loaded { Objects.requireNonNull(world); }
	}
	record LoadFailure(FailureCode reason) implements LoadResult {
		LoadFailure { Objects.requireNonNull(reason); }
	}
	enum UnloadResult { UNLOADED, STILL_ACTIVE, SAVE_FAILED }

	static ResourceKey<Level> dimension(UUID cabinId) {
		return ResourceKey.create(Registries.DIMENSION,
			PortablePocketCabin.id("arboretum/" + Objects.requireNonNull(cabinId)));
	}

	/** Revalidates registry state and loads only the entry core before normal door transfer. */
	static CompletionStage<LoadResult> loadCore(MinecraftServer server, CabinRecord observed) {
		throw new UnsupportedOperationException("B-0023 Design stub: arboretum world loading");
	}

	/** Saves chunks and entities before releasing an idle world's resources. */
	static UnloadResult saveAndUnloadIfIdle(MinecraftServer server, LoadedWorld world) {
		throw new UnsupportedOperationException("B-0023 Design stub: arboretum world unloading");
	}
}
