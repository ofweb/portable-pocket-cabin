# Delivery 3.2a implementation plan: Purchasable cabin windows

**Intent:** [Milestone 3: Upgrade interface](../roadmap/03-upgrade-interface.md)

**Scope:** Replace automatic side-window projections with independently purchased windows on the
left, rear and right walls. Implement schema-6 state, schema-5 grandfathering, base purchases, every
upward tier, placement validation, exterior-condition panes, interface panels and recoverable
installation. Downgrade, removal, refunds and funded-target invalidation remain Delivery 3.2b.

**Status:** Complete; automated and manual client acceptance passed.

## Agreed behavior

- Each eligible wall has two stable window identities. The first may be purchased when tier one fits;
  the second remains visible but locked until the first is installed and their combined tier-one
  layout fits.
- Each installed window advances one tier at a time through `1x2`, `2x2`, `3x3`, `5x4`, `7x6` and
  `9x8`. A window at tier six remains visible as complete and accepts no fund contributions.
- The Cabin category shows general space plus the six window panels. Panel selection remains
  ephemeral and one panel is visible at a time.
- Window funds use the stable wall/slot target. Their requirements describe only the next purchase
  step and become stale if the installed tier changes independently.
- Only the owner may install a base window or higher tier. The owner and currently authorised trusted
  players may fund and withdraw from currently available window purchases. Creative mode receives no
  bypass.
- Before accepting materials and immediately before installation, the complete resulting wall layout
  must fit the cabin and must not overwrite a non-managed wall block or displace an attached block or
  hanging decoration. Failure leaves player items, funds, receipts and world blocks unchanged.
- One installed window is horizontally centered. Two retain slot order, have one solid divider, and
  are centered as a group. When exact integer centering is impossible, placement uses the lower local
  coordinate consistently. Window bottoms remain at floor level; height is limited by clear interior
  height.
- Installed panes display the existing dawn, day, sunset, night, rain, thunder, Nether, End and
  inactive profiles. Profile refresh mutates only derived installed-window positions.
- Every paid base or tier installation stores the exact contributed stacks as an immutable receipt.
  Delivery 3.2a does not expose any way to refund those receipts.
- Loading schema 5 grants every existing cabin one tier-one window in slot one on the left and right
  walls, with empty base receipts. The old automatic blocks are replaced by palette wall blocks and
  the new centered pane projections during reconciliation. Newly allocated schema-6 cabins start
  with solid walls and no window state.
- General-space expansion preserves window identities, tiers and receipts and applies the newly
  derived layout after expanding. Any conflicting decoration makes the expansion unavailable rather
  than silently destroying it.
- Packing and orphaning display inactive window panes. Redeployment resumes the exterior-derived
  profile. Funds, windows, receipts and an interrupted install survive restart.

## Explicitly out of scope

- downgrade, removal, material refund and invalidated-fund ejection actions
- front-wall windows, a third window on a wall or arbitrary placement
- player-selected colors, cosmetic styles, rendered portals or exterior terrain views
- room-category upgrades, storage integration or automated material sourcing
- live cost changes while a window fund is non-empty

## Technical approach

- Add a `CabinWindowState` value to the cabin-owned upgrade state, keeping `CabinRecord` inside the
  record-codec field limit. It owns a bounded collection of wall/slot windows; each installed window
  stores its tier and one exact-stack receipt per paid step.
- Advance the registry to schema 6. Decode schema-5 records with empty window fields, then explicitly
  map them to grandfathered side windows before constructing the registry. Never make grandfathering
  the codec default.
- Extend `CabinUpgradeState.Target` with a stable window target keyed by wall and slot. Generalize its
  persisted installation journal to a validated typed operation carrying the expected pre-state.
- Extend the reloadable progression definition with code-recognized base and tier window costs. The
  executable window types, tier dimensions and target grammar remain code-defined.
- Add a pure window-layout model that derives wall-plane footprints and affected positions for old and
  resulting states. It owns fit math, deterministic centering and frame preservation.
- Add a window world-effect adapter that validates managed wall blocks plus neighboring attachments,
  applies wall restoration and profile panes idempotently, and refreshes profile state.
- Generalize the catalog and upgrade service around target-specific resolved offers and effects while
  keeping all fund and ownership checks server-authoritative. One cabin-wide journal continues to
  serialize upgrades.
- Synchronize installable, locked and maximum-tier window panels through the existing 3.1b menu. No
  persistent selection or client-authored geometry is introduced.
- Reconcile migrated projections and interrupted typed installations after the pocket dimension is
  available. Follow the existing explicit flush boundary before world mutation and after completion.
- This design is recorded by
  [ADR-0003](../adr/0003-persist-window-identity-and-derive-geometry.md).

## Documentation

- Update the README with new-cabin window purchasing, tiering, permissions and lifecycle behavior.
- Split Delivery 3.2 into 3.2a and 3.2b in the Milestone 3 roadmap and mark only verified work as
  implemented.
- Keep downgrade, removal and refund instructions future-tense until Delivery 3.2b is accepted.

## Testing scope

- codec and invariant tests for window identities, tiers, exact receipt stacks, typed installations,
  schema-5 grandfathering, schema-6 new-cabin defaults, future-schema rejection and malformed state
- pure geometry tests for every tier, all three wall orientations, odd/even centering, one/two-window
  layouts, the one-block divider, frame preservation, clear-height limits and the size-21 tier-six pair
- catalog and service GameTests for six stable panels, second-window prerequisites, base and sequential
  tier costs, target-specific funds, permissions, stale state and two-step installation
- world GameTests for safe pane placement, obstruction and attached-decoration rejection, no partial
  mutation, general-space recentering, all condition profiles and inactive lifecycle behavior
- recovery and regression coverage for interrupted window/general-space installation, migrated old
  projections, restart, packing/redeployment and dedicated-server client-class isolation
- manual acceptance at common GUI scales for seven Cabin panels, locked/ready/maximum states,
  thirteen-item base funding and visible pane layouts

The complete GameTest and dedicated-server startup/reload suites remain the automated acceptance gate.

## Implementation sequence

1. Add failing state-codec, migration and pure-geometry tests.
2. Implement schema-6 window state, stable targets, typed installation records and migration.
3. Implement derived layout, safe world validation/application and migrated projection reconciliation.
4. Extend definitions and catalog with the six panels and base/tier offers.
5. Generalize fund installation and connect window state, receipts and recovery.
6. Add locked/maximum window presentation to the existing menu and screen.
7. Run focused tests, then the complete clean build and startup/reload gate.
8. Complete or explicitly leave pending the manual client checklist and update documentation status.

## Acceptance criteria

- Schema-5 cabins receive exactly one grandfathered tier-one left and right window; schema-6 cabins
  created afterward have none.
- Every eligible window identity can be funded and installed through its next valid tier without
  affecting another target's fund, tier or receipts.
- Window geometry is deterministic, centered, frame-safe and never replaces an obstruction or
  attached decoration.
- Every installed window shows the correct exterior-condition or inactive profile at its full derived
  footprint.
- A paid install records exact stacks, consumes only its complete fund and recovers safely after an
  interruption.
- General-space expansion retains and recenters installed windows without changing their identities,
  tiers or receipts.
- Owner, trusted-player, visitor, creative-mode, forged-action and access-loss behavior matches the
  existing upgrade-fund contract.
- All focused and complete automated gates pass; any unperformed manual checks remain explicit.
