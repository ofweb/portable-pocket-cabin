# PDR-0007: Fund and install cabin upgrades

Status: Accepted

## Context

Players must see upgrade effects, costs, access, and failures before a contribution. Multiple upgrades can accept contributions at the same time. Upgrade materials cannot become general cabin storage.

## Decision

The protected interior Lodestone opens **Cabin Upgrades**. Players with cabin access can inspect available categories and panels. The menu checks access again while open. It shows one panel with the effect, specified requirements, progress, status, and blocking reason. Empty categories and categories without implemented actions stay hidden. Each category can contain multiple stable targets with up to sixteen material requirements per panel.

Each target has an [upgrade fund](../../context.md#upgrade-fund) with capacity up to its requirements. Owners and residents can contribute specified items or withdraw saved stacks. Locked or blocked targets reject new contributions. Players can withdraw from their funds. Incorrect items and excess quantities stay with the player. Deliberate deposits keep stack components. Automatic funding from inventory selects only matching stacks without custom data. A player-inventory shift-click does not select a fund. Creative mode follows the same rules.

Only the owner can [install an upgrade](../../context.md#upgrade-installation). A complete fund does not install automatically. Installation requires two clicks on the same target with an unchanged fund. A panel change, fund change, access removal, or timeout cancels confirmation. Before committing, the server validates permissions, requirements, prerequisites, and physical space again. Failed validation leaves the fund available for withdrawal.

Funds stay with their targets through packing, travel, and restart. Storage and automation cannot use them. Cost changes during operation cannot apply to non-empty funds.

## Rationale

Funds for each target let players contribute to multiple upgrades without transferring materials between funds. Restricted slots permit deliberate contributions and corrections. Two clicks confirm installation before using a full fund.

## Scope

This decision applies to the upgrade interface, contribution permissions, and installation controls. [PDR-0006](0006-expand-general-space-with-world-materials.md) states general-space growth. [PDR-0008](0008-purchase-and-reverse-cabin-windows.md) states Cabin window behavior. [PDR-0012](0012-books-reveal-upgrades-before-purchase.md) states book requirements for revealed upgrades. Central storage and automatic crafting are independent features.

## Related records

- [ADR-0002](../adr/0002-use-target-keyed-upgrade-funds.md) states fund persistence and transaction rules.
- [ADR-0003](../adr/0003-persist-window-identity-and-derive-geometry.md) states installation recovery rules.
- [PDR-0009](0009-use-fixed-household-roles.md) states household permissions after the role change.
