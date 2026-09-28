# ADR-0003: Persist window identity and derive geometry

Status: Accepted

## Context

Cabin expansion can move window blocks while purchased windows retain their tiers and paid materials. World blocks cannot safely own that state. An interrupted installation must not consume a fund twice.

## Decision

The cabin upgrade state stores each window by eligible wall and bounded slot, installed tier, and exact paid-step receipts. Window block coordinates derive from saved general size, structural corner frames, and installed window state. The registry does not save window coordinates. General-space expansion can recenter windows without changing their identities or receipts.

A typed installation journal covers both general-space and window upgrades. The service records intent before the deterministic world effect, then commits progression and fund consumption. Reconciliation can replay an interrupted effect. The journal prevents a second installation while one remains active.

Schema-5 cabins receive grandfathered tier-one windows on the left and right walls. Those windows have empty base receipts. New schema-6 cabins begin with solid walls. Grandfathering applies during migration, not as a default for absent window state.

## Rationale

Saved identities prevent block damage or profile changes from redefining a purchase. Derived geometry avoids a second source of truth when the cabin grows. One typed installation journal keeps upward upgrades within one recovery boundary.

## Scope

This decision covers purchased-window state, placement derivation, and installation recovery. [ADR-0008](0008-derive-expansion-geometry-from-saved-size.md) owns general-space geometry. [PDR-0008](../pdr/0008-purchase-and-reverse-cabin-windows.md) owns visible window behavior. [ADR-0004](0004-persist-window-reversals-through-refund-ejection.md) owns downward transitions.

## Consequences

Every wall mutation must validate old and resulting layouts. Migration must distinguish established cabins from new cabins so it does not grant new windows by default.
