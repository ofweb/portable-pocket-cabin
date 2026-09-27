# ADR-0005: Keep cabin identity in an authoritative registry

Status: Accepted

## Context

A portable cabin has a persistent interior but a replaceable exterior and packed item. A crash can interrupt deployment, packing, item delivery, or exterior removal. The cabin must remain recoverable without a copied interior or two active entrances.

## Decision

The world registry owns each cabin's permanent UUID, owner, interior cell, lifecycle state, exterior location, and packed-item generation. Items, structures, entrances, and chunk tickets are projections of that state. A packed item does not store the interior.

The registry allocates permanent, isolated interior cells in a pocket dimension and never reuses a cabin UUID or cell index. It allocates cell indices monotonically with enough separation for supported cabin space and simulation. Deployment and packing persist intent before changing physical projections. Startup and chunk-load reconciliation finish or reverse interrupted transitions. Item recovery advances the generation so older copies cannot act as current items.

## Rationale

The exterior can disappear through damage or world changes, and players can lose or copy items. Neither can safely own the only record of a cabin. A permanent cell lets ordinary Minecraft blocks keep their normal persistence while the exterior moves. Persisted transition intent gives recovery enough information to restore one valid state after an interruption.

## Scope

This decision applies to cabin identity, interior allocation, lifecycle transitions, and recovery. It does not define player controls, room behavior, or upgrade storage.

## Consequences

Every lifecycle change must preserve registry authority and the one-active-exterior rule. Recovery can defer physical cleanup, but it cannot discard the interior or make two entrances usable. Features that add cabin state must survive registry copy, save, and reconciliation paths.

## Related records

- [PDR-0001](../pdr/0001-preserve-the-portable-home.md) states the home-preservation rule.
- [PDR-0002](../pdr/0002-safe-cabin-travel.md) states the safe-travel rule.
- [Direction](../../direction.md) sets the travelling-home boundary.
