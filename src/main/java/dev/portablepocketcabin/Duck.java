package dev.portablepocketcabin;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.chicken.ChickenVariants;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The duck, a bird of rivers and swamps. It lives like a chicken: it floats, breeds with seeds, and lays eggs. It
 * drops a duck carcass. Until B-0034 adds its own art it wears the cold chicken's coat.
 */
final class Duck extends Chicken {
	Duck(EntityType<? extends Chicken> type, Level level) {
		super(type, level);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
		SpawnGroupData spawned = super.finalizeSpawn(level, difficulty, reason, data);
		level.registryAccess().lookupOrThrow(Registries.CHICKEN_VARIANT).get(ChickenVariants.COLD).ifPresent(this::setVariant);
		return spawned;
	}

	@Override
	public Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
		Duck duckling = Butchery.DUCK.create(level, EntitySpawnReason.BREEDING);
		if (duckling != null) duckling.setVariant(getVariant());
		return duckling;
	}
}
