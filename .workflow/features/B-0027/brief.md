# A cabin uses process profiles for optional mods

Status: Draft
Feature ID: B-0027

## Goal

An optional mod can add a safe production method. The cabin can operate without that mod.

## Stories and acceptance

### S1: Use a kiln or wood process profile

Story: An owner selects a kiln or wood process profile and sees local jobs make its outputs from its inputs.

Acceptance:

- Kiln jobs and charcoal jobs each have a process profile. A charcoal job uses wood that its profile lists.
- Each profile lists inputs, outputs, items the job gives back, work time, tier, and capability rules.
- A coal process profile must have a tier that follows kiln work and listed renewable inputs.
- Coal in storage and the coal product template do not install the coal capability.
- Each process profile uses a job reservation for inputs and output space. Its jobs follow the job feature's pause, recovery, and packed-time limits.
- If the cabin cannot use a process profile, only jobs for that profile stop and give a reason.

## Feature-wide constraints and acceptance

- Each optional process profile applies only to the mods and item variants that the profile lists.
- If an optional mod is missing, central storage and jobs without that mod are available. The cabin starts.

## Scope

This feature includes kiln jobs, charcoal jobs that use wood that the process profile lists, and coal production.

## Non-goals

- Automatic production does not use a modded recipe, block, entity, or item component without a process profile.
- Placed blocks from optional mods do not operate while the cabin is packed.
- Food process profiles for Farmer's Delight and its add-ons are retired. Cabin cooking replaces them under [PDR-0015](../../decisions/pdr/0015-build-cooking-into-the-cabin.md).

## Related records

- [Process profile](../../context.md#process-profile).
- [Cooking and brewing jobs brief](../B-0025/brief.md).
- [Crafting jobs brief](../B-0024/brief.md).
- [Mod compatibility](../../backlog.md#b-0011-optional-mod-compatibility).

## Open questions and assumptions

- The versions of each mod that the cabin can use, process profiles, the sequence of tiers, and work values are open.
- The wood source for charcoal jobs is open.
- The scope of coal production is open. This brief stays Draft until that scope has a decision.
