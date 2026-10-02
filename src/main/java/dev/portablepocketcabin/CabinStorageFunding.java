package dev.portablepocketcabin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

final class CabinStorageFunding {
	private CabinStorageFunding() { }
	static CabinUpgradeService.Outcome fill(CabinRegistry registry, UUID cabinId, UUID actor,
		CabinUpgradeState.Target target, List<CabinUpgradeState.Requirement> expected,
		WorldAttunement attunement, CabinUpgradeDefinitions.Definitions definitions) {
		synchronized (registry) {
			CabinRecord cabin = registry.find(cabinId).orElse(null);
			if (!CabinStorage.mayUse(cabin, actor) || !cabin.owner().equals(actor))
				return CabinUpgradeService.Outcome.failure("Only the owner may fill from storage.");
			if (cabin.upgrades().operationInProgress())
				return CabinUpgradeService.Outcome.failure("A cabin upgrade operation is in progress.");
			CabinUpgradeCatalog.Offer offer = CabinUpgradeCatalog.offer(cabin, target, attunement, definitions).orElse(null);
			if (offer == null || offer.complete() || offer.locked() || !offer.requirements().equals(expected))
				return CabinUpgradeService.Outcome.failure("The selected upgrade or requirements changed.");
			CabinUpgradeState.Fund existing = cabin.upgrades().fund(target).orElse(null);
			if (existing != null && !existing.requirements().equals(expected))
				return CabinUpgradeService.Outcome.failure("The funded requirements changed.");
			List<ItemStack> stored = new ArrayList<>(cabin.upgrades().storage().stacks());
			List<ItemStack> funded = new ArrayList<>(existing == null ? List.of() : existing.stacks());
			int moved = 0;
			for (var requirement : expected) {
				int missing = requirement.count() - (existing == null ? 0 : existing.fundedCount(requirement.itemId()));
				for (ItemStack stack : stored) {
					if (missing <= 0) break;
					if (stack.isEmpty() || !stack.getComponentsPatch().isEmpty()
						|| !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(requirement.itemId())) continue;
					int count = Math.min(missing, stack.getCount());
					funded.add(stack.copyWithCount(count));
					stack.shrink(count);
					missing -= count;
					moved += count;
				}
			}
			if (moved == 0) return CabinUpgradeService.Outcome.failure("Storage has no eligible missing materials.");
			stored.removeIf(ItemStack::isEmpty);
			CabinStorageState storage = cabin.upgrades().storage();
			registry.updateUpgradeState(cabinId, cabin.upgrades().withFund(new CabinUpgradeState.Fund(target, expected, funded))
				.withStorage(storage.withStacks(stored)));
			return CabinUpgradeService.Outcome.success("Moved " + moved + " materials from storage.");
		}
	}
}
