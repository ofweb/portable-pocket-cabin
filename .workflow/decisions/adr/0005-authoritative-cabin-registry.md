# ADR-0005: Keep cabin identity in the registry

Status: Accepted

## Context

A cabin has a lasting interior and an exterior and packed item that can be replaced. A crash can stop deployment, packing, item delivery, or exterior removal. Recovery must keep one interior and one active entrance at most.

## Decision

The world registry is authoritative for each cabin's UUID, owner, interior cell, lifecycle state, exterior location, and packed-item generation. Items, structures, entrances, and chunk tickets derive from that state. A packed item does not store the interior.

The registry assigns permanent interior cells with no shared space in the pocket dimension. It never reuses a cabin UUID or cell index. New indices always increase. Cell separation accommodates supported cabin space and simulation. Deployment and packing save intent before changing items or world blocks. Startup and chunk-load reconciliation complete or reverse transitions that stopped before completion. Item recovery increases the generation, so earlier copies cannot act as active items.

## Rationale

Exteriors can disappear, and items can be lost or copied. Neither can hold the authoritative cabin record. A permanent cell lets placed blocks keep Minecraft persistence through travel. Saved transition intent lets recovery restore a valid state.

## Scope

This decision applies to identity, interior cell assignment, lifecycle transitions, and recovery. It does not define player controls, room behavior, or upgrade storage.

Lifecycle changes must keep registry authority and one active exterior at most. Recovery can delay exterior cleanup but cannot delete the interior or give access through two entrances. New cabin state must persist through registry copy, save, and reconciliation.

## Related records

- [PDR-0001](../pdr/0001-preserve-the-portable-home.md) states home protection rules.
- [PDR-0002](../pdr/0002-safe-cabin-travel.md) states travel safety rules.
- [Direction](../../direction.md) states the travelling-home goal.
