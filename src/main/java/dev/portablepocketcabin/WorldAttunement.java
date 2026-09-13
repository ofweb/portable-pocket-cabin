package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** The once-per-save resolution of variable upgrade material slots. */
public record WorldAttunement(int definitionVersion, Identifier woodProfile) {
	public static final Codec<WorldAttunement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("definition_version").forGetter(WorldAttunement::definitionVersion),
		Identifier.CODEC.fieldOf("wood_profile").forGetter(WorldAttunement::woodProfile)
	).apply(instance, WorldAttunement::new));

	public WorldAttunement {
		Objects.requireNonNull(woodProfile, "woodProfile");
		if (definitionVersion < 1) {
			throw new IllegalArgumentException("World attunement definition version must be positive");
		}
	}
}
