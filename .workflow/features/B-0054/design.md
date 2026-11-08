# B-0054 design: grow every food

Status: Ready
Feature ID: B-0054

This design implements the [B-0054 brief](brief.md). It builds on the plant engine in the [B-0050 design](../B-0050/design.md), the shellfish and prawn trap in the [B-0049 design](../B-0049/design.md), and greenhouse catch-up from [B-0019](../B-0019/brief.md).

## Decisions

- **Delivery:** one pull request, in steps. Each step is a commit with its tests green.
- **Sea plants and beds:** they grow in stages, like sweet berries, rather than growing tall like kelp. A grown plant gives more when harvested, and spreads slowly to suitable water beside it.
- **Spread rate:** slow. A full patch takes several in-game days to double.
- **Prawn traps:** away from prawn waters, a trap catches at half the rate.
- **Apples:** a fruit tree species, like the plum, that bears vanilla apples.

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

A grown cardoon (age 3) drops 2 cardoons instead of 1, so a patch grows. Cardoon flowers are unchanged.

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

## Delivery

1. Shade through glass, prawn traps in any water, and the cardoon's double drop.
2. The apple tree, oak-leaf sapling drops, and trades.
3. Sea plant and shellfish bed growth, harvest yields, and spreading.
4. Greenhouse catch-up for saplings, trunks, sea plants, and beds.
5. Garden pages, the growability GameTest, and the completion check's `--growable` mode.
6. README, screenshots, and backlog.

## Assumptions

- **Tuning:** growth chances, the 1 in 200 sapling chance, spread chances, and the half rate for traps are first guesses for B-0033.
- **Art:** sea plant and bed stages reuse the single texture they have now until B-0034. Apple leaves reuse the fruit-tree leaf stages with a red fruit tint.
