package dev.portablepocketcabin;

import java.io.IOException;
import java.util.Objects;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;

final class CabinPlayerSave {
	private CabinPlayerSave() { }

	static boolean confirm(ServerPlayer player) {
		var server = player.level().getServer();
		server.getPlayerList().saveAll();
		var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
		player.saveWithoutId(output);
		var expected = output.buildResult();
		var file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID() + ".dat");
		try {
			var saved = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
			return Objects.equals(expected.get("Inventory"), saved.get("Inventory"))
				&& Objects.equals(expected.get("equipment"), saved.get("equipment"));
		} catch (IOException | NbtException exception) {
			PortablePocketCabin.LOGGER.error("Could not confirm player inventory save for {}", player.getUUID(), exception);
			return false;
		}
	}
}
