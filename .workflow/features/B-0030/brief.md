# Players keep a safe exit from connected cabins

Status: Draft
Feature ID: B-0030

## Goal

Players can reach connected packed cabins while players have a permitted route to a safe exterior.

## Stories and acceptance

The stories below are proposals for Shape.

### S1: Visit a packed cabin

Story: A player enters a packed member cabin through its hallway door and leaves through another deployed cabin.

Acceptance:

- Packing keeps the interior and hallway door. The packed cabin has no exterior exit.
- Entry checks destination roles, membership, cabin state, and a permitted exit route.
- A usable exit reaches a deployed member cabin and a safe loaded exterior position.
- Entry loads only necessary cabin or room space and applies managed progress within catch-up limits.
- Empty packed cabins do not stay loaded. Inactive placed blocks stay paused.
- A hallway mailbox uses the destination cabin's existing mailbox and owner access rules.
- Owner hallway entry uses that cabin's loadout rules. Loadout state stays in the destination cabin.

### S2: Pack from inside

Story: A connected cabin owner packs from inside while occupants keep a safe exit or evacuate safely.

Acceptance:

- Only the owner can initiate interior packing through a protected control.
- Packing checks cabin state, exterior state, and guaranteed delivery of the bound packed item.
- Each online occupant keeps a permitted exit or receives a safe evacuation destination before exterior removal.
- The owner can stay inside when another permitted exit remains available.
- The exterior does not accept new entry during packing.
- Concurrent packing cannot let two cabins rely on each other's disappearing exterior.
- If evacuation or item delivery cannot complete safely, packing leaves the exterior and cabin state as before.

### S3: Recover after a route disappears

Story: A player returns after a membership or permission change and receives a safe destination.

Acceptance:

- Membership removal and dismantling check each affected online occupant's permitted exit before they complete.
- A failed change leaves membership, exteriors, and furnishings as before.
- Offline players do not block packing. Login checks cabin occupancy and available permitted exits again.
- A player without an exit receives a destination through the safe-travel emergency chain.
- Recovery after interruption keeps cabin identity and does not strand an occupant.

## Scope

This feature includes packed access, interior packing, hallway mailbox access, loadout entry, safe departure, and login recovery.

## Non-goals

Background simulation of inactive placed blocks and an exterior exit from a packed cabin are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Hallways](../B-0009/brief.md).
- [Safe travel](../../decisions/pdr/0002-safe-cabin-travel.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Mailbox](../B-0003/brief.md).
- [Owner loadouts](../B-0029/brief.md).

## Open questions and assumptions

- Interior packing controls, hallway mailbox controls, and route failure messages are open.
- The entry response when no permitted exit exists needs confirmation in Shape.
