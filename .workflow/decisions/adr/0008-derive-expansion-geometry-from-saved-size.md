# ADR-0008: Derive expansion geometry from saved size

Status: Accepted

## Context

Cabins have permanent cells with no shared space in the pocket dimension. Expansion must increase the protected interior without moving player blocks. Configuration changes must not reshape saved cabins.

## Decision

Each cabin saves its general-space size. The structure, usable bounds, clear height, and simulation tickets derive from that size and saved palette. The entrance wall stays fixed. Each one-block expansion adds a rear row and a side column. The side alternates with each expansion. Lateral geometry is deterministic and has no datapack control.

Sizes 4–5 give two blocks of clear height. Each two-size interval adds one block of height. Size 20 and larger give ten blocks. Expansion validates all new structure and usable-volume positions, with new height too, before world changes. An occupied position prevents expansion and keeps the earlier usable volume unchanged.

The configured maximum size cannot exceed the cell safety limit of 32. Cell assignment and spacing accommodate all supported sizes. Earlier fixed-height schemas reject migration when generated ceiling blocks cannot be safely distinguished from player blocks.

## Rationale

Saved size keeps geometry stable through restart and configuration changes. The fixed entrance keeps player paths and construction in place. Complete volume validation protects blocks above and beside the interior. The limit keeps cells from sharing space.

## Scope

This decision applies to general-space geometry and safe expansion. [ADR-0005](0005-authoritative-cabin-registry.md) states cell assignment and lifecycle authority. [PDR-0006](../pdr/0006-expand-general-space-with-world-materials.md) states player-visible growth. The [README](../../../README.md) states save migration rules.

Expansion must derive structure blocks and simulation bounds from saved size. An unsafe earlier save requires a backup and a new development world instead of automatic migration.
