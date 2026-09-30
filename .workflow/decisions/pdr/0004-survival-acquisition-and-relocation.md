# PDR-0004: Acquire and move a cabin through survival play

Status: Accepted

## Context

The cabin is a travelling home for survival play. Players need a way to obtain and move it without routine operator commands. Deployment and packing also need deliberate controls that avoid accidental moves.

## Decision

Players discover and craft three dimensional cores, combine them into a Dimensional Foundation, and craft a Cabin Kit. Normal recipe-book discovery starts with a Block of Amethyst. The components are crafting materials, not placeable machines.

A player deploys a Cabin Kit or current packed cabin item through a site preview and second use. The first successful deployment gives the cabin its permanent identity. The owner packs the cabin through two deliberate uses of the exterior controller. Failed validation leaves the cabin and held item unchanged. Routine deployment and packing do not require commands.

## Rationale

The staged recipes make the cabin attainable through normal exploration and crafting. A preview lets players inspect the footprint and orientation before committing. Two-step packing reduces accidental removal of the only active entrance.

## Scope

This decision covers survival acquisition and player controls for deployment and packing. [PDR-0002](0002-safe-cabin-travel.md) owns destination safety and evacuation. The [README](../../../README.md#playing) gives player controls. The [Cabin Kit recipe](../../../src/main/resources/data/portable_pocket_cabin/recipe/cabin_kit.json) gives crafting ingredients.

## Consequences

Ordinary lifecycle commands remain operator tools. A future interaction change must keep command-free survival play and clear confirmation before a move.
