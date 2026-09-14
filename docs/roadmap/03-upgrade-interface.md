# Milestone 3: Upgrade interface

**Depends on:** Milestone 2's world-attuned expansion services.

**Outcome:** Cabin upgrades are discoverable, understandable and purchased through a deliberate protected interface rather than provisional chat messages and sneak-use gestures.

**Status:** Behavior, technical approach and pushback are aligned. Documentation and testing alignment remain before implementation.

## Deliveries

### Delivery 3.1: Tracked upgrade interface and fund — Aligned

Replace the provisional Milestone 2 controller interaction with one player-facing interface for inspecting, tracking, funding and installing the next general-space expansion. This delivery establishes the reusable upgrade-fund contract without changing the existing automatic fake windows.

### Delivery 3.2: Configurable cabin windows — Aligned

Use the Delivery 3.1 interface and fund to purchase individual functional windows on the left, rear and right walls. Add independent size tiers, spatial validation, reversible purchased upgrades and exact material refunds.

The deliveries are intentionally separate coherent changes. Delivery 3.1 remains playable on its own; Delivery 3.2 does not need to duplicate or replace the funding system.

## Existing behavior and rollout

Normal-use of the interior Lodestone currently sends a chat message containing the cabin's current and maximum size, attuned wood and next expansion requirements. Sneak-use immediately attempts to purchase that expansion from the owner's inventory.

Delivery 3.1 replaces those interactions. The world-attunement, requirement-resolution, validation and expansion services remain authoritative; the delivery replaces their presentation and player interaction rather than the installed general-space state.

Milestone 0 currently gives every generated interior two functional fake-window panels. Delivery 3.1 preserves that behavior for all cabins so it does not remove windows before their replacement purchase flow exists. Delivery 3.2 then:

- migrates every cabin that already exists to one grandfathered tier-one window on each side wall
- recentres those windows under the new placement rules and changes their glass blocks to the new functional panes
- records no base refund receipt for those free grandfathered windows
- makes cabins created after the migration begin with palette-matched solid walls and no installed windows

## Delivery 3.1 player-facing behavior

### Upgrade overview

Using the protected interior Lodestone opens one **Cabin Upgrades** interface regardless of whether the player is sneaking. Sneak-use no longer purchases anything.

The overview shows:

- the cabin's current and maximum general-space size
- its world-attuned wood
- only the next general-space expansion rather than the complete expansion ladder
- each visible upgrade's effect, resolved requirements, funding state and blocking reason

Delivery 3.2 adds per-wall window installation, tier, fit and refund controls to this same overview.

A player who may enter the cabin may inspect the overview. The interface exposes only server-authoritative state and rechecks access while it remains open.

### Tracking

The owner selects one available upgrade and deliberately chooses **Track upgrade**. A cabin may track only one upgrade at a time.

The tracked upgrade is pinned prominently in the interface. Its requirements show contributed, required and missing quantities. Other upgrades remain inspectable but cannot receive contributions until the owner stops tracking the current upgrade.

Tracking captures the resolved requirement set presented to the players. A reload that removes the upgrade or changes that requirement set makes the tracked upgrade stale. A stale upgrade cannot receive contributions or be installed; its existing fund remains intact until the owner stops tracking it.

### Upgrade fund

The [upgrade fund](../../CONTEXT.md) belongs to the cabin and exclusively serves its tracked upgrade. It is not general-purpose storage.

- The fund is accessible only through the tracked upgrade's funding view.
- Players manually transfer items into that view; the cabin never scans or deducts their inventory.
- Hoppers, pipes, ordinary inventories and external storage systems cannot insert into, extract from or discover the fund.
- Only currently missing required items are accepted, and each material is capped at its outstanding quantity.
- Wrong items and excess quantities remain with the contributing player.
- Contributions become cabin property and cannot be withdrawn as individual stacks.
- Fund materials cannot be crafted with, redirected to another upgrade or used by another cabin system.
- Tracking, the resolved requirements and contributed stacks persist through restart, packing and redeployment.

The owner and current trusted players may contribute. Trusted players act as resident-equivalent contributors during this milestone and are migrated to the formal resident role in Milestone 4. Contribution permission does not grant authority to track, stop tracking, install, downgrade, remove or refund an upgrade.

Creative mode does not waive an upgrade's funding requirement or any permission or validation rule.

### Stopping tracking and ejection

Only the owner may stop tracking. The interface requires confirmation and warns that the fund will be physically ejected.

On confirmation, the fund stops accepting contributions, open funding views close or refresh, and every contributed stack is dropped beside the interior controller as if a full chest had been broken. Counts and stack data are preserved. The tracked upgrade is cleared only as part of the complete handled operation; otherwise the fund remains intact.

Dropped stacks follow normal Minecraft item-entity behavior: nearby players may collect them, and abandoned items may despawn. The ejection shares the hard-crash persistence boundary of breaking a vanilla chest. Selecting another upgrade never ejects the current fund implicitly.

### Installation and failures

Completing the fund does not install an upgrade automatically. Only the owner may choose **Install upgrade**.

Immediately before installation, the server rechecks:

- cabin identity, lifecycle and controller access
- ownership
- the tracked upgrade and its captured requirements
- the currently loaded upgrade definition
- complete funding
- upgrade-specific prerequisites and target validation

A failed check leaves the tracked upgrade and fund unchanged and presents its actionable reason in the interface. An obstructed general-space expansion may remain tracked and funded while the owner clears the obstruction. Installation is a persisted, recoverable transition so an interruption cannot apply an upgrade twice or consume its fund without its effect.

Successful installation applies the upgrade, consumes the complete fund and clears tracking.

## Delivery 3.2 cabin windows

### Eligible walls and window identity

Windows are available on three walls: left, rear and right. The entrance wall is excluded so the controller, exit and entrance composition remain stable.

Each eligible wall supports at most two independently purchased windows. A wall's first window may be installed immediately when its tier-one footprint fits. Its second window becomes available after that wall has one installed window and the combined layout fits. Each window retains its own installed tier and refund receipts. Either window may later be resized or removed independently; a lone remaining window recentres.

Only the owner may track, install, resize, remove or refund a window. Residents may contribute to a tracked window purchase under the ordinary fund rules.

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

The installed windows on a wall form one centred group. One window is centred by itself. With two windows, the pair is centred with at least one solid divider block and the structural corner posts intact. Installing, resizing or removing a window may therefore reposition both windows on that wall without changing the other window's identity, tier or receipts.

The complete resulting wall layout must pass before the owner can track or commit a change:

- every window's height is at most the cabin's current clear interior height
- one window fits between the structural corner posts
- two windows satisfy `first width + one-block divider + second width <= general cabin size`
- no affected block or attached decoration would be destroyed or displaced

The interface shows the minimum required general-space size and the blocking reason. It refuses to track an upgrade that cannot fit and repeats validation immediately before installation. Two tier-six windows fit on one wall from general size 19 onward.

### Costs and exterior-condition signal

The base window purchase pays once for the complete exterior-condition signal palette. The installed panes preserve the existing dawn, day, sunset, night, rain, thunder, Nether, End and inactive profiles. Packing and orphaning show the inactive shutter profile; redeployment resumes the exterior-derived profile.

Each new tier-one window has this data-driven cost:

- 16 ordinary Glass Panes
- 4 Amethyst Shards
- one each of Yellow, Orange, White, Light Blue, Magenta, Blue, Light Gray, Cyan, Gray, Red and Purple Dye

Later tiers require only the added construction material:

| Upgrade | Ordinary Glass Panes | Amethyst Shards |
|---|---:|---:|
| Tier 1 to 2 | 4 | 1 |
| Tier 2 to 3 | 8 | 2 |
| Tier 3 to 4 | 12 | 3 |
| Tier 4 to 5 | 24 | 6 |
| Tier 5 to 6 | 32 | 8 |

These costs roughly follow the newly added pane area, rounded to groups of four. The deposited pane and dye items are construction requirements; the installed panes' changing colors remain driven by the existing exterior-condition logic rather than player-selected colors.

### Downgrades, removal and refunds

Every successful base installation and size-tier installation records an immutable receipt containing the exact material stacks paid for that step. Later definition or datapack changes do not alter an existing receipt. Grandfathered base windows have an empty base receipt.

The owner may use either of two confirmed actions:

- **Downgrade** removes the latest installed size tier and refunds only that tier's receipt.
- **Remove window** removes the complete window and refunds its base receipt plus every remaining tier receipt.

Both actions validate the resulting wall first. They physically eject the refunded stacks beside the interior controller under the same pickup, despawn and hard-crash semantics as stopping tracking; refunds never enter the upgrade fund or a player inventory.

A purchased-upgrade refund and an unpurchased tracked upgrade are otherwise independent. Resizing or removing a purchased window does not alter the tracked upgrade or its fund. If the resulting cabin state would invalidate the tracked target, however, the confirmation explicitly warns that the tracked upgrade will also be cancelled. Confirming then untracks it and ejects its fund alongside, but separately from, the refund stacks.

## Future material sources

The fund remains distinct from the central storage introduced in Milestone 4 and the crafting automation introduced in Milestone 7.

- After a cabin has purchased central storage, the owner may explicitly choose to draw a tracked upgrade's remaining exact requirements from that storage.
- After crafting automation is installed, the owner may approve a shown bounded crafting plan that makes missing upgrade ingredients from raw materials in central storage.
- Fund contributions are consumed first. Neither later source acts without owner approval.
- Contributions do not enter central storage, teach item templates or become inputs available to general automation.

Those integrations belong to their respective milestones; Milestone 3 establishes only the boundary they will use.

## Technical approach

### Shared interface and fund

The existing interior Lodestone remains the protected entry point. A custom menu-backed client screen presents the overview, funding slots, progress, confirmations and inline failures. Client code renders synchronized state only; every permission, contribution, refund and installation decision remains server-authoritative. Client-only registration is isolated from dedicated-server initialization.

Upgrade behavior is divided into:

- a catalog that resolves visible upgrade descriptors and stable targets from cabin state and loaded definitions
- a service that owns tracking, contribution, ejection, validation, installation and refund mutations
- upgrade-specific world services that apply validated general-space and window changes

The cabin registry persists an optional tracked-upgrade record containing its stable target identifier, resolved requirement snapshot and contributed item stacks. The fund is registry-owned and deliberately exposes no general inventory or automation capability.

Upgrade costs remain data-driven while effects and allowed target types remain code-defined. Datapacks may balance known upgrades but cannot declare arbitrary executable effects. Resolved duplicate ingredients are consolidated before an upgrade is tracked.

All menu actions identify the open menu and are revalidated against current server state. Shared viewers receive refreshed progress after a contribution or owner action. Losing access invalidates further interaction.

Installation uses a persisted transition around deterministic, idempotent world changes and final fund consumption. Startup reconciliation finishes an interrupted installation. Ordinary fund persistence and physical ejection follow vanilla container and item-entity durability semantics.

Delivery 3.1 advances the cabin registry from schema 3 to schema 4. The explicit migration gives existing cabins no tracked upgrade and an empty fund while leaving their current automatic windows unchanged.

### Window state and rendering

Delivery 3.2 advances schema 4 to schema 5, with migrations chaining for an older schema 3 save. Per-window state records its eligible wall, stable identity, installed tier and the actual material stacks in each paid-step receipt. The migration creates left and right tier-one state with empty base receipts for every existing cabin; new schema 5 cabins begin with no window state.

Window geometry is derived from current general size and the installed window states rather than storing block coordinates. Profile refreshes apply the existing exterior-condition selection across the derived pane positions. Installation, downgrade and removal use recoverable, idempotent state transitions so restart reconciliation cannot duplicate an effect or refund.

## Documentation impact

Delivery 3.1 acceptance updates the README's playable controller interaction, funding and general-expansion instructions. Delivery 3.2 acceptance updates its new-cabin, window-purchase, resizing and refund instructions. Milestones 0, 2, 4 and 7 link to the superseding tracking, contribution and future material-source rules rather than duplicating them.

## Out of scope

- a separate upgrade station or replaceable controller block
- more than one concurrently tracked upgrade
- an unassigned material wallet or general-purpose upgrade inventory
- direct player-inventory payment or creative-mode cost bypass
- hopper, pipe or storage-network access to the fund
- central storage or autocrafting implementation
- front-wall windows, a third window on one wall or arbitrary window coordinates
- player-selected window colors or cosmetic window styles
- the complete future upgrade ladder and functional-room upgrades
- real rendered portals or terrain views

## Deferred decisions

- high-level automated, integration, regression, manual and acceptance testing scope
- exact screen composition and visual styling within the aligned information hierarchy
- later central-storage and autocrafting transaction details
