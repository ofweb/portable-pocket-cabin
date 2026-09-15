# ADR-0003: Persist Window Identity and Derive Geometry

**Date**: 2026-09-15
**Status**: accepted
**Deciders**: Project owner and Codex

## Context

Configurable cabin windows retain their identity and paid tiers while cabin expansion may reposition
them. Their exterior-condition blocks are only a projection of cabin-owned state, and an interrupted
installation must remain recoverable without consuming a fund twice. Later downgrade and removal
also need the exact materials paid for each purchased step.

## Decision

Persist each window inside the cabin-owned upgrade state by its eligible wall and bounded slot
identity, installed tier, and immutable paid-step receipts. Derive centered block geometry from the
cabin's current general size and installed window state rather than storing coordinates. Nesting the
window value under upgrade state keeps `CabinRecord` within Minecraft's 16-field record-codec limit.
Extend the upgrade installation journal with typed
general-space and window operations so deterministic world changes can be replayed before state and
fund completion. Schema 5 cabins migrate to grandfathered tier-one left and right windows with empty
base receipts; cabins created under schema 6 begin without windows.

## Alternatives Considered

### Alternative 1: Persist window coordinates

- **Pros**: World placement can read positions directly.
- **Cons**: Every cabin expansion must transactionally rewrite coordinates, and invalid combinations
  can be persisted.
- **Why not**: Coordinates are a deterministic projection of size, wall, slot and tier and should not
  become a second source of truth.

### Alternative 2: Reconstruct purchased state from window blocks

- **Pros**: Adds little registry data.
- **Cons**: Profile changes alter the blocks, player damage may make layouts ambiguous, and blocks
  cannot preserve exact purchase receipts.
- **Why not**: The pocket structure is a projection, not the owner of cabin progression.

### Alternative 3: Give windows a separate transaction journal

- **Pros**: Avoids changing the existing general-space installation record.
- **Cons**: Creates two competing cabin-wide upgrade locks and duplicates recovery sequencing.
- **Why not**: A typed journal preserves one authoritative upgrade transition boundary.

## Consequences

### Positive

- General-space expansion can recenter every installed window without changing identity or receipts.
- Window funds use stable targets, and paid stacks remain available for exact future refunds.
- Startup reconciliation can replay both general-space and window installations deterministically.

### Negative

- Schema 6 adds nested window state and a more general installation codec.
- All world mutations must derive and validate both the old and resulting wall layouts.

### Risks

- A geometry bug could overwrite cabin structure; pure layout invariants and obstruction GameTests
  must cover every wall, tier, one-window and two-window combination.
- A migration could confuse existing cabins with new ones; grandfathering is applied only while
  decoding a schema-5 registry, never as a field default.
