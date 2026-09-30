# Optional mod features have compatibility limits

Status: Draft
Feature ID: B-0011

## Goal

Players can use optional mod features that have profiles. The cabin operates without those mods.

## Stories and acceptance

Shape must confirm these stories.

### S1: Use a mod profile

Story: A player opens a world with mods that have profiles and uses the cabin features for those mods.

Acceptance:

- Compatibility information identifies versions and groups of mods that completed checks. A profile without test results does not confirm compatibility.
- Checks without optional mods include all completed cabin features.
- Checks for `Biomes O' Plenty` include Cabin palette, door, and World attunement profiles.
- Checks for Farmer's Delight Refabricated include crops, `Rich Soil`, cooking and storage blocks, and cooking profiles.
- `Tom's Simple Storage` networks in cabins do not connect to central storage.
- Checks for `Alex's Mobs Continued` include stable residents and livestock types. Only types with profiles can use cabin actions.
- Checks for Alex's Mobs Continued Delight include only ingredients, meals, and process profiles that the cabin can use.
- Placed blocks and block entities use Minecraft persistence. Cabin actions for entities, item data, and production must have profiles.

### S2: Missing optional mods

Story: A player opens or loads a copy of a world without an optional mod, as stated in its removal policy.

Acceptance:

- Missing mods or profiles stop only their features. The cabin operates, and features without those mods can operate.
- Missing mods or profiles keep World attunement and saved state. They do not replace saved materials without player information.
- Compatibility checks include one mod at a time, groups of mods, and mod removal that follows each documented policy.
- Failure information identifies the feature and the reason it cannot operate.

## Scope

This feature includes mod lists, profile limits, behavior without optional mods, and compatibility information from completed checks.

## Non-goals

The cabin does not create production or entity rules from internal mod behavior without profiles. Only documented mod features are in scope.

## Related records

- [Direction](../../direction.md).
- [World attunement](../../context.md#world-attunement).
- [Optional production profiles](../B-0027/brief.md).
- [Version checks](../B-0035/brief.md).

## Open questions and assumptions

- Mods in this brief are targets for compatibility checks. Available versions, profiles, groups of mods, and removal policies are open.
- This brief does not confirm that these mods operate with the project version.
