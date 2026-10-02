# The cabin has a manual kitchen

Status: Draft
Feature ID: B-0041

## Goal

Give the household a dedicated kitchen that works before storage or automation.

## Stories and acceptance

### S1: Add the kitchen

Story: An owner installs the kitchen book and purchases the room.

Acceptance:

- The kitchen has its own book and upgrade path under B-0038.
- It branches from the south side of the east corridor.
- Its usable floor stays fixed at 5×5 through upgrades.
- The kitchen handles food preparation.
- Installation, traversal, permissions, and persistence follow B-0005.

### S2: Prepare food manually

Story: A household member uses the kitchen before automation is installed.

Acceptance:

- Manual use requires neither central storage nor automation.
- Installed central storage becomes available without another connection upgrade.
- Automation requires its separate book and upgrade purchase.

## Scope

This draft records kitchen placement, size, manual availability, and storage integration. Food preparation behavior still needs shaping.

## Non-goals

Cooking automation belongs to B-0025. Material smelting belongs to B-0040.

## Related records

- [Room purchase and traversal](../B-0005/brief.md).
- [Cabin books](../B-0038/brief.md).
- [Central storage](../B-0004/brief.md).
- [Cooking and brewing automation](../B-0025/brief.md).
- [Manual production rooms](../../direction.md#manual-production-rooms).

## Open questions and assumptions

- Room order, height, supplied stations, recipes, optional mod support, upgrade tiers, and costs remain open.
- Ingredient, fuel and output transfers, station interfaces, role permissions, and interrupted-operation recovery remain open.
