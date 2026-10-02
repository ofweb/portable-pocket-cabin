# ADR-0008: Derive expansion geometry from saved size

Status: Accepted

## Context

Cabins have permanent cells with no shared space in the pocket dimension. Expansion must increase the protected interior without moving player blocks in the main room. Configuration changes must not reshape saved cabins. Room growth needs reserved space beyond the main room.

## Decision

Each cabin saves its general-space size. The structure, usable bounds, clear height, and simulation tickets derive from that size and saved palette. The main-room center stays fixed. Each expansion adds one row or column on each of the four sides. Wall-centered openings and the south entrance move outward with their walls. Main-room player blocks stay in place. Lateral geometry is deterministic and has no datapack control.

Clear height derives from saved general size: two blocks at size 3, increasing by one for each two-block size step, capped at ten from size 19. Expansion validates all new structure and usable-volume positions, with new height too, before world changes. Player blocks that conflict with required structure prevent expansion and keep the earlier usable volume unchanged. Generated cabin structures require separate treatment from player construction.

Cell allocation must reserve the maximum supported main-room footprint, corridors, and specialized-room growth. The intended main-room maximum is 21. Each specialized room has reserved space for its own maximum growth. The complete footprint and required spacing depend on the room limits and layout. Main-room size alone cannot establish a safe allocation limit. Earlier fixed-height schemas reject migration when generated ceiling blocks cannot be safely distinguished from player blocks.

## Rationale

Saved size keeps geometry stable through restart and configuration changes. A fixed center gives symmetric growth and centered passages without moving main-room construction. Complete volume validation protects blocks above and beside the interior. Reserving the full cabin footprint keeps cabins from sharing space as rooms grow.

## Scope

This decision applies to general-space geometry and safe expansion. [ADR-0005](0005-authoritative-cabin-registry.md) states cell assignment and lifecycle authority. [PDR-0006](../pdr/0006-expand-general-space-with-world-materials.md) states player-visible growth. The [README](../../../README.md) states save migration rules.

Expansion must derive structure blocks and simulation bounds from saved size. An unsafe earlier save requires a backup and a new development world instead of automatic migration.

The [relocation experiment](../../../experiments/room-relocation/README.md) supports moving tested vanilla contents and recovering a saved partial move. It does not establish complete cabin transaction recovery or compatibility with external state. The [room brief](../../features/B-0005/brief.md) retains those unverified assumptions.
