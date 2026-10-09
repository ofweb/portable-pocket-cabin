# ADR-0010: Give each arboretum a private world

Status: Accepted

## Context

Cabin interiors share a void dimension with 512-block cells. The arboretum's
woodland must extend far beyond one cell while staying separate from other
cabins. Its core and woodland must connect through ordinary walking. The
entrance door permits normal loading and transfer.

## Decision

Each installed arboretum has a permanent dimension key derived from its cabin
UUID. All four tiers use that world. The registry remains authoritative for
installation, tier, planting selection, and the saved generation seed and
version. World instances and loading tickets derive from that state.

Runtime worlds reuse a registered dimension type and keep separate Minecraft
chunk and entity storage. A narrow world-loading boundary owns access to the
server's level map and level construction. World resources can close after
saving when the arboretum has no live work or occupants. Reopening uses the
same identity and generation pattern.

The cultivated core stays at fixed coordinates. Main-room expansion moves
the cabin-side door with its west corridor. Return destinations derive from
the cabin's current geometry. Cabin entry does not load arboretum chunks;
arboretum entry loads its core before transfer.

## Rationale

A separate world provides isolation without a reachable boundary between
cabins. Keeping every tier there avoids moving cultivated contents when the
woodland tier opens. Ordinary chunk storage accommodates exploration without
putting terrain into the cabin registry.

## Scope

This decision applies to arboretum world identity, allocation, and loading
ownership. Other cabin rooms keep their current cells. It does not settle
woodland generation algorithms, fruit packaging, or reclaiming player blocks.

## Consequences

Runtime level integration needs explicit save, reopen, login, and shutdown
handling. Woodland occupants must participate in cabin permissions, packing,
recovery, and expansion checks. A dimension key alone does not grant access.

## Related records

- [Arboretum contract](../../features/B-0023/brief.md).
- [Registry authority](0005-authoritative-cabin-registry.md).
- [Safe travel](../pdr/0002-safe-cabin-travel.md).
- [World-loading boundary](../../../src/main/java/dev/portablepocketcabin/CabinArboretumWorlds.java).
- [Runtime-world experiment](../../../experiments/arboretum-worlds/README.md).
