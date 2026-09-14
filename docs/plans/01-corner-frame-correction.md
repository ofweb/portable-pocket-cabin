# Corner-frame correction implementation plan

**Intent:** Correct the palette-driven cabin structure defined by
[Milestone 1](../roadmap/01-acquisition-relocation.md#palette-driven-structures).

**Scope:** Exterior and general-space interior corner framing only. This change does not redesign
the upgrade interface, alter cabin dimensions or costs, implement configurable windows, or change
permissions and lifecycle rules.

**Status:** Implemented and accepted. The clean build, all 53 GameTests, and the dedicated-server
startup/restart checks pass.

## Agreed behavior

- Every corner has an L-shaped vertical Structural Wood frame consisting of the corner column and
  one adjacent column along each joining wall.
- Floors, ceilings and roofs remain palette-selected planks. Expansion derives the same frame at
  the new outer shell.
- Existing and future windows remain between the inner edges of the frames and cannot replace any
  frame block. A window operation that cannot fit must fail validation.
- The door remains centered. A Lodestone controller may replace one inner front-frame column as a
  deliberate functional exception rather than moving the controller or widening the cabin.
- Existing recognizable single-column cabins are converted automatically. Only old wall planks at
  newly structural coordinates are replaced; furnishings and unrelated blocks are untouched.
- Complete, current and partially migrated projections are recognized so migration is idempotent.
  Unrecognizable damage is not repaired and retains existing reconciliation behavior.
- No registry schema or persistent cabin-state change is required because framing is derived from
  the palette, dimensions and fixed structure template.

## Technical approach

- Parameterize interior and exterior structure-map generation by legacy or current corner-frame
  depth while keeping the public/current map on the two-column design.
- Recognize old/current hybrids by requiring unchanged protected positions to match and changed
  positions to contain either the legacy wall block or current Structural Wood block.
- Upgrade recognized exterior projections before lifecycle validity is decided, preventing an old
  valid cabin from being orphaned merely because the template changed.
- Upgrade recognized generated interiors during reconciliation and retain the same opportunistic
  repair when their controller is opened.
- Exclude declared automatic-window positions from interior legacy matching; the new frame does not
  overlap those positions.

## Testing scope

- Block-map tests cover all four interior and exterior L-shaped corners across rotations and sizes.
- Tests assert the controller exception and that current automatic windows do not replace frames.
- Migration tests cover complete legacy, current, partially migrated and damaged structures,
  including repeat execution.
- Expansion tests assert that old posts are removed and the enlarged shell receives the new frame.
- The full GameTest and dedicated-server restart suites remain regression gates.

## Documentation

- Milestone 1 records the now-fixed frame distribution and removes it from the open playtest list.
- No player instructions change; README updates are unnecessary.
