# A cabin can add a room

Status: Draft
Feature ID: B-0005

## Goal

Let a cabin owner add a room that keeps its space and contents through cabin travel. Players can enter the room from the cabin interior.

## Stories and acceptance

### S1: Add a room

Story: A cabin owner installs a room upgrade and gets one empty room in that cabin.

Acceptance:

- Only the owner can install the room. Residents can contribute to its upgrade fund by the household role rules.
- The room has space that other rooms do not use. Adding a room does not move player blocks in the cabin or other rooms.
- If installation does not complete, the fund, cabin, and other rooms stay the same.
- Players cannot enter a room before installation is complete.

### S2: Use the room door

Story: A player in a deployed cabin uses an interior door to move between the cabin and a room.

Acceptance:

- Owners, residents, and guests can use the door. The door checks the player's role for that cabin before entry.
- Each door connects only the cabin and its room. A player cannot enter a different cabin or room through that door.
- If the target space is not safe or ready, the player stays at the source and gets a reason.
- Entry and exit do not show the void, pocket coordinates, or a different cabin's space.
- A guest can inspect the room but cannot make a change to its blocks or state.

### S3: Keep the room through travel

Story: A cabin owner moves the cabin or finds its exterior missing and can enter the same room again.

Acceptance:

- The room, its door, and player blocks stay with the cabin through restart, packing, and redeployment.
- A missing exterior does not remove the room or its contents.
- Packing closes manual room access until a player can enter the cabin again.
- If installation or cabin travel does not complete, recovery cannot add the same room two times or show a different cabin's space.

## Feature-wide constraints and acceptance

- A room has a space limit. Room space cannot use space from other rooms or cabins.
- The cabin checks role and target safety again before each door transfer.
- The cabin checks roles again before room actions after a role change.

## Scope

This feature includes room installation, an empty room, safe interior travel, and room persistence.

## Non-goals

- Greenhouse, stable, aquatic berth, livestock, and forestry behavior.
- Room automation, room upgrades, or manual access through a connected hallway while packed.

## Related records

- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Upgrade funding decision](../../decisions/pdr/0007-fund-and-install-cabin-upgrades.md).
- [Cabin identity decision](../../decisions/adr/0005-authoritative-cabin-registry.md).
- [Room source](../../../docs/roadmap/05-functional-rooms.md).
- [Related room items](../../backlog.md#b-0019-greenhouse).

## Open questions and assumptions

- The first room upgrade, room limits, material requirements, and installation point are open.
- The door position and exit behavior when a door is blocked are open.
- The rules for more than one room of one type are open.
