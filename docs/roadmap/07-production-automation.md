# Milestone 7: Targeted production and room automation

**Depends on:** [Milestone 4](04-household-storage.md) storage and permissions; [Milestone 5](05-functional-rooms.md) managed rooms.

**Outcome:** Exploration-discovered books unlock small, individually controlled automations that fulfil concrete jobs without turning the cabin into an unbounded factory.

**Status:** Draft. Each delivery requires alignment before implementation.

## Scope

This milestone adds:

- safe, permanent item, prepared-meal and potion templates
- consumable automation books and per-cabin capabilities
- bounded recursive job planning with explicit recipe selection
- persisted timed work, hard reserves and transactional inputs and outputs
- local crafting, cooking and brewing
- owner-configured local stock targets
- automatic room actions delivered separately from manual room behavior
- explicit Farmer's Delight and other optional process profiles

[Milestone 8](08-enchanting-loadouts.md) owns equipment requisitions and loadout restocking. [Milestone 10](10-cooperative-logistics.md) owns mailbox fulfilment, surplus delivery, storage sharing and remote jobs. This milestone exposes job and reserve primitives to them without defining their triggers.

## Delivery sequence

| Delivery | Result |
| --- | --- |
| 7.1 | Learn safe product templates and consume one discovery book to install one capability. |
| 7.2 | Plan, persist and execute bounded crafting jobs with reserves and timed work. |
| 7.3 | Add profiled cooking and brewing jobs. |
| 7.4 | Add each greenhouse, livestock and forestry action independently. |
| 7.5 | Add optional cooking profiles, kiln processing and late coal synthesis. |

Each delivery must remain useful without the later triggers in Milestones 8 and 10.

## Product knowledge

The first eligible item stack entering central storage teaches that cabin a safe canonical product template. Removing every matching item does not erase the template. Recipe discovery or viewing alone teaches nothing.

A template records only approved identity and variant components. It never copies:

- container contents
- arbitrary mod data
- custom names or lore
- current durability
- enchantments
- unsafe nested item data

Unsupported variants remain storable but report why they cannot be learned. Compatibility profiles may allow components only when their preservation and reproduction semantics are explicit.

Knowing a product does not create it. Production also requires a supported recipe or process, the installed capability, inputs, destination capacity, sufficient work time and every tier or resource prerequisite.

Prepared meals and potions use the same local-knowledge rule with their specialized profiles below. Ordinary product, meal and potion knowledge never propagates automatically to another cabin.

## Automation books

Automation capabilities are discovered as distinctive enchanted treasure books. They are cabin discoveries, not ordinary enchantments.

- storing a book does not install it
- installation is an owner action at a protected cabin interface
- each book installs one named capability
- successful installation consumes the physical book
- failed validation and duplicate knowledge leave the book untouched
- duplicate capabilities provide no additional power
- installed knowledge persists with the cabin and remains individually enabled or disabled
- loot placement and rarity are data-driven

Capabilities remain narrow: feeding, collection, harvesting, replanting, felling, cooking and brewing are separate discoveries. No generic “automate room” capability exists.

## Permissions

- Only the owner may install capabilities, enable room actions, configure stock targets or reserves, and approve a plan that funds an upgrade.
- Owners and residents may submit explicit local crafting, cooking and brewing requests through enabled facilities.
- Every resident job obeys owner reserves, installed capabilities and the same planning limits as an owner job.
- Guests cannot submit jobs, configure automation or consume central-storage resources.

Permission loss invalidates an uncommitted request. A committed job retains its reserved resources but pauses before further owner-restricted configuration or delivery decisions.

## Bounded job planning

Automation starts only from:

- an explicit local production request
- an owner-configured local stock target
- one enabled functional-room action
- an owner-approved cabin-upgrade funding plan

There is no opportunistic “craft everything known” mode.

A plan may recursively produce known intermediates within configured depth, operation and output limits. It rejects cyclic and net-positive paths, including cycles hidden through container returns or by-products.

When several supported recipe paths can produce the result, the interface shows their complete plans. The requesting owner or resident selects one before a local job starts. Inventory order never selects a recipe. Unattended stock targets and room actions use an owner-selected recipe or explicit process profile and pause if the choice becomes ambiguous.

Every plan reports:

- exact inputs and intermediates
- outputs, container returns and by-products
- reserved quantities and protected reserves
- total work and expected duration at the installed tier
- unsupported, unknown or ambiguous steps
- destination-capacity requirements

## Timed execution

Automation is persisted work, not instant crafting. Each crafting operation costs:

```text
base recipe work + consumed ingredient units
```

Recursive plans sum every intermediate operation. Multiple recipe batches multiply that work. Installed automation tier determines work completed per game tick. Base work and throughput are data-driven balancing values.

Cooking, brewing, kiln and room processes use explicit profile durations instead of the crafting formula.

Before work begins, the job reserves every required input and enough destination capacity for outputs, returned containers and by-products. Reserved inputs are unavailable to other jobs and players. A successful job commits all outputs atomically. Cancellation or unrecoverable failure returns unused inputs and releases capacity to the same cabin storage.

Jobs persist their plan, selected recipes, committed inputs, reserved capacity, completed work and blocking reason. Restart, packing and redeployment preserve that state.

Central crafting, cooking and brewing advance through bounded server work even while the cabin is packed. Catch-up uses elapsed game time, caps both elapsed duration and operations per pass, and never loads cabin chunks or ticks simulated blocks. Server downtime advances no game time.

Room automation uses [Milestone 5](05-functional-rooms.md) activation and bounded catch-up. Packing never grants ordinary blocks, furnaces or modded machines background simulation.

## Blocking and reserves

Jobs pause without losing their request or committed state when blocked by:

- missing uncommitted inputs
- unknown product or intermediate template
- unsupported or ambiguous recipe
- unavailable or disabled capability
- protected reserve
- insufficient output or by-product capacity
- unmet progression tier
- exhausted catch-up budget

Every pause exposes one actionable reason through role-filtered local status.

Owners may define hard item reserves. Cabin-controlled jobs cannot commit resources below them. Manual owner and resident withdrawals remain possible; reserves constrain automation, not people. Changing a reserve does not reclaim inputs already committed to an active job.

## Crafting automation

Crafting produces locally known safe templates from central-storage materials. It cannot reproduce custom names, arbitrary data, durability or enchantments. Returned containers and by-products must have reserved destinations before the job starts.

For a cabin upgrade, only the owner may approve the displayed plan for missing ingredients. Existing target-fund materials count first, and completed outputs enter only that capped fund. Funding never installs the upgrade; [Milestone 3](03-upgrade-interface.md) remains authoritative for installation.

## Cooking and brewing

A prepared meal must enter central storage before the cabin learns it. Reproduction requires a supported process profile, installed cooking capability, exact ingredients, destination capacity and timed work. Cooking never implies farming, feeding, slaughtering or ingredient acquisition.

A potion must likewise enter central storage. Its template retains only allowlisted potion identity and effects. Reproduction requires a declared brewing path, bottles, ingredients and the brewing capability. Custom and modded potions require explicit compatibility profiles when their data cannot be handled safely.

Farmer's Delight and compatible delight integrations contribute declared processes rather than exposing arbitrary block behavior. An Alex's Mobs Continued Delight profile may add declared ingredients and meals through Farmer's Delight or Farmer's Delight Refabricated recipes. Missing integrations leave unknown food storable but not reproducible and never prevent base startup.

## Room automation

Every [Milestone 5](05-functional-rooms.md) room remains useful manually. Each automatic action is installed and enabled separately:

- greenhouse harvesting and replanting
- livestock feeding, passive-product collection and surplus slaughtering
- forestry felling and replanting
- declared local-fixture and central-storage transfers required by those actions

An action stops at missing input, full output, population, safety, reserve or catch-up limits and reports the reason. Replanting consumes suitable seed or sapling stock; no action creates inputs.

Later forestry capabilities may process managed wood into charcoal and, at a late progression tier, declared renewable inputs into coal. Their recipes, rates, work and tier requirements are data-driven. Knowing or storing coal does not unlock synthesis.

## Technical approach

The cabin registry persists product knowledge, installed capabilities, owner configuration, reserves and jobs. A planner resolves safe recipe graphs and immutable work plans. A bounded scheduler advances persisted work from game time. Storage transactions own reservations, cancellation and atomic output commits. Room-specific services adapt the same work and transaction primitives to managed fixtures.

All planning and mutation decisions are server-authoritative. Client interfaces render synchronized plans, choices, progress and blocking reasons. Optional profiles must fail locally when their content is absent or invalid.

## Evergreen acceptance contract

Milestone 7 remains accepted only while automated tests and targeted manual checks establish that:

1. Safe templates persist after their source item leaves storage; unsafe components are never learned or reproduced.
2. Successful capability installation consumes one book, while duplicate or failed installation consumes nothing.
3. Owners and residents may request permitted local jobs; only owners change automation configuration, reserves and upgrade-funding plans.
4. Recursive planning respects limits, rejects cyclic or net-positive paths and accounts for every return and by-product.
5. Ambiguous supported recipes require explicit selection and never depend on inventory or registry order.
6. Work scales with ingredient units and recipe batches; process profiles use their declared durations.
7. Inputs and output capacity reserve before work, survive restart and return cleanly on cancellation.
8. Hard reserves block cabin-controlled commitment without blocking manual withdrawals or reclaiming active-job inputs.
9. Packed central jobs advance only through bounded game-time work without loading chunks; ordinary blocks remain paused.
10. Upgrade jobs fill only the selected capped fund and never install it.
11. Each room action requires its own installed and enabled capability and stops at every declared limit.
12. Missing optional integrations disable only their profiles and leave base startup and storage usable.

Codec, template-safety, planner, transaction, scheduler, room-service, GameTest and dedicated-server restart suites are the automated gates. Manual acceptance covers plan selection, progress, cancellation, status visibility and multiplayer requests.

## Out of scope

- enchantment learning, equipment requisitions and owner loadouts
- mailbox fulfilment, surplus delivery and inter-cabin transfers
- remote jobs or combined network storage
- arbitrary recipe, block, entity or component inference
- unbounded recursion, work, catch-up or background chunk loading
- automatic installation of produced cabin upgrades
- exact balancing values, loot tables and final interface art before delivery alignment
