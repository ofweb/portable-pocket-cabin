# Players cook real recipes from a cookbook

Status: Ready
Feature ID: B-0045

## Goal

Players follow [cooking recipes](../../context.md#cooking-recipe) from a [cookbook](../../context.md#cookbook) at cooking equipment anywhere in the world. Recipes teach real techniques through ordered steps, heat, and time. Cooking is forgiving: wrong actions are ignored, and food never burns or spoils. This feature provides the cookbook, the first equipment, and a starter set of recipes. Later cooking features add their own recipes and equipment through the same rules.

## Stories and acceptance

### S1: Browse the cookbook

Story: A player crafts a cookbook and looks up how to make a dish.

Acceptance:

- A cookbook is crafted from 1 book and 1 bowl. It is not a cabin book and reveals no upgrades.
- Using the cookbook opens a screen listing every cooking recipe from the start. Players can search by name.
- Each recipe shows its ingredients and quantities, equipment, ordered steps, techniques, temperature ranges, durations, output, and portions.
- An ingredient that has its own cooking recipe links to it. Selecting the link opens that recipe.
- Each technique has a short explanation, such as what simmering means and why it is used.
- Each recipe shows whether it is unavailable or ready. Unavailable recipes mark the missing ingredients.
- Availability counts the player's inventory. Inside a cabin with installed central storage, owners and residents also count that storage.

### S2: Set up cooking equipment

Story: A player crafts and places cooking equipment to start cooking without the cabin.

Acceptance:

- The first equipment is a stove, cooking pot, skillet, oven, cutting board, and kitchen knife.
- Equipment can be crafted and placed anywhere players can normally place blocks. The knife is a held tool.
- The stove and oven burn normal furnace fuel. A heat dial sets off, low, medium, or high.
- Temperature rises and falls gradually toward the dial setting, so preheating takes time. The equipment shows its current temperature.
- A cooking pot or skillet heats when placed directly on a lit stove. Removing it from the stove lets it cool.
- The equipment controls heat, and recipes check the temperature range. Ingredient temperatures are not simulated.
- Each piece of equipment holds one preparation at a time.

| Equipment | Ingredients | Techniques |
|---|---|---|
| Cutting board | 2 planks, 1 stick | Chopping, slicing, mixing cold ingredients |
| Kitchen knife | 1 iron ingot, 1 stick | Used at the cutting board |
| Stove | 1 furnace, 2 iron ingots | Heat for a pot or skillet |
| Cooking pot | 6 copper ingots | Boiling, simmering, stewing |
| Skillet | 3 iron ingots, 1 stick | Frying, searing, sautéing |
| Oven | 1 furnace, 8 bricks | Baking, roasting |

### S3: Cook one recipe

Story: A player activates a ready recipe and follows its steps to make the dish.

Acceptance:

- The player selects a ready recipe in the cookbook and activates it.
- Activation consumes nothing. Ingredients are used only when an accepted step takes them.
- The cookbook shows each active recipe's current step, the action it needs, the equipment, and the required temperature.
- Active steps need a player action, such as adding an ingredient, chopping with the knife, stirring, flipping, or moving food between equipment.
- Passive steps progress while their conditions hold, such as simmering at the right temperature or baking in a hot oven.
- The equipment reports what the step needs, such as "Heating: 65°C / 85–95°C required".
- An action the step does not need is ignored. A wrong ingredient is not accepted and stays with the player.
- Unsuitable temperature, a missing stove, or an empty fuel slot pauses progress. Progress is never lost.
- Food never burns, spoils, or fails. Overheating pauses progress until the temperature returns to the range.
- Completing the last step gives the output and any returned containers, such as an emptied bucket or a bowl.

### S4: Cook several recipes at once

Story: A player keeps a pot simmering while frying something else.

Acceptance:

- A player can have several recipes active at once.
- Progress belongs to the food in the equipment, not to the player. A simmering pot keeps progressing while the player cooks elsewhere or leaves.
- Moving food between equipment uses a preparation item. It records its recipe and completed steps.
- A preparation item cannot be eaten. Any player can continue it at suitable equipment.
- Two recipes cannot share one piece of equipment at the same time. The cookbook says which equipment is busy.
- Breaking equipment returns its preparation as an item. It keeps its progress.

### S5: Replace vanilla cooking

Story: A player cooks familiar foods through the cookbook instead of a furnace or crafting grid.

Acceptance:

- Furnaces, smokers, and campfires no longer cook food. Crafting no longer makes mushroom stew, beetroot soup, or rabbit stew.
- Cookbook recipes make those vanilla foods instead, along with new dishes.
- Bread, cookies, cake, and pumpkin pie keep their vanilla recipes until milling in B-0046 supplies flour. Dried kelp keeps its recipe until drying in B-0048.
- Golden foods, suspicious stew, and items that are not food keep their vanilla recipes.
- Villager trades and loot keep vanilla food. Bought food represents work someone else did.
- Existing food items keep working in inventories, storage, and upgrade funds.

| Recipe | Main ingredients | Equipment and techniques | Output |
|---|---|---|---|
| Pan-fried meat | One raw beef, porkchop, chicken, mutton, or rabbit | Preheat skillet; sear; flip; finish at frying heat | Matching vanilla cooked meat |
| Pan-fried fish | One raw cod or salmon | Preheat skillet; fry; flip | Matching vanilla cooked fish |
| Baked potatoes | 4 potatoes | Preheat oven; bake | 4 baked potatoes |
| Mushroom stew | Brown and red mushrooms, water, bowl | Slice on the cutting board; sauté in a skillet; simmer in a pot | Mushroom stew |
| Beetroot soup | 6 beetroots, water, bowl | Chop; simmer | Beetroot soup |
| Rabbit stew | Cooked rabbit, baked potato, carrot, mushroom, water, bowl | Chop; brown the rabbit; simmer | Rabbit stew |
| Vegetable soup | Carrot, potato, beetroot, water, bowls | Chop; simmer | New dish, several portions |
| Sautéed mushrooms | Brown mushrooms | Slice; sauté at medium heat | New dish |
| Fried eggs | Eggs | Preheat skillet; fry gently | New dish |
| Apple compote | Apples, sugar, water | Chop; simmer until soft | New dish, several portions |

## Feature-wide constraints and acceptance

- Each step action rechecks the recipe, the equipment, its contents, and the player's permission before it changes anything.
- An interrupted action completes once or changes nothing. Recovery cannot lose ingredients or duplicate food.
- Equipment contents, temperatures, fuel, and preparations persist through restart.
- Inside a cabin, equipment follows household roles in B-0002. Guests cannot use it.
- In a packed cabin with no players, equipment pauses with the rest of the interior. This feature adds no catch-up.
- Cooking recipes are data that datapacks can add to or change. Later cooking features add recipes this way.
- Recipes follow real dishes and techniques where practical.

## Scope

This feature includes the cookbook, recipe states and steps, temperature and fuel, the six pieces of starter equipment, preparation items, the starter recipes, and the replacement of vanilla cooking listed in S5.

## Non-goals

- The kitchen room and its supplied equipment belong to B-0041.
- Food benefits beyond hunger and saturation belong to B-0051. New dishes give hunger and saturation only.
- Milling, flour, bread, and dairy belong to B-0046. Stocks, sauces, and seasoning belong to B-0047. Drying, fermentation, and preservation belong to B-0048. Butchery belongs to B-0049. New crops belong to B-0050. Drinks belong to B-0052.
- Cooking automation and storage filling by jobs belong to B-0025.

## Related records

- [Cooking concept](../../cooking/concept.md).
- [Cooking and food production](../../direction.md#cooking-and-food-production).
- [The cabin provides its own cooking](../../decisions/pdr/0015-build-cooking-into-the-cabin.md).
- [Manual kitchen](../B-0041/brief.md).
- [Household roles](../B-0002/brief.md).
- [Central storage](../B-0004/brief.md).
- [Cooking and brewing jobs](../B-0025/brief.md).

## Open questions and assumptions

- Hunger and saturation values for new dishes, and the exact temperature ranges and durations for each recipe, are set during Design. B-0033 balances equipment costs.
- The kitchen knife is assumed not to wear out, so chopping never costs durability.
- Catch-up for preparations while an empty cabin is packed remains open under [Direction](../../direction.md#progress-after-an-empty-cabin-becomes-occupied).
