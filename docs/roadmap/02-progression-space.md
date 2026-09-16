# Milestone 2: World-attuned expansion

**Depends on:** [Milestone 1](01-acquisition-relocation.md).

**Outcome:** A new cabin starts as a 4×4 home and can grow through world-specific material requirements without moving player blocks or risking another cabin's space.

**Status:** Implemented; retrospective acceptance audit pending.

## Scope

This milestone replaces the MVP's fixed 21×21 interior with:

- a palette-aware 4×4 starting room
- one-block general-space expansions
- height growth derived from room size
- one persisted material attunement shared by the world
- data-defined expansion costs and maximum size

[Milestone 0](00-safe-mvp.md) remains authoritative for identity, cell isolation, lifecycle and simulation. [Milestone 3](03-upgrade-interface.md) owns the current funding and installation interface. Storage, automation, roles and connected access belong to their later milestones.

## General-space geometry

New cabins have 4×4 usable floor space, excluding the protected shell. Each upgrade increases both dimensions by one block:

```text
4×4 → 5×5 → 6×6 → 7×7 → …
```

The entrance wall and doorway stay fixed. Each step adds one rear row and a deterministically alternating side column, so existing player blocks never move.

Clear interior height is derived only from persisted general-space size:

```text
sizes 4–5   → 2 blocks
sizes 6–7   → 3 blocks
…
sizes 18–19 → 9 blocks
size 20+    → 10 blocks
```

Changing the configured maximum cannot reshape an existing cabin. An expansion validates every new shell and usable-volume position, including newly exposed height. Any obstruction aborts before world mutation or material consumption. The unchanged usable volume and its player blocks remain untouched.

The definition sets expansion costs and the maximum general size. The bundled maximum is 21; the implementation rejects configured values above the absolute cell-safety cap of 32. Definitions must provide every one-block step from size 5 through their maximum. Lateral geometry is deterministic implementation behavior, not datapack configuration.

General space remains ordinary Minecraft space. Compatible blocks may be placed normally, but enlargement does not grant packed-time simulation. Milestone 0's deployed-only simulation rule still applies outside explicitly managed future fixtures.

The variable-height geometry is fresh-world-only from the earlier fixed-height schema. Unsupported saves fail closed with backup and `just fresh-world` guidance because generated ceiling blocks cannot be distinguished safely from player construction. The [README](../../README.md) is authoritative for the current schema migration matrix.

## Material language

- **Amethyst carries resonance:** it preserves patterns and powers cabin computation, enchanting and automation.
- **Obsidian anchors dimensions:** it installs or strengthens entrances, rooms and connections.

An upgrade may require either or both. This distinction guides default costs without making both materials mandatory for every feature.

## World attunement

Expansion requirements may contain exact item quantities and one world-attuned Planks requirement. The attuned slot resolves to one specific loaded wood profile, not the general Planks tag.

The first progression lookup creates one attunement for the save. Its definition version and resolved wood profile are persisted and shared by every cabin and player. Restart, configuration reload and later datapack or mod changes must not reroll it. If the saved definition version or profile is no longer valid, upgrades stop with an actionable error instead of selecting a replacement.

Candidate wood profiles must be declared in the definition and available through [Milestone 1's material-profile system](01-acquisition-relocation.md#material-profile-format). The bundled pool includes vanilla candidates and conditional Biomes O' Plenty profiles. Missing optional mods do not prevent the base mod from loading. A custom pool with no loaded candidate fails closed rather than producing an impossible requirement.

The shared progression definition is replaceable at:

```text
data/portable_pocket_cabin/portable_pocket_cabin/progression/default.json
```

Milestone 2 owns its `definition_version`, `maximum_general_size`, `wood_pool` and `general_expansions` fields. Each expansion declares a `target_size` and positive-count ingredients that select either an exact `item` or `"attuned_slot": "planks"`. The current schema also contains window costs owned by Milestone 3; overrides must retain all fields required by the loaded schema.

Item tags, more attuned slots, biome or dimension groups, discovery catalysts and prerequisite tiers remain out of scope until a feature requires them.

## Evergreen acceptance contract

Milestone 2 remains accepted only while automated tests and targeted manual checks establish that:

1. New cabins generate a protected, palette-aware 4×4 usable interior.
2. Each expansion grows to exactly the next square size while keeping the entrance fixed and existing player blocks unmoved.
3. Horizontal or vertical obstructions abort without changing the shell, progression state or funded materials.
4. Clear height follows the persisted size formula and caps at ten blocks from size 20.
5. Definitions reject missing size steps, invalid ingredients and maxima outside the supported range.
6. One attunement is shared by multiple players and survives restart and reload without rerolling.
7. Bundled definitions retain usable vanilla candidates when optional profiles are absent; unavailable saved attunements stop upgrades with an actionable error.
8. Cabins at the configured maximum offer no further general-space expansion.
9. Separate cabin cells cannot overlap at any supported size, and simulation tickets track the current bounds.
10. Unsupported fixed-height saves fail without overwriting their registry or pocket-space blocks.

The GameTest and dedicated-server restart suites are the normal automated gates. Manual checks cover the current Milestone 3 interface, visual geometry and multiplayer presentation.
