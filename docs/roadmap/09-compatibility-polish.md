# Milestone 9: Compatibility, balance and presentation pass

**Depends on:** The systems being tuned or presented.

**Outcome:** The complete feature set feels coherent in survival and remains reliable in the intended modpack.

Major scope:

- full vanilla, Biomes O' Plenty, Farmer's Delight Refabricated, Tom's Simple Storage, Alex's Mobs Continued and Alex's Mobs Continued Delight compatibility matrix
- survival tuning for acquisition recipes, attunement pools, room costs, capacities, catch-up caps, production rates, automation loot and enchanting formulas
- higher-fidelity fake windows while retaining the lightweight state-driven architecture
- consistent models, textures, sounds, tooltips, interfaces and actionable status messages
- multiplayer soak tests, repeated restart/reconciliation tests and administrative recovery rehearsal
- documented supported versions, datapack/profile extension points and failure behavior when optional mods are absent

**Red:** Add compatibility and regression cases for every declared integration and visual state before final tuning.

**Green:** Resolve compatibility and presentation gaps without weakening lifecycle or transaction invariants.

**Refactor:** Remove provisional interfaces only after their replacements preserve the same interaction contracts and diagnostics.

**Exit gate:** A fresh modpack survival world can progress through every completed milestone without commands, silent data loss, impossible requirements or undocumented integration behavior.


## Future decision gates and exploratory backlog

These ideas are recorded so they are not lost, but they are not committed milestones until their open safety, UX or scope questions are specified:

- controller-based Cabin palette renovation after the progression controller is stable
- automatic emergency fire-packing after offline-owner, full-inventory, evacuation and network-race behavior is specified
- additional exterior styles and specialised-room exterior designs
- biome-aware fake-window scenery and richer weather animation
- cabin naming and map/waypoint integration
- making cabin sleep affect the exterior world's night
- shared ownership, which would require deliberately replacing the current exactly-one-owner role model
- inferred arbitrary modded-material support, which remains rejected in favor of explicit profiles unless a safe contract is designed

Upgrade recipes remain in the world-attuned controller system rather than the crafting table. True rendered cross-dimensional windows remain outside the project scope.

Before beginning a milestone, settle only its listed data-driven decisions and expand that milestone into implementation-sized tasks. Do not prematurely freeze balance values belonging to later systems.

## Progression balance decisions

### Balancing decisions intentionally left data-driven

- exact upgrade recipes and quantities
- world-attunement material pools, exclusions and selection weights
- maximum general-room dimensions
- functional-room dimensions and tier counts
- catch-up duration caps
- growth, breeding and production rates
- storage and local-fixture capacities
- supported integration entries
- charcoal and late coal-synthesis recipes

## Network balance and interface decisions

### Balancing and interface decisions intentionally deferred

- exact connection-upgrade recipes and tiers
- hallway dimensions and visual variants
- maximum cabins per hallway, if a practical server limit is needed
- mailbox slot count
- hallway creation, invitation and departure screen layout
- explicit hallway dismantling and administrative-recovery commands
- whether residents may bind their respawn point to a cabin bed

## Storage and automation balance decisions

### Balancing decisions intentionally left data-driven

- storage slots per upgrade tier
- automation-book loot sources and rarity
- job depth and operation limits
- hard-reserve defaults
- enchantment material-cost formulas
- library slots per tier
- supported crafting, cooking, brewing and enchanting integrations
- whether installing an automation book consumes the physical book

## Later-feature context and references

_Source: later-feature, inspiration and design-principle sections of the former root specification._

## Later features

Once the basic model is stable, the larger design continues in four independent feature tracks:

- [Cabin Acquisition and Relocation](01-acquisition-relocation.md)
- [Cabin Progression and Functional Rooms](02-progression-space.md)
- [Cabin Storage and Automation](05-production-automation.md)
- [Cabin Network and Access](07-connected-cabins.md)

Their implementation order and cross-feature dependencies are maintained in [the project roadmap](../../ROADMAP.md).

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

## Inspiration / related mods

### Pocket Dimension

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

### Simply Tents

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

### AreaScale

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

### MoveYourHouse

Useful inspiration for treating relocation as an explicit gameplay action tied to a house/territory rather than simply carrying a portal block.

The mod currently supports Fabric 26.2 and exposes a house block, permissions and a move-house action.

Useful ideas:

```text
house ownership
permissions
relocation UX
multiplayer access
```

### Immersive Portals

Reference only.

It demonstrates genuine see-through portals between dimensions and seamless dimensional transitions.

It is useful evidence for what would technically be possible, but that rendering complexity is specifically excluded from this project. Fake windows provide the useful part of the experience much more cheaply.

---

## Design principle

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
