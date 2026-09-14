# Milestone 2: World-attuned expansion

**Depends on:** Milestone 1 palette persistence and structure generation.

**Outcome:** A new cabin starts as a genuinely small 4×4 home and can grow through visible, world-specific upgrade requirements without moving player blocks or risking another cabin's space.

**Status:** Implemented; retrospective acceptance audit pending.

## Deliveries

### Delivery 2.1: Small progression interior — Implemented

Replace the MVP's 21×21 starting interior with a palette-aware 4×4 usable shell whose entrance wall remains anchored.

### Delivery 2.2: World attunement — Implemented

Resolve one persisted material attunement per world save, shared by every player and stable across restart and configuration reloads. Invalid saved attunements fail closed rather than silently rerolling.

### Delivery 2.3: General-space expansion — Implemented

Show the next expansion's requirements at the protected interior controller and grow the general interior exactly one block per dimension without moving player blocks or consuming materials after failed validation.

The normal-use status message and sneak-use purchase gesture are provisional. [Milestone 3](03-upgrade-interface.md) replaces them with the aligned upgrade interface.

**Exit gate:** Two players can receive the same world attunement, independently expand their cabins to the configured limit and restart without rerolls, overlap or moved blocks.


## Consolidated specification

_Source: progression specification sections governing shared progression foundations._

### Status and relationship to the MVP

This document specifies a post-MVP feature track for Portable Pocket Cabin.

The nine MVP deliveries in [Milestone 0](00-safe-mvp.md) remain the implementation foundation. This milestone deliberately supersedes the MVP's fixed 21×21 starting interior. Functional-room milestones may later supersede packed-time behavior for their own managed fixtures.

The project is not yet released. Progression may replace MVP interior generation and persistence without providing a migration path.

Storage, automation and learned-item behavior are specified in [Cabin Storage and Automation](07-production-automation.md). Roles, shared hallways and packed-cabin access are specified in [Cabin Network and Access](09-connected-cabins.md).

### Goals

Progression should:

- begin with a genuinely small but usable home
- make every increase in general-purpose space valuable
- reward travel through varied biomes, structures and dimensions, with different material requirements in different world saves
- establish amethyst as the active magical medium of the cabin system
- make obsidian the material that anchors dimensional expansion
- remain safe and bounded while cabins are packed or unoccupied

It should not turn ordinary blocks anywhere in the pocket dimension into always-running machinery.

### General interior size

New progression cabins begin with approximately 4×4 blocks of usable floor space, excluding their protected shell.

Each size upgrade increases both usable dimensions by one block:

```text
4×4 → 5×5 → 6×6 → 7×7 → …
```

The clear interior height grows with the general-space size. A 4×4 or 5×5 cabin has two
air blocks between its floor and ceiling. Every second size step adds one block of clear
height, capped at ten blocks from size 20 onward:

```text
4–5 → 2, 6–7 → 3, …, 18–19 → 9, 20+ → 10
```

Height is derived only from the persisted general-space size, not from the datapack's
configured maximum, so reloading progression definitions cannot reshape an existing cabin.
An expansion that raises the ceiling validates the newly exposed vertical volume together
with the horizontal extension and fails without consuming materials when either is obstructed.
Existing player blocks inside the unchanged usable volume remain untouched.

The entrance wall and doorway remain anchored. Expansion extends the rear boundary and uses a deterministic lateral pattern so existing player blocks never move. An upgrade must validate its target volume before modifying the shell and must abort without partial changes if that volume is not safe.

Each successive expansion costs more than the previous one. Costs, the maximum general-room size and the exact lateral expansion pattern are data-driven balancing values, but a finite maximum must be declared before implementation so interior allocations cannot collide.

General-purpose space remains ordinary Minecraft space. Players can furnish it with normal and compatible modded blocks; those blocks do not gain packed-time simulation merely because the room has been enlarged.

This geometry change is fresh-world-only. Registries created with the earlier fixed-height
interior schema fail closed with the existing backup-and-`just fresh-world` guidance; the mod
does not guess which old ceiling-area blocks are generated shell and which are player-built.

### Cabin magic and material language

Cabin progression uses two materials for different purposes:

- **Amethyst carries resonance.** Amethyst preserves learned patterns and performs the magical work behind cabin control, enchanting and automation. It is the primary recurring magical material when the cabin computes or transforms something.
- **Obsidian forms dimensional anchors.** Obsidian fixes entrances, rooms and hallway connections to the pocket dimension. It is primarily an installed structural cost when adding or strengthening dimensional capacity, not routine fuel for work the cabin performs.

This distinction guides default recipes without requiring every upgrade to contain both materials. A room expansion may need more anchoring, an automation or enchanting operation may consume resonance, and a complex new room may need both.

### World-attuned upgrade requirements

Upgrade requirements are data-driven. A requirement set may specify:

- exact items
- quantities
- the world's attuned planks selection

The version 1 definitions provide a vanilla-compatible expansion ladder using exact items and one attuned-planks slot. Servers and modpacks may override that ladder with datapacks using the same schema.

Each world save creates one persistent **world attunement** before its first cabin upgrade is purchased. Version 1 resolves one specific wood family for structural upgrades. A resolved wood slot requires that family's planks rather than accepting any item in the general planks tag.

The same attunement applies to every cabin and player in the save. Its resolved material identifiers and definition version are persisted; restarts, seed reuse, configuration reloads and later mod or datapack changes must not silently reroll existing requirements. An incompatible saved attunement disables further upgrades with an actionable error. No attunement-migration tooling or save-migration contract is required before public release.

Attunement pools may contain only declared, loaded materials with a valid acquisition profile for that world configuration. Optional integrations contribute candidates only while their required content and world generation are present. Every pool must have a vanilla fallback, and the selection process must fail closed rather than produce an impossible recipe.

The protected controller shows the current size, maximum size, attuned wood and exact requirements for the next expansion. The complete interaction is provisional until Milestone 3.

Item tags, additional attuned slots, biome or dimension groups, discovery catalysts and prerequisite tiers are outside Milestone 2. A later delivery adds one of those capabilities only after the feature that needs it has been aligned.

### Optional content integrations

Optional integrations must fail closed and never prevent the base mod from loading when an integrated mod is absent.

#### Biomes O' Plenty

Loaded Biomes O' Plenty wood profiles may participate in the attuned wood pool. The pool retains vanilla candidates when the mod is absent.
