package dev.portablepocketcabin;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/** Resolves ephemeral category and panel selection from a changing server-side catalog. */
final class CabinUpgradeSelection {
	record Selected(
		int groupIndex,
		int panelIndex,
		CabinUpgradeCatalog.Group group,
		CabinUpgradeCatalog.Offer offer
	) {
	}

	private CabinUpgradeSelection() {
	}

	static Optional<Selected> resolve(
		List<CabinUpgradeCatalog.Group> groups,
		Identifier preferredGroup,
		CabinUpgradeState.Target preferredTarget
	) {
		for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
			CabinUpgradeCatalog.Group group = groups.get(groupIndex);
			for (int panelIndex = 0; panelIndex < group.panels().size(); panelIndex++) {
				CabinUpgradeCatalog.Offer offer = group.panels().get(panelIndex);
				if (preferredTarget != null && offer.target().equals(preferredTarget)) {
					return Optional.of(new Selected(groupIndex, panelIndex, group, offer));
				}
			}
		}
		if (preferredGroup != null) {
			for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
				if (groups.get(groupIndex).id().equals(preferredGroup)) {
					return selectGroup(groups, groupIndex);
				}
			}
		}
		return selectGroup(groups, 0);
	}

	static Optional<Selected> selectGroup(List<CabinUpgradeCatalog.Group> groups, int groupIndex) {
		if (groupIndex < 0 || groupIndex >= groups.size()) {
			return Optional.empty();
		}
		CabinUpgradeCatalog.Group group = groups.get(groupIndex);
		return Optional.of(new Selected(groupIndex, 0, group, group.panels().getFirst()));
	}

	static Optional<Selected> cyclePanel(
		List<CabinUpgradeCatalog.Group> groups, Selected current, int direction
	) {
		if (current == null || direction == 0
			|| current.groupIndex() < 0 || current.groupIndex() >= groups.size()) {
			return Optional.empty();
		}
		CabinUpgradeCatalog.Group group = groups.get(current.groupIndex());
		if (!group.id().equals(current.group().id()) || group.panels().isEmpty()) {
			return Optional.empty();
		}
		int panelIndex = Math.floorMod(current.panelIndex() + Integer.signum(direction), group.panels().size());
		return Optional.of(new Selected(
			current.groupIndex(), panelIndex, group, group.panels().get(panelIndex)
		));
	}

	static boolean isPresentable(CabinUpgradeCatalog.Offer offer, int maximumRequirements) {
		return offer != null && maximumRequirements > 0
			&& offer.requirements().size() <= maximumRequirements;
	}
}
