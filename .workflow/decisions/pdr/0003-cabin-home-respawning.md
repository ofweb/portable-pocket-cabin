# PDR-0003: Resolve cabin-home respawning safely

Status: Accepted

## Context

A player can sleep in a cabin and later die after that cabin moves, packs, or loses its bed. A cabin-home binding cannot assume that the original bed remains safe.

## Decision

Successful cabin sleep binds the owner to that cabin and bed. The most recently used cabin bed replaces the earlier cabin-home binding. A trusted visitor can sleep without changing the visitor's existing home.

Respawning first tries a safe position beside the bound bed in a deployed cabin. If the bed is missing or blocked, it tries the current exterior doorway. For an inactive cabin, it searches near the death position in the death dimension. If that bounded search fails, it tries the last valid campsite and then Overworld world spawn. It never selects the pocket dimension as an emergency spawn.

## Rationale

The cabin remains a useful home even when it travels. The fallback order favors the current home, then a nearby safe return, before using distant world spawn. Bounded search avoids delaying respawn indefinitely.

## Scope

This decision applies to cabin-home bindings and respawning. [PDR-0002](0002-safe-cabin-travel.md) defines the shared destination safety rules.

## Consequences

The server rechecks cabin state and destination safety before respawn. Concurrent packing cannot return a player to an inactive cabin. Operators can configure the near-death search bounds.
