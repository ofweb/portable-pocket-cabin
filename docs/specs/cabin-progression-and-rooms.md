# Cabin Progression and Functional Rooms

## Status and relationship to the MVP

This document specifies a post-MVP feature track for Portable Pocket Cabin.

The nine MVP deliveries in [`SPEC.md`](../../SPEC.md) remain the implementation foundation. Once progression work begins, this document deliberately supersedes the MVP's fixed 21×21 starting interior and its rule that every packed interior is completely paused.

The project is not yet released. Progression may replace MVP interior generation and persistence without providing a migration path.

Storage, automation and learned-item behavior are specified in [Cabin Storage and Automation](cabin-storage-and-automation.md). Roles, shared hallways and packed-cabin access are specified in [Cabin Network and Access](cabin-network-and-access.md).

## Goals

Progression should:

- begin with a genuinely small but usable home
- make every increase in general-purpose space valuable
- reward travel through varied biomes, structures and dimensions
- make obsidian a meaningful mid-game cabin material
- provide specialised rooms that players inhabit, operate and customise
- remove repetitive resource chores gradually rather than granting an instant all-purpose factory
- remain safe and bounded while cabins are packed or unoccupied

It should not turn ordinary blocks anywhere in the pocket dimension into always-running machinery.

## General interior size

New progression cabins begin with approximately 4×4 blocks of usable floor space, excluding their protected shell.

Each size upgrade increases both usable dimensions by one block:

```text
4×4 → 5×5 → 6×6 → 7×7 → …
```

The entrance wall and doorway remain anchored. Expansion extends the rear boundary and uses a deterministic lateral pattern so existing player blocks never move. An upgrade must validate its target volume before modifying the shell and must abort without partial changes if that volume is not safe.

Each successive expansion costs more than the previous one. Costs, the maximum general-room size and the exact lateral expansion pattern are data-driven balancing values, but a finite maximum must be declared before implementation so interior allocations cannot collide.

General-purpose space remains ordinary Minecraft space. Players can furnish it with normal and compatible modded blocks; those blocks do not gain packed-time simulation merely because the room has been enlarged.

## Pocket room graph

A cabin is a graph of persistent bounded spaces rather than one indefinitely expanding cell.

- The general interior has its own allocated cell.
- Every purchased functional room receives a separate allocated cell.
- Every shared connection hallway receives a separate allocation as described in the network specification.
- Protected internal doors connect these spaces using short same-dimension teleports.
- Door traversal must not expose pocket coordinates, the void or another cabin's unconnected rooms.
- Unpurchased rooms allocate no cell.
- Cell and room identifiers are permanent and are never reused while their persisted contents may remain recoverable.
- Every room type declares a maximum footprint and the chunks needed for its active representation.

This model lets individual rooms expand without moving neighbouring cabins or reserving the maximum possible compound for every owner.

## Upgrade requirements

Upgrade requirements are data-driven. A requirement set may specify:

- exact items
- item tags or material categories
- quantities
- distinct biome or dimension material groups
- a reusable discovery catalyst from a structure or treasure source
- prerequisite room or cabin tiers

The default definitions must provide a vanilla progression. Optional integration profiles may replace or extend those definitions when compatible mods are installed. Servers and modpacks may override them with datapacks.

Structure-exclusive treasure should normally be a discovery catalyst rather than a repeatedly consumed ingredient. This preserves the exploration gate without turning finite structure loot into an ongoing multiplayer bottleneck.

The intended broad material arc is:

1. common Overworld wood, stone and agricultural materials
2. materials gathered from increasingly varied Overworld biomes
3. rarer structure discoveries and obsidian
4. Nether materials for stronger dimensional connections and magical infrastructure
5. End materials for the highest cabin and automation tiers

Exact recipes and quantities are balancing data rather than architectural rules.

## Optional content integrations

Optional integrations must fail closed and never prevent the base mod from loading when an integrated mod is absent.

### Biomes O' Plenty

An integration profile may use biome-specific woods, plants, stones and other unusual materials in the intended modpack progression. Requirements remain data-driven and retain vanilla fallbacks outside that pack.

### Alex's Mobs Continued

An explicit integration profile may provide:

- exploration catalysts tied to supported mobs, structures, advancements or loot
- unusual upgrade materials
- stable eligibility for declared rideable species
- livestock production profiles for declared sustainable species and drops

The cabin never infers that every entity is breedable, rideable or safe to manage. Each supported species needs an allowlisted profile describing the data that may be preserved and the actions the room supports.

### Alex's Mobs Continued Delight

An explicit cooking integration may expose declared ingredients and meals to kitchen learning and automation through compatible Farmer's Delight or Farmer's Delight Refabricated recipes.

Unknown food remains storable as an ordinary item. It becomes reproducible only when its recipe and item components pass the safe-learning rules in the storage and automation specification.

## Managed functional rooms

Functional rooms combine a physical player-facing room with an authoritative persisted controller.

The controller owns only declared fixtures and managed positions. Those fixtures are protected from pistons, explosions and unauthorised modification. The remainder of the room is ordinary customisable space.

When a managed room becomes active, the server:

1. computes bounded catch-up since its last persisted timestamp
2. applies only work supported by the room's installed and enabled capabilities
3. updates fixtures and visible managed contents to match authoritative state
4. runs normal active-room updates while the cell remains loaded
5. persists state and a new timestamp when the room becomes inactive

The room controller, not a displayed crop block or entity, decides whether an output has already been collected. All state-changing interactions are resolved server-side so concurrent visitors cannot collect the same result twice.

Catch-up has a configurable maximum duration and stops cleanly when it reaches an input, feed, seed, storage, local-output or population limit. It never ticks arbitrary player blocks, furnaces or modded machines.

Visitors see the same current physical projection whether the cabin is deployed or reached through a hallway while packed.

- Owners and residents may operate shared room facilities.
- Guests may enter and inspect but cannot change managed fixtures, harvest managed products or remove animals.
- Automated outputs go to that cabin's central storage.
- Actionable failure reasons appear through the cabin status system.

## Manual operation and targeted automation

Every functional room provides useful manual behavior before automation is discovered.

Automation books unlock small actions, not an all-or-nothing automated room. Examples include feeding, collecting, harvesting, replanting, felling and processing. Each action is installed knowledge, individually enabled per cabin by its owner, and subject to the bounded-job rules.

When a room is packed without an action being automated:

- passive growth or maturation may advance through catch-up
- preloaded local inputs may be consumed where the room explicitly supports that behavior
- outputs may accumulate only up to the fixture's small local capacity
- an action that requires a player, such as harvesting or replanting, does not happen by itself

## Greenhouse

The greenhouse is purchased separately from general interior space.

Its initial form includes protected growing beds and the infrastructure needed to support them, but no crops. Players supply and plant all seeds, saplings or crop items.

Without automation:

- owners and residents plant and harvest supported managed beds manually
- supported crops mature while packed through bounded catch-up
- mature crops remain unharvested until a player collects them
- unsupported crops behave as ordinary blocks and pause when the room is not active

Later upgrades may add:

- more beds and usable room space
- additional supported crop categories
- increased local output capacity
- automatic harvesting
- automatic replanting

Harvesting and replanting are separate automation abilities. Automatic replanting requires suitable seed stock; neither ability creates missing inputs.

## Stable

The stable holds specific individual rideable animals rather than abstracting them into a generic capacity count.

An eligible mount is deliberately checked into a managed stall while dismounted. The stable preserves its global entity UUID and all integration-approved identity data, including:

- entity type and owner
- health
- custom name
- equipment
- inventory
- declared mod-specific state

The mount is visibly materialised in its stall while the room is active, protected from damage and prevented from accidentally leaving the managed area. Checking it out rematerialises the same mount at a validated destination. The stored and released forms must never exist simultaneously.

Stable records remain attached to the cabin UUID while its exterior is packed. Carrying the packed cabin by foot, boat or other ordinary transport does not remove, copy or rematerialise stored mounts.

Residents may store and retrieve their own mounts. A mount may be released only by its owner or the cabin owner. Guests and unrelated residents cannot remove it.

Unsupported entities are rejected with a clear explanation. Stable upgrades add managed stalls and may unlock additional explicitly supported mount categories.

## Livestock rooms

Livestock production is a late-game feature purchased separately for each supported animal type.

Four eligible animals establish a room's breeding population. The founders become managed residents of that room and cannot simultaneously exist elsewhere. Only the cabin owner may dismantle the population and recover them; dismantling stops production. Founders are never selected for slaughter.

Each species profile declares:

- valid founders
- acceptable feed
- passive products and their cycles
- surplus-population rules
- slaughter products
- visual representation
- safe persisted data

### Manual baseline

- Players place feed into the room trough.
- A fed population produces and breeds up to the room's limits.
- Running out of feed pauses production; animals do not starve or die.
- Eggs, wool, milk, feathers and similar passive products accumulate in appropriate room fixtures up to a small local limit.
- Owners and residents collect those products manually.
- Surplus offspring accumulate as a managed count.
- A player deliberately uses the butchery station to convert eligible surplus into meat, leather and declared species drops.

Residents may tend the population and collect or process products, but cannot dismantle the founding population or change cabin-wide production rules.

### Targeted automation

The following are independent discoveries and owner-controlled toggles:

- **Automatic feeding:** moves declared feed from cabin storage into the room trough.
- **Automatic collection:** transfers eligible passive products into cabin storage.
- **Automatic slaughtering:** processes surplus above an owner-configured retained-population threshold.

Every action stops when its inputs, output space or safety limits are unavailable and reports the exact reason.

## Forestry room

The forestry room provides renewable wood production after the player supplies each desired sapling variety.

Without automation, supported managed trees may mature during bounded packed catch-up, but owners and residents must fell them and replant saplings manually. Unsupported trees behave as ordinary blocks.

Upgrades may increase:

- managed plot count
- room space
- number of simultaneously configured tree varieties
- local output capacity
- growth or processing throughput

Automatic felling and automatic replanting are separate discoveries. Replanting consumes a suitable sapling from local fixtures or cabin storage.

A later kiln capability converts managed forestry output into charcoal through its own bounded job. It does not imply felling or replanting. A very late coal-synthesis upgrade may convert declared renewable inputs into actual coal for recipes that do not accept charcoal. Its recipe and rate are data-driven and require late dimensional progression; learning or storing coal alone does not unlock it.

## Required status messages

At minimum, functional rooms must be able to explain:

- missing or unsupported seed, sapling, animal or mount
- missing feed or process input
- full local fixture or cabin storage
- population below the configured surplus threshold
- automation unknown, unavailable or disabled
- catch-up limit reached
- room tier or capacity too low
- integration profile absent or incompatible

These messages follow the visibility rules in the network and access specification.

## Balancing decisions intentionally left data-driven

- exact upgrade recipes and quantities
- maximum general-room dimensions
- functional-room dimensions and tier counts
- catch-up duration caps
- growth, breeding and production rates
- storage and local-fixture capacities
- supported integration entries
- charcoal and late coal-synthesis recipes
