package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Durable purchased-window identity, tier and exact paid-step receipts. */
record CabinWindowState(List<CabinWindowState.Window> windows) {
	static final int MAX_TIER = 6;
	static final CabinWindowState EMPTY = new CabinWindowState(List.of());
	static final Codec<CabinWindowState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Window.CODEC.listOf().optionalFieldOf("windows", List.of()).forGetter(CabinWindowState::windows)
	).apply(instance, CabinWindowState::new));

	CabinWindowState {
		Objects.requireNonNull(windows, "windows");
		windows = List.copyOf(windows);
		Set<Identity> identities = new HashSet<>();
		for (Window window : windows) {
			if (!identities.add(window.identity())) {
				throw new IllegalArgumentException("Only one cabin window may use an identity");
			}
		}
	}

	static CabinWindowState grandfathered() {
		return new CabinWindowState(List.of(
			Window.grandfathered(new Identity(Wall.LEFT, 0)),
			Window.grandfathered(new Identity(Wall.RIGHT, 0))
		));
	}

	Optional<Window> window(Identity identity) {
		return windows.stream().filter(value -> value.identity().equals(identity)).findFirst();
	}

	int tier(Identity identity) {
		return window(identity).map(Window::tier).orElse(0);
	}

	CabinWindowState install(Identity identity, int expectedTier, List<ItemStack> paidStacks) {
		int currentTier = tier(identity);
		if (currentTier != expectedTier || expectedTier >= MAX_TIER) {
			throw new IllegalStateException("Cabin window tier changed before installation could commit");
		}
		Receipt receipt = new Receipt(expectedTier + 1, paidStacks);
		List<Window> updated = new ArrayList<>(windows.size() + (currentTier == 0 ? 1 : 0));
		boolean replaced = false;
		for (Window current : windows) {
			if (current.identity().equals(identity)) {
				updated.add(current.withReceipt(receipt));
				replaced = true;
			} else {
				updated.add(current);
			}
		}
		if (!replaced) {
			updated.add(new Window(identity, 1, List.of(receipt)));
		}
		return new CabinWindowState(updated);
	}

	ReversalResult downgrade(Identity identity, int expectedTier) {
		Window current = requireTier(identity, expectedTier);
		if (expectedTier <= 1) {
			throw new IllegalStateException("A tier-one cabin window cannot be downgraded");
		}
		Receipt removed = current.receipts().getLast();
		List<Receipt> remainingReceipts = current.receipts().subList(0, current.receipts().size() - 1);
		Window downgraded = new Window(identity, expectedTier - 1, remainingReceipts);
		return new ReversalResult(replace(identity, Optional.of(downgraded)), removed.stacks());
	}

	ReversalResult remove(Identity identity, int expectedTier) {
		Window current = requireTier(identity, expectedTier);
		List<ItemStack> refund = new ArrayList<>();
		for (Receipt receipt : current.receipts()) {
			refund.addAll(receipt.stacks());
		}
		return new ReversalResult(replace(identity, Optional.empty()), refund);
	}

	private Window requireTier(Identity identity, int expectedTier) {
		Window current = window(identity)
			.orElseThrow(() -> new IllegalStateException("That cabin window is not installed"));
		if (current.tier() != expectedTier) {
			throw new IllegalStateException("Cabin window tier changed before reversal could commit");
		}
		return current;
	}

	private CabinWindowState replace(Identity identity, Optional<Window> replacement) {
		List<Window> updated = new ArrayList<>(windows.size());
		for (Window current : windows) {
			if (current.identity().equals(identity)) {
				replacement.ifPresent(updated::add);
			} else {
				updated.add(current);
			}
		}
		return new CabinWindowState(updated);
	}

	record ReversalResult(CabinWindowState state, List<ItemStack> refundStacks) {
		ReversalResult {
			Objects.requireNonNull(state, "state");
			refundStacks = copyStacks(refundStacks);
		}

		@Override
		public List<ItemStack> refundStacks() {
			return copyStacks(refundStacks);
		}
	}

	enum Wall {
		LEFT("left"),
		REAR("rear"),
		RIGHT("right");

		static final Codec<Wall> CODEC = Codec.STRING.xmap(Wall::parse, Wall::key);
		private final String key;

		Wall(String key) {
			this.key = key;
		}

		String key() {
			return key;
		}

		static Wall parse(String value) {
			for (Wall wall : values()) {
				if (wall.key.equals(value)) {
					return wall;
				}
			}
			throw new IllegalArgumentException("Unsupported cabin window wall " + value);
		}
	}

	record Identity(Wall wall, int slot) {
		static final Codec<Identity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Wall.CODEC.fieldOf("wall").forGetter(Identity::wall),
			Codec.INT.fieldOf("slot").forGetter(Identity::slot)
		).apply(instance, Identity::new));

		Identity {
			Objects.requireNonNull(wall, "wall");
			if (slot < 0 || slot > 1) {
				throw new IllegalArgumentException("Cabin window slot must be zero or one");
			}
		}

		String key() {
			return wall.key() + "/" + slot;
		}

		static Identity parse(String key) {
			String[] parts = key.split("/", -1);
			if (parts.length != 2) {
				throw new IllegalArgumentException("Cabin window target key must contain wall and slot");
			}
			try {
				return new Identity(Wall.parse(parts[0]), Integer.parseInt(parts[1]));
			} catch (NumberFormatException exception) {
				throw new IllegalArgumentException("Cabin window target slot must be a number", exception);
			}
		}
	}

	record Window(Identity identity, int tier, List<Receipt> receipts) {
		static final Codec<Window> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identity.CODEC.fieldOf("identity").forGetter(Window::identity),
			Codec.INT.fieldOf("tier").forGetter(Window::tier),
			Receipt.CODEC.listOf().fieldOf("receipts").forGetter(Window::receipts)
		).apply(instance, Window::new));

		Window {
			Objects.requireNonNull(identity, "identity");
			Objects.requireNonNull(receipts, "receipts");
			receipts = List.copyOf(receipts);
			if (tier < 1 || tier > MAX_TIER) {
				throw new IllegalArgumentException("Cabin window tier is outside the supported range");
			}
			if (receipts.size() != tier) {
				throw new IllegalArgumentException("Cabin window must retain one receipt per installed tier");
			}
			for (int index = 0; index < receipts.size(); index++) {
				if (receipts.get(index).tier() != index + 1) {
					throw new IllegalArgumentException("Cabin window receipts must be ordered by tier");
				}
			}
		}

		static Window grandfathered(Identity identity) {
			return new Window(identity, 1, List.of(new Receipt(1, List.of())));
		}

		Window withReceipt(Receipt receipt) {
			if (receipt.tier() != tier + 1) {
				throw new IllegalArgumentException("Cabin window receipt must install the next tier");
			}
			List<Receipt> updated = new ArrayList<>(receipts);
			updated.add(receipt);
			return new Window(identity, receipt.tier(), updated);
		}
	}

	record Receipt(int tier, List<ItemStack> stacks) {
		static final Codec<Receipt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("tier").forGetter(Receipt::tier),
			ItemStack.CODEC.listOf().optionalFieldOf("stacks", List.of()).forGetter(Receipt::stacks)
		).apply(instance, Receipt::new));

		Receipt {
			Objects.requireNonNull(stacks, "stacks");
			if (tier < 1 || tier > MAX_TIER) {
				throw new IllegalArgumentException("Cabin window receipt tier is outside the supported range");
			}
			stacks = copyStacks(stacks);
			for (ItemStack stack : stacks) {
				if (stack.isEmpty()) {
					throw new IllegalArgumentException("Cabin window receipt cannot contain an empty stack");
				}
			}
		}

		@Override
		public List<ItemStack> stacks() {
			return copyStacks(stacks);
		}
	}

	private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
		List<ItemStack> copies = new ArrayList<>(stacks.size());
		for (ItemStack stack : stacks) {
			copies.add(stack.copy());
		}
		return List.copyOf(copies);
	}
}
