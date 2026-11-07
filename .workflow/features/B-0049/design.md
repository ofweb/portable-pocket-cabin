# B-0049 design: butchery and seafood

Status: Ready
Feature ID: B-0049

This design implements the [B-0049 brief](brief.md), phase 5 of the [cooking program](../../cooking/program.md). Animals drop carcasses. Players dress them on a hanging hook and break them down on a butcher block, and every part has a use. Fish are prepared on the block too, and new fish and shellfish come from where they live. The [butchery reference](../../cooking/butchery.md) lists the parts. This design decides how they are built.

## Decisions

- **Delivery:** stage by stage, in four pull requests:
  1. Carcasses, hook, block, and the first chains.
  2. The meat grinder, the sausage stuffer, and their chains.
  3. The duck.
  4. Fish and seafood.

  Each ships every recipe its stage completes.
- **Carcass:** an item that stacks to one. It is carried to the pot, the hook, and the block, and it works with chests and hoppers.
- **Drops:** every adult death of a butcherable animal drops its carcass, whatever the cause, fire included. Vanilla meat, leather, feathers, rabbit hide, and wool never drop from these deaths. Babies drop nothing, as in vanilla.
- **Duck:** a new mob that looks like a chicken. It reuses the chicken model with a vanilla chicken variant texture until [B-0034](../../backlog.md#b-0034-cabin-presentation) adds art.
- **Data:** each animal's dressing and its ordered parts are data, so datapacks can change them.
- **Art:** placeholders reuse and tint vanilla textures, as the earlier cooking features do.

## Carcasses

### Items and states

Each butcherable animal has a carcass item and a dressed carcass item:

| Animal | Carcass | Dressed |
|---|---|---|
| Cow | `cow_carcass` | `dressed_cow` |
| Pig | `pig_carcass` | `dressed_pig` |
| Sheep | `sheep_carcass` | `dressed_sheep` |
| Chicken | `chicken_carcass` | `dressed_chicken` |
| Rabbit | `rabbit_carcass` | `dressed_rabbit` |
| Goat | `goat_carcass` | `dressed_goat` |
| Duck | `duck_carcass` | `dressed_duck` |

- **Stacking:** carcasses and dressed carcasses stack to 1.
- **Data component:** `portable_pocket_cabin:carcass` records what has been done to a carcass:
  - **Fresh:** no component. Only a fresh carcass can be bled.
  - **Bled:** blood was collected.
  - **Scalded:** the carcass was scalded. Scalding a fresh carcass also ends the chance to collect its blood.
- **Fleece colour:** a sheep carcass also records its fleece colour, so skinning gives wool of that colour.
- **Butchering progress:** a dressed carcass records how many parts it has given, in `portable_pocket_cabin:butchered`. A part-butchered carcass can be taken off the block and put back later.

### Loot

- **Replacement:** the entity loot tables of the cow, mooshroom, pig, sheep, chicken, rabbit, and goat are replaced through Fabric's loot table events with one pool that drops the carcass. Vanilla drops nothing for babies, and that stays.
- **Sheep:** the sheep pool sets the fleece colour from the sheep's colour. A sheared sheep's carcass records no fleece, so skinning it gives no wool.
- **Fire:** the vanilla `furnace_smelt` function is not applied, so burning animals still drop raw carcasses.
- **Duck:** the duck has its own table, `portable_pocket_cabin:entities/duck`.

### Scalding

- **Recipe:** a pot recipe at low heat with one water level scalds a pig, chicken, or duck carcass. The pig takes 200 ticks, and the chicken and duck 100.
- **Result:** the same carcass with the scalded state. The recipe result sets the component, so it does not need its own item.

## Hanging hook

The hanging hook is a click station. It holds one carcass and shows it hanging.

- **Placement:** on the underside of a block, or on a wall. It needs that block to stay up, like a lantern.
- **Hanging:** using a carcass on an empty hook hangs it. Using an empty hand on a hook with an undressed carcass takes the carcass back.
- **Bleeding:** a glass bottle on a fresh hung carcass gives a blood bottle and marks the carcass bled.
- **Dressing:** the tool and the order come from the carcass data:
  - **Skinning:** the knife skins cows, sheep, rabbits, and goats.
  - **Plucking:** an empty hand plucks a scalded chicken or duck.
  - **Scraping:** the knife scrapes a scalded pig.
  - **Not scalded:** plucking or scraping an unscalded carcass is refused, with an action-bar message that it needs scalding in a pot first.
- **Yields:** dressing gives the yields to the player, with overflow dropped below the hook. The dressed carcass replaces the carcass on the hook. An empty hand takes it down.
- **Yield items:**
  - Hide is vanilla leather, fleece is vanilla wool, and feathers and rabbit hide are vanilla.
  - Rabbits also give a rabbit's foot.
  - Pigs give a new `pig_bristles` item, which crafts a brush with copper and a stick.
- **Permissions:** inside a cabin, only the household can use it, like the other stations.

## Butcher block

The butcher block is a click station that holds one dressed carcass or fish and shows it on top.

- **Placing:** using a dressed carcass or a fish on the empty block puts it there. An undressed carcass is refused, with a message that it must be skinned or plucked first.
- **Cleaver:** a new iron tool, crafted from two iron ingots and a stick. Each use takes the next part entry from the carcass data and wears the cleaver. Sneak-use takes every remaining entry at once and wears the cleaver once per entry.
- **Done:** when the last entry is taken, the block is empty.
- **Where parts go:** to the user's inventory, with overflow dropped beside the block. In the [B-0041](../B-0041/brief.md) kitchen, overflow goes to central storage first, through the existing station receipts.
- **Taking back:** an empty hand takes a part-butchered carcass back, keeping its progress.
- **Fish:**
  1. The knife first scales and guts a fish, giving fish scales and fish guts.
  2. Further knife uses take its parts in order.

  Fish need no hook.
- **Permissions:** inside a cabin, only the household can use it.

### Carcass data

Each carcass has a file at `data/<namespace>/carcass/<animal>.json`, loaded with datapacks and reloaded with `/reload`, and synced to clients for the cookbook:

```json
{
  "carcass": "portable_pocket_cabin:pig_carcass",
  "dressing": "scrape",
  "scald": true,
  "dressing_gives": [{"id": "portable_pocket_cabin:pig_bristles"}],
  "dressed": "portable_pocket_cabin:dressed_pig",
  "parts": [
    {"id": "portable_pocket_cabin:pork_loin", "count": 2},
    {"id": "portable_pocket_cabin:pork_shoulder"}
  ]
}
```

- **Dressing:** `skin`, `pluck`, `scrape`, or `none` for fish, which go straight to the block.
- **Fish:** a fish entry names the fish as its carcass, and lists the scales and guts in `dressing_gives`.
- **Parts:** the reference's order is the data's order: primal cuts, then offal, then bones and fat. The counts are the reference's, and [B-0033](../../backlog.md#b-0033-survival-balance) tunes them.

## Cuts and the existing meat recipes

- **Cut items:** every part in the reference is an item. Raw cuts are edible like vanilla raw meat, with the same hunger and the same food poisoning chance for raw chicken parts.
- **Meat tags:** six item tags hold each animal's cooking cuts and its vanilla meat: `portable_pocket_cabin:meat/beef`, `meat/pork`, `meat/mutton`, `meat/chicken`, `meat/rabbit`, and `meat/chevon`.
  - Vanilla meat stays in the tags, so meat from chests, trades, and other mods still cooks.
  - Existing recipes that take vanilla raw meat take the tag instead. The seared and fried cuts, skewers, beef strips, and brined pork keep working.
  - Offal, bones, and fat are not in the meat tags.
- **Fish:**
  - `portable_pocket_cabin:fish/cod` and `fish/salmon` hold the fillets and the vanilla fish.
  - Existing fish recipes take those tags.
  - New fish recipes that need fillets name the fillet.

## New stations

The grinder and stuffer follow the [B-0045](../B-0045/design.md) station rules: recipe types, saving, breaking with contents spilled, hoppers, recipe panels, and household permissions.

### Meat grinder

The meat grinder is a hand-worked container station. Each use with an empty hand turns the crank once, and sneaking opens its screen.

- **Slots:** two ingredient slots and an output.
- **Recipe type:** `meat_grinder`. Its `time` is a number of turns. It takes no heat, water, oil, or container.
- **Placement:** it stands on the prep counter, like the mortar and mixing bowl.

### Sausage stuffer

The sausage stuffer is a hand-worked container station on the prep counter.

- **Slots:** one ingredient slot, a casing slot, and an output.
- **Recipe type:** `sausage_stuffer`. The casing is the recipe's `container`, so pork, sheep, and goat casings are each named where a recipe needs them. Its `time` is a number of turns.

## Duck

- **Entity:** `portable_pocket_cabin:duck`, a subclass of the vanilla chicken.
  - It swims, breeds with seeds, lays eggs, and follows players holding seeds.
  - It spawns in groups of two to four in rivers, swamps, and mangrove swamps, through Fabric biome modifications.
- **Renderer:** the chicken renderer with the vanilla cold chicken texture, as a placeholder.
- **Spawn egg:** a duck spawn egg is added to the spawn egg tab.
- **Loot:** a duck carcass for adults.
- **Data:** duck dressing and parts are carcass data like the others.

## Fish and seafood

- **New fish:** anchovy, bonito, and herring are raw fish items.
  - Fabric loot events add them to the fishing fish table, `minecraft:gameplay/fishing/fish`.
  - Each one has a `location_check` biome condition: anchovy and bonito in warm and lukewarm oceans, herring in cold and frozen oceans.
  - Each fish keeps a share of fish catches in those biomes, and other catches stay vanilla.
- **Fish data:** cod, salmon, tropical fish, pufferfish, anchovy, bonito, and herring have carcass data with `dressing: none`. Pufferfish give only fish bones.
- **Shellfish:**
  - Mussel and oyster blocks are wild forage, like the B-0050 sea plants.
  - They stand underwater on stone or gravel, and using them by hand gives mussels or oysters and removes them.
  - Mussels generate on stony shores and in cold oceans, and oysters in lukewarm oceans and mangrove swamps, through placed features and biome tags like `grows_wild/<plant>`.
- **Prawn trap:** a block placed in water in a warm or lukewarm ocean, or a mangrove swamp.
  - A trap in water collects a prawn about once a Minecraft hour, up to eight. It catches nothing out of water or in other biomes.
  - Using it with an empty hand takes the prawns. Hoppers below take them.
  - It catches up from saved game time, as the [ADR-0013](../../decisions/adr/0013-catch-up-time-stations-from-saved-game-time.md) time stations do.

## By-products

Each by-product goes to a station:

| By-product | Station and recipe |
|---|---|
| Bones | Mill: bone meal. Pot: stock and broth, by animal. |
| Back fat, suet, sheep and goat fat, chicken skin and fat, duck fat | Pot at low heat: rendered fat in a bottle, then the strainer: lard, tallow, schmaltz, or duck fat |
| Trotters, chicken feet, pork skin | Pot at low heat: gelatin |
| Blood | Mixing bowl, then the sausage stuffer: blood sausage |
| Liver | Meat grinder, mixing bowl, then the oven: leverpostej and pâté |
| Calf stomach | Fermentation crock with salt water: calf rennet |
| Fish scales | Composter |
| Fish guts | Composter, and the crock: fish sauce |

- **Rennet:** calf rennet and the existing vegetable rennet join `portable_pocket_cabin:rennets`. The curd recipes take the tag.
- **Fats:** lard and tallow join `portable_pocket_cabin:cooking_oils`, so they fill the pot and wok, and schmaltz and duck fat join them too.
- **Whole birds:** a dressed chicken or duck roasts whole in the oven at medium heat, giving a roast chicken or roast duck to serve.

## Recipes per delivery

Each delivery runs the completion check with its new raw items and ships every intermediate and dish it completes, as B-0050 did.

- **Check tool update:** the catalogue lists a carcass's parts together in one row, so the check gains the reference's parts as aliases. Each part then counts on its own.
- **Not part of this feature:**
  - Chains that also need koji spores, such as miso and soy sauce, stay blocked. Koji's first spores need a trade or a rare find, which is a separate gap.
  - Anything still missing another ingredient waits.

| Delivery | Brings | Completes |
|---|---|---|
| 1 | Carcasses for cow, pig, sheep, chicken, rabbit, and goat, the hook, the block, the cleaver, scalding, blood, and the cookbook's Butchery section | Cuts, offal, stocks, broths, rendered fats, gelatin, calf rennet, bone meal, roast chicken, and the dishes they complete |
| 2 | Meat grinder and sausage stuffer | Mince, sausages, blood sausage, bacon, ham, chorizo, leverpostej, and the dishes they complete |
| 3 | The duck | Duck cuts, duck fat, duck confit, roast duck, and duck dishes |
| 4 | Fish preparation, anchovy, bonito, herring, mussels, oysters, and the prawn trap | Fillets, fish stock, fish sauce, dashi and katsuobushi, shrimp paste, pickled herring, and the seafood dishes |

## Cookbook

- **Butchery section:** a section like Garden, with a page per animal and fish.
  - Each page shows the carcass, how to dress it, whether it needs scalding, what dressing gives, and every part in order.
  - The parts come from the synced carcass data, so datapack changes show.
- **Stations and items:** the hook, block, grinder, stuffer, cleaver, and every new item appear with their recipes and techniques.

## Verification

GameTests:

- **Loot:**
  - Adult cows, pigs, sheep, chickens, rabbits, and goats drop their carcass, with no vanilla meat, leather, feathers, hide, or wool. This holds when they burn too.
  - Babies drop nothing.
  - A sheep's carcass skins to wool of its colour.
- **Hook:**
  - Hanging works.
  - Blood comes only from a fresh carcass.
  - Skinning needs the knife.
  - Plucking and scraping need scalding, and are refused with a message without it.
  - Dressing gives its yields and the dressed carcass.
- **Scalding:** the pot scalds a chicken and a pig, and a scalded carcass can't be bled.
- **Block:**
  - Undressed carcasses are refused.
  - Each cleaver use gives the next part in data order.
  - Sneak-use finishes the carcass.
  - Overflow drops beside the block, or goes to storage in a kitchen.
  - A part-butchered carcass keeps its progress when taken back.
- **Data:** a datapack's carcass file changes the parts after a reload.
- **Meat tags:** the existing meat recipes accept both the new cuts and vanilla meat.
- **Recipes:** each delivery's recipes are found at their own stations from exactly their inputs, as in B-0050.
- **Later deliveries:**
  - The grinder and stuffer work, save, spill, and take hoppers.
  - Ducks spawn, breed, and drop duck carcasses.
  - Fishing gives the new fish in their biomes.
  - Mussels and oysters are gathered by hand.
  - A prawn trap fills only in its water and catches up after time passes.

Client GameTests capture the hook with a hanging carcass, the block mid-butchery, and the cookbook's Butchery pages for `screenshots/cooking/`.

## Assumptions

- **Crafting:**
  - The hanging hook: an iron ingot over a chain.
  - The butcher block: a log slab on two logs.
  - The meat grinder: iron ingots around a lever.
  - The sausage stuffer: iron ingots around a piston.
- **Tuning:** part counts, hunger values, prawn timing, and fish shares are first guesses for B-0033.
- **Automation:** slaughter by room automation belongs to [B-0026](../../backlog.md#b-0026-room-automation), which decides how automation handles carcasses.
