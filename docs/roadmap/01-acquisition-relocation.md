# Milestone 1: Survival crafting and command-free relocation

**Depends on:** [Milestone 0](00-safe-mvp.md).

**Outcome:** A survival player can craft a palette-aware Cabin Kit, deploy it by using terrain twice and pack it by sneak-using the exterior controller twice. Ordinary lifecycle commands are unnecessary.

**Status:** Complete. Milestone 0 remains authoritative for lifecycle, recovery, access and one-active-exterior invariants.

## Scope

This milestone replaces the MVP's single-step recipe, fixed stone shell and player-facing `/cabin preview`, `/cabin deploy` and `/cabin pack` workflow. It adds:

- a staged five-recipe acquisition chain
- recipe-selected floor, wall/frame, roof/ceiling and door materials
- versioned material profiles for vanilla, bundled Biomes O' Plenty materials and datapack extensions
- crash-safe first binding and item-driven deployment
- controller-driven packing through the existing transaction
- palette-aware protected structures and portal doors

Palette renovation, automatic emergency packing, inferred support for arbitrary modded material families and crafting-table upgrades remain out of scope. [Milestone 2](02-progression-space.md) owns world-attuned upgrades and current interior geometry.

## Acquisition

### Recipe discovery

Recipes use normal crafting tables, recipe-book advancements and notifications:

1. Obtaining a Block of Amethyst reveals the three dimensional-core recipes.
2. Obtaining all three cores reveals the Dimensional Foundation.
3. Obtaining a Dimensional Foundation reveals the Cabin Kit.

The three cores and Foundation are non-placeable crafting components. Amethyst shards cannot replace Blocks of Amethyst.

### Dimensional components

The Dimensional Logic Core uses copper for signals, redstone for logic and amethyst for resonance:

```text
C R C
R B R
C R C

C = Copper Ingot
R = Redstone Dust
B = Block of Amethyst
```

The Dimensional Anchor uses iron for structure, obsidian for dimensional stability and amethyst for identity:

```text
I O I
O B O
I O I

I = Iron Ingot
O = Obsidian
B = Block of Amethyst
```

The Dimensional Folding Core uses an Ender Pearl for travel and copper and amethyst to control the connection:

```text
C B C
B E B
C B C

C = Copper Ingot
B = Block of Amethyst
E = Ender Pearl
```

The Dimensional Foundation combines all three functions:

```text
S A S
C L C
S F S

S = Lodestone
A = Dimensional Anchor
C = Cut Copper Block
L = Dimensional Logic Core
F = Dimensional Folding Core
```

Before palette materials, these components require the equivalent of 26 Copper Ingots, eight Iron Ingots, four Redstone Dust, four Obsidian, six Blocks of Amethyst, one Ender Pearl and four Lodestones, including 32 Chiseled Stone Bricks. The Kit adds five Planks, two Structural Wood items and one Door. This is the survival-balancing baseline.

### Cabin Kit

The final recipe uses all nine slots:

```text
R R R
W D W
F X F

R = roof-family Planks
W = wall-family Structural Wood
D = supported Door
F = floor-family Planks
X = Dimensional Foundation
```

The roof, wall and floor roles may use different supported families. Repeated ingredients within one role must use the same family. A profile defines Structural Wood: logs for ordinary trees, stems for crimson and warped wood, and Bamboo Blocks for bamboo. The door is independent of all three wood selections.

The custom recipe returns one unbound, stack-limited Cabin Kit with:

- the resolved Cabin palette
- an immutable Kit identity
- no cabin UUID

The Kit identity distinguishes the physical crafting result during preview and recovery. It is not the permanent cabin identity. A Kit may be stored or given to another player before its first deployment.

## Palette model

A Cabin palette contains four independent selections:

- floor family
- wall/frame family
- roof/ceiling family
- exact door variant

The unbound Kit carries the resolved palette. First deployment copies it into the authoritative cabin record. Current-generation packed items repeat it for presentation and validation, but item data cannot alter registry-owned palette state.

The palette survives packing, restart, recovery and redeployment. Removing a content mod that supplies blocks already persisted in a palette is unsupported; the cabin must never silently fall back to oak or another material.

### Structure projection

The same palette controls the exterior and general interior:

- floor planks form walking surfaces and the exterior front stair
- Structural Wood forms frames, while related planks fill wall positions
- roof-family stairs, slabs and planks form the exterior roof and interior ceiling
- the selected door appears at the exterior entrance and interior exit
- lodestone controllers and other functional blocks remain fixed

Palette changes affect block states, not structure dimensions or ownership masks. Exterior and general-interior corners use L-shaped vertical Structural Wood frames: the corner column and adjacent column on each joining wall. Doors, controllers and windows may replace only their declared positions and never a protected corner frame.

Recognizable single-column legacy frames migrate in place. Migration replaces only legacy wall planks at new frame coordinates, accepts partially migrated projections and is safe to repeat. It does not rebuild the shell, change its dimensions or repair unrelated damage. Unrecognizable exteriors use normal reconciliation.

Cabin-owned wooden shell blocks cannot ignite or be consumed by fire. Player-placed wood retains normal fire behavior. Existing break, piston and explosion protection still applies.

Protected entrances are portal doors. They remain visually closed, ignore redstone and ordinary open-state changes, and transport authorized players when used. Mutable door variants do not oxidize, wax or otherwise change independently after installation.

[Milestone 2](02-progression-space.md) replaced the original 21×21 interior with an expandable 4×4 starting room. Every current shell and expansion uses the persisted palette.

## First binding and recovery

First deployment must:

1. Verify that the player does not own a cabin and still holds the Kit identity and palette used for preview.
2. Create the permanent cabin UUID and cell index and copy the palette and source Kit identity into the registry.
3. Persist `DEPLOYING` with an unresolved item-delivery obligation.
4. Replace the held Kit with a non-usable pending item.
5. Generate the current interior and palette-driven exterior.
6. Commit `DEPLOYED`, consume the matching pending item and resolve the obligation.

Failed validation consumes nothing and creates no cabin record. Inventory and world persistence are not treated as atomic, so interrupted deployment or redeployment must reconcile to one result:

- A valid exterior and interior commit as `DEPLOYED`; associated pending, source-Kit and current-generation packed items become unusable.
- An invalid or absent exterior rolls back to `PACKED`; exactly one current-generation bound item is active or owed to the owner.

The registry retains an item-delivery obligation until fulfilled. On owner login, reconciliation compares lifecycle and exterior state with matching Kit, pending-item and packed-item identities. It invalidates duplicates, converts a matching item when possible or retries delivery later when inventory is full. Ordinary recovery requires no operator command.

## Deployment interaction

Ordinary deployment requires an unbound Cabin Kit or current bound Packed Cabin.

### Site selection

The player uses the item on the top face of a solid support block. That block remains untouched; the air block above it becomes the cabin-owned front stair. The door sits behind the stair and faces the player's horizontal direction at the first use.

Placement fails without consuming the item when:

- the clicked face is not the top of a solid block
- any owned floor or stair position occupies or is supported by lava
- the exact rotated ownership mask is obstructed or unsupported
- the exterior doorway has no safe destination
- the site violates world-border, build-height or supported-dimension rules
- the item identity, palette, cabin UUID or item generation no longer matches the preview

Adjacent lava is allowed because the protected shell cannot burn; direct lava support is not.

### Preview and confirmation

The first valid use shows the exact footprint and records the support block, dimension, orientation, item identity and palette for 30 seconds.

- Reusing the same item on the same surface within that window confirms deployment.
- Using it on another valid surface replaces the preview and orientation.
- Expiry, changing dimension or losing the matching item cancels the preview.
- Confirmation reruns every placement and authority check before journalling deployment.

## Packing interaction

Normal use of the exterior lodestone enters the cabin. The owner may hold any item and sneak-use the same controller twice:

1. The first use arms packing for 10 seconds and explains how to confirm.
2. A second use during that window requests packing.
3. Expiry or arming another controller cancels the request without changing cabin state.

Arming performs no lifecycle transition and reserves no inventory space. Confirmation invokes the Milestone 0 packing transaction, which must:

- verify ownership and the exact deployed controller
- reconcile the exterior before mutation
- reserve one owner inventory slot for the pending next-generation item
- prove that every occupant has a safe evacuation destination
- reject entry throughout `PACKING`
- run the five-second countdown
- abort if the owner disconnects or the reservation or evacuation becomes invalid
- evacuate occupants, remove only the protected exterior mask and activate the packed item

Failure leaves the cabin `DEPLOYED` and reports the reason. The controller never bypasses the authoritative registry or removes the exterior directly.

Cabin Kit and Packed Cabin tooltips explain deployment and packing. First deployment teaches the packing gesture, the first sneak-use reports its timeout, and non-owners receive a permission error.

The lifecycle commands remain operator-only diagnostics:

```text
/cabin preview
/cabin deploy
/cabin pack
```

They call the same validation and transaction services as item and controller interactions. Player status, trust and access commands remain available until their own interfaces replace them.

## Save compatibility

Milestone 1 deliberately required a fresh world instead of migrating unversioned stone-cabin MVP records. Unsupported registry versions fail startup with backup and `just fresh-world` guidance without creating a replacement registry or modifying the rejected save.

That constraint records this milestone's rollout, not the current migration matrix. The [README](../../README.md) is authoritative for save compatibility between current registry schemas.

## Material-profile format

Cabin material profiles are server datapack resources at:

```text
data/<namespace>/portable_pocket_cabin/material_profiles/<path>.json
```

The resource becomes the stable profile ID `<namespace>:<path>`. Version 1 uses exact item and block identifiers; it does not infer families from tags or names.

### Wood family

```json
{
  "schema_version": 1,
  "type": "wood_family",
  "required_mod": "examplemod",
  "planks_ingredient": "examplemod:cedar_planks",
  "structural_wood_ingredient": "examplemod:cedar_log",
  "planks": "examplemod:cedar_planks",
  "structural_wood": "examplemod:cedar_log",
  "stairs": "examplemod:cedar_stairs",
  "slab": "examplemod:cedar_slab"
}
```

`planks_ingredient` and `structural_wood_ingredient` must be the item forms of their corresponding blocks. All declared block forms must exist.

### Door

```json
{
  "schema_version": 1,
  "type": "door",
  "required_mod": "examplemod",
  "door_ingredient": "examplemod:cedar_door",
  "door": "examplemod:cedar_door"
}
```

`door` must resolve to a door block, and `door_ingredient` must be that block's item form.

`required_mod` is optional for both profile types. When present, the profile is skipped if that Fabric mod ID is absent.

Reload fails with an actionable error when any loaded profile:

- uses an unsupported schema version or type
- omits a required field
- references a missing or unsuitable registry entry
- overlaps another profile's planks, Structural Wood or door ingredient

A failed reload leaves the previous successful profile set active. At least one wood profile and one door profile must load.

Bundled resources define all supported vanilla families and door variants. Conditional Biomes O' Plenty 26.2 profiles cover its wood families and doors when `biomesoplenty` is loaded. Other mods require explicit datapack profiles. [Milestone 11](11-compatibility-polish.md) owns the optional-mod compatibility matrix.

## Evergreen acceptance contract

Milestone 1 remains accepted only while automated tests and targeted manual checks establish that:

1. The five recipes use the declared patterns and discovery chain; shards never substitute for amethyst blocks.
2. The Cabin Kit accepts independent supported floor, wall, roof and door choices but rejects a mixed family within one role.
3. Every bundled vanilla profile projects the declared blocks inside and outside; external profiles use the same recipe and structure pipeline.
4. Palette and item identity survive binding, packing, restart, recovery and redeployment without inference from stale items.
5. Interrupted first deployment and redeployment reconcile to one valid exterior or one current or owed bound item without operator help.
6. Top-surface selection, locked orientation, replacement previews and confirmation behave consistently in every rotation.
7. Invalid support, obstruction, unsafe doorway, lava, world-border, height, dimension and stale-item checks fail without consuming the item or creating a cabin.
8. Normal controller use enters; two owner sneak-uses request packing with any held item; timeout and non-owner attempts do not mutate lifecycle state.
9. Packing retains the five-second entry lock, evacuation, inventory reservation, concurrency and crash-recovery guarantees from Milestone 0.
10. Cabin-owned wood resists fire, player wood behaves normally, and protected portal doors resist physical, redstone and cosmetic state changes.
11. Unsupported registry versions and invalid profile reloads fail closed without overwriting valid persisted data or the previous profile set.
12. The GameTest suite and both dedicated-server persistence boots pass.

Manual testing is reserved for recipe-book presentation, tooltips, structure appearance, interaction discoverability and optional-mod combinations not established by the automated suite.
