# Players can identify cabin actions and status

Status: Draft
Feature ID: B-0034

## Goal

Players can identify cabin systems and the available actions, costs, status, and failures.

## Stories and acceptance

Shape must confirm these stories.

### S1: Identify a cabin action

Story: A player inspects a cabin interface and identifies its action and the reason it cannot complete.

Acceptance:

- Cabin interfaces use the same buttons for the same actions. Failure information identifies the problem and the actions necessary to complete the request.
- Status shows only information that follows the player role rules.
- Models, textures, sounds, and tooltips identify systems that players can use and other blocks. Players can see the Minecraft behavior of each block.
- Changes to displays, sounds, and interfaces do not bypass server checks or prevent players from inspecting important state.

### S2: Read exterior conditions through a Cabin window

Story: A player sees exterior conditions or state without an active exterior through a Cabin window.

Acceptance:

- Each Cabin window keeps its exterior-condition display and saved behavior.
- Possible changes include textures, display changes with time, biome information, and weather animation.
- A Cabin window does not show a view of exterior terrain or into a different dimension.

## Scope

This feature includes controls, status, failure information, Cabin window displays, models, textures, sounds, and tooltips.

## Non-goals

Views of exterior terrain or into different dimensions, and changes to permissions or cabin behavior, are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Cabin window behavior](../../decisions/pdr/0008-purchase-and-reverse-cabin-windows.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Version checks](../B-0035/brief.md).

## Open questions and assumptions

- Displays, sounds, interface dimensions, and the acceptance method for clear player information are open.
