# Delivery 3.1a implementation plan: Per-upgrade fund interface

**Intent:** [Milestone 3: Upgrade interface](../roadmap/03-upgrade-interface.md)

**Scope:** Correct the Delivery 3.1 interface and fund model for the existing next general-space
upgrade. Configurable windows, window state and refunds remain Delivery 3.2. Existing automatic
windows and installed general-space progression remain unchanged.

**Status:** Implemented; automated acceptance complete and manual client acceptance pending.

## Design mapping

- `CabinRegistry` remains the authoritative durable owner of funds and installation operations.
- Schema 5 replaces one optional tracked fund with non-empty funds keyed by stable upgrade target.
- The schema-4 tracked target, requirement snapshot and stacks migrate into the matching schema-5
  fund without ejection or loss. Schema 3 continues to chain through the existing migration.
- The catalog resolves grouped upgrade-panel descriptors. Delivery 3.1a returns one general-space
  panel in the Cabin group, while the representation can carry several panels and groups.
- Registry-backed menu slots expose familiar container interactions without exposing a block
  inventory or automation capability.
- A server-side service owns deposit, withdrawal, validation, installation and later invalidation
  ejection. Menus and client screens do not mutate registry state directly.
- The client uses a compact Minecraft-style container with fixed player inventory, independently
  scrolling upgrade panels, vertical icon tabs, icon actions and hover explanations.
- The existing persisted, idempotent general-space installation operation remains the world-change
  boundary.

## Task 1: Replace tracked state with target-keyed funds

**Requirement:** Several upgrades may eventually be funded independently; schema-4 materials must
survive migration exactly; empty funds need no persistent record.

### RED

- Add codec round-trip coverage for zero, one and several target-keyed fund records with requirement
  snapshots and component-bearing contributed stacks.
- Cover one logical requirement whose total exceeds the item's maximum stack size and one containing
  several component-incompatible stacks in deterministic contribution order.
- Add migration coverage that a schema-4 empty tracked state becomes no schema-5 fund and that a
  partially or completely funded tracked state becomes exactly one matching fund.
- Assert schema 3 chains through schema 4 to schema 5, schema 2 remains rejected, and future schemas
  remain rejected.
- Exercise cabin record copy and lifecycle paths and assert every fund and installation operation is
  preserved through packing and redeployment.
- Expected failure: schema 4 stores only one optional `TrackedUpgrade`.
- If a test passes unexpectedly: ensure it decodes serialized schema-4 data rather than constructing
  the new type with defaults.

### GREEN

- Introduce an immutable codec-backed fund record keyed by stable target and containing its resolved
  requirements plus an ordered queue of contributed stacks per exact required item.
- Store only non-empty funds, reject duplicate targets or requirements, and retain the existing
  installation operation in the upgrade state.
- Advance the registry to schema 5 and implement lossless schema-4 migration.
- Preserve the new state through every cabin lifecycle mutation.

### REFACTOR

- Replace tracked-specific names with target/fund terminology without renaming unrelated installed
  upgrade concepts.
- Centralize upgrade-state copy construction and invariants.

## Task 2: Resolve grouped offers and fund availability

**Requirement:** Panels are grouped by cabin area, only currently available upgrades accept new
materials, and a funded upgrade that later becomes temporarily obstructed remains withdrawable.

### RED

- Test that the catalog groups the next general-space offer under the Cabin tab and reports its
  stable target, compact effect, requirements and status.
- Test available, missing-material, ready, maximum, obstructed and unavailable statuses with an
  actionable reason suitable for a tooltip.
- Test that locked or forged targets reject deposits while an already-funded target retains its
  materials and permits withdrawal after temporary obstruction.
- Assert empty or unimplemented categories are absent.
- Expected failure: the catalog exposes a single trackable offer and tracked-state flags rather than
  grouped panel descriptors.
- If a test passes unexpectedly: verify the result is not reconstructing status on the client.

### GREEN

- Replace the single-offer presentation contract with bounded group and panel descriptors resolved
  entirely on the server.
- Keep current Delivery 3.1a output to the next general-space panel in the Cabin group.
- Separate eligibility for new deposits from retention and withdrawal of an existing fund.

### REFACTOR

- Keep stable target identity independent from labels, icons and tab placement.
- Reuse current definition resolution and expansion validation rather than duplicating them in the
  menu.

## Task 3: Add exact-item deposit and withdrawal transactions

**Requirement:** Owners and current trusted players can manipulate exact, capped material slots like
a restricted container; funds cannot become general storage; creative mode has no exception.

### RED

- Test deposit by carried stack, right-click single-item deposit and drag distribution into matching
  slots, including exact-item rejection and remainder preservation at the requirement cap.
- Test full-stack, half-stack and single-item withdrawal while preserving custom stack components.
- Test aggregate requirements above one physical stack and deterministic withdrawal of incompatible
  component stacks in contribution order.
- Test owner and trusted-player deposit/withdraw authority and rejection for guests, revoked players
  and forged requests.
- Test that creative players follow the same material and permission rules.
- Test concurrent stale transactions so every item remains either with a player or in one durable
  fund and is never duplicated or lost.
- Test that emptying the last contributed stack removes the persisted fund record.
- Expected failure: the current service only contributes to one tracked fund and exposes no
  individual withdrawal transaction.
- If a test passes unexpectedly: verify the test checks the persisted registry after each mutation,
  not only a menu-side stack copy.

### GREEN

- Implement server-authoritative deposit and withdrawal operations identified by cabin, stable
  target and requirement item, with one logical slot aggregating an ordered queue of real stacks.
- Accept only the exact displayed item, cap deposits at the captured requirement and preserve actual
  stack components.
- Flush each successful mutation and refresh viewers of the same cabin.

### REFACTOR

- Share requirement accounting between slot caps, completeness and installation.
- Keep fund operations independent from player inventory scanning; only the explicit icon action in
  Task 4 may search inventory.

## Task 4: Replace the deposit control with registry-backed slots

**Requirement:** Material icons are the fund controls; normal player-inventory shift-click never
chooses a fund; shift-clicking a material icon funds that exact requirement from inventory.

### RED

- Add menu integration tests for left-click, right-click, carried-stack placement, pickup and drag on
  requirement slots.
- Test that shift-clicking a requirement icon searches the player's main inventory and hotbar for
  ordinary/default stacks of the exact item and deposits only the missing amount.
- Test that automatic funding ignores named or otherwise modified matching stacks while deliberate
  carried-stack placement accepts and preserves them.
- Test that shift-clicking a player-inventory or hotbar slot keeps ordinary player-inventory behavior
  and never contributes to any fund.
- Test close, disconnect and access loss with a carried stack, asserting vanilla return/drop behavior
  and no menu-side stranded stack.
- Test two viewers mutating the same requirement and receiving refreshed capped state.
- Expected failure: the current menu has one stateless green deposit slot and display-only
  requirement rows.
- If a test passes unexpectedly: confirm the tested slots commit through the registry service and
  are not a mutable `SimpleContainer` copy.

### GREEN

- Intercept every fund-slot click in the server menu, calculate the complete cursor/inventory/fund
  result, validate it and commit it as one transaction; virtual slots only render synchronized state.
- Implement the explicit icon shift-click inventory search while excluding player-slot quick move
  from contribution behavior and excluding non-default stacks from automatic selection.
- Revalidate cabin identity, target, authority and availability for every mutation.

### REFACTOR

- Isolate vanilla click adaptation from fund-domain transactions.
- Keep slot indexing bounded and deterministic between client and server so future panels can reuse
  it without making the current menu dynamic or unbounded.

## Task 5: Build the compact grouped client screen

**Requirement:** The interface feels like a Minecraft container, conserves screen space, shows
several panels when available and explains disabled actions without permanent text.

### RED

- Add pure layout tests for an approximately 248 by 220 screen, fixed nine-column inventory and
  hotbar, upper panel viewport, side tabs and clipping boundaries.
- Test group/panel scroll calculations and that changing tabs or scrolling cannot address a hidden
  panel's slots or actions.
- Extend dedicated-server startup coverage to ensure no client screen or rendering class is loaded.
- Define manual checks at Auto, Small, Normal and Large GUI scales.
- Expected failure: the current 248 by 276 screen draws blank rectangles, has no slot backplates or
  tabs, and can extend beyond the available height.
- If a test passes unexpectedly: verify actual slot and widget bounds, not only declared image size.

### GREEN

- Render Minecraft-style panel, slot and player-inventory backgrounds in a fixed compact container.
- Add vertical icon tabs for implemented groups and independently scroll stacked panels in the upper
  viewport.
- Use short panel names plus upgrade, status and action icons with hover tooltips; remove the
  dedicated deposit control and long repeated labels.
- Show contributed/required quantities on material slots and provide tooltips for their exact click
  behavior.

### REFACTOR

- Keep all layout and rendering constants client-side and all status decisions server-side.
- Extract reusable tab, panel and tooltip rendering without adding window-specific behavior.

## Task 6: Adapt deliberate installation and reconciliation

**Requirement:** Only the owner can deliberately install a complete valid fund; two-step confirmation
prevents accidental irreversible installation; failure and interruption do not consume materials.

### RED

- Test owner-only installation from the specified target fund and rejection for incomplete,
  obstructed, unavailable, inactive or forged targets with every fund unchanged.
- Test successful installation consumes only its target fund, advances exactly one size and leaves
  unrelated funds unchanged.
- Test that the server rejects a forged final Install click without a matching arm.
- Test that changing the fund revision, target, panel or tab invalidates an armed confirmation and
  that timeout does the same without a persistent cabin mutation.
- Retain interruption/replay tests at every persisted operation boundary and startup reconciliation.
- Expected failure: current installation is tied to the optional tracked fund and its text button has
  no arming step.
- If a test passes unexpectedly: confirm the second action revalidates server state after the first
  action rather than trusting client confirmation state.

### GREEN

- Identify installation actions by stable target and consume that fund only after current server
  validation succeeds.
- Implement per-menu server arming that captures target and fund revision with a short timeout, and
  synchronize the icon/tooltip change to the client. Do not persist arming in cabin state.
- Adapt the persisted operation and reconciliation path to schema-5 target-keyed state.

### REFACTOR

- Keep presentation confirmation separate from durable installation intent.
- Reuse the existing idempotent general-space world effect and automatic-window refresh.

## Task 7: Acceptance and current-play documentation

**Requirement:** The correction is complete only when migration, interaction, layout, restart,
regression, documentation and manual client behavior agree.

### RED

- Follow the current README and record every tracking, green-control and inventory-shift-click step
  that becomes false after the correction.
- Run the new tests against the tracked implementation and retain evidence of their intended failures.

### GREEN

- Update README gameplay instructions from tracking to target-specific material slots only after the
  implementation is playable.
- Run the complete clean GameTest and dedicated-server startup/reload suites.
- Complete the manual checklist with an owner and trusted player, including a migrated partial fund.

### REFACTOR

- Remove obsolete tracking buttons, protocol flags, services, translations and tests.
- Review the final diff against Delivery 3.1a only; leave configurable windows and schema 6 for the
  next coherent change.

## Manual client acceptance checklist

- [ ] The Lodestone opens a compact screen at common GUI scales without clipping; the normal player
  inventory has visible vanilla-style slots and no blank panel.
- [ ] The Cabin icon tab is highlighted, empty future groups are absent, and the upper panel viewport
  scrolls without moving the player inventory.
- [ ] Material icons accept only their displayed item through normal left/right-click and drag,
  preserve excess on the cursor and show contributed/required quantities.
- [ ] Shift-clicking a material icon pulls its exact item from inventory and hotbar; shift-clicking a
  player stack does not contribute.
- [ ] Owner and trusted player can deposit and withdraw; a guest cannot; only the owner sees an
  actionable Install icon.
- [ ] Missing, ready, obstructed and unavailable states are distinguishable by compact icons and
  useful hover text without repeated paragraphs.
- [ ] Install requires two deliberate clicks, cancels after timeout or intervening state changes, and
  revalidates on commit.
- [ ] A schema-4 partial fund appears in the matching schema-5 slots after restart with exact contents.
- [ ] Packing and redeployment preserve a partial fund and existing automatic windows still follow
  exterior conditions.
- [ ] A dedicated server starts without loading client screen or rendering classes.
