# Cooking and food production

This concept describes the intended cabin cooking system. It replaces Farmer's Delight as the cabin's food content. [PDR-0015](../decisions/pdr/0015-build-cooking-into-the-cabin.md) records that decision. The [source material](source/) keeps the ingredient catalogue, dependency graphs, and target recipes from the design conversation.

## Goal

Players learn how real food is made. Food changes apply throughout the world, not only in the cabin. They use actual cooking techniques and follow food from farm to table. Ordinary dishes such as a burger, pizza, or lasagne turn out to need many ingredients and steps when made from scratch. Most of that work is normally done by others. The player does it first by hand, then builds a kitchen and automation that make complex cooking convenient.

## Principles

- **Teach real cooking.** Recipes use real techniques such as simmering, braising, proofing, fermentation, emulsification, and flavour balancing. The cookbook explains each technique briefly.
- **Feel like Minecraft.** Stations work like vanilla furnaces, campfires, and stonecutters. Fuel sets the heat. Depth comes from longer chains and more stations, not from unfamiliar controls.
- **Show the whole chain.** Ingredients come from farming, animals, fishing, foraging, exploration, and trade. Processing turns them into intermediate products and dishes.
- **Base recipes on real dishes.** Recipes follow real dishes and preparation methods where practical.
- **Keep cooking forgiving.** Food does not burn, spoil, or lose progress. Unsuitable conditions pause progress.
- **Reward complexity.** Ingredients carry small benefits. Processing and complete dishes make them worthwhile.
- **Earn automation.** Players prepare food manually first. Automation takes over familiar, repetitive processes later.
- **Reward exploration.** Crops, fruits, mushrooms, herbs, and spices grow in different environments. Rare ingredients give exploration goals. Common ingredients stay available early.

## Ingredients and processing

Raw ingredients become intermediate products through processing. Intermediate products combine or process further into dishes.

| Product | Chain |
|---|---|
| Bread | Wheat → milling → flour → mixing with yeast, water, and salt → dough → kneading → proofing → shaping → baking |
| Tomato sauce | Tomatoes, onion, garlic, oil, oregano → chopping → sautéing → simmering |
| Chicken stock | Chicken carcass → butchery → bones, vegetables, water → simmering → straining |
| Cheeseburger | Bun, minced beef, cheese, vegetables, pickles → shaping and frying the patty → assembly |

Every stage produces a real item, like ingots from ore. Examples are chopped onions, sautéed mushrooms, flour, dough, butter, stock, cheese, tomato sauce, and dried mushrooms. Players can hold, store, trade, and reuse them, so the chain behind a dish stays visible.

Food is cooked only at cooking stations. Furnaces, smokers, campfires, and crafting stop making food once cooking stations arrive. Cooking recipes make vanilla foods instead. Simple versions stand in until their full chains exist, such as bread baked from wheat until milling supplies flour. Villager trades and loot keep vanilla food, because bought food represents work done by others.

Processing methods are shared across recipes. One simmering mechanic serves stock, soup, tomato sauce, and braised meat. The ingredient graph should stay smaller than the recipe graph: a few shared intermediates support many dishes.

## Cooking stations and the cookbook

Cooking works like Minecraft's own stations. Players can cook anywhere with the right stations. The cabin kitchen makes it more practical.

### Stations

Each station does one kind of work and matches its inputs, like a furnace or stonecutter. There is no recipe activation or step tracking.

- **Click stations** work like the stonecutter: the cutting board turns an ingredient into its prepared item when the player uses a knife on it.
- **Surface stations** work like the campfire: the skillet holds a few items and cooks each one while heated.
- **Container stations** work like the furnace or brewing stand: the cooking pot and oven take ingredients in slots and cook while heated.
- **Time stations** work at ambient conditions: drying racks, fermentation crocks, and ageing cellars progress over time without heat.

Several stations cook at once, and each keeps its own progress while the player works elsewhere or leaves. Food never burns, spoils, or fails. Unsuitable conditions pause progress, and the station says what it needs.

### Heat from fuel

Fuel sets the heat. Each fuel burns at one heat level, and each heated recipe needs one level. Choosing the fuel is how players control temperature.

| Heat level | Example fuels | Techniques |
|---|---|---|
| Low | Sticks, planks, campfires | Simmering, braising, gentle frying, slow baking |
| Medium | Logs, charcoal, coal | Sautéing, frying, baking |
| High | Coal blocks, blaze rods, lava | Searing, roasting |

A pot or skillet cooks on a lit stove at the stove's fuel heat, or on a campfire at low heat. The station controls heat, so ingredient temperatures are not simulated.

### The cookbook

The [cookbook](../context.md#cookbook) teaches. It lists every recipe from the start with its station, ingredients, heat level, time, and output. Each ingredient that has its own recipe links to it, so a cheeseburger links to its bun, and the bun links to flour. Each technique has a short explanation.

The cookbook shows which recipes the player has ingredients for, counting installed cabin storage. At a container station, a recipe-book button fills the slots from a selected recipe, like the vanilla recipe book.

## Food production areas

### Crops and cultivation

Farming expands with grains, vegetables, alliums, legumes, chilies, herbs, spices, fruit trees, and berries. Crops everywhere have preferences for light, temperature, soil, moisture, and supports such as trellises. Rice needs flooded farmland. Poor conditions slow or pause growth rather than destroy the crop. Some seeds or cuttings require exploring particular biomes.

Mushrooms have their own cultivation through compost beds, inoculated logs, and controlled environments. Some varieties stay wild and must be foraged.

### Butchery

Animals everywhere drop carcasses instead of ordinary meat. A butcher's table splits a carcass into cuts, bones, fat, and trimmings. Cuts include steaks, chops, roasting joints, shoulders, ribs, and stew meat. Cuts matter: a steak suits frying, and a tough shoulder suits slow braising. Further processing includes cubing, mincing, patties, meatballs, sausages, curing, smoking, rendering fat, and stock from bones. Fish are cleaned and filleted into fillets, bones, and trimmings.

### Milling, baking, and dairy

Flour is a central intermediate. Wheat, rye, oats, rice, and other grains mill into flours. Oats can also be rolled. Baking covers mixing, kneading, shaping, yeast, sourdough starters, proofing, and oven baking. Doughs support bread, buns, flutes, pizza, pasta, and pastry.

Dairy covers cream separation, churning butter with buttermilk as a by-product, yogurt, cheese making, and cheese ageing. By-products such as whey and buttermilk feed other recipes.

### Stocks, sauces, and flavour

Stocks are key intermediates: vegetable, chicken, beef, pork, fish, and mushroom. Dashi uses kombu, dried shiitake, and bonito flakes. Stocks feed soups, sauces, stews, risotto, and braises, and can be reduced. Sauce making covers roux, béchamel, tomato sauce, ragù, gravy, emulsions, reductions, and herb sauces.

Cooking teaches the five basic tastes: sweet, sour, salty, bitter, and umami. Chili heat, fat, and aromatics add further dimensions. Following a recipe produces a good dish. A well-balanced dish can earn a small quality bonus.

### Fermentation and preservation

Fermentation is a shared process: yeast and sourdough starters, sauerkraut, kimchi, fermented chili, yogurt and cultured dairy, miso, soy sauce, and vinegar. It needs ingredients, cultures, temperature, and time. Other methods are drying, smoking, curing, pickling, brining, and ageing. Drying preserves and concentrates mushrooms, chilies, herbs, tomatoes, and fruit.

### Fruit, drinks, and cooking wines

Fruit is a substantial cooking ingredient in savoury dishes, baking, desserts, sauces, and preserves. It supplies sweetness and acidity.

Drinks emphasise interesting non-alcoholic preparations, such as æblemost and saftevand. Techniques include pressing, infusion, filtration, syrup making, and concentrates that dilute into several servings.

Wine, port, vermouth, sake, mirin, and Shaoxing wine are cooking ingredients only. They serve braising, deglazing, sauces, and reductions. Alcohol production is outside the focus.

## Food benefits

Cooking gives rewards beyond hunger and saturation.

- Each ingredient contributes a very small amount to one or more benefit families. Candidate families are endurance, resilience, recovery, warmth, perception, and agility. The total stays near six to eight.
- Processing develops those contributions in potency or duration. Each recipe awards bounded improvement for its intended steps. Repeating or adding unnecessary processing gives no further gain.
- Complete dishes combine contributions from several ingredients. A dish has a limited number of dominant effects.
- Only a small number of major food benefits can be active together.
- Complex dishes generally give stronger, longer, or more specialised benefits. Different dishes suit mining, exploring, fighting, or building.
- A dish gives the same benefits whether the player cooked it, bought its parts, or the cabin made it.
- Ingredient contributions stay traceable through intermediates.

## Kitchen automation

The effort of manual cooking motivates automation. Automation follows familiar processes in three broad stages:

| Stage | Examples |
|---|---|
| Pantry staples | Salt, flour, oil, sugar, vinegar |
| Prepared ingredients | Butter, dough, tomato sauce, stocks, dried and fermented products |
| Kitchen production | Butchery, sausages, cheese, bread, pastry |

The stages are not rigid technology tiers. Kitchen automation requires its automation book and upgrade purchase. Commissioning automation for a recipe also consumes a quantity of its finished product and needs installed equipment. No count of personal manual preparations is required, following [PDR-0013](../decisions/pdr/0013-gate-production-through-player-progress.md). Automating some of the work is expected: the complexity of cooking makes it a large gameplay improvement.

Automation uses the same ingredients, stations, heat levels, and times as manual cooking. Equipment capacity and resources limit concurrent production. Stock targets keep pantry quantities, such as 32 salt, 16 flour, and 8 chicken stock. Automation expands upstream over time: once butchery is automated, stock production can draw bones from carcasses. Eventually a player making lasagne finds pasta sheets, minced beef, tomato sauce, stock, and cheese already in storage.

## Equipment and ingredients

| Equipment | Main functions |
|---|---|
| Cutting board | Preparing vegetables, herbs, and ingredients |
| Mixing bowl | Dough, batter, combining ingredients |
| Grain mill | Flour and ground grains |
| Rolling mill | Rolled oats, pasta |
| Skillet | Frying, sautéing, searing |
| Cooking pot | Boiling, simmering, stocks, soups |
| Oven | Baking, roasting, gratinating |
| Dutch oven | Braising, covered slow cooking |
| Butter churn | Butter and buttermilk |
| Cheese press | Cheese |
| Meat grinder | Mince |
| Butcher's table | Carcasses |
| Sausage stuffer | Sausages |
| Drying rack | Herbs, mushrooms, fruit, chilies |
| Fermentation jars and crocks | Starters, pickles, kimchi, sauces |
| Smoker | Meat and fish |
| Oil press | Plant oils |
| Fruit press | Juice |
| Mortar and pestle | Spices and pastes |
| Ageing cellar | Long fermentation and maturation |
| Mushroom beds and logs | Mushroom cultivation |

Ingredient families cover grains, root vegetables, alliums, leafy vegetables, legumes, chilies, mushrooms, orchard fruit, citrus, berries, tropical fruit, herbs, spices, nuts and seeds, animal products, sea ingredients, and basic seasonings such as salt, sugar, vinegar, and oils. Salt comes from seawater evaporation, rock salt deposits, or trade. The [ingredient catalogue](source/ingredients.md) lists candidates.

## Recipe targets

The target catalogue has 147 recipes from French, German, Japanese, Thai, Mexican, South American, and Danish cuisine: 56 main dishes, 56 desserts and snacks, and 35 non-alcoholic drinks. Each has a suggested complexity tier for its full chain from scratch. Tiers do not unlock equipment. The [target recipes](source/recipes.md) list them.

Shared intermediates come before many finished dishes. Flour, stock, cheese, tomato sauce, fermented vegetables, curry paste, and corn masa support a large share of the catalogue. Fourteen early dishes exercise most systems: ratatouille, French onion soup, spätzle, bratwurst with sauerkraut, miso soup, gyoza, pad thai, green curry, quesadillas, tamales, ceviche, feijoada, rugbrød, and tarteletter.

## Player experience

- **Early game:** Players grow basic crops, gather ingredients, and learn simple techniques. Flour, bread, butter, and basic meals take several steps.
- **Mid game:** Players add butchery, stocks, fermentation, and more vegetables. They automate frequently used staples and build a pantry.
- **Late game:** The cabin supports broad farming, processing, preservation, and automated ingredients. Players prepare elaborate dishes from a stocked kitchen and choose meals for their adventures.

A meal that once needed substantial preparation becomes easy because the player built the equipment, ingredients, knowledge, and automation for it.

## Relationship to cabin features

- The [kitchen](../features/B-0041/brief.md) hosts manual cooking equipment.
- The [greenhouse](../features/B-0019/brief.md) grows cooking crops under its own conditions.
- [Livestock rooms](../features/B-0022/brief.md) supply animals for butchery, milk, and eggs.
- [Central storage](../features/B-0004/brief.md) holds the pantry and counts toward recipe availability.
- [Product templates](../features/B-0007/brief.md), [crafting jobs](../features/B-0024/brief.md), [cooking jobs](../features/B-0025/brief.md), and [stock targets](../features/B-0036/brief.md) carry kitchen automation.

## Open questions

- **Packed time:** Passive steps such as fermentation and ageing need catch-up limits while an empty cabin is packed.
- **Farmer's Delight crops:** The greenhouse ships growth profiles for Farmer's Delight crops. They stay until cabin cooking supplies its own crops.
- **Balance:** Benefit families, magnitudes, durations, and active-benefit limits are open.
