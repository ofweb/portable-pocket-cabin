# ADR-0001: Persist Upgrade Funds in Cabin State

**Date**: 2026-09-14
**Status**: accepted
**Deciders**: Project owner and Codex

## Context

Cabin owners and residents need to contribute materials deliberately toward one tracked upgrade without turning those materials into general-purpose storage. Contributions must remain bound to the cabin across packing, relocation and restart, while the protected interior Lodestone remains the interface entry point. Later central storage and crafting automation need to supply upgrade requirements without becoming prerequisites for the first upgrade interface.

## Decision

Persist the tracked upgrade, its resolved requirement snapshot and its contributed item stacks in the authoritative cabin registry. Expose the fund only through a server-authoritative menu and do not publish it as a block inventory, hopper target, pipe endpoint or general storage capability.

## Alternatives Considered

### Alternative 1: Store the fund in a controller block entity
- **Pros**: Uses familiar container mechanics and vanilla-style slot persistence.
- **Cons**: Couples authoritative materials to a replaceable world projection and makes automation capability exposure easy or accidental.
- **Why not**: The cabin, not the currently rendered Lodestone, owns the fund, and the fund must remain unavailable to general logistics systems.

### Alternative 2: Use central cabin storage
- **Pros**: Avoids a second cabin-owned item store and naturally supports later automation.
- **Cons**: Pulls Milestone 4 storage into Milestone 3 and makes contributed materials available beyond their selected upgrade.
- **Why not**: The initial interface must work before storage exists, and contribution intent must remain narrower than ordinary storage access.

### Alternative 3: Pay directly from player inventory
- **Pros**: Requires little persistent state and matches the provisional Milestone 2 interaction.
- **Cons**: Prevents cooperative funding and can remove materials without a deliberate contribution step.
- **Why not**: It contradicts the agreed player-facing funding model.

## Consequences

### Positive
- Contributions survive cabin packing and relocation because they share the cabin's authoritative identity.
- Requirement-limited menu access prevents the fund from becoming free storage or an automation endpoint.
- Later storage and crafting sources can satisfy requirements through explicit owner-authorised transactions without changing fund ownership.

### Negative
- The mod must provide a custom synchronized menu instead of relying on an ordinary container block.
- Persisting item stacks and shared open views adds validation, migration and synchronization work.
- Physical ejection cannot exceed vanilla item-entity durability semantics.

### Risks
- A stale client or concurrent contributor could exceed a requirement unless every slot mutation is capped against current server state; the server revalidates each mutation.
- A datapack reload could change a partially funded cost; tracking captures resolved requirements and pauses when the loaded definition no longer matches.
- Permanent installation spans saved state and world blocks; a persisted, idempotent installation transition provides restart reconciliation.
