# Phase 3 design: milling, baking, dairy, salt, sauces, and preserving

Status: Ready
Feature IDs: B-0046, B-0047, B-0048

This is the joint design for phase 3 of the [cooking program](../../cooking/program.md). It implements three briefs, which ship together:

- [B-0046 milling, baking, and dairy](brief.md).
- [B-0047 stocks, sauces, and flavour](../B-0047/brief.md).
- [B-0048 fermentation, drying, and preservation](../B-0048/brief.md).

It builds on the [B-0053](../B-0053/design.md) and [B-0052](../B-0052/design.md) designs. It follows [ADR-0010](../../decisions/adr/0010-define-cooking-as-station-recipe-types.md) for recipe types, [ADR-0013](../../decisions/adr/0013-catch-up-time-stations-from-saved-game-time.md) for time stations, and [ADR-0014](../../decisions/adr/0014-store-dish-quality-on-the-item-stack.md) for dish quality.

## Shared engine

### Station kinds

Phase 3 adds these station kinds, each with its own recipe type:

| Station | Path | Kind | Ingredient slots | Container | Byproduct slots | Progress |
|---|---|---|---|---|---|---|
| Mill | `mill` | Container | 1 | Yes | 1 | Ticks, no heat |
| Mixing bowl | `mixing_bowl` | Counter-top, worked with the spoon | 6 | Yes | 1 | Spoon uses |
| Mortar and pestle | `mortar` | Counter-top, worked with the pestle | 2 | No | 0 | Pestle uses |
| Butter churn | `churn` | Container, worked by hand | 2 | No | 2 | Uses |
| Cheese press | `cheese_press` | Container | 2 | No | 1 | Ticks, no heat |
| Proofing basket | `proofing` | Time, one batch | 1 | No | 0 | Game time |
| Dairy pan | `dairy_pan` | Time, one batch | 1 | Yes | 1 | Game time |
| Fermentation crock | `crock` | Time, one batch | 4 | Yes | 2 | Game time |
| Brine barrel | `brine` | Time, one batch | 4 | No | 1 | Game time |
| Ageing shelf | `ageing` | Time, four spots | 1 per spot | No | 0 | Game time |
| Drying rack | `drying` | Time, four spots | 1 per spot | No | 0 | Game time |

### Recipe format

- **Worked stations:** the mixing bowl, mortar and pestle, and churn read a recipe's `time` as the number of uses. A quick mix takes a few spoon uses, and kneading takes more.
- **Two byproduct slots:** a station kind can have a second byproduct slot. Remainders fill the byproduct slots in order, one kind of item per slot, and a batch waits while any remainder doesn't fit.
  - **Who uses it:** the cooking pot gains one, which places it next to the first. The crock and churn have two.
  - **Why:** seawater leaves nigari and bottles, and churning leaves buttermilk and a bottle.
  - **Saving:** existing pots load with the new slot empty.
- **Sun:** drying recipes may set `sun: true`. A sun recipe matches only while the rack has open sky above it. A recipe with `sun: false` matches only in shade, and a recipe without the field matches anywhere.
- **Tastes:** a dish recipe may list `tastes`, the tastes that balance it. See [seasoning](#tastes-and-seasoning).
- **Water bottles:** recipes take water as water bottles, through Fabric's `fabric:components` ingredient on `minecraft:potion`. They return glass bottles as remainders.

### Time stations

- **Single-batch stations:** the proofing basket, dairy pan, crock, and brine barrel have screens with a recipe panel and a progress arrow, like the press. They reuse `ProcessStationBlockEntity`, with game-time progress.
- **Spot stations:** the ageing shelf and drying rack hold four items each. Each item ages or dries on its own and shows on the block, like the skillet. They reuse `SurfaceCooking`, with game-time progress.
  - **Using them:** using one with an item places it if a recipe takes it, and an empty hand takes a finished item first.
  - **Hoppers:** hoppers from above add items, and hoppers below take finished ones.
- **Catch-up, per ADR-0013:**
  - Each station saves the game time of its last tick.
  - A tick advances the current batch by the game time that passed, so a station whose chunk was unloaded, or whose cabin was packed, finishes its batch when it next ticks.
  - Game time doesn't pass while the server is stopped, so only time while the server operates counts.
  - A catch-up tick finishes at most the current batch, or each spot's current item. It drops the rest of the elapsed time, so it never starts the next batch, collects, or refills.
- **Screen time:** single-batch screens show the time left in Minecraft hours, such as "6 h left".

### Stations

- **Mill:** a full block. Wheat, rock salt, and sugar cane go in, with bottles for cane juice.
- **Mixing bowl:** a counter-top block on the prep counter.
  - **Spoon:** a new tool. Using it on the bowl adds one use to the current recipe.
  - **Screen:** opening the bowl shows six ingredient slots, a container slot, an output, a byproduct slot, and two seasoning slots.
- **Mortar and pestle:** a counter-top block. Using the pestle on it adds one use.
- **Butter churn:** a full block. Using it with an empty hand churns once, and sneak-use opens its screen.
- **Cheese press:** a full block that presses for a set time.
- **Proofing basket:** a small block, usable on a counter or anywhere.
- **Dairy pan:** a small block.
- **Fermentation crock:** a full-height pot block.
- **Brine barrel:** a barrel-shaped block.
- **Ageing shelf:** a wall-standing shelf block with four spots.
- **Drying rack:** a frame block with four spots. It checks the sky above it for sun recipes.
- **Rules for all of them:**
  - They follow the B-0045 rules for saving, breaking, hoppers, and household permissions.
  - Inside the B-0041 kitchen they fill from central storage.
  - Their crafting recipes don't collide with vanilla ones.

### Smoking

The vanilla smoker smokes cured food with new `minecraft:smoking` recipes in the mod's namespace. B-0045 keeps removing vanilla food smoking recipes, but these new ones load. The smoker takes any fuel, as in vanilla.

## B-0046: milling, baking, and dairy

### Grain and leavening

| Product | Station | Inputs | Time |
|---|---|---|---|
| Wheat flour | Mill | Wheat, gives 1 flour | 100 ticks |
| Sourdough starter | Crock | 2 flour, 1 water bottle, in a bowl | 1 day |
| Fed starter | Crock | 1 starter, 1 flour, 1 water bottle, a bowl, gives 2 starters | Half a day |
| Yeast culture | Crock | 1 starter, 1 sugar, 1 water bottle, in a bottle | 1 day |
| Yeast | Drying rack | Yeast culture | Half a day |

Bowls and bottles come back as remainders where a recipe uses water bottles or a starter.

### Doughs and bakes

| Product | Station | Inputs | Work or time |
|---|---|---|---|
| Bread dough | Mixing bowl, knead | 3 flour, 1 leaven, 1 water bottle, 1 salt | 8 uses |
| Proofed bread dough | Proofing basket | Bread dough | 2 hours |
| Bread | Oven, medium heat | Proofed bread dough | 200 ticks |
| Shaped rolls | Prep counter | Proofed bread dough, gives 2 | 40 ticks |
| Bread roll | Oven, medium heat | Shaped rolls, gives 2 | 160 ticks |
| Shortcrust dough | Mixing bowl, rub in | 2 flour, 1 salted butter, 1 water bottle | 6 uses |
| Shortcrust pastry | Ageing shelf, chill | Shortcrust dough | 1 hour |
| Unbaked pumpkin pie | Prep counter, fill | Shortcrust pastry, pumpkin, egg, sugar | 40 ticks |
| Pumpkin pie | Oven, medium heat | Unbaked pumpkin pie | 200 ticks |
| Cookie dough | Mixing bowl, mix | 2 flour, 1 salted butter, 1 sugar, 1 egg, 1 cocoa beans | 4 uses |
| Cookies | Oven, medium heat | Cookie dough, gives 8 | 160 ticks |
| Cake batter | Mixing bowl, whisk | 3 flour, 2 sugar, 2 eggs, 1 salted butter, 1 milk bucket | 6 uses |
| Cake | Oven, low heat | Cake batter | 300 ticks |
| Sweet yeast dough | Mixing bowl, knead | 2 flour, 1 yeast, 1 milk bucket, 1 sugar, 1 egg, 1 salted butter | 8 uses |
| Proofed sweet dough | Proofing basket | Sweet yeast dough | 2 hours |
| Berliner | Cooking pot, high heat, 1 oil, then prep counter | Proofed sweet dough fries into 4 fried doughnuts. The counter fills them with sweet berry jam and dusts them with sugar. | 100 ticks, then 40 |

- **The leaven:** an item tag, `portable_pocket_cabin:leavens`, holds sourdough starter and yeast. The starter is used up, and a household keeps one by feeding it.
- **Replaced recipes:** these replace the B-0045 oven recipes for bread, cookies, cake, and pumpkin pie, which bake straight from wheat.
- **Food values:**
  - Vanilla bakes keep vanilla food values.
  - A bread roll restores 3 hunger and 3.6 saturation.
  - A Berliner restores 6 hunger and 7.2 saturation.
- **Berliner:** its inputs all exist in phase 3, so it ships here as the first use of sweet yeast dough. Sweet berry jam comes from B-0047.

### Dairy

| Product | Station | Inputs | Work or time |
|---|---|---|---|
| Cream and skimmed milk | Dairy pan | 1 milk bucket and 2 glass bottles, gives 2 cream and a skimmed milk bucket | 1 day |
| Raw butter and buttermilk | Butter churn | 2 cream, gives raw butter, a buttermilk bottle, and a glass bottle | 12 uses |
| Salted butter | Mixing bowl, work | Raw butter, salt | 3 uses |
| Whipped cream | Mixing bowl, whisk | 2 cream, 1 sugar, in a bowl, giving 2 glass bottles | 8 uses |
| Warm milk | Cooking pot, low heat | Milk bucket, gives a warm milk bucket | 100 ticks |
| Yogurt | Crock | Warm milk bucket with 1 yogurt and 3 bowls, or with 1 buttermilk and 4 bowls, gives 4 yogurt | 1 day |
| Crème fraîche | Crock | 2 cream, 1 buttermilk, in a bowl, giving 3 glass bottles | 1 day |

- **Containers:** milk stays in buckets. Cream and buttermilk come in bottles, skimmed and warm milk in buckets, and yogurt, whipped cream, and crème fraîche in bowls.
- **The yogurt culture:** an item tag, `portable_pocket_cabin:yogurt_cultures`, holds yogurt and buttermilk. Buttermilk starts the first batch. Every batch gives back the milk bucket, and a batch started with buttermilk also gives back its bottle.
- **Foods:**
  - Yogurt restores 4 hunger and 4.8 saturation, and returns its bowl.
  - Buttermilk is a drink that restores 2 hunger.

### Cheese

| Product | Station | Inputs | Work or time |
|---|---|---|---|
| Cardoon flowers | Cardoon plant | A mature plant drops 1 to 2 flowers and a cardoon | - |
| Vegetable rennet | Crock, steep | 3 cardoon flowers, 1 salt, 1 water bottle, in its bottle | 1 day |
| Curds | Cooking pot, low heat | 2 milk buckets, 1 rennet, gives 4 curds, 2 buckets, and a whey bottle | 200 ticks |
| Fresh cheese | Cheese press, light | 2 curds, 1 salt, gives 2 | 100 ticks |
| Pressed cheese | Cheese press, hard | 4 curds | 400 ticks |
| Soured milk | Crock | Skimmed milk bucket, 1 buttermilk, giving a glass bottle | 1 day |
| Quark | Strainer | Soured milk bucket, in a bowl, giving a bucket | 100 ticks |
| Cultured cream | Crock | 2 cream, 1 buttermilk, in a bowl, giving 3 glass bottles | 1 day |
| Drained cream | Strainer | Cultured cream | 100 ticks |
| Cream cheese | Mixing bowl, work | Drained cream, salt | 3 uses |

B-0048 brines and ages the [hard and soft cheeses](#cheese-ageing).

- **Cardoon:**
  - **The block:** a new plant with four growth stages. It generates in patches in meadows and savannas through a placed feature, and grows on grass and dirt.
  - **Harvesting:** breaking a mature plant drops flowers and a cardoon item, which replants it. Bone meal grows it.
- **Whey:** a drink that restores 1 hunger. It feeds pigs, through the vanilla food tag.
- **Foods:**
  - Fresh cheese and quark restore 3 hunger and 3.6 saturation. Quark returns its bowl.
  - Cream cheese is an ingredient.

## B-0047: salt, sugars, sauces, and seasoning

### Salt

| Product | Station | Inputs | Time |
|---|---|---|---|
| Rock salt | Rock salt ore | Mining drops 1 to 2, more with Fortune. Silk Touch drops the ore. | - |
| Salt | Mill | Rock salt, gives 2 | 100 ticks |
| Seawater | Glass bottle | Filled from water in an ocean biome | - |
| Wet salt and nigari | Cooking pot, high heat | 3 seawater, gives wet salt, a nigari bottle, and 2 glass bottles | 300 ticks |
| Salt | Drying rack, in sun | Wet salt | Half a day |
| Coarse salt | Drying rack, in shade | Wet salt | 2 days |
| Salt | Mortar and pestle, grind | Coarse salt | 4 uses |

- **Rock salt ore:**
  - **The block:** a stone-like ore, mined with a stone pickaxe or better.
  - **Where it generates:** ore veins in dripstone caves and every badlands biome, through placed features and Fabric biome modifications.
- **Seawater:** using a glass bottle on water in a biome tagged `minecraft:is_ocean` gives seawater instead of a water bottle.
- **Slow drying:** wet salt dried in the shade grows large crystals, so coarse salt comes from the same wet salt by drying it slowly.
- **Nigari:** an ingredient for later tofu. It is also a bitter seasoning.

### Sugars and syrups

| Product | Station | Inputs | Time |
|---|---|---|---|
| Cane juice | Mill | 4 sugar cane, in a glass bottle | 100 ticks |
| Cane sugar paste | Cooking pot, high heat | 3 cane juice, giving 3 glass bottles | 300 ticks |
| Brown sugar | Drying rack | Cane sugar paste, gives 2 | Half a day |
| Panela | Ageing shelf, set | Cane sugar paste | 1 day |
| Simple syrup | Cooking pot, low heat | 2 sugar, 1 water, in a glass bottle | 100 ticks |
| Caramel | Skillet, medium heat | Sugar | 100 ticks |

Brown sugar works wherever a recipe takes sugar, through the `portable_pocket_cabin:sugars` item tag. Phase 3 recipes use that tag for sugar.

### Sauces

| Product | Station | Inputs | Time |
|---|---|---|---|
| Roux | Cooking pot, low heat | 1 salted butter, 1 flour, in a bowl | 100 ticks |
| Sweet berry jam | Cooking pot, low heat | 6 sweet berries, 3 sugar, in a glass bottle | 300 ticks |
| Caramel sauce | Cooking pot, low heat | 1 caramel, 1 cream | 100 ticks |

Roux is an ingredient for béchamel and gravies in later features. Jam fills Berliners. Caramel sauce is a drink until a dessert uses it.

### Mortar and pestle

Phase 3 has one mortar recipe, grinding coarse salt. Pastes and spice blends follow with B-0050 ingredients.

### Tastes and seasoning

- **Tastes:**
  - **The tags:** `portable_pocket_cabin:tastes/salty`, `sweet`, `sour`, `bitter`, and `umami` are item tags. Every ingredient and dish in the mod, and the vanilla foods the recipes use, belongs to the tags of its tastes.
  - **Examples:**
    - Salt is salty.
    - Sugar and fruit are sweet.
    - Cider vinegar, buttermilk, yogurt, and quark are sour.
    - Nigari and toasted pumpkin seeds are bitter.
    - Aged hard cheese, dried kelp, and mushrooms are umami.
- **Seasonings:** the `portable_pocket_cabin:seasonings` tag lists what goes in a seasoning slot: salt, coarse salt, sugar, brown sugar, cider vinegar, nigari, and dried kelp.
- **Wanted tastes:** a dish recipe lists its `tastes`. For example, vegetable soup wants salty and umami, and cookies want sweet and salty.
- **Seasoning slots:**
  - **Where they are:** the cooking pot, oven, wok, prep counter, and mixing bowl screens have two seasoning slots.
  - **When they count:** when a batch finishes and its recipe lists tastes, the station takes one item from each filled seasoning slot.
  - **Batches:** the whole batch gets the quality.
  - **Without tastes:** a recipe without tastes leaves the seasonings alone.
- **Quality:**
  - **Scoring:** each taste a used seasoning brings scores +1 if the dish wants it and no earlier seasoning brought it, and −1 otherwise. The total is clamped to the three levels: plain at 0, seasoned at 1, and well balanced at 2.
  - **Storing it:** quality is saved on the result as a `portable_pocket_cabin:dish_quality` data component, per ADR-0014. Different qualities don't stack.
  - **Without seasoning:** a dish made without seasoning has no component and is plain.
- **Effect:** each quality level raises the dish's saturation by 15 percent of its base saturation. The result stack gets its own food component, and the tooltip names the level.
- **Skillet and grill dishes:** these stations have no screens. Their dishes are seasoned afterwards on the prep counter, with a `season` counter recipe whose result is the same dish.
- **Filling:** the recipe panel never fills seasoning slots. Hoppers can't reach them.

## B-0048: fermentation, drying, and preservation

### Cider and vinegar

| Product | Station | Inputs | Time |
|---|---|---|---|
| Cider | Crock | 4 apple juice, gives 4 | 2 days |
| Cider vinegar, first batch | Crock | 4 cider, left open, gives 4 | 5 days |
| Cider vinegar | Crock | 3 cider and 1 cider vinegar, gives 4 | 2 days |

Cider isn't drinkable. It exists only as the step to vinegar.

### Pickles

| Product | Station | Inputs | Time |
|---|---|---|---|
| Boiled beetroot | Cooking pot, low heat | 4 beetroot, 1 water, gives 4 | 200 ticks |
| Sliced beetroot | Cutting board | Boiled beetroot | - |
| Pickled beetroot | Crock | 4 sliced beetroot, 1 cider vinegar, 1 sugar, gives 4 and a glass bottle | 1 day |
| Pickled carrots | Crock | 4 chopped carrots, 1 cider vinegar, 1 salt, gives 4 and a glass bottle | 1 day |

Pickles restore 3 hunger and 3.6 saturation.

### Drying

| Product | Station | Inputs | Time |
|---|---|---|---|
| Dried apple | Drying rack | Chopped apple | Half a day |
| Dried sweet berries | Drying rack | Sweet berries | Half a day |
| Dried kelp | Drying rack | Kelp | Half a day |

Dried kelp replaces the B-0045 oven recipe.

Food values:

- Dried apple restores 3 hunger and 2.4 saturation.
- Dried sweet berries restore 2 hunger and 1.6 saturation.

### Curing and smoking

| Product | Station | Inputs | Time |
|---|---|---|---|
| Salted cod | Brine barrel | 2 cod, 1 salt, gives 2 | 1 day |
| Salt cod | Drying rack | Salted cod | 2 days |
| Beef strips | Cutting board | Beef, gives 2 | - |
| Cured beef strips | Brine barrel | 4 beef strips, 1 salt, gives 4 | Half a day |
| Jerky | Drying rack | Cured beef strips | 1 day |
| Cured salmon | Brine barrel | 2 salmon, 1 salt, 1 sugar, gives 2 | 1 day |
| Smoked salmon | Vanilla smoker | Cured salmon | 400 ticks |
| Brined pork | Brine barrel | 2 porkchops, 1 salt, 1 water bottle, gives 2 and a glass bottle | 1 day |
| Smoked pork | Vanilla smoker | Brined pork | 400 ticks |

Cured foods stack to 64. Their food values:

| Food | Hunger | Saturation |
|---|---|---|
| Salt cod | 6 | 9.6 |
| Jerky | 5 | 8.0 |
| Smoked salmon | 7 | 11.2 |
| Smoked pork | 8 | 12.8 |

### Cheese ageing

| Product | Station | Inputs | Time |
|---|---|---|---|
| Brined cheese | Brine barrel | Pressed cheese, 1 salt, 1 water bottle, giving a glass bottle | 1 day |
| Aged hard cheese | Ageing shelf | Brined cheese | 3 days |
| Hard cheese wedge | Cutting board | Aged hard cheese, gives 4 | - |
| Soft cheese curds | Strainer | 2 curds, 1 salt | 100 ticks |
| Aged soft cheese | Ageing shelf | Soft cheese curds | 2 days |

Food values:

- A hard cheese wedge restores 4 hunger and 6.4 saturation.
- Aged soft cheese restores 4 hunger and 5.0 saturation.

## Cookbook

- **Stations and techniques:** each new station and technique has an entry, and every page explains why the process works, including why curing and drying preserve food.
- **Times:** time station recipes show their time in Minecraft days and hours.
- **Worked recipes:** worked recipes show their number of uses.
- **Tastes:**
  - Every page shows the result's tastes, and a dish's page shows its wanted tastes.
  - A taste page explains each taste and lists its sources, as the B-0047 brief describes.
- **Notes:** item notes from B-0052 explain the leaven, the first yogurt culture, the first vinegar, and coarse salt.

## Verification

GameTests for the shared engine:

- **Recipe format:** the new recipe types load, and validation rejects heat, water, and containers where a station has none.
- **Worked stations:** spoon, pestle, and churn uses advance a batch, and a batch finishes at its use count.
- **Byproduct slots:** remainders fill both byproduct slots, and a batch waits while one is full. An old pot save loads.
- **Catch-up:**
  - A crock whose saved time is a day behind finishes its batch on its next tick and doesn't start a second.
  - A shelf finishes each spot's item but not the next.
- **Sun:** a sun recipe on the rack stops under a block, and a shade recipe stops under open sky.
- **Seasoning:**
  - A wanted seasoning gives "seasoned".
  - Two wanted tastes give "well balanced".
  - An unwanted taste lowers the quality.
  - A recipe without tastes leaves seasonings in place.
  - Quality changes saturation, and different qualities don't stack.
  - The recipe panel never fills seasoning slots.

GameTests for content:

- **Feature chains:** each chain in this design runs from raw ingredient to product. The chains are:
  - Flour, starter, bread, and rolls.
  - Pastry and pie.
  - Cookies and cake.
  - Berliner.
  - Cream, butter, and buttermilk.
  - Yogurt from buttermilk, then from yogurt.
  - Rennet, curds, and each cheese.
  - Rock salt and sea salt.
  - Coarse salt in shade.
  - Brown sugar and panela.
  - Jam, roux, and caramel sauce.
  - Cider and both vinegar recipes.
  - Pickles, dried foods, cured fish and meat, and smoking.
- **Supporting items:**
  - Seawater fills only in oceans.
  - Rock salt ore drops rock salt.
  - The cardoon plant grows and drops flowers.
  - Whey feeds pigs.
- **Replaced recipes:** the B-0045 bread, cookie, cake, pumpkin pie, and kelp oven recipes are gone.
- **Kitchen:** a crock and a mixing bowl in the kitchen fill from storage.
- **Crafting:** none of the new crafting recipes collides with a vanilla one.

Server checks: a time station in a packed cabin finishes its batch after the cabin is unpacked.

Client GameTests capture the mill, mixing bowl, crock, shelf and rack with items, a seasoned dish's tooltip, and cookbook taste pages for `screenshots/cooking/`.

## Delivery

Each step is a commit with its tests green:

1. **Shared engine:**
   - Worked stations, two byproduct slots, sun, water bottle ingredients, and game-time progress with catch-up.
   - The ADRs.
   - Test recipes.
2. **Preparation stations:** the mill, mixing bowl and spoon, mortar and pestle, butter churn, and cheese press.
3. **Time stations:**
   - The proofing basket, dairy pan, crock, brine barrel, ageing shelf, and drying rack.
   - Vanilla smoker recipes.
4. **Salt:** rock salt ore and its world generation, seawater, sea salt and nigari, and coarse salt.
5. **Sugars:** cane juice, brown sugar, panela, simple syrup, and caramel.
6. **Flour and bakes:**
   - Flour, leavening, doughs, and bakes, including the Berliner.
   - Removing the replaced oven recipes.
7. **Dairy:** cream, butter, buttermilk, whipped cream, yogurt, and crème fraîche.
8. **Cheese:** cardoon and its world generation, rennet, curds, and every cheese, including brining and ageing.
9. **Preserving:** cider, vinegar, pickles, drying, curing, and smoking.
10. **Sauces:** roux, jam, and caramel sauce.
11. **Seasoning:** tastes, seasoning slots, dish quality, and the counter's season recipes.
12. **Docs and the rest:**
    - Cookbook entries and taste pages.
    - Screenshots, README, and backlog.

## Assumptions

- **Art:** placeholder models and textures reuse vanilla art until B-0034.
- **Units:**
  - A Minecraft day is 24000 ticks, and an hour is 1000.
  - Times, use counts, food values, and the saturation bonus are first guesses. B-0033 tunes them.
- **Ore:** rock salt ore frequency is a first guess. B-0033 tunes it.
- **Recipe counts:** the cookbook groups phase 3's roughly 90 new recipes by station, as before.
