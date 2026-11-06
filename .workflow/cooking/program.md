# Cooking program

This plan builds the full cooking design from [B-0045](../features/B-0045/brief.md) onward as one program. Each feature keeps its own brief and design. The order below makes each delivery playable on its own and lets later features complete earlier chains.

## Rule for shared chains

A product ships with the last feature its ingredients and stations need. For example:

- Beef stock is a B-0047 product, but it needs beef bones, so it ships with B-0049 butchery.
- Salted butter is B-0046, and salt comes from B-0047 rock salt, so salted butter ships with B-0047, or with B-0046 if B-0046 ships later.

Each feature's brief lists the products it ships and the ones it leaves to a later feature.

## Order

| Phase | Feature | Ships | Why here |
|---|---|---|---|
| 1 | [B-0053](../backlog.md#b-0053-prep-counter-and-specialty-cooking-stations) Prep counter and specialty stations | Prep counter, strainer, grill, wok, æbleskive pan, and oil filling for deep-frying | Every counter-top station needs the counter, and many chains end in a strain. |
| 2 | [B-0052](../backlog.md#b-0052-fruit-pressing-and-drinks) Pressing and drinks | Press, juices, syrups, and pumpkin seed oil, the first cooking oil | Oil unlocks frying in the wok and pot. Apple juice starts cider. |
| 3 | [B-0046](../backlog.md#b-0046-milling-baking-and-dairy), [B-0047](../backlog.md#b-0047-stocks-sauces-and-flavour), and [B-0048](../backlog.md#b-0048-fermentation-drying-and-preservation) together | Mill, mixing bowl and spoon, proofing basket, dairy pan, butter churn, and cheese press (B-0046). Rock salt, sea salt, sugars, and the mortar and pestle (B-0047). Fermentation crock, ageing shelf, drying rack, brine barrel, and smoker (B-0048). With them, every flour, bread, pastry, butter, cheese, salt, sauce, ferment, pickle, cider vinegar, and dried-food chain that existing ingredients allow | The three depend on each other in a loop: salt needs the mill, bread needs salt and a starter from the crock, and brining needs salt. They share one design and ship together. |
| 4 | [B-0050](../backlog.md#b-0050-cooking-crops-and-foraging) Cooking crops and foraging | About 115 plants, sea plants, and their growing, foraging, and greenhouse rules | The largest content step. It completes most recipe chains from phases 1–3. |
| 5 | [B-0049](../backlog.md#b-0049-butchery-and-carcasses) Butchery and carcasses | Carcasses, hanging hook, butcher block, meat grinder, sausage stuffer, cuts, offal, bones, fat, and seafood | Completes stocks, sausages, cured meat, lard and tallow, and calf rennet. |

[B-0051](../backlog.md#b-0051-food-benefits) food benefits is not a dependency of any of these and stays separate.

## First choices already made

- **Prep counter:** it belongs to B-0053, which ships first.
- **Rennet:** cardoon flowers give vegetable rennet in B-0046. B-0049 adds calf rennet as a second source.
- **First cooking oil:** pumpkin seed oil. Pumpkin seeds are toasted on the skillet and pressed whole, as an expeller press does. Sunflower, rapeseed, olive, and sesame oils follow with B-0050.
- **First vinegar:** apples are chopped on the cutting board and pressed into juice in phase 2. In phase 3 the juice ferments into cider in the crock, then sours into cider vinegar. Wine and rice vinegars follow with B-0050.
- **Cheese:** curds set with cardoon rennet. Fresh cheese is pressed, quark and cream cheese drain in the strainer, and hard cheese is brined and then aged, all in phase 3.

## Size

The program adds about 125 raw ingredients and 20 stations, and roughly 890 recipe steps. B-0050 and B-0049 are the largest. Each needs textures and models, which [B-0034](../backlog.md#b-0034-cabin-presentation) and the [texture audit](texture-audit.md) cover.
