# Milestone 8: Enchanting, equipment requisitions and owner loadouts

**Depends on:** Milestone 7's knowledge, job and storage systems.

**Outcome:** The cabin can learn enchantments destructively, reproduce valid equipment using materials instead of experience and maintain explicit Factorio-style owner loadouts.

Major scope:

- capacity-limited enchantment library with one selected extraction per destroyed source item
- level improvement, pending shared discoveries and vanilla/modded applicability rules
- amethyst-and-lapis enchanting costs with obsidian as installed infrastructure rather than fuel
- exact equipment requisitions with full cost preview
- named owner loadout groups, effective min/max rules and optional automated production
- protected slots, durability/enchantment requirements and the advanced deposit-unlisted control
- Take for this trip, Take back and entry transaction summaries

**Red:** Add failing destructive-extraction, capacity, incompatibility, cost, protected-item, competing-group and temporary-exception tests.

**Green:** Build manual enchantment learning and requisition first, then add entry-triggered loadout exchange.

**Refactor:** Use canonical safe templates and the bounded-job engine for both requisitions and restocking.

**Exit gate:** Loadouts never alter unspecified or unsafe items, enchanting never consumes experience, and every planned material cost is visible before commitment.


## Consolidated specification

_Source: enchantment, equipment-requisition, loadout and provisional-interface sections of the former storage specification._

### Enchantment library

Placing enchanted equipment or an enchanted book in ordinary or central storage does not teach its enchantments. Learning is a deliberate destructive action at the enchantment-library interface.

For each extraction:

1. The player supplies one enchanted source item, such as equipment or an enchanted book, and one blank book.
2. The interface shows the enchantments that can be learned or improved.
3. The player selects exactly one enchantment.
4. Validation checks library capacity and whether the selected level is useful.
5. A successful extraction destroys the enchanted source item and consumes the blank book.
6. Only the selected enchantment is learned; every other enchantment on the item is lost.

The operation rejects before consuming anything when the cabin already knows the same or a higher level.

Each enchantment family occupies one library slot regardless of level. Learning level III permits requisitioning levels I through III. Extracting a higher level improves the existing entry without consuming another slot. Library upgrades add slots.

Shared enchantment discoveries that exceed a receiving cabin's local capacity enter a permanent pending-discoveries list. After expanding the library, the owner chooses which pending discoveries to install. Learned entries cannot be forgotten.

Vanilla applicability and incompatibility rules remain authoritative. Modded enchantments require an explicit profile. Curses may be learned but are never applied unless explicitly requested.

#### Enchanting costs

Cabin enchanting is material-only and never consumes player experience.

- Amethyst is the primary recurring resource: its resonance performs the pattern work that replaces experience levels.
- Lapis remains a secondary enchanting reagent.
- Obsidian is used to install and strengthen the library's dimensional anchor, but is not consumed as routine enchanting fuel.
- Nether and End materials gate the highest tiers.
- Cost scales with enchantment rarity, requested level, number of enchantments already applied and equipment tier.
- Treasure enchantments still require prior extraction; materials alone cannot discover them.

Exact formulas and material quantities are data-driven balancing values.

### Equipment requisition

The cabin owner may request an exact equipment template containing:

- a locally known base item
- a supported material or item variant
- specific learned enchantments and levels
- a requested quantity

The cabin validates the complete recipe and enchanting plan before accepting the job. It reports unknown items, missing enchantments, incompatible combinations, unavailable automation and insufficient materials separately.

Residents may use shared crafting and enchanting facilities manually but do not gain the owner's personal requisition rules, automatic restocking or saved loadouts from that cabin.

### Owner loadouts

Loadouts are personal to the cabin owner. They run when the owner enters their own cabin, including through its hallway door while the exterior is packed.

Each rule contains:

- a safe item template
- minimum desired quantity
- maximum retained quantity
- preferred inventory slots, when relevant
- whether automated production is permitted
- optional durability or enchantment requirements

Behavior follows Factorio-style personal logistics:

- below the minimum, withdraw or produce enough to reach it
- above the maximum, deposit the excess
- minimum `0` and maximum `0` means deposit all matching items
- no rule means leave that item alone
- equal minimum and maximum maintains an exact quantity

Named groups such as `Everyday`, `Mining`, `Building` and `Nether` may be enabled independently. When active groups mention the same item, the highest requested minimum and maximum win. An item absent from a group does not impose a limit.

Depositing every unlisted item is an explicit advanced toggle and is off by default. Equipped items, protected slots, named items, container items and unsupported custom-data items remain untouched unless an exact rule includes them. Damaged equipment is not replaced or deposited without a specific durability rule.

Loadout processing uses existing stored items before creating bounded production jobs. Unresolved entries remain unchanged and produce status messages.

#### Temporary exceptions

The storage interface provides **Take for this trip**:

- it requests an item once without modifying saved loadouts
- it overrides automatic deposit rules for that item
- it remains active until the owner leaves and later returns to their cabin, or clears it manually
- it does not count toward multi-restock sharing reserves

The entry exchange summary lists deposited and restored items. **Take back** creates the same temporary exception for an automatically deposited item.

### Provisional storage and loadout interface

The protected cabin terminal provides separate pages for storage, loadouts, automation and status. The exact art, dimensions and widgets remain an implementation design task, but the interaction contract is fixed.

The loadout page provides:

- named group selectors with independent enabled states
- searchable ghost item slots
- minimum and maximum controls on one item row
- a clear `deposit all` state for a zero maximum
- visible warnings for contradictory or unsupported rules
- an advanced `deposit unlisted` control with a persistent visible warning while enabled
- a preview of the effective combined loadout

The normal storage page exposes **Take for this trip** without requiring the owner to edit a saved group. The latest entry transaction remains available as a concise summary with **Take back** actions.
