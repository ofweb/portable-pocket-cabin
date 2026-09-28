# A cabin can do selected room actions

Status: Draft
Feature ID: B-0026

## Goal

An owner selects which room actions operate. Manual room actions stay available.

## Stories and acceptance

### S1: Select one room action

Story: An owner installs one automation capability for a room and selects it to operate. The cabin then does only that action.

Acceptance:

- Only the owner can select a room action to operate. Only the owner can stop that action.
- Each action must have a different installed automation capability.
- Greenhouse harvesting and replanting have different automation capabilities.
- The cabin must have different capabilities to feed livestock, collect products without slaughter, and slaughter surplus livestock.
- The cabin must have different capabilities to fell trees and place saplings.
- Players can use a room through manual actions when its automation capability is off or missing.

### S2: Stop room work at limits

Story: An owner checks an active room and sees a reason when work stops for missing resources or a limit.

Acceptance:

- An action starts only if the room has its inputs, space for outputs, and safe conditions.
- A replant action removes seeds or saplings from available inputs. No room action creates its inputs.
- Each action follows the room's population and safety limits.
- If an action moves items between room fixtures and central storage, it counts inputs and outputs.
- An action pauses at a missing input, full output, hard reserve, population limit, safety limit, or catch-up limit.
- The cabin gives a permitted player a clear reason for each pause.
- Room work follows that room's active and packed-time rules. Packing does not operate placed blocks.
- The cabin saves completed work and resource quantities through packing, restart, and redeployment. The cabin does not make the same output two times.

## Feature-wide constraints and acceptance

- Room actions follow the job feature's work limits and resource rules when those rules apply.
- An action starts only after the owner selects its automation capability to operate.

## Scope

This feature includes greenhouse, livestock, and forestry actions and the item transfers for those actions.

## Non-goals

- Room installation, new manual room rules, and actions for other blocks or animals.
- Charcoal or coal production, local stock targets, and work between cabins.

## Related records

- [Room brief](../B-0005/brief.md).
- [Known items and books brief](../B-0007/brief.md).
- [Crafting jobs brief](../B-0024/brief.md).
- [Greenhouse item](../../backlog.md#b-0019-greenhouse).
- [Livestock room item](../../backlog.md#b-0022-livestock-room).
- [Forestry room item](../../backlog.md#b-0023-forestry-room).

## Open questions and assumptions

- Each room's population, safety, fixture, and packed-time rules are open.
- The action set and item transfer rules for each room are open.
