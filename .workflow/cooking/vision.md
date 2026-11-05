# Cooking when it is finished

This describes the intended end state of cabin cooking, after every cooking feature from B-0041 and B-0045 through B-0052, plus cooking automation in B-0025, is done. The [cooking concept](concept.md) explains the reasoning, and each feature brief owns its details. Items marked as proposals still need a decision.

## In one paragraph

Food in this world is real work. Bread starts as wheat in a field, then becomes flour at a mill, dough in a mixing bowl, a risen loaf in a proofing basket, and finally bread in an oven fired with the right fuel. A burger needs a bun, minced beef from a butchered carcass, cheese pressed and aged from milk, and pickles fermented in a crock. Every step happens at a station that works like a Minecraft furnace, campfire, or stonecutter, so nothing is hard to operate, but there is a lot to do. Early on a player cooks simple meals by hand. Later the cabin's kitchen keeps salt, flour, stock, and sauces stocked, and the player cooks lasagne from a full pantry. Good food is worth it: complex dishes give strong, long-lasting benefits for mining, exploring, or fighting.

## What the player sees

### Stations

All cooking happens at cooking stations. Furnaces, smokers, campfires, and crafting tables do not make food. Each station does one kind of work and behaves like a vanilla block players already know.

| Kind | Works like | Stations |
|---|---|---|
| Click | Stonecutter | Cutting board, mortar and pestle, butcher block |
| Surface | Campfire | Skillet, grill |
| Container | Furnace or brewing stand | Cooking pot, oven, mill, mixing bowl, meat grinder, sausage stuffer, fruit press, butter churn, cheese press |
| Time | Ambient, no heat | Drying rack, fermentation crock, pickling jar, proofing basket, brine barrel, ageing shelf |
| Heat source | Fuel block | Stove, campfire |

Stations can be crafted and placed anywhere. The cabin kitchen supplies protected sets of them and connects them to central storage.

### Heat

Fuel sets the heat. Sticks and planks give low heat for simmering and slow baking. Logs, charcoal, and coal give medium heat for frying and baking. Coal blocks, blaze rods, and lava give high heat for searing and roasting. A campfire under a pot gives low heat with no fuel at all. Choosing the right fuel is the player's temperature control, and the station says when the heat is wrong.

Food never burns, spoils, or fails. Wrong heat or a missing ingredient just pauses progress.

### Real intermediate items

Every stage is an item: chopped onion, flour, dough, risen dough, butter, buttermilk, curds, cheese, aged cheese, stock, roux, béchamel, tomato sauce, minced beef, sausage, bacon, sauerkraut, dried mushrooms, chili paste, miso, dashi. Players can store them, trade them, and reuse them. The items themselves show how much work is in a dish.

### The cookbook

The cookbook is the player's guide to all of it:

- every recipe from the start, searchable, with its station, ingredients, heat, and time
- links from each ingredient to the recipe that makes it, so players can follow a dish back to the field
- short explanations of each technique, such as simmering, braising, proofing, emulsifying, curing, and nixtamalization
- where raw ingredients come from: which crops need what soil and light, which mushrooms grow wild, and which animals give which cuts
- which recipes the player can make right now, counting cabin storage for the household

## What there is to make

### The processes

| Area | Feature | What players do |
|---|---|---|
| Cooking basics | B-0045 | Chop, fry, sear, simmer, bake. First stations and the cookbook. |
| Milling, baking, and dairy | B-0046 | Mill wheat, rye, oats, and corn into flour. Mix and proof yeast and sourdough. Churn butter. Press and age cheese. Culture yogurt. |
| Stocks, sauces, and flavour | B-0047 | Simmer stocks from bones and vegetables. Make dashi, roux, béchamel, tomato sauce, gravy, and emulsions. Grind spices and pastes. |
| Fermentation, drying, and preservation | B-0048 | Ferment sauerkraut, kimchi, chili sauce, miso, soy sauce, and vinegar. Dry herbs, mushrooms, chilies, fruit, and kelp. Smoke, cure, brine, pickle, and age. |
| Butchery | B-0049 | Butcher carcasses into steaks, chops, joints, ribs, stew meat, offal, bones, and fat, using the [whole animal](butchery.md). Mince, stuff sausages, render fat, and fillet fish. |
| Crops and foraging | B-0050 | Grow grains, vegetables, alliums, legumes, chilies, herbs, spices, fruit trees, and berries with real needs. Cultivate mushrooms on logs and beds. Forage rare wild ingredients. |
| Drinks | B-0052 | Press apple juice for æblemost. Make saftevand and other cordials and concentrates. Infuse and filter. |
| Food benefits | B-0051 | Turn the work into rewards. |
| Kitchen | B-0041 | A protected, storage-connected cooking room in the cabin. |
| Automation | B-0025 | Let the cabin make familiar products on its own. |

Wine, sake, mirin, and similar ingredients exist only for cooking, in braises, reductions, and sauces. Alcohol production is not a focus.

### The dishes

The target catalogue has 147 recipes from French, German, Japanese, Thai, Mexican, South American, and Danish cooking: 56 main dishes, 56 desserts and snacks, and 35 non-alcoholic drinks. Many vanilla foods become the simplest members of these chains. The [target recipes](source/recipes.md) list them.

[Five recipes, start to finish](examples.md) work through bread, French onion soup, bratwurst with sauerkraut, miso soup, and æblemost in station-by-station detail.

### Two examples from scratch

**Cheeseburger**

1. Grow wheat. Mill it into flour. Mix flour, water, salt, and yeast into dough. Proof it, shape it into buns, and bake them at medium heat.
2. Butcher a cow carcass into beef. Grind it into mince.
3. Milk the cow. Press curds into cheese and age it on a shelf.
4. Grow cucumbers. Brine and ferment them into pickles in a crock.
5. Grow lettuce and tomatoes. Slice them on the cutting board.
6. Shape and sear the patty at high heat, then assemble the burger.

**Lasagne**

1. Make pasta dough from flour and eggs. Roll it into sheets.
2. Make ragù: sauté onion and garlic, add minced beef and tomato sauce, and simmer at low heat.
3. Make béchamel: cook butter and flour into a roux, then whisk in milk.
4. Grate aged cheese.
5. Layer everything and bake.

The first time takes a long session. Once the kitchen keeps pasta sheets, tomato sauce, béchamel, and cheese stocked, lasagne is a quick meal. That change is the whole point.

## Why players bother

### Food benefits

- Every ingredient carries a tiny contribution to one or more benefit families, about six to eight in all, such as endurance, resilience, recovery, warmth, perception, and agility.
- Processing develops those contributions. Simmering lengthens them, roasting strengthens them, and fermentation can add new ones. Each recipe gives a bounded improvement, so repeated processing gives nothing extra.
- Complete dishes combine their ingredients' contributions into a few dominant effects. Only a few major food benefits can be active at once.
- Complex dishes give stronger, longer, or more specialised benefits. A slow beef stew suits a long mining trip, and a spicy seafood dish suits exploring.
- A dish gives the same benefits whoever made it: the player, a villager, or the cabin.

### Shortcuts that cost something

Villagers sell some prepared ingredients and vanilla foods. Buying cheese means someone else did the cheese-making. It is a fair shortcut, not a way around the system.

## How the cabin fits

- **Kitchen:** an installable room with supplied stations. Later cooking features add purchases for their own stations, such as a pantry wall, a cellar of ageing shelves and crocks, or a butchery corner.
- **Storage:** the kitchen fills stations from central storage first, including fuel at the right heat level. The cookbook counts storage when it shows what the player can make.
- **Greenhouse and livestock:** they grow the crops and raise the animals that supply the kitchen.
- **Automation:** an automation book reveals kitchen automation.
  - Commissioning a recipe consumes some of its finished product.
  - The cabin then makes it with the same stations, heat, and times as a player would, limited by its equipment.
  - Stock targets keep the pantry full, for example 32 salt, 16 flour, and 8 chicken stock.
  - Automation spreads upstream over time: once butchery is automated, stock production draws its bones from carcasses.
- **Packed cabins:** stations pause while an empty cabin is packed. Catch-up limits are still open.

## A player's arc

- **Early:** fried eggs, seared meat, mushroom stew, and baked potatoes on a campfire and a homemade stove. A first loaf of bread feels like an achievement.
- **Middle:** a kitchen room, a mill, a crock of sauerkraut, a cellar of aging cheese, and butchered meat cut by cut. Salt, flour, and stock get automated first.
- **Late:** a big kitchen with several cooking lines, a stocked pantry, and automation reaching back to the farm. The player cooks cassoulet or ramen for the benefits, and knows exactly what went into it.

## What it is not

- No burning, spoilage, failure, or timing minigames.
- No unfamiliar controls. Every station works like a vanilla block.
- No alcohol production as a focus.
- No dependency on Farmer's Delight or other food mods.

## Proposals to confirm

- **Seasoning and the five tastes:** recipes could have optional seasoning slots. Adding salt, an herb, acid, or umami that balances a dish would raise its quality, and so its benefits. Following the basic recipe still makes a good dish.
- **Stations in the kitchen:** later features would add their stations to the kitchen through new purchases, rather than growing the 5×5 room. The room might need to be larger for that.
- **Villager prepared ingredients:** which prepared ingredients villagers sell, and from which professions.
