package dev.portablepocketcabin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CabinCommands {
	private CabinCommands() {
	}

	static void register(
		CommandDispatcher<CommandSourceStack> dispatcher,
		CommandBuildContext registryAccess,
		Commands.CommandSelection environment
	) {
		dispatcher.register(Commands.literal("cabin")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("status").executes(context -> status(context.getSource())))
			.then(Commands.literal("create")
				.executes(context -> createForSource(context.getSource()))
				.then(Commands.argument("player", EntityArgument.player())
					.executes(context -> create(
						context.getSource(),
						EntityArgument.getPlayer(context, "player")
					))))
			.then(Commands.literal("list").executes(context -> list(context.getSource())))
			.then(Commands.literal("inspect")
				.then(Commands.argument("uuid", UuidArgument.uuid())
					.executes(context -> inspect(
						context.getSource(),
						UuidArgument.getUuid(context, "uuid")
					))))
			.then(Commands.literal("visit")
				.then(Commands.argument("uuid", UuidArgument.uuid())
					.executes(context -> visit(
						context.getSource(),
						UuidArgument.getUuid(context, "uuid")
					))))
			.then(Commands.literal("preview").executes(context -> preview(context.getSource())))
			.then(Commands.literal("deploy").executes(context -> deploy(context.getSource())))
			.then(Commands.literal("pack").executes(context -> pack(context.getSource())))
			.then(Commands.literal("trust")
				.then(Commands.literal("add")
					.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> trust(
							context.getSource(), EntityArgument.getPlayer(context, "player"), true
						))))
				.then(Commands.literal("remove")
					.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> trust(
							context.getSource(), EntityArgument.getPlayer(context, "player"), false
						)))))
			.then(Commands.literal("access")
				.then(Commands.literal("private")
					.executes(context -> access(context.getSource(), CabinEntryPermission.OWNER_ONLY)))
				.then(Commands.literal("trusted")
					.executes(context -> access(context.getSource(), CabinEntryPermission.TRUSTED_PLAYERS))))
			.then(Commands.literal("reconcile")
				.then(Commands.argument("uuid", UuidArgument.uuid())
					.executes(context -> reconcile(
						context.getSource(),
						UuidArgument.getUuid(context, "uuid")
					))))
			.then(Commands.literal("recover-item")
				.then(Commands.argument("uuid", UuidArgument.uuid())
					.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> recoverItem(
							context.getSource(),
							UuidArgument.getUuid(context, "uuid"),
							EntityArgument.getPlayer(context, "player")
						)))))
			.then(Commands.literal("visit-test").executes(context -> visitTest(context.getSource())))
			.then(Commands.literal("leave-test").executes(context -> leaveTest(context.getSource()))));
	}

	private static int status(CommandSourceStack source) {
		boolean loaded = source.getServer().getLevel(PocketDimension.LEVEL_KEY) != null;
		CabinRegistry registry = CabinRegistry.get(source.getServer());
		source.sendSuccess(() -> Component.literal(
			"Portable Pocket Cabin " + PortablePocketCabin.VERSION
				+ " | delivery=6 | pocket_dimension=" + (loaded ? "ready" : "missing")
				+ " | cabins=" + registry.size() + " | next_cell=" + registry.nextCellIndex()
				+ " | simulated_cabins=" + CabinSimulation.ticketedCabinCount(source.getServer())
		), false);
		return loaded ? 1 : 0;
	}

	private static int createForSource(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception exception) {
			source.sendFailure(Component.literal("Specify an online player when running this command from the console."));
			return 0;
		}
		return create(source, player);
	}

	private static int create(CommandSourceStack source, ServerPlayer owner) {
		CabinRegistry registry = CabinRegistry.get(source.getServer());
		CabinRecord existing = registry.findByOwner(owner.getUUID()).orElse(null);
		if (existing != null) {
			source.sendFailure(Component.literal(owner.getGameProfile().name() + " already owns cabin ")
				.append(copyableUuid(existing.uuid())));
			return 0;
		}
		if (owner.getInventory().getFreeSlot() < 0) {
			source.sendFailure(Component.literal(owner.getGameProfile().name()
				+ " needs one free inventory slot for the packed cabin."));
			return 0;
		}

		CabinRecord cabin;
		try {
			cabin = registry.create(owner.getUUID());
		} catch (IllegalStateException exception) {
			source.sendFailure(Component.literal(exception.getMessage()));
			return 0;
		}
		CabinRegistry.flush(source.getServer());

		ServerLevel pocket = source.getServer().getLevel(PocketDimension.LEVEL_KEY);
		if (pocket != null) {
			PocketDimension.ensureDebugMarker(pocket, cabin.cellIndex());
		}
		if (!CabinItems.give(owner.getInventory(),
			CabinItems.createBound(cabin, cabin.packedItemGeneration(), false))) {
			throw new IllegalStateException("Reserved packed-cabin inventory slot became unavailable");
		}
		source.sendSuccess(() -> Component.literal("Created cabin ")
			.append(copyableUuid(cabin.uuid()))
			.append(" for " + owner.getGameProfile().name() + " at cell " + cabin.cellIndex() + "."), true);
		return 1;
	}

	private static int list(CommandSourceStack source) {
		List<CabinRecord> cabins = CabinRegistry.get(source.getServer()).cabins();
		if (cabins.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No cabin records exist."), false);
			return 1;
		}

		source.sendSuccess(() -> Component.literal("Cabin records (" + cabins.size() + "):"), false);
		for (CabinRecord cabin : cabins) {
			source.sendSuccess(() -> format(cabin), false);
		}
		return cabins.size();
	}

	private static int inspect(CommandSourceStack source, UUID cabinId) {
		CabinRecord cabin = CabinRegistry.get(source.getServer()).find(cabinId).orElse(null);
		if (cabin == null) {
			source.sendFailure(Component.literal("No cabin record exists for " + cabinId + "."));
			return 0;
		}

		var center = PocketDimension.cellCenter(cabin.cellIndex());
		String exterior = cabin.exterior()
			.map(value -> " exterior=" + value.dimension().identifier() + "@"
				+ value.anchor().getX() + "," + value.anchor().getY() + "," + value.anchor().getZ()
				+ "," + value.facing().getSerializedName())
			.orElse(" exterior=none");
		String lastExterior = cabin.lastExterior()
			.map(value -> " last_exterior=" + value.dimension().identifier() + "@"
				+ value.anchor().getX() + "," + value.anchor().getY() + "," + value.anchor().getZ())
			.orElse(" last_exterior=none");
		source.sendSuccess(() -> format(cabin).copy()
			.append(" center=" + center.getX() + "," + center.getY() + "," + center.getZ()
				+ exterior + lastExterior + " interior_generated=" + cabin.interiorGenerated()
				+ " item_generation=" + cabin.packedItemGeneration()
				+ " cleanup_pending=" + cabin.exteriorCleanupPending()
				+ " access=" + cabin.entryPermission().serializedName()
				+ " trusted=" + cabin.trustedPlayers()), false);
		return 1;
	}

	private static int visit(CommandSourceStack source, UUID cabinId) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception exception) {
			source.sendFailure(Component.literal("This command must be run by a player."));
			return 0;
		}

		CabinRecord cabin = CabinRegistry.get(source.getServer()).find(cabinId).orElse(null);
		if (cabin == null) {
			source.sendFailure(Component.literal("No cabin record exists for " + cabinId + "."));
			return 0;
		}

		ServerLevel pocket = source.getServer().getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			source.sendFailure(Component.literal("Pocket dimension is unavailable."));
			return 0;
		}

		if (!cabin.interiorGenerated()) {
			PocketDimension.ensureDebugMarker(pocket, cabin.cellIndex());
		}
		var destination = cabin.interiorGenerated()
			? PocketDimension.interiorEntrance(cabin.cellIndex())
			: PocketDimension.cellCenter(cabin.cellIndex()).offset(0, 1, 2);
		player.teleportTo(
			pocket,
			destination.getX() + 0.5,
			destination.getY(),
			destination.getZ() + 0.5,
			Set.of(),
			180.0F,
			0.0F,
			false
		);
		source.sendSuccess(() -> Component.literal("Visited cabin ")
			.append(copyableUuid(cabin.uuid()))
			.append((cabin.interiorGenerated() ? " interior" : " debug marker")
				+ " at cell " + cabin.cellIndex() + "."), false);
		return 1;
	}

	static Component format(CabinRecord cabin) {
		return Component.empty()
			.append(copyableUuid(cabin.uuid()))
			.append(" owner=" + cabin.owner() + " cell=" + cabin.cellIndex()
				+ " state=" + cabin.lifecycle()
				+ " access=" + cabin.entryPermission().serializedName()
				+ " trusted=" + cabin.trustedPlayers().size()
				+ cabin.exterior().map(value -> " exterior=" + value.dimension().identifier() + "@"
					+ value.anchor().getX() + "," + value.anchor().getY() + "," + value.anchor().getZ())
					.orElse(""));
	}

	private static int preview(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception exception) {
			source.sendFailure(Component.literal("This command must be run by a player."));
			return 0;
		}
		return CabinPlacement.preview(player);
	}

	private static int deploy(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception exception) {
			source.sendFailure(Component.literal("This command must be run by a player."));
			return 0;
		}
		return CabinPlacement.deploy(player);
	}

	private static int pack(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception exception) {
			source.sendFailure(Component.literal("This command must be run by a player."));
			return 0;
		}
		return CabinPacking.request(player);
	}

	private static int trust(CommandSourceStack source, ServerPlayer target, boolean add) {
		ServerPlayer owner;
		try {
			owner = source.getPlayerOrException();
		} catch (Exception exception) {
			source.sendFailure(Component.literal("This command must be run by the cabin owner."));
			return 0;
		}
		CabinRegistry registry = CabinRegistry.get(source.getServer());
		CabinRecord cabin = registry.findByOwner(owner.getUUID()).orElse(null);
		if (cabin == null) {
			source.sendFailure(Component.literal("You do not own a cabin."));
			return 0;
		}

		try {
			if (add) {
				registry.trust(cabin.uuid(), owner.getUUID(), target.getUUID());
			} else {
				registry.untrust(cabin.uuid(), owner.getUUID(), target.getUUID());
			}
			CabinRegistry.flush(source.getServer());
		} catch (IllegalStateException exception) {
			source.sendFailure(Component.literal(exception.getMessage()));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(
			target.getGameProfile().name() + (add ? " is now trusted." : " is no longer trusted.")
		), true);
		return 1;
	}

	private static int access(CommandSourceStack source, CabinEntryPermission permission) {
		ServerPlayer owner;
		try {
			owner = source.getPlayerOrException();
		} catch (Exception exception) {
			source.sendFailure(Component.literal("This command must be run by the cabin owner."));
			return 0;
		}
		CabinRegistry registry = CabinRegistry.get(source.getServer());
		CabinRecord cabin = registry.findByOwner(owner.getUUID()).orElse(null);
		if (cabin == null) {
			source.sendFailure(Component.literal("You do not own a cabin."));
			return 0;
		}
		registry.setEntryPermission(cabin.uuid(), owner.getUUID(), permission);
		CabinRegistry.flush(source.getServer());
		source.sendSuccess(() -> Component.literal("Cabin entry is now "
			+ (permission == CabinEntryPermission.OWNER_ONLY ? "private." : "open to trusted players.")), true);
		return 1;
	}

	private static int reconcile(CommandSourceStack source, UUID cabinId) {
		if (CabinRegistry.get(source.getServer()).find(cabinId).isEmpty()) {
			source.sendFailure(Component.literal("No cabin record exists for " + cabinId + "."));
			return 0;
		}
		String result = CabinReconciliation.reconcile(source.getServer(), cabinId);
		source.sendSuccess(() -> Component.literal("Cabin " + cabinId + ": " + result + "."), true);
		return 1;
	}

	private static int recoverItem(CommandSourceStack source, UUID cabinId, ServerPlayer target) {
		CabinRegistry registry = CabinRegistry.get(source.getServer());
		CabinRecord cabin = registry.find(cabinId).orElse(null);
		if (cabin == null) {
			source.sendFailure(Component.literal("No cabin record exists for " + cabinId + "."));
			return 0;
		}
		if (!cabin.owner().equals(target.getUUID())) {
			source.sendFailure(Component.literal("Recovery target must be the cabin owner."));
			return 0;
		}
		if (target.getInventory().getFreeSlot() < 0) {
			source.sendFailure(Component.literal("The cabin owner needs one free inventory slot."));
			return 0;
		}
		if (cabin.lifecycle() == CabinLifecycle.DEPLOYED
			&& CabinReconciliation.hasValidProjection(source.getServer(), cabin)) {
			source.sendFailure(Component.literal("Cannot recover an item while the deployed exterior is valid."));
			return 0;
		}

		CabinReconciliation.reconcile(source.getServer(), cabinId);
		cabin = registry.find(cabinId).orElseThrow();
		if (cabin.lifecycle() == CabinLifecycle.DEPLOYED) {
			source.sendFailure(Component.literal("Reconciliation restored the deployed exterior; no item was issued."));
			return 0;
		}
		CabinRecord packed = registry.recoverPacked(cabinId);
		CabinRegistry.flush(source.getServer());
		if (!CabinItems.give(target.getInventory(),
			CabinItems.createBound(packed, packed.packedItemGeneration(), false))) {
			source.sendFailure(Component.literal("The packed item could not be delivered."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("Issued packed cabin " + cabinId
			+ " generation " + packed.packedItemGeneration() + " to " + target.getGameProfile().name() + "."), true);
		return 1;
	}

	private static Component copyableUuid(UUID cabinId) {
		String value = cabinId.toString();
		return Component.literal(value).withStyle(style -> style
			.withColor(ChatFormatting.AQUA)
			.withUnderlined(true)
			.withClickEvent(new ClickEvent.CopyToClipboard(value))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy cabin UUID"))));
	}

	private static int visitTest(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception exception) {
			source.sendFailure(Component.literal("This command must be run by a player."));
			return 0;
		}

		ServerLevel pocket = source.getServer().getLevel(PocketDimension.LEVEL_KEY);
		if (pocket == null) {
			source.sendFailure(Component.literal("Pocket dimension is unavailable."));
			return 0;
		}

		PocketDimension.ensureTestPlatform(pocket);
		player.teleportTo(pocket, 0.5, 64.0, 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
		source.sendSuccess(() -> Component.literal("Entered the pocket-dimension test platform."), false);
		return 1;
	}

	private static int leaveTest(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception exception) {
			source.sendFailure(Component.literal("This command must be run by a player."));
			return 0;
		}

		if (!player.level().dimension().equals(PocketDimension.LEVEL_KEY)) {
			source.sendFailure(Component.literal("You are not in the pocket dimension."));
			return 0;
		}

		ServerLevel overworld = source.getServer().overworld();
		var spawn = overworld.getRespawnData().pos();
		player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5,
			Set.of(), player.getYRot(), player.getXRot(), false);
		source.sendSuccess(() -> Component.literal("Returned to the Overworld spawn."), false);
		return 1;
	}
}
