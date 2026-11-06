package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The kitchen book and purchase, and receipts for storage transfers that are not yet confirmed saved. */
record CabinKitchenState(boolean revealed, int level, List<CabinKitchenTransfer> pending) {
	static final CabinKitchenState EMPTY = new CabinKitchenState(false, 0, List.of());
	static final Codec<CabinKitchenState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("revealed", false).forGetter(CabinKitchenState::revealed),
		Codec.INT.optionalFieldOf("level", 0).forGetter(CabinKitchenState::level),
		CabinKitchenTransfer.CODEC.listOf().optionalFieldOf("pending", List.of()).forGetter(CabinKitchenState::pending)
	).apply(instance, CabinKitchenState::new));

	CabinKitchenState {
		if (level < 0 || level > 1 || level > 0 && !revealed)
			throw new IllegalArgumentException("Invalid kitchen installation");
		pending = List.copyOf(pending);
		if (pending.stream().map(CabinKitchenTransfer::player).distinct().count() != pending.size())
			throw new IllegalArgumentException("A player may have only one pending kitchen transfer");
	}

	CabinKitchenState(boolean revealed, int level) {
		this(revealed, level, List.of());
	}

	CabinKitchenState reveal() {
		return new CabinKitchenState(true, level, pending);
	}

	CabinKitchenState install() {
		if (level != 0) throw new IllegalArgumentException("The kitchen is already installed");
		return new CabinKitchenState(revealed, 1, pending);
	}

	Optional<CabinKitchenTransfer> pendingFor(UUID player) {
		return pending.stream().filter(transfer -> transfer.player().equals(player)).findFirst();
	}

	CabinKitchenState withPending(CabinKitchenTransfer transfer) {
		List<CabinKitchenTransfer> updated = new ArrayList<>(pending);
		updated.add(transfer);
		return new CabinKitchenState(revealed, level, updated);
	}

	CabinKitchenState withReplaced(CabinKitchenTransfer transfer) {
		return new CabinKitchenState(revealed, level, pending.stream()
			.map(value -> value.operation().equals(transfer.operation()) ? transfer : value).toList());
	}

	CabinKitchenState withoutPending(UUID operation) {
		return new CabinKitchenState(revealed, level, pending.stream().filter(transfer -> !transfer.operation().equals(operation)).toList());
	}
}
