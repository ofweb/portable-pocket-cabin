# Owners connect their cabins

Status: Draft
Feature ID: B-0009

## Goal

Owners connect cabins through one hallway for the network. Each cabin keeps its identity, roles, storage, and settings.

## Stories and acceptance

Shape must confirm these stories.

### S1: Connect cabins through a hallway

Story: Two owners confirm a connection and enter the cabins through the hallway.

Acceptance:

- Each cabin purchases a permanent connection upgrade before it connects to the network.
- Owners confirm connections and membership. Exterior positions near each other do not create a connection.
- A cabin can have membership in one hallway network at most.
- The first connection creates a lasting hallway. Each cabin in the network has one door that players cannot break. The door identifies its cabin.
- Exterior distance and the dimensions that the cabin can use do not prevent access after connection.
- Each entry checks the role and state for its target cabin, as stated in the household role rules.
- Membership, hallway identity, and door connections stay through restart.

### S2: Place blocks in the hallway

Story: Players place blocks in the hallway without adding those blocks to a cabin's resources.

Acceptance:

- Breaking, explosions, and pistons cannot damage the hallway structure or cabin doors.
- Other hallway space accepts Minecraft blocks. Cabin roles do not prevent changes to player blocks there.
- Cabin storage and automation do not use hallway inventories.

### S3: Remove a cabin from the network

Story: An owner removes a cabin from the network and keeps that cabin and its purchased upgrade.

Acceptance:

- Removal seals and removes only the door for that cabin. It does not delete the cabin or player blocks.
- Removal does not give back upgrade materials. The cabin can then connect to a different network.
- A small network or one that players do not use stays until owners remove it or an operator uses recovery.
- Cabin and network removal follow the safe-exit rules in B-0030.
- Recovery after a membership change that stops before it completes does not create more than one door or give access to a different cabin.

## Feature-wide constraints and acceptance

- Inspection through the network shows no more status than local inspection for the same player.
- Household roles stay local to each cabin. Network membership does not give control of a different cabin.

## Scope

This feature includes connection upgrades, owner confirmation, hallway membership, doors, player blocks, cabin removal, and recovery.

## Non-goals

Storage and roles stay with each cabin. Ownership and resource transfers, world travel without a cabin in the network, and automatic network removal are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Safe travel](../../decisions/pdr/0002-safe-cabin-travel.md).
- [Packed access](../B-0030/brief.md).
- [Resource requests](../B-0031/brief.md).

## Open questions and assumptions

- Upgrade costs, tiers, hallway dimensions, membership limits, and owner controls are open.
- Possible upgrade materials include obsidian and amethyst, with Nether and End materials for higher tiers. Shape must confirm these materials.
- Authority for network removal and the result for player blocks during removal are open questions.
