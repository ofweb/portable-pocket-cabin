# Cabin Storage and Automation

## Status and relationship to the MVP

This document specifies the post-MVP cabin-owned storage, knowledge and automation track.

It complements ordinary Minecraft inventories rather than replacing them. Chests, Tom's Simple Storage and other compatible blocks continue to behave normally inside active cabin rooms under the rules in [`SPEC.md`](../../SPEC.md).

Functional-room production is specified in [Cabin Progression and Functional Rooms](cabin-progression-and-rooms.md). Roles, mailboxes and cabin connections are specified in [Cabin Network and Access](cabin-network-and-access.md).

## Central cabin storage

Each cabin UUID owns one authoritative persistent virtual inventory.

- Capacity is measured in Minecraft-style slots.
- Every item obeys its normal maximum stack size.
- Non-stackable and unique stacks occupy one slot each.
- Capacity increases only through explicit storage upgrades.
- Packing, deployment, exterior loss and hallway membership never transfer ownership of this inventory.
- Storage mutations are journalled or otherwise atomic so interrupted automation and network transfers cannot duplicate or delete items.

Players deposit and retrieve items through protected cabin interfaces. Ordinary placed inventories are independent and are not scanned, merged or consumed automatically. Moving items between ordinary storage and cabin storage is always an explicit player action or a separately configured integration.

Greenhouses, kitchens, crafting systems, brewing, enchanting and other cabin automation consume from and deposit into this central inventory through server-side transactions.

## Storage access

The role matrix in the network specification applies.

- Owners and residents may deposit, withdraw and consume resources through shared facilities.
- Guests cannot browse, deposit into or withdraw from main storage.
- Guest exchange uses the mailbox rather than main storage.
- Only the owner may change capacity, automation, sharing, reserve and loadout configuration.

## Learned item templates

An item entering central storage for the first time teaches that cabin a safe canonical product template. Merely unlocking or viewing a recipe is insufficient.

Learning records only approved item identity and variant components. It never copies:

- container contents
- arbitrary mod data
- custom names or lore
- current durability
- enchantments
- unsafe nested item data

Unsupported variants remain storable but report that they cannot be learned. Compatibility profiles may allow additional components only when their preservation and reproduction semantics are explicit.

An item being known is a discovery gate, not permission to create it freely. Production also requires:

- a valid supported recipe or process
- the relevant installed automation ability
- every required input
- sufficient destination capacity
- satisfaction of tier and resource costs

Enchantments, meals and potions add their own learning rules below.

## Scope of shared knowledge

Knowledge is divided deliberately:

- **Shared discoveries:** installed automation books and learned enchantments copy permanently to cabins connected through the same hallway network.
- **Local product knowledge:** ordinary item templates, prepared meals and potion variants remain specific to the cabin whose storage received them.

Connecting temporarily is allowed to spread shared discoveries throughout a small friend group. A cabin retains copied discoveries after leaving the network. This is intended cooperative progression.

Every cabin owner independently enables or disables each usable automation. Knowing an ability never silently activates it.

## Automation books

Automation abilities are discovered as distinctive enchanted treasure books while exploring. They are cabin items, not ordinary enchantments.

- Storing a book does not install it.
- Installation is a deliberate action at a cabin interface.
- Each book teaches one small, named capability.
- Duplicate discoveries do not stack into additional power.
- Capabilities remain individually controllable per cabin.
- Loot placement and rarity are data-driven so modpack structures may participate.

Examples include automatic feeding, collection, slaughtering, harvesting, replanting, felling, cooking, brewing, mailbox fulfilment and surplus delivery. A single generic "automate room" discovery does not exist.

Whether installation consumes the physical discovery book is a balancing decision to settle before implementing loot tables; it does not change permanent knowledge semantics.

## Bounded automation jobs

Automation runs only in response to a concrete job:

- an explicit production or equipment request
- an owner loadout restock
- an owner-configured stock target
- one enabled functional-room action
- one enabled mailbox request or delivery rule

There is no opportunistic "craft everything known" mode.

A job may recursively produce known intermediate ingredients, subject to configured maximum depth and operation counts. Cyclic and net-positive recipe paths are rejected. The plan selects only declared supported recipes and does not silently substitute valuable variants.

Explicit equipment requisitions show their complete projected material cost before confirmation.

Jobs pause without losing their request when blocked by:

- missing ingredients
- unknown product or intermediate template
- unsupported or ambiguous recipe
- unavailable or disabled automation
- protected reserve
- insufficient storage or room output space
- unmet progression tier

Every paused job exposes the exact blocking reason through local status.

## Reserves and execution priority

Owners may define hard item reserves. Automated jobs and storage sharing cannot consume below those reserves. Manual actions by an owner or resident remain possible; reserves constrain cabin-controlled behavior, not people.

Loadout-restock commitments are evaluated before items become shareable. A rule such as "share wood only after five complete loadout restocks" computes the full material requirement of five current owner manifests, including craftable intermediates, before exposing surplus.

In-progress jobs reserve their already committed inputs transactionally. On failure or cancellation, unused inputs return to the same cabin storage.

## Crafting automation

Crafting automation produces locally known safe item templates from stored materials through bounded jobs.

- It may prepare known intermediate components recursively.
- It cannot reproduce custom names, arbitrary data, durability or enchantments as part of ordinary crafting.
- Container-return items and recipe by-products must be accounted for before starting.
- A job does not start unless all outputs and by-products have a valid destination.

## Cooking automation

A prepared food must be cooked, found or otherwise obtained and then enter that cabin's central storage before the cabin knows the meal.

The cabin must also have a supported cooking recipe and the relevant targeted automation. Farmer's Delight and optional delight integrations provide explicit process profiles rather than being inferred from arbitrary block behavior.

Cooking automation never implies farming, animal feeding, slaughtering or ingredient acquisition. Those are separate manual actions or automation capabilities.

## Brewing automation

A potion must first be brewed or found and enter that cabin's central storage. The learned template includes only allowlisted potion identity and effect components.

Reproduction requires a declared brewing path, ingredients, bottles and the installed brewing capability. Custom or modded potions require an explicit compatibility profile when their data cannot be handled safely.

## Enchantment library

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

### Enchanting costs

Cabin enchanting is material-only and never consumes player experience.

- Lapis and amethyst are the primary recurring resources.
- Obsidian appears in stronger mid-game library and enchantment tiers.
- Nether and End materials gate the highest tiers.
- Cost scales with enchantment rarity, requested level, number of enchantments already applied and equipment tier.
- Treasure enchantments still require prior extraction; materials alone cannot discover them.

Exact formulas and material quantities are data-driven balancing values.

## Equipment requisition

The cabin owner may request an exact equipment template containing:

- a locally known base item
- a supported material or item variant
- specific learned enchantments and levels
- a requested quantity

The cabin validates the complete recipe and enchanting plan before accepting the job. It reports unknown items, missing enchantments, incompatible combinations, unavailable automation and insufficient materials separately.

Residents may use shared crafting and enchanting facilities manually but do not gain the owner's personal requisition rules, automatic restocking or saved loadouts from that cabin.

## Owner loadouts

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

### Temporary exceptions

The storage interface provides **Take for this trip**:

- it requests an item once without modifying saved loadouts
- it overrides automatic deposit rules for that item
- it remains active until the owner leaves and later returns to their cabin, or clears it manually
- it does not count toward multi-restock sharing reserves

The entry exchange summary lists deposited and restored items. **Take back** creates the same temporary exception for an automatically deposited item.

## Provisional storage and loadout interface

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

## Cabin-owned storage sharing

Every cabin retains ownership of its storage. There is no combined network inventory.

An owner may expose surplus through rules such as:

- share this item above a fixed quantity
- retain enough inputs for a configured number of complete owner loadout restocks
- transfer a configured amount on request

Remote automation never consumes directly from donor storage. Instead:

1. A recipient creates a transfer request.
2. Each donor independently evaluates its current sharing and reserve rules.
3. An eligible amount is atomically removed from the donor.
4. The same amount is atomically deposited into the recipient cabin or its configured mailbox destination.
5. Failure rolls the transaction back without partial movement.

Every completed transfer is attributable and visible to both cabins. A recipient job consumes the items only after they have become local storage.

## Status and inspection

Status is always grouped by cabin and job. The system never merges every connected cabin's shortages into one mandatory list.

- Owners see full storage, automation, loadout, reserve, sharing, mailbox, room and network diagnostics.
- Residents see actionable status for facilities they may use, without private owner loadout or sharing configuration.
- Guests may see plain-language cabin warnings and shortages but cannot browse the storage catalogue, exact quantities, private loadouts or mailbox contents.
- Remote inspection exposes no more detail than the inspecting player could see for that destination cabin locally.
- Owners may publish exact requests or surplus quantities to the hallway network.

Failures should name the cabin, job and concrete reason, for example:

```text
Sune's Cabin / Dinner restock: missing mutton
Adam's Cabin / Diamond pickaxe: enchantment Efficiency V is unknown
Alex's Cabin / Greenhouse collection: central storage has no free slot
Sam's Cabin / Shared wood request: retained for five owner loadout restocks
```

## Balancing decisions intentionally left data-driven

- storage slots per upgrade tier
- automation-book loot sources and rarity
- job depth and operation limits
- hard-reserve defaults
- enchantment material-cost formulas
- library slots per tier
- supported crafting, cooking, brewing and enchanting integrations
- whether installing an automation book consumes the physical book
