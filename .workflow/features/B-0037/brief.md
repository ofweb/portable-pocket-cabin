# A cabin can fund an upgrade through production

Status: Draft
Feature ID: B-0037

## Goal

Let an owner confirm a local production plan for one upgrade's missing materials without installing the upgrade.

## Stories and acceptance

### S1: Confirm a funding plan

Story: An owner opens one upgrade fund, sees its missing materials, and confirms a plan to make those materials.

Acceptance:

- Only the owner can confirm a production plan or select a new plan for an upgrade fund.
- The cabin first counts materials in the selected fund. The plan includes only the missing quantities.
- The plan shows recipes, inputs, intermediates, items the jobs give back, by-products, hard reserves, time, and output capacity.
- If more than one complete recipe plan is available, the owner selects one before work starts.
- An unknown product, missing capability, inputs, tier, capacity, or a safe plan blocks the request and gives a reason.
- The plan stays bound to its selected upgrade target. A changed target or requirement cannot receive its output.

### S2: Fill the selected fund

Story: A cabin completes confirmed jobs and puts their materials into only the selected upgrade fund.

Acceptance:

- A job puts only the missing quantity for each material into the fund.
- Other upgrade funds and general storage cannot receive the selected fund's output in error.
- Job inputs, work, and reasons to stop follow the bounded crafting rules. Cancellation and recovery follow the same rules.
- After packing, restart, or a transfer that stops, the fund cannot receive the same materials two times. Committed inputs stay saved.
- A complete fund does not install the upgrade. The owner must use the installation action.

## Feature-wide constraints and acceptance

- Production follows the owner's hard reserve and does not use materials committed to a different fund or job.
- The fund keeps the same rules when players contribute materials, withdraw materials, or install the upgrade.

## Scope

This feature includes local production that the owner confirms for one selected upgrade fund.

## Non-goals

- Automatic upgrade installation, fund selection, or production for all funds that have missing materials.
- A transfer from a different cabin or production in a different cabin.

## Related records

- [Production plan](../../context.md#production-plan).
- [Production job](../../context.md#production-job).
- [Crafting jobs brief](../B-0024/brief.md).
- [Upgrade funding decision](../../decisions/pdr/0007-fund-and-install-cabin-upgrades.md).
- [Upgrade fund architecture](../../decisions/adr/0002-use-target-keyed-upgrade-funds.md).
- [Central storage brief](../B-0004/brief.md).

## Open questions and assumptions

- The result when an upgrade target has new requirements during a job is open.
- The place for completed output that cannot enter the fund after cancellation is open.
