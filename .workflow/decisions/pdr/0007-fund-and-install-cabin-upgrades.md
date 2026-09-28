# PDR-0007: Fund and install cabin upgrades

Status: Accepted

## Context

Players need to understand upgrade effects, costs, access, and failures before a material contribution. Several upgrades may need contributions at the same time. Material contributions must stay separate from general cabin storage.

## Decision

Using the protected interior Lodestone opens **Cabin Upgrades**. Authorized visitors may inspect the available categories and panels. The menu rechecks access while open. It shows one upgrade panel at a time, with its effect, exact requirements, progress, status, and blocking reason. Empty or unimplemented categories stay hidden. A category can contain several stable upgrade targets and up to sixteen material requirements per panel.

Each target has its own requirement-capped [upgrade fund](../../context.md#upgrade-fund). Owners and residents may contribute exact required items or withdraw preserved stacks. Locked or obstructed targets reject new contributions. Existing funds remain withdrawable. A wrong item or excess quantity stays with the player. Deliberate deposits preserve stack components. Automatic funding from the inventory selects only ordinary matching stacks. A player-inventory shift-click does not choose a fund. Creative mode grants no exception.

Only the owner may [install an upgrade](../../context.md#upgrade-installation). A complete fund never installs itself. Installation needs two clicks on the same target and unchanged fund. A panel change, fund change, loss of access, or timeout cancels confirmation. The server checks permission, requirements, prerequisites, and physical space again before installation commits. A failed check leaves the fund available for withdrawal.

Funds survive packing, relocation, and restart. They remain bound to their targets and unavailable to ordinary storage or automation. Live cost changes for a non-empty fund are unsupported.

## Rationale

Separate funds let players support several improvements without moving materials between targets. Restricted slots make contributions deliberate and correctable. Two-click confirmation protects a fully funded purchase from accidental installation.

## Scope

This decision covers the cabin upgrade interface, contribution permissions, and installation controls. [PDR-0006](0006-expand-general-space-with-world-materials.md) owns general-space growth. [PDR-0008](0008-purchase-and-reverse-cabin-windows.md) owns window behavior. Central storage and automatic crafting are separate work.

## Related records

- [ADR-0002](../adr/0002-use-target-keyed-upgrade-funds.md) owns fund persistence and transactions.
- [ADR-0003](../adr/0003-persist-window-identity-and-derive-geometry.md) owns recoverable upgrade installation.
- [PDR-0009](0009-use-fixed-household-roles.md) owns household permission after the role change.
