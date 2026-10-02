# ADR-0003: Save window identity and derive geometry

Status: Accepted

## Context

Expansion can move Cabin window blocks while their tiers and purchased materials stay unchanged. World blocks cannot hold the authoritative state. An installation that stops before completion must not use a fund again on recovery.

## Decision

The upgrade state records each Cabin window by permitted wall and slot, installed tier, and specified paid-step receipts. Slot counts have limits. Block coordinates derive from saved general size, structural corner frames, reserved wall centers, and installed window state. Each slot stays on its side of the reserved center. The registry does not save window coordinates. Expansion can reposition Cabin windows without changing their identities or receipts.

An installation journal has types for general-space and window upgrades. The service saves intent before applying the deterministic world effect. It then commits progression and fund consumption. Reconciliation can repeat an effect that stopped before completion. Only one installation journal can be active.

Registry schema 8 uses centered, odd-size interiors. Earlier registries are rejected because their generated geometry cannot safely be interpreted as the new layout. New cabins have solid walls. Missing window state does not give free windows.

## Rationale

Saved identities keep purchase records independent of block damage or profile changes. Derived geometry uses saved cabin state as its only source. One installation journal gives upgrades one recovery boundary.

## Scope

This decision applies to purchased window state, derived positions, and installation recovery. [ADR-0008](0008-derive-expansion-geometry-from-saved-size.md) states general-space geometry. [PDR-0008](../pdr/0008-purchase-and-reverse-cabin-windows.md) states window behavior. [ADR-0004](0004-persist-window-reversals-through-refund-ejection.md) states downgrade and removal recovery.

Wall changes must validate earlier and resulting layouts. Unsupported registry geometry must fail before loading or rewriting cabin state.
