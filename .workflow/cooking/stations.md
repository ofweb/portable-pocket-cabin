# Cooking stations

This document lists every process the [recipe catalogue](recipe-catalogue.md) and [intermediate products](intermediates.md) use, and the stations that support them. Step counts come from both documents: about 860 steps in all. Decisions are recorded below. The remaining recommendations are proposals until they are agreed.

Station kinds work like vanilla blocks:

- **Click:** like a stonecutter. A tool or a hand acts on one item.
- **Surface:** like a campfire. Items sit on the station and cook one by one.
- **Container:** like a furnace. Several inputs in slots and a progress arrow.
- **Time:** progresses over time without heat.
- **Heat source:** heats the station above it.
- **Work surface:** holds the stations placed on it, the way a stove holds a pot.
- **Counter-top:** sits on a prep counter. Ingredients are placed into it, then a tool acts on them.

## Stations in use

### Heat sources

| Station | Kind | Heat | Supports | Feature |
|---|---|---|---|---|
| Stove | Heat source | Fuel sets low, medium, or high | Heats the pot, skillet, wok, and æbleskive pan above it | B-0045 |
| Campfire | Heat source | Low | Heats a pot or skillet above it, with no fuel | B-0045 |

### The prep counter

| Station | Kind | Steps | Supports | Feature |
|---|---|---|---|---|
| Prep counter | Work surface | 90 | Holds the cutting board, mixing bowl, and mortar and pestle. Takes shaping, rolling, filling, wrapping, skewering, layering, and assembling a dish from its components. | None yet |

### Cooking with heat

| Station | Kind | Steps | Heat used | Techniques | Feature |
|---|---|---|---|---|---|
| Cooking pot | Container on a heat source | 189 | Low 134, medium 15, high 27, high with oil 13 | Simmer, boil, steam, braise, poach, steep, infuse, reduce, render, scald, blanch, curdle, nixtamalize, deep-fry | B-0045 |
| Skillet | Surface on a heat source | 57 | Low 9, medium 37, high 11 | Fry, sauté, sear, toast, griddle, caramelise, char, brown | B-0045 |
| Oven | Container with its own fuel | 52 | Low 11, medium 27, high 14 | Bake, roast, gratinate, dry, burn logs to ash, bake in moulds | B-0045 |
| Grill | Surface with its own fuel | 6 | High | Grill, char | None yet |
| Smoker | Container with its own fuel | 6 | Low | Smoke | B-0048, as the vanilla smoker |
| Wok | Container on a heat source | 4 | High | Stir-fry | None yet |
| Æbleskive pan | Surface on a heat source | 3 | Medium | Cook in round wells, turning | None yet |

### Preparing without heat

| Station | Kind | Steps | Techniques | Feature |
|---|---|---|---|---|
| Mixing bowl | Counter-top, with a spoon | 120 | Mix, knead, whisk, beat, fold, temper, marinate, season, dip, strain, settle starch, leach lye, dilute, sweeten | B-0046 |
| Cutting board | Counter-top, with a knife | 65 | Chop, slice, dice, grate, shred, bruise, quarter, peel, shave | B-0045 |
| Mill | Container | 54 | Grind, crush, wet-grind, crack, blend, purée, crumble, stone-grind | B-0046 |
| Fruit press | Container | 20 | Press juice and oil | B-0052 |
| Mortar and pestle | Counter-top, with a pestle | 13 | Pound pastes, mash, grind coarsely | B-0047 |
| Butcher block | Click | 9 | Butcher, scale, gut, fillet | B-0049 |
| Cheese press | Container | 6 | Press curds | B-0046 |
| Sausage stuffer | Container | 5 | Stuff casings | B-0049 |
| Meat grinder | Container | 4 | Grind meat and fish | B-0049 |
| Hanging hook | Click | 4 | Skin, scrape, pluck | B-0049 |
| Butter churn | Container | 1 | Churn | B-0046 |

### Time

| Station | Steps | Techniques | Feature |
|---|---|---|---|
| Drying rack | 61 | Dry, sun-dry, wither | B-0048 |
| Fermentation crock | 32 | Ferment, culture, sour, soak, sprout, steep, infuse | B-0048 |
| Ageing shelf | 29 | Age, ripen, cure, oxidise, rest, set, settle, chill, cool | B-0048 |
| Brine barrel | 12 | Salt, brine, cure, marinate | B-0048 |
| Proofing basket | 10 | Proof | B-0046 |
| Pickling jar | 4 | Pickle | B-0048 |
| Dairy pan | 2 | Settle milk, skim cream | B-0046 |

### Tools

The knife, a spoon for the mixing bowl, a pestle, a cleaver, glass bottles for skimming, skewers, a bamboo rolling mat, and moulds for madeleines, canelés, and taiyaki. Moulds go in the oven.

## Decisions

- **Prep counter:** a work surface that holds counter-top stations. The mixing bowl sits on the counter, ingredients are placed in it, and a spoon mixes them. The cutting board and the mortar and pestle sit on it the same way. The counter takes every step that combines several items, and assembling finished dishes.
- **The cutting board keeps single-item cuts.** Shaping, rolling, filling, and skewering moved to the counter.
- **Charcoal and coal give the same heat,** medium under the current fuel tags.
- **The meat grinder and the mill are separate stations.**
- **Moulds go in the oven** with the batter. Taiyaki bakes in a fish mould instead of cooking on the skillet.

## What the counts show

- **The cooking pot and mixing bowl do the most work.** Together they cover about 310 of the 860 steps. The pot's range is realistic, since real kitchens do most cooking in a pot. The mixing bowl is overloaded: it mixes and kneads, but it also strains, settles starch, leaches lye, dilutes drinks, and marinates.
- **Some stations are barely used.** The pickling jar (4 steps) overlaps the fermentation crock. The butter churn (1) and dairy pan (2) are small but agreed.
- **Four stations have no owning feature:** the prep counter, grill, wok, and æbleskive pan.

## Proposals

| Change | Why |
|---|---|
| Add a **strainer**, a counter-top station, for straining, draining, sieving, settling starch, and leaching lye | It takes about 15 steps the mixing bowl does not fit, and straining is a technique worth teaching on its own. |
| Merge the **pickling jar** into the fermentation crock | Pickling in vinegar and fermenting both take time in a sealed container. One crock covers both with fewer stations. |
| Rename the ageing shelf to **cellar shelf** | Its steps are cool storage: ageing, ripening, resting, setting, and chilling. "Cellar" says why it chills. |
| Rename the fruit press to **press** | It presses oil from olives and seeds as well as juice from fruit. The cheese press stays separate. |
| Give the **grill** its own fuel slot, like the oven | Grilling happens over its own fire, not on a stove. |
| Add a new backlog item for the **prep counter, grill, wok, and æbleskive pan**, with oil filling for the pot | These stations serve many dishes across cuisines, and no current feature owns them. |
