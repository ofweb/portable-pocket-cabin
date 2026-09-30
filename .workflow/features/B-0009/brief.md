# Owners connect their cabins

Status: Draft
Feature ID: B-0009

## Goal

Owners connect separate cabins through a shared hallway. Each cabin keeps its identity, roles, storage, and settings.

## Stories and acceptance

The stories below are proposals for Shape.

### S1: Join a hallway

Story: Two owners approve a connection and visit each other's cabins through the shared hallway.

Acceptance:

- Each cabin purchases a permanent connection upgrade before it joins.
- Owners approve connections and membership. Nearby exteriors do not create a connection.
- Each cabin belongs to at most one hallway network.
- The first connection creates a lasting hallway. Each member cabin has one protected door with a label.
- Exterior distance and supported dimensions do not prevent access after connection.
- Each entry checks the destination cabin's role and state under the household role rules.
- Membership, hallway identity, and door destinations stay through restart.

### S2: Furnish the hallway

Story: Players place blocks in the shared hallway without adding those blocks to a cabin's resources.

Acceptance:

- The hallway shell and cabin doors resist breaking, explosions, and pistons.
- Other hallway space accepts Minecraft blocks. Cabin roles do not protect player furnishings there.
- Cabin storage and automation do not use hallway inventories.

### S3: Leave a hallway

Story: An owner removes a cabin from the network and keeps that cabin and its purchased upgrade.

Acceptance:

- Removal seals and removes only the affected protected door. It does not delete the cabin or player furnishings.
- Departure gives no upgrade refund. The cabin can join another network later.
- A small or inactive network stays until owners dismantle it or an administrator performs recovery.
- Removal and dismantling follow the safe-exit rules in B-0030.
- Recovery after an interrupted membership change does not create a second door or expose a different cabin.

## Feature-wide constraints and acceptance

- Remote status shows no more than local inspection permits for the same player.
- Household roles stay local to each cabin. Network membership does not grant control of another cabin.

## Scope

This feature includes connection upgrades, owner consent, hallway membership, protected doors, furnishings, departure, and recovery.

## Non-goals

Combined storage or roles, world waypoints, ownership transfer, resource transfers, and automatic deletion of inactive networks are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Safe travel](../../decisions/pdr/0002-safe-cabin-travel.md).
- [Packed access](../B-0030/brief.md).
- [Shared discoveries](../B-0010/brief.md).
- [Resource requests](../B-0031/brief.md).

## Open questions and assumptions

- Upgrade costs, tiers, hallway dimensions, membership limits, and owner controls are open.
- Earlier cost proposals use obsidian and amethyst, with possible Nether and End gates. These materials are not settled.
- Authority to dismantle a network and treatment of furnishings during dismantling are open.
