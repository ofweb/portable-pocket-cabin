package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Durable, target-keyed cabin upgrade funds and the recoverable installation transition. */
record CabinUpgradeState(
	List<CabinUpgradeState.Fund> funds,
	Optional<CabinUpgradeState.Installation> installation,
	long fundRevision,
	CabinWindowState windows,
	Optional<CabinUpgradeState.WindowReversal> reversal
) {
	static final CabinUpgradeState EMPTY = new CabinUpgradeState(
		List.of(), Optional.empty(), 0L, CabinWindowState.EMPTY, Optional.empty()
	);

	private static final Codec<CabinUpgradeState> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Fund.CODEC.listOf().fieldOf("funds").forGetter(CabinUpgradeState::funds),
		Installation.CODEC.optionalFieldOf("installation").forGetter(CabinUpgradeState::installation),
		Codec.LONG.optionalFieldOf("fund_revision", 0L).forGetter(CabinUpgradeState::fundRevision),
		CabinWindowState.CODEC.optionalFieldOf("windows", CabinWindowState.EMPTY)
			.forGetter(CabinUpgradeState::windows),
		WindowReversal.CODEC.optionalFieldOf("window_reversal").forGetter(CabinUpgradeState::reversal)
	).apply(instance, CabinUpgradeState::new));

	private static final Codec<CabinUpgradeState> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		LegacyTrackedUpgrade.CODEC.optionalFieldOf("tracked")
			.forGetter(state -> state.funds().isEmpty()
				? Optional.empty()
				: Optional.of(LegacyTrackedUpgrade.from(state.funds().getFirst()))),
		Installation.CODEC.optionalFieldOf("installation").forGetter(CabinUpgradeState::installation)
	).apply(instance, CabinUpgradeState::fromLegacy));

	static final Codec<CabinUpgradeState> CODEC = Codec.withAlternative(CURRENT_CODEC, LEGACY_CODEC);

	CabinUpgradeState {
		Objects.requireNonNull(funds, "funds");
		Objects.requireNonNull(installation, "installation");
		Objects.requireNonNull(windows, "windows");
		Objects.requireNonNull(reversal, "reversal");
		funds = List.copyOf(funds);
		if (fundRevision < 0) {
			throw new IllegalArgumentException("Upgrade fund revision must not be negative");
		}
		Set<Target> targets = new HashSet<>();
		for (Fund fund : funds) {
			if (!targets.add(fund.target())) {
				throw new IllegalArgumentException("Only one fund may exist for an upgrade target");
			}
			if (fund.stacks().isEmpty()) {
				throw new IllegalArgumentException("Empty upgrade funds must not be persisted");
			}
		}
		if (installation.isPresent()) {
			Fund installingFund = funds.stream()
				.filter(fund -> fund.target().equals(installation.get().target()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("An installation must retain its target fund"));
			if (!installingFund.isComplete()) {
				throw new IllegalArgumentException("An installation must retain a complete target fund");
			}
		}
		if (installation.isPresent() && reversal.isPresent()) {
			throw new IllegalArgumentException("A cabin may have only one upgrade operation in progress");
		}
		if (reversal.isPresent()) {
			WindowReversal value = reversal.get();
			int currentTier = windows.tier(value.target().windowIdentity());
			int requiredTier = value.phase() == ReversalPhase.WORLD_PENDING
				? value.expectedTier() : value.targetTier();
			if (currentTier != requiredTier) {
				throw new IllegalArgumentException("Window reversal phase does not match cabin window state");
			}
			for (Fund invalidated : value.invalidatedFunds()) {
				Fund current = funds.stream()
					.filter(fund -> fund.target().equals(invalidated.target()))
					.findFirst().orElse(null);
				boolean present = current != null;
				if (present != (value.phase() == ReversalPhase.WORLD_PENDING)) {
					throw new IllegalArgumentException("Window reversal phase does not match invalidated funds");
				}
				if (current != null && !fundMatches(current, invalidated)) {
					throw new IllegalArgumentException("Window reversal invalidated fund payload changed");
				}
			}
			if (value.phase() == ReversalPhase.WORLD_PENDING) {
				CabinWindowState.ReversalResult expected = value.action() == ReversalAction.DOWNGRADE
					? windows.downgrade(value.target().windowIdentity(), value.expectedTier())
					: windows.remove(value.target().windowIdentity(), value.expectedTier());
				if (!stacksMatch(expected.refundStacks(), value.receiptRefund())) {
					throw new IllegalArgumentException("Window reversal receipt payload changed");
				}
			}
		}
	}

	@Override
	public List<Fund> funds() {
		return List.copyOf(funds);
	}

	Optional<Fund> fund(Target target) {
		return funds.stream().filter(value -> value.target().equals(target)).findFirst();
	}

	CabinUpgradeState withFund(Fund value) {
		if (operationInProgress()) {
			throw new IllegalStateException("Cabin upgrade state is locked by an active operation");
		}
		List<Fund> updated = new ArrayList<>(funds.size() + 1);
		boolean replaced = false;
		for (Fund existing : funds) {
			if (existing.target().equals(value.target())) {
				updated.add(value);
				replaced = true;
			} else {
				updated.add(existing);
			}
		}
		if (!replaced) {
			updated.add(value);
		}
		return new CabinUpgradeState(updated, installation, Math.addExact(fundRevision, 1L), windows, reversal);
	}

	CabinUpgradeState withoutFund(Target target) {
		if (operationInProgress()) {
			throw new IllegalStateException("Cabin upgrade state is locked by an active operation");
		}
		List<Fund> updated = funds.stream().filter(value -> !value.target().equals(target)).toList();
		return updated.size() == funds.size()
			? this
			: new CabinUpgradeState(updated, installation, Math.addExact(fundRevision, 1L), windows, reversal);
	}

	CabinUpgradeState withInstallation(Installation value) {
		if (operationInProgress()) {
			throw new IllegalStateException("A cabin upgrade operation is already in progress");
		}
		return new CabinUpgradeState(funds, Optional.of(value), fundRevision, windows, reversal);
	}

	CabinUpgradeState completeInstallation(Target target) {
		List<Fund> remaining = funds.stream().filter(value -> !value.target().equals(target)).toList();
		return new CabinUpgradeState(
			remaining, Optional.empty(), Math.addExact(fundRevision, 1L), windows, reversal
		);
	}

	CabinUpgradeState withWindows(CabinWindowState value) {
		return new CabinUpgradeState(funds, installation, fundRevision, value, reversal);
	}

	boolean operationInProgress() {
		return installation.isPresent() || reversal.isPresent();
	}

	CabinUpgradeState withReversal(WindowReversal value) {
		if (operationInProgress()) {
			throw new IllegalStateException("A cabin upgrade operation is already in progress");
		}
		return new CabinUpgradeState(funds, installation, fundRevision, windows, Optional.of(value));
	}

	CabinUpgradeState commitReversal(CabinWindowState resultingWindows) {
		WindowReversal value = reversal.orElseThrow();
		if (value.phase() != ReversalPhase.WORLD_PENDING) {
			return this;
		}
		Set<Target> invalidated = value.invalidatedFunds().stream()
			.map(Fund::target).collect(java.util.stream.Collectors.toSet());
		List<Fund> remaining = funds.stream().filter(fund -> !invalidated.contains(fund.target())).toList();
		return new CabinUpgradeState(
			remaining, installation, Math.addExact(fundRevision, 1L), resultingWindows,
			Optional.of(value.withPhase(ReversalPhase.EJECTION_PENDING))
		);
	}

	CabinUpgradeState completeReversal() {
		WindowReversal value = reversal.orElseThrow();
		if (value.phase() != ReversalPhase.EJECTION_PENDING) {
			throw new IllegalStateException("Window reversal has not reached refund ejection");
		}
		return new CabinUpgradeState(funds, installation, fundRevision, windows, Optional.empty());
	}

	CabinUpgradeState(List<Fund> funds, Optional<Installation> installation, long fundRevision) {
		this(funds, installation, fundRevision, CabinWindowState.EMPTY, Optional.empty());
	}

	CabinUpgradeState(
		List<Fund> funds, Optional<Installation> installation, long fundRevision, CabinWindowState windows
	) {
		this(funds, installation, fundRevision, windows, Optional.empty());
	}

	private static CabinUpgradeState fromLegacy(
		Optional<LegacyTrackedUpgrade> tracked, Optional<Installation> installation
	) {
		List<Fund> migrated = tracked
			.filter(value -> !value.fund().isEmpty())
			.map(value -> List.of(value.asFund()))
			.orElse(List.of());
		return new CabinUpgradeState(
			migrated, installation, migrated.isEmpty() ? 0L : 1L, CabinWindowState.EMPTY, Optional.empty()
		);
	}

	record Target(Identifier type, String key) {
		private static final Identifier GENERAL_SPACE = PortablePocketCabin.id("general_space");
		private static final Identifier WINDOW = PortablePocketCabin.id("window");
		static final Codec<Target> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("type").forGetter(Target::type),
			Codec.STRING.fieldOf("key").forGetter(Target::key)
		).apply(instance, Target::new));

		Target {
			Objects.requireNonNull(type, "type");
			Objects.requireNonNull(key, "key");
			if (key.isBlank()) {
				throw new IllegalArgumentException("Upgrade target key must not be blank");
			}
			if (type.equals(GENERAL_SPACE)) {
				try {
					int size = Integer.parseInt(key);
					if (size <= CabinProgression.INITIAL_GENERAL_SIZE
						|| !CabinProgression.isSupportedGeneralSize(size)) {
						throw new IllegalArgumentException("General-space target is outside the supported range");
					}
				} catch (NumberFormatException exception) {
					throw new IllegalArgumentException("General-space target key must be a size", exception);
				}
			} else if (type.equals(WINDOW)) {
				CabinWindowState.Identity.parse(key);
			} else {
				throw new IllegalArgumentException("Unsupported cabin upgrade target type " + type);
			}
		}

		static Target generalSpace(int targetSize) {
			if (targetSize <= CabinProgression.INITIAL_GENERAL_SIZE) {
				throw new IllegalArgumentException("General-space target must expand the initial cabin");
			}
			return new Target(GENERAL_SPACE, Integer.toString(targetSize));
		}

		static Target window(CabinWindowState.Identity identity) {
			return new Target(WINDOW, identity.key());
		}

		boolean isGeneralSpace() {
			return type.equals(GENERAL_SPACE);
		}

		boolean isWindow() {
			return type.equals(WINDOW);
		}

		CabinWindowState.Identity windowIdentity() {
			if (!isWindow()) {
				throw new IllegalStateException("Upgrade target is not a cabin window");
			}
			return CabinWindowState.Identity.parse(key);
		}

		int generalSpaceSize() {
			if (!isGeneralSpace()) {
				throw new IllegalStateException("Upgrade target is not a general-space expansion");
			}
			return Integer.parseInt(key);
		}
	}

	record Requirement(Identifier itemId, int count) {
		static final Codec<Requirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("item").forGetter(Requirement::itemId),
			Codec.INT.fieldOf("count").forGetter(Requirement::count)
		).apply(instance, Requirement::new));

		Requirement {
			Objects.requireNonNull(itemId, "itemId");
			if (count <= 0) {
				throw new IllegalArgumentException("Upgrade requirement count must be positive");
			}
		}
	}

	record Fund(Target target, List<Requirement> requirements, List<ItemStack> stacks) {
		static final Codec<Fund> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Target.CODEC.fieldOf("target").forGetter(Fund::target),
			Requirement.CODEC.listOf().fieldOf("requirements").forGetter(Fund::requirements),
			ItemStack.CODEC.listOf().optionalFieldOf("stacks", List.of()).forGetter(Fund::stacks)
		).apply(instance, Fund::new));

		Fund {
			Objects.requireNonNull(target, "target");
			Objects.requireNonNull(requirements, "requirements");
			Objects.requireNonNull(stacks, "stacks");
			requirements = List.copyOf(requirements);
			stacks = copyStacks(stacks);
			validateContents("Upgrade fund", requirements, stacks);
		}

		@Override
		public List<ItemStack> stacks() {
			return copyStacks(stacks);
		}

		int fundedCount(Identifier itemId) {
			return stacks.stream()
				.filter(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId))
				.mapToInt(ItemStack::getCount)
				.sum();
		}

		boolean isComplete() {
			return requirements.stream().allMatch(value -> fundedCount(value.itemId()) == value.count());
		}

		Fund withStacks(List<ItemStack> value) {
			return new Fund(target, requirements, value);
		}
	}

	/** Decoder-only representation of schema 4's singleton tracked fund. */
	private record LegacyTrackedUpgrade(Target target, List<Requirement> requirements, List<ItemStack> fund) {
		private static final Codec<LegacyTrackedUpgrade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Target.CODEC.fieldOf("target").forGetter(LegacyTrackedUpgrade::target),
			Requirement.CODEC.listOf().fieldOf("requirements").forGetter(LegacyTrackedUpgrade::requirements),
			ItemStack.CODEC.listOf().optionalFieldOf("fund", List.of()).forGetter(LegacyTrackedUpgrade::fund)
		).apply(instance, LegacyTrackedUpgrade::new));

		LegacyTrackedUpgrade {
			requirements = List.copyOf(requirements);
			fund = copyStacks(fund);
			validateContents("Tracked upgrade", requirements, fund);
		}

		private Fund asFund() {
			return new Fund(target, requirements, fund);
		}

		private static LegacyTrackedUpgrade from(Fund fund) {
			return new LegacyTrackedUpgrade(fund.target(), fund.requirements(), fund.stacks());
		}
	}

	enum ReversalAction {
		DOWNGRADE("downgrade"),
		REMOVE("remove");

		static final Codec<ReversalAction> CODEC = Codec.STRING.xmap(ReversalAction::parse, value -> value.key);
		private final String key;

		ReversalAction(String key) {
			this.key = key;
		}

		private static ReversalAction parse(String key) {
			for (ReversalAction value : values()) {
				if (value.key.equals(key)) {
					return value;
				}
			}
			throw new IllegalArgumentException("Unsupported window reversal action " + key);
		}
	}

	enum ReversalPhase {
		WORLD_PENDING("world_pending"),
		EJECTION_PENDING("ejection_pending");

		static final Codec<ReversalPhase> CODEC = Codec.STRING.xmap(ReversalPhase::parse, value -> value.key);
		private final String key;

		ReversalPhase(String key) {
			this.key = key;
		}

		private static ReversalPhase parse(String key) {
			for (ReversalPhase value : values()) {
				if (value.key.equals(key)) {
					return value;
				}
			}
			throw new IllegalArgumentException("Unsupported window reversal phase " + key);
		}
	}

	record WindowReversal(
		UUID operationId,
		ReversalAction action,
		Target target,
		int expectedTier,
		int targetTier,
		List<ItemStack> receiptRefund,
		List<Fund> invalidatedFunds,
		ReversalPhase phase
	) {
		static final Codec<WindowReversal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.STRING_CODEC.fieldOf("operation_id").forGetter(WindowReversal::operationId),
			ReversalAction.CODEC.fieldOf("action").forGetter(WindowReversal::action),
			Target.CODEC.fieldOf("target").forGetter(WindowReversal::target),
			Codec.INT.fieldOf("expected_tier").forGetter(WindowReversal::expectedTier),
			Codec.INT.fieldOf("target_tier").forGetter(WindowReversal::targetTier),
			ItemStack.CODEC.listOf().optionalFieldOf("receipt_refund", List.of())
				.forGetter(WindowReversal::receiptRefund),
			Fund.CODEC.listOf().optionalFieldOf("invalidated_funds", List.of())
				.forGetter(WindowReversal::invalidatedFunds),
			ReversalPhase.CODEC.fieldOf("phase").forGetter(WindowReversal::phase)
		).apply(instance, WindowReversal::new));

		WindowReversal {
			Objects.requireNonNull(operationId, "operationId");
			Objects.requireNonNull(action, "action");
			Objects.requireNonNull(target, "target");
			Objects.requireNonNull(receiptRefund, "receiptRefund");
			Objects.requireNonNull(invalidatedFunds, "invalidatedFunds");
			Objects.requireNonNull(phase, "phase");
			if (!target.isWindow() || expectedTier < 1 || expectedTier > CabinWindowState.MAX_TIER) {
				throw new IllegalArgumentException("Window reversal must target an installed cabin window");
			}
			if (action == ReversalAction.DOWNGRADE
				&& (expectedTier == 1 || targetTier != expectedTier - 1)) {
				throw new IllegalArgumentException("Window downgrade must remove exactly one tier");
			}
			if (action == ReversalAction.REMOVE && targetTier != 0) {
				throw new IllegalArgumentException("Window removal must target the uninstalled state");
			}
			receiptRefund = copyStacks(receiptRefund);
			invalidatedFunds = List.copyOf(invalidatedFunds);
			Set<Target> invalidatedTargets = new HashSet<>();
			for (Fund fund : invalidatedFunds) {
				if (!invalidatedTargets.add(fund.target())) {
					throw new IllegalArgumentException("Window reversal invalidated funds must be unique by target");
				}
			}
		}

		@Override
		public List<ItemStack> receiptRefund() {
			return copyStacks(receiptRefund);
		}

		@Override
		public List<Fund> invalidatedFunds() {
			return List.copyOf(invalidatedFunds);
		}

		List<ItemStack> payloadStacks() {
			List<ItemStack> result = new ArrayList<>(receiptRefund);
			for (Fund fund : invalidatedFunds) {
				result.addAll(fund.stacks());
			}
			return copyStacks(result);
		}

		WindowReversal withPhase(ReversalPhase value) {
			return new WindowReversal(
				operationId, action, target, expectedTier, targetTier,
				receiptRefund, invalidatedFunds, value
			);
		}
	}

	record Installation(UUID operationId, Target target, int expectedState, int targetState) {
		private static final Codec<Installation> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.STRING_CODEC.fieldOf("operation_id").forGetter(Installation::operationId),
			Target.CODEC.fieldOf("target").forGetter(Installation::target),
			Codec.INT.fieldOf("expected_state").forGetter(Installation::expectedState),
			Codec.INT.fieldOf("target_state").forGetter(Installation::targetState)
		).apply(instance, Installation::new));
		private static final Codec<Installation> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.STRING_CODEC.fieldOf("operation_id").forGetter(Installation::operationId),
			Target.CODEC.fieldOf("target").forGetter(Installation::target),
			Codec.INT.fieldOf("expected_size").forGetter(Installation::expectedState)
		).apply(instance, (operationId, target, expectedSize) ->
			new Installation(operationId, target, expectedSize, target.generalSpaceSize())
		));
		static final Codec<Installation> CODEC = Codec.withAlternative(CURRENT_CODEC, LEGACY_CODEC);

		Installation {
			Objects.requireNonNull(operationId, "operationId");
			Objects.requireNonNull(target, "target");
			if (target.isGeneralSpace()) {
				if (!CabinProgression.isSupportedGeneralSize(expectedState)
					|| targetState != expectedState + CabinProgression.GENERAL_SIZE_STEP
					|| target.generalSpaceSize() != targetState) {
					throw new IllegalArgumentException("Installation must target the next general-space size");
				}
			} else if (target.isWindow()) {
				if (expectedState < 0 || expectedState >= CabinWindowState.MAX_TIER
					|| targetState != expectedState + 1) {
					throw new IllegalArgumentException("Installation must target the next cabin window tier");
				}
			} else {
				throw new IllegalArgumentException("Installation target type is unsupported");
			}
		}

		static Installation generalSpace(UUID operationId, Target target, int expectedSize) {
			return new Installation(operationId, target, expectedSize, expectedSize + CabinProgression.GENERAL_SIZE_STEP);
		}

		static Installation window(UUID operationId, Target target, int expectedTier) {
			return new Installation(operationId, target, expectedTier, expectedTier + 1);
		}
	}

	private static void validateContents(
		String description, List<Requirement> requirements, List<ItemStack> stacks
	) {
		if (requirements.isEmpty()) {
			throw new IllegalArgumentException(description + " must have requirements");
		}
		Set<Identifier> uniqueItems = new HashSet<>();
		Map<Identifier, Integer> limits = new HashMap<>();
		for (Requirement requirement : requirements) {
			if (!uniqueItems.add(requirement.itemId())) {
				throw new IllegalArgumentException(description + " requirements must be consolidated by item");
			}
			limits.put(requirement.itemId(), requirement.count());
		}
		Map<Identifier, Integer> totals = new HashMap<>();
		for (ItemStack stack : stacks) {
			if (stack.isEmpty()) {
				throw new IllegalArgumentException(description + " cannot contain empty stacks");
			}
			Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
			Integer limit = limits.get(itemId);
			if (limit == null) {
				throw new IllegalArgumentException(description + " contains an item outside its requirements");
			}
			int total = Math.addExact(totals.getOrDefault(itemId, 0), stack.getCount());
			if (total > limit) {
				throw new IllegalArgumentException(description + " exceeds its requirement for " + itemId);
			}
			totals.put(itemId, total);
		}
	}

	private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
		List<ItemStack> copies = new ArrayList<>(stacks.size());
		for (ItemStack stack : stacks) {
			copies.add(stack.copy());
		}
		return List.copyOf(copies);
	}

	private static boolean fundMatches(Fund first, Fund second) {
		return first.target().equals(second.target())
			&& first.requirements().equals(second.requirements())
			&& stacksMatch(first.stacks(), second.stacks());
	}

	private static boolean stacksMatch(List<ItemStack> first, List<ItemStack> second) {
		if (first.size() != second.size()) {
			return false;
		}
		for (int index = 0; index < first.size(); index++) {
			if (!ItemStack.matches(first.get(index), second.get(index))) {
				return false;
			}
		}
		return true;
	}
}
