# Portable Pocket Cabin

## Goal

Create a Minecraft Fabric 26.2 mod for a travelling play style.

Players should be able to settle somewhere for a while, establish a campsite, farm, cook, craft and collect large amounts of loot, then pack their important home infrastructure and move somewhere new.

The portable part is deliberately limited to a small cabin with a larger interior in a pocket dimension. The MVP interior is fixed-size; post-MVP progression may expand it and attach specialised rooms.

The surrounding settlement remains part of the exterior world. Defences, walls, paths, ordinary animal pens, mines, docks and other local construction must be rebuilt at each new campsite. Household pets and animals deliberately checked into purchased functional rooms are narrow progression exceptions, not a way to carry an entire settlement.

This should make moving convenient without making location irrelevant.

---

## Core concept

Each portable cabin consists of two connected pieces:

**Exterior cabin**

A small attractive cabin placed in the Overworld, Nether or End, approximately 5×5 blocks.

It contains the entrance to the house but very little usable internal space itself.

**Pocket interior**

A private room located in a dedicated pocket-home dimension.

The interior is significantly larger than the exterior cabin and persists permanently regardless of whether the cabin is deployed or packed.

Typical contents:

- beds
- personal storage
- Tom's Simple Storage network
- crafting stations
- furnaces
- Farmer's Delight kitchen
- indoor crop plots
- decorations
- other normal blocks and block entities

Nothing inside needs to be serialized when travelling. Moving the cabin only moves its entrance.

---

## Player loop

The intended loop is:

```text
Explore
↓
Find somewhere interesting
↓
Deploy cabin(s)
↓
Establish local defences and outdoor infrastructure
↓
Live there for some time
↓
Collect resources / cook / farm / explore
↓
Pack cabins
↓
Travel onwards
```

A multiplayer campsite may therefore look like:

```text
Sune's cabin
Adam's cabin
Alex's shared kitchen/storage cabin
Sam's farm/greenhouse cabin

+ locally built walls
+ animal pens
+ watchtower
+ roads
+ mine
+ docks
+ outdoor farms
```

A group can keep most functions in personal cabins or let individual players specialise their cabins as shared facilities.

---

# Cabin identity

Every cabin has a permanent UUID.

The UUID identifies:

- its pocket-dimension interior
- owner
- access permissions
- dimension-qualified exterior position, when deployed
- last valid dimension-qualified exterior position
- permanent interior cell index
- permanent Cabin palette

A packed cabin item contains the UUID and display information.

The actual interior never lives inside the ItemStack.

This also means losing or duplicating an item cannot silently duplicate the contents of a house.

The authoritative cabin registry, rather than an item or exterior block, decides whether a cabin is deployed and where its entrance is. Duplicated or stale items must never be able to activate a second entrance.

## Lifecycle and crash consistency

Every cabin has exactly one persisted lifecycle state:

```text
PACKED
DEPLOYING
DEPLOYED
PACKING
ORPHANED
```

Placement and packing are journalled, idempotent transitions in the authoritative cabin registry. Cabin items and exterior blocks are projections of that state, not independent sources of truth.

Startup and chunk-load reconciliation must finish or safely roll back interrupted transitions. It must remove or disable stale entrances, restore a recoverable packed representation when necessary and preserve the one-active-entrance invariant.

A crash during packing or placement may leave physical cleanup for reconciliation, but it must not make the interior unreachable or allow two usable entrances.

---

# Interior dimension

Use one dedicated dimension containing all portable-house interiors.

Individual cabins receive isolated cells far enough apart that players cannot normally reach another interior.

Each cabin receives a permanent, monotonically increasing cell index. The index maps to a fixed grid with enough space for the 21×21 room, its shell, all simulated chunks and an isolation buffer. Cell indices are never reused, including after a cabin becomes orphaned.

Conceptually:

```text
Pocket Homes Dimension

[House 1]          [House 2]          [House 3]


          hundreds of blocks apart
```

The dimension should be a void or otherwise inaccessible world except through cabin entrances.

During the MVP, every deployed cabin keeps only the chunks covering its bounded interior cell fully simulated. Packing the cabin removes those chunk tickets, pausing crops, furnaces, storage networks and other ticking blocks until it is deployed again.

The interior shell should prevent players from digging into the void or escaping into another cabin.

---

# Cabin acquisition

The first cabin begins as a craftable unbound cabin kit with no UUID or interior.

On its first successful placement, the authoritative registry atomically verifies that the player does not already own a cabin, creates the permanent UUID and cell index, binds the player as owner and begins the `DEPLOYING` transition.

A player who already owns a cabin cannot bind another unbound kit but may give it to a player who does not own one.

The implemented MVP recipe is a functional placeholder. Its multi-stage replacement, material budget, palette-aware Cabin Kit recipe and fresh-world policy are specified in [Cabin Acquisition and Relocation](docs/specs/cabin-acquisition-and-relocation.md).

# Cabin placement

An unbound cabin kit or owner-held packed cabin can be placed in the Overworld, Nether or End. Only the owner may redeploy a bound cabin during the MVP. Modded dimensions are rejected in the MVP.

The post-MVP acquisition release replaces player-facing placement commands with the predictable two-use surface interaction specified in [Cabin Acquisition and Relocation](docs/specs/cabin-acquisition-and-relocation.md): the clicked terrain supports the front stair, and the door faces the player.

Each cabin UUID may have only one deployed exterior across all dimensions at a time.

Placement should:

1. Show the footprint before confirming.
2. Require sufficient free space for the exact cabin-owned structure mask.
3. Require the shared safe-destination resolver to find a valid area outside the door.
4. Generate the exterior cabin.
5. Register its dimension-qualified exterior position.
6. Enable the interior exit.
7. Update the fake windows with information from the new location.

The cabin should face the direction chosen by the player.

Rotation affects only the exterior and the relationship between the interior door/window state and outside location. The interior itself does not need to rotate.

The MVP exterior is a fixed, protected multiblock structure with one controller block and an exact cabin-owned placement mask. Ordinary players cannot modify the owned blocks, and explosions cannot damage them. Packing removes only blocks in that mask and never clears an enclosing volume or nearby player construction.

Placement never requires sky exposure. A cabin may be deployed underground in any supported dimension when the player has excavated enough room for the exact protected structure mask and a safe standing area outside its door. Placement validates this volume but never clears terrain on the player's behalf.

# Safe destinations

Placement, ordinary exit, packing evacuation, offline-player recovery and respawning must all use the same safe-destination resolver.

A safe destination requires:

- a solid floor
- enough collision-free space for the player
- no dangerous fluid or fire
- a position inside the world border
- a destination chunk that can be loaded under a bounded temporary ticket

The resolver loads the destination chunk before validating collision and hazards. It must release temporary search tickets after success, failure or timeout.

The resolver searches within a fixed radius around the preferred position and uses this fallback order:

1. current exterior doorway
2. a nearby safe position in the current exterior dimension
3. the last valid exterior campsite and nearby positions
4. Overworld world spawn as the final emergency fallback

Voluntary packing must abort before entering `PACKING` if current occupants cannot be evacuated safely. Unexpected exterior loss uses the full emergency fallback order.

---

# Entering and leaving

Opening or interacting with the exterior cabin door transports the player to the corresponding interior entrance.

Using the interior door transports the player back to a safe position outside the exterior door.

The transition may initially use a normal dimension-loading screen.

Seamless rendered portals are explicitly unnecessary.

---

# Packing a cabin

During the MVP, packing is performed from outside the cabin. The post-MVP connection upgrade adds network-aware packing from inside as specified in [Cabin Network and Access](docs/specs/cabin-network-and-access.md).

The acquisition release makes outside packing command-free: the owner sneak-uses the exterior controller twice before the existing validation and countdown begin. Commands remain operator tools. The complete interaction contract is specified in [Cabin Acquisition and Relocation](docs/specs/cabin-acquisition-and-relocation.md).

Only the cabin owner may pack it during the MVP. Before entering `PACKING`, the operation reserves enough owner inventory capacity for the bound packed item. If the owner disconnects or item delivery cannot be guaranteed, packing aborts and reconciliation restores `DEPLOYED`.

Packing should behave as:

```text
request pack
↓
validate permission and evacuation destinations
↓
atomically enter PACKING
↓
disable the exterior entrance
↓
show occupants a short countdown
↓
identify online occupants from their interior cell coordinates
↓
teleport all online occupants outside
↓
commit PACKED in the authoritative registry
↓
remove the protected exterior mask
↓
create or update the packed cabin item
```

Players inside should get a short countdown before being moved outside. New entry is rejected for the entire countdown, and occupants are identified again immediately before evacuation.

Example:

```text
Cabin is being packed in 5 seconds…
```

During the MVP, nobody may enter a packed interior through normal gameplay. A later connection upgrade deliberately supersedes this rule by retaining access through a shared connection hallway while removing only the exterior anchor.

The interior itself remains completely untouched.

---

# Offline players

A player may log out while inside a cabin.

This must never prevent the owner from moving.

Packing does not need to enumerate or modify offline player files. A player's pocket coordinates map unambiguously to a permanent cabin cell index.

During the MVP, when the player next logs in:

```text
player is inside a cabin whose lifecycle is not DEPLOYED
↓
resolve the cabin UUID from the player's pocket coordinates
↓
teleport using the shared safe-destination resolver
↓
continue login normally
```

This avoids both trapping players and preventing someone from packing a house because another player logged out days earlier.

The post-MVP connection upgrade replaces this unconditional evacuation with a permission-aware check for a reachable deployed exterior in the cabin's hallway network.

---

# Respawning

Portable Pocket Cabin owns player respawn handling; Better Respawn is not a dependency.

Successfully sleeping in a cabin binds the player to that cabin UUID and bed position. The most recently used cabin bed replaces the previous cabin-home binding.

During the MVP, only the cabin owner can establish this binding. Trusted visitors may sleep without changing their existing home binding. Resident respawn rights are deferred to the communal-access upgrade.

At respawn time:

1. If the bound cabin is `DEPLOYED` and the bed still exists with a safe adjacent position, respawn beside that bed inside the pocket interior.
2. If the cabin is deployed but the bed is missing or obstructed, use the current exterior doorway through the shared safe-destination resolver.
3. If the cabin is packed or orphaned, search for a safe location in the same dimension between 128 and 256 blocks from the death position by default. Both bounds are server-configurable.
4. If that search fails, use the shared resolver beginning at the cabin's last valid exterior campsite.
5. If no cabin destination remains usable, use Overworld world spawn.

Near-death searching is allowed in the Overworld, Nether and End. It must never select a location inside the pocket-home dimension; death there falls back to the cabin exterior or Overworld world spawn.

The near-death search examines at most 16 candidate regions within a bounded time budget. Every candidate uses the shared safe-destination rules. Exhaustion or timeout immediately continues to the fallback steps rather than blocking respawn.

The cabin lifecycle and selected destination must be revalidated immediately before respawning so a concurrent packing operation cannot place a player inside a disabled cabin.

---

# Fake windows

Pocket interiors should contain windows that visually represent conditions outside the currently deployed cabin.

These are deliberately fake windows rather than rendered portals.

The current window presentation is acceptable only as an MVP placeholder. A later visual pass should provide substantially more fidelity and a stronger sense of depth while preserving the lightweight fake-window architecture and its readable outside-condition cues. Exact art direction is deferred until after the core progression systems are established.

They should communicate enough information that players can decide whether they want to leave.

At minimum:

```text
dawn
day
sunset
night
rain
thunderstorm
```

The visual state should be derived from the dimension-qualified exterior position of that cabin.

For an Overworld placement, important information is:

- exterior world's time of day
- rain
- thunder
- possibly exterior light level
- optionally biome/temperature later

For example, a night window could show a dark sky and stars; rain adds rain streaks; thunderstorms occasionally illuminate the window.

Nether placement uses a static Nether ambience without weather states. End placement uses a static End-sky ambience without weather states. A packed or orphaned cabin shows closed shutters or another inactive opaque state.

Later versions could add broad biome scenery:

```text
forest     leafy silhouette
plains     open horizon
snow       snowy landscape
desert     sandy landscape
ocean      blue horizon
```

This should remain stylised rather than attempting to render the actual overworld terrain.

Real see-through dimensional windows are outside the scope of the mod. Immersive Portals demonstrates that this is possible, but it introduces a much larger rendering/portal problem and currently targets older Minecraft versions.

---

# Interior size and future progression

The MVP has one fixed 21×21 usable base interior.

After the MVP is stable, the progression release replaces the fixed starting layout with an upgradeable 4×4 usable interior and persistent specialised rooms. Its design is specified in [Cabin Progression and Functional Rooms](docs/specs/cabin-progression-and-rooms.md).

The player's base interior and attached rooms together form that player's single cabin. The project is not yet released, so this progression release may replace MVP interior generation and persistence without a migration path.

Progression is grounded in a consistent material language: amethyst resonance performs the cabin's magical pattern work, including enchanting without experience, while obsidian anchors entrances, rooms and hallways to the pocket dimension. Upgrade requirements vary between world saves through one persistent world attunement shared by every cabin in that save.

The progression cabin may also become a home for one previously tamed house cat. Stable upgrades preserve individual checked-in mounts, including a dedicated flooded aquatic berth for a tamed nautilus. These residents retain their identity and never become duplicated item data or generic production counts.

---

# Multiple players and connected cabins

The world supports many cabins, but each player may own exactly one personal cabin.

Cabin ownership should support:

- one owner
- optional trusted players
- configurable entry permission

A cabin remains owned by one player while trusted players may use it through the deployed exterior.

## Future communal-access upgrade

Persistent connections are not part of the MVP. A later paid connection upgrade lets mutually consenting cabin owners join a persistent shared hallway, with one protected doorway per connected cabin.

The hallway supplies the physical wall and common space for those doors. Players may furnish it with ordinary blocks, but it is not a combined storage or automation system. Destination-cabin permissions are rechecked at every doorway.

Hallway access remains available when a member cabin is packed. Network-aware packing evacuates only occupants who would otherwise lose their final permitted route to a deployed exterior. This deliberately supersedes the MVP rule that packing disables all access to the interior.

The complete role, mailbox, connection and safe-packing rules are specified in [Cabin Network and Access](docs/specs/cabin-network-and-access.md).

---

# Exterior design

The exterior should look like a proper Minecraft cabin rather than a tent or technical portal block.

The initial implementation uses the fixed, protected structure and ownership mask defined under Cabin placement.

The acquisition release replaces its fixed stone palette with a recipe-selected Cabin palette containing independent floor, wall/frame, roof/ceiling and door materials. The same palette appears inside and outside and persists with cabin identity. Vanilla materials and a bundled Biomes O' Plenty profile are specified in [Cabin Acquisition and Relocation](docs/specs/cabin-acquisition-and-relocation.md).

Cosmetic variation should preferably use ordinary Minecraft blocks and/or resource-pack-friendly models.

The exterior should intentionally remain small relative to the interior. The slightly impossible "bigger inside" quality is part of the idea.

---

# Farming

Farming inside cabins is explicitly supported.

The room should support normal blocks including:

- dirt
- farmland
- water
- crops
- Farmer's Delight crops
- Farmer's Delight kitchen/storage blocks

Farmer's Delight Refabricated currently supports Fabric 26.2 and includes crops, rich soil and kitchen equipment, making it an important compatibility target.

Crop ticking and other block simulation behave continuously while the cabin is deployed because its bounded interior chunks remain fully simulated.

MVP crop simulation pauses while a cabin is packed. Post-MVP functional rooms use bounded managed catch-up instead: designated greenhouse crops may mature while packed without ticking arbitrary blocks or machinery. See [Cabin Progression and Functional Rooms](docs/specs/cabin-progression-and-rooms.md).

---

# Storage

The interior should behave like ordinary Minecraft space so storage mods work without special integration.

Tom's Simple Storage is a particularly important compatibility target because the planned modpack uses it and it has a current Fabric 26.2 version. It provides connected storage and crafting terminals over ordinary inventories.

The portable-home mod should know nothing about Tom's internal storage model.

If Tom's works inside any normal Minecraft dimension, it should work inside a cabin.

This principle should apply to other modded blocks wherever possible.

Post-MVP cabin-owned storage and automation are an additional explicit system, not a replacement backend for ordinary chests or Tom's Simple Storage. Those rules are specified in [Cabin Storage and Automation](docs/specs/cabin-storage-and-automation.md).

---

# Exterior destruction

Unexpectedly destroying the exterior must never destroy the interior.

If the cabin controller or exterior disappears despite MVP protection because of:

- player
- explosion
- command
- world modification

reconciliation should transition the cabin safely into an orphaned state. Ordinary player modification and explosion damage to the protected exterior are cancelled.

No operation on the exterior should delete the pocket interior.

Recovery uses explicit OP-only commands rather than a generic command that guesses the intended mutation:

```text
/cabin list [state|owner]
/cabin inspect <uuid>
/cabin reconcile <uuid>
/cabin recover-item <uuid> <player>
```

`reconcile` idempotently repairs physical projections to match the authoritative registry. `recover-item` refuses to run while a valid deployed exterior exists. Otherwise it first reconciles the cabin to `PACKED`, increments a packed-item generation counter so every older copy becomes stale, and gives the new bound item to the named owner.

These commands are primarily development diagnostics and world-corruption insurance.

---

# Data safety

The interior is the valuable part of the system.

Rules:

1. Never delete an interior when an exterior disappears.
2. Never reuse a cabin UUID.
3. Never create two simultaneously active exterior entrances for the same cabin.
4. Journal lifecycle state before removing or replacing an exterior cabin, and reconcile interrupted transitions.
5. Keep enough information to recover orphaned cabins administratively.

A server crash during placement or packing may interrupt physical cleanup, but reconciliation must restore a usable `DEPLOYED` or `PACKED` projection without exposing two entrances or losing administrative recovery access.

---

# MVP

The first useful version should deliberately stay small.

MVP features:

```text
Fabric 26.2

one exterior cabin design

one pocket-home dimension

one permanent UUID per cabin

21×21 interior

place cabin in the Overworld, Nether or End

enter/exit cabin

pack cabin

teleport occupants outside when packing

handle offline occupants on next login

fake window with:
  day
  night
  rain
  thunder
  Nether ambience
  End ambience
  packed/orphaned inactive state

built-in cabin-bed and near-death respawning

persistent contents

owner-only packing, deployment and cabin-bed respawn binding

one owned cabin per player

trusted-player entry through the deployed exterior

explicit OP inspection, reconciliation and item-recovery commands
```

Do not implement progression until this version is stable.

The first compatibility test world should contain:

```text
vanilla chest with items
double chest
furnace
bed
water
farmland
growing crops
Farmer's Delight crops
Farmer's Delight kitchen
Tom's Simple Storage network
```

Pack/unpack the exterior repeatedly and verify that nothing inside changes.

## Implementation deliveries

The MVP is implemented as nine gated deliveries. After every delivery, an operator must be able to create a fresh world and see and interact with the completed mod subsystem. OP-only development commands may stand in for unfinished survival recipes or UI, but every delivery must produce a runnable build with explicit automated and manual acceptance tests.

### Delivery 1: Project and world foundation

- Fabric 26.2 project
- automated game-test world
- empty pocket-home dimension
- dedicated-server startup test
- OP commands to report mod status and visit/leave a safe pocket-dimension test platform

### Delivery 2: Persistent cabin registry

- permanent UUIDs and monotonic cell allocation
- lifecycle states
- one-cabin-per-player rule
- OP commands to create, list and inspect cabin records and visit a visible debug marker at each allocated cell
- restart and allocation-collision tests

### Delivery 3: Overworld cabin vertical slice

- protected exterior and controller
- 21×21 base interior generation
- placement preview
- owner-only placement
- enter and exit
- no packing yet

### Delivery 4: Packing and crash recovery

- shared safe-destination resolver
- entrance locking and countdown
- occupant evacuation
- inventory reservation and bound item delivery
- packing and redeployment
- reconciliation tests interrupted at every lifecycle transition

### Delivery 5: Multiplayer safety

- trusted-player entry
- permission rechecks
- offline-player evacuation
- simultaneous placement and packing attempts
- two-client acceptance tests

### Delivery 6: Interior simulation

- deployed interior chunk tickets
- packed simulation pause
- crops, furnaces, water and vanilla storage tests

### Delivery 7: Cabin respawning

- owner-only cabin-bed binding
- deployed, packed and orphaned behavior
- bounded 128–256-block near-death search
- death-screen packing race tests

### Delivery 8: Nether and End support

- dimension-qualified exterior locations
- placement and safe evacuation in each supported dimension
- same-dimension near-death respawning
- Nether, End and inactive window profiles

### Delivery 9: Compatibility and release UX

- survival crafting recipe
- complete fake-window presentation
- Farmer's Delight compatibility world
- Tom's Simple Storage compatibility world
- admin recovery workflow
- complete MVP regression test

Save compatibility is required between these nine deliveries because together they form one MVP.

---

# Later features

Once the basic model is stable, the larger design continues in four independent feature tracks:

- [Cabin Acquisition and Relocation](docs/specs/cabin-acquisition-and-relocation.md)
- [Cabin Progression and Functional Rooms](docs/specs/cabin-progression-and-rooms.md)
- [Cabin Storage and Automation](docs/specs/cabin-storage-and-automation.md)
- [Cabin Network and Access](docs/specs/cabin-network-and-access.md)

Their implementation order and cross-feature dependencies are maintained in [the project roadmap](ROADMAP.md).

Additional later possibilities include:

```text
several exterior cabin styles

wood/material customisation

shared ownership

biome-aware fake windows

weather animation

sleeping inside affects overworld night

named cabins

map/waypoint integration

specialised greenhouse/workshop exterior designs
```

True rendered windows into the overworld remain intentionally outside the plan unless there is a very compelling reason later.

---

# Inspiration / related mods

## Pocket Dimension

Closest inspiration for the interior architecture.

It gives players persistent personal rooms accessed through portable Pocket blocks, limits players to one active entrance and has explicit handling for what happens when an entrance disappears while occupants are inside.

Useful ideas to examine:

```text
interior allocation
persistent dimension ownership
entrance ↔ interior mapping
occupant recovery
one-active-entrance rule
```

Our design differs by making the entrance a physical cabin and deliberately connecting the interior visually to exterior time/weather.

## Simply Tents

Useful inspiration for the player interaction and ownership side.

It supports deployable structures, ownership, multiple sizes, packing and interior preservation.

Useful ideas:

```text
placement validation
owner-only packing
size progression
cosmetic/material variation
pack interaction
```

The visual tent design and physically stored interior are specifically things this mod does differently from our design.

## AreaScale

Useful reference for robust interaction around portable builds.

AreaScale can select and capture an arbitrary region into an item, including block-entity contents and entities.

Useful ideas:

```text
placement preview
space validation
safe world mutation
visual bounding boxes
```

Our cabin avoids needing to serialize arbitrary structures because the real base never moves.

## MoveYourHouse

Useful inspiration for treating relocation as an explicit gameplay action tied to a house/territory rather than simply carrying a portal block.

The mod currently supports Fabric 26.2 and exposes a house block, permissions and a move-house action.

Useful ideas:

```text
house ownership
permissions
relocation UX
multiplayer access
```

## Immersive Portals

Reference only.

It demonstrates genuine see-through portals between dimensions and seamless dimensional transitions.

It is useful evidence for what would technically be possible, but that rendering complexity is specifically excluded from this project. Fake windows provide the useful part of the experience much more cheaply.

---

# Design principle

The portable cabin carries the things that make a location feel like home:

```text
your room
food
storage
crafting
farming
possessions
a tamed house cat
deliberately checked-in stable residents
```

It does not carry the settlement.

Every new location still asks the players to establish themselves there:

```text
make the area safe
build defences
create paths
build outdoor pens for ordinary animals
explore
mine
adapt to the terrain
```

The cabin makes moving pleasant without removing the reason to settle somewhere in the first place.
