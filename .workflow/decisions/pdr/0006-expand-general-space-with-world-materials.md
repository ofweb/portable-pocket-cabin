# PDR-0006: Expand general space with world materials

Status: Accepted

## Context

A travelling home needs room to grow as players explore. Expansion must retain the blocks and appearance that make each cabin feel permanent. Material requirements should connect growth to the world without assigning each player a different recipe.

## Decision

Each new cabin has a protected, palette-aware interior with 4×4 blocks of usable [general space](../../context.md#general-space). Each purchased expansion increases both usable dimensions by one block. The entrance remains in place, and existing player blocks stay in their positions. Clear height grows with general size and stops at ten blocks from size 20. General space retains ordinary Minecraft behavior while the cabin is deployed.

One [World attunement](../../context.md#world-attunement) sets the variable plank requirement for every cabin in a save. Each expansion can require exact items and planks from the attuned wood profile. Amethyst expresses [Resonance](../../context.md#resonance), while obsidian anchors dimensional work. An individual upgrade can require either material or both.

The installed definition sets the costs and maximum general size. The bundled maximum is 21. A cabin at its configured maximum has no further general-space offer. An obstruction prevents expansion before the cabin changes or committed materials are consumed.

## Rationale

Small steps let players grow one home without moving their construction. A shared attunement gives the world a consistent material identity. Exact costs let packs set progression without changing the cabin's geometry.

## Scope

This decision covers general-space growth and its material language. [PDR-0005](0005-keep-the-chosen-cabin-palette.md) owns the saved palette. [PDR-0007](0007-fund-and-install-cabin-upgrades.md) owns funding and installation controls. Storage, rooms, and packed-time automation remain separate work.

## Related records

- [ADR-0008](../adr/0008-derive-expansion-geometry-from-saved-size.md) owns the geometry and safe expansion method.
- [ADR-0009](../adr/0009-persist-versioned-world-attunement.md) owns attunement persistence and definitions.
