# Cooking stations

This document lists every process the [recipe catalogue](recipe-catalogue.md) and [intermediate products](intermediates.md) use, and the stations that support them. Step counts come from both documents: about 850 steps in all. Recommendations are proposals until they are agreed.

Station kinds work like vanilla blocks:

- **Click:** like a stonecutter. A tool or a hand acts on one item.
- **Surface:** like a campfire. Items sit on the station and cook one by one.
- **Container:** like a furnace. Several inputs in slots and a progress arrow.
- **Time:** progresses over time without heat.
- **Heat source:** heats the station above it.

## Stations in use

### Heat sources

| Station | Kind | Heat | Supports | Feature |
|---|---|---|---|---|
| Stove | Heat source | Fuel sets low, medium, or high | Heats the pot, skillet, wok, and æbleskive pan above it | B-0045 |
| Campfire | Heat source | Low | Heats a pot or skillet above it, with no fuel | B-0045 |

### Cooking with heat

| Station | Kind | Steps | Heat used | Techniques | Feature |
|---|---|---|---|---|---|
| Cooking pot | Container on a heat source | 189 | Low 134, medium 15, high 27, high with oil 13 | Simmer, boil, steam, braise, poach, steep, infuse, reduce, render, scald, blanch, curdle, nixtamalize, deep-fry | B-0045 |
| Skillet | Surface on a heat source | 58 | Low 9, medium 38, high 11 | Fry, sauté, sear, toast, griddle, caramelise, char, brown | B-0045 |
| Oven | Container with its own fuel | 51 | Low 11, medium 26, high 14 | Bake, roast, gratinate, dry, burn logs to ash | B-0045 |
| Grill | Surface with its own fuel | 6 | High | Grill, char | None yet |
| Smoker | Container with its own fuel | 6 | Low | Smoke | B-0048, as the vanilla smoker |
| Wok | Container on a heat source | 4 | High | Stir-fry | None yet |
| Æbleskive pan | Surface on a heat source | 3 | Medium | Cook in round wells, turning | None yet |

### Preparing without heat

| Station | Kind | Steps | Techniques | Feature |
|---|---|---|---|---|
| Mixing bowl | Container | 120 | Mix, knead, whisk, beat, fold, temper, marinate, season, dip, strain, settle starch, leach lye, dilute, sweeten | B-0046 |
| Cutting board | Click | 104 | Chop, slice, dice, grate, shred, bruise, quarter, peel, plus shaping, rolling, filling, and skewering | B-0045 |
| Mill | Container | 54 | Grind, crush, wet-grind, crack, blend, purée, crumble, stone-grind | B-0046 |
| Fruit press | Container | 20 | Press juice and oil | B-0052 |
| Mortar and pestle | Click | 13 | Pound pastes, mash, grind coarsely | B-0047 |
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

### Serving

| Station | Steps | Techniques | Feature |
|---|---|---|---|
| Assemble | 44 | Plate, top, layer, fill, sandwich, wrap, roll, serve | Open |

### Tools

The knife, a cleaver, glass bottles for skimming, skewers, a bamboo rolling mat, and moulds for madeleines, canelés, and taiyaki.

## What the counts show

- **The cooking pot and mixing bowl do half the work.** Together they cover about 300 of the 850 steps. The pot's range is realistic, since real kitchens do most cooking in a pot. The mixing bowl is overloaded: it mixes and kneads, but it also strains, settles starch, leaches lye, dilutes drinks, and marinates.
- **The cutting board does two different jobs.** Cutting a single ingredient is a click action. Shaping, filling, rolling, and skewering combine several items, which a click station cannot hold.
- **Assembly is common and has no station.** 44 dishes end with plating or layering several components.
- **Some stations are barely used.** The pickling jar (4 steps) overlaps the fermentation crock. The butter churn (1) and dairy pan (2) are small but agreed.
- **Four stations have no owning feature:** the grill, wok, æbleskive pan, and whatever handles assembly.

## Recommendations

| Change | Why |
|---|---|
| Add a **prep counter**, a container station for shaping, filling, rolling, wrapping, skewering, layering, and plating | It takes the multi-item work from the cutting board and gives the 44 assembly steps a home. The cutting board keeps single-item cuts. |
| Add a **strainer** for straining, draining, sieving, settling starch, and leaching lye | It takes about 15 steps the mixing bowl does not fit, and straining is a technique worth teaching on its own. |
| Merge the **pickling jar** into the fermentation crock | Pickling in vinegar and fermenting both take time in a sealed container. One crock covers both with fewer stations. |
| Rename the ageing shelf to **cellar shelf** | Its steps are cool storage: ageing, ripening, resting, setting, and chilling. "Cellar" says why it chills. |
| Make the **mortar and pestle** a container station | Curry pastes and som tam pound several ingredients together, which a click station cannot hold. |
| Rename the fruit press to **press** | It presses oil from olives and seeds as well as juice from fruit. The cheese press stays separate. |
| Give the **grill** its own fuel slot, like the oven | Grilling happens over its own fire, not on a stove. |
| Add a new backlog item for the **grill, wok, and æbleskive pan**, with oil filling for the pot | These are cooking techniques no current feature owns, and many dishes across cuisines need them. |

With these changes, the cutting board, prep counter, mixing bowl, and strainer form the core of hand preparation, and the cooking pot, skillet, and oven the core of heated cooking.

## Open questions

- **Charcoal heat:** charcoal gives medium heat under the current fuel tags, but grilling over charcoal is usually hot. Charcoal could move to high heat.
- **Meat grinder and mill:** the meat grinder has 4 steps. It could stay separate for teaching, or the mill could grind meat too.
- **Moulds:** proposal: moulds are items placed in the oven or skillet, which change which recipe matches.
