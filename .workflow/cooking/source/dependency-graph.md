# Ingredient dependency graph

> Source material from the cooking design conversation. Only formatting has changed. Where this file differs from the [cooking concept](../concept.md), the concept states the current direction. Farmer's Delight references predate the decision to replace it with cabin cooking.

## Core branches

I think we should model the entire cooking system as an **ingredient dependency graph**. Raw ingredients form the roots, processing produces intermediate ingredients, and those combine into increasingly complex dishes.

The interesting part is identifying ingredients that connect multiple branches. Butter, flour, stock, salt, eggs and tomato sauce should be especially important.

I'd start with six ingredient families and then connect them through actual recipes.

### 1. Grains and baking

```text
Wheat
 └─ Milling → Wheat flour
     ├─ + Water → Basic dough
     │   └─ Shaping + Baking → Flatbread
     │
     ├─ + Water + Yeast + Salt → Yeast dough
     │   ├─ Proof + Bake → Bread
     │   ├─ Shape + Proof + Bake → Buns
     │   ├─ Shape + Proof + Bake → Flutes
     │   └─ + Oil + Herbs → Focaccia
     │
     ├─ + Water + Sourdough starter + Salt
     │   └─ Ferment → Sourdough
     │       ├─ Proof + Bake → Sourdough bread
     │       └─ Shape + Bake → Sourdough pizza
     │
     ├─ + Eggs → Pasta dough
     │   ├─ Roll + Cut → Fresh pasta
     │   └─ Roll → Lasagne sheets
     │
     └─ + Butter → Roux
         └─ + Milk → Béchamel

Oats
 ├─ Roll → Rolled oats
 │   ├─ + Milk → Porridge
 │   └─ + Honey + Nuts → Granola
 └─ Mill → Oat flour
     └─ + Butter + Sugar → Oat biscuits

Rye
 └─ Mill → Rye flour
     └─ + Sourdough starter → Rye bread
```

Flour is a major hub. The player could discover milling early, then progressively learn fermentation, proofing, shaping, baking and sauce thickening.

### 2. Vegetables, herbs and spices

This branch should supply ingredients to nearly every other cooking family.

```text
Tomato
 ├─ Chop → Chopped tomatoes
 │   └─ + Onion + Garlic + Oil + Oregano
 │       └─ Sauté + Simmer → Tomato sauce
 │           ├─ + Pasta → Pasta with tomato sauce
 │           ├─ + Minced meat → Meat ragù
 │           └─ + Pizza dough + Cheese → Pizza
 │
 ├─ Reduce → Tomato paste
 └─ Dry → Dried tomatoes

Onion + Carrot + Celery
 └─ Chop → Mirepoix
     ├─ + Water + Herbs → Vegetable stock
     ├─ + Bones + Water → Meat stock
     └─ + Meat + Tomato → Stew base

Garlic
 ├─ Crush → Garlic paste
 ├─ Roast → Roasted garlic
 └─ + Butter + Herbs → Garlic butter

Chili
 ├─ Dry → Dried chili
 │   └─ Grind → Chili powder
 ├─ + Salt → Ferment → Fermented chili
 │   └─ + Vinegar → Hot sauce
 └─ + Garlic + Spices → Chili paste
```

I would also add a few very useful vegetable processes:

| Process | Product | Used for |
|---|---|---|
| Slowly frying onions | Caramelised onions | Burgers, soups, sauces |
| Roasting peppers | Roasted peppers | Salads, soups, sauces |
| Fermenting cabbage | Sauerkraut | Sausages, stews, side dishes |
| Fermenting napa cabbage | Kimchi | Rice dishes, soups |
| Crushing basil, garlic and nuts | Pesto | Pasta, bread, sauces |
| Roasting vegetables | Roasted vegetables | Sides, soup bases |

### 3. Animals, butchery and dairy

This is probably our most interconnected branch.

```text
Cow carcass
 └─ Butcher
     ├─ Steak → Fry / Grill → Cooked steak
     ├─ Roasting joint → Roast → Roast beef
     ├─ Tough cuts → Cubes → Braise → Beef stew
     ├─ Trimmings → Grind → Minced beef
     │   ├─ Shape → Burger patties
     │   ├─ + Egg + Breadcrumbs → Meatballs
     │   └─ + Tomato sauce → Meat ragù
     │
     ├─ Bones → Roast + Vegetables + Water
     │   └─ Simmer → Beef stock
     │       └─ Reduce → Concentrated stock
     │
     └─ Fat → Render → Tallow

Pig carcass
 └─ Butcher
     ├─ Chops → Fry
     ├─ Shoulder → Braise / Slow roast
     ├─ Belly → Cure + Smoke → Bacon
     ├─ Trimmings → Mince
     │   └─ + Fat + Spices + Casing → Sausages
     ├─ Bones → Pork stock
     └─ Fat → Render → Lard

Chicken carcass
 └─ Butcher
     ├─ Breast → Fry / Roast
     ├─ Thighs → Fry / Braise
     ├─ Wings → Roast
     └─ Frame → Chicken stock
```

Dairy connects particularly well with baking and sauces:

```text
Milk
 ├─ Separate → Cream
 │   └─ Churn → Butter + Buttermilk
 │       ├─ Butter → Pastry, sauces, frying
 │       └─ Buttermilk → Pancakes, scones
 │
 ├─ Ferment → Yogurt
 └─ Coagulate → Curds + Whey
     └─ Press / Salt / Age → Cheese
         ├─ Pizza
         ├─ Lasagne
         └─ Cheese sauces
```

The important decision is which cuts to produce from an animal. A player intending to make sausages might butcher differently from one preparing roast beef.

### 4. Mushrooms, sea ingredients and umami

```text
Shiitake mushroom
 └─ Dry → Dried shiitake
     ├─ + Water → Shiitake dashi
     └─ Grind → Mushroom powder
         └─ + Salt → Umami seasoning

Porcini
 └─ Dry → Dried porcini
     ├─ + Stock → Mushroom risotto
     └─ Grind → Mushroom powder

Kelp
 └─ Dry → Kombu
     ├─ + Water → Kombu dashi
     └─ + Bonito flakes → Awase dashi

Fish
 └─ Butcher
     ├─ Fillets → Fry / Roast / Smoke
     └─ Bones + Vegetables → Fish stock

Bonito
 └─ Process + Smoke + Dry → Bonito flakes
     └─ + Kombu → Awase dashi
```

Dashi would introduce players to a type of stock based on extraction from dried ingredients. Different dashi recipes could provide distinct flavours and unlock Japanese dishes.

### 5. Fermentation and preservation

I'd make this a common processing system, shared across several ingredient families.

```text
Flour + Water
 └─ Ferment → Sourdough starter
     └─ Feed → Active starter
         └─ + Flour + Water + Salt → Sourdough

Cabbage + Salt
 └─ Ferment → Sauerkraut

Napa cabbage + Chili + Garlic + Ginger + Salt
 └─ Ferment → Kimchi

Chili + Salt
 └─ Ferment → Chili mash
     └─ Blend + Vinegar → Hot sauce

Milk + Yogurt culture
 └─ Ferment → Yogurt

Soybeans + Koji + Salt
 └─ Ferment + Age → Miso

Soybeans + Wheat + Koji + Salt
 └─ Ferment + Age + Press → Soy sauce
```

Salt becomes another central ingredient:

```text
Seawater → Evaporate ─┐
                      ├→ Salt
Rock salt → Crush ────┘
                       ├→ Seasoning
                       ├→ Dough
                       ├→ Brining
                       ├→ Fermentation
                       └→ Meat curing
```

Fermentation and drying would be especially good candidates for cabin automation, since they involve waiting for the right conditions over time.

### 6. Connecting the branches into elaborate dishes

This is where the graph becomes interesting.

#### Example: Lasagne

- Pasta sheets: Wheat → Flour → Pasta dough → Rolling
- Meat ragù: Carcass → Mince + Tomato sauce + Aromatics → Simmer
- Béchamel: Milk → Butter + Flour → Roux + Milk
- Cheese: Milk → Curds → Pressing / Aging

↓

Layer in baking dish

**Unbaked lasagne**

↓

Bake at the correct oven temperature

Lasagne

This single dish uses farming, milling, butchery, sauce preparation, dairy processing and baking.

It feels like a genuinely advanced recipe, and the intermediate ingredients remain useful independently.

#### Example: Beef stew

```text
Cow → Butcher → Beef cubes ───────────┐
                                      │
Onion + Carrot + Celery → Mirepoix ───┤
                                      ├→ Brown meat
Bones + Mirepoix → Beef stock ────────┤   + Sauté vegetables
                                      │   + Add stock
Mushrooms → Slice ────────────────────┘        │
                                              ↓
                                      Braise at low heat
                                              ↓
                                          Beef stew
```

The player learns that browning meat, preparing stock and slow braising are distinct techniques.

### 7. Finding the most important ingredients

From these initial trees, we can identify high-reuse ingredients.

| Ingredient | Connected branches | Importance |
|---|---|---|
| Flour | Bread, pasta, sauces, pastry | Very high |
| Salt | Cooking, baking, preservation, fermentation | Very high |
| Butter | Baking, sauces, frying, pastry | Very high |
| Eggs | Baking, pasta, sauces, meatballs | High |
| Onion | Stocks, sauces, stews, meat dishes | Very high |
| Garlic | Sauces, seasoning, fermentation | High |
| Tomato sauce | Pasta, pizza, stews, ragù | High |
| Stock | Soups, braising, sauces, risotto | Very high |
| Mushrooms | Stocks, sauces, umami, drying | High |
| Oil | Frying, sauces, baking, preservation | Very high |

This suggests that our earliest kitchen progression should focus on milling, cutting, frying, boiling, simmering and simple oven baking. The advanced processing systems can then reuse everything players learned earlier.

### 8. How this translates into progression

I think we can use five rough levels of recipe complexity.

| Tier | Available techniques | Example recipes |
|---|---|---|
| 1 | Cutting, grinding, mixing, boiling, simple frying | Porridge, flatbread, vegetable soup |
| 2 | Milling, proofing, baking, roasting, simmering | Bread, buns, tomato sauce, chicken stock |
| 3 | Butchery, churning, fermentation, drying, braising | Sausages, sourdough, cheese, beef stew |
| 4 | Reduction, curing, smoking, dough lamination | Bacon, dashi, croissants, complex sauces |
| 5 | Combining several advanced production chains | Lasagne, elaborate roast dinners, complex pastries |

These should be descriptive tiers used by the recipe book. A recipe becomes available when the player has its ingredients and the necessary equipment.

#### 9. One structural choice I'd make now

I'd represent this as a directed graph with three kinds of nodes:

- **Ingredients:** Wheat, flour, butter, stock, dough, minced meat.
- **Processes:** Milling, grinding, churning, fermenting, drying, simmering.
- **Dishes:** Bread, sausages, pasta, stew, lasagne.

A process connects its input ingredients to its outputs, with equipment, temperature and duration attached to the process.

The same process can appear in hundreds of recipes. For example, simmering stock and simmering tomato sauce use the same underlying mechanic with different temperatures, durations and ingredients.

We can then evaluate the graph to find ingredients with too few uses, recipes that require excessive processing, rare resources that block large parts of cooking, and production chains suitable for automation.

**I think the next design work should concentrate on breadth before adding more complexity.** We have strong branches for baking, meat and sauces. I'd expand the vegetable, mushroom, fish, fruit and preservation branches until they offer a similarly rich range of complete dishes.

That will give us a much stronger basis for deciding which crops, equipment and processing mechanics are actually worth implementing.

## Expanded branches

I'd expand five areas: vegetables, mushrooms, fish and seafood, fruit, and preservation.

The goal is to make each ingredient useful in several recipes, with increasingly complex dishes combining products from different branches.

For now, these are **proposed recipes and ingredients for our mod**. We'll reuse the existing Farmer's Delight content where possible. It already includes vegetable soup, ratatouille, mushroom rice, fish stew, grilled salmon and several other dishes we can expand into more complete cooking processes.

### 1. Vegetables — building flavours

Vegetables should have a much bigger role than simply being added to soups. Different preparation methods can fundamentally change how an ingredient is used.

#### Important intermediate products

```text
Onion
 ├─ Chop → Raw onion preparation
 ├─ Slowly sauté → Caramelised onions
 └─ + Carrot + Celery → Mirepoix
                         ├─ Vegetable stock
                         ├─ Chicken stock
                         ├─ Beef stock
                         └─ Stews and braises

Garlic
 ├─ Crush → Garlic paste
 ├─ Roast → Roasted garlic
 └─ + Oil + Herbs → Flavoured oil

Tomatoes
 ├─ Crush → Tomato purée
 │    └─ + Onion + Garlic + Herbs
 │         └─ Simmer → Tomato sauce
 ├─ Reduce → Tomato paste
 └─ Dry → Dried tomatoes

Peppers
 ├─ Roast → Roasted peppers
 ├─ Dry + Grind → Paprika
 └─ + Chili + Garlic → Pepper paste

Beans / Lentils
 ├─ Soak + Boil → Cooked legumes
 └─ Grind → Bean purée
```

Mirepoix is particularly important. It connects vegetables with our entire stock, soup and braising systems.

#### Vegetable dishes

| Dish | Ingredients / intermediates | New techniques | Tier |
|---|---|---|---|
| Mashed potatoes | Potatoes, butter, milk | Boil, mash | 1 |
| Vegetable soup | Mixed vegetables, water or stock | Chop, simmer | 1 |
| Roasted root vegetables | Carrot, parsnip, beetroot, oil, thyme | Roast | 2 |
| Potato and leek soup | Leeks, potatoes, vegetable stock, cream | Sweat, simmer, blend | 2 |
| Ratatouille | Tomato, courgette, aubergine, peppers, herbs | Sauté, simmer | 3 |
| French onion soup | Caramelised onions, beef stock, bread, cheese | Caramelise, simmer, gratinate | 3 |
| Minestrone | Beans, vegetables, tomato, stock, pasta | Soak, boil, simmer | 3 |
| Shakshuka | Tomatoes, peppers, onion, spices, eggs | Sauté, reduce, gently poach | 3 |
| Cabbage rolls | Cabbage, minced meat, rice, tomato sauce | Blanch, fill, simmer | 3 |
| Hummus | Chickpeas, tahini, garlic, lemon, oil | Soak, boil, grind | 3 |
| Borscht | Beetroot, cabbage, stock, sour cream | Sauté, simmer | 4 |
| Vegetable curry | Vegetables, spice paste, coconut milk | Grind spices, sauté, simmer | 4 |

I like the educational opportunities here. French onion soup teaches caramelisation, shakshuka teaches gentle egg cooking, and minestrone introduces dried beans that must be soaked and boiled.

A player who understands these techniques can use them again in more advanced dishes.

#### Farming progression

Vegetables could also introduce new agricultural equipment.

| Farming method | Suitable crops | Mechanic |
|---|---|---|
| Ordinary farmland | Carrots, onions, potatoes | Light, moisture, soil |
| Trellises | Tomatoes, peas, beans | Physical support |
| Warm greenhouse | Peppers, chilies, aubergines | Temperature regulation |
| Cool growing beds | Cabbage, leek, broccoli | Lower temperature preference |
| Flooded beds | Rice | Standing water |
| Deep, loose soil | Parsnips, root vegetables | Soil preparation |

I'd make unsuitable conditions slow or pause growth. We already agreed on a forgiving system for cooking; using the same philosophy for farming seems sensible.

### 2. Mushrooms — cultivation, foraging and umami

Mushrooms deserve a distinct progression because they can be grown on logs or compost, foraged in forests, dried, rehydrated and turned into flavour concentrates.

#### Dependency graph

```text
Button / Brown mushroom
 ├─ Sauté → Fried mushrooms
 └─ + Cream + Stock → Mushroom soup

Oyster mushroom
 ├─ Fry → Crispy oyster mushrooms
 └─ + Garlic + Herbs → Mushroom dish

Shiitake
 ├─ Sauté → Stir-fried shiitake
 └─ Dry → Dried shiitake
           ├─ Rehydrate → Shiitake dashi
           └─ Grind → Mushroom powder

Porcini
 └─ Dry → Dried porcini
           ├─ + Stock + Rice → Mushroom risotto
           └─ + Cream → Porcini sauce

Chanterelle
 └─ Sauté + Cream + Herbs → Chanterelle sauce

Mixed mushrooms
 └─ Chop finely + Shallot + Butter
      └─ Cook down → Duxelles
                      ├─ Stuffed pastries
                      └─ Beef Wellington
```

Duxelles is particularly interesting because it's a real preparation of finely chopped mushrooms cooked with aromatics until much of the moisture has evaporated.

#### Mushroom dishes

| Dish | Intermediate products | Techniques | Tier |
|---|---|---|---|
| Sautéed mushrooms | Mushrooms, butter, garlic | Sauté | 1 |
| Cream of mushroom soup | Mushrooms, stock, cream | Sauté, simmer, blend | 2 |
| Mushroom toast | Sautéed mushrooms, bread | Sauté, toast | 2 |
| Mushroom gravy | Mushrooms, stock, roux | Sauté, thicken | 3 |
| Shiitake dashi | Dried shiitake | Dry, soak, extract | 3 |
| Mushroom risotto | Mushroom stock, rice, cheese, mushrooms | Toast rice, gradually add stock | 4 |
| Mushroom ravioli | Pasta dough, mushroom filling | Chop, fill, boil | 4 |
| Porcini cream sauce | Dried porcini, cream, stock | Rehydrate, reduce | 4 |
| Beef Wellington | Beef joint, duxelles, puff pastry | Sear, reduce, wrap, bake | 5 |

Foraging should remain valuable even after mushroom cultivation becomes available. Porcini, chanterelles and morels could be wild-only varieties.

I'd also make cultivation depend on the substrate. Shiitake uses hardwood logs, oyster mushrooms use logs or suitable growing blocks, and button mushrooms use compost beds.

### 3. Fish and seafood — a second butchery branch

Fish should have processing just like land animals.

A caught fish can be cleaned and filleted, producing edible meat and useful leftovers.

#### Dependency graph

```text
Whole fish
 └─ Clean / Butcher
     ├─ Fillets
     │   ├─ Fry
     │   ├─ Poach
     │   ├─ Roast
     │   ├─ Salt → Cure / Dry
     │   └─ Brine + Smoke → Smoked fish
     │
     ├─ Trimmings
     │   └─ Grind + Potato + Egg → Fish cakes
     │
     └─ Bones and head
         └─ + Vegetables + Herbs
             └─ Simmer → Fish stock
                          ├─ Fish soup
                          ├─ Seafood risotto
                          └─ Fish sauce base
```

Specific species can then extend this.

```text
Herring
 └─ Clean + Salt + Vinegar + Spices
      └─ Pickle → Pickled herring

Anchovies
 └─ Salt + Age
      └─ Preserved anchovies
          ├─ Pizza toppings
          └─ Dressings and sauces

Bonito
 └─ Cook + Smoke + Dry + Shave
      └─ Bonito flakes
          └─ + Kombu → Awase dashi

Shrimp
 └─ Peel
     ├─ Meat → Cooking
     └─ Shells → Seafood stock
```

#### Fish and seafood dishes

| Dish | Intermediate products | Techniques | Tier |
|---|---|---|---|
| Pan-fried cod | Cod fillet, butter | Fry | 1 |
| Grilled salmon | Salmon fillet, herbs | Grill | 2 |
| Poached fish | Fish fillet, aromatics, liquid | Poach | 2 |
| Fish cakes | Fish, mashed potato, egg, herbs | Mince, shape, fry | 3 |
| Smoked salmon | Salmon, salt brine | Brine, smoke | 3 |
| Pickled herring | Herring, vinegar, salt, spices | Salt, pickle | 3 |
| Fish pie | Fish, white sauce, mashed potato | Poach, thicken, bake | 4 |
| Seafood risotto | Fish stock, rice, seafood | Simmer, gradually add stock | 4 |
| Bouillabaisse | Fish, shellfish, fish stock, fennel, tomato, saffron | Prepare stock, simmer, assemble | 5 |
| Miso fish soup | Dashi, miso, fish, vegetables | Extract, ferment ingredients, gently heat | 5 |

Fish introduces some techniques we haven't covered much yet, particularly **poaching, brining and curing**.

I would add an optional fishing progression through species found in different waters and biomes. We could reuse Minecraft's cod and salmon early, while introducing additional species for more advanced recipes.

### 4. Fruit — baking, preserves, acidity and sweetness

Fruit should become much more than something players eat directly.

It connects orchards, wild foraging, baking, fermentation, preserving and even savoury cooking.

#### Dependency graph

```text
Apple
 ├─ Chop + Cook → Apple compote
 │                 ├─ Apple pie filling
 │                 └─ Pancake topping
 │
 ├─ + Sugar + Cinnamon → Apple pie filling
 │                        ├─ Apple pie
 │                        └─ Apple crumble
 │
 ├─ Press → Apple juice
 │           └─ Ferment → Apple cider
 │                         └─ Vinegar fermentation
 │                             → Apple cider vinegar
 │
 └─ Dry → Dried apple

Berries
 ├─ + Sugar + Lemon → Cook down → Jam
 │                                ├─ Jam tarts
 │                                ├─ Filled buns
 │                                └─ Layer cakes
 ├─ + Sugar → Berry syrup
 └─ Dry → Dried berries

Lemon
 ├─ Squeeze → Lemon juice
 │            ├─ Salad dressings
 │            ├─ Lemon butter sauce
 │            └─ Lemon curd
 └─ Zest → Lemon zest
           └─ Cakes and pastries

Grapes
 ├─ Dry → Raisins
 └─ Press → Grape juice
```

I'd also use pears, plums, cherries, peaches, oranges, limes and rhubarb.

Citrus is especially useful because it introduces sourness to a wide range of savoury dishes.

#### Fruit dishes

| Dish | Intermediate products | Techniques | Tier |
|---|---|---|---|
| Apple compote | Apples, sugar, cinnamon | Simmer | 1 |
| Berry jam | Berries, sugar, lemon | Reduce | 2 |
| Apple crumble | Cooked apples, flour, butter, sugar | Mix, bake | 2 |
| Fruit pancakes | Pancake batter, fresh fruit, syrup | Fry, assemble | 2 |
| Apple pie | Pastry dough, apple filling | Make pastry, bake | 3 |
| Lemon curd | Lemon juice, eggs, butter, sugar | Gentle heating, thicken | 3 |
| Orange marmalade | Oranges, peel, sugar | Boil, reduce | 3 |
| Fruit tart | Pastry shell, custard, fruit | Bake, thicken, assemble | 4 |
| Cinnamon and raisin bread | Enriched dough, raisins, cinnamon | Knead, proof, bake | 4 |
| Apple chutney | Apple, onion, vinegar, sugar, ginger | Chop, simmer, preserve | 4 |
| Black Forest cake | Chocolate sponge, cherries, cream | Bake, whip, layer | 5 |
| Danish pastries | Laminated dough, custard, fruit preserves | Laminate, proof, bake | 5 |

Apple chutney introduces something interesting: fruit can be used to balance savoury dishes.

We could eventually pair it with pork, cheese or cold cuts.

#### Fruit cultivation

I'd distinguish between three ways of obtaining fruit.

- **Orchards:** Apples, pears, cherries, plums and peaches. Trees take longer to establish but provide recurring harvests.
- **Berry bushes:** Strawberries, raspberries, blueberries and blackberries, with different preferences for soil and moisture.
- **Warm-climate cultivation:** Citrus, grapes and tropical fruits, requiring exploration or controlled greenhouse conditions.

Fruit trees could be one of the longer-term investments in the cabin's garden.

### 5. Preservation — making other systems more useful

I think preservation should be one of the core processing mechanics, with several distinct methods.

| Process | Examples | What it teaches |
|---|---|---|
| Drying | Mushrooms, herbs, fruit, chili | Removing moisture and concentrating flavour |
| Salting | Fish, meat | Salt-based preservation |
| Brining | Meat, vegetables | Preparing food in salted liquid |
| Vinegar pickling | Onion, cucumber, beetroot | Preserving with acidity |
| Lactic fermentation | Sauerkraut, kimchi, chili | Microbial fermentation producing acidity |
| Smoking | Fish, bacon, sausages | Smoke flavour and preservation |
| Curing | Bacon, ham | Salt, time and controlled conditions |
| Sugar preserving | Jam, marmalade | Concentration and high sugar content |
| Cultured dairy | Yogurt, cheese | Cultures transforming milk |
| Long maturation | Miso, soy sauce | Fermentation and ageing |

I'd add a cellar or pantry as an upgrade for storing and processing preserved ingredients. Some cultures and cured products could benefit from controlled temperature.

#### Preservation dependencies

```text
Cucumber + Salt + Water
 └─ Fermentation → Fermented cucumber

Cucumber + Vinegar + Sugar + Spices
 └─ Pickling → Pickled cucumber

Cabbage + Salt
 └─ Fermentation → Sauerkraut

Chili + Garlic + Salt
 └─ Fermentation → Chili mash
                    └─ + Vinegar → Hot sauce

Pork belly + Salt + Sugar
 └─ Cure → Cured pork belly
           └─ Smoke → Bacon

Salmon + Salt
 └─ Cure → Cured salmon
           └─ Optional smoking → Smoked salmon

Soybeans + Koji + Salt
 └─ Fermentation + Aging → Miso
```

The difference between fermented and vinegar-pickled cucumbers is worth preserving in the game. They involve different techniques and have different flavour profiles.

Since we don't want mandatory food spoilage, I'd make preservation useful through new recipes, compact long-term storage, useful food effects and trade value.

Preserved ingredients should also open new cooking possibilities. Dried mushrooms can make stronger stock, while fresh mushrooms work better for sautéing.

### 6. Connecting all five branches

This is where we should get some really interesting advanced dishes.

Here are four examples that use ingredients from several independent branches.

**Mushroom and leek pie — Tier 4**

Vegetables + Mushrooms + Dairy + Baking

- Leeks → Sauté
- Mushrooms → Sauté
- Butter + Flour + Milk → White sauce
- Flour + Butter → Pastry

↓

Combine filling, cover with pastry, bake

**Mushroom and leek pie**

**Fish with lemon butter sauce — Tier 4**

Fishing + Dairy + Fruit + Sauces

- Fish → Fillet → Fry
- Fish bones → Fish stock
- Lemon → Juice
- Cream + Butter → Sauce ingredients

↓

Reduce stock, finish sauce, serve

**Fish with lemon butter sauce**

**Ramen — Tier 5**

Stock + Fermentation + Vegetables + Noodles

- Pork / Chicken bones → Broth
- Flour → Noodles
- Soybeans → Miso
- Eggs → Marinated eggs
- Pork → Braised pork
- Vegetables → Prepared toppings

↓

Prepare and combine components

**Ramen**

**Roast pork with apple chutney — Tier 4**

Butchery + Fruit + Preservation + Roasting

- Pig → Butcher → Pork joint
- Apple + Onion + Vinegar → Chutney
- Potato + Herbs → Roasted potatoes
- Pork drippings + Stock → Gravy

↓

Roast, reduce, assemble

**Roast pork dinner**

### 7. What ingredients emerge as the most useful?

Looking across the expanded graph, a few ingredients and intermediates deserve priority because they connect so many dishes.

| Ingredient | Important downstream uses |
|---|---|
| Onion | Mirepoix, sauces, soups, stocks, chutneys, stews |
| Garlic | Sauces, mushroom dishes, curries, fermentation |
| Celery | Stocks, soups, braises, fish dishes |
| Tomato | Sauces, stews, soups, curries |
| Mushrooms | Sauces, dashi, risotto, pies |
| Lemon | Fish sauces, baking, jam, dressings |
| Flour | Baking, pasta, pastry, thickening |
| Butter | Baking, frying, sauces |
| Salt | Seasoning, bread, curing, fermentation |
| Stock | Soups, sauces, risotto, braises |
| Vinegar | Pickling, sauces, chutneys, dressings |
| Chili | Curries, spice mixes, fermented sauces |
| Eggs | Baking, pasta, mayonnaise, prepared dishes |
| Cream | Mushroom sauces, soups, pastries |

I think we should give ingredients with many downstream uses priority over exotic ingredients that only appear in one recipe.

However, rarer ingredients like saffron, vanilla, chanterelles and cardamom are valuable as exploration rewards.

### 8. A first progression for the expanded kitchen

The dependency graph suggests a fairly natural progression of equipment and techniques.

| Tier | Equipment and capabilities |
|---|---|
| 1 | Cutting board, basic pot, skillet, hand mill |
| 2 | Oven, mixing bowl, drying rack, fruit press |
| 3 | Butter churn, meat grinder, fermentation jars, mushroom cultivation |
| 4 | Smokehouse, cheese press, temperature-controlled fermentation, pasta equipment |
| 5 | Advanced oven, ageing cellar, specialised processing and multi-stage automation |

I'd avoid requiring a tier-5 workstation simply because a recipe is labelled tier 5. The complexity of advanced dishes should primarily come from their many dependencies.

A large kitchen with several workstations can also be an important part of the cabin's upgrade progression.

### 9. Keep the ingredient graph smaller than the recipe graph

One design rule I'd establish is to distinguish **stored intermediate products** from **temporary preparation states**.

For example:

- Store flour, butter, stock, dried mushrooms, tomato sauce, pastry and fermented vegetables as inventory items.
- Handle chopped onions, sliced carrots, seasoned meat and kneaded dough as states inside an active recipe, unless another recipe needs that intermediate separately.

This should prevent us from ending up with hundreds of nearly identical inventory items.

The recipe engine can still track every preparation step. If a player needs to chop carrots and sauté them, those actions must happen in the correct order and temperature range. Incorrect actions simply have no effect on the recipe or consume nothing.

For automation, the same recipe graph can be executed by the cabin, using its available equipment.

### 10. Scope after this expansion

We now have a useful foundation for a substantial cooking system:

- Around 50 candidate dishes across these five expanded branches.
- Several dozen reusable raw ingredients.
- Shared intermediates such as stock, sauces, pastry, dried ingredients, preserves and spice preparations.
- Meaningful growing requirements for crops and mushrooms.
- Distinct cooking techniques, including simmering, poaching, braising, frying, roasting, fermentation and drying.
- Advanced recipes that combine several production chains.

I think this is enough breadth to start designing the underlying recipe system without immediately needing hundreds of dishes.

The most important next piece would be a **formal recipe model**: how recipes specify ingredients, allowed substitutions, ordered preparation steps, equipment, temperature ranges, duration, outputs and automation requirements.

That model should be expressive enough to represent both a simple boiled potato and an elaborate ramen without requiring special-purpose code for every dish.
