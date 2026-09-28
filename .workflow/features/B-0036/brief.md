# A cabin keeps local stock targets

Status: Draft
Feature ID: B-0036

## Goal

An owner selects a local stock quantity that starts bounded production for a known product.

## Stories and acceptance

### S1: Create a stock target

Story: An owner selects a quantity for a known product and sees the plan that the cabin will use.

Acceptance:

- Only the owner can add, change, pause, or remove a stock target.
- The target has one known safe product and one recipe or process profile that the cabin can use. The owner selects the plan.
- The cabin shows inputs, outputs, work, space for outputs, and each hard reserve.
- Owners and residents can see completed work, job status, and the reason work stops. Guests cannot see storage quantities or settings.
- A target and its selected plan stay saved through packing, restart, and redeployment.

### S2: Keep local stock at its target

Story: A cabin with an active target finds its quantity in storage below the target and starts local jobs.

Acceptance:

- A target starts work for its missing quantity only. Each job follows job and output limits.
- The cabin does not select a plan from item positions in storage or the registry sequence.
- If the selected plan is not clear or the cabin cannot use it, the target stops and gives a reason.
- The target follows installed capabilities, owner hard reserves, inputs, tiers, work time, and output capacity.
- A blocked target keeps its settings and shows the reason work stops.
- After an owner removes or changes a target, the active job keeps committed resources.
- Owners and residents can withdraw items. If stock falls, the cabin can do more work. The same limits apply.

## Feature-wide constraints and acceptance

- A target makes items only for local central storage. It does not send stock to a different cabin.
- The target uses the same job recovery and packed-time rules as jobs that players request.

## Scope

This feature includes owner-configured local stock triggers and their status.

## Non-goals

- Production without a stock target, automatic mailbox delivery, surplus delivery, and jobs in a different cabin.
- Automatic selection between two plans for the same product.

## Related records

- [Stock target](../../context.md#stock-target).
- [Crafting jobs brief](../B-0024/brief.md).
- [Known items brief](../B-0007/brief.md).
- [Central storage brief](../B-0004/brief.md).

## Open questions and assumptions

- The target limit, quantity range, and time between checks for missing items are open.
- The rule for items in an active job reservation when the cabin checks stock is open.
