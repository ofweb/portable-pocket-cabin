# Milestone 5: Functional rooms

**Depends on:** Milestones 3 and 4.

**Outcome:** Players purchase, enter and manually operate useful specialised rooms that remain bounded, persistent and isolated from every other pocket space.

**Status:** Draft. Each delivery requires alignment before implementation.

## Deliveries

### Delivery 5.1: Room installation and traversal — Draft

Purchase one generic empty room, allocate its permanent isolated cell, enter it through a protected internal door and return safely after restart. This delivery establishes the minimum room controller and traversal contract used by later room types.

### Delivery 5.2: Greenhouse — Draft

One greenhouse with manually planted and harvested managed beds.

### Delivery 5.3: Stable — Draft

One stable that checks eligible mounts in and out without changing or duplicating their identity.

### Delivery 5.4: Aquatic berth — Draft

One stable upgrade that safely houses and releases an eligible tamed nautilus.

### Delivery 5.5: Livestock room — Draft

One manually tended, bounded population for a single explicitly supported animal type.

### Delivery 5.6: Forestry room — Draft

One manually felled and replanted managed tree plot.

**Milestone exit gate:** Every accepted room delivery remains bounded and restart-safe; no room exposes another pocket cell, and managed animals never have two simultaneously materialised representations.


## Consolidated specification

_Source: managed-room sections of the former progression specification. Automatic room actions remain here beside their manual contracts and are gated by Milestone 7._

### Room installation and traversal

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

### Managed functional rooms

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

### Greenhouse

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

### Stable

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

#### Aquatic berth and nautilus

The base stable uses dry stalls. An **aquatic berth** upgrade adds a protected, fully flooded stall and explicitly supports a normal tamed nautilus as a stable resident.

The nautilus must already have been tamed, must be owned by the player checking it in and must be dismounted. Check-in preserves the same global entity UUID, owner, health, custom name, saddle, nautilus armour and other allowlisted vanilla nautilus state. Wild nautiluses and zombie nautiluses are not eligible for the initial upgrade.

While the stable room is active, the nautilus is visibly materialised in the berth with enough water to avoid suffocation. While inactive or packed, its authoritative stable record remains attached to the cabin like any other checked-in mount.

Checkout uses an aquatic destination resolver. It requires sufficient connected water, collision-free space and no immediate environmental hazard at the destination. A dry exterior, shallow decorative pool or obstructed water volume is rejected without removing the nautilus from its berth. This allows a player to carry a checked-in nautilus between oceans without ever materialising it on land.

The aquatic berth is a stable-capacity upgrade, not a livestock room: it does not breed nautiluses, generate shells or abstract the animal into production.

### Livestock rooms

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

#### Manual baseline

- Players place feed into the room trough.
- A fed population produces and breeds up to the room's limits.
- Running out of feed pauses production; animals do not starve or die.
- Eggs, wool, milk, feathers and similar passive products accumulate in appropriate room fixtures up to a small local limit.
- Owners and residents collect those products manually.
- Surplus offspring accumulate as a managed count.
- A player deliberately uses the butchery station to convert eligible surplus into meat, leather and declared species drops.

Residents may tend the population and collect or process products, but cannot dismantle the founding population or change cabin-wide production rules.

#### Targeted automation

The following are independent discoveries and owner-controlled toggles:

- **Automatic feeding:** moves declared feed from cabin storage into the room trough.
- **Automatic collection:** transfers eligible passive products into cabin storage.
- **Automatic slaughtering:** processes surplus above an owner-configured retained-population threshold.

Every action stops when its inputs, output space or safety limits are unavailable and reports the exact reason.

### Forestry room

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

### Required status messages

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

### Optional entity integrations

The cabin never infers that every entity is breedable, rideable or safe to manage. Each supported species needs an allowlisted profile describing the data that may be preserved and the actions its room supports.

An explicit Alex's Mobs Continued profile may provide:

- exploration catalysts tied to supported mobs, structures, advancements or loot
- unusual room-upgrade materials
- stable eligibility for declared rideable species
- livestock production profiles for declared sustainable species and drops
