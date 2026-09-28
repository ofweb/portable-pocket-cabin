# A cabin uses process profiles for optional mods

Status: Draft
Feature ID: B-0027

## Goal

An optional mod can add a safe production method. The cabin can operate without that mod.

## Stories and acceptance

### S1: Use a food process profile

Story: A player uses a process profile for an optional food mod to request a known meal.

Acceptance:

- For Farmer's Delight and Farmer's Delight Refabricated, process profiles list ingredients, meals, item parts, and cooking jobs the cabin can use.
- A process profile for Alex's Mobs Continued Delight can add ingredients and meals through a Farmer's Delight recipe sequence the cabin can use.
- A profile has rules only for the blocks that the profile lists. The profile keeps only the item data that the profile lists.
- The cabin gets a meal product template only after the meal enters central storage. The job must have the correct capability, inputs, output space, and time.
- If an optional mod is missing or the cabin cannot use its profile, those meals stay in storage. The cabin cannot make them, but the cabin starts.

### S2: Use a kiln or wood process profile

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

This feature includes food process profiles for optional mods, kiln jobs, charcoal jobs that use wood that the process profile lists, and coal production.

## Non-goals

- Automatic production does not use a modded recipe, block, entity, or item component without a process profile.
- Placed blocks from optional mods do not operate while the cabin is packed.

## Related records

- [Process profile](../../context.md#process-profile).
- [Cooking and brewing jobs brief](../B-0025/brief.md).
- [Crafting jobs brief](../B-0024/brief.md).
- [Mod compatibility](../../backlog.md#b-0011-optional-mod-compatibility).

## Open questions and assumptions

- The versions of each mod that the cabin can use, process profiles, the sequence of tiers, and work values are open.
- The wood source for charcoal jobs is open.
- The scope of coal production is open. This brief stays Draft until that scope has a decision.
