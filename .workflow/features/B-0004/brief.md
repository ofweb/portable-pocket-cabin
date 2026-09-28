# The cabin has central storage

Status: Draft
Feature ID: B-0004

## Goal

Give each cabin one inventory for household items. The cabin does not move items from placed inventories.

## Stories and acceptance

### S1: Keep household items

Story: An owner or resident puts items into central storage and withdraws them through a cabin interface.

Acceptance:

- A cabin has one storage inventory after storage installation.
- Owners and residents can put items in storage, see its contents, and withdraw items. Guests cannot use or inspect storage.
- Storage has slots. Each slot holds one stack up to that item's Minecraft stack limit.
- An item that cannot stack uses one slot.
- Storage keeps each stack and its data through restart, packing, redeployment, and a missing exterior.
- Chests and other placed inventories keep their contents. Central storage does not inspect or move those items.

### S2: Increase capacity

Story: A cabin owner installs a storage upgrade and gets more storage slots.

Acceptance:

- Only storage upgrades add slots. Only the owner can install or configure them.
- If a capacity change does not complete, items in storage and the number of slots stay the same.
- Owners see storage settings, contents, and available slots. Residents see contents and available slots.
- Guests can see when storage has a problem, without item numbers or owner settings.

### S3: Fund one cabin upgrade

Story: A cabin owner selects one upgrade and moves its missing materials from central storage into its upgrade fund.

Acceptance:

- The owner starts the transfer for one target. Storage moves only items in that target's requirements that the fund does not have.
- Storage does not fill funds without an owner action. Storage does not install upgrades.
- A changed target or requirement stops the transfer. Missing items or fund space also stop it. No items move.

## Feature-wide constraints and acceptance

- Each transfer checks cabin identity, cabin state, player role, capacity, and selected stacks before items move.
- Each transfer moves all selected items or moves no items.
- If a transfer stops before it completes, recovery completes the transfer one time or moves no items.
- An open storage interface checks the player's role again before each transfer.

## Scope

The feature includes manual storage, capacity upgrades, storage status, and owner-approved transfers into one upgrade fund.

## Non-goals

- Automatic movement of items from placed inventories or between cabins.
- Production jobs that use storage items.
- Automatic upgrade funding or installation.

## Related records

- [Central storage](../../context.md#central-storage).
- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Upgrade funding decision](../../decisions/pdr/0007-fund-and-install-cabin-upgrades.md).
- [Household role brief](../B-0002/brief.md).
- [Mailbox brief](../B-0003/brief.md).

## Open questions and assumptions

- The initial number of slots, capacity upgrade steps, material requirements, and installation point are open.
- The problems that guests can see are open.
- The recovery rule for a transfer between player, fund, and storage inventories is open.
