# ADR-0003: Save window identity and derive geometry

Status: Accepted

## Context

Expansion can move Cabin window blocks while their tiers and purchased materials stay unchanged. World blocks cannot hold the authoritative state. An installation that stops before completion must not use a fund again on recovery.

## Decision

The upgrade state records each Cabin window by permitted wall and slot, installed tier, and specified paid-step receipts. Slot counts have limits. Block coordinates derive from saved general size, structural corner frames, and installed window state. The registry does not save window coordinates. Expansion can recenter Cabin windows without changing their identities or receipts.

An installation journal has types for general-space and window upgrades. The service saves intent before applying the deterministic world effect. It then commits progression and fund consumption. Reconciliation can repeat an effect that stopped before completion. Only one installation journal can be active.

Schema 5 migration gives cabins tier-one Cabin windows on the left and right walls with empty base receipts. New schema 6 cabins have solid walls. Missing window state outside migration does not give free windows.

## Rationale

Saved identities keep purchase records independent of block damage or profile changes. Derived geometry uses saved cabin state as its only source. One installation journal gives upgrades one recovery boundary.

## Scope

This decision applies to purchased window state, derived positions, and installation recovery. [ADR-0008](0008-derive-expansion-geometry-from-saved-size.md) states general-space geometry. [PDR-0008](../pdr/0008-purchase-and-reverse-cabin-windows.md) states window behavior. [ADR-0004](0004-persist-window-reversals-through-refund-ejection.md) states downgrade and removal recovery.

Wall changes must validate earlier and resulting layouts. Migration must distinguish earlier cabins from new cabins before giving free Cabin windows.
