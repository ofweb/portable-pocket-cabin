# Cooking stations

This document lists every process the [recipe catalogue](recipe-catalogue.md) and [intermediate products](intermediates.md) use, and the stations that support them. Step counts come from both documents: about 890 steps in all. Decisions are recorded below. The remaining recommendations are proposals until they are agreed.

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
| Prep counter | Work surface | 90 | Holds the cutting board, mixing bowl, strainer, and mortar and pestle. Takes shaping, rolling, filling, wrapping, skewering, layering, and assembling a dish from its components. | B-0046 |

### Cooking with heat

| Station | Kind | Steps | Heat used | Techniques | Feature |
|---|---|---|---|---|---|
| Cooking pot | Container on a heat source | 190 | Low 135, medium 15, high 27, high with oil 13 | Simmer, boil, steam, braise, poach, steep, infuse, reduce, render, scald, blanch, curdle, nixtamalize, deep-fry | B-0045 |
| Skillet | Surface on a heat source | 57 | Low 9, medium 37, high 11 | Fry, sauté, sear, toast, griddle, caramelise, char, brown | B-0045 |
| Oven | Container with its own fuel | 52 | Low 11, medium 27, high 14 | Bake, roast, gratinate, dry, burn logs to ash, bake in moulds | B-0045 |
| Grill | Surface with its own fuel slot | 6 | Fuel sets the heat, as in the oven | Grill, char | B-0053 |
| Smoker | Container with its own fuel | 6 | Low | Smoke | B-0048, as the vanilla smoker |
| Wok | Container on a heat source | 4 | High | Stir-fry | B-0053 |
| Æbleskive pan | Surface on a heat source | 3 | Medium | Cook in round wells, turning | B-0053 |

### Preparing without heat

| Station | Kind | Steps | Techniques | Feature |
|---|---|---|---|---|
| Mixing bowl | Counter-top, with a spoon | 107 | Mix, knead, whisk, beat, fold, temper, marinate, season, dip, dilute, sweeten | B-0046 |
| Strainer | Counter-top | 39 | Strain, drain, sieve, settle starch, leach lye | B-0053 |
| Cutting board | Counter-top, with a knife | 65 | Chop, slice, dice, grate, shred, bruise, quarter, peel, shave | B-0045 |
| Mill | Container | 54 | Grind, crush, wet-grind, crack, blend, purée, crumble, stone-grind | B-0046 |
| Press | Container | 20 | Press juice and oil | B-0052 |
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
| Fermentation crock | 36 | Ferment, culture, sour, pickle, soak, sprout, steep, infuse | B-0046, used by B-0048 |
| Ageing shelf | 31 | Age, ripen, cure, oxidise, rest, set, settle, chill, cool | B-0046, used by B-0048 |
| Brine barrel | 12 | Salt, brine, cure, marinate | B-0048 |
| Proofing basket | 10 | Proof | B-0046 |
| Dairy pan | 2 | Settle milk, skim cream | B-0046 |

### Tools

The knife, a spoon for the mixing bowl, a pestle, a cleaver, glass bottles for skimming, skewers, a bamboo rolling mat, and moulds for madeleines, canelés, and taiyaki. Moulds go in the oven.

## Decisions

- **Prep counter:** a work surface that holds counter-top stations. The mixing bowl sits on the counter, ingredients are placed in it, and a spoon mixes them. The cutting board and the mortar and pestle sit on it the same way. The counter takes every step that combines several items, and assembling finished dishes.
- **The cutting board keeps single-item cuts.** Shaping, rolling, filling, and skewering moved to the counter.
- **Charcoal and coal give the same heat,** medium under the current fuel tags.
- **The meat grinder and the mill are separate stations.**
- **Moulds go in the oven** with the batter. Taiyaki bakes in a fish mould instead of cooking on the skillet.
- **The strainer sits on the counter.** It takes straining, draining, sieving, settling starch, and leaching lye from the mixing bowl and the cooking pot.
- **Pickling happens in the fermentation crock.** The pickling jar is merged into it.
- **The grill has its own fuel slot.** Fuel sets its heat, as in the oven.
- **The fruit press is renamed the press,** because it presses oil as well as juice. The cheese press keeps its name.
- **B-0046 brings forward the prep counter,** with the fermentation crock and ageing shelf, because milling, baking, and dairy need them. B-0053 owns the strainer, grill, wok, and æbleskive pan, and oil filling for deep-frying in the cooking pot.

## What the counts show

- **The cooking pot does the most work,** with 190 of the 890 steps. That is realistic, since real kitchens do most cooking in a pot.
- **The counter takes the hand work.** The prep counter, cutting board, mixing bowl, strainer, and mortar and pestle together cover about 310 steps.
- **Small stations are agreed:** the butter churn (1 step) and dairy pan (2) teach real dairy steps.
- **B-0053 owns four of the stations that had no feature:** the strainer, grill, wok, and æbleskive pan. B-0046 owns the prep counter.

## Proposals

| Change | Why |
|---|---|
| Rename the ageing shelf to **cellar shelf** | Its steps are cool storage: ageing, ripening, resting, setting, and chilling. "Cellar" says why it chills. Undecided. |
