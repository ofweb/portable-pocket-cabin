# Cabin upgrade regression checks

The cabin upgrade experience remains accepted while automated and targeted manual checks establish these rules. [PDR-0007](../../.workflow/decisions/pdr/0007-fund-and-install-cabin-upgrades.md) and [PDR-0008](../../.workflow/decisions/pdr/0008-purchase-and-reverse-cabin-windows.md) state the product decisions.

## Automated checks

- Normal and sneak use open the same menu. Loss of access invalidates an open interaction.
- Category and panel navigation retain valid targets, cancel armed actions, and show no empty category.
- Fund slots enforce exact-item caps and preserve stack components through left-click, right-click, drag, shift-click, withdrawal, and restart.
- Trusted players can contribute and withdraw. Only the owner can install, downgrade, remove, or refund. Creative mode adds no exception.
- Stale or concurrent fund actions reject atomically without moving items. Accepted actions refresh every viewer.
- Two-click installation rechecks identity, lifecycle, access, ownership, funding, prerequisites, and geometry before one recoverable commit.
- Windows retain identity, correct placement, tiers, and exterior-condition signals across general-space growth, packing, and redeployment.
- Downgrade and removal refund exact receipts. Grandfathered empty receipts and invalidated funds remain distinct.
- Schemas 3–6 migrate as specified. Interrupted installation and reversal complete safely after restart.
- Dedicated-server startup and reload work without client-only classes.

## Manual checks

Check GUI scales, layout, tooltips, confirmation, visual geometry, and multiplayer presentation in a client. Codec, service, menu-integration, GameTest, and dedicated-server restart suites are the automated gates.
