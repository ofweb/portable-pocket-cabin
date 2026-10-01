# PDR-0001: Keep the portable home

Status: Accepted

## Context

Players build and store valuable things in the cabin. Moving or losing an exterior must not delete or copy the home.

## Decision

Each cabin keeps one lasting interior and one active exterior at most. Packing moves access to the home. It does not move the interior or construction outside the cabin.

Interior blocks follow Minecraft behavior while the cabin is deployed or players are in a connected packed cabin. They pause when the packed cabin is empty. On return, the cabin calculates progress for the time it was empty. Only time while the server operates counts. Progress for that time includes rooms, jobs, and placed blocks such as furnaces and crops. [Direction](../../direction.md#progress-after-an-empty-cabin-becomes-occupied) must confirm the progress limits. Rooms can define progress limits for their fixtures while the cabin has no active exterior.

The interior boundary keeps players in their cabin. The exterior protects only blocks that belong to the cabin. Packing does not remove player construction near it.

Recovery keeps the cabin after exterior loss, item loss, or a move that stops before completion. A copied or incorrect packed item cannot create a different entrance or modify cabin data.

## Rationale

A lasting interior keeps Minecraft construction and storage through travel. One active exterior prevents the same home from appearing at two sites. Construction outside the cabin stays at its site.

## Scope

This decision applies to the cabin lifecycle, rooms, connections, and storage. [Direction](../../direction.md) states the travelling-home goal. [ADR-0005](../adr/0005-authoritative-cabin-registry.md) states the saved data model.

All features must keep cabin identity and interior contents through packing, restart, and recovery. Interior blocks do not simulate while a packed cabin is empty.
