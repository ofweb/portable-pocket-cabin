# B-0052 design: pressing and drinks

Status: Ready
Feature ID: B-0052

This design implements the [B-0052 brief](brief.md). It builds on the [B-0053 design](../B-0053/design.md) and follows [ADR-0010](../../decisions/adr/0010-define-cooking-as-station-recipe-types.md): the press gets its own recipe type in the shared cooking recipe format.

## Press

- **Recipe type:** `CookingStation.PRESS`, path `press`, has one ingredient slot and no heat. It takes a container. Its time is required and short, like the strainer's.
- **The block:** `PressBlock` is a free-standing full block, so it can stand anywhere, including the [B-0041](../B-0041/brief.md) kitchen. It doesn't take up the prep counter's top.
- **Block entity:** it reuses `ProcessStationBlockEntity` with a new `CookingMenu.Kind.PRESS`. It has an ingredient slot, a container slot for glass bottles, an output slot, and a solids slot, which is the byproduct slot.
- **Process:** the result is the liquid in its bottle, and `remainders` give the solids.
- **Screen:** the press screen has the recipe panel. Kitchen storage fills it as it does other stations.
- **Hoppers:** hoppers on the top feed the ingredient. Side hoppers feed the bottles. Hoppers below take both outputs.
- **Placement:** the block faces the player. It drops itself when broken and spills its contents like other stations.

## Oil

- **Frying oil:** the pot and wok keep holding generic frying oil, as B-0053 built it.
- **Filling:** sunflower oil joins the `portable_pocket_cabin:cooking_oils` tag, so its bottle fills the pot and wok. Rapeseed and olive oil join the tag later with B-0050.
- **Recipes:** a recipe with `oil` takes any frying oil.
- **A specific oil:** a recipe that needs one lists that oil's bottle as an ingredient.
- **Pumpkin seed oil:** it isn't in the tag, because toasted pumpkin seed oil smokes at frying heat. It is an ingredient for dressings in later features.

## Items

Ingredients and solids:

| Item | From | Notes |
|---|---|---|
| Grated carrot | Chopped carrot on the cutting board, grating | The cutting board already turns carrots into chopped carrot, so grating is a second cut. |
| Sunflower seeds | Vanilla sunflower on the cutting board, threshing, gives 4 | A picked sunflower counts as a dried head. |
| Toasted pumpkin seeds | Vanilla pumpkin seeds on the skillet, medium heat, 100 ticks | |
| Apple pomace | Pressing chopped apples | Composts. |
| Pulp | Pressing berries, melon, or carrots | Composts. |
| Press cake | Pressing seeds | Composts. Breeds and feeds cows, mooshrooms, sheep, and goats, through their vanilla food tags. |

Apples use the existing chopped apple from B-0045 as the quartered fruit. A new apple quarters item would only duplicate it.

Liquids and drinks:

| Item | Made by | Drink |
|---|---|---|
| Apple juice (æblemost) | Press: 4 chopped apples and a glass bottle, giving apple pomace | 3 hunger |
| Sweet berry juice | Press: 4 sweet berries and a glass bottle, giving pulp | 3 hunger |
| Melon juice | Press: 4 melon slices and a glass bottle, giving pulp | 3 hunger |
| Carrot juice | Press: 4 grated carrots and a glass bottle, giving pulp | 3 hunger |
| Unstrained berry cordial | Cooking pot, low heat, 200 ticks: 6 sweet berries, 2 sugar, 1 water, and a glass bottle | Not drinkable |
| Sweet berry cordial | Strainer, 60 ticks: unstrained berry cordial, giving pulp | 4 hunger |
| Apple syrup | Cooking pot, low heat, 400 ticks: 4 apple juice, giving 3 glass bottles | 6 hunger, like a honey bottle |
| Sunflower oil | Press: 4 sunflower seeds and a glass bottle, giving press cake | Not drinkable. Fills the pot or wok. |
| Pumpkin seed oil | Press: 4 toasted pumpkin seeds and a glass bottle, giving press cake | Not drinkable |

- **Drinks:** they use the drink animation, return the glass bottle, and can be drunk when full, like a honey bottle.
- **Cordial and syrup:** cordial is a concentrate. Diluting it into saftevand needs the B-0046 mixing bowl, so diluted drinks ship in phase 3. Until then, cordial and syrup are sweet drinks on their own.
- **Pressing time:** 60 ticks for every press recipe.

## Frying recipes

Phase 2 ships the frying recipes whose ingredients exist:

| Dish | Station | Inputs | Result |
|---|---|---|---|
| Fried potatoes | Cooking pot, high heat, oil, 100 ticks | 4 diced potatoes, 1 oil level | 4 fried potatoes, 3 hunger each |
| Stir-fried vegetables | Wok, high heat, oil, 120 ticks | Chopped carrot, chopped beetroot, sliced brown mushrooms, 1 oil level, a bowl | Stir-fried vegetables in a bowl, 8 hunger |

Fried potatoes are also an intermediate for later dishes such as lomo saltado. The catalogue has no wok dish whose ingredients all exist yet. Stir-fried vegetables is a simple starter dish, so the wok is useful now.

## Cookbook

- **Entries:** the press appears as a station, and the cookbook lists its recipes and every new item.
- **Techniques:** pressing, grating, threshing, toasting, making cordial, reducing, deep-frying, and stir-frying. Deep-frying and stir-frying already exist from B-0053.
- **Oil notes:**
  - Sunflower oil's page says it is the everyday frying oil.
  - Pumpkin seed oil's page says it is a finishing oil for dressings, not for frying.

## Verification

GameTests:

- **Format:** press recipes load, and the press refuses water, oil, and heat in its recipes.
- **Press:**
  - Each juice and oil is pressed with its solids.
  - The press waits without bottles.
  - A full output stops it.
  - Hoppers feed and empty it.
  - It keeps progress when saved and loaded.
- **Chains:**
  - A sunflower is threshed into seeds, pressed into oil, and the oil fills a wok.
  - Pumpkin seeds are toasted on a skillet, then pressed into pumpkin seed oil.
  - Pumpkin seed oil doesn't fill a pot.
  - Sweet berries are cooked into unstrained cordial, then strained into cordial and pulp.
  - Apple juice is reduced into syrup, giving back bottles.
- **Frying:**
  - Fried potatoes deep-fry in a pot with sunflower oil and use one level.
  - Stir-fried vegetables cook in an oiled wok.
- **Items:**
  - Drinks return their bottle.
  - Each solid composts.
  - Press cake breeds cows and sheep.
- **Crafting:** the press recipe doesn't collide with a vanilla recipe.
- **Kitchen:** a press in the kitchen fills from central storage.
- **Counts:** the cooking recipe count matches the new total.

Client GameTests capture the press screen, a pressed juice, and the cookbook's sunflower oil page for `screenshots/cooking/`.

## Delivery

Each step is a commit with its tests green:

1. The press, its recipe type, block, screen, and recipe panel.
2. Fruit items, juices, solids, composting, and the press cake animal food.
3. Sunflower and pumpkin seed oil, filling the pot and wok, and the frying recipes.
4. Cordial and syrup.
5. Cookbook entries, screenshots, README, and backlog.

## Assumptions

- **Press crafting:** planks around a piston, with a slab on top: `SSS`, `PIP`, `PPP`, where S is any wooden slab, P is planks, and I is a piston.
- **Art:** placeholder models reuse vanilla textures until B-0034 adds art.
- **Tuning:** pressing ratios, times, and hunger values are first guesses. B-0033 tunes them.
