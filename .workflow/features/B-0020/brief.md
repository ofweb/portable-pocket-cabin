# The cabin has a stable

Status: Draft
Feature ID: B-0020

## Goal

Give the household dedicated stalls for rideable animals.

## Stories and acceptance

### S1: Add the stable

Story: An owner purchases a stable with its first stall.

Acceptance:

- The stable branches from the north side of the west corridor.
- It has a three-block-wide internal corridor with stalls on one side.
- Each stall has a 5×7 usable floor and a wall or partition separating it from the internal corridor.
- The stable starts with one stall.
- Installation, traversal, permissions, and persistence follow B-0005.

### S2: Add stalls

Story: An owner upgrades the stable to provide more stalls.

Acceptance:

- The stable supports at most five stalls within its reserved space.
- Additional stalls stay on the same side of its internal corridor.

## Scope

This draft records stable placement, stall geometry, and capacity. Housing and releasing animals still need shaping.

## Non-goals

- Livestock rooms belong to B-0022.
- The aquatic berth is a separate stable extension in B-0021.

## Related records

- [Room purchase and traversal](../B-0005/brief.md).
- [Aquatic berth](../B-0021/brief.md).
- [Direction](../../direction.md#main-room-and-purpose-specific-rooms).

## Open questions and assumptions

- The [layout reference](../../../experiments/room-layout/README.md) records the stable's position. Stall orientation, height, entrances, and partitions remain open.
- Upgrade steps, costs, animal profiles, occupancy limits, housing, release, and animal identity preservation remain open.
- Expansion behavior for existing animals and player contents remains open.
