# PDR-0003: Resolve cabin-home respawning safely

Status: Accepted

## Context

A player can sleep in a cabin and die after the cabin moves, packs, or loses its bed. Respawn must confirm bed safety again.

## Decision

Successful cabin sleep sets the owner's home to that cabin and bed. The last used cabin bed replaces the earlier cabin-home binding. A resident visitor can sleep without replacing the visitor's home.

Respawning tries these destinations in order:

- A safe position near the bed in a deployed cabin.
- The active exterior doorway if the bed is missing or blocked.
- For a cabin without an active exterior, a safe position near the death position in that dimension.
- The last safe site if the search reaches its limit without a destination.
- Overworld world spawn.

Emergency respawn never selects the pocket dimension.

## Rationale

The order gives priority to the player's home and then a safe position near where they died. Search limits prevent a respawn delay without an end.

## Scope

This decision applies to cabin-home bindings and respawning. [PDR-0002](0002-safe-cabin-travel.md) states destination safety rules.

The server checks cabin state and destination safety again before respawn. Packing at the same time cannot return a player to a cabin without an active exterior. Operators can configure the search bounds near the death position.
