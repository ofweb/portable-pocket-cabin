# Milestone 7: Targeted production and room automation

**Depends on:** Milestones 4 and 5.

**Outcome:** Exploration-discovered books unlock small, individually controlled automations that fulfil concrete jobs without turning the cabin into an unbounded factory.

Major scope:

- safe learned item, prepared-meal and potion templates
- installable automation books and per-cabin enablement
- bounded job planning, recursion, cycle rejection, transactional inputs and exact blocking reasons
- owner reserves, committed inputs and output/by-product capacity checks
- crafting, cooking and brewing jobs
- Farmer's Delight and declared delight integration profiles
- automatic greenhouse harvesting/replanting
- stable/livestock feeding, collection and surplus processing
- forestry felling/replanting, later kiln processing and late coal synthesis
- storage, automation and job-status interfaces

**Red:** Add failing unsafe-template, cyclic-recipe, reserve, full-output, missing-input, duplicate-result, pause/resume and integration-absence tests.

**Green:** Implement one bounded job type at a time and add each room action independently.

**Refactor:** Share planning and transaction primitives without treating arbitrary blocks, entities or mod recipes as trusted automation definitions.

**Exit gate:** Every automation starts from an explicit request or enabled bounded action, stops cleanly at limits and explains why it cannot continue.


## Room-automation boundary

_Source: manual-operation and targeted-automation boundary from the former progression specification._

### Manual operation and targeted automation

Every functional room provides useful manual behavior before automation is discovered.

Automation books unlock small actions, not an all-or-nothing automated room. Examples include feeding, collecting, harvesting, replanting, felling and processing. Each action is installed knowledge, individually enabled per cabin by its owner, and subject to the bounded-job rules.

When a room is packed without an action being automated:

- passive growth or maturation may advance through catch-up
- preloaded local inputs may be consumed where the room explicitly supports that behavior
- outputs may accumulate only up to the fixture's small local capacity
- an action that requires a player, such as harvesting or replanting, does not happen by itself


## Production and automation specification

_Source: learning, automation-book, bounded-job, reserve, crafting, cooking and brewing sections of the former storage specification._

### Learned item templates

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

### Automation books

Automation abilities are discovered as distinctive enchanted treasure books while exploring. They are cabin items, not ordinary enchantments.

- Storing a book does not install it.
- Installation is a deliberate action at a cabin interface.
- Each book teaches one small, named capability.
- Duplicate discoveries do not stack into additional power.
- Capabilities remain individually controllable per cabin.
- Loot placement and rarity are data-driven so modpack structures may participate.

Examples include automatic feeding, collection, slaughtering, harvesting, replanting, felling, cooking, brewing, mailbox fulfilment and surplus delivery. A single generic "automate room" discovery does not exist.

Whether installation consumes the physical discovery book is a balancing decision to settle before implementing loot tables; it does not change permanent knowledge semantics.

### Bounded automation jobs

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

### Reserves and execution priority

Owners may define hard item reserves. Automated jobs and storage sharing cannot consume below those reserves. Manual actions by an owner or resident remain possible; reserves constrain cabin-controlled behavior, not people.

Loadout-restock commitments are evaluated before items become shareable. A rule such as "share wood only after five complete loadout restocks" computes the full material requirement of five current owner manifests, including craftable intermediates, before exposing surplus.

In-progress jobs reserve their already committed inputs transactionally. On failure or cancellation, unused inputs return to the same cabin storage.

### Crafting automation

Crafting automation produces locally known safe item templates from stored materials through bounded jobs.

For a specific cabin upgrade, an owner may explicitly approve a displayed crafting plan for missing
ingredients. The plan uses raw materials from central storage, obeys the same recipe-safety,
recursion, reserve and capacity rules as other jobs, and commits outputs only to that target's capped
fund. Existing fund materials are applied first and installation remains a separate owner action
under [Milestone 3](03-upgrade-interface.md).

- It may prepare known intermediate components recursively.
- It cannot reproduce custom names, arbitrary data, durability or enchantments as part of ordinary crafting.
- Container-return items and recipe by-products must be accounted for before starting.
- A job does not start unless all outputs and by-products have a valid destination.

### Cooking automation

A prepared food must be cooked, found or otherwise obtained and then enter that cabin's central storage before the cabin knows the meal.

The cabin must also have a supported cooking recipe and the relevant targeted automation. Farmer's Delight and optional delight integrations provide explicit process profiles rather than being inferred from arbitrary block behavior.

Cooking automation never implies farming, animal feeding, slaughtering or ingredient acquisition. Those are separate manual actions or automation capabilities.

### Brewing automation

A potion must first be brewed or found and enter that cabin's central storage. The learned template includes only allowlisted potion identity and effect components.

Reproduction requires a declared brewing path, ingredients, bottles and the installed brewing capability. Custom or modded potions require an explicit compatibility profile when their data cannot be handled safely.

### Optional cooking integrations

An explicit Alex's Mobs Continued Delight integration may expose declared ingredients and meals to kitchen learning and automation through compatible Farmer's Delight or Farmer's Delight Refabricated recipes.

Unknown food remains storable as an ordinary item. It becomes reproducible only when its recipe and item components pass the safe-learning rules in this milestone.
