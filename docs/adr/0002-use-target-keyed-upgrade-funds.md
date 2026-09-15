# ADR-0002: Use Target-Keyed Upgrade Funds

**Date**: 2026-09-15
**Status**: accepted
**Deciders**: Project owner and Codex
**Supersedes**: [ADR-0001](0001-persist-upgrade-funds-in-cabin-state.md)

## Context

The first upgrade interface persisted one owner-tracked target and accepted contributions through a
special deposit control. Funded requirements were display-only and could be recovered only by
stopping tracking and ejecting the complete fund. In play, that interaction did not feel like a
Minecraft container, consumed too much screen space and prevented residents from correcting a
deposit naturally.

Future window, storage and functional-room upgrades also need distinct material commitments. One
global tracked target would serialize otherwise independent contributions and make switching between
upgrade views a destructive action.

The fund must still remain narrower than general storage: it may accept only exact displayed
requirements, up to fixed caps, and must remain unavailable to hoppers and automation.

## Decision

Persist a collection of non-empty upgrade funds in the authoritative cabin registry. Each fund is
keyed by a stable upgrade target and captures that target's resolved requirements plus exact
contributed stacks. There is no persistent tracked or selected upgrade.

Expose funds as virtual registry-backed menu slots. One requirement icon aggregates an ordered queue
of preserved real stacks, so totals may exceed an item's maximum stack size and component-incompatible
stacks remain distinct. Owners and authorised residents may deposit and withdraw through those slots.
Every complete cursor/inventory/fund transaction is validated and persisted by the server-side
upgrade service; independent slot callbacks never mutate durable state. The slots are not a
controller block inventory or a discoverable storage capability.

Only currently available upgrades accept new deposits. Each slot accepts only its displayed exact
item and is capped at the requirement total. Shift-clicking the requirement icon explicitly pulls
that item from the player's inventory, while shift-clicking a player-inventory stack never selects a
fund implicitly. Automatic icon funding selects only ordinary/default stacks; modified stacks require
deliberate carried-stack placement.

Install confirmation is ephemeral server-menu state. Arming captures one target and its current fund
revision; a final click is accepted only while that arm remains current and unexpired.

Advance the cabin registry from schema 4 to schema 5. Migrate a schema-4 tracked fund into the fund
for its existing stable target without changing its requirement snapshot or contributed stacks.

## Alternatives Considered

### Alternative 1: Retain tracking and only restyle the screen

- **Pros**: Avoids a state migration and reuses the implemented service directly.
- **Cons**: Retains destructive switching, display-only material rows and only one contribution target.
- **Why not**: It does not solve the interaction or future multi-upgrade problems observed in play.

### Alternative 2: Copy a fund into a temporary menu inventory

- **Pros**: Uses ordinary `SimpleContainer` slots with little adapter code.
- **Cons**: Save-on-close can lose changes on crash and conflicts when several players have the menu open.
- **Why not**: Fund mutations must be durable and authoritative when they occur.

### Alternative 3: Store funds in a controller block entity

- **Pros**: Native container persistence and familiar hopper integration.
- **Cons**: Couples cabin-owned funds to a temporary projection and exposes them as general logistics storage.
- **Why not**: Packing, relocation and the deliberately restricted fund boundary require cabin ownership.

## Consequences

### Positive

- Funding uses familiar item-slot interactions and permits mistakes to be corrected by withdrawal.
- Independent upgrades can eventually receive materials without destructive tracking changes.
- Exact item and count limits keep the funds unsuitable as general-purpose storage.
- Funds survive packing, relocation and restart independently of the Lodestone projection.

### Negative

- Registry-backed slots need careful adaptation of vanilla click, drag and cursor semantics.
- One logical slot representing several physical stacks requires explicit aggregate-count and
  deterministic-withdrawal presentation.
- Schema 5 and concurrent menu synchronization add migration and integration-test work.
- Residents who can contribute can also withdraw any material in an upgrade fund.

### Risks

- A stale or concurrent click could duplicate or lose items unless each server transaction owns both
  the accepted fund mutation and the player-stack remainder.
- Client confirmation and status can become stale; the server must revalidate the target, fund,
  permission and structure before installation.
- Live cost changes for non-empty funds remain unsupported until post-MVP balancing defines a policy.
