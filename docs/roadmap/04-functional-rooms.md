# Milestone 4: Manual functional rooms, mounts and house cats

**Depends on:** Milestones 2 and 3.

**Outcome:** Players purchase, inhabit and operate useful specialised rooms manually; their named animals remain individual, safe residents rather than serialized copies or generic production items.

Major scope:

- bounded managed-room controller, activation lifecycle and capped catch-up framework
- greenhouse beds with manual planting and harvesting
- stable stalls that preserve eligible mounts by global entity UUID
- aquatic berth upgrade and safe check-in/check-out for a previously tamed nautilus
- one previously tamed house cat living naturally in the general cabin interior
- manual livestock populations founded by four real animals, with collection and deliberate surplus processing
- forestry plots with manual felling and replanting
- explicit species/crop/tree profiles, including supported Alex's Mobs Continued integration
- role-aware room operation and actionable failure messages

**Red:** Add failing identity, duplicate-entity, packed catch-up, capacity, ownership, aquatic-destination, cat-boundary and concurrent-collection tests.

**Green:** Deliver each room's manual loop before adding any automatic action.

**Refactor:** Reuse one managed-resident identity model and one bounded catch-up framework while retaining type-specific safety policies.

**Exit gate:** Every manual room remains useful, bounded and restart-safe; managed animal state can never create a second materialised copy of a mount, nautilus or cat.


## Consolidated specification

_Source: managed-room and animal sections of the former progression specification. Automatic room actions remain here beside their manual contracts and are gated by Milestone 5._

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

### House cats

A house cat is a companion resident of the general cabin interior, not a stable mount or livestock population. The base progression cabin supports one house cat without requiring a stable upgrade.

The owner must first tame a cat normally in the exterior world. While the cabin is deployed, the owner can bring that same cat through the entrance and deliberately assign the cabin as its home at the interior controller. Wild cats, another player's cats and cats already homed to another cabin are rejected. Registration preserves the cat's global entity UUID, owner, appearance, custom name, health and other safe vanilla state.

A homed cat behaves like a house cat rather than continuously following its owner:

- it remains in the general interior when the owner leaves through the exterior or hallway
- while not ordered to sit, it roams within the safe interior and favours beds, warm blocks, carpets, window perches and nearby owners
- the owner can still tell it to sit or stand using normal pet interaction
- it may sleep near its sleeping owner and produce vanilla cat gifts only from a real completed sleep event while the room is active
- it never generates gifts, breeding progress or other outputs through packed-time catch-up
- it cannot automatically cross a cabin exit, hallway door or functional-room boundary

The room controller keeps the cat away from the protected exit and void boundary. A homed cat is protected from damage and is returned to its home position if pathfinding or an owner-built hazard leaves it outside the safe interior. Because it cannot accompany players outside, this protection cannot be used to create an invulnerable combat pet. Other players cannot move or release it.

The owner may explicitly release the cat from its cabin home while the exterior is deployed. Release requires a safe exterior destination and removes the cabin protections; it never creates a second copy. Packing, hallway access and owner logout leave the cat safely at home.

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
- wild, differently owned or already homed cat
- missing feed or process input
- full local fixture or cabin storage
- population below the configured surplus threshold
- automation unknown, unavailable or disabled
- catch-up limit reached
- room tier or capacity too low
- integration profile absent or incompatible

These messages follow the visibility rules in the network and access specification.
