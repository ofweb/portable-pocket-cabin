# A cabin makes known products through crafting jobs

Status: Draft
Feature ID: B-0024

## Goal

Owners and residents can request a known product. The cabin shows a production plan, commits resources, and makes the product after work is complete.

## Stories and acceptance

### S1: Select a production plan

Story: An owner or resident requests a local product and selects one complete production plan before work starts.

Acceptance:

- A request must have a safe product template, recipes the cabin can use, an installed automation capability, a tier, inputs, and output space.
- A production plan can make intermediate products. It must follow limits on recipe depth, the number of operations, and output quantity.
- A production plan cannot have cycles or a recipe sequence that creates a net gain of resources. The check includes all containers the job gives back and by-products.
- The cabin shows inputs, intermediate products, items the job gives back, by-products, hard reserves, total work, work time, and output space.
- When more than one recipe sequence is possible, the player sees each complete plan and selects one. Item positions in storage do not select a plan.
- A production plan does not commit inputs or output space. The cabin commits resources before it creates a production job.
- Owners and residents can request jobs. Guests cannot request jobs or use central storage resources.
- If a player becomes a guest before the cabin commits resources, the request stops. A committed job keeps its resources but pauses before a decision that a guest cannot make.

### S2: Complete and recover a job

Story: A player starts a selected plan. The cabin shows completed work, output, or a reason for a pause.

Acceptance:

- Before the first operation, the cabin makes a job reservation for all inputs.
- The job reservation also includes space for outputs, items the job gives back, and by-products.
- Other jobs and players cannot use committed inputs.
- Each crafting operation has a recipe work value plus one unit of work for each ingredient unit the operation uses up.
- Each batch and intermediate adds work. The installed tier has a work rate for each game tick.
- A job does not create its output at request time.
- A completed job places all outputs together or places none.
- Cancellation or permanent failure puts inputs that the job did not use back into the same cabin storage and frees output space.
- The cabin saves the selected plan, committed inputs, output space, completed work, and pause reason through restart, packing, and redeployment.
- Packed jobs complete operations only for game time between job checks, up to time and operation limits. The cabin does not load cabin chunks.
- Jobs do no game-time work while the server is off. Placed blocks do no work while the cabin is packed.

### S3: Keep hard reserves

Story: An owner selects a hard reserve for an item. Jobs keep that quantity in storage before the jobs commit inputs.

Acceptance:

- Only the owner can make a change to a hard reserve. Jobs cannot commit inputs if storage falls below the hard reserve.
- Owners and residents can withdraw items below a hard reserve.
- A new hard reserve does not remove a job's committed inputs.
- A blocked job keeps its request and committed inputs. Local status shows one clear reason to owners and residents.

## Feature-wide constraints and acceptance

- The cabin creates a job only after a player request or a trigger that the owner confirms in a different feature.
- The cabin has no general production mode.
- Jobs can pause if inputs or product templates are missing, a recipe has more than one plan, or an automation capability is off.
- Hard reserves, full output space, tier limits, and packed work limits can also pause jobs.
- After an interruption, the cabin can recover completed work and resource quantities. The cabin does not make the same output two times.

## Scope

This feature includes local crafting plans, hard reserves, jobs with work time, cancellation, status, and recovery.

## Non-goals

- Cooking, brewing, room actions, stock targets, upgrade funding plans, and jobs in a different cabin.
- Jobs do not operate furnaces or other placed blocks.

## Related records

- [Production plan](../../context.md#production-plan).
- [Production job](../../context.md#production-job).
- [Minimum item quantity](../../context.md#hard-reserve).
- [Job reservation](../../context.md#job-reservation).
- [Known items brief](../B-0007/brief.md).
- [Central storage brief](../B-0004/brief.md).
- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Cabin decision](../../decisions/pdr/0001-preserve-the-portable-home.md).

## Open questions and assumptions

- Plan limits, recipe work values, tier throughput, and catch-up limits are open.
- The set of recipes that the cabin can use and the job cancellation control are open.
