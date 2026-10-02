# A cabin makes meals and potions with jobs

Status: Draft
Feature ID: B-0025

## Goal

A cabin can make known meals and potions through process profiles that the cabin can use. Each job has a work time.

## Stories and acceptance

### S1: Prepare a meal

Story: An owner or resident requests a known meal and receives it after a cooking job.

Acceptance:

- The cabin gets a safe meal product template only after that meal enters central storage. A recipe does not give the cabin a product template.
- A job must have a process profile for cooking, its installed capability, the ingredients in that profile, output space, and work time.
- Cooking jobs require the kitchen room in B-0041. Manual kitchen use does not require cooking automation.
- The player sees the selected process profile, inputs, items the job gives back, other items the job makes, work time, and reason for each pause.
- Cooking jobs do not make their ingredients. They do not place seeds, harvest, feed, or slaughter.

### S2: Brew a potion

Story: An owner or resident requests a known potion and receives the potion after a brewing job.

Acceptance:

- The cabin gets a potion product template only after the potion enters central storage. The product template keeps only approved potion identity and effects.
- A job must have a process profile for brewing, bottles, ingredients, its installed capability, output space, and work time.
- Brewing jobs require the potion room in B-0043. Manual brewing does not require brewing automation.
- A potion with data that is not standard or with mod data must have a safe profile that lists its parts.
- Without that profile, the potion stays in storage, but the cabin cannot make that potion.

## Feature-wide constraints and acceptance

- Owners and residents can request local jobs. Guests cannot request jobs or use storage resources.
- Each cooking or brewing job uses its process profile's work time. It does not use the crafting work rule.
- Jobs use the crafting feature's reservation, capacity, cancellation, pause, packed-time, and recovery rules.
- If an optional process profile is missing or the cabin cannot use it, only its jobs stop and give a reason.

## Scope

This feature includes cooking and brewing process profiles that the cabin can use without optional mods. The feature includes meal and potion product templates and local jobs.

## Non-goals

- Room actions that make ingredients, feeding, slaughter, or jobs for blocks without a process profile.
- Jobs for optional mods, kiln work, or coal production.

## Related records

- [Process profile](../../context.md#process-profile).
- [Crafting jobs brief](../B-0024/brief.md).
- [Known items brief](../B-0007/brief.md).
- [Production profiles](../B-0027/brief.md).
- [Manual kitchen room](../../backlog.md#b-0041-manual-kitchen-room).
- [Manual potion room](../../backlog.md#b-0043-manual-potion-room).

## Open questions and assumptions

- Cooking and brewing process profiles that the cabin can use without optional mods are open. Safe meal and potion parts are open.
- Work times and other work values are open.
