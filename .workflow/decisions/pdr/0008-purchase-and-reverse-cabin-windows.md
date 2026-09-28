# PDR-0008: Purchase and reverse cabin windows

Status: Accepted

## Context

The moving home needs windows that show exterior conditions. Owners also need to change a purchased window without losing its identity or the exact materials paid for each step.

## Decision

The left, rear, and right walls each support up to two [Cabin windows](../../context.md#cabin-window). The entrance wall stays unchanged. A wall's second window becomes available after the first is installed and both footprints fit. Each window has its own identity and six possible [tiers](../../context.md#window-tier): 1×2, 2×2, 3×3, 5×4, 7×6, and 9×8.

One window is centered on its wall. Two form a centered group with at least one solid block between the windows. The layout preserves structural corner frames. Two tier-one windows fit from general size 5, and two tier-six windows fit from size 21. A purchase requires enough wall width and height and cannot displace player blocks or attached decoration. The interface explains the required general size or other obstruction. Expansion can recenter windows without changing their identities or paid steps.

The base purchase includes the exterior-condition color palette. Installed panes indicate dawn, day, sunset, night, rain, thunder, Nether, End, or inactivity. Packing and orphaning show inactive shutters. Deposited dye does not select a pane color. New cabins start with solid walls.

Each successful purchase saves the exact paid stacks for that step. Only the owner may confirm a downgrade or removal. A downgrade refunds the latest tier. Removal refunds the base and all remaining tiers. Grandfathered base windows have an empty base receipt. Refunds appear beside the interior controller and follow ordinary item-entity behavior. Any fund invalidated by the resulting wall layout is named during confirmation and returned separately.

## Rationale

Independent identities let owners arrange windows as the cabin grows. Exact receipts make reversible upgrades fair when costs or stack components differ. Controller-side refunds remain available even when the owner's inventory is full.

## Scope

This decision covers purchased windows and their reversal. [PDR-0007](0007-fund-and-install-cabin-upgrades.md) owns funding and installation permissions. The [bundled progression definition](../../../src/main/resources/data/portable_pocket_cabin/portable_pocket_cabin/progression/default.json) states current costs. Rendered terrain views and player-selected colors are outside scope.

## Related records

- [ADR-0003](../adr/0003-persist-window-identity-and-derive-geometry.md) owns window identity and layout.
- [ADR-0004](../adr/0004-persist-window-reversals-through-refund-ejection.md) owns recovery and exact refunds.
