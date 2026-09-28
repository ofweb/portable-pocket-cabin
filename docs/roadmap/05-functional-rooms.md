# Milestone 5: Functional rooms

**Depends on:** [PDR-0007](../../.workflow/decisions/pdr/0007-fund-and-install-cabin-upgrades.md) installation, [PDR-0009](../../.workflow/decisions/pdr/0009-use-fixed-household-roles.md) roles, and [central storage](../../.workflow/features/B-0004/brief.md).

**Outcome:** Players purchase, enter and manually operate useful specialised rooms that remain bounded, persistent and isolated from every other pocket space.

**Status:** Draft. Each delivery requires alignment before implementation.

## Scope

This milestone establishes the room model and delivers manual versions of:

- a greenhouse with managed growing beds
- a stable for individual eligible mounts
- an aquatic stable berth for a tamed nautilus
- a livestock room with exact founders and bounded offspring
- a forestry room with managed tree plots

Rooms may apply bounded passive catch-up while inactive. Actions that require a player never happen automatically. [Milestone 7](07-production-automation.md) owns automatic feeding, collection, harvesting, slaughtering, felling, replanting and processing.

## Deliveries

| Delivery | Result |
| --- | --- |
| 5.1 | Purchase one empty room, allocate its isolated cell and traverse its protected internal door after restart. |
| 5.2 | Manually plant and harvest one greenhouse's managed beds. |
| 5.3 | Check eligible mounts into and out of one stable without changing or duplicating identity. |
| 5.4 | House and release an eligible tamed nautilus through one aquatic berth. |
| 5.5 | Manually tend a bounded population of one supported livestock species. |
| 5.6 | Manually fell and replant one managed forestry plot. |

Each delivery must remain independently playable and restart-safe.

## Room installation and traversal

A cabin is a graph of persistent bounded spaces, not one indefinitely expanding cell.

- the general interior retains its existing cell
- every purchased room receives a separate permanent cell and room identifier
- unpurchased rooms allocate nothing
- room and cell identifiers are never reused while their contents may remain recoverable
- each room type declares its maximum footprint and required active chunks
- protected internal doors use short same-dimension teleports
- traversal never exposes pocket coordinates, the void or another cabin's space

Separate cells let rooms expand within their declared bounds without moving neighbouring spaces or reserving every possible room for each cabin. [Milestone 9](09-connected-cabins.md) owns hallway allocation and packed-cabin traversal.

## Managed-room lifecycle

Each functional room combines a physical room with an authoritative persisted controller. The controller owns only declared fixtures, managed positions and managed residents. Those elements are protected from pistons, explosions and unauthorized mutation; the remaining space accepts ordinary player construction.

When a room becomes active, the server:

1. computes passive catch-up since the last persisted timestamp, capped by configured duration
2. stops catch-up at any input, feed, seed, output, storage or population limit
3. projects authoritative fixtures and managed residents into the room
4. runs ordinary active-room updates while its cell remains loaded
5. persists state and a new timestamp when the room becomes inactive

The controller, not a displayed crop block or entity, decides whether an output was collected. State-changing interactions are server-authoritative and atomic so concurrent visitors cannot collect the same result twice.

Catch-up never ticks arbitrary player blocks, furnaces or modded machines. It may advance declared passive growth, maturation, breeding or production, but it cannot plant, harvest, feed, slaughter, fell, replant or process without the automation delivered by Milestone 7.

[PDR-0009](../../.workflow/decisions/pdr/0009-use-fixed-household-roles.md) roles apply:

- owners and residents may operate shared room facilities
- guests may inspect but cannot mutate managed fixtures, harvest products or remove animals
- failures expose only the status detail allowed for the viewer's role

## Greenhouse

The greenhouse is purchased separately from general space. Its initial room contains protected growing beds and supporting fixtures but no crops.

- owners and residents supply, plant and harvest supported crops manually
- supported crops may mature through bounded passive catch-up
- mature crops remain unharvested until collected
- unsupported crops behave as ordinary blocks and pause while the room is inactive

Later room upgrades may add beds, usable space, supported crop categories and local output capacity. Automatic harvesting and replanting remain separate Milestone 7 capabilities and never create missing seeds.

## Stable

The stable houses exact individual mounts rather than converting them into a capacity count. Each supported mount type requires an allowlisted profile for identity and state preservation.

### Check-in

Check-in uses a protected stable control while the cabin is deployed. It accepts one nearby mount that is:

- eligible under a loaded profile
- owned by the acting player
- dismounted
- outside near the current cabin exterior
- not already managed by another cabin or system

The resulting stable record preserves the mount's global entity UUID and allowlisted state, including type, owner, health, custom name, equipment, inventory and declared mod-specific data. A recoverable transition creates the record and removes the exterior entity exactly once.

The stable record is authoritative while the mount is checked in. An active room displays a protected projection with the same UUID and state. Reconciliation removes duplicate projections before completing the recorded state. Packing, carrying, restart and owner logout never remove, copy or release the mount.

### Release

Release is an explicit action at the stable control:

1. Require the cabin to have a deployed exterior.
2. Allow a resident to release their own mount and the cabin owner to release any stable resident.
3. Find a bounded, collision-free and hazard-free egress position beside the exterior.
4. Persist a pending release before removing the interior projection.
5. Materialize the same entity UUID and state outside.
6. Remove the stable record only after successful materialization.

Failure leaves the mount checked in and reports the reason. Startup and live reconciliation complete the pending transition without producing both a record and an independent entity. A packed cabin cannot release mounts and explains that it must be redeployed first.

Guests and unrelated residents cannot release a mount. Stable upgrades may add managed stalls or explicitly supported mount categories.

### Aquatic berth

An aquatic berth is a protected, fully flooded stable upgrade for a normal tamed nautilus. Wild and zombie nautiluses are ineligible.

Check-in requires a dismounted nautilus owned by the acting player and preserves its UUID, owner, health, custom name, saddle, nautilus armour and other allowlisted vanilla state. The active projection has enough water to prevent suffocation.

Release uses the stable transaction but requires sufficient connected water, collision-free space and no immediate environmental hazard. Dry terrain, shallow decorative water and obstructed volumes fail without changing the record. The berth never breeds nautiluses, generates shells or treats the animal as livestock.

## Livestock room

Each livestock room supports one explicitly profiled species. Four eligible animals establish its breeding population.

The controller preserves every founder's UUID and allowlisted state. Founders remain exact managed residents, never enter the offspring count and are never selected for slaughter. Only the cabin owner may dismantle the population; recovery requires a deployed exterior and safe egress for every founder.

Later offspring are bounded aggregate counts. Each species profile declares:

- eligible founders and preserved data
- acceptable feed
- passive products and cycles
- population and surplus limits
- slaughter products
- active-room projection

Manual operation follows these rules:

- players place feed in the trough
- a fed population breeds and produces only within declared limits
- missing feed pauses production without starvation or death
- passive products accumulate in local fixtures up to their capacity
- owners and residents collect products manually
- surplus offspring remain a managed count
- a player deliberately uses the butchery fixture to convert eligible surplus into declared drops

Residents may tend the population and process surplus but cannot dismantle founders or change cabin-wide production rules.

## Forestry room

The forestry room grows supported trees after players supply each sapling variety.

- supported trees may mature through bounded passive catch-up
- owners and residents fell and replant them manually
- unsupported trees behave as ordinary blocks
- upgrades may add plots, room space, supported varieties and local output capacity

Automatic felling, replanting, kiln processing and coal synthesis belong to Milestone 7. Replanting and processing must consume their declared inputs when delivered.

## Status and compatibility

Room status must distinguish:

- missing or unsupported seed, sapling, animal or mount
- missing feed or process input
- full local fixture or central storage
- population below a required surplus threshold
- catch-up duration exhausted
- insufficient room tier or capacity
- absent or incompatible integration profile
- packed cabin or unsafe release destination

The cabin never infers that every entity is breedable, rideable or safe to manage. Optional content requires an explicit profile defining preserved data, supported actions and failure behavior. Missing optional mods or profiles disable only their content and never prevent the base mod from loading.

An Alex's Mobs Continued profile may add declared exploration catalysts, upgrade materials, stable eligibility and sustainable livestock species or drops.

## Technical approach

The cabin registry owns room identities, cell allocations, controller state, timestamps and managed-resident records. Physical fixtures, crops and entities are validated projections of that state. Room-specific services apply bounded catch-up and world mutations; shared traversal and destination services enforce cell isolation and safe egress.

Check-in, release, founder recovery and any other identity transfer use typed persisted transitions. Each transition records enough source, destination and entity identity to reconcile interruption without duplication or loss. Room activation locks or serializes controller mutations before projecting state.

## Evergreen acceptance contract

Milestone 5 remains accepted only while automated tests and targeted manual checks establish that:

1. Purchased rooms receive permanent isolated cells; unpurchased rooms allocate nothing.
2. Internal traversal survives restart without exposing the void, coordinates or unrelated cells.
3. Catch-up is bounded, stops at every declared limit and never ticks ordinary player blocks or performs manual actions.
4. Role checks prevent guests and unauthorized players from mutating fixtures or collecting managed output.
5. Greenhouse crops mature passively but require manual planting and harvesting.
6. Stable check-in and release preserve exact UUID and allowlisted state across restart, packing and interrupted transitions.
7. Stable reconciliation never leaves an authoritative record and an independent copy of the same mount.
8. Aquatic release succeeds only at a safe connected-water destination.
9. Livestock preserves exact founders, bounds offspring and products, and recovers founders safely on owner-confirmed dismantling.
10. Forestry matures only supported trees and requires manual felling and replanting.
11. Missing optional profiles fail locally with actionable status and do not prevent base startup.
12. Separate room cells and active chunk bounds cannot overlap another cabin or room.

Codec, controller-service, traversal, identity-transition, GameTest and dedicated-server restart suites are the automated gates. Manual acceptance covers room presentation, animal behavior, destination safety and multiplayer interaction.

## Out of scope

- automatic room actions, processing jobs and central-storage transfers
- connection hallways and packed-cabin traversal
- house cats and other general-interior companions
- inferred support for arbitrary crops, trees, mounts or livestock
- arbitrary player-block ticking during inactive time
- mount release while packed or without a safe exterior destination
- exact room geometry, costs, capacities and optional profile contents before delivery alignment
