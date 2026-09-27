# Delivery 3.2b implementation plan: Reversible cabin windows

**Intent:** [Milestone 3: Upgrade interface](../roadmap/03-upgrade-interface.md)

**Scope:** Let the cabin owner downgrade or remove an installed window, restore and recenter the
resulting wall safely, refund the exact applicable purchase receipts, and eject any funds invalidated
by the resulting state. Make the complete reversal recoverable across interruption and restart.

**Status:** Complete; automated and manual client acceptance passed.

## Agreed behavior

- An installed tier-two through tier-six window offers both **Downgrade** and **Remove**. A tier-one
  window offers only **Remove**.
- Downgrade removes exactly the latest installed tier and refunds only that tier's immutable receipt.
  Remove deletes the window and refunds its base receipt plus every installed tier receipt.
- Removing a grandfathered tier-one window succeeds but refunds nothing because its base receipt is
  empty.
- Refund stacks preserve their exact counts and components and appear as item entities beside the
  protected interior controller. The drop cell is not required to be clear or hazard-free; after
  materialisation, obstruction, fire, lava, pickup and despawn follow normal Minecraft behavior.
  Refunds never enter a fund or player inventory.
- The owner is warned about every funded target that the resulting cabin state would invalidate.
  Confirmation ejects those exact fund stacks alongside, but distinctly from, receipt refunds.
  Temporarily obstructed but otherwise valid funds remain intact.
- Only the cabin owner may downgrade or remove. Trusted players retain their existing authority to
  fund and withdraw, but receive no reversal authority. Creative mode has no bypass.
- Reversal is available only through an active deployed cabin's protected interface. It uses the same
  two-click confirmation duration and cancels on navigation, action change, fund revision change,
  target change, timeout or access loss.
- The complete resulting layout is validated both before arming and immediately before execution.
  A non-managed current pane, occupied target pane position, attached block or hanging decoration
  rejects the action without changing blocks, windows, receipts or funds.
- Removing either identity leaves any other installed identity unchanged and recenters a lone
  remaining window. Slot two may remain without slot one after removal; the removed stable identity
  may later be purchased again under ordinary prerequisites.
- An interrupted reversal resumes without applying its state twice or materialising a second refund
  batch. Dropped items otherwise follow normal Minecraft pickup, despawn and hard-crash durability.

## Explicitly out of scope

- general-space downgrade or refunds for any upgrade other than cabin windows
- refund delivery directly to inventories, storage, mailboxes or an unassigned material wallet
- changing window tier costs, receipts, stable identities, pane profiles or placement dimensions
- front-wall windows, additional wall slots, arbitrary placement or window styling
- redesigning categories, panel navigation, fund-slot interactions or trusted-player permissions

## Technical approach

- Advance the registry to schema 7. Schema-6 cabin records decode with no active reversal and retain
  every window, receipt, fund and installation unchanged.
- Add an optional `WindowReversal` journal to `CabinUpgradeState` while retaining the existing
  installation journal. State validation rejects simultaneous operations. The reversal contains an
  operation UUID, action, stable window target, expected/resulting tier, receipt-refund stacks,
  invalidated funds and a `WORLD_PENDING` or `EJECTION_PENDING` phase.
- Treat either journal as a cabin-wide upgrade lock. Deposit, withdrawal, installation, downgrade and
  removal services reject mutation while the lock is held, and the menu exposes all fund and action
  controls as read-only until recovery clears it.
- Add pure downgrade and remove operations to `CabinWindowState`. They return the resulting immutable
  state and exact receipt stacks without mutating the source value.
- Extend `CabinWindowLayout` and `CabinWindowWorld` with a general current-to-resulting-state
  transition. Reuse managed-pane, palette-wall, attachment and hanging-entity validation and the
  existing wall restoration/profile projection.
- Add a reversal service that rechecks owner, lifecycle, target tier, fund revision and derived world
  transition. It computes a tentative resulting cabin, classifies invalidated funds using catalog
  existence, prerequisite and requirement snapshots, and persists the full journal before world
  mutation.
- After the idempotent world transition, atomically commit the resulting window state, remove only
  invalidated funds, advance the fund revision, set `EJECTION_PENDING` and flush. Materialise the
  recorded refund and fund stacks at the fixed controller-side position as one all-or-nothing entity
  batch, without validating the drop cell, then clear and flush the journal.
- Mark every spawned entity with its operation UUID and payload index. Recovery validates matching
  entities beside the controller, treats a complete batch as already materialised, and creates only
  missing indexed entries when a hard crash persisted part of the batch. An existing index whose
  stack no longer matches its recorded payload is an explicit recovery error.
- Synchronously load the controller/refund chunk before scanning or spawning marked entities during
  normal execution and startup recovery. Do not depend on cabin simulation tickets, which are
  restored later in server startup.
- Reconcile both reversal phases after the pocket dimension is available. Installation and reversal
  recovery remain mutually exclusive and use their own state-specific services.
- Add distinct server menu actions and client buttons for downgrade and remove. Synchronise installed
  tier plus independent install, downgrade and remove availability and blocking reasons. A blocked
  next-tier installation does not block a valid reversal. Bind arming to action, stable target and
  fund revision; include invalidated target titles in the first-click warning.
- This recovery boundary is recorded by
  [ADR-0004](../adr/0004-persist-window-reversals-through-refund-ejection.md).

## Documentation

- Update the README with owner-only downgrade/removal, exact controller-side refunds, invalidated-fund
  ejection and grandfathered-window behavior.
- Mark Delivery 3.2b implemented only after automated acceptance, keeping manual GUI checks explicit.
- Update schema references from 6 to 7 without changing the schema-2 fresh-world warning.

## Testing scope

- codec and invariant GameTests for schema-6 migration, mutually exclusive journals, both phases,
  bounded tier transitions and exact component-bearing payload stacks
- pure state tests for downgrade latest-receipt selection, complete removal, empty grandfathered
  refund and source immutability
- service tests for owner/trusted/visitor/creative behavior, stale tier or revision, absent targets,
  action-specific confirmation, the cabin-wide operation lock and preservation of unaffected funds
- invalidation tests for the reversed target's stale next-tier fund, other valid target funds and
  temporary obstruction that must not eject a fund
- world tests for safe shrinking/removal, palette-wall restoration, slot-two recentering, obstruction
  and attached-decoration rejection, and no partial mutation
- ejection tests confirming that refund creation does not special-case an occupied or hazardous drop
  cell and that the resulting entities use normal Minecraft behavior
- recovery tests before and after world mutation, before ejection, from an initially unloaded refund
  chunk, with a complete tagged entity batch, with missing indices in a partial batch, and with a
  mismatched existing index
- menu tests for button visibility, independent install/reversal availability, navigation/action
  cancellation, warning text and forged actions
- manual acceptance at common GUI scales for install/downgrade/remove controls, warnings and visible
  refund drops

The complete GameTest and dedicated-server startup/reload suites remain the automated acceptance gate.

Automated acceptance passed with all 68 required GameTests. The dedicated-server startup/reload gate
also passes as part of the complete build. Manual GUI-scale and visible-drop checks passed.

## Implementation sequence

1. Add failing state, migration and pure reversal tests.
2. Implement schema-7 reversal journal and immutable window downgrade/removal results.
3. Add resulting-layout validation and idempotent world application.
4. Implement invalidated-fund classification, phased registry commits and marked atomic ejection.
5. Add startup recovery for both phases and entity-batch recognition.
6. Connect server menu actions, synchronized presentation and client controls.
7. Add focused service, world, recovery and menu regression tests.
8. Update playable documentation and run the complete automated acceptance gate.

## Acceptance criteria

- Downgrade returns only the latest tier receipt; removal returns every receipt; grandfathered base
  removal returns nothing.
- Exact stack counts and components are preserved in physical controller-side refund entities.
- Only the owner can reverse a window, and every failed validation leaves all persistent and world
  state unchanged.
- The resulting panes are centered, frame-safe and obstruction-safe for either removed identity,
  including a lone remaining slot-two window.
- An unavailable next-tier installation does not prevent an independently valid downgrade or removal.
- Invalidated funds are warned about and ejected exactly once while every still-valid fund remains.
- An active installation or reversal makes every fund and upgrade action read-only until recovery.
- Recovery completes either persisted phase without repeating state changes or persisted refund
  entries, including when only part of the indexed batch reached disk.
- Schema-6 cabins migrate without losing windows, receipts, funds or an active installation.
- All focused and complete automated gates pass; any unperformed manual checks remain explicit.
