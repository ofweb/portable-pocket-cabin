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
	CabinWindowState windows
) {
	static final CabinUpgradeState EMPTY = new CabinUpgradeState(
		List.of(), Optional.empty(), 0L, CabinWindowState.EMPTY
	);

	private static final Codec<CabinUpgradeState> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Fund.CODEC.listOf().fieldOf("funds").forGetter(CabinUpgradeState::funds),
		Installation.CODEC.optionalFieldOf("installation").forGetter(CabinUpgradeState::installation),
		Codec.LONG.optionalFieldOf("fund_revision", 0L).forGetter(CabinUpgradeState::fundRevision),
		CabinWindowState.CODEC.optionalFieldOf("windows", CabinWindowState.EMPTY)
			.forGetter(CabinUpgradeState::windows)
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
	}

	@Override
	public List<Fund> funds() {
		return List.copyOf(funds);
	}

	Optional<Fund> fund(Target target) {
		return funds.stream().filter(value -> value.target().equals(target)).findFirst();
	}

	CabinUpgradeState withFund(Fund value) {
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
		return new CabinUpgradeState(updated, installation, Math.addExact(fundRevision, 1L), windows);
	}

	CabinUpgradeState withoutFund(Target target) {
		if (installation.isPresent() && installation.get().target().equals(target)) {
			throw new IllegalStateException("Cannot remove a fund while its installation is in progress");
		}
		List<Fund> updated = funds.stream().filter(value -> !value.target().equals(target)).toList();
		return updated.size() == funds.size()
			? this
			: new CabinUpgradeState(updated, installation, Math.addExact(fundRevision, 1L), windows);
	}

	CabinUpgradeState withInstallation(Installation value) {
		return new CabinUpgradeState(funds, Optional.of(value), fundRevision, windows);
	}

	CabinUpgradeState completeInstallation(Target target) {
		List<Fund> remaining = funds.stream().filter(value -> !value.target().equals(target)).toList();
		return new CabinUpgradeState(remaining, Optional.empty(), Math.addExact(fundRevision, 1L), windows);
	}

	CabinUpgradeState withWindows(CabinWindowState value) {
		return new CabinUpgradeState(funds, installation, fundRevision, value);
	}

	CabinUpgradeState(List<Fund> funds, Optional<Installation> installation, long fundRevision) {
		this(funds, installation, fundRevision, CabinWindowState.EMPTY);
	}

	private static CabinUpgradeState fromLegacy(
		Optional<LegacyTrackedUpgrade> tracked, Optional<Installation> installation
	) {
		List<Fund> migrated = tracked
			.filter(value -> !value.fund().isEmpty())
			.map(value -> List.of(value.asFund()))
			.orElse(List.of());
		return new CabinUpgradeState(
			migrated, installation, migrated.isEmpty() ? 0L : 1L, CabinWindowState.EMPTY
		);
	}

	record Target(Identifier type, String key) {
		private static final Identifier GENERAL_SPACE = PortablePocketCabin.id("general_space");
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
			if (!type.equals(GENERAL_SPACE)) {
				throw new IllegalArgumentException("Unsupported schema 5 upgrade target type " + type);
			}
			try {
				int size = Integer.parseInt(key);
				if (size <= CabinProgression.INITIAL_GENERAL_SIZE
					|| size > CabinProgression.ABSOLUTE_MAX_GENERAL_SIZE) {
					throw new IllegalArgumentException("General-space target is outside the supported range");
				}
			} catch (NumberFormatException exception) {
				throw new IllegalArgumentException("General-space target key must be a size", exception);
			}
		}

		static Target generalSpace(int targetSize) {
			if (targetSize <= CabinProgression.INITIAL_GENERAL_SIZE) {
				throw new IllegalArgumentException("General-space target must expand the initial cabin");
			}
			return new Target(GENERAL_SPACE, Integer.toString(targetSize));
		}

		boolean isGeneralSpace() {
			return type.equals(GENERAL_SPACE);
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

	record Installation(UUID operationId, Target target, int expectedSize) {
		static final Codec<Installation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.STRING_CODEC.fieldOf("operation_id").forGetter(Installation::operationId),
			Target.CODEC.fieldOf("target").forGetter(Installation::target),
			Codec.INT.fieldOf("expected_size").forGetter(Installation::expectedSize)
		).apply(instance, Installation::new));

		Installation {
			Objects.requireNonNull(operationId, "operationId");
			Objects.requireNonNull(target, "target");
			if (expectedSize < CabinProgression.INITIAL_GENERAL_SIZE) {
				throw new IllegalArgumentException("Installation expected size is invalid");
			}
			if (!target.isGeneralSpace() || target.generalSpaceSize() != expectedSize + 1) {
				throw new IllegalArgumentException("Installation target must be the next general-space size");
			}
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
}
