# Cabin acquisition and relocation regression checks

The acquisition and relocation experience remains accepted while automated and targeted manual checks establish these rules. [PDR-0004](../../.workflow/decisions/pdr/0004-survival-acquisition-and-relocation.md) and [PDR-0005](../../.workflow/decisions/pdr/0005-keep-the-chosen-cabin-palette.md) state the product decisions.

## Automated checks

- The five recipes and discovery sequence use their declared patterns. Amethyst shards do not replace Blocks of Amethyst.
- The Kit accepts independent supported floor, wall, roof, and door choices. Repeated ingredients within one role must use the same family.
- Vanilla and loaded optional profiles select the correct item and structure blocks. Invalid profiles reject reload without replacing the previous valid set.
- The chosen palette and item identity survive first binding, packing, restart, recovery, and redeployment.
- Interrupted first deployment or redeployment leaves one valid exterior or one current bound item active or owed to its owner.
- Site selection, orientation, preview replacement, and confirmation work in every rotation. Invalid support, obstruction, lava, safety, border, height, dimension, or stale-item checks fail without consuming the item.
- Normal controller use enters. Two owner sneak-uses request packing. Timeout, non-owner use, and failed validation leave cabin state unchanged.
- Packing retains the [foundation safety checks](cabin-foundation.md) for evacuation, entry lock, concurrent requests, and crash recovery.
- Cabin-owned wood resists fire while player wood behaves normally. Portal doors resist physical, redstone, and cosmetic state changes. Structural corner frames remain protected.
- Recognizable legacy corner frames migrate in place without replacing player blocks. Repeated migration is safe, and unrelated damage follows normal recovery.
- Unsupported save versions fail without overwriting valid world data.

## Manual checks

Check recipe-book presentation, tooltips, structure appearance, preview clarity, packing controls, and optional-mod combinations in a client.
