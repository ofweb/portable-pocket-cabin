package dev.portablepocketcabin;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Cooking ingredients that come from the world: rock salt ore in dripstone caves and badlands, and seawater. */
final class CookingWorld {
	static final ResourceKey<PlacedFeature> ROCK_SALT_ORE = ResourceKey.create(Registries.PLACED_FEATURE, PortablePocketCabin.id("rock_salt_ore"));

	private CookingWorld() {
	}

	static void register() {
		BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_BADLANDS).or(BiomeSelectors.includeByKey(Biomes.DRIPSTONE_CAVES)),
			GenerationStep.Decoration.UNDERGROUND_ORES, ROCK_SALT_ORE);
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack held = player.getItemInHand(hand);
			if (!held.is(Items.GLASS_BOTTLE)) return InteractionResult.PASS;
			HitResult hit = player.pick(player.blockInteractionRange(), 0, true);
			if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
			BlockPos pos = block.getBlockPos();
			if (!level.getFluidState(pos).is(FluidTags.WATER) || !isSea(level.getBiome(pos)) || !level.mayInteract(player, pos)) {
				return InteractionResult.PASS;
			}
			level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
			if (!level.isClientSide()) player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(CookingItems.SEAWATER)));
			return InteractionResult.SUCCESS;
		});
	}

	/** Whether water in this biome is seawater: every ocean biome, as the {@code minecraft:is_ocean} tag lists them. */
	static boolean isSea(Holder<Biome> biome) {
		return biome.is(BiomeTags.IS_OCEAN);
	}

	/** Seawater filled at a position, or nothing when the water there is fresh. */
	static boolean seawaterAt(Level level, BlockPos pos) {
		return level.getFluidState(pos).is(FluidTags.WATER) && isSea(level.getBiome(pos));
	}
}
