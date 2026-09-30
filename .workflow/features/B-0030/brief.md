# Players keep a safe exit from connected cabins

Status: Draft
Feature ID: B-0030

When players enter a connected packed cabin, its blocks operate again. The cabin calculates progress for the time it was empty. Only time while the server operates counts. Progress for that time includes rooms, jobs, and placed blocks such as furnaces and crops. Shape must confirm the draft against the [home decision](../../decisions/pdr/0001-preserve-the-portable-home.md). Direction must confirm the progress limits.

## Goal

Players can enter connected packed cabins while they have a permitted path to a safe exterior.

## Stories and acceptance

Shape must confirm these stories.

### S1: Enter a packed cabin

Story: A player enters a packed cabin in the network through its hallway door and exits through a different deployed cabin.

Acceptance:

- Packing keeps the interior and hallway door. The packed cabin has no exterior exit.
- Entry checks roles for the target cabin, membership, cabin state, and a permitted exit path.
- An exit path must lead to a deployed cabin in the network and a safe loaded exterior position.
- Entry loads only necessary cabin or room space.
- Empty packed cabins do not stay loaded.
- A hallway mailbox uses the target cabin mailbox and its owner access rules.
- Owner hallway entry uses the loadout rules and state for the target cabin.

### S2: Interior packing

Story: A connected cabin owner starts packing from the interior while players keep a safe exit or move to safe positions.

Acceptance:

- Only the owner can start interior packing through a control that other players cannot use.
- Packing checks cabin state and exterior state. Delivery of the bound packed item must be possible.
- Before exterior removal, each online player keeps a permitted exit or moves to a safe position.
- The owner can stay in the interior when a different permitted exit is available.
- The exterior does not accept new entry during packing.
- When two cabins enter packing at the same time, each cannot use the exterior that the other removes as its only exit.
- If player movement or item delivery cannot complete safely, the exterior and cabin state stay as before.

### S3: Recover after path removal

Story: A player enters after a membership or permission change and receives a safe position.

Acceptance:

- Cabin and network removal check permitted exits for online players before they complete.
- If a change cannot complete, membership, exteriors, and player blocks stay as before.
- Players who are not online do not block packing. When a player connects, the cabin checks their position and permitted exits again.
- For a player without an exit, recovery follows the emergency travel sequence in the safe-travel decision.
- Recovery after interruption keeps cabin identity and gives players a safe exit.

## Scope

This feature includes packed access, interior packing, hallway mailbox access, loadout entry, safe network removal, and recovery when players connect.

## Non-goals

Keeping empty packed cabins loaded and an exterior exit from a packed cabin are not part of this feature.

## Related records

- [Direction](../../direction.md#progress-after-an-empty-cabin-becomes-occupied).
- [Hallways](../B-0009/brief.md).
- [Safe travel](../../decisions/pdr/0002-safe-cabin-travel.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Mailbox](../B-0003/brief.md).
- [Owner loadouts](../B-0029/brief.md).

## Open questions and assumptions

- Interior packing controls, hallway mailbox controls, and information about exit path failures are open.
- Shape must confirm the result of an entry request without a permitted exit.
