# B-0050 design: cooking crops and foraging

Status: Ready
Feature ID: B-0050

This design implements the [B-0050 brief](brief.md), phase 4 of the [cooking program](../../cooking/program.md). It adds 118 plants and completes the recipe chains they unlock. It ships region by region, each delivery a pull request of its own.

## Decisions

- **Delivery:** one engine delivery with the first region, then one delivery per biome region. Each delivery ships its plants and every recipe whose last missing ingredient it supplies.
- **Art:** placeholders reuse and tint vanilla textures, as the earlier cooking features do. [B-0034](../../backlog.md#b-0034-cabin-presentation) adds real art.
- **Recipes:** each delivery ships every catalogue dish and intermediate product it completes. Chains that still wait for [B-0049](../../backlog.md#b-0049-butchery-and-carcasses) butchery stay out.
- **Arboretum:** fruit trees grow outdoors and in the greenhouse now. [B-0023](../B-0023/brief.md) adds them as arboretum tree types when it is built, and its brief records that.

## Plant engine

### Plant kinds

Every plant is one of seven kinds. Each kind is one block class, configured per plant:

| Kind | Like | Grows | Harvest |
|---|---|---|---|
| Field crop | Wheat | Seeds on farmland, or on sand for crops that need it, through 4 or 8 stages | Breaking a ripe crop drops its ingredient and seeds |
| Trellis crop | - | Seeds used on a trellis standing on farmland | Using a ripe vine picks it, and it drops back two stages |
| Paddy crop | - | Seedlings in a water source one block deep over dirt, mud, or farmland | Breaking a ripe crop drops its grain and seedlings |
| Herb bush | Sweet berry bush | A cutting on grass, dirt, or farmland | Using a ripe bush picks it, and it drops back two stages. It grows back without replanting. |
| Two-season bush | Sweet berry bush | Like an herb bush, with a flowering stage and a fruiting stage | Flowers at one stage, fruit at the next: elderflower then elderberry |
| Fruit tree | Oak | A sapling grows into a vanilla-wood trunk with the species' fruiting leaves | Using fruiting leaves picks the fruit, and the leaves flower and fruit again |
| Wild forage | Mushrooms, kelp | Only wild, and on its block: sea plants underwater, mushrooms on logs | Breaking it drops the ingredient. Some can also be farmed as field crops. |

Rules for every plant:

- **Vanilla behaviour:**
  - Plants follow vanilla rules for bone meal, trampling, and breaking.
  - Field and paddy crops extend `CropBlock`. Bushes and leaves keep an `age` property, so greenhouse catch-up treats them like vanilla crops.
  - A `CookingPlant` marker interface lets the greenhouse allowlist accept them.
- **The trellis:** a craftable block of sticks that stands on farmland. These plants climb one, as the [sources](../../cooking/sources.md) say:
  - **Delivery 1:** cucumber, white grape, and hops.
  - **Delivery 3:** tomato, grape, and red grape.
  - **Delivery 6:** long bean, peppercorn, vanilla, and betel.
- **One plant, several ingredients:**
  - Cilantro gives leaves when ripe, and coriander seed if it is left one stage longer to flower.
  - Olive leaves give green olives at the first fruit stage and black olives at the second.
  - Tea gives tea leaves. A tea bush kept under a roof for its last stage gives shaded tea leaves.
- **Rosehip:** using shears on a vanilla rose bush gives one or two rosehips about one time in three, wears the shears, and leaves the bush in place.
- **Bean sprouts:** dried soybeans sprout in the fermentation crock.

### Growing needs as data

Each plant has a growing needs file at `data/portable_pocket_cabin/cooking_plant/<plant>.json`, loaded with datapacks and reloaded with `/reload`:

```json
{
  "soil": ["farmland"],
  "light": 9,
  "water": "nearby",
  "support": "trellis",
  "growth_chance": 0.25,
  "shade": false
}
```

- **Soil:** the blocks the plant can stand on, as block ids or tags.
- **Light:** the minimum light level to grow.
- **Water:**
  - `none`.
  - `nearby`: water within four blocks, as for farmland.
  - `standing`: the plant stands in a water source.
  - `running`: flowing water beside it, as for wasabi.
- **Support:** `none`, `trellis`, or `log`.
- **Growth chance:** the chance per random tick to grow a stage.
- **Shade:** the plant needs a block above it, as for shaded tea's last stage.

A plant whose needs aren't met doesn't grow but survives. That matches vanilla crops in the dark. The blocks are registered from a fixed plant list in code, because Minecraft registers blocks at startup. Their needs come from data.

### Wild placement as data

- **Biomes:** each plant has a biome tag, `portable_pocket_cabin:grows_wild/<plant>`, listing the biomes from the [sources](../../cooking/sources.md).
- **Generation:** a placed feature per plant generates it as a small patch. Fabric biome modifications add the feature to the biomes in its tag, so a datapack changes where it grows by editing the tag.
- **Rarity:** patches are rare enough that biomes with many plants still look like themselves.
- **Windswept savannas** stand in for the Mediterranean, with olives, rosemary, oregano, and savory.

### Trades

- **Wandering traders** sell seeds, cuttings, or saplings of two random plants from other regions.
- **Farmers at journeyman level** sell seeds of one far-off field crop.

Trades come from Fabric's trade offer helper and use emeralds.

### Greenhouse

`CabinGreenhouseGrowth.canProgress` accepts `CookingPlant` blocks whose `age` is below their maximum. Catch-up then grows them like vanilla crops: it never harvests or replants them. Fruiting leaves only flower and fruit; they never grow into new trees.

### Cookbook

A **Garden** section lists every plant. Its page shows:

- Where the plant grows wild, from its biome tag.
- How to grow it, from its growing needs: soil, light, water, and support.
- What it gives.

### Art

- **Field crops:** stage textures from vanilla wheat, carrots, potatoes, and beetroot, tinted per plant through a block color provider.
- **Bushes:** the sweet berry bush.
- **Fruiting leaves:** oak or jungle leaves with tinted fruit overlays.
- **Items:** tinted vanilla item textures.

The [texture audit](../../cooking/texture-audit.md) stays the list for B-0034's real art.

## Deliveries

| Delivery | Region | Plants |
|---|---|---|
| 1 | **Engine, plains, meadows, and forests** | Barley, rye, corn, rapeseed, mustard seed, white bean, asparagus, cabbage, courgette, cucumber, garlic, lettuce, onion, red onion, shallot, spring onion, radish, cilantro and coriander, dill, marjoram, parsley, thyme, caraway, strawberry, white grape, hops, plum, elder, raspberry, oyster mushroom, rosehip |
| 2 | **Taigas, cold waters, and coasts** | Red cabbage, wasabi, blackcurrant, redcurrant, rhubarb, wakame, laver, sea lettuce |
| 3 | **Savannas, deserts, and badlands** | Blue corn, sesame, peanut, chickpea, aubergine, bell pepper, paprika pepper, tomato, tomatillo, green chili, poblano chili, red chili, basil, bay laurel, anise, cumin, apricot, grape, red grape, lemon, orange, roselle, tamarind, almond |
| 4 | **Windswept savannas and hills** | Olive with green and black olives, rosemary, oregano, savory, purple corn, ají amarillo |
| 5 | **Rivers, swamps, cherry groves, and bamboo jungles** | Rice, glutinous rice, soybean and bean sprouts, azuki bean, celery, water chestnut, mint, cherry, ume plum, star anise, tea and shaded tea |
| 6 | **Jungles** | Black bean, long bean, Thai aubergine, cassava, sweet potato, green papaya, bird's eye chili, holy basil, Thai basil, lemongrass, pandan, betel, makrut lime, allspice, annatto, cardamom, cinnamon, clove, galangal, ginger, nutmeg, peppercorn, turmeric, vanilla, coconut, lime, mango, pineapple, oil palm and palm sap, coffee, yerba mate |

- **Delivery 1** builds the whole engine:
  - Every plant kind, growing needs, wild placement, trellis, and greenhouse support.
  - Trades.
  - The Garden section.
- **Later deliveries** reuse it, adding only special harvests, such as cinnamon bark stripped with an axe and palm sap tapped from the trunk in delivery 6.

### Recipes per delivery

Each delivery runs a completion check over the [recipe catalogue](../../cooking/recipe-catalogue.md) and [intermediate products](../../cooking/intermediates.md). The check lists every product whose ingredients all exist once the delivery's plants do, apart from B-0049 meat and seafood. The delivery then ships all of them:

- Recipe data.
- Intermediate items.
- Technique entries.
- Tastes for seasoning.

Each delivery's pull request records its list. Products for stations that don't exist yet are noted and wait for them.

## Verification

GameTests for the engine:

- **Field crops:** they grow only on their soil, in enough light, and with their water. They drop the ingredient and seeds when ripe.
- **Trellis crops:** they need a trellis. Picking them drops them back two stages.
- **Paddy crops and wasabi:** they need standing or running water.
- **Bushes:** they regrow after picking. The elder bush gives elderflower and elderberry at its two stages.
- **Fruit trees:**
  - A sapling grows into a tree with fruiting leaves.
  - Leaves flower and fruit, and picking leaves the tree whole.
  - Olives give green or black olives by stage.
- **Growing needs:** a datapack change to a plant's needs takes effect after reload.
- **Greenhouse:** catch-up grows the new crops, bushes, and leaves, and never harvests.
- **Wild placement:** every plant's placed feature and biome tag load. A test region of each tagged biome can host a patch.
- **Trades:** traders offer seeds of the new plants.

GameTests for content: for each delivery, every completed chain runs from plant to product. The recipe count test tracks the total.

Client GameTests capture a field of the delivery's crops, a fruiting tree, a trellis, and a Garden page for `screenshots/cooking/`.

## Assumptions

- **Tuning:** growth chances, yields, and patch rarity are first guesses. B-0033 tunes them.
- **Tree trunks:** fruit trees reuse vanilla logs: oak, birch, jungle, acacia, or cherry, as the species suits. A new wood type per species waits for real art.
- **Seed items:** seeds are their own items, except where the ingredient replants itself, as garlic, onion, and potato-like roots do.

## Delivery 1 as built

The [completion check](../../cooking/tools/completion_check.py) found 23 intermediate products and 6 dishes that delivery 1 completes. All of them shipped, adapted to the existing stations:

- **Grain:**
  - Rye flour and cracked rye. Cracked rye uses the mortar, so the mill keeps one rye recipe.
  - Roasted barley, malted barley, and roasted malt.
  - Rugbrød.
- **Corn:**
  - Husking corn on the cutting board gives a husk and keeps the kernels.
  - Dried corn and dried corn husks.
  - Cornstarch.
  - Wood ash and lye, hominy, masa, and corn tortillas.
  - Masarepa and toasted corn.
- **Ferments:**
  - Sauerkraut and gherkins.
  - White wine and wine vinegar.
  - Wort and beer, which are cooking ingredients only.
  - Dried rosehip.
- **Condiments:**
  - Mustard and mayonnaise.
  - Strawberry and raspberry jam. These join a `jams` tag with sweet berry jam, and the Berliner takes any of them.
- **Dishes:** spätzle and Käsespätzle, Kartoffelpuffer, Malzkaffee, Mugicha, and Rugbrød.

The check also lists 36 intermediates that phase 3's ingredients already allowed but phase 3 did not ship. Among them are chocolate, puff pastry, and breadcrumbs. They are a follow-up outside this delivery.
