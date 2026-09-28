# Cabin expansion regression checks

The expansion experience remains accepted while automated tests and targeted manual checks establish these rules. [PDR-0006](../../.workflow/decisions/pdr/0006-expand-general-space-with-world-materials.md) states the product decision.

## Automated checks

- New cabins have a protected, palette-aware 4×4 usable interior.
- Each expansion adds exactly one usable block to both dimensions. The entrance and existing player blocks stay in place.
- Horizontal or vertical obstructions leave the shell, progression state, and committed materials unchanged.
- Clear height follows saved general size and stops at ten blocks from size 20.
- Definitions reject missing size steps, invalid ingredients, and unsupported maximum sizes.
- One attunement applies to all players and cabins. Restart and reload preserve its version and exact wood profile.
- Missing optional profiles leave valid bundled candidates available. Invalid saved attunement stops upgrades with an actionable error.
- Cabins at the configured maximum have no further general-space offer.
- Separate cabin cells cannot overlap at any supported size. Simulation tickets follow the current bounds.
- Unsupported fixed-height saves fail without overwriting registry state or pocket-space blocks.

## Manual checks

Check the current upgrade interface, visual geometry, and multiplayer presentation in a client. The GameTest and dedicated-server restart suites are the normal automated gates.
