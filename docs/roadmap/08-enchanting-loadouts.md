# Milestone 8: Enchanting, equipment requisitions and owner loadouts

**Depends on:** [Milestone 7](07-production-automation.md) knowledge, timed jobs and storage transactions.

**Outcome:** Cabins learn enchantments destructively, reproduce valid equipment with materials instead of experience and maintain explicit owner loadouts.

**Status:** Draft. Each delivery requires alignment before implementation.

[Milestone 9](09-connected-cabins.md) extends loadout triggers to hallway entry. [Milestone 10](10-cooperative-logistics.md) owns discovery propagation and multi-restock sharing reserves.

## Enchantment library

Placing an enchanted item in storage teaches nothing. An owner or resident learns one enchantment through a deliberate extraction:

1. Supply one enchanted source item and one blank book.
2. Select one useful enchantment and level.
3. Validate the source, library state and selected level.
4. Destroy the source and consume the blank book.
5. Add or improve only the selected enchantment; all others on the source are lost.

Validation failure consumes nothing. Extracting a known enchantment at the same or a lower level is rejected.

### Known and active enchantments

The **known library** is permanent and uncapped. Each enchantment family has one entry at its highest learned level; learning level III permits levels I–III. Known enchantments cannot be forgotten.

The library has a capacity-limited set of **active slots**:

- only active enchantments may be applied or advance requisition jobs
- upgrades add active slots, not knowledge capacity
- the owner may activate or deactivate any known enchantment
- deactivation pauses dependent jobs without losing materials, work or plans; reactivation resumes them
- shared discoveries become known but never activate automatically

The Enchanting section of **Cabin Upgrades** shows the known catalogue and active slots. Selection is server-authoritative and changing it never consumes the learned enchantment.

Vanilla applicability and incompatibility rules remain authoritative. Modded enchantments require explicit profiles. Curses may be learned but apply only when explicitly requested.

## Enchanting costs

Cabin enchanting consumes materials and timed work, never player experience. Amethyst provides recurring resonance, lapis is a secondary reagent, and obsidian installs or strengthens library infrastructure rather than serving as routine fuel. Nether and End materials gate the highest tiers.

Cost scales with rarity, level, existing enchantments and equipment tier. Treasure enchantments require prior learning. Exact formulas, quantities and work durations are data-driven.

## Equipment requisitions

Only the owner may request a quantity of a locally known base-item variant with selected active enchantments and levels.

Before commitment, the interface shows the complete crafting, enchanting, material and time plan. Validation reports unknown items, inactive or missing enchantments, incompatible combinations, unavailable capabilities, protected reserves and insufficient materials separately.

Requisitions use Milestone 7's selected recipes, reservations, timed work and atomic output commit. Deactivating a required enchantment pauses the job. Residents may contribute enchantments and use active enchantments manually but cannot save requisitions or change active slots.

## Owner loadouts

Loadouts belong only to the cabin owner and reconcile when that owner enters the cabin. Each rule contains:

- a safe item template
- minimum and maximum retained quantities
- preferred inventory slots when needed
- whether automated production is permitted
- optional durability or enchantment requirements

Rules behave as follows:

- below the minimum, withdraw or produce the deficit
- above the maximum, deposit the excess
- minimum `0` and maximum `0` deposits every matching item
- equal minimum and maximum maintains that quantity
- no rule leaves the item unchanged

Named groups such as `Everyday`, `Mining`, `Building` and `Nether` may be enabled independently. When active groups mention one item, the highest minimum and highest maximum form its effective rule.

`Deposit unlisted` is an advanced option that remains off by default and visibly warns while enabled. Equipped items, protected slots, named items, container items and unsupported custom-data items remain untouched unless an exact rule includes them. Damaged equipment moves only under a matching durability rule.

### Entry processing

Each effective item rule is one atomic transfer:

- a blocked rule leaves that item unchanged and reports why
- independent rules may still succeed
- existing central-storage items satisfy minimums before production begins
- storage overflow leaves excess items with the player
- completed transfers appear in the entry summary

Restocking is idempotent. Each owner and item template has at most one outstanding restock request. Reconciliation counts matching player inventory, central-storage stock and committed loadout output, then updates the remaining deficit instead of queuing another job.

### Temporary exceptions

**Take for this trip** requests an item once without changing saved groups. It overrides automatic deposit for that template until the owner leaves and later returns or clears the exception manually. It does not count toward future multi-restock sharing reserves.

**Take back** attempts to reverse one available transfer from the latest entry summary and creates the same temporary exception. It commits only if the deposited item remains available and the player inventory can accept it; failure moves nothing and explains why.

The protected storage interface provides loadout groups, searchable ghost slots, minimum and maximum controls, a distinct deposit-all state, effective-rule previews, warnings, temporary actions and the latest entry summary. Exact art and dimensions remain a delivery-level decision.

The cabin registry persists known and active enchantments, loadout groups, protected settings, temporary exceptions, entry summaries and coalesced restock requests. Enchantment selection, extraction, entry transfers and reversals use server-authoritative transactions and survive restart, packing and redeployment.

## Evergreen acceptance contract

Milestone 8 remains accepted only while automated tests and targeted manual checks establish that:

1. Extraction consumes one selected source and blank book only after successful validation.
2. Higher levels improve permanent known entries; lower or equal levels consume nothing.
3. Only the owner changes active slots, while owners and residents may extract and use active enchantments.
4. Shared discoveries never activate automatically, and inactive knowledge is never lost.
5. Deactivation pauses dependent jobs with their plan, work and materials intact.
6. Requisitions reject invalid combinations and show complete material, recipe and time costs before commitment.
7. Effective loadout rules merge deterministically and never alter unspecified or protected items.
8. Each item rule transfers atomically; blocked rules do not prevent independent safe transfers.
9. Repeated entry coalesces restocking and counts committed output without duplicating jobs.
10. Temporary exceptions and Take back survive races without duplicating or replacing unavailable items.

Codec, extraction, applicability, transaction, loadout, scheduler, GameTest and dedicated-server restart suites are the automated gates. Manual acceptance covers the known/active interface, effective loadout preview, warnings and entry summary.

## Out of scope

- automatic discovery from stored enchanted items
- player-experience costs
- resident or guest loadouts and saved requisitions
- automatic activation of shared enchantments
- inter-cabin discovery propagation or resource sharing
- arbitrary modded-enchantment inference
- exact costs, active-slot counts and final interface art before delivery alignment
