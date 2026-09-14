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

/** Durable state for the single cabin-bound upgrade fund and its installation transition. */
record CabinUpgradeState(
	Optional<CabinUpgradeState.TrackedUpgrade> tracked,
	Optional<CabinUpgradeState.Installation> installation
) {
	static final CabinUpgradeState EMPTY = new CabinUpgradeState(Optional.empty(), Optional.empty());
	static final Codec<CabinUpgradeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		TrackedUpgrade.CODEC.optionalFieldOf("tracked").forGetter(CabinUpgradeState::tracked),
		Installation.CODEC.optionalFieldOf("installation").forGetter(CabinUpgradeState::installation)
	).apply(instance, CabinUpgradeState::new));

	CabinUpgradeState {
		Objects.requireNonNull(tracked, "tracked");
		Objects.requireNonNull(installation, "installation");
		if (installation.isPresent()
			&& (tracked.isEmpty() || !installation.get().target().equals(tracked.get().target()))) {
			throw new IllegalArgumentException("An installation must target the tracked upgrade");
		}
	}

	CabinUpgradeState withTracked(TrackedUpgrade value) {
		return new CabinUpgradeState(Optional.of(value), Optional.empty());
	}

	CabinUpgradeState withInstallation(Installation value) {
		return new CabinUpgradeState(tracked, Optional.of(value));
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
				throw new IllegalArgumentException("Unsupported schema 4 upgrade target type " + type);
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
			try {
				return Integer.parseInt(key);
			} catch (NumberFormatException exception) {
				throw new IllegalStateException("Invalid general-space target key " + key, exception);
			}
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

	record TrackedUpgrade(Target target, List<Requirement> requirements, List<ItemStack> fund) {
		static final Codec<TrackedUpgrade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Target.CODEC.fieldOf("target").forGetter(TrackedUpgrade::target),
			Requirement.CODEC.listOf().fieldOf("requirements").forGetter(TrackedUpgrade::requirements),
			ItemStack.CODEC.listOf().optionalFieldOf("fund", List.of()).forGetter(TrackedUpgrade::fund)
		).apply(instance, TrackedUpgrade::new));

		TrackedUpgrade {
			Objects.requireNonNull(target, "target");
			Objects.requireNonNull(requirements, "requirements");
			Objects.requireNonNull(fund, "fund");
			requirements = List.copyOf(requirements);
			fund = copyStacks(fund);
			if (requirements.isEmpty()) {
				throw new IllegalArgumentException("A tracked upgrade must have requirements");
			}

			Set<Identifier> uniqueItems = new HashSet<>();
			Map<Identifier, Integer> limits = new HashMap<>();
			for (Requirement requirement : requirements) {
				if (!uniqueItems.add(requirement.itemId())) {
					throw new IllegalArgumentException("Tracked requirements must be consolidated by item");
				}
				limits.put(requirement.itemId(), requirement.count());
			}

			Map<Identifier, Integer> totals = new HashMap<>();
			for (ItemStack stack : fund) {
				if (stack.isEmpty()) {
					throw new IllegalArgumentException("Upgrade funds cannot contain empty stacks");
				}
				Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
				Integer limit = limits.get(itemId);
				if (limit == null) {
					throw new IllegalArgumentException("Fund contains an item outside the tracked requirements");
				}
				int total = Math.addExact(totals.getOrDefault(itemId, 0), stack.getCount());
				if (total > limit) {
					throw new IllegalArgumentException("Fund exceeds the tracked requirement for " + itemId);
				}
				totals.put(itemId, total);
			}
		}

		@Override
		public List<ItemStack> fund() {
			return copyStacks(fund);
		}

		int fundedCount(Identifier itemId) {
			return fund.stream()
				.filter(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId))
				.mapToInt(ItemStack::getCount)
				.sum();
		}

		boolean isComplete() {
			return requirements.stream().allMatch(value -> fundedCount(value.itemId()) == value.count());
		}

		TrackedUpgrade withFund(List<ItemStack> stacks) {
			return new TrackedUpgrade(target, requirements, stacks);
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

	private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
		List<ItemStack> copies = new ArrayList<>(stacks.size());
		for (ItemStack stack : stacks) {
			copies.add(stack.copy());
		}
		return List.copyOf(copies);
	}
}
