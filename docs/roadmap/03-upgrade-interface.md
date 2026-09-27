# Milestone 3: Upgrade interface

**Depends on:** [Milestone 2](02-progression-space.md).

**Outcome:** Cabin upgrades are discoverable, understandable and purchased through a protected interface instead of chat messages and sneak-use gestures.

**Status:** Complete. Deliveries 3.1a–3.2b pass automated and manual acceptance.

## Scope

This milestone adds:

- one server-authoritative **Cabin Upgrades** interface
- requirement-capped funds keyed by stable upgrade target
- category tabs and multiple selectable upgrade panels
- individually purchased, resized and reversible windows on three walls
- exact paid-step receipts and material refunds

[Milestone 2](02-progression-space.md) owns general-space geometry, costs and attunement. Central storage, automatic crafting, functional rooms and the complete upgrade ladder belong to later milestones.

## Upgrade interface

Using the protected interior Lodestone opens **Cabin Upgrades**, whether the player is sneaking or not. Anyone allowed inside may inspect upgrades, and access is rechecked while the menu remains open.

The interface is a 248×220 logical-pixel Minecraft-style container:

- the player inventory and hotbar remain fixed at the bottom
- the upper area shows one compact upgrade panel at a time
- previous and next controls navigate panels within the selected category
- vertical icon tabs group upgrades by the cabin part they affect
- the Cabin tab contains whole-cabin structure, size, window and storage upgrades
- each implemented functional room receives a tab, including its automation
- empty and unimplemented categories do not appear
- two rows of eight material slots support up to sixteen exact requirements

Panel, category and confirmation selection are ephemeral menu state. Changing category or panel cancels an armed action. Catalog refresh retains the selected stable target when it still exists.

Permanent text stays minimal. Upgrade, tab, action and status icons provide tooltips. Each panel shows a short name, compact effect, material slots, status icon and owner actions. The status distinguishes ready, missing-material, locked, obstructed and unavailable states; its tooltip gives the complete blocking reason.

Only an upgrade whose prerequisites and spatial requirements pass accepts materials. Locked upgrades may remain visible, but their inactive slots explain the prerequisite. A funded upgrade that later becomes obstructed retains its withdrawable fund until the obstruction is cleared.

## Per-upgrade funds

Each available upgrade owns a separate [upgrade fund](../../.workflow/context.md#upgrade-fund) keyed by its stable target. Persistent cabin state has no tracked or selected upgrade. Viewing a panel or changing tabs has no gameplay effect, and several upgrades may hold materials concurrently.

Each material icon is an interactive fund slot:

- it represents one exact required item and accepts no more than the required total
- it may aggregate several preserved stacks, including totals above the normal stack limit and mutually incompatible component-bearing stacks
- its overlay shows contributed and required quantities
- withdrawals expose preserved stacks in contribution order
- placing a matching carried stack deposits up to the remaining amount
- left-click, right-click and drag behave like restricted container-slot interactions
- shift-clicking the icon pulls ordinary, unmodified matching stacks from the player inventory into that fund
- named or modified stacks are never selected automatically but may be deposited deliberately
- shift-clicking a player-inventory stack retains ordinary inventory behavior and never selects a fund
- wrong items and excess quantities remain with the player
- deposit, persistence and withdrawal preserve exact stack components

The owner and currently trusted players may deposit into and withdraw from available or already-funded upgrades. Other authorized visitors may inspect but not mutate slots. Only the owner may install, downgrade, remove or refund an upgrade. Creative mode grants no permission or material-cost exception.

Fund materials remain bound to their target. They cannot be crafted with, redirected, used by another cabin system or exposed as block inventories, hopper targets, pipe endpoints or general storage. Funds survive restart, packing and redeployment.

Live material-cost changes for a non-empty fund are unsupported until post-MVP balancing defines their behavior.

## Installation and failure handling

Completing a fund does not install its upgrade. The owner's Install control remains disabled until the fund is complete and the target validates; the status tooltip explains each failure.

Installation requires two clicks. The first arms the selected target at its current fund revision. The second commits only while the target and revision still match. Moving away, changing category or panel, mutating the fund or waiting for the timeout cancels confirmation. Arming is not persisted.

Before committing, the server rechecks:

- cabin identity, lifecycle and controller access
- ownership
- the target and its captured requirements
- complete funding
- upgrade prerequisites and target validation

A failed check leaves the fund unchanged and refreshes its status. One typed, persisted transition surrounds each deterministic, idempotent world change, preventing interruption from applying an effect twice or consuming its fund without the effect. Success applies the upgrade and consumes only its complete target fund.

Fund-slot clicks are complete server transactions rather than independent slot callbacks. Each transaction validates the cursor, player inventory and resulting fund before committing and persisting them together. Shared viewers refresh after every mutation. A stale or concurrent rejection does not move the offered stack, and losing access closes or invalidates the interaction.

## Cabin windows

### Identity and placement

The left, rear and right walls each support at most two independently purchased windows. The entrance wall remains unchanged so its controller, exit and composition stay stable.

A wall's first window becomes available when its tier-one footprint fits. The second requires one installed window and enough space for both. Each window retains its identity, installed tier and receipts when resized or repositioned. Removing one recentres the other.

Each window advances one tier at a time:

| Tier | Dimensions |
| --- | ---: |
| 1 | 1×2 |
| 2 | 2×2 |
| 3 | 3×3 |
| 4 | 5×4 |
| 5 | 7×6 |
| 6 | 9×8 |

One window is centred by itself. Two form one centred group separated by at least one solid block. Both layouts preserve the structural corner frames.

The resulting wall layout must pass before its fund accepts materials and again before installation:

- every window fits within the current clear interior height
- one window fits between the structural corner frames
- two windows satisfy `first width + one-block divider + second width <= general cabin size`
- no affected block or attached decoration would be destroyed or displaced

The status tooltip gives the minimum required general-space size or another blocking reason. The two-block-deep corner frames reduce usable wall span by two blocks, so two tier-one windows fit from general size 5 and two tier-six windows fit from size 21.

### Costs and exterior conditions

A new tier-one window costs:

- 16 ordinary Glass Panes
- 4 Amethyst Shards
- one each of Yellow, Orange, White, Light Blue, Magenta, Blue, Light Gray, Cyan, Gray, Red and Purple Dye

Later tiers require only construction materials:

| Upgrade | Ordinary Glass Panes | Amethyst Shards |
| --- | ---: | ---: |
| Tier 1 to 2 | 4 | 1 |
| Tier 2 to 3 | 8 | 2 |
| Tier 3 to 4 | 12 | 3 |
| Tier 4 to 5 | 24 | 6 |
| Tier 5 to 6 | 32 | 8 |

The base purchase includes the complete exterior-condition signal palette. Installed panes show dawn, day, sunset, night, rain, thunder, Nether, End and inactive profiles. Packing and orphaning show inactive shutters; redeployment resumes the exterior-derived profile. Deposited dye does not select installed pane colors.

### Downgrades, removal and refunds

Every successful base and tier installation records an immutable receipt containing the exact paid stacks. Grandfathered base windows have an empty base receipt.

The owner may confirm either action:

- **Downgrade** removes the latest installed tier and refunds only that tier's receipt.
- **Remove window** removes the window and refunds its base receipt and every remaining tier receipt.

Both actions validate the resulting wall before mutation. Refund stacks are ejected beside the interior controller, never inserted into a fund or player inventory. The drop position need not be clear or hazard-free; materialized entities follow normal pickup, movement, fire, lava and despawn behavior.

Purchased-upgrade refunds and uninstalled funds remain independent. If the resulting layout invalidates funded targets, confirmation names them and ejects their exact stacks separately from the refund. Counts and stack data are preserved.

Window reversal uses a persisted two-phase journal for world mutation and indexed refund ejection. It is mutually exclusive with installation, and either journal locks upgrade and fund actions until live or startup recovery completes it.

## Persistence and migration

Schema 7 persists:

- non-empty funds keyed by stable target, each with its requirement snapshot and contributed stacks
- window identities, tiers and paid-step receipts
- an optional active installation
- an optional window-reversal journal

Empty panels resolve from the current catalog and require no saved fund. A synchronized fund revision changes after every successful mutation and invalidates stale confirmations.

Schema 6 retains its windows, receipts, funds and active installation while adding no reversal journal. Schema 5 cabins receive one grandfathered tier-one window on each side wall, recentered under the current rules and carrying no base receipt. Schema 4's optional tracked fund migrates into its matching target fund without changing requirements or stacks. Schema 3 chains through the existing migration. Older and future schemas remain rejected; the [README](../../README.md) is authoritative for the current migration matrix.

New cabins begin with palette-matched solid walls and no windows.

## Integration boundary

Funds remain distinct from [Milestone 4](04-household-storage.md) central storage and [Milestone 7](07-production-automation.md) crafting automation. Those milestones may let the owner choose one upgrade and explicitly fund or craft its missing requirements. Existing fund contents are consumed first. Contributions never enter storage, teach templates or become general automation inputs.

Client code renders synchronized state. Permissions, slot mutations, ejections, refunds and installations remain server-authoritative, and client-only registration remains isolated from dedicated-server initialization.

The catalog resolves grouped descriptors and stable targets from cabin state and definitions. The upgrade service owns fund mutations, validation, installation and refunds. Virtual registry-backed slots aggregate preserved stacks without exposing a block inventory. Upgrade-specific world services apply validated general-space and window effects.

## Evergreen acceptance contract

Milestone 3 remains accepted only while automated tests and targeted manual checks establish that:

1. Normal-use and sneak-use open the same interface; access loss invalidates an open interaction.
2. Category and panel navigation retain valid selections, cancel armed actions and expose no empty category.
3. Fund slots enforce exact-item caps, preserve component-bearing stacks and support left-click, right-click, drag and icon shift-click behavior.
4. Permission checks allow trusted contributions and owner-only installation, downgrade, removal and refund without a Creative-mode bypass.
5. Concurrent or stale fund actions reject atomically without moving items; successful mutations refresh every viewer.
6. Two-click installation revalidates identity, lifecycle, access, ownership, funding, prerequisites and target geometry before one recoverable commit.
7. Window identity, placement, tiering, obstruction checks and exterior-condition projection remain correct across general-space growth.
8. Downgrade and removal refund exact receipts, preserve grandfathered empty receipts and separately eject invalidated funds.
9. Schema 3–6 saves migrate as specified, and interrupted installation or reversal completes safely after restart.
10. Packing and redeployment preserve funds, windows and receipts without exposing client-only code to a dedicated server.

Codec, service, menu-integration, GameTest and dedicated-server restart suites are the automated gates. Manual acceptance covers supported GUI scales, layout, tooltips, confirmation, visual window geometry and multiplayer presentation.

## Implementation history

| Delivery | Result |
| --- | --- |
| 3.1 | Introduced server-authoritative funding and installation through one tracked fund; superseded by 3.1a. |
| 3.1a | Replaced tracking with target-keyed funds and interactive material slots. |
| 3.1b | Added reusable categories, panel navigation and sixteen visible requirement slots. |
| 3.2a | Added purchased windows, independent tiers, receipts and condition projection. |
| 3.2b | Added downgrade, removal, exact refunds and invalidated-fund ejection. |

## Out of scope

- a separate upgrade station or replaceable controller
- an unassigned material wallet or general-purpose upgrade inventory
- implicit fund selection from a shift-clicked player-inventory stack
- direct whole-inventory payment or Creative-mode cost bypass
- hopper, pipe or storage-network access to funds
- central-storage or automatic-crafting implementation
- front-wall windows, a third window on one wall or arbitrary window coordinates
- player-selected window colors or cosmetic window styles
- the complete future upgrade ladder and functional-room implementations
- live cost-definition migration for non-empty funds
- rendered portals or terrain views
- final art beyond the aligned layout, controls and dimensions
