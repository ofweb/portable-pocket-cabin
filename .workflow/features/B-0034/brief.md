# Cabin controls and appearance communicate their purpose

Status: Draft
Feature ID: B-0034

## Goal

Players can identify cabin systems and understand available actions, costs, status, and failures.

## Stories and acceptance

The stories below are proposals for Shape.

### S1: Understand a cabin action

Story: A player inspects a cabin control and understands its action and any reason it cannot complete.

Acceptance:

- Protected interfaces use consistent controls and actionable failure messages.
- Status shows only information allowed by the player's role.
- Models, textures, sounds, and tooltips distinguish interactive systems from decoration without hiding Minecraft behavior.
- Presentation does not weaken server checks or conceal important state.

### S2: Read exterior conditions through a window

Story: A player sees window cues for the deployed cabin's exterior conditions or inactive state.

Acceptance:

- Windows keep the existing exterior-condition display and saved window behavior.
- Proposed improvements include textures, transitions, biome cues, and weather animation.
- Windows do not render terrain views or cross-dimensional portals.

## Scope

This feature includes controls, status, failure messages, window presentation, models, textures, sounds, and tooltips.

## Non-goals

Rendered portals, terrain views, and changes to permissions or cabin behavior are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Window behavior](../../decisions/pdr/0008-purchase-and-reverse-cabin-windows.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Release verification](../B-0035/brief.md).

## Open questions and assumptions

- Visual and audio designs, interface dimensions, and the acceptance method for clarity are open.
