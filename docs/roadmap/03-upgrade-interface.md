# Milestone 3: Upgrade interface

**Depends on:** Milestone 2's world-attuned expansion services.

**Outcome:** Cabin upgrades are discoverable, understandable and purchased through a deliberate
protected interface rather than provisional chat messages and sneak-use gestures.

**Status:** Deliveries 3.1a and 3.1b are implemented and pass automated acceptance; manual client
acceptance remains. Delivery 3.2's configurable windows are aligned but not implemented.

## Deliveries

### Delivery 3.1: Tracked upgrade interface and fund — Superseded by 3.1a

The first interface established server-authoritative funding and installation for the next
general-space expansion. Its single tracked fund was replaced by the correction below.

### Delivery 3.1a: Per-upgrade fund interface correction — Implemented; manual acceptance pending

Replace tracking and the dedicated deposit control with requirement-capped, chest-like material
slots owned by each upgrade. Rebuild the screen as a compact Minecraft-style container with several
upgrade panels, icon tabs and the normal player inventory.

This correction is limited to the existing general-space upgrade. It establishes the reusable
interface and fund model without implementing configurable windows.

### Delivery 3.1b: Multi-panel interface enablement — Implemented; manual acceptance pending

Finish the interface boundary required by configurable windows. Keep vertical icon tabs attached to
the side of the container for upgrade categories, show one upgrade panel at a time within the active
category, and provide previous/next navigation when that category has several panels. Expand the
visible material area to two rows of eight exact-requirement slots so a window's thirteen base
requirements fit without another paging layer.

Panel, category and confirmation selection remain ephemeral menu state. Changing category or panel
cancels an armed action, and catalog refresh retains the selected stable target when it still exists.
Delivery 3.1b changes no cabin save schema, upgrade costs, permissions or installed effects.

### Delivery 3.2: Configurable cabin windows — Aligned

Use the corrected interface and funds to purchase individual functional windows on the left, rear
and right walls. Add independent size tiers, spatial validation, reversible purchased upgrades and
exact material refunds.

The deliveries remain separate coherent changes. Delivery 3.1a is playable with one general-space
panel; Delivery 3.1b makes the interface capable of selecting several panels; Delivery 3.2 supplies
the window panels and their behavior.

## Existing behavior and rollout

Before Delivery 3.1, normal-use of the interior Lodestone sent a chat-only status message and
sneak-use attempted an immediate inventory purchase. Delivery 3.1 removed both interactions and
introduced the currently implemented tracked fund.

Delivery 3.1a migrates every schema-4 tracked fund into the equivalent per-upgrade fund without
changing its requirement snapshot or contributed stacks. No material is ejected or discarded.

Milestone 0 currently gives every generated interior two functional fake-window panels. Delivery
3.1a preserves that behavior. Delivery 3.2 then:

- migrates every cabin that already exists to one grandfathered tier-one window on each side wall
- recentres those windows under the new placement rules and changes their glass blocks to the new
  functional panes
- records no base refund receipt for those free grandfathered windows
- makes cabins created afterwards begin with palette-matched solid walls and no installed windows

## Corrected player-facing behavior

### Upgrade overview

Using the protected interior Lodestone opens one **Cabin Upgrades** interface regardless of whether
the player is sneaking. A player who may enter the cabin may inspect it, and access is rechecked
while the menu remains open.

The interface uses an approximately 248 by 220 logical-pixel Minecraft-style container:

- the normal nine-column player inventory and hotbar remain fixed at the bottom
- the upper area shows one compact upgrade panel at a time, with previous/next navigation when the
  selected category contains several panels
- vertical icon tabs attached to the container group upgrades by the part of the cabin they affect
- the Cabin tab contains whole-cabin structure, size, window and storage upgrades
- each implemented functional room receives its own tab, including automation specific to that room
- empty or unimplemented categories do not appear

Delivery 3.1a initially shows only the next general-space expansion in the Cabin tab. Delivery 3.1b
adds the reusable category and panel navigation plus sixteen visible material slots. Delivery 3.2
populates the Cabin category with window panels without changing the layout.

Permanent text stays minimal. Upgrade, tab, action and status icons provide hover tooltips. Each
panel shows a short upgrade name, compact effect, material slots, status icon and owner action. The
status icon distinguishes ready, missing materials, locked, obstructed and unavailable states; its
tooltip gives the complete blocking reason.

### Availability

Only an upgrade whose prerequisites and spatial requirements currently pass accepts materials.
Locked upgrades may remain visible for discovery, but their material slots are inactive and explain
the prerequisite through their status tooltip.

If an already-funded upgrade later becomes temporarily obstructed, its fund remains intact and
withdrawable. Installation waits until the obstruction is cleared. Live material-cost changes while
a fund is non-empty are unsupported during MVP and are deferred until after MVP balancing.

### Per-upgrade funds

Each available upgrade owns a separate [upgrade fund](../../CONTEXT.md) keyed by its stable target.
There is no tracked or selected upgrade in persistent cabin state. Merely viewing a panel or changing
tabs has no gameplay effect, and several upgrades may eventually hold materials at the same time.

The material icons are interactive fund slots:

- each visible slot represents one exact required item and is capped at that requirement's total
- a requirement may hold several real stacks behind its one icon, including totals above an item's
  normal maximum stack size and mutually incompatible component-bearing stacks
- its overlay shows the aggregate contributed and required quantities; withdrawals expose preserved
  stacks one at a time in contribution order
- placing a carried matching stack on it deposits up to the remaining amount
- normal left-click, right-click and drag interactions behave like restricted container slots
- normal left-click and right-click allow an authorised player to withdraw contributed materials
- shift-clicking a material icon deliberately pulls ordinary/default matching stacks from the
  player's inventory and hotbar into that specific slot, up to its cap
- named or otherwise modified stacks are never selected automatically, but may be deposited
  deliberately by placing them on the slot
- shift-clicking a player-inventory stack retains ordinary player-inventory behavior and never
  chooses an upgrade fund
- wrong items and excess quantities remain with the player
- exact stack components are preserved through deposit, persistence and withdrawal

The owner and current trusted players may deposit and withdraw from any available or already-funded
fund. Trusted players are resident-equivalent for this milestone. Players without contribution
authority may inspect but cannot mutate slots. Only the owner may install, downgrade, remove or
refund an upgrade. Creative mode has no special permission or material-cost behavior.

Fund materials remain bound to their target and cannot be crafted with, redirected to another
upgrade, or used by another cabin system. Funds persist through restart, packing and redeployment,
but are not block inventories, hopper targets, pipe endpoints or general storage capabilities.

### Installation and failures

Completing a fund does not install an upgrade automatically. The owner's icon-only Install control
is disabled until the fund is complete and the target validates; the adjacent status icon always
explains why it is disabled.

Installation uses a compact two-step confirmation enforced by ephemeral state in the open server
menu. The first click arms one target at its current fund revision and changes the icon and tooltip;
the second commits only if the target and revision still match. Moving away, changing tabs, changing
the fund, or waiting briefly cancels confirmation. Arming is not written to cabin save data.

Immediately before the second click commits, the server rechecks:

- cabin identity, lifecycle and controller access
- ownership
- the target and its captured requirements
- complete funding
- upgrade-specific prerequisites and target validation

A failed check leaves the fund unchanged and refreshes its actionable status. Installation remains
a persisted, recoverable transition so interruption cannot apply an effect twice or consume its
fund without that effect. Success applies the upgrade and consumes only that target's complete fund.

### Invalidation and ejection

Ordinary funds are emptied by withdrawing from their slots; there is no tracking-cancellation action.

If an owner-confirmed downgrade or removal of a purchased upgrade would invalidate one or more funded
targets, its confirmation lists those targets. Confirming ejects every affected fund beside the
interior controller, separately from any purchased-upgrade refund. Counts and stack data are
preserved. Dropped stacks follow normal Minecraft pickup, despawn and hard-crash durability semantics.

## Delivery 3.2 cabin windows

### Eligible walls and window identity

Windows are available on three walls: left, rear and right. The entrance wall is excluded so the
controller, exit and entrance composition remain stable.

Each eligible wall supports at most two independently purchased windows. A wall's first window may
be installed when its tier-one footprint fits. Its second becomes available after that wall has one
installed window and the combined layout fits. Each window retains its own installed tier and refund
receipts. Either may later be resized or removed independently; a lone remaining window recentres.

Only the owner may install, resize, remove or refund a window. Residents may deposit into and
withdraw from its available fund under the ordinary fund rules.

### Size tiers and placement

Each individual window advances one tier at a time through this fixed ladder:

| Tier | Dimensions |
|---|---:|
| 1 | 1 x 2 |
| 2 | 2 x 2 |
| 3 | 3 x 3 |
| 4 | 5 x 4 |
| 5 | 7 x 6 |
| 6 | 9 x 8 |

The installed windows on a wall form one centred group. One window is centred by itself. With two,
the pair is centred with at least one solid divider block and the structural corner frames intact.
Installing, resizing or removing may reposition both windows without changing identity, tier or
receipts.

The complete resulting wall layout must pass before its fund accepts materials and again before
installation:

- every window's height is at most the current clear interior height
- one window fits between the inner edges of the structural corner frames
- two windows satisfy `first width + one-block divider + second width <= general cabin size`
- no affected block or attached decoration would be destroyed or displaced

The status tooltip shows the minimum required general-space size or other blocking reason. Two
tier-six windows fit on one wall from general size 19 onward.

### Costs and exterior-condition signal

The base purchase pays once for the complete exterior-condition signal palette. Installed panes
preserve the dawn, day, sunset, night, rain, thunder, Nether, End and inactive profiles. Packing and
orphaning show inactive shutters; redeployment resumes the exterior-derived profile.

Each new tier-one window costs:

- 16 ordinary Glass Panes
- 4 Amethyst Shards
- one each of Yellow, Orange, White, Light Blue, Magenta, Blue, Light Gray, Cyan, Gray, Red and
  Purple Dye

Later tiers require only added construction material:

| Upgrade | Ordinary Glass Panes | Amethyst Shards |
|---|---:|---:|
| Tier 1 to 2 | 4 | 1 |
| Tier 2 to 3 | 8 | 2 |
| Tier 3 to 4 | 12 | 3 |
| Tier 4 to 5 | 24 | 6 |
| Tier 5 to 6 | 32 | 8 |

The deposited items are construction requirements. Installed pane colors remain controlled by the
existing exterior-condition logic rather than player-selected colors.

### Downgrades, removal and refunds

Every successful base and size-tier installation records an immutable receipt containing the exact
material stacks paid for that step. Grandfathered base windows have an empty base receipt.

The owner may use either confirmed action:

- **Downgrade** removes the latest installed size tier and refunds only that tier's receipt.
- **Remove window** removes the complete window and refunds its base receipt plus every remaining
  tier receipt.

Both actions validate the resulting wall first. Refund stacks are physically ejected beside the
interior controller and never enter an upgrade fund or player inventory.

Purchased-upgrade refunds and uninstalled funds are otherwise independent. If the resulting cabin
state invalidates funded targets, the confirmation warns about them and the handled operation ejects
those funds alongside, but separately from, the refund.

## Future material sources

Funds remain distinct from central storage in Milestone 4 and crafting automation in Milestone 7.

- After purchasing central storage, the owner may explicitly choose one upgrade and draw its exact
  remaining requirements from storage.
- After crafting automation is installed, the owner may approve a bounded plan that makes one
  upgrade's missing ingredients from raw materials in storage.
- Existing fund contents are consumed first. Neither source acts without owner approval.
- Contributions do not enter central storage, teach templates or become general automation inputs.

Those integrations belong to their respective milestones; Milestone 3 establishes only their
transaction boundary.

## Technical approach

The protected interior Lodestone remains the entry point. Client code renders synchronized state;
every permission, slot mutation, ejection, refund and installation decision remains server-authoritative.
Client-only registration remains isolated from dedicated-server initialization.

Upgrade behavior is divided into:

- a catalog resolving grouped visible descriptors and stable targets from cabin state and definitions
- a service owning deposits, withdrawals, invalidation ejection, validation, installation and refunds
- virtual registry-backed requirement slots providing container interactions without an exposed
  block inventory; each visible slot aggregates a queue of preserved real stacks
- upgrade-specific world services applying validated general-space and window effects

Schema 5 persists a collection of non-empty funds keyed by stable target. Each fund contains its
resolved requirement snapshot and contributed item stacks. Empty fund panels resolve from the current
catalog and need no saved record. Schema 4 migrates its optional tracked fund into the equivalent
schema-5 fund, preserving target, requirements and stacks. Schema 3 chains through the existing
migration; older and future schemas remain rejected.

Fund-slot clicks are intercepted as complete server-side transactions rather than independent
`Slot` callbacks. Each transaction computes and validates the cursor, player inventory and fund
result before committing them together and persisting the fund. Virtual slots synchronize only the
resulting display. Shared viewers refresh after every mutation. A stale or concurrent rejection
does not move the offered stack. Losing access closes or invalidates interaction.

Each cabin's synchronized fund revision changes after a successful fund mutation. The open server
menu records target and revision when Install is first armed, rejects forged final clicks without an
arm, and cancels the arm after a revision change, target change or timeout.

Installation keeps the existing persisted transition around deterministic, idempotent world changes.
Delivery 3.2 advances schema 5 to schema 6 for per-window state and receipts.

## Documentation impact

Delivery 3.1a acceptance replaces the README's currently playable tracking instructions with the new
fund-slot interactions. Delivery 3.2 acceptance adds window purchasing, resizing and refund guidance.
Milestones 0, 4 and 7 link to these rules rather than duplicating them.

## Delivery 3.1a testing scope

Confidence is required at five levels:

- codec and service GameTests for schema migration, multiple target-keyed funds, requirements above
  one stack, incompatible component stacks, exact-item caps, permissions, deposit, withdrawal and
  installation atomicity
- menu integration tests for cursor placement, right-click, drag, icon shift-click funding, ordinary
  player-inventory shift-click, concurrent viewers, stale actions and access loss
- restart coverage for migrated and partially funded schema-5 cabins through packing and redeployment
- client/manual acceptance at several GUI scales for layout, tabs, scrolling, tooltips, status and
  two-step confirmation
- the complete GameTest and dedicated-server startup/reload suites as regression gates

Configurable-window behavior remains excluded except that the corrected interface and state model
must not prevent Delivery 3.2 from adding target-keyed funds.

## Out of scope

- configurable window implementation in Delivery 3.1a
- a separate upgrade station or replaceable controller block
- an unassigned material wallet or general-purpose upgrade inventory
- shift-clicking a player-inventory stack into an implicitly chosen fund
- direct whole-inventory payment or creative-mode cost bypass
- hopper, pipe or storage-network access to funds
- central storage or autocrafting implementation
- front-wall windows, a third window on one wall or arbitrary window coordinates
- player-selected window colors or cosmetic window styles
- the complete future upgrade ladder and functional-room implementations
- handling live material-cost changes for a non-empty fund before post-MVP balancing
- real rendered portals or terrain views

## Deferred decisions

- exact art beyond the aligned Minecraft-style panels, icons, slots and dimensions
- post-MVP handling of live cost-definition changes for non-empty funds
- later central-storage and autocrafting transaction details
