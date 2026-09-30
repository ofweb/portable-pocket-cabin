# PDR-0001: Preserve the portable home

Status: Accepted

## Context

The cabin moves between campsites, but players build and store valuable things inside it. Moving or losing an exterior must not erase the home or copy its contents.

## Decision

Each cabin keeps one lasting interior and can have at most one active exterior. Packing moves access to the home, not the interior itself. The surrounding campsite stays where it is. Ordinary interior blocks behave normally while the cabin is deployed or a connected packed cabin is occupied. They pause when the packed cabin is empty. On return, the cabin calculates progress for the time it was empty. Only time while the server operates counts. Progress for that time includes rooms, jobs, and placed blocks such as furnaces and crops. [Direction](../../direction.md#progress-after-an-empty-cabin-becomes-occupied) must confirm the progress limits. A managed room can define bounded inactive progress for its own fixtures.

The protected interior keeps players within their cabin. Its exterior protects only cabin-owned blocks and does not remove nearby player construction when packed.

The cabin remains recoverable after exterior loss, item loss, or an interrupted move. A stale or duplicated packed item cannot create another entrance or alter cabin-owned data.

## Rationale

Players need to trust the cabin with their home. A permanent interior preserves ordinary Minecraft construction and storage. One active exterior prevents the same home from appearing at two sites. Keeping outdoor construction local preserves the value of each campsite.

## Scope

This decision applies to every cabin lifecycle and to later rooms, connections, and storage features. The [Direction](../../direction.md) owns the long-term travelling-home goal. [ADR-0005](../adr/0005-authoritative-cabin-registry.md) owns the persistent technical model.

## Consequences

New features must preserve cabin identity and interior contents through packing, restart, and recovery. Ordinary interior blocks do not simulate while a packed cabin is empty.
