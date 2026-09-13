# Portable Pocket Cabin Roadmap

## Purpose

This roadmap turns the core specification and all post-MVP feature specifications into one implementation order. It stays deliberately high-level: each milestone defines a player-visible outcome, its major systems and an exit gate. Detailed recipes, behavior and safety rules remain authoritative in the linked specifications.

The project has not been publicly released. Development worlds are disposable until a milestone explicitly introduces a migration contract. Every milestone must work in a fresh world and survive a server restart before work advances.

## Ordering principles

1. Complete survival acquisition and command-free deployment and packing first.
2. Establish persistent data models before building features that depend on them.
3. Make rooms useful manually before adding automation.
4. Make one cabin safe and understandable before connecting multiple cabins.
5. Add cross-cabin resource movement only after local storage and network permissions are proven independently.
6. Keep balancing values data-driven and settle them through survival playtests at the milestone that consumes them.

## Milestone overview

| Milestone | Outcome | Status |
| --- | --- | --- |
| 0 | Safe portable-cabin MVP | Complete |
| 1 | Survival crafting and command-free relocation | Implemented; manual acceptance pending |
| 2 | World-attuned progression and expandable pocket spaces | Planned |
| 3 | Household roles, mailbox and central storage | Planned |
| 4 | Manual functional rooms, mounts and house cats | Planned |
| 5 | Targeted production and room automation | Planned |
| 6 | Enchanting, equipment requisitions and owner loadouts | Planned |
| 7 | Connected cabins and safe packed-cabin access | Planned |
| 8 | Cooperative knowledge, mail and resource logistics | Planned |
| 9 | Compatibility, balance and presentation pass | Planned |

## Milestone 0: Safe portable-cabin MVP

**Outcome:** One UUID-backed cabin can be deployed, entered, simulated, packed, recovered and redeployed without moving or losing its persistent interior.

This completed foundation includes lifecycle journalling and reconciliation, one active exterior, protected structure masks, safe destinations, occupant evacuation, stale-item generations, trust-based entry, offline recovery, cabin-bed respawning, vanilla-dimension support, fake-window states and ordinary block/mod compatibility inside the pocket dimension.

The regression suite for these guarantees remains mandatory for every later milestone.

## Milestone 1: Survival crafting and command-free relocation

**Depends on:** Milestone 0.

**Outcome:** A survival player crafts a palette-aware Cabin Kit through the complete dimensional-component chain, deploys it by using terrain twice and packs it by sneak-using the exterior controller twice. No ordinary lifecycle command is needed.

Major scope:

- Dimensional Logic Core, Dimensional Anchor, Dimensional Folding Core and Dimensional Foundation recipes
- the nine-slot Cabin Kit recipe and its six-Block-of-Amethyst commitment
- vanilla-style staged recipe-book discovery from Amethyst Block to cores, Foundation and Cabin Kit
- persisted floor, wall/frame, roof/ceiling and exact door selections
- all vanilla material profiles, bundled Biomes O' Plenty profiles and the versioned datapack profile extension point
- palette-driven wooden exterior and interior shell, protected portal doors and fireproof cabin-owned wood
- top-surface placement, player-facing orientation, exact preview confirmation and lava rejection
- controller-driven packing while preserving the existing transactional countdown and recovery rules
- operator-only lifecycle commands, player tooltips and first-use teaching messages
- versioned, fail-fast fresh-world-only rollout with no stone-cabin migration

Delivery order inside the milestone:

1. Crafting components, recipe discovery, versioned material profiles and the palette codec used by the Kit and registry.
2. Authoritative first binding, crash-safe item obligations, item-driven preview/deployment and controller-driven packing on the existing lifecycle services.
3. Palette-driven structure generation and protected portal-door behavior.
4. Vanilla and Biomes O' Plenty compatibility, survival balance and complete regression acceptance.

**Red:** Add failing recipe, palette, interaction, rotation, lava, fire, stale-item, restart and packing-concurrency GameTests before each slice.

**Green:** Implement only enough of each slice to make those player flows and inherited safety guarantees pass.

**Refactor:** Remove player-facing command coupling, consolidate material-profile lookup and ensure item/controller entry points share the lifecycle services used by operator recovery commands.

**Exit gate:** Every acceptance criterion in [Cabin Acquisition and Relocation](docs/specs/cabin-acquisition-and-relocation.md) passes in a fresh survival world and after restart.

## Milestone 2: World-attuned progression and expandable pocket spaces

**Depends on:** Milestone 1 palette persistence and structure generation.

**Outcome:** A new cabin starts as a genuinely small 4×4 home and can grow through visible, world-specific upgrade requirements without moving player blocks or risking another cabin's space.

Major scope:

- replace the MVP 21×21 starting interior with the 4×4 progression shell
- deterministic one-block-at-a-time general-space expansion with a declared finite maximum
- permanent room/cell graph and protected internal teleport doors
- one persisted world attunement shared by every cabin in the save
- visible vanilla upgrade requirements with optional Biomes O' Plenty exploration pools and datapack overrides
- explicit administrative validation/migration for invalid attunement definitions
- amethyst resonance and obsidian anchoring as the common progression material language
- the controller-facing upgrade and status foundation used by later milestones

**Red:** Add failing tests for attunement stability, impossible pools, allocation collisions, obstructed expansions, restart persistence and cross-cabin isolation.

**Green:** Implement the smallest upgrade path that grows one cabin safely and allocates one disconnected room cell.

**Refactor:** Separate balancing definitions from persisted resolutions and centralise room-allocation and protected-door traversal services.

**Exit gate:** Two players can receive the same world attunement, independently expand their cabins to the configured limit and restart without rerolls, overlap or moved blocks.

## Milestone 3: Household roles, mailbox and central storage

**Depends on:** Milestone 2's controller and room identity model.

**Outcome:** A cabin behaves as a household with clear owner, resident and guest boundaries, a safe public receiving mailbox and one authoritative cabin-owned inventory.

Major scope:

- local owner, resident and guest roles with destination-specific permission checks
- the bounded exterior receiving mailbox with insert-only public access and owner collection
- atomic, slot-based central cabin storage with explicit protected interfaces
- role-aware deposit, withdrawal, facility use and configuration permissions
- storage capacity upgrades, hard reserves and transaction-safe mutations
- local status visibility that reveals only information appropriate to each role
- the first storage/status interface, while ordinary chests and Tom's Simple Storage remain independent

**Red:** Add failing permission-matrix, mailbox privacy/full-capacity, concurrent mutation, restart and rollback tests.

**Green:** Implement one local household with usable mailbox and central storage, without networking or automation.

**Refactor:** Route all cabin-owned inventory changes through one transaction boundary and all UI/status checks through the same role policy.

**Exit gate:** Owners and residents can use shared storage, guests can safely deliver mail but cannot inspect resources, and interrupted transfers neither duplicate nor lose items.

## Milestone 4: Manual functional rooms, mounts and house cats

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

## Milestone 5: Targeted production and room automation

**Depends on:** Milestones 3 and 4.

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

## Milestone 6: Enchanting, equipment requisitions and owner loadouts

**Depends on:** Milestone 5's knowledge, job and storage systems.

**Outcome:** The cabin can learn enchantments destructively, reproduce valid equipment using materials instead of experience and maintain explicit Factorio-style owner loadouts.

Major scope:

- capacity-limited enchantment library with one selected extraction per destroyed source item
- level improvement, pending shared discoveries and vanilla/modded applicability rules
- amethyst-and-lapis enchanting costs with obsidian as installed infrastructure rather than fuel
- exact equipment requisitions with full cost preview
- named owner loadout groups, effective min/max rules and optional automated production
- protected slots, durability/enchantment requirements and the advanced deposit-unlisted control
- Take for this trip, Take back and entry transaction summaries

**Red:** Add failing destructive-extraction, capacity, incompatibility, cost, protected-item, competing-group and temporary-exception tests.

**Green:** Build manual enchantment learning and requisition first, then add entry-triggered loadout exchange.

**Refactor:** Use canonical safe templates and the bounded-job engine for both requisitions and restocking.

**Exit gate:** Loadouts never alter unspecified or unsafe items, enchanting never consumes experience, and every planned material cost is visible before commitment.

## Milestone 7: Connected cabins and safe packed-cabin access

**Depends on:** Stable local permissions from Milestone 3 and lifecycle regression coverage from Milestone 1.

**Outcome:** Mutually consenting owners connect cabins through a persistent shared hallway, and a packed cabin remains reachable when the player still has a permitted route to another deployed exterior.

Major scope:

- connection upgrade, mutual consent and persistent hallway membership graph
- separately allocated shared hallways with labelled protected cabin doors
- ordinary, non-cabin-owned hallway furnishing behavior
- permission rechecks at every destination doorway
- hallway access to packed cabins without permanently loading empty rooms
- packing initiated from the protected interior interface
- graph-locked, per-occupant reachability checks and network-aware evacuation
- hallway mailbox projection backed by the same logical inbox
- explicit safe membership removal, doorway sealing and administrative recovery

**Red:** Add failing consent, permission-revocation, simultaneous-pack, last-exit, offline-login, stale-network-generation and hallway-removal tests.

**Green:** Connect two cabins first, then permit one to pack only when every affected player retains a safe route or can be evacuated.

**Refactor:** Centralise graph locking and route validation so exterior entry, interior packing and login recovery cannot disagree.

**Exit gate:** Concurrent packing cannot strand a player, expose an unauthorised destination, remove another cabin's doorway or delete hallway furnishings.

## Milestone 8: Cooperative knowledge, mail and resource logistics

**Depends on:** Milestones 5 through 7.

**Outcome:** Connected cabins cooperate without becoming one inventory or leaking private household configuration.

Major scope:

- permanent propagation of installed automation discoveries and learned enchantments
- cabin-local ordinary item, meal and potion knowledge
- owner-published requests and surplus offers
- reserve-aware, attributable and atomic inter-cabin storage transfers
- mailbox fulfilment and surplus-delivery automation
- cabin-specific network status and visibility
- leaving a network without revoking already copied shared discoveries

**Red:** Add failing privacy, reserve, partial-transfer, disconnect, capacity, discovery-propagation and attribution tests.

**Green:** Move one explicitly requested stack between two cabins transactionally before enabling automated fulfilment.

**Refactor:** Keep all remote transfers request-based; no automation may read or consume donor storage directly.

**Exit gate:** Every shared item changes ownership atomically and visibly, every permission is evaluated at the destination cabin, and no combined network inventory exists.

## Milestone 9: Compatibility, balance and presentation pass

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

## Specification coverage

| Source | Roadmap coverage |
| --- | --- |
| [`SPEC.md`](SPEC.md) | Milestone 0 preserves the implemented core; its later possibilities are tracked below. |
| [Cabin Acquisition and Relocation](docs/specs/cabin-acquisition-and-relocation.md) | Milestone 1. |
| [Cabin Progression and Functional Rooms](docs/specs/cabin-progression-and-rooms.md) | Milestones 2, 4 and 5; balancing and integrations finish in Milestone 9. |
| [Cabin Storage and Automation](docs/specs/cabin-storage-and-automation.md) | Milestones 3, 5, 6 and 8; final balancing in Milestone 9. |
| [Cabin Network and Access](docs/specs/cabin-network-and-access.md) | Local roles and mailbox begin in Milestone 3; connections and network packing land in Milestone 7; shared logistics land in Milestone 8. |

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
