# ADR-0004: Persist Window Reversals Through Refund Ejection

**Date**: 2026-09-15
**Status**: accepted
**Deciders**: Project owner and Codex

## Context

Window reversal changes registry state, physical wall blocks, upgrade funds and dropped refund
entities. Minecraft cannot commit those stores atomically, so an interruption must remain recoverable
without consuming receipts twice or duplicating a refund. The existing installation journal already
protects upward transitions and should not be destabilised by reversal-only state.

## Decision

Schema 7 adds a window-reversal journal alongside the existing installation journal and permits only
one of them to be active. Either journal holds a cabin-wide upgrade lock: fund deposits, withdrawals
and every upgrade action remain read-only until recovery completes. The reversal journal records the
action, target, expected and resulting tier, exact
receipt refund stacks, invalidated-fund stacks and transition phase. Recovery applies the idempotent
wall mutation, commits and flushes the resulting cabin state, then materialises operation-and-index-
tagged item entities beside the controller before clearing and flushing the journal. Persisted tags
let recovery recognise a complete batch or materialise only missing indexed entries from a partially
persisted batch. Recovery synchronously loads the controller/refund chunk before inspecting those
entities; it does not depend on later restoration of cabin simulation tickets.

## Alternatives Considered

### Alternative 1: Generalise the installation journal

- **Pros**: One persisted operation type and one recovery entry point.
- **Cons**: Reversal phases, refund payloads and fund invalidation complicate the proven upward-install
  invariants.
- **Why not**: A separate mutually exclusive journal adds the new recovery boundary without risking
  installation compatibility.

### Alternative 2: Mutate state and eject without a journal

- **Pros**: Few data-model changes.
- **Cons**: Interruption between wall, state and entity changes could lose or duplicate exact stacks.
- **Why not**: Refund correctness is the central promise of reversible purchased upgrades.

### Alternative 3: Return materials directly to the owner inventory

- **Pros**: Immediate collection when inventory capacity is available.
- **Cons**: Full inventories, disconnection and offline recovery introduce another partial-delivery
  path.
- **Why not**: Controller-side item entities provide one deterministic recovery destination independent
  of player state.

## Consequences

### Positive

- Exact component-bearing receipts and invalidated funds survive ordinary interruption and restart.
- Schema-6 cabin windows, receipts and funds migrate without gameplay changes.
- Installation and reversal retain separate, locally understandable invariants.
- Failed ejection cannot race new fund or upgrade mutations while recovery remains pending.

### Negative

- Cabin upgrade state gains another phased operation and schema migration.
- Refund item entities need persistent operation markers and reconciliation logic.

### Risks

- A hard crash remains subject to Minecraft entity and chunk-save durability; operation tags prevent
  duplication when the spawned batch was persisted.
- Partial entity persistence could split a refund across a hard crash; recovery must validate every
  matching index and materialise only entries not already present.
- Startup recovery runs before normal cabin simulation tickets are restored; the reversal service
  must explicitly load the refund chunk before deciding that a tagged entry is absent.
- Refund entities spawn at the fixed controller-side position without checking whether the cell is
  clear or hazard-free. Once materialised they deliberately follow ordinary Minecraft movement,
  fire, lava, pickup and despawn behavior.
