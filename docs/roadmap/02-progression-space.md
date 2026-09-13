# Milestone 2: World-attuned progression and expandable pocket spaces

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


## Consolidated specification

_Source: progression specification sections governing shared progression foundations._

### Status and relationship to the MVP

This document specifies a post-MVP feature track for Portable Pocket Cabin.

The nine MVP deliveries in [Milestone 0](00-safe-mvp.md) remain the implementation foundation. Once progression work begins, this document deliberately supersedes the MVP's fixed 21×21 starting interior and its rule that every packed interior is completely paused.

The project is not yet released. Progression may replace MVP interior generation and persistence without providing a migration path.

Storage, automation and learned-item behavior are specified in [Cabin Storage and Automation](05-production-automation.md). Roles, shared hallways and packed-cabin access are specified in [Cabin Network and Access](07-connected-cabins.md).

### Goals

Progression should:

- begin with a genuinely small but usable home
- make every increase in general-purpose space valuable
- reward travel through varied biomes, structures and dimensions, with different material requirements in different world saves
- establish amethyst as the active magical medium of the cabin system
- make obsidian the material that anchors rooms and connections to the pocket dimension
- provide specialised rooms that players inhabit, operate and customise
- remove repetitive resource chores gradually rather than granting an instant all-purpose factory
- remain safe and bounded while cabins are packed or unoccupied

It should not turn ordinary blocks anywhere in the pocket dimension into always-running machinery.

### General interior size

New progression cabins begin with approximately 4×4 blocks of usable floor space, excluding their protected shell.

Each size upgrade increases both usable dimensions by one block:

```text
4×4 → 5×5 → 6×6 → 7×7 → …
```

The entrance wall and doorway remain anchored. Expansion extends the rear boundary and uses a deterministic lateral pattern so existing player blocks never move. An upgrade must validate its target volume before modifying the shell and must abort without partial changes if that volume is not safe.

Each successive expansion costs more than the previous one. Costs, the maximum general-room size and the exact lateral expansion pattern are data-driven balancing values, but a finite maximum must be declared before implementation so interior allocations cannot collide.

General-purpose space remains ordinary Minecraft space. Players can furnish it with normal and compatible modded blocks; those blocks do not gain packed-time simulation merely because the room has been enlarged.

### Pocket room graph

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

### Cabin magic and material language

Cabin progression uses two materials for different purposes:

- **Amethyst carries resonance.** Amethyst preserves learned patterns and performs the magical work behind cabin control, enchanting and automation. It is the primary recurring magical material when the cabin computes or transforms something.
- **Obsidian forms dimensional anchors.** Obsidian fixes entrances, rooms and hallway connections to the pocket dimension. It is primarily an installed structural cost when adding or strengthening dimensional capacity, not routine fuel for work the cabin performs.

This distinction guides default recipes without requiring every upgrade to contain both materials. A room expansion may need more anchoring, an automation or enchanting operation may consume resonance, and a complex new room may need both.

### World-attuned upgrade requirements

Upgrade requirements are data-driven. A requirement set may specify:

- exact items
- item tags or material categories
- quantities
- distinct biome or dimension material groups
- a reusable discovery catalyst from a structure or treasure source
- prerequisite room or cabin tiers

The default definitions must provide a vanilla progression. Optional integration profiles may replace or extend those definitions when compatible mods are installed. Servers and modpacks may override them with datapacks.

Each world save creates one persistent **world attunement** before its first cabin upgrade is purchased. The attunement resolves declared variable slots in upgrade requirements, such as:

- one specific wood family used by structural upgrades
- offerings associated with selected Overworld biome families
- materials associated with the Nether, End or supported modded exploration profiles

For example, one world may ask for spruce components and a desert offering where another asks for dark-oak components and a cold-biome offering. A resolved wood slot requires the selected family rather than accepting any item in the general planks or logs tag.

The same attunement applies to every cabin and player in the save. Its resolved material identifiers and definition version are persisted; restarts, seed reuse, configuration reloads and later mod or datapack changes must not silently reroll existing requirements. Deliberate administrative migration may replace invalid definitions, but it must be explicit and must report affected upgrades before committing.

Attunement pools may contain only declared, loaded materials with a valid acquisition profile for that world configuration. Optional integrations contribute candidates only while their required content and world generation are present. Every pool must have a vanilla fallback, and the selection process must fail closed rather than produce an impossible recipe.

All resolved requirements are visible through the recipe book or cabin progression interface from the beginning. Biome-associated requirements identify the broad environment to explore without revealing exact coordinates. World variation is intended to change exploration goals, not create hidden recipe guessing.

Structure-exclusive treasure should normally be a discovery catalyst rather than a repeatedly consumed ingredient. This preserves the exploration gate without turning finite structure loot into an ongoing multiplayer bottleneck.

The intended broad material arc is:

1. common Overworld wood, stone and agricultural materials
2. materials gathered from increasingly varied Overworld biomes
3. rarer structure discoveries, stronger amethyst resonance and obsidian anchors
4. Nether materials for stronger dimensional connections and magical infrastructure
5. End materials for the highest cabin and automation tiers

Exact recipes and quantities are balancing data rather than architectural rules.

### Optional content integrations

Optional integrations must fail closed and never prevent the base mod from loading when an integrated mod is absent.

#### Biomes O' Plenty

An integration profile may use biome-specific woods, plants, stones and other unusual materials in the intended modpack progression. Requirements remain data-driven and retain vanilla fallbacks outside that pack.

#### Alex's Mobs Continued

An explicit integration profile may provide:

- exploration catalysts tied to supported mobs, structures, advancements or loot
- unusual upgrade materials
- stable eligibility for declared rideable species
- livestock production profiles for declared sustainable species and drops

The cabin never infers that every entity is breedable, rideable or safe to manage. Each supported species needs an allowlisted profile describing the data that may be preserved and the actions the room supports.

#### Alex's Mobs Continued Delight

An explicit cooking integration may expose declared ingredients and meals to kitchen learning and automation through compatible Farmer's Delight or Farmer's Delight Refabricated recipes.

Unknown food remains storable as an ordinary item. It becomes reproducible only when its recipe and item components pass the safe-learning rules in the storage and automation specification.
