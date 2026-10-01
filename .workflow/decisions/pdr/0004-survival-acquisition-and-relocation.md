# PDR-0004: Get and move a cabin through Survival mode

Status: Accepted

## Context

Players must be able to get and move their home without operator commands. Deployment and packing require confirmation to prevent accidental moves.

## Decision

Players find and craft three dimensional cores. They combine the cores into a Dimensional Foundation and craft a Cabin Kit. Recipe-book discovery starts with a Block of Amethyst. The components are crafting materials that players cannot place as blocks.

A player deploys a Cabin Kit or active packed cabin item through a site preview and confirmation on the second use. The first successful deployment creates the cabin's lasting identity. The owner packs the cabin through two deliberate uses of the exterior controller. Failed validation keeps the cabin and held item as before. Deployment and packing do not require commands.

## Rationale

The recipes make the cabin available through exploration and crafting. A preview shows the footprint and orientation before deployment. Two uses confirm packing before entrance removal.

## Scope

This decision applies to Survival mode acquisition and controls for deployment and packing. [PDR-0002](0002-safe-cabin-travel.md) states destination safety and evacuation rules. The [README](../../../README.md#playing) gives player controls. The [Cabin Kit recipe](../../../src/main/resources/data/portable_pocket_cabin/recipe/cabin_kit.json) gives crafting ingredients.

Lifecycle commands stay available to operators. Changes to interactions must keep Survival mode usable without commands and require clear confirmation before a move.
