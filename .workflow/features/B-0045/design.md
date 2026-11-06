# B-0045 design: cooking stations and cookbook

Status: Ready
Feature ID: B-0045

This design implements the [B-0045 brief](brief.md). [ADR-0010](../../decisions/adr/0010-define-cooking-as-station-recipe-types.md) and [ADR-0011](../../decisions/adr/0011-remove-vanilla-food-recipes-with-load-conditions.md) record the decisions that later cooking features reuse.

## Shape of the change

This is the mod's first set of custom blocks and block entities. Everything else in B-0045 hangs off four things:

1. **Cooking recipe types.** One recipe type per station kind, with one shared JSON format. Datapacks and later features add recipes as data.
2. **Heat.** A small service that answers "what heat level does this station have right now?" from fuel tags and campfires.
3. **Station block entities.** Each one ticks like a furnace and stores its contents, fuel, water, and progress in block entity NBT.
4. **Client recipe sync.** The cookbook and the station recipe panels read recipes that Fabric's recipe sync sends to the client.

Code stays in the flat `dev.portablepocketcabin` package, with a `Cooking` prefix for new classes, which matches the existing `Cabin` prefix convention.

## Recipes

### Recipe types

| Type | Station | Input | Heat | Matching |
|---|---|---|---|---|
| `portable_pocket_cabin:cutting` | Cutting board | One item | None | Single input, knife use |
| `portable_pocket_cabin:skillet` | Skillet | One item per slot | Required | Each slot matches on its own |
| `portable_pocket_cabin:pot` | Cooking pot | 6 ingredient slots, container, water | Required | Counted and shapeless over the ingredient slots |
| `portable_pocket_cabin:oven` | Oven | 6 ingredient slots | Required | Counted and shapeless over the ingredient slots |

One `CookingRecipe` record backs all four types, and each type has its own `RecipeSerializer` built from the shared codec. Example:

```json
{
  "type": "portable_pocket_cabin:pot",
  "technique": "simmering",
  "ingredients": [
    { "item": "portable_pocket_cabin:chopped_carrot" },
    { "item": "portable_pocket_cabin:diced_potato" },
    { "item": "portable_pocket_cabin:chopped_beetroot" }
  ],
  "container": { "item": "minecraft:bowl", "count": 3 },
  "water": 1,
  "heat": "low",
  "time": 300,
  "result": { "id": "portable_pocket_cabin:vegetable_soup", "count": 3 }
}
```

Cake shows counted ingredients and remainders:

```json
{
  "type": "portable_pocket_cabin:oven",
  "technique": "baking",
  "ingredients": [
    { "item": "minecraft:wheat", "count": 3 },
    { "item": "minecraft:sugar", "count": 2 },
    { "item": "minecraft:egg" },
    { "item": "minecraft:milk_bucket", "count": 3 }
  ],
  "remainders": [{ "id": "minecraft:bucket", "count": 3 }],
  "heat": "low",
  "time": 200,
  "result": { "id": "minecraft:cake" }
}
```

- `ingredients` lists each required ingredient with an optional `count`, which defaults to 1. Matching is counted and shapeless:
  - Each filled ingredient slot is assigned to one entry, and every filled slot must be assigned.
  - An entry matches when its assigned slots hold at least `count` matching items in total, so a stack of 3 wheat in one slot satisfies `count: 3`.
  - Unstackable items such as milk buckets take one slot each. Cake fills all six oven slots: wheat, sugar, egg, and three milk buckets.
  - With six slots or fewer, the station tries every assignment, so overlapping ingredients cannot block a match.
  - Completion takes `count` items for each entry from its assigned slots. Any extra items stay in their slots.
- `container` is optional and goes in the pot's container slot. The finished output keeps it, like soup in a bowl.
- `water` is the pot water used per craft. It is optional and defaults to 0.
- `remainders` is optional. It lists items returned to the byproduct slot, such as empty buckets from cake.
- `heat` is `low`, `medium`, or `high`, and is absent for the cutting board.
- `time` is in ticks.
- `technique` is a key for the cookbook's explanation text in `en_us.json`, such as `cooking.technique.simmering`.

`isSpecial()` returns true and `placementInfo()` returns `PlacementInfo.NOT_PLACEABLE`, so the vanilla recipe book neither lists nor places these recipes. `recipeBookCategory()` returns one registered `portable_pocket_cabin:cooking` category.

A result or remainder must fit one stack. Item stack sizes are not known while recipes load, so a GameTest checks every loaded cooking recipe instead of the recipe format.

### Client sync

`RecipeSynchronization.synchronizeRecipeSerializer` registers the four cooking serializers. The client receives the recipes on join and `/reload` through `ClientRecipeSynchronizedEvent`. A small client cache groups them by station for the cookbook and the station panels.

## Heat

- Item tags `portable_pocket_cabin:heat/low`, `heat/medium`, and `heat/high` assign each fuel one level. Burn time stays vanilla `FuelValues`, so fuel slots accept exactly what a furnace accepts.
- A fuel that is in no heat tag, such as a fuel from another mod, burns at medium heat.
- `CookingHeat.at(level, pos)` returns the heat for a pot or skillet at `pos`:
  - If the block below is a stove that is burning, it returns that stove's current fuel level.
  - If the block below is a lit campfire or soul campfire, it returns low.
  - Otherwise there is no heat.
- **Lighting and burning are separate.** A stove lights a new fuel item only when it is not burning and the vessel above has a matching recipe that needs heat, like a furnace. Once lit, the fuel burns down every tick until it runs out, even when nothing can progress. Then the next fuel item lights, which is when the heat level changes.
- **Putting the fire out:** using a shovel on a burning stove or oven puts the fire out, as with a campfire, and the burning fuel is lost. A player can switch to a lower heat at once instead of waiting for long-burning fuel such as a lava bucket.
- The oven has its own fuel slot and uses the same rules.

| Tag | Initial members |
|---|---|
| `heat/low` | `#minecraft:planks`, `minecraft:stick`, `#minecraft:saplings`, `#minecraft:wooden_slabs`, `#minecraft:wooden_stairs`, `minecraft:bamboo`, `minecraft:dried_kelp_block`, `minecraft:bowl` |
| `heat/medium` | `#minecraft:logs`, `minecraft:charcoal`, `minecraft:coal` |
| `heat/high` | `minecraft:coal_block`, `minecraft:blaze_rod`, `minecraft:lava_bucket` |

## Stations

All stations are blocks with block entities that use standard ticking. That means they pause automatically in a packed, empty cabin and during room relocation, because the existing chunk-ticket and `CorridorBlockEntityMixin` pauses cover every ticking block entity.

| Station | Block entity state | Interaction |
|---|---|---|
| Stove | Fuel slot, burn time left, burn duration, current heat level | Opens a one-slot fuel screen. Shows flames when burning. |
| Cooking pot | 6 ingredient slots, container slot, output slot, byproduct slot, water 0–3, progress, current recipe | Use with a water bucket to fill water and get an empty bucket. Otherwise opens the pot screen. |
| Skillet | 4 item slots, each with its own progress | Use with a cookable item to add it. Use with an empty hand to take finished items. Renders its items like a campfire. |
| Oven | 6 ingredient slots, fuel slot, output slot, byproduct slot, burn state, progress | Opens a furnace-style screen. |
| Cutting board | One item stack | Use with a cuttable item to place it. Use with the knife to cut one item, or sneak-use to cut the whole stack. Use with an empty hand to take it back. Renders the item lying on the board. |

Shared rules:

- A station looks up its current recipe from its inputs when they change, not every tick.
- Progress advances by one per tick only while the heat equals the recipe's heat. Otherwise it holds its value, and the screen shows a status line, for example "Needs low heat".
- When progress reaches `time` and the output and byproduct slots have room, the station consumes the inputs, the container, and the water, then adds the output and byproducts. A full output slot pauses progress.
- A change of inputs that changes the matched recipe resets progress to 0. Breaking a station drops all its contents, and progress restarts when the items are cooked again.
- Each completion happens in one server-tick transaction on the block entity, so it either fully happens or doesn't.
- Cutting gives its output to the player's inventory and drops any overflow beside the board.
- Hoppers work like a furnace. The pot takes ingredients from above, containers from the sides, and gives output and byproducts from below. The oven works the same, with fuel from the sides instead of containers. The stove takes fuel from any side. Both give back only spent fuel, such as the empty bucket from a lava bucket, through the bottom of the oven or any side of the stove. Hoppers can't use the skillet or the cutting board.

### Crafting

| Station or tool | Recipe |
|---|---|
| Stove | Shaped: `I I` / `IFI`, with I iron ingot and F furnace |
| Cooking pot | Shaped: `C C` / `C C` / `CCC`, with C copper ingot |
| Skillet | Shaped: `II ` / `IIS`, with I iron ingot and S stick |
| Oven | Shaped: `BBB` / `BFB` / `BBB`, with B brick and F furnace |
| Cutting board | Shaped: `##`, with # any wooden slab |
| Kitchen knife | Shaped: `I` / `S`, with I iron ingot and S stick |
| Cookbook | Shapeless: book and bowl |

### Permissions

Inside the pocket dimension every station interaction and every screen's `stillValid` calls `CabinCorridors.mayChange(level, player, pos)`. Guests are already blocked from using blocks there, and screens close when a player's role is removed. Outside the cabin, stations follow normal Minecraft rules.

### Models

There are no custom textures yet. The stations use block models that reference vanilla textures:

- Stove: smoker and blast furnace faces.
- Cooking pot: a cauldron-shaped model with copper faces.
- Skillet: a thin iron slab with a stick handle.
- Oven: brick sides with a furnace front.
- Cutting board: a one-pixel plank slab.

Prepared items and new dishes use placeholder models that reuse vanilla textures. B-0034 replaces them with final art.

## Items

| Item | Kind | Food |
|---|---|---|
| Kitchen knife | Tool, no durability | — |
| Cookbook | Opens the cookbook screen | — |
| Chopped carrot, diced potato, chopped beetroot, chopped apple | Prepared | Not edible |
| Sliced brown mushrooms, sliced red mushrooms | Prepared | Not edible |
| Sautéed mushrooms | Prepared and edible | 3 hunger, 3.6 saturation |
| Fried egg | Dish | 3 hunger, 3.6 saturation |
| Vegetable soup | Dish in a bowl, stacks to 16 | 7 hunger, 8.4 saturation, returns the bowl |
| Apple compote | Dish in a bowl, stacks to 16 | 5 hunger, 6.0 saturation, returns the bowl |

Vanilla outputs keep their vanilla food values. The new soups stack to 16, unlike vanilla stews, because a pot makes several portions into one output slot. Recipe times start at 100 ticks for skillet recipes, 200 for oven recipes, and 300 for pot recipes. B-0033 tunes them.

## Cookbook

- The cookbook is a client screen opened by using the item. It reads the synced recipe cache. A server menu isn't needed for browsing.
- The left side lists recipes grouped by station with a search box. The right side shows the selected recipe: station, ingredients with links, heat, time, output, and the technique explanation.
- Ingredient links search the cache for recipes whose result is that item.
- **Availability:**
  - When the screen opens, the client sends a `CookbookAvailabilityRequest` payload.
  - The server counts the player's inventory. If the player is inside a cabin, is household, and the cabin has central storage installed, it also counts that storage.
  - The server replies with the set of recipe ids whose ingredients are all present. It never sends storage contents to guests.

## Station recipe panel

- The pot and oven screens have a recipe-panel button, like the vanilla recipe book. It lists that station's synced recipes and marks the ones the player can fill.
- Selecting a recipe sends a `CookingFillRequest(pos, recipeId)`. The server first returns the current ingredient-slot contents to the player, then moves the recipe's ingredients and container from the player's inventory.
- Filling skips named and customised stacks, using the same check as the crafting room, `CabinStation.ordinary`.
- The server code takes an ingredient source list. B-0041 adds central storage in front of the player inventory, and adds fuel filling, without changing the payload.

## Vanilla food removal

[ADR-0011](../../decisions/adr/0011-remove-vanilla-food-recipes-with-load-conditions.md) removes these 35 vanilla recipes:

- `smelting`, `smoking`, and `campfire_cooking` for baked potato, cooked beef, chicken, cod, mutton, porkchop, rabbit, salmon, and dried kelp: 27 recipes.
- Crafting `bread`, `cake`, `cookie`, `pumpkin_pie`, `mushroom_stew`, `beetroot_soup`, `rabbit_stew_from_brown_mushroom`, and `rabbit_stew_from_red_mushroom`: 8 recipes.

With its recipes gone, a campfire no longer accepts raw food, because vanilla only places items it can cook. Golden foods, suspicious stew, and unpacking dried kelp and honey blocks are unchanged.

## Verification

GameTests:

- Recipe codec round trip, and each starter recipe matching at its station.
- Heat:
  - Each tag's fuel gives its level.
  - An untagged fuel gives medium.
  - A campfire gives low.
  - No heat source gives none.
- Progress:
  - Progress pauses at the wrong heat and resumes without loss.
  - A full output slot pauses.
  - A recipe change resets.
  - The stove lights only with work above it, and lit fuel burns down even when nothing progresses.
  - Leftover high-heat fuel burns out after a switch to a low-heat recipe, then the next low-heat fuel lights.
  - A shovel puts out a burning stove or oven, and the next fuel lights at its own level.
- Counted matching:
  - Cake completes from 3 wheat, 2 sugar, an egg, and 3 milk buckets in the six oven slots, and returns 3 empty buckets to the byproduct slot.
  - A stack larger than an entry's count leaves its extra items in the slot.
  - An extra unmatched item in any slot prevents a match.
- Persistence: a save and reload of each station keeps its contents, water, fuel, and progress.
- Removal: the 35 vanilla recipes are absent, the replacement cooking recipes are present, and a campfire refuses raw beef.
- Permissions: a guest cannot use or open a station in a cabin, and a resident can.
- Filling: it skips named and customised stacks and returns displaced items.

Client GameTests capture the cookbook list and detail views and the pot, oven, and stove screens for `screenshots/cooking/`.

## Delivery

Each step is a commit with its tests green:

1. Recipe types, codec, sync, items, heat tags, and the vanilla removal data.
2. Stove, cooking pot, skillet, and campfire heat.
3. Oven.
4. Cutting board and knife.
5. Cookbook screen and availability payload.
6. Station recipe panel and filling.
7. Screenshots, README section, and backlog update.

## Assumptions

- Fabric's `fabric:load_conditions` with the `fabric:false` condition removes an overridden vanilla recipe. A GameTest confirms all 35 are gone and that non-food recipes remain.
- Placeholder textures are acceptable until B-0034.
