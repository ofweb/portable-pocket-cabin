# A cabin can add a purpose-specific room

Status: Draft
Feature ID: B-0005

## Goal

Let owners purchase purpose-specific rooms with continuous access and contents that survive cabin travel. The main room provides general space.

## Stories and acceptance

### S1: Add a room

Story: A cabin owner purchases a purpose-specific room through Cabin Upgrades.

Acceptance:

- Only the owner can install the room. Residents can contribute to its upgrade fund by the household role rules.
- A room purchase includes the facilities and behavior defined by its feature. No empty-room purchase is required.
- The first room purchased on a corridor creates that corridor in the same purchase. There is no separate corridor purchase.
- Each cabin can purchase one room of each type, with livestock rooms defined per animal type. Upgrades provide more space or capacity; players cannot purchase duplicates.
- Rooms have separate space. Adding a room does not move player blocks in existing spaces.
- Rooms branch off corridor sides. The west corridor bends north into the livestock corridor; goats occupy its north end.
- Room positions are fixed, independent of purchase order. Corridors extend past unpurchased positions when necessary.
- West rooms use both corridor sides. Potions branches north of the east corridor; kitchen stays south. Smelting sits farther north to keep these wings separate.
- Each room has reserved space for the maximum growth defined by its feature. Its upgrades cannot overlap another room's space.
- If installation does not complete, the fund, cabin, and other rooms stay the same.
- Players cannot enter a room before installation is complete.

### S2: Walk to a room

Story: A player in a deployed cabin walks through an open passage and corridor to a purpose-specific room.

Acceptance:

- Owners, residents, and guests can walk between the main room and installed rooms.
- The main room, corridors, and specialized rooms form one continuous interior. Their openings require no doors or room transfers.
- Corridors have 3×3 clear cross-sections; openings are 1×2. Width and height stay fixed as the main room expands.
- Corridors connect only spaces belonging to the same cabin. A shared-lounge connection is a separate feature.
- Unfinished spaces remain inaccessible. Walking between completed spaces does not expose the void or a different cabin's space.
- A guest can inspect the room but cannot make a change to its blocks or state.
- Owners and residents may place blocks that obstruct a passage and clear those blocks themselves. The cabin does not require passages to stay unobstructed.

### S3: Keep the room through travel

Story: A cabin owner moves the cabin or finds its exterior missing and can enter the same room again.

Acceptance:

- The room, its corridor connection, and player blocks stay with the cabin through restart, packing, and redeployment.
- A missing exterior does not remove the room or its contents.
- Packing closes manual room access until a player can enter the cabin again.
- If installation or cabin travel does not complete, recovery cannot add the same room two times or show a different cabin's space.

### S4: Expand the main room beside installed rooms

Story: An owner expands the main room and keeps short connections to the installed specialized rooms.

Acceptance:

- The main room grows around a fixed center. Each expansion adds one block on all four sides. Player blocks in the main room stay in place.
- Installed room wings move outward with the expanded main-room walls. Their internal arrangement and contents remain intact.
- Corridor player blocks, decorations, and inventory contents move with the corridor and keep their arrangement. They remain corridor contents after expansion.
- Expansion requires every player currently in the cabin to be in the main room. The upgrade interface identifies any wing still occupied and keeps the upgrade fund unchanged until players return.
- The cabin loads the moving spaces automatically before expansion. Players do not need to visit each room to prepare it.
- Before moving a wing, the cabin blocks entry and pauses its activity, including crafting, furnaces, automation, and animals. Players can remain in the main room while the owner uses the upgrade interface.
- Entry and activity resume when expansion completes. An interrupted move keeps the affected spaces closed and paused until recovery finishes.
- Players disconnected in a moved room reconnect safely in the main room. If the interior is unavailable, the usual safe-return rules apply.
- Failed expansion leaves the main room, installed rooms, their contents, and the upgrade fund unchanged. Recovery cannot duplicate or lose moved contents.

## Feature-wide constraints and acceptance

- Passages become available when the main room reaches 5×5. Players can then see the reserved north, west, and east opening positions. Windows cannot occupy those wall centers.
- The cabin checks roles again before room actions after a role change.

## Scope

Shared installation, traversal, and persistence belong here. Each room's feature owns facilities, costs, size, and upgrades.

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

- The drawing records agreed room placement and the open corridor bend. Aquatic berth dimensions, room heights, and growth anchors remain open.
- Relocation is feasible for the vanilla contents covered by the isolated experiment. Supported mod state, external links, cabin-specific records, and safe reconnect handling remain unverified.
- Room growth limits belong to each room's feature. Its reserved space must account for those limits.
