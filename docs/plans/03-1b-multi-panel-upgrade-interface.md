# Delivery 3.1b implementation plan: Multi-panel upgrade interface

**Intent:** [Milestone 3: Upgrade interface](../roadmap/03-upgrade-interface.md)

**Scope:** Finish the bounded category, panel and requirement presentation needed by Delivery 3.2.
Production 3.1b still offers only the next general-space expansion. Cabin-window offers, window
state, receipts, refunds, placement and schema 6 remain Delivery 3.2.

**Status:** Complete; automated and manual client acceptance passed.

## Agreed behavior

- The approximately 248 by 220 container and fixed player inventory remain unchanged.
- Vertical icon tabs attached to the left side select implemented upgrade categories. Empty
  categories remain absent. The Cabin category is the only production category in 3.1b.
- The selected category shows one upgrade panel at a time. Previous and next controls cycle through
  that category's panels and are hidden or disabled when it contains only one panel.
- The panel displays two rows of eight exact-requirement fund slots. Sixteen is the presentation
  limit; a larger offer is unavailable with a clear explanation and is never silently truncated.
- Category and panel selection exist only in the open server menu. Merely navigating does not mutate
  cabin state, increment fund revision or persist through closing the menu.
- A catalog refresh retains the selected stable target if it remains visible. Otherwise it selects
  the first panel in the active category, or the first available category when the category vanished.
- Changing category or panel cancels an armed installation. Every deposit, withdrawal and install
  action applies only to the currently selected stable target and is revalidated server-side.
- Existing owner, trusted-player, guest and creative-mode rules remain unchanged. Current
  general-space funding and installation behavior remains unchanged.
- Catalog, definition or access failures leave player inventory and every fund unchanged. Existing
  access-loss behavior still closes or invalidates interaction.

## Explicitly out of scope

- cabin-window offers, purchasing, tiers, placement, exterior-condition panes or grandfathering
- downgrade, removal, receipt or refund actions
- schema changes or persistent interface selection
- front-wall windows, room categories or placeholder/dummy production upgrades
- copying Traveler's Backpack code or artwork; it is only a visual interaction reference for compact
  controls attached to a Minecraft-style container

## Technical approach

- Generalize the server-resolved panel presentation away from general-space-only labels while
  retaining stable `CabinUpgradeState.Target` identity and server-authoritative validation.
- Add a small deterministic selection model that resolves active category and panel from catalog
  groups, prefers the previously selected stable target during refresh, and bounds every client
  navigation request.
- Keep the active category, selected target and armed confirmation in `CabinUpgradeMenu`. Allocate
  bounded button identifiers for category and previous/next panel navigation. Forged or out-of-range
  identifiers are rejected without state changes.
- Expand the menu's virtual requirement slots from eight to sixteen and lay them out as two rows of
  eight. A panel exceeding the bound is marked unavailable before its slots can transact.
- Synchronize only the selected panel's presentation to the client. Hidden panels expose no active
  slots or actions, and changing selection clears the old display before broadcasting the new one.
- Keep `CabinUpgradeService` as the transaction authority. No block inventory, client-owned catalog
  state or persistent selected-upgrade field is introduced.
- No ADR is required: this completes the already-recorded target-keyed interface boundary and does
  not change durable ownership or transaction architecture.

## Documentation

- Update the Milestone 3 roadmap language from stacked panels to side category tabs with one active
  panel and record Delivery 3.1b separately from windows.
- Mark this plan and the roadmap implemented only after automated acceptance passes. The README's
  gameplay instructions need no behavioral rewrite because production still has one general-space
  offer; its milestone summary may be updated when 3.1b is accepted.
- Delivery 3.2 documentation retains the full player-facing window instructions and manual checks.

## Testing scope

Confidence is required at four levels:

- pure selection and layout coverage for category fallback, stable-target retention, panel cycling,
  attached-tab bounds, one-panel viewport and all sixteen requirement coordinates
- server-side GameTests using synthetic catalog groups for multiple categories and panels, stable
  target retention, category fallback, panel cycling, forged index rejection, exactly thirteen
  requirements, the sixteen-slot boundary and an oversized offer
- menu structure coverage for two deterministic rows of registry-backed virtual requirement slots
  while existing interaction regressions continue to exercise target-specific transactions
- regression coverage for current general-space deposit, withdrawal, shift-click, drag, two-step
  installation, permissions, access loss and dedicated-server client-class isolation
- manual acceptance at common GUI scales for the real single Cabin tab, two-row slot layout,
  tooltips and unchanged general-space behavior; manual multi-panel/window acceptance remains a
  Delivery 3.2 gate because 3.1b adds no dummy production offers

The complete GameTest and dedicated-server startup/reload suites remain the automated acceptance
gate.

## Implementation sequence

1. Add failing selection/layout tests and synthetic-catalog menu tests.
2. Generalize catalog panel presentation and add deterministic ephemeral selection.
3. Add bounded category/panel controls and synchronize only the selected panel.
4. Expand and render the two-row sixteen-slot requirement area with oversize failure behavior.
5. Run focused regressions, then the complete clean build and startup/reload acceptance gate.
6. Perform or leave explicitly pending the common-scale manual client checklist, then update status
   documentation to match the verified result.

## Acceptance criteria

- Production general-space funding and installation behave as before.
- Synthetic catalogs can select every category and panel, retain stable targets and reject forged
  category indexes.
- Thirteen distinct requirements are simultaneously visible and interactive; sixteen is supported;
  more than sixteen is explained and cannot mutate a fund.
- Navigation is ephemeral, retains a still-visible stable target across refresh and cancels armed
  installation when the target changes.
- Automated GameTests and dedicated-server startup/reload checks pass.
- Remaining manual acceptance is named explicitly rather than reported as complete.

## Manual client acceptance checklist

- [ ] At Auto, Small, Normal and Large GUI scales, the 248 by 220 container, attached Cabin tab,
  panel, both requirement rows and fixed player inventory remain visible without overlap or clipping.
- [ ] The selected Cabin tab is visibly distinct and its hover tooltip names the category.
- [ ] With the production general-space offer, unused requirement slots remain absent and the visible
  slots preserve left-click, right-click, drag, shift-click and tooltip behavior.
- [ ] Previous/next panel controls remain hidden while the Cabin category contains one offer.
- [ ] The owner-only Install control and status icon remain readable and do not overlap the panel
  title, effect or first requirement row.
- [ ] A dedicated server and ordinary client can connect and open the menu without protocol or slot
  index errors.

## Post-acceptance layout correction

Manual screenshots at a large GUI scale showed that the original fixed geometry did not satisfy the
no-overlap acceptance criteria: the 38-pixel panel inset left excessive border space, 22-pixel
requirement columns allowed longer progress labels to collide, and the title, page count, status icon
and action controls shared the same header area.

The agreed correction retains the 248 by 220 container, fixed player inventory, side tabs, navigation
and vanilla-style bevels. The inner panel instead uses four-pixel side insets, requirement columns use
28-pixel spacing with centered progress labels, and the description and requirement rows receive
separate vertical space. Header content uses bounded title, page, status and action zones so those
elements cannot overlap. This is presentation-only: menu slots, protocol indexes, permissions,
transactions and persisted state remain unchanged.
