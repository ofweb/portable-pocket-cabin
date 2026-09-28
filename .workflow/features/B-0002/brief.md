# Household roles give players cabin permissions

Status: Ready
Feature ID: B-0002

## Goal

Each player has one [household role](../../context.md#household-role) in each cabin. The owner can give residents access to the household. Guests can enter but cannot make a change to the cabin.

## Stories and acceptance

### S1: Add a resident

Story: A cabin owner adds or removes a resident to give or remove household access for that player.

Acceptance:

- The cabin has one owner. Only that player can add or remove residents.
- `/cabin resident add <player>` and `/cabin resident remove <player>` are available when the cabin is deployed or packed.
- The command accepts a player who is online or was known to the server before.
- The command rejects an unknown player, the owner, or a request that makes no change. It gives a reason.
- `/cabin household list` shows the owner and assigned residents. All other players are guests.
- Resident assignments stay saved after restart, packing, and redeployment. A role in one cabin gives no access to a different cabin.

### S2: Use a cabin as a resident

Story: A resident enters the cabin and contributes to upgrades without ownership.

Acceptance:

- Owners, residents, and guests can enter a deployed cabin and use its exit.
- Residents can contribute to and withdraw from upgrade funds. Only the owner can install or reverse upgrades.
- Guests can see the upgrade interface but cannot make a change to a fund or other cabin state.
- Guests cannot place or break interior blocks, open interior inventories, interact with entities, or collect or release items in the cabin.
- Guests can use player actions that do not make a change to the cabin. The exit and upgrade inspection are available.

### S3: Remove a resident

Story: An owner removes a resident, and the player's access stops before the next cabin action.

Acceptance:

- The removed resident becomes a guest immediately, and the change stays saved.
- An open upgrade interface shows the new role before the next action. That player can see it but cannot make a change to a fund.
- Each cabin action checks the player's role and cabin before it changes state.
- If the server rejects a role change, saved roles and open interactions stay the same.

## Feature-wide constraints and acceptance

- The `/cabin trust` and `/cabin access` commands have no aliases.
- The game rejects earlier cabin save formats before a change to cabin or world data. The error tells the player to back up the save and create a new world.
- Other guest actions cannot make a change to the cabin interior.

## Scope

This feature replaces the entry policy with cabin-local roles. It includes role commands, entry, upgrade funds, and guest actions in the cabin.

## Non-goals

- Household management screens, mailbox, central storage, and storage-funded upgrades.
- Owner-only entry, permission settings for each player, or ownership transfer.

## Related records

- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Upgrade funding decision](../../decisions/pdr/0007-fund-and-install-cabin-upgrades.md).
- [Safe travel decision](../../decisions/pdr/0002-safe-cabin-travel.md).
