# ADR-0006: Bind Cabin Kits through a recoverable item obligation

Status: Accepted

## Context

An unbound Cabin Kit has no cabin UUID. First deployment creates permanent cabin state while replacing an inventory item and projecting an exterior. Inventory and world saves cannot commit as one atomic store. An interruption can otherwise consume a Kit without a cabin or leave two usable items.

## Decision

Each crafted Kit has an immutable identity for preview and recovery. First deployment validates that identity, the selected palette, and the player's ownership limit. It creates the cabin UUID and cell only when deployment begins.

The registry persists `DEPLOYING` and an unresolved item-delivery obligation before replacing the held Kit with a non-usable pending item. Reconciliation commits a valid deployed cabin or rolls back to `PACKED` with one current bound item active or owed. It invalidates matching duplicates and retries owed delivery on owner login when inventory capacity permits. Redeployment uses the same obligation model and packed-item generation checks.

## Rationale

The immutable Kit identity connects a physical crafting result to its preview without making the item the cabin's authority. A persisted obligation keeps the cabin recoverable when item delivery and world changes save at different times. Generation checks stop old copies from becoming another entrance.

## Scope

This decision extends [ADR-0005](0005-authoritative-cabin-registry.md) for first binding and item delivery. [PDR-0004](../pdr/0004-survival-acquisition-and-relocation.md) owns the player interaction.

## Consequences

Failed validation creates no cabin and consumes no item. Recovery may owe an item until the owner has capacity, but it cannot create a second valid exterior.
