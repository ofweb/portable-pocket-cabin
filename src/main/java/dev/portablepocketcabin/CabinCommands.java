package dev.portablepocketcabin;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

import java.util.Set;

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
			.then(Commands.literal("visit-test").executes(context -> visitTest(context.getSource())))
			.then(Commands.literal("leave-test").executes(context -> leaveTest(context.getSource()))));
	}

	private static int status(CommandSourceStack source) {
		boolean loaded = source.getServer().getLevel(PocketDimension.LEVEL_KEY) != null;
		source.sendSuccess(() -> Component.literal(
			"Portable Pocket Cabin " + PortablePocketCabin.VERSION
				+ " | delivery=1 | pocket_dimension=" + (loaded ? "ready" : "missing")
		), false);
		return loaded ? 1 : 0;
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
