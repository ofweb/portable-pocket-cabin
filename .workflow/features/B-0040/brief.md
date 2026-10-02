# The cabin has a manual smelting room

Status: Draft
Feature ID: B-0040

## Goal

Give the household a dedicated smelting room that works before storage or automation.

## Stories and acceptance

### S1: Add the smelting room

Story: An owner installs the smelting room book and purchases the room.

Acceptance:

- The room has its own book and upgrade path under B-0038.
- It branches from the north corridor.
- Its usable floor stays fixed at 7×7 through upgrades.
- It handles furnaces and blast furnaces; food preparation belongs to the kitchen.
- Installation, traversal, permissions, and persistence follow B-0005.

### S2: Smelt manually

Story: A household member uses the room before automation is installed.

Acceptance:

- Manual use requires neither central storage nor automation.
- Installed central storage becomes available without another connection upgrade.
- Automation requires its separate book and upgrade purchase.

## Scope

This draft records room placement, size, manual availability, and storage integration. Station operation still needs shaping.

## Non-goals

Automated smelting jobs belong to B-0042. Cooking belongs to B-0041.

## Related records

- [Room purchase and traversal](../B-0005/brief.md).
- [Cabin books](../B-0038/brief.md).
- [Central storage](../B-0004/brief.md).
- [Smelting jobs](../../backlog.md#b-0042-smelting-jobs).
- [Manual production rooms](../../direction.md#manual-production-rooms).

## Open questions and assumptions

- Corridor side, order, height, station counts, upgrade tiers, and costs remain open.
- Fuel, ingredient and output transfers, station interfaces, role permissions, and interrupted-operation recovery remain open.
