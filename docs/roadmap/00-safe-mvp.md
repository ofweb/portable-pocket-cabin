# Milestone 0: Safe portable-cabin MVP

**Outcome:** A UUID-backed cabin can be deployed, entered, simulated, packed, recovered and redeployed without moving or losing its persistent interior.

**Status:** Complete. Its lifecycle and data-safety guarantees remain regression requirements for every later milestone.

## Scope and authority

This document defines the enduring foundation beneath later cabin features. It is not a guide to current crafting, geometry or upgrade interactions; the [README](../../README.md) describes current play, and later milestone documents own the behavior they replace.

Milestone 0 established:

- permanent cabin identity and isolated interior allocation
- one authoritative, crash-recoverable lifecycle
- one active exterior per cabin
- safe cross-dimensional entry, exit, evacuation and respawning
- protected exterior and interior shells
- deployed-only simulation for ordinary interior blocks
- stale-item protection, offline-player recovery and operator diagnostics
- normal vanilla block and block-entity behavior inside the pocket dimension

The cabin carries a small home between campsites, not the surrounding settlement. Exterior walls, paths, farms, mines, docks and other local construction remain tied to their location. Travel moves the entrance; it never serializes or moves the interior.

## Persistent model

### Cabin identity

Every cabin has a permanent UUID. The authoritative registry associates it with:

- one owner and access settings
- one permanent, monotonically allocated interior cell
- the current lifecycle state
- the current and last valid dimension-qualified exterior locations
- the packed-item generation
- the persistent Cabin palette and later milestone state

The registry, not an item or exterior block, decides whether and where a cabin is deployed. A packed item contains identity and presentation data but never contains the interior. Copying or losing an item therefore cannot copy or erase the home.

Each current packed item has a generation. Recovery increments that generation, making every older copy stale. A stale or duplicated item must never create another active entrance or alter registry-owned cabin data.

Cabin UUIDs and cell indices are never reused, including after a cabin becomes orphaned. The baseline ownership model permits many cabins in a world, exactly one owner per cabin and at most one owned cabin per player.

### Lifecycle and reconciliation

Every cabin has exactly one persisted lifecycle state:

```text
PACKED
DEPLOYING
DEPLOYED
PACKING
ORPHANED
```

Deployment and packing are journalled, idempotent registry transitions. Items, structures, entrances and chunk tickets are projections of that state. Operations must persist enough intent to finish or roll back after a crash.

Startup and chunk-load reconciliation must:

- finish or safely reverse interrupted transitions
- remove or disable stale entrances
- restore a recoverable packed representation when necessary
- repair physical projections without replacing authoritative registry data
- preserve the one-active-entrance invariant

A crash may defer physical cleanup. It must not make the interior unrecoverable, expose two usable entrances or silently replace a valid cabin record.

## Interior and simulation

All cabins occupy isolated cells in one otherwise inaccessible pocket-home dimension. Allocations must leave enough separation for the maximum supported cabin geometry, simulated chunks and an isolation buffer. Players must not be able to escape through the protected shell or reach another cell through ordinary play.

The cell is permanent, so packing changes access and simulation rather than relocating blocks. Ordinary blocks and block entities remain in the world and use normal Minecraft persistence.

While a cabin is deployed, bounded tickets keep only its required interior chunks fully simulated. Packing removes those tickets, pausing crops, furnaces and other ordinary ticking blocks until redeployment. Later managed-room systems may define bounded packed-time progress for their own fixtures; they must not turn arbitrary interior blocks into always-running machinery.

[Milestone 2](02-progression-space.md) replaces the original fixed 21×21 interior with the current expandable 4×4 starting geometry. Geometry changes must retain permanent cell isolation and must not move player blocks.

## Exterior and deployment

A cabin may have only one deployed exterior across all dimensions. The supported baseline dimensions are the Overworld, Nether and End; unsupported dimensions fail closed.

Deployment must validate, before committing:

- the cabin, item generation, owner and lifecycle
- sufficient free space for the exact cabin-owned structure mask
- a safe standing area outside the door
- the selected dimension, position and orientation

The exterior is a protected multiblock with one controller and an exact ownership mask. Ordinary players, explosions and pistons cannot alter owned blocks. Packing removes only that mask, never an enclosing volume or nearby player construction. Deployment may occur underground when the player has excavated the required space; it validates terrain but never clears it.

[Milestone 1](01-acquisition-relocation.md) owns the current palette-aware recipe, first binding, two-use placement preview, orientation and command-free packing interaction. Those changes retain the lifecycle and protection rules above.

## Safe destinations

Placement, ordinary exit, packing evacuation, offline recovery and respawning share one resolver. A valid destination has:

- a solid floor and enough collision-free player space
- no dangerous fluid or fire
- a position inside the world border
- a destination chunk loadable under a bounded temporary ticket

The resolver loads a candidate chunk before checking collision and hazards, searches within a fixed radius and releases temporary tickets after success, failure or timeout.

Emergency resolution uses this order:

1. the current exterior doorway
2. a nearby safe position in the current exterior dimension
3. the last valid campsite and nearby positions
4. Overworld world spawn

Voluntary packing must abort before entering `PACKING` if every current occupant cannot be evacuated safely. Unexpected exterior loss may use the full emergency fallback chain.

## Entry, access and packing

The exterior door or controller sends an authorized player to the cabin's interior entrance. The interior door returns the player to a safe position outside the current exterior. A normal dimension-loading screen is acceptable; rendered cross-dimensional portals remain out of scope.

The owner controls deployment and packing. Trusted players may enter when the cabin's access mode permits it. Permission and lifecycle are rechecked when an entry completes so a concurrent access change or packing operation cannot admit a player to a disabled cabin.

Packing follows one transaction:

1. Validate ownership, lifecycle, proximity, exterior integrity, occupant destinations and owner inventory capacity.
2. Persist `PACKING` and the item-delivery obligation, then place a pending next-generation item in the reserved slot.
3. Disable entry and begin the five-second occupant countdown.
4. Re-identify online occupants by interior cell immediately before evacuation.
5. Move every occupant to a safe destination.
6. Persist `PACKED`, remove only the protected exterior mask and activate the pending item.

If the owner disconnects or delivery cannot be guaranteed, packing aborts and reconciliation restores `DEPLOYED`. New entry remains blocked throughout a packing countdown. The interior itself is never removed or rewritten.

The baseline makes an inactive cabin inaccessible through ordinary play. [Milestone 9](09-connected-cabins.md) may deliberately replace that rule for cabins reachable through an authorized shared hallway; its network-aware packing must preserve safe evacuation and the one-active-exterior invariant.

## Offline occupants

Logging out inside a cabin must not prevent its owner from packing or moving it. Packing does not edit offline player files. Pocket coordinates map unambiguously to a permanent cabin cell, and the occupancy record captures the cabin identity and item generation.

On login, a player whose recorded cabin route is no longer valid is resolved from that cell and returned through the safe-destination chain before play resumes. Later connected-cabin access may retain the player only when a permitted route to a deployed exterior still exists.

## Cabin-home respawning

Portable Pocket Cabin owns its respawn behavior and has no Better Respawn dependency. Successful sleep binds the cabin owner to that cabin UUID and bed position; the most recently used cabin bed replaces the previous cabin-home binding. In the baseline, a trusted visitor may sleep without changing their existing home.

Respawning uses this order:

1. If the cabin is `DEPLOYED` and the bound bed has a safe adjacent position, respawn there.
2. If the cabin is deployed but the bed is missing or blocked, resolve a position outside its current doorway.
3. If the cabin is packed or orphaned, search the death dimension 128–256 horizontal blocks from the death position by default.
4. If that bounded search fails, resolve from the last valid campsite.
5. If no cabin destination works, use Overworld world spawn.

The near-death bounds are server-configurable. The search considers at most 16 candidate regions within a bounded time budget, applies the shared safety rules and proceeds immediately to fallback on exhaustion or timeout. It may run in the Overworld, Nether or End but never select a pocket-dimension destination.

Lifecycle and destination safety are revalidated immediately before respawn so concurrent packing cannot return a player to an inactive cabin.

## Exterior-condition windows

Cabin windows use lightweight state projection, not rendered portals. Their signal is derived from the cabin's dimension-qualified exterior:

- dawn, day, sunset, night, rain and thunder in the Overworld
- static Nether and End profiles
- an opaque inactive state while packed or orphaned

[Milestone 3](03-upgrade-interface.md) replaces Milestone 0's automatic two-window layout with individually purchased, reversible windows while preserving these state signals. [Milestone 11](11-compatibility-polish.md) owns higher-fidelity presentation and optional-mod validation.

## Damage, recovery and data safety

Unexpected exterior loss must never delete or rewrite the interior. If the controller or structure disappears through commands, world modification or a protection failure, reconciliation transitions the cabin to a recoverable orphaned or packed state.

Operators have explicit diagnostics and recovery tools:

```text
/cabin list
/cabin inspect <uuid>
/cabin reconcile <uuid>
/cabin recover-item <uuid> <player>
```

`reconcile` idempotently repairs projections to match the registry. `recover-item` refuses while a valid deployed exterior exists; otherwise it reconciles the cabin to `PACKED`, increments the item generation and issues the owner a new bound item.

The non-negotiable data rules are:

1. Never delete an interior because its exterior disappeared.
2. Never reuse a cabin UUID or cell index.
3. Never expose two active exterior entrances for one cabin.
4. Persist lifecycle intent before replacing or removing an exterior.
5. Preserve enough registry data to recover every orphaned cabin administratively.

## Compatibility boundary

General interior space supports ordinary vanilla blocks, inventories, beds, water, farmland, crops and block entities. Packing and redeployment must preserve them without a custom storage backend. The vanilla regression fixture contains a named-item chest, double chest, furnace, bed, water, farmland and growing crops.

Optional storage and automation integrations do not belong to this milestone. Their systems must extend ordinary world persistence rather than replace it; [Milestone 7](07-production-automation.md) owns cabin automation, and [Milestone 11](11-compatibility-polish.md) owns the optional-mod compatibility matrix.

## Evergreen acceptance contract

Milestone 0 remains accepted only while automated tests and targeted manual checks establish that:

1. UUIDs, owners, cell indices, palettes, locations and lifecycle states survive restart; allocation remains monotonic.
2. Repeated or concurrent deployment and packing requests produce one transition, one exterior and one current packed item.
3. Crashes at each lifecycle boundary reconcile to one usable `DEPLOYED` or `PACKED` projection without losing the interior.
4. Exact-mask placement, protection and removal leave adjacent player blocks unchanged in every rotation.
5. Entry, exit and evacuation use safe loaded destinations in the Overworld, Nether and End, including last-campsite and world-spawn fallbacks.
6. Trusted access is enforced and rechecked; entry stays disabled for the full packing countdown.
7. Online occupants are evacuated, while offline occupants recover safely on login after packing or relocation.
8. Deployed interiors retain bounded simulation tickets; packed interiors do not. Vanilla fixtures persist across packing, restart and redeployment.
9. Cabin-home respawning handles a valid bed, missing or blocked bed, inactive cabin, bounded-search timeout and death-screen packing race.
10. Exterior loss and item recovery cannot activate stale items, duplicate entrances or make the interior unreachable.
11. Every supported exterior condition produces the correct window state, and inactive cabins show no live exterior signal.
12. A dedicated server can start, reload the saved registry and complete the lifecycle regression without client-only dependencies.

The build and dedicated-server restart suite are the normal automated gate. Manual testing is reserved for client interaction, presentation and multiplayer timing that the suite cannot establish reliably.

## Implementation history

Milestone 0 was delivered in nine restart-safe slices. This table preserves traceability without treating obsolete development commands or geometry as current instructions.

| Delivery | Foundation added |
| --- | --- |
| 1 | Fabric project, pocket dimension, GameTest world and dedicated-server startup |
| 2 | Persistent registry, cabin UUIDs, lifecycle states and monotonic cell allocation |
| 3 | Protected exterior, persistent interior, placement, entry and exit |
| 4 | Packing, evacuation, bound items and crash reconciliation |
| 5 | Trusted access, offline recovery and concurrent-operation safety |
| 6 | Bounded interior simulation and vanilla fixture persistence |
| 7 | Cabin-home binding and bounded near-death respawning |
| 8 | Nether and End lifecycle support and exterior-condition signals |
| 9 | Survival acquisition, operator recovery workflow and complete regression gate |

Later milestones supersede only the behavior they name. They must retain the identity, lifecycle, recovery and data-safety guarantees in this document.
