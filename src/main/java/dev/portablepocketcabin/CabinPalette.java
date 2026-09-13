package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Objects;

public record CabinPalette(
	WoodSelection floor,
	WoodSelection walls,
	WoodSelection roof,
	DoorSelection door
) {
	public static final Codec<CabinPalette> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		WoodSelection.CODEC.fieldOf("floor").forGetter(CabinPalette::floor),
		WoodSelection.CODEC.fieldOf("walls").forGetter(CabinPalette::walls),
		WoodSelection.CODEC.fieldOf("roof").forGetter(CabinPalette::roof),
		DoorSelection.CODEC.fieldOf("door").forGetter(CabinPalette::door)
	).apply(instance, CabinPalette::new));

	public static final CabinPalette DEFAULT = new CabinPalette(
		WoodSelection.vanilla("vanilla/wood/oak", "oak_planks", "oak_log", "oak_stairs", "oak_slab"),
		WoodSelection.vanilla("vanilla/wood/oak", "oak_planks", "oak_log", "oak_stairs", "oak_slab"),
		WoodSelection.vanilla("vanilla/wood/oak", "oak_planks", "oak_log", "oak_stairs", "oak_slab"),
		new DoorSelection(
			PortablePocketCabin.id("vanilla/door/iron"), Identifier.withDefaultNamespace("iron_door")
		)
	);

	public CabinPalette {
		Objects.requireNonNull(floor, "floor");
		Objects.requireNonNull(walls, "walls");
		Objects.requireNonNull(roof, "roof");
		Objects.requireNonNull(door, "door");
	}

	public record WoodSelection(
		Identifier profileId,
		Identifier planks,
		Identifier structuralWood,
		Identifier stairs,
		Identifier slab
	) {
		public static final Codec<WoodSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("profile").forGetter(WoodSelection::profileId),
			Identifier.CODEC.fieldOf("planks").forGetter(WoodSelection::planks),
			Identifier.CODEC.fieldOf("structural_wood").forGetter(WoodSelection::structuralWood),
			Identifier.CODEC.fieldOf("stairs").forGetter(WoodSelection::stairs),
			Identifier.CODEC.fieldOf("slab").forGetter(WoodSelection::slab)
		).apply(instance, WoodSelection::new));

		public WoodSelection {
			Objects.requireNonNull(profileId, "profileId");
			Objects.requireNonNull(planks, "planks");
			Objects.requireNonNull(structuralWood, "structuralWood");
			Objects.requireNonNull(stairs, "stairs");
			Objects.requireNonNull(slab, "slab");
		}

		static WoodSelection vanilla(String profile, String planks, String structuralWood, String stairs, String slab) {
			return new WoodSelection(
				PortablePocketCabin.id(profile),
				Identifier.withDefaultNamespace(planks),
				Identifier.withDefaultNamespace(structuralWood),
				Identifier.withDefaultNamespace(stairs),
				Identifier.withDefaultNamespace(slab)
			);
		}

		Block planksBlock() {
			return requireBlock(planks);
		}

		Block structuralWoodBlock() {
			return requireBlock(structuralWood);
		}

		Block stairsBlock() {
			return requireBlock(stairs);
		}

		Block slabBlock() {
			return requireBlock(slab);
		}
	}

	public record DoorSelection(Identifier profileId, Identifier block) {
		public static final Codec<DoorSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("profile").forGetter(DoorSelection::profileId),
			Identifier.CODEC.fieldOf("block").forGetter(DoorSelection::block)
		).apply(instance, DoorSelection::new));

		public DoorSelection {
			Objects.requireNonNull(profileId, "profileId");
			Objects.requireNonNull(block, "block");
		}

		Block doorBlock() {
			return requireBlock(block);
		}
	}

	private static Block requireBlock(Identifier id) {
		return BuiltInRegistries.BLOCK.getOptional(id)
			.orElseThrow(() -> new IllegalStateException("Cabin palette block is unavailable: " + id));
	}
}
