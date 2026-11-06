package dev.portablepocketcabin;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Which cooking recipes a player has every ingredient for. The cookbook asks when it opens. The server counts the
 * player's inventory, and the cabin's central storage too when the player is household in that cabin. It replies
 * only with recipe ids, so storage contents never reach the client.
 */
final class CookbookAvailability {
	private CookbookAvailability() {
	}

	/**
	 * Sent by the client when the cookbook or a station screen opens. For a station, the reply covers what the recipe
	 * panel can fill: that station's recipes, from the inventory and what the station already holds.
	 */
	record Request(boolean station) implements CustomPacketPayload {
		static final Type<Request> TYPE = new Type<>(PortablePocketCabin.id("cookbook_availability_request"));
		static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = ByteBufCodecs.BOOL.map(Request::new, Request::station).cast();

		@Override
		public Type<Request> type() {
			return TYPE;
		}
	}

	/** The ids of the recipes the player can make now, for the cookbook or for the open station. */
	record Reply(Set<Identifier> recipes, boolean station) implements CustomPacketPayload {
		static final Type<Reply> TYPE = new Type<>(PortablePocketCabin.id("cookbook_availability"));
		static final StreamCodec<RegistryFriendlyByteBuf, Reply> CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC.apply(ByteBufCodecs.collection(java.util.HashSet::new, 4096)).map(Set::copyOf, java.util.HashSet::new),
			Reply::recipes, ByteBufCodecs.BOOL, Reply::station, Reply::new);

		@Override
		public Type<Reply> type() {
			return TYPE;
		}
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Reply.TYPE, Reply.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (request, context) -> {
			ServerPlayer player = context.player();
			if (!request.station()) {
				ServerPlayNetworking.send(player, new Reply(available(player), false));
			} else if (player.containerMenu instanceof CookingMenu menu && menu.stillValid(player)
				&& player.level().getBlockEntity(menu.pos()) instanceof CookingStationBlockEntity station && menu.kind() != CookingMenu.Kind.STOVE) {
				CookingStation kind = menu.kind() == CookingMenu.Kind.POT ? CookingStation.POT : CookingStation.OVEN;
				ServerPlayNetworking.send(player, new Reply(fillable(player, station, kind), true));
			}
		});
	}

	static Set<Identifier> available(ServerPlayer player) {
		List<ItemStack> inventory = new ArrayList<>();
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot));
		CabinRecord cabin = null;
		if (player.level().dimension().equals(PocketDimension.LEVEL_KEY)) {
			long cell = PocketDimension.cellIndexAt(player.blockPosition()).orElse(-1L);
			cabin = CabinRegistry.get(player.level().getServer()).findByCell(cell).orElse(null);
		}
		return available(recipes(player.level().getServer().getRecipeManager().getRecipes()), sources(cabin, player.getUUID(), inventory));
	}

	/** The station's recipes that the recipe panel can fill from the player's inventory and the station's own ingredients. */
	static Set<Identifier> fillable(ServerPlayer player, Container station, CookingStation kind) {
		List<ItemStack> sources = new ArrayList<>(player.getInventory().getNonEquipmentItems());
		for (int slot = 0; slot < kind.ingredientSlots(); slot++) sources.add(station.getItem(slot));
		if (kind == CookingStation.POT) sources.add(station.getItem(CookingPotBlockEntity.CONTAINER_SLOT));
		var recipes = recipes(player.level().getServer().getRecipeManager().getRecipes()).stream()
			.filter(holder -> holder.value().station() == kind).toList();
		return available(recipes, sources);
	}

	/** The stacks a player may cook from: storage first when they are household in a cabin with storage, then their inventory. */
	static List<ItemStack> sources(CabinRecord cabin, UUID player, List<ItemStack> inventory) {
		List<ItemStack> sources = new ArrayList<>();
		if (cabin != null && CabinStorage.mayUse(cabin, player) && cabin.upgrades().storage().level() > 0) {
			sources.addAll(cabin.upgrades().storage().stacks());
		}
		sources.addAll(inventory);
		return sources;
	}

	/** The cooking recipes among all recipes, from the server's manager or the client's synced cache. */
	@SuppressWarnings("unchecked")
	static List<RecipeHolder<CookingRecipe>> recipes(Collection<RecipeHolder<?>> all) {
		List<RecipeHolder<CookingRecipe>> cooking = new ArrayList<>();
		for (RecipeHolder<?> holder : all) if (holder.value() instanceof CookingRecipe) cooking.add((RecipeHolder<CookingRecipe>) holder);
		return cooking;
	}

	static Set<Identifier> available(Collection<RecipeHolder<CookingRecipe>> recipes, List<ItemStack> sources) {
		return recipes.stream().filter(holder -> canMake(holder.value(), sources))
			.map(holder -> holder.id().identifier()).collect(Collectors.toUnmodifiableSet());
	}

	/** Whether the sources hold every counted ingredient and the container, using each item for only one entry. */
	static boolean canMake(CookingRecipe recipe, List<ItemStack> sources) {
		return CookingAllocation.allocate(demands(recipe), sources).isPresent();
	}

	/** The recipe's counted ingredients, then its container. */
	static List<CookingRecipe.Counted> demands(CookingRecipe recipe) {
		List<CookingRecipe.Counted> demands = new ArrayList<>(recipe.ingredients());
		recipe.container().ifPresent(demands::add);
		return demands;
	}
}
