# Milestone 1: Survival crafting and command-free relocation

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

**Exit gate:** Every [acceptance criterion](#acceptance-criteria) passes in a fresh survival world and after restart.


## Consolidated specification

_Source: former `docs/specs/cabin-acquisition-and-relocation.md`._

### Status and scope

This document specifies the first post-MVP replacement for cabin acquisition, material appearance, deployment and packing interaction.

It supersedes the MVP's single-step Cabin Kit recipe, fixed stone shell and player-facing `/cabin preview`, `/cabin deploy` and `/cabin pack` workflow. The authoritative cabin UUID, lifecycle, item-generation, crash-reconciliation, access-control and one-active-exterior invariants in [Milestone 0](00-safe-mvp.md) remain unchanged.

This release intentionally requires a fresh world. Existing stone cabins and cabin records are not migrated.

The Milestone 1 cabin registry has an explicit schema version. Loading an unversioned MVP registry or any unsupported registry version fails server startup with a clear message directing the developer to back up the world and use `just fresh-world`. The failed load must not create a replacement registry, apply a default palette or modify the legacy world files.

Later cabin upgrades use the separate world-attuned upgrade system in [Cabin Progression and Functional Rooms](02-progression-space.md). They are not crafting-table recipes.

### Goals

The acquisition and relocation experience should:

- make the first cabin a meaningful but achievable survival milestone
- express the distinct lore roles of amethyst resonance, obsidian anchoring and controlled dimensional folding
- use a sequence of understandable recipes in the normal 3×3 crafting table
- let recipe-selected materials define both the exterior and interior appearance
- let ordinary players deploy and pack without commands
- preserve all existing transaction and recovery guarantees

It should not introduce a general upgrade station, palette renovation, automatic emergency packing or arbitrary inference of modded material families yet.

### Dimensional components

The recipes follow normal Minecraft recipe-book discovery. They are always craftable when their pattern is known, but hidden from the recipe book until their vanilla-style recipe advancement is completed:

1. Obtaining a Block of Amethyst reveals the Dimensional Logic Core, Dimensional Anchor and Dimensional Folding Core recipes.
2. Obtaining all three cores reveals the Dimensional Foundation recipe.
3. Obtaining a Dimensional Foundation reveals the Cabin Kit recipe.

The unlocks use ordinary recipe advancements and notifications. They do not require a discovered schematic, guide item or custom progression screen.

The three cores and the Dimensional Foundation are non-placeable crafting components. The Foundation uses a chunky, block-like 3D item model so it reads as a dense slab of machinery and stone rather than another glowing core.

#### Dimensional Logic Core

The Logic Core defines and controls the rules of the pocket space. Copper carries signals, redstone supplies logic and a concentrated amethyst block provides the resonance substrate.

```text
C R C
R B R
C R C
```

- `C`: Copper Ingot
- `R`: Redstone Dust
- `B`: Block of Amethyst

Result: one Dimensional Logic Core.

#### Dimensional Anchor

The Anchor gives the pocket space a stable identity and location. Iron provides structure, obsidian provides dimensional stability and amethyst holds the identity pattern.

```text
I O I
O B O
I O I
```

- `I`: Iron Ingot
- `O`: Obsidian
- `B`: Block of Amethyst

Result: one Dimensional Anchor.

#### Dimensional Folding Core

The Folding Core creates the controlled connection between the exterior cabin and its anchored pocket space. The Ender Pearl supplies dimensional travel; copper and amethyst shape it into a repeatable connection.

```text
C B C
B E B
C B C
```

- `C`: Copper Ingot
- `B`: Block of Amethyst
- `E`: Ender Pearl

Amethyst shards are not accepted directly.

Result: one Dimensional Folding Core.

#### Dimensional Foundation

The Foundation combines all three dimensional functions into one cabin base.

```text
S A S
C L C
S F S
```

- `S`: Lodestone
- `A`: Dimensional Anchor
- `C`: Cut Copper Block
- `L`: Dimensional Logic Core
- `F`: Dimensional Folding Core

Result: one Dimensional Foundation.

### Palette-aware Cabin Kit recipe

The final recipe uses all nine crafting-table slots as a small cross-section of the cabin:

```text
R R R
W D W
F X F
```

- `R`: three Planks from one supported wood family; selects the exterior roof and interior ceiling family
- `W`: two Structural Wood items from one supported wood family; selects the exterior framing and interior/exterior wall family
- `D`: one supported Door; selects the exact protected door used inside and outside
- `F`: two Planks from one supported wood family; selects the interior and exterior floor family
- `X`: Dimensional Foundation

**Structural Wood** is defined by the selected material profile: ordinary tree families use Logs, crimson and warped families use their matching Stems, and bamboo uses Bamboo Blocks. The roof, wall and floor families may differ from one another. Repeated ingredients within a role must belong to the same family. The door is independent of all three wood families.

Result: one unbound Cabin Kit carrying the resolved Cabin palette, an immutable unique Kit identity and no cabin UUID. The Kit identity distinguishes a physical crafting result during preview and crash recovery; it is not the permanent cabin identity.

This is a custom palette-aware recipe presented through the normal crafting table and recipe book. It rejects mismatched ingredients within a role rather than silently selecting one of them.

### Material support profiles

A material profile maps a selectable ingredient family to every block form required by the protected structure templates. Persisted palettes contain the resolved block identifiers needed for generation rather than depending only on a profile that may later be removed.

Profiles are versioned datapack resources loaded from `data/<namespace>/portable_pocket_cabin/material_profiles/<path>.json`. A resource has a stable `<namespace>:<path>` profile identifier and declares one of two profile types:

- A `wood_family` profile declares its accepted Planks ingredient, accepted Structural Wood ingredient and resolved Planks, Structural Wood block, Stairs and Slab block identifiers.
- A `door` profile declares its accepted Door ingredient and resolved Door block identifier.

Every profile declares `schema_version`. A bundled optional profile may also declare `required_mod`; it is skipped when that mod is absent. Version 1 loaders fail the resource reload with an actionable error when a loaded profile has an unsupported schema version, references a missing or unsuitable item/block, omits a required form or overlaps another loaded profile so that one recipe input would resolve ambiguously.

Recipe matching and structure generation use the same successfully loaded profile registry. The initial implementation must publish the exact version 1 JSON fields and validation rules alongside its bundled profiles rather than maintaining a separate hard-coded interpretation.

The release ships with:

- profiles for every supported vanilla wood family and vanilla door
- a maintained bundled profile for Biomes O' Plenty wood families and doors when that mod is installed
- a datapack extension point for other modded wood families and doors

The mod never guesses relationships between arbitrary modded logs, planks, stairs, slabs and doors. An ingredient without a complete loaded profile does not match the Cabin Kit recipe. Removing a content mod that supplies blocks already used by a cabin is unsupported; the cabin is never silently converted to oak or another fallback.

Doors with mutable cosmetic states use the exact supported variant selected by the recipe. The generated doors do not oxidise, wax, open or otherwise evolve independently after becoming protected cabin entrances.

### Initial material budget

Before the selected door and palette wood, one Cabin Kit consumes the equivalent of:

- 26 Copper Ingots: eight used directly by cores and eighteen compressed into two Cut Copper Blocks
- eight Iron Ingots: four in the Anchor and four in the Lodestones
- four Redstone Dust
- four Obsidian
- six Blocks of Amethyst
- one Ender Pearl
- 32 Chiseled Stone Bricks used to craft four Lodestones

The final palette adds five Planks, two Structural Wood items and one Door. Door cost varies by the selected type.

This is the initial survival-balancing baseline. It deliberately asks the player to mine copper, iron and redstone, find and harvest an amethyst geode, obtain obsidian and acquire one Ender Pearl without requiring entry into the Nether. Exact quantities may change after survival playtesting, but the multi-stage structure and six-block amethyst commitment remain the intended starting point.

### Cabin palette persistence

A Cabin palette contains four independent selections:

- floor family
- wall/frame family
- roof/ceiling family
- door variant

The unbound Cabin Kit stores its resolved palette and unique Kit identity. On its first successful deployment, cabin creation atomically copies that palette into the authoritative cabin record together with the new UUID and permanent cell index.

After binding, the registry is authoritative. Every current-generation packed item carries the palette for presentation and validation, but editing, duplicating or retaining stale item data cannot change the cabin or create another exterior.

The palette remains fixed across packing, restart and redeployment. A later controller-based renovation system may consume replacement materials and safely update it; palette renovation is not part of this release, and the packed item cannot be surrounded or recrafted to change materials.

### Palette-driven structures

The exterior and pocket interior use the same Cabin palette:

- floor-family planks form the walking surfaces and the matching stair in front of the exterior door
- wall-family Structural Wood blocks form structural framing, with related wall-family planks filling the declared wall positions
- roof-family stairs, slabs or planks form the exterior roof, while related planks form the interior ceiling
- the selected door variant appears at both the exterior entrance and interior exit
- the Lodestone controller and other explicitly functional blocks remain fixed regardless of palette

The exact protected masks remain deterministic and rotation-safe. Palette selection changes block states, not structure dimensions or ownership boundaries.

All cabin-owned wooden shell blocks are fireproof and cannot ignite or be consumed by fire. Player-placed wood remains governed by normal Minecraft fire behavior. Player breaking, piston and explosion protections continue to apply.

The selected door is a portal door rather than an ordinary physical door. It remains visually closed, ignores redstone and ordinary open-state changes, and transports an authorised player when used. This prevents an open door from exposing the hollow exterior projection or behaving differently from its interior counterpart.

The initial implementation updates the current 21×21 MVP interior shell. The later 4×4 progression interior and every expanded shell must consume the same persisted palette when they replace that geometry.

### First deployment and binding

An unbound Cabin Kit remains stack-limited to one. It may be carried, stored or given to another player before deployment.

The first successful deployment transaction:

1. verifies that the deploying player does not already own a cabin
2. verifies that the same Cabin Kit identity and palette used for preview remain present
3. creates the permanent cabin UUID and cell index
4. copies the Cabin palette and source Kit identity into the cabin record
5. journals `DEPLOYING` together with an unresolved deployment-item obligation
6. replaces the held Kit with a non-usable pending item for the new cabin
7. generates the palette-driven interior and exterior
8. commits `DEPLOYED` and resolves the item obligation

A failed validation consumes nothing and creates no cabin record. A player who already owns a cabin may give an unused Kit to another eligible player.

Inventory persistence and world persistence are not assumed to be atomic. Reconciliation therefore guarantees that an interrupted first deployment or later redeployment converges to exactly one of these outcomes:

- A valid exterior and generated interior commit as `DEPLOYED`; any pending, source-Kit or current-generation packed item associated with that deployment is invalidated before it can be used.
- An invalid or absent exterior rolls back to `PACKED`; exactly one current-generation bound Packed Cabin is activated or owed to the owner.

The registry retains an unresolved item-delivery obligation until it is fulfilled. On owner login, reconciliation checks the authoritative lifecycle, physical exterior and matching Kit, pending item or Packed Cabin identities in the player's inventory. It removes or invalidates duplicates, converts the matching item when possible and otherwise delivers the owed bound item. If the inventory cannot accept it, the obligation remains persisted, the owner receives an actionable message and delivery is retried later. Ordinary recovery never requires an operator command.

### Item-driven deployment

Ordinary deployment requires only the Cabin Kit or current bound Packed Cabin item.

#### Selecting a site

The player uses the item on the top face of a solid terrain block. That clicked block remains untouched and acts only as support. The air block directly above it is the position of the cabin-owned front stair.

The door is placed immediately behind the stair and faces outward toward the player. Orientation is resolved to the player's horizontal cardinal facing at the first use and remains locked for that preview even if the player later moves or turns.

Placement is rejected when:

- the clicked face is not the top of a solid supporting block
- any owned floor or stair position would occupy or be supported by lava
- the exact rotated structure mask is obstructed or unsupported
- the outside doorway destination is unsafe
- the world border, build height or supported-dimension rules fail
- the item, palette, UUID or item generation no longer matches the preview

Adjacent lava does not burn the protected shell, but deployment directly on lava is never permitted.

#### Preview and confirmation

The first valid use displays the exact footprint and records the support block, dimension, orientation, item identity and palette for 30 seconds.

- Using the same item on the same top surface again within that window confirms deployment.
- Using it on a different valid surface replaces the old preview with the new location and orientation.
- Expiry, changing dimension or losing the matching item cancels the preview.
- Confirmation reruns every placement and authority check before beginning the deployment journal.

Player-facing text says to use the same surface again; it never instructs an ordinary player to run `/cabin deploy`.

### Controller-driven packing

Packing begins outside at the deployed cabin's Lodestone controller.

Normal use continues to enter the cabin. The owner may hold any item and **sneak-use the same controller twice**:

1. The first sneak-use arms packing for 10 seconds and explains how to confirm.
2. The second sneak-use during that window requests packing.
3. Expiry or arming a different controller cancels the first request without changing cabin state.

The first interaction performs no lifecycle transition and reserves no inventory space. The second interaction invokes the existing transactional packing workflow, which must still:

- verify ownership and the exact deployed controller
- reconcile the exterior before mutation
- reserve one owner inventory slot for the next-generation Packed Cabin
- prove that every required occupant has a safe evacuation destination
- reject new entry while `PACKING`
- show occupants the existing five-second countdown
- abort if the owner disconnects or the reservation or evacuation becomes invalid
- evacuate occupants, remove only the exact protected exterior mask and activate the packed item on success

Failed validation leaves the cabin `DEPLOYED` and explains the concrete reason. The packing gesture never bypasses the cabin registry or calls physical removal directly.

Discoverability is supported by:

- Cabin Kit and Packed Cabin tooltips that explain deployment and packing
- the first successful deployment message explicitly teaching the two sneak-uses
- immediate feedback after the first sneak-use, including the confirmation timeout
- a clear message when a non-owner attempts the packing gesture

This interaction is provisional. If survival playtesting shows that players still fail to discover it, the later protected controller interface may replace it with an explicit Pack action.

### Command policy

Ordinary players no longer need or receive the lifecycle-transition commands:

- `/cabin preview`
- `/cabin deploy`
- `/cabin pack`

These commands remain available to operators as debug and recovery tools and must invoke the same validation and transactional services as item/controller interactions.

Player commands for status, trust and access remain until their own interfaces replace them. Administrative inspection, reconciliation and item recovery commands remain unchanged.

### Deferred features

The following are recorded but intentionally excluded from this release:

- a controller-based renovation interface for changing the Cabin palette
- automatic emergency packing when a protected block intercepts an ignition or fire-consumption event
- higher-fidelity fake-window rendering
- inferred support for arbitrary modded wood or door families
- upgrade recipes in the crafting table

Emergency fire-packing needs separate rules for offline owners, full inventories, unsafe evacuation and simultaneous lifecycle operations before it can be implemented safely.

### Implementation plan

#### Delivery 1: Components and recipes

- register the three core items and non-placeable Dimensional Foundation
- add their four shaped recipes and vanilla-style recipe advancements
- implement the palette-aware Cabin Kit recipe
- assign every crafted Cabin Kit an immutable unique Kit identity
- define and validate the version 1 material-profile datapack schema
- add vanilla and conditional Biomes O' Plenty material profiles
- attach the resolved palette to the crafted unbound Kit
- add recipe-discovery, recipe and invalid-material GameTests

#### Delivery 2: Palette persistence and generation

- add the Cabin palette codec to items and cabin records
- add the versioned registry format and fail-fast legacy-world handling
- make first binding copy the palette atomically
- journal and reconcile deployment-item obligations for first deployment and redeployment
- reconcile pending items and delivery obligations when the owner logs in
- refactor exterior and interior templates to resolve blocks through the palette
- add the wood-accented exterior, palette-driven interior and portal-door behavior
- make generated shell wood fireproof without changing ordinary wood
- reject lava-supported placement
- add persistence, palette, protection and fresh-world tests

#### Delivery 3: Predictable item deployment

- anchor placement to the clicked top surface and put the front stair directly above it
- lock the door facing toward the player at preview time
- bind confirmation to the same item, palette and surface
- replace command-oriented player messages and tooltips
- make transition commands operator-only
- add rotated-placement, replacement-preview and stale-item GameTests

#### Delivery 4: Controller packing

- route owner sneak-use on the controller through a short-lived arming state
- distinguish normal entry from packing regardless of held item
- call the existing transactional pack request only on confirmation
- preserve countdown, inventory reservation, evacuation, concurrency and crash-recovery behavior
- add permission, timeout, duplicate-use and failure-path GameTests

#### Delivery 5: Survival and integration acceptance

- craft the full chain in survival without commands
- verify distinct floor, wall and roof families plus a selected door inside and outside
- repeat pack, restart and redeploy cycles without palette drift
- test every vanilla material profile
- test the bundled Biomes O' Plenty profiles with the supported mod version
- test fire, lava, redstone, explosion and piston boundaries
- complete two-player occupied-packing and stale-item scenarios

### Acceptance criteria

The release is complete only when:

1. All five recipes craft through a normal 3×3 crafting table and appear through the declared vanilla-style discovery chain.
2. No recipe accepts Amethyst Shards in place of Blocks of Amethyst.
3. The Cabin Kit recipe accepts different supported families for floor, wall and roof while rejecting mismatches within one role.
4. Every supported door and all vanilla and bundled Biomes O' Plenty material profiles generate the declared blocks inside and outside.
5. The crafted palette survives binding, packing, restart, recovery and redeployment without being inferred from a possibly stale item.
6. Clicking a supported top surface previews a cabin whose stair is directly above the clicked block and whose door faces the player.
7. Clicking the same surface again confirms; clicking elsewhere replaces the preview; no ordinary command is required.
8. Normal controller use enters, while two owner sneak-uses arm and request packing with any held item.
9. Packing validation, the five-second lock/countdown, safe evacuation, inventory reservation and crash reconciliation retain their existing guarantees.
10. Protected wooden shell blocks cannot burn, but ordinary player wood can.
11. The selected protected door cannot open physically, change state independently or expose the hollow exterior.
12. Placement directly on lava is rejected without consuming the Kit or creating a cabin.
13. Crashes injected around every deployment journal, item, generation and commit boundary reconcile to either one valid deployed exterior or exactly one owed/current bound Packed Cabin without operator help.
14. An unversioned MVP registry fails startup with an actionable message and no world-file mutation.
15. A fresh dedicated-server restart and the complete GameTest suite pass with no legacy-world migration path.

### Playtest decisions

These values are deliberately provisional rather than architecturally fixed:

- exact ingredient quantities after the first survival acquisition run
- the 10-second packing-confirmation window
- whether the sneak-use gesture is discoverable enough to keep
- exact distribution of logs, planks, stairs and slabs within each palette-driven structure mask
- item models, textures, sounds and crafting feedback

## Material-profile format

_Source: former `docs/material-profiles.md`._


Cabin material profiles are server datapack resources stored at
`data/<namespace>/portable_pocket_cabin/material_profiles/<path>.json`. The resource path becomes the
stable profile ID. For example, `data/example/portable_pocket_cabin/material_profiles/wood/cedar.json`
defines `example:wood/cedar`.

Version 1 deliberately uses exact item and block identifiers. The loader does not infer a family from
tags or naming conventions.

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

`required_mod` is optional. When present, the profile is ignored if that Fabric mod ID is absent.
`planks_ingredient` and `structural_wood_ingredient` must be the item forms of the corresponding
blocks. All four block forms must exist.

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

The resolved block must be a door and `door_ingredient` must be that block's item form.

Reload fails with an actionable error when a profile has an unsupported version or type, omits a
required field, references a missing or unsuitable registry entry, or overlaps another loaded profile's
planks, structural wood, or door ingredient. A failed reload leaves the previous successfully loaded
profile set active.

The bundled resources define all vanilla families and door variants. Biomes O' Plenty 26.2 profiles
are conditional on the `biomesoplenty` mod ID and cover its complete wood-family set.

## Manual acceptance

### Milestone 1 manual acceptance

1. Use `just fresh-world`, join in survival, and obtain a Block of Amethyst. Confirm the three core recipes appear in the normal recipe book.
2. Craft the Logic Core, Anchor, and Folding Core, then the Foundation. Confirm each discovery reveals the next recipe stage and that every amethyst ingredient is a full block.
3. Craft a Cabin Kit with different supported roof, structural-wall, and floor woods plus a door. Confirm mismatched materials within one row are rejected.
4. Use the Kit on the top of a clear solid block. Confirm the first use previews, the second use on that same surface deploys, the front stair occupies the block above the click, and the door faces you. Confirm placement over lava is rejected.
5. Enter through the door or lodestone and confirm the exterior and 21×21 interior use the chosen palette and exact door. Confirm the protected wood does not burn and the portal doors stay closed under use and redstone.
6. Sneak-use the exterior lodestone once, let the ten-second confirmation expire, and confirm nothing changes. Sneak-use it twice, then confirm the five-second transactional countdown packs the cabin and preserves its interior.
7. Redeploy in each supported vanilla dimension, restart while deployed and packed, and confirm item identity, palette, cabin UUID, cell, contents, and one-active-exterior guarantees survive.
8. With Biomes O' Plenty 26.2 installed, repeat the recipe with each bundled BOP family used in every material role. Add a test datapack profile and confirm it participates in the same recipe and structure pipeline.
9. Run `./gradlew build` and confirm all GameTests and both dedicated-server persistence boots pass.
