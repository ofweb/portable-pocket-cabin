# Players can rely on tested release documentation

Status: Draft
Feature ID: B-0035

## Goal

Documentation describes tested survival behavior, multiplayer recovery, optional content, save migration, and extension formats.

## Stories and acceptance

The stories below are proposals for Shape.

### S1: Use a tested version

Story: Players use the documented versions and keep their cabin state through multiplayer actions and restart.

Acceptance:

- Fresh survival play reaches every completed feature without operator commands.
- Release checks cover the declared compatibility matrix with optional content individually, together, and removed under documented policies.
- Repeated packing, restart, migration, and interrupted actions keep cabin identity, ownership, resources, and lifecycle rules.
- Concurrent entry, packing, storage, automation, and network actions keep those same rules.
- Administrator recovery repairs damaged projections and stale items without deleting interiors, rooms, storage, or managed residents.
- The cabin accepts valid extension data. Missing or malformed datapack and profile data fails early with a useful reason.

### S2: Find accurate version limits

Story: A player or extension author reads documentation and knows which versions, migrations, and formats the version supports.

Acceptance:

- Documentation names exact tested versions, known incompatibilities, save-migration limits, and extension schemas.
- Public compatibility claims agree with completed verification.
- Release acceptance includes automated checks and manual survival, interface, audio, visual, and multiplayer checks.
- Feature acceptance remains valid after later changes. Verification does not rely only on the first successful check.

## Scope

This feature includes release verification, multiplayer and restart coverage, administrative recovery checks, and published support documentation.

## Non-goals

Untested compatibility claims and new feature behavior are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Optional compatibility](../B-0011/brief.md).
- [Survival balance](../B-0033/brief.md).
- [Presentation](../B-0034/brief.md).

## Open questions and assumptions

- Release versions, test combinations, multiplayer duration, migration policies, and extension formats are open.
