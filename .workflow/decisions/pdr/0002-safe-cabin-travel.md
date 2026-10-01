# PDR-0002: Fail safely during cabin travel

Status: Accepted

## Context

Players can enter, exit, pack, and recover a cabin across dimensions. A failed move must not leave players without safe access.

## Decision

Cabin travel requires a safe, loaded destination with solid ground, clear player space, and no dangerous fluid or fire. The position must be inside the world border. Destination checks have work limits and release temporary chunk tickets.

Emergency travel tries these destinations in order:

- The active exterior doorway.
- A safe position near that doorway in the same dimension.
- The last safe site.
- Overworld world spawn.

Packing stops before exterior removal if a player in the cabin cannot exit safely or packed item delivery cannot be confirmed. Entry stays disabled during packing. Players who disconnect inside a cabin without an active exterior receive a safe destination when they reconnect.

## Rationale

A failed move keeps the last usable cabin state and gives players a safe route home.

## Scope

This decision applies to entry, exit, packing, recovery after a disconnect, and emergency travel. [PDR-0003](0003-cabin-home-respawning.md) states the different respawn order. Connections between cabins must keep a safe exit or move affected players to safe positions.

Travel checks permissions and cabin state again before completion. Packing and connection changes at the same time cannot leave a player without an exit or create two active entrances.
