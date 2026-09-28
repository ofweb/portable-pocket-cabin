# ADR-0008: Derive expansion geometry from saved size

Status: Accepted

## Context

Cabins occupy separate permanent cells in one pocket dimension. An expansion must enlarge a protected shell without moving player blocks. A changed configuration must not reshape saved cabins.

## Decision

Each cabin saves its general-space size. The shell, usable bounds, clear height, and simulation tickets derive from that size and the saved palette. The entrance wall remains fixed. Each one-block expansion adds a rear row and an alternating side column. Lateral geometry is deterministic and has no datapack control.

Sizes 4–5 have two blocks of clear height. Each two-size interval adds one block of height. Size 20 and larger have ten blocks. The expansion checks every new shell and usable-volume position, including new height, before world mutation. It rejects an occupied position without changing the existing usable volume.

The configured maximum cannot exceed the absolute cell-safety cap of 32. Cell allocation and spacing remain sufficient for every supported size. Saves from the earlier fixed-height schema fail closed when safe migration cannot distinguish generated ceiling blocks from player blocks.

## Rationale

Saved size makes reconstruction stable after restart and configuration changes. Fixed entrance geometry preserves navigation and player construction. Complete volume checks protect blocks above and beside the current room. The cap keeps distinct cabin cells isolated.

## Scope

This decision covers general-space geometry and safe expansion. [ADR-0005](0005-authoritative-cabin-registry.md) owns cell allocation and lifecycle authority. [PDR-0006](../pdr/0006-expand-general-space-with-world-materials.md) owns the player-visible growth rule. The [README](../../../README.md) states the current save migration matrix.

## Consequences

Expansion must use the saved size when it computes shell blocks and simulation bounds. An unsafe old save needs a backup and a fresh development world instead of an automatic conversion.
