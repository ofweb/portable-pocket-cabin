# Installed rooms connect through complete corridors

Status: Ready
Feature ID: B-0005

## Goal

Connect installed rooms through continuous corridors that survive cabin travel.

## Stories and acceptance

### S1: Install a corridor with its first room

Story: A cabin owner purchases a purpose-specific room through Cabin Upgrades.

Acceptance:

- Owners install rooms; residents can contribute to funds under household role rules.
- Installing the first room on a corridor builds the complete corridor for free. Corridors have no separate purchase or material cost.
- Later room installations connect to the existing corridor without rebuilding it or disturbing its contents.
- Each cabin has at most one room per type; livestock types have separate rooms.
- Adding a room does not move player blocks in existing spaces.
- Rooms branch off corridor sides. The complete west corridor includes the open northward bend and livestock stretch. Any first west-wing room installs that whole passage; goats occupy its north end.
- Room positions and complete corridor lengths are fixed, independent of purchase order.
- West rooms use both corridor sides. Potions branches north of the east corridor; kitchen stays south. Smelting sits farther north to keep these wings separate.
- Each room has reserved space for the maximum growth defined by its feature. Its upgrades cannot overlap another room's space.
- If installation does not complete, the fund, cabin, and other rooms stay the same.
- Players cannot enter a room before installation is complete.

### S2: Walk to a room

Story: A player walks from the main room to an installed room in a deployed cabin.

Acceptance:

- Owners, residents, and guests can walk between the main room and installed rooms.
- The main room, corridors, and specialized rooms form one continuous interior. Their openings require no doors or room transfers.
- Corridors have 3×3 clear cross-sections. Main-room corridor entrances and room entrances are both 1×2. These dimensions stay fixed through expansion.
- Uninstalled rooms have ordinary protected walls at their entrance positions. Installing a room opens its 1×2 passage.
- Corridors connect only spaces belonging to the same cabin. A shared-lounge connection is a separate feature.
- Unfinished spaces remain inaccessible. Walking between completed spaces does not expose the void or a different cabin's space.
- A guest can inspect the room but cannot make a change to its blocks or state.
- Owners and residents may place blocks that obstruct a passage and clear those blocks themselves. The cabin does not require passages to stay unobstructed.

### S3: Keep the room through travel

Story: An owner moves the cabin or loses its exterior and keeps the installed room.

Acceptance:

- The room, its corridor connection, and player blocks stay with the cabin through restart, packing, and redeployment.
- A missing exterior does not remove the room or its contents.
- Packing closes manual room access until a player can enter the cabin again.
- If installation or cabin travel does not complete, recovery cannot add the same room two times or show a different cabin's space.

### S4: Expand the main room beside installed rooms

Story: An owner expands the main room and keeps short connections to installed rooms.

Acceptance:

- Main-room growth follows the expansion decision. Main-room player blocks stay in place.
- Installed room wings move outward with the expanded main-room walls. Their internal arrangement and contents remain intact.
- Corridor player blocks, decorations, and inventory contents move with the corridor and keep their arrangement. They remain corridor contents after expansion.
- Construction wholly inside a moving wing keeps its arrangement and contents. Connections between the main room and a wing may stop working after expansion; players reconnect them.
- Expansion refuses before changes or material consumption if moving a wing would split an object or encounter contents that cannot be moved safely. The interface identifies what needs clearing.
- Expansion requires every player currently in the cabin to be in the main room. The upgrade interface identifies any wing still occupied and keeps the upgrade fund unchanged until players return.
- The cabin loads moving spaces automatically before expansion, without requiring player visits.
- Before moving a wing, the cabin blocks entry and pauses crafting, furnaces, automation, and animals. Players can remain in the main room.
- Entry and activity resume when expansion completes. An interrupted move keeps the affected spaces closed and paused until recovery finishes.
- Players disconnected in a moved room reconnect safely in the main room. If the interior is unavailable, the usual safe-return rules apply.
- Failed expansion leaves the main room, installed rooms, their contents, and the upgrade fund unchanged. Recovery cannot duplicate or lose moved contents.

## Feature-wide constraints and acceptance

- Passages become available when the main room reaches 5×5. Players can then see the reserved north, west, and east opening positions. Windows cannot occupy those wall centers.
- The cabin checks roles again before room actions after a role change.

## Scope

This feature provides corridors, shared installation rules, traversal, and persistence. Each room's feature owns purchases, facilities, costs, size, and upgrades. Room installation creates its required corridor connection; B-0005 offers no room purchases.

## Non-goals

- Purchasing an empty general-purpose room.
- Greenhouse, stable, aquatic berth, livestock, and forestry behavior.
- Room automation, room upgrades, or manual access through a connected hallway while packed.

## Related records

- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Safe cabin travel](../../decisions/pdr/0002-safe-cabin-travel.md).
- [Upgrade funding decision](../../decisions/pdr/0007-fund-and-install-cabin-upgrades.md).
- [Cabin identity decision](../../decisions/adr/0005-authoritative-cabin-registry.md).
- [Manual crafting room](../B-0039/brief.md).
- [Expansion decision](../../decisions/pdr/0006-expand-general-space-with-world-materials.md).
- [Main room and purpose-specific rooms](../../direction.md#main-room-and-purpose-specific-rooms).
- [Room relocation feasibility](../../../experiments/room-relocation/README.md).
- Floor-layout reference: [scale drawing](../../../experiments/room-layout/layout.svg) and [dimensions and fit limits](../../../experiments/room-layout/README.md).
- [Related room items](../../backlog.md#b-0019-greenhouse).

## Open questions and assumptions

- Room features own aquatic berth dimensions, room heights, and growth anchors. Each must settle its geometry within the reserved layout before its room can be installed.
- The experiment supports relocation of tested vanilla contents. Design must verify cabin-specific records, safe reconnects, and supported mod state against the preservation rules. Unsafe contents block expansion under S4.
