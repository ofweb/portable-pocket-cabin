package dev.portablepocketcabin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeSerializer;

final class CabinRecipes {
	private static final CabinKitRecipe CABIN_KIT = new CabinKitRecipe();
	private static final ResourceKey<RecipeSerializer<?>> CABIN_KIT_SERIALIZER_KEY = ResourceKey.create(
		Registries.RECIPE_SERIALIZER, PortablePocketCabin.id("cabin_kit")
	);

	static final RecipeSerializer<CabinKitRecipe> CABIN_KIT_SERIALIZER = Registry.register(
		BuiltInRegistries.RECIPE_SERIALIZER,
		CABIN_KIT_SERIALIZER_KEY,
		new RecipeSerializer<>(MapCodec.unit(CABIN_KIT), StreamCodec.<RegistryFriendlyByteBuf, CabinKitRecipe>unit(CABIN_KIT))
	);

	private CabinRecipes() {
	}

	static void register() {
		// Triggers static registration during mod initialization.
	}
}
