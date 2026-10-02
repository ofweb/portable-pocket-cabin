# The cabin has livestock rooms by animal type

Status: Draft
Feature ID: B-0022

## Goal

Give the household separate spaces for its livestock types.

## Stories and acceptance

### S1: Add a livestock room

Story: An owner purchases a room for one supported livestock type.

Acceptance:

- Supported types are pigs, chickens, sheep, cows, and goats.
- Each type has its own room. A cabin can have one room for each of these five types.
- Livestock rooms occupy the south side of the west wing.
- Each room starts with a 3×3 usable floor.
- Installation, traversal, permissions, and persistence follow B-0005.

### S2: Expand a livestock room

Story: An owner upgrades one animal type's room to provide more space.

Acceptance:

- Each room grows through 3×3, 5×5, 7×7, 9×9, and 11×11 usable floors.
- Each room has reserved space for its own maximum size.

## Scope

This draft records supported animal types, separate rooms, placement, and floor progression. Manual husbandry still needs shaping.

## Non-goals

- Rideable-animal stalls belong to B-0020.
- Feeding, product collection, and slaughter automation belong to B-0026.

## Related records

- [Room purchase and traversal](../B-0005/brief.md).
- [Automatic room actions](../B-0026/brief.md).
- [Stable](../B-0020/brief.md).
- [Direction](../../direction.md#main-room-and-purpose-specific-rooms).

## Open questions and assumptions

- Room order, height, entrances, and whether livestock rooms connect directly to the west corridor or a branch remain open.
- Costs, population limits, animal variants, manual housing, feeding, breeding, and product collection remain open.
- Expansion behavior for existing animals and player contents remains open.
