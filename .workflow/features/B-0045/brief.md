# Players cook real recipes at cooking stations

Status: Ready
Feature ID: B-0045

## Goal

Players cook real dishes at cooking stations that work like Minecraft's own furnace, campfire, and stonecutter. Food passes through real intermediate items, such as chopped vegetables and sautéed mushrooms, so each chain shows the work behind a dish. A station's [heat level](../../context.md#heat-level) comes from the fuel that heats it. A [cookbook](../../context.md#cookbook) explains every recipe and technique. Food is cooked only at cooking stations. This feature provides the stations, the cookbook, and a starter set of recipes. Later cooking features add stations, such as drying racks and fermentation crocks, and longer chains through the same rules.

## Stories and acceptance

### S1: Set up cooking stations

Story: A player crafts and places cooking stations to start cooking without the cabin.

Acceptance:

- The first stations are a stove, cooking pot, skillet, oven, and cutting board. The kitchen knife is a held tool.
- Stations can be crafted and placed anywhere players can normally place blocks.
- The stove and oven burn fuel in a fuel slot. A cooking pot or skillet cooks while it sits on a lit stove or a lit campfire.
- Each station shows its heat level and cooking progress.
- Cooking progresses only at the heat level its recipe needs. At any other heat, progress pauses and the station names the heat it needs.
- Food never burns, spoils, or fails. Progress is never lost.

| Station | Ingredients | How it works |
|---|---|---|
| Cutting board | 2 planks, 1 stick | Place an ingredient on the board, then use the knife on it. The ingredient becomes its prepared item. |
| Kitchen knife | 1 iron ingot, 1 stick | Used at the cutting board. It does not wear out. |
| Stove | 1 furnace, 2 iron ingots | Burns fuel to heat the pot or skillet above it. |
| Cooking pot | 6 copper ingots | Container with ingredient slots, a container slot, and an output slot. A water bucket fills it with water for recipes that need water. |
| Skillet | 3 iron ingots, 1 stick | Holds up to four items, like a campfire. Each cooks into its output separately. |
| Oven | 1 furnace, 8 bricks | Container like a furnace, with ingredient slots, a fuel slot, and an output slot. |

### S2: Control heat with fuel

Story: A player picks a fuel to give a recipe the heat it needs.

Acceptance:

- Each fuel burns at one heat level: low, medium, or high.
- A lit campfire or soul campfire gives low heat to a pot or skillet above it, without fuel.
- Changing fuel changes the heat when the new fuel starts burning.
- The cookbook and the station both show each recipe's heat level, so players learn which fuel suits simmering, frying, or searing.

| Heat level | Fuels | Typical techniques |
|---|---|---|
| Low | Sticks, planks, saplings, wooden items, dried kelp blocks, bamboo | Simmering, gentle frying, slow baking |
| Medium | Logs, charcoal, coal | Sautéing, frying, baking |
| High | Coal blocks, blaze rods, lava buckets | Searing, roasting |

### S3: Cook through intermediate items

Story: A player makes a dish in several stages, from raw ingredients to the finished food.

Acceptance:

- Each stage produces a real item that can be stored, traded, or used later. Raw ingredients become prepared items, and prepared items become dishes.
- A recipe matches its station's inputs, like a furnace or stonecutter. No activation or step tracking is needed.
- Several stations can cook at once. Each keeps its own progress while the player works elsewhere or leaves.
- Recipes that need water use the pot's water. One water bucket fills the pot for three water uses.
- Completing a recipe returns its containers, such as a bowl, or keeps them for the output.
- Breaking a station drops its contents. Unfinished progress restarts when the items are cooked again.

### S4: Learn from the cookbook

Story: A player looks up how a dish is made and fills a station from the recipe.

Acceptance:

- A cookbook is crafted from 1 book and 1 bowl. It is not a cabin book and reveals no upgrades.
- The cookbook lists every cooking recipe from the start. Players can search by name.
- Each recipe shows its station, ingredients, heat level, time, and output. An ingredient that has its own recipe links to it.
- Each technique has a short explanation, such as what simmering means and why it is used.
- Each recipe shows whether the player has its ingredients. Inside a cabin with installed central storage, owners and residents also count that storage.
- At a container station, a recipe-book button lists that station's recipes. Selecting one fills its slots, like the vanilla recipe book. It skips named and customised stacks.

### S5: Cook only at cooking stations

Story: A player makes familiar foods through cooking stations instead of furnaces or crafting.

Acceptance:

- Furnaces, smokers, and campfires no longer cook food. Campfires still heat a pot or skillet above them.
- Crafting no longer makes bread, cookies, cake, pumpkin pie, mushroom stew, beetroot soup, or rabbit stew. Furnaces no longer make dried kelp.
- Cooking recipes make those vanilla foods instead, along with new dishes.
- Golden foods, suspicious stew, and items that are not food keep their vanilla recipes.
- Villager trades and loot keep vanilla food. Bought food represents work someone else did.
- Existing food items keep working in inventories, storage, and upgrade funds.

| Station | Recipe | Inputs | Heat | Output |
|---|---|---|---|---|
| Cutting board | Chopped vegetables | Carrot, potato, or beetroot | — | Chopped carrot, diced potato, or chopped beetroot |
| Cutting board | Sliced mushrooms | Brown or red mushroom | — | Sliced mushrooms of that colour |
| Cutting board | Chopped apple | Apple | — | Chopped apple |
| Skillet | Seared meat | Raw beef, porkchop, mutton, chicken, or rabbit | High | Matching cooked meat |
| Skillet | Fried fish | Raw cod or salmon | Medium | Matching cooked fish |
| Skillet | Sautéed mushrooms | Sliced brown mushrooms | Medium | Sautéed mushrooms |
| Skillet | Fried egg | Egg | Low | Fried egg |
| Cooking pot | Mushroom stew | Sautéed mushrooms, sliced red mushrooms, water, bowl | Low | Mushroom stew |
| Cooking pot | Beetroot soup | 6 chopped beetroot, water, bowl | Low | Beetroot soup |
| Cooking pot | Rabbit stew | Cooked rabbit, baked potato, chopped carrot, sautéed mushrooms, water, bowl | Low | Rabbit stew |
| Cooking pot | Vegetable soup | Chopped carrot, diced potato, chopped beetroot, water, 3 bowls | Low | 3 vegetable soup |
| Cooking pot | Apple compote | 3 chopped apple, sugar, water, 2 bowls | Low | 2 apple compote |
| Oven | Baked potato | Potato | Medium | Baked potato |
| Oven | Bread | 3 wheat | Medium | Bread |
| Oven | Cookies | 2 wheat, cocoa beans | Medium | 8 cookies |
| Oven | Pumpkin pie | Pumpkin, sugar, egg | Medium | Pumpkin pie |
| Oven | Cake | 3 wheat, 2 sugar, egg, 3 milk buckets | Low | Cake and 3 empty buckets |
| Oven | Dried kelp | Kelp | Low | Dried kelp |

Bread, cookies, pumpkin pie, and cake bake from wheat until B-0046 supplies flour and dough. Dried kelp uses the oven until B-0048 supplies drying.

## Feature-wide constraints and acceptance

- Each fill, cut, and output rechecks the recipe, the station, its contents, and the player's permission before it changes anything.
- An interrupted action completes once or changes nothing. Recovery cannot lose ingredients or duplicate food.
- Station contents, water, fuel, heat, and progress persist through restart.
- Inside a cabin, stations follow household roles in B-0002. Guests cannot use them.
- In a packed cabin with no players, stations pause with the rest of the interior. This feature adds no catch-up.
- Cooking recipes and fuel heat levels are data that datapacks can add to or change. Later cooking features add recipes and stations this way.
- Recipes follow real dishes and techniques where practical.

## Scope

This feature includes the six stations and the knife, heat levels from fuel and campfires, the cookbook and recipe-book filling, the prepared items, the starter recipes, and the replacement of vanilla food cooking listed in S5.

## Non-goals

- The kitchen room and its supplied stations belong to B-0041.
- Food benefits beyond hunger and saturation belong to B-0051. New dishes give hunger and saturation only.
- Milling, flour, dough, and dairy belong to B-0046. Stocks, sauces, and seasoning belong to B-0047. Drying, fermentation, smoking, and preservation belong to B-0048. Butchery belongs to B-0049. New crops belong to B-0050. Drinks belong to B-0052.
- Cooking automation belongs to B-0025.

## Related records

- [Cooking concept](../../cooking/concept.md).
- [Cooking and food production](../../direction.md#cooking-and-food-production).
- [The cabin provides its own cooking](../../decisions/pdr/0015-build-cooking-into-the-cabin.md).
- [Manual kitchen](../B-0041/brief.md).
- [Household roles](../B-0002/brief.md).
- [Central storage](../B-0004/brief.md).
- [Manual crafting room](../B-0039/brief.md).
- [Cooking and brewing jobs](../B-0025/brief.md).

## Open questions and assumptions

- Hunger and saturation values for new dishes, cooking times, and the fuel burn length at each heat level are set during Design. B-0033 balances station costs.
- Catch-up for stations while an empty cabin is packed remains open under [Direction](../../direction.md#progress-after-an-empty-cabin-becomes-occupied).
