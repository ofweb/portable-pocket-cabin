# ADR-0006: Bind Cabin Kits with saved item delivery

Status: Accepted

## Context

An unbound Cabin Kit has no cabin UUID. First deployment creates saved cabin state, replaces an inventory item, and creates an exterior. Inventory and world saves cannot commit together. An interruption must not remove a Cabin Kit without a cabin or create two usable items.

## Decision

Each crafted Cabin Kit has a fixed identity for preview and recovery. First deployment validates that identity, its palette, and the player's ownership limit. It creates a cabin UUID and cell only when deployment begins.

The registry saves `DEPLOYING` and pending item delivery before replacing the held Cabin Kit with a pending item that players cannot use. Reconciliation commits a valid deployed cabin or restores `PACKED` with one active bound item or pending delivery. It invalidates matching copies and attempts pending delivery again when the owner reconnects with enough inventory capacity. Redeployment uses the same item delivery model and generation checks.

## Rationale

A fixed Cabin Kit identity connects its crafting result to its preview without making the item authoritative. Saved pending delivery keeps the cabin through inventory and world saves at different times. Generation checks prevent earlier copies from creating a different entrance.

## Scope

This decision extends [ADR-0005](0005-authoritative-cabin-registry.md) for first binding and item delivery. [PDR-0004](../pdr/0004-survival-acquisition-and-relocation.md) states player interactions.

Failed validation creates no cabin and uses no item. Pending delivery can stay until the owner has capacity. It cannot create two valid exteriors.
