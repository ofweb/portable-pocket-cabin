# Central storage

Status: Draft
Feature ID: B-0004

## Goal

Give each cabin one lasting inventory for household use while ordinary placed inventories remain independent.

## Stories and acceptance

### S1: Store household items

Story: As an owner or resident, I can place items in central storage and retrieve them through a protected cabin interface.

Acceptance:

- One cabin owns one authoritative storage inventory after the storage feature is installed.
- Owners and residents can deposit, browse, and withdraw. Guests cannot use or inspect central storage.
- Storage capacity uses Minecraft-style slots. Each item obeys its normal stack limit. Unique or non-stackable items use one slot each.
- Storage and exact stack contents survive restart, packing, redeployment, and exterior loss.
- Ordinary chests and other placed inventories remain independent. The cabin does not scan or merge their contents.

### S2: Increase capacity

Story: As an owner, I can install a storage upgrade to increase the cabin's available slots.

Acceptance:

- Only storage upgrades increase capacity. Only the owner can install or configure them.
- A failed capacity change leaves stored items and capacity unchanged.
- Exact storage and configuration status is visible to the owner. Residents see exact shared-storage status. Guests see only plain-language warnings.

### S3: Fund one cabin upgrade

Story: As an owner, I can authorize storage to provide the missing materials for one selected cabin upgrade.

Acceptance:

- Existing materials in the selected upgrade fund count first.
- The owner approves the transfer for one target. Storage supplies only that target's remaining exact requirements.
- Storage does not fill funds or install upgrades without an owner action.
- A stale target, changed requirement, missing material, or insufficient space rejects the transfer without moving items.

## Feature-wide constraints and acceptance

- Each transfer checks cabin identity, lifecycle, current role, capacity, and offered stacks before it changes state.
- A rejected or interrupted transfer leaves all involved inventories unchanged or completes once after recovery.
- A role change invalidates an open storage interaction before another change commits.
- Later facilities may consume storage resources only through their own permitted actions.

## Scope

This feature includes protected manual storage use, capacity upgrades, status, and owner-approved funding from storage.

## Non-goals

- Automatic scanning of ordinary inventories, production automation, reserves, or sharing between cabins.
- Automatic upgrade funding or installation.
- Final interface art and layout.

## Related records

- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Upgrade funding decision](../../decisions/pdr/0007-fund-and-install-cabin-upgrades.md).

## Open questions and assumptions

- Open: Set the initial slot count, upgrade levels, costs, and installation point.
- Open: Define which status warnings a guest may see without revealing exact quantities.
- Open: Confirm recovery behavior for transfers between player, fund, and storage inventories.
