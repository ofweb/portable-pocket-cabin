# PDR-0002: Fail safely during cabin travel

Status: Accepted

## Context

Players can enter, leave, pack, and recover a cabin across dimensions. A failed move or blocked destination can strand a player or remove the only usable entrance.

## Decision

Cabin travel uses a safe, loaded destination with solid ground, clear player space, no dangerous fluid or fire, and a position inside the world border. Destination checks have bounded work and release temporary chunk tickets.

Emergency travel tries the current exterior doorway, a nearby position in that dimension, the last valid campsite, and then Overworld world spawn. Voluntary packing aborts before removing the exterior when any current occupant cannot evacuate safely or delivery of the packed item is uncertain. Entry stays disabled during packing. A player who logged out inside an inactive cabin receives a safe destination on login.

## Rationale

The owner should be able to move the cabin without leaving occupants behind. A failed operation should keep the last usable cabin state. The fallback chain gives players a predictable route home when the exterior disappears.

## Scope

This decision applies to entry, exit, packing, offline recovery, and emergency travel. [PDR-0003](0003-cabin-home-respawning.md) defines the distinct respawn order. Future connected-cabin routes must preserve a safe exit or evacuate affected occupants.

## Consequences

Permissions and cabin state must be checked again before travel completes. Concurrent packing and connection changes cannot strand a player or expose two active entrances.
