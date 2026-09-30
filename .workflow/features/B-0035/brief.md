# Players can use documents for a tested version

Status: Draft
Feature ID: B-0035

## Goal

Documents state behavior in Survival mode, multiplayer recovery, optional mod features, save migration, and extension formats after completed checks.

## Stories and acceptance

Shape must confirm these stories.

### S1: Use a tested version

Story: Players use the documented versions and keep their cabin state through multiplayer actions and restart.

Acceptance:

- A player can use all completed features in a new Survival mode world without operator commands.
- Version checks include one optional mod at a time, mods together, and removed as stated in their documented policies.
- Packing, restart, migration, and actions that stop before they complete keep cabin identity, ownership, resources, and lifecycle rules after more than one use.
- Entry, packing, storage, automation, and network actions by players at the same time keep those same rules.
- Operator recovery repairs damaged [generated cabin structures](../../context.md#generated-cabin-structure) and items with incorrect saved state. It does not delete interiors, rooms, storage, or room animals.
- The cabin accepts correct extension data. Missing or incorrect datapack and profile data stops before use and gives a reason.

### S2: Find accurate version limits

Story: A player or player who creates an extension reads documents and identifies the available versions, migrations, and formats.

Acceptance:

- Documents identify versions with completed checks, mods that cannot operate together, save-migration limits, and extension schemas.
- Compatibility information for players agrees with completed checks.
- Version acceptance includes automatic checks and manual checks for Survival mode, interfaces, sounds, displays, and multiplayer behavior.
- Feature acceptance applies after changes. Checks must complete without errors before and after changes.

## Scope

This feature includes version checks, multiplayer and restart checks, operator recovery checks, and documents that state available features and compatibility.

## Non-goals

Compatibility information without completed checks and new feature behavior are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Optional compatibility](../B-0011/brief.md).
- [Balance for Survival mode](../B-0033/brief.md).
- [Player interfaces and displays](../B-0034/brief.md).

## Open questions and assumptions

- Versions, test groups, time for multiplayer checks, migration policies, and extension formats are open.
