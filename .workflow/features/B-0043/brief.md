# The cabin has a manual potion room

Status: Draft
Feature ID: B-0043

## Goal

Give the household a dedicated brewing room that works before storage or automation.

## Stories and acceptance

### S1: Add the potion room

Story: An owner installs the potion room book and purchases the room.

Acceptance:

- The potion room has its own book and upgrade path under B-0038.
- It branches from the north side of the east corridor.
- Its usable floor stays fixed at 5×5 through upgrades.
- The room handles brewing.
- Installation, traversal, permissions, and persistence follow B-0005.

### S2: Brew manually

Story: A household member uses the potion room before automation is installed.

Acceptance:

- Manual use requires neither central storage nor automation.
- Installed central storage becomes available without another connection upgrade.
- Automation requires its separate book and upgrade purchase.

## Scope

This draft records potion-room placement, size, manual availability, and storage integration. Brewing behavior still needs shaping.

## Non-goals

Brewing automation belongs to B-0025. Enchanting belongs to B-0008.

## Related records

- [Room purchase and traversal](../B-0005/brief.md).
- [Cabin books](../B-0038/brief.md).
- [Central storage](../B-0004/brief.md).
- [Cooking and brewing automation](../B-0025/brief.md).
- [Manual production rooms](../../direction.md#manual-production-rooms).

## Open questions and assumptions

- Room order, height, supplied stations, upgrade tiers, and costs remain open.
- Ingredients, bottles, fuel and output transfers, station interfaces, role permissions, optional potion support, and interrupted-operation recovery remain open.
