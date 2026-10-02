# PDR-0008: Purchase and reverse Cabin windows

Status: Accepted

## Context

Cabin windows show exterior conditions. Reversal must keep window identities and return the same stacks used for each purchased step.

## Decision

Left, rear, and right walls each permit two [Cabin windows](../../context.md#cabin-window) at most. The entrance wall stays unchanged. A wall's second Cabin window becomes available after the first is installed and their footprints fit. Each has an identity and six [window tiers](../../context.md#window-tier): 1×2, 2×2, 3×3, 5×4, 7×6, and 9×8.

The north, west, and east wall centers are reserved for passages. Windows cannot occupy those reserved centers, even before a corridor is installed. Windows sit beside the reserved openings and keep structural corner frames. Exact footprints and minimum sizes remain open; they must fit beside the passages defined by the [room brief](../../features/B-0005/brief.md). Purchase requires enough wall width and height without replacing player blocks or attached decoration. The interface shows the minimum main-room size or the blocking obstruction. Expansion can reposition Cabin windows without changing identities or purchased steps.

The base purchase includes the exterior-condition color palette. Panes show dawn, day, sunset, night, rain, thunder, Nether, End, or a cabin without an active exterior. Packing and exterior loss show inactive shutters. Dye deposits do not select pane color. New cabins have solid walls.

Each purchase saves the stacks used for that step. Only the owner can confirm downgrade or removal. Downgrade refunds the last tier. Removal refunds the base and all installed tiers. Base windows from earlier schema migration have empty base receipts. Refunds appear beside the interior controller and follow Minecraft item-entity behavior. Confirmation identifies funds invalidated by the resulting layout. Their materials return independently of the upgrade refund.

## Rationale

Reserving wall centers keeps window purchases from blocking future passages. Independent identities keep Cabin windows through expansion. Saved purchase stacks keep refunds correct after cost changes and preserve stack components. Items beside the controller stay available when the owner's inventory is full.

## Scope

This decision applies to purchased Cabin windows and reversal. [PDR-0007](0007-fund-and-install-cabin-upgrades.md) states funding and installation permissions. The [cost definition](../../../src/main/resources/data/portable_pocket_cabin/portable_pocket_cabin/progression/default.json) gives costs. Terrain views and colors selected by players are outside this scope.

## Related records

- [ADR-0003](../adr/0003-persist-window-identity-and-derive-geometry.md) states window identity and layout rules.
- [ADR-0004](../adr/0004-persist-window-reversals-through-refund-ejection.md) states recovery and refund rules.
