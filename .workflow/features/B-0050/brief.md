# Players grow and forage the world's cooking plants

Status: Ready
Feature ID: B-0050

## Goal

The cooking catalogue needs grains, vegetables, chilies, herbs, spices, fruit, tea, and coffee that Minecraft doesn't have. This feature adds all 118 of them as distinct plants, each with its real growing needs. Each grows wild where its wild form lives, so cooking widely means travelling widely. Players bring seeds, cuttings, and saplings home and grow them in fields, the cabin greenhouse, and the arboretum.

It is phase 4 of the [cooking program](../../cooking/program.md), and completes most recipe chains from phases 1–3.

## Stories and acceptance

### S1: Find plants in the wild

Story: A player explores and finds wild cooking plants in fitting biomes.

Acceptance:

- **The list:** every plant in the [raw ingredient sources](../../cooking/sources.md) marked B-0050, 118 in all, exists as its own plant with its own seeds, cuttings, or sapling, and its own art.
- **Wild patches:** each grows wild in the biomes the sources list, as natural patches like vanilla sweet berries or wild flowers. Biomes with many plants share them without crowding out vanilla vegetation.
- **No Mediterranean biome:** windswept savannas stand in for it, with olives, rosemary, and oregano.
- **Where to look:** wandering traders and village farmers occasionally sell seeds of plants from far-off biomes. This gives players another way to start, but exploring stays the main one.

### S2: Grow field crops, vegetables, herbs, and spices

Story: A player plants and harvests cooking crops.

Acceptance:

- **Growing needs:** each crop has the growing needs the sources and the design give it:
  - **Soil:** farmland, sand, or flooded farmland for paddy rice.
  - **Light:** the level it needs.
  - **Water:** whether it needs water nearby.
  - **Support:** whether it climbs a stick or a trellis.
- **Growth and harvest:** crops grow in stages like vanilla crops and give their ingredient on harvest.
- **One plant, several ingredients:** cilantro gives leaves, and coriander seed when left to flower. Olives are green or black by ripeness. Tea leaves become sencha, matcha, or black tea by how they are processed.
- **Perennials:** herbs and spices that grow back after harvest, such as rosemary, mint, and lemongrass, are bushes that regrow without replanting.

### S3: Grow fruit trees

Story: A player plants a fruit tree and picks its fruit.

Acceptance:

- **Saplings:** fruit trees are new tree species grown from saplings found in their biomes.
- **Fruiting leaves:** the leaves flower and then bear fruit. Using a fruiting leaf picks the fruit without breaking the tree, and it fruits again.
- **The fruit:** olive, lemon, lime, orange, apricot, plum, mango, coconut, cocoa, and the other tree fruit in the sources.
- **Arboretum:** fruit trees are selectable tree types in the [B-0023](../B-0023/brief.md) arboretum.

### S4: Use the greenhouse

Story: A household grows its cooking plants in the cabin greenhouse.

Acceptance:

- **Planting:** every new crop, bush, and sapling can grow in the [B-0019](../B-0019/brief.md) greenhouse when its growing needs are met there.
- **Catch-up:** greenhouse catch-up treats new plants like vanilla crops. They progress toward their mature state while the cabin is packed, and are never harvested or replanted by catch-up.

### S5: Complete the chains

Story: With the plants available, the dishes and products that need them become makeable.

Acceptance:

- Every recipe and intermediate product held back by the program's [rule for shared chains](../../cooking/program.md#rule-for-shared-chains) ships when this feature supplies its last ingredient. That excludes those still waiting for butchery from B-0049.
- **Seasonal plants:** elderflower and elderberry, and rosehip, come from two growth stages of the same bush, because Minecraft has no seasons.

## Feature-wide constraints and acceptance

- Plants follow vanilla rules for bone meal, trampling, and breaking. Their own growing needs replace vanilla's only where the plant really needs something different.
- Every new plant and ingredient appears in the cookbook, with where it grows and how to grow it.
- Growing needs and wild placement are data, so datapacks can adjust them.

## Scope

- **Plants:** 118 plants: 19 grains and field crops, 24 vegetables, 5 chilies, 17 herbs, 17 spices and roots, 28 fruit, and 4 tea, coffee, and mate plants. This includes their seeds, cuttings, and saplings, world placement, growing rules, art, and cookbook entries.
- **Integrations:** the greenhouse, the arboretum, and trades.
- **Recipes:** every recipe completed by these plants.

## Non-goals

- **B-0026:** farm automation.
- **Seasons and climate:** seasons, and a temperature or climate system. Biomes and growing needs stand in for them.
- **Non-food plants:** plants grown only for decoration or non-food use.

## Related records

- [Cooking program](../../cooking/program.md).
- [Raw ingredient sources](../../cooking/sources.md).
- [Greenhouse](../B-0019/brief.md) and [arboretum](../B-0023/brief.md).
- [Texture audit](../../cooking/texture-audit.md).

## Open questions and assumptions

- **Delivery:** the design settles the order the plants ship in. Grouping by biome lets each delivery open a region's cooking.
- **Art:** the texture audit lists candidate sources. Plants without a found source need new art.
- **Tuning:** B-0033 tunes yields, growth times, and wild frequency.
