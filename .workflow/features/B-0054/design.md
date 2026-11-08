# B-0054 design: grow every food

Status: Ready
Feature ID: B-0054

This design implements the [B-0054 brief](brief.md). It builds on the plant engine in the [B-0050 design](../B-0050/design.md), the shellfish and prawn trap in the [B-0049 design](../B-0049/design.md), and greenhouse catch-up from [B-0019](../B-0019/brief.md).

## Decisions

- **Delivery:** three pull requests, each in steps with its tests green. See [Delivery](#delivery).
- **Sea plants and beds:** they grow in stages, like sweet berries, rather than growing tall like kelp. A grown plant gives more when harvested, and spreads slowly to suitable water beside it.
- **Spread rate:** slow. A full patch takes several in-game days to double.
- **Prawn traps:** away from prawn waters, a trap catches at half the rate.
- **Apples:** a fruit tree species, like the plum, that bears vanilla apples.
- **Rich soil:** it is ready at once, and lasts about four harvests. Plants on it grow about twice as fast, through an extra random tick from the soil.
- **Bone meal:** it stays vanilla. The composter makes compost instead.
- **Wheat:** vanilla wheat replants from wheat, and its crop drops wheat only.
- **Climate:** outside its climate, a plant grows at a quarter of its speed. Inside the greenhouse, every climate is met.

## Apple tree

- **Species:** `fruitTree("apple", () -> Items.APPLE)`.
  - Apple leaves flower, then fruit, and picking them gives vanilla apples.
  - The tree has an oak trunk, an apple sapling, and the same growing needs and Garden page as the other fruit trees.
- **Saplings from oak:** the oak and dark oak leaf loot tables are overridden as data, as the entity tables were in B-0049. Each is vanilla's table with one extra pool: without shears or silk touch, a 1 in 200 chance of an apple sapling, raised by fortune like vanilla's apple chance. Vanilla apples and saplings still drop as before.
- **Wild:** it grows wild in forests, flower forests, and plains, through `grows_wild/apple`, like the plum tree.
- **Trades:** wandering traders sometimes sell apple saplings.

## Sea plants and shellfish beds

`SeaPlantBlock` gains an `age` (0 to 3) and implements `CookingPlant`, so growing needs come from data and greenhouse catch-up treats it like any cooking plant.

- **Growth:** a random tick ages the plant by one stage at its growth chance, while it stands in water with enough light.
- **Sea plants:** wakame, laver, and sea lettuce.
  - Breaking a grown plant (age 3) drops 2 to 3 of its item. Breaking a younger one drops 1.
  - Bone meal ages it by one stage.
- **Shellfish beds:**
  - Using a grown bed with an empty hand gives 2 to 3 mussels or oysters and sets it back to age 1, as picking a bush does.
  - Using a younger bed gives nothing.
  - Breaking any bed drops 1, so a bed can be moved.
- **Spreading:** a grown plant or bed may spread on a random tick, at its `spread_chance`.
  - It picks one of the 8 water blocks around it at its own height or one below.
  - It places a new age-0 plant there if that spot is a water source on a floor where the plant can survive.
  - A plant surrounded on all sides stops spreading.
- **Anywhere:** planted sea plants and beds grow in any water. Their biome tags only decide where they generate wild.
- **Growing needs data:** each sea plant and bed gets a needs file with `water: standing`, `light: 0` for beds and 6 for sea plants, a growth chance, and the new optional `spread_chance`. Soil is left out, because they survive on any sturdy floor, as seagrass does.

## Cardoon

The cardoon becomes a perennial crown (see [Harvest models](#harvest-models)). A grown one is harvested again and again, and breaking it drops two cardoons, so a patch grows. Cardoon flowers are unchanged.

## Prawn trap

- **Any water:** a trap catches in any water.
- **Rate:** it takes 1000 ticks per prawn in `prawn_waters`, and 2000 ticks elsewhere.
- **Unchanged:** it still holds up to eight and still catches up from saved game time.

## Greenhouse

`CabinGreenhouseGrowth.canProgress` gains three kinds:

- **Fruit-tree saplings:** any `FruitSaplingBlock`. Catch-up random ticks advance its stage and grow the tree when the feature fits. A sapling with too little room stays a sapling.
- **Harvest trunks:** cinnamon and palm trunks progress while not `ready`. Catch-up grows bark back and fills palm sap.
- **Sea plants and beds:** these are covered by their new `age` and `CookingPlant`.

Spreading happens on live random ticks only. Catch-up grows existing plants but doesn't place new ones, so a greenhouse never fills beyond what its owner planted while they're away.

## Shade

`PlantNeeds.roofed` changes. Before, it counted any motion-blocking block overhead. Now it counts only blocks that don't let skylight through: it walks up from above the plant to the motion-blocking height and looks for a block whose state doesn't `propagatesSkylightDown`.

- **Glass and air don't shade.** Tea under greenhouse glass gives tea leaves.
- **Opaque roofs and leaves shade.** Tea under them gives shaded tea leaves.
- **Shade-needing plants:** the same rule applies to plants whose needs ask for shade.

## Garden pages

- **Coverage:** sea plants and shellfish beds already have pages, because they are in `CookingPlants.PLANTS`. The cardoon gets one through a new garden-only list, so it isn't added to B-0050's wild placement.
- **Spreading:** a page whose needs have a `spread_chance` adds "It spreads to suitable water beside it."
- **Where to start:**
  - The server adds to the plant needs sync the items wandering traders sell, read from the villager trade registry.
  - A page whose planting item is among them adds "Wandering traders sometimes sell it."
  - The existing "Grows wild in ..." line covers wild plants.
  - Apple trees also say their saplings drop from oak and dark oak leaves, through a lang note.

## The growability check

- **In the game:** a GameTest walks every plant in `CookingPlants.PLANTS` and the cardoon, and checks:
  - **A planting item:** the plant has a planting item, and that item places the plant.
  - **It multiplies:**
    - **Perennials:** bushes, trellis vines, fruiting leaves, harvest trunks, and shellfish beds keep growing after a harvest. The test harvests a grown plant and checks the block is still there.
    - **Annuals:** field and paddy crops, sea plants, and the cardoon are broken to harvest. Over 50 seeded harvests of a grown plant, the test averages what comes back and checks it gives more than one planting item per harvest.
- **Against the sources:** the completion check tool gains a `--growable` mode. It lists every raw ingredient in the sources marked Farm, Tree, Forage, Fish, or Trap that no plant, shellfish bed, trap, or fish in the mod gives. That keeps a future ingredient from arriving forage-only unnoticed.

## Harvest models

Every plant follows one model. Each model is one way of giving the harvest and the next planting, built from the existing B-0050 block kinds. Most changes are loot tables and data. The planting item a model uses is the plant's `PLANTED_FROM`.

| Model | How it gives food | How it is planted | Plants |
|---|---|---|---|
| **Grain and tuber** | Breaking a ripe plant drops 2 to 4 of its harvest | From the harvest itself | Barley, rye, corn, blue corn, purple corn, rice, glutinous rice, white bean, black bean, chickpea, soybean, azuki bean, peanut, sesame, cumin, anise, caraway, mustard, rapeseed, garlic, onion, red onion, shallot, sweet potato, cassava, ginger, galangal, turmeric, lemongrass, water chestnut, wasabi, and vanilla wheat, carrot, and potato |
| **Fruit with seeds** | Breaking a ripe plant drops its produce and 1 to 3 seeds | From seeds | Cabbage, red cabbage, bell pepper, paprika pepper, green chili, poblano, red chili, bird's eye chili, ají amarillo, aubergine, Thai aubergine, courgette, tomatillo, roselle, cardamom, pineapple (its crown), vanilla beetroot, and the tomato, cucumber, and long bean vines |
| **Leaf, then seed** | At its leaf stage it drops leaves or a root. One stage later it flowers and drops 2 to 3 seeds instead | From seeds | Lettuce, radish, basil, holy basil, Thai basil, dill, parsley, celery, spring onion, and cilantro, which keeps coriander seed as its seed |
| **Cuttings** | Picking a grown plant gives its fruit or leaves and sets it back, as now. Shears on a grown plant give a cutting and set it back too | From a cutting. The fruit no longer plants it | Blackcurrant, redcurrant, raspberry, strawberry (a runner), thyme, marjoram, rosemary, oregano, savory, mint, pandan, tea, elder, and the grape, red grape, white grape, hop, peppercorn, vanilla, and betel vines |
| **Perennial crown** | Using a grown plant cuts its harvest and sets it back to its first grown stage. Breaking a grown plant drops 2 crowns | From a crown | Asparagus, rhubarb, cardoon |
| **Fruit tree** | Picking the leaves, as now | From a sapling, from breaking leaves | Every fruit tree, and the apple |
| **Trunk** | An axe or a glass bottle, as now | From a sapling | Cinnamon, oil palm, coconut palm |
| **Water forage** | See [Sea plants and shellfish beds](#sea-plants-and-shellfish-beds) | From what it gives | Wakame, laver, sea lettuce, mussels, oysters |
| **Spreading mushroom** | Breaking gives mushrooms. A grown patch spreads along its log | From what it gives | Oyster mushrooms |

How the models change today's plants:

- **New planting items:** each cutting plant that today plants from its fruit gets a cutting item:
  - blackcurrant, redcurrant, raspberry, thyme, marjoram, rosemary, oregano, savory, mint, and pandan cuttings
  - strawberry runner
- **Removed seed items:** barley, rye, corn, blue corn, and purple corn seeds go, and so do rice and glutinous rice seedlings. Their grain plants them. Recipes, trades, and loot that name them change to the grain.
- **Wheat:**
  - A use event plants vanilla wheat from wheat on farmland, as from seeds.
  - The vanilla wheat crop's loot table is overridden to drop 2 to 4 wheat when ripe and 1 wheat when not, with no seeds.
  - Wheat seeds still plant wheat and still come from grass.
- **Leaf, then seed:** these crops gain the cilantro behaviour. `FieldCropBlock` already lets the loot table choose by age: age 6 gives the leaf or root, and age 7 the seeds.
- **Shears:**
  - Using shears on a grown `HerbBushBlock` or `TrellisCropBlock` gives its cutting, sets it back two stages, and wears the shears.
  - Breaking a trellis vine gives only the trellis.
- **Perennial crowns:**
  - `FieldCropBlock` gains a `perennial` flag. A perennial picked with an empty hand at full age gives its harvest and drops to age 4.
  - The cardoon gains the same behaviour.

## Growing difficulty

Growing needs gain two fields, and growth chances follow tiers.

- **`climate`:** one of `cold`, `temperate`, `warm`, `hot`, or `any`. It is checked against the biome's base temperature: below 0.3 is cold, below 0.9 temperate, below 1.5 warm, and the rest hot.
  - Outside its climate, a plant's growth chance is quartered.
  - Inside the greenhouse, every climate is met.
- **`shade`:** it stays as it is. It now also applies to vanilla, coffee, and cardamom.
- **Speed tiers:** `growth_chance` follows a tier.

  | Tier | Growth chance | Plants |
  |---|---|---|
  | Fast | 0.35 | Leafy greens and radish |
  | Average | 0.25 | Most crops |
  | Slow | 0.12 | Roots and rhizomes |
  | Very slow | 0.06 | Asparagus, rhubarb, pineapple, vanilla, wasabi, and fruit leaves |
  | Trunks | 0.05 | Cinnamon and palm trunks, unchanged |

- **Wasabi:** cold climate, running water, gravel, shade, and the very slow tier.
- **The Garden page:** it shows the tier ("grows quickly", "slowly", ...) and the climate.

## Compost and rich soil

- **Compost:**
  - A mixin on `ComposterBlock` makes a full composter give `compost` where it gave bone meal. This covers both its hand output and its hopper output.
  - Every compostable item still fills the composter.
  - Bone meal still comes from bones and the mill.
- **Blocks:**
  - `rich_soil` is a dirt-like block in `minecraft:dirt` and `supports_vegetation`.
  - `rich_farmland` extends `FarmBlock`. It is in `supports_crops`, `supports_stem_crops`, and `maintains_farmland`, so vanilla and cooking crops grow on it.
  - Both have a `charge` from 1 to 4.
- **Making it:**
  - Using compost on farmland gives rich farmland at charge 4, and on dirt, grass, or coarse dirt gives rich soil at charge 4.
  - Using compost on rich ground tops it up to 4.
  - A hoe tills rich soil into rich farmland with the same charge.
  - Rich farmland that dries out or is trampled becomes rich soil.
- **Faster:** both blocks tick randomly. Each random tick gives the plant above one extra random tick, so it grows about twice as fast. This covers vanilla crops, stems, berries, saplings, and every cooking plant. The plant's own growing needs still apply.
- **Wearing out:** a charge is spent when a ripe plant on rich ground is harvested:
  - a player breaks a crop at full age, through Fabric's block break event
  - a cooking plant is picked, which its harvest code reports to `RichSoil.spend`

  At charge 0, rich farmland becomes farmland and rich soil becomes dirt.
- **Greenhouse catch-up:** it runs plant ticks directly, so rich soil's extra tick doesn't apply there. Catch-up stays at the plant's own speed.

## Verification

GameTests:

- **Apples:**
  - An apple sapling grows a tree whose leaves give vanilla apples and fruit again.
  - Oak leaves can drop an apple sapling, using a seeded loot roll with fortune.
- **Sea plants:**
  - They age in water and drop 2 to 3 when grown.
  - Bone meal ages them.
  - A grown plant spreads into neighbouring water and not onto land.
- **Shellfish beds:**
  - Picking a grown bed gives 2 to 3 and leaves a young bed.
  - A young bed gives nothing.
  - Beds grow outside their wild biomes.
- **Cardoon:** a grown cardoon drops two.
- **Prawn trap:** it catches outside prawn waters at half the rate.
- **Greenhouse:**
  - `canProgress` accepts fruit saplings, unready trunks, and young sea plants, and rejects ripe ones.
  - Tea under glass isn't roofed, and under stone it is.
- **Growability check:** it passes for every plant.
- **Harvest models:**
  - Each model drops what its table says.
  - Barley plants from barley, and wheat from wheat.
  - A ripe wheat crop drops no seeds.
  - Lettuce gives leaves at age 6 and seeds at age 7.
  - Shears on a grown blackcurrant bush give a cutting and set it back, and a blackcurrant no longer plants a bush.
  - Picking asparagus leaves the plant.
- **Difficulty:**
  - Out of climate, a plant's growth chance is quartered, and in the greenhouse it isn't.
  - Wasabi refuses to grow without shade.
- **Compost:**
  - A full composter gives compost.
  - Compost makes farmland rich, and a hoe tills rich soil.
  - A crop on rich farmland ripens in about half the ticks.
  - Four harvests use up the charge and return plain farmland.

## Delivery

**Pull request 1: grow everything.**
1. Shade through glass, prawn traps in any water, and the cardoon's harvest.
2. The apple tree, oak-leaf sapling drops, and trades.
3. Sea plant and shellfish bed growth, yields, and spreading.
4. Greenhouse catch-up for saplings, trunks, sea plants, and beds.

**Pull request 2: harvest models and difficulty.**
1. Grain and tuber crops plant from their harvest, including vanilla wheat.
2. Leaf-then-seed crops, cutting items, and shears on bushes and vines.
3. Perennial crowns.
4. Climate, speed tiers, and the demanding plants' needs.

**Pull request 3: compost, rich soil, and the cookbook.**
1. The composter makes compost, and rich soil and rich farmland.
2. Faster growth and wearing out.
3. Garden pages show model, speed, climate, spreading, and trades.
4. The growability GameTest, the completion check's `--growable` mode, README, screenshots, and backlog.

## Assumptions

- **Tuning:** these are first guesses for B-0033:
  - growth tiers and climate bands
  - the 1 in 200 sapling chance
  - spread chances and the half rate for traps
  - compost charges and the rich-soil speed
- **Art:** sea plant and bed stages reuse the single texture they have now until B-0034. Apple leaves reuse the fruit-tree leaf stages with a red fruit tint.
