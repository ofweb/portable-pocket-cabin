# B-0053 design: prep counter and specialty stations

Status: Ready
Feature ID: B-0053

This design implements the [B-0053 brief](brief.md). It extends the [B-0045 design](../B-0045/design.md), and follows [ADR-0010](../../decisions/adr/0010-define-cooking-as-station-recipe-types.md): each new station kind gets its own recipe type in the shared cooking recipe format.

## Recipe format

Five station kinds join `CookingStation`:

| Kind | Path | Ingredient slots | Heat | Inputs |
|---|---|---|---|---|
| Prep counter | `counter` | 6 | None | Counted and shapeless |
| Strainer | `strainer` | 4 | None | Counted and shapeless, optional container |
| Grill | `grill` | 1 | Required, from its own fuel | Single item |
| Wok | `wok` | 4 | Required, from the heat source below | Counted and shapeless, oil |
| Æbleskive pan | `aebleskive_pan` | 1 | Required, from the heat source below | Single item |

The format gains one field, `oil`, the number of oil levels a recipe uses (0 to 3). Validation by station kind:

| Field | Pot | Wok | Strainer | Counter | Others |
|---|---|---|---|---|---|
| `water` | Yes | No | No | No | No |
| `oil` | Yes | Yes | No | No | No |
| `container` | Yes | Yes | Yes | No | No |
| `time` | Required with heat | Required | Required, short | Required, short | As B-0045 |

A recipe can't use both water and oil. `CookingInput` gains an `oil` amount next to `water`. The existing three-argument constructor stays, so B-0045 call sites don't change. Unheated stations with a positive `time` progress without heat, the way heated ones progress at their heat.

## Pot liquids

- **What the pot holds:** `CookingPotBlockEntity` holds water or oil, never both, up to three levels.
- **Saving:** it saves `liquid`, which is `water` or `oil`, next to the existing `water` level field, so B-0045 saves load as water.
- **Filling with oil:** an item in the `portable_pocket_cabin:cooking_oils` tag adds one oil level and returns its empty bottle. A water bucket still fills water to three.
- **Refusals:** neither liquid can be added to a pot holding the other.
- **The tag:** it is empty in this feature. B-0052 fills it with its oils, so deep-frying recipes ship with them under the [program's rule](../../cooking/program.md#rule-for-shared-chains).
- **Matching:** a recipe with `oil` matches only a pot holding oil, and a recipe with `water` only a pot holding water. Each batch uses its `oil` or `water` amount.
- **Emptying:** levels empty only by use, or when the pot is broken. No item takes oil back out.

## Prep counter

- **The block:** `PrepCounterBlock` is a full block with a block entity. It extends `CookingStationBlockEntity` with six ingredient slots and an output slot, and runs a `CookingProcess` with no heat.
- **Screen:** using the counter opens the counter screen, a new `CookingMenu.Kind.COUNTER`.
- **Recipe panel:** the screen has one, and kitchen storage fills it as B-0041 does.
- **Pausing:** hand work pauses while any cooking station block stands on top of the counter. The ticker checks the block above, so placing a mixing bowl on a busy counter holds its progress, and removing it resumes.
- **The rule for counter-top stations:** `CounterTopBlock`, a base class for counter-top stations, refuses placement unless the block below is a prep counter. Its `canSurvive` returns false otherwise, so it pops off if the counter is broken.
  - **This feature:** the strainer.
  - **Later:** B-0046 makes the mixing bowl and B-0047 the mortar and pestle counter-top blocks.
  - **The cutting board:** it doesn't extend this class, so it keeps working anywhere.

## Strainer

- **The block:** a counter-top container station with four ingredient slots, a container slot, a liquid output, and a solids output.
- **Process:** it reuses `CookingProcess` like the pot. The result goes to the liquid output, and `remainders` go to the solids output.
- **Liquids and solids:** liquids come out in the recipe's container, such as glass bottles or bowls.
- **Screen:** a new `CookingMenu.Kind.STRAINER`, with the recipe panel.

## Grill

- **The block:** a surface station with its own fire. `GrillBlockEntity` extends `FiredStationBlockEntity` with four item slots and a fuel slot.
- **Cooking:** each slot cooks on its own at the burner's heat, as the skillet does on a stove.
- **Using it:**
  - A fuel item that isn't a grill recipe input goes into the fuel slot.
  - Any other item with a grill recipe goes on a free spot.
  - An empty hand takes a finished item first, otherwise the last one placed.
  - A shovel puts the fire out, through the B-0045 `Fired` block rules.
- **When it burns:** the burner lights fuel only while a slot holds an item that a recipe would progress at some fuel heat, so it doesn't burn fuel for an empty grill.
- **Hoppers:** they add fuel from the side and take spent fuel from below. They can't reach the cooking slots.
- **Display:** items render on the grate like the skillet's, and the block shows when it's lit.

## Wok

- **The block:** a container station that sits on a heat source, like the pot.
- **Holding:** four ingredient slots, a container slot, an output slot, and up to three oil levels.
- **Heat and oil:** it reuses `CookingVessel` for heat and the pot's liquid handling, with oil as its only liquid.
- **Screen:** a new `CookingMenu.Kind.WOK`, with the recipe panel.

## Æbleskive pan

- **The block:** a surface station on a heat source with seven wells. Each well holds one batter item.
- **Turning:** a well progresses to half its recipe's time, then stops and marks itself as needing turning.
  - Using a skewer on the pan turns every well that needs it.
  - Those wells then cook to the end.
  - An unturned well stays at half, never burns, and can be taken out as unfinished batter.
- **Saving:** each well's progress and turned flag are saved.
- **Display:** the renderer shows the seven wells. A turned ball shows its browned side.

## Skewers and phase 1 recipes

- **Skewers:** an ingredient item. A stick on the cutting board, cut with the knife, gives four skewers.
- **Raw skewers:** counter recipes put one skewer, one vanilla meat, and one vegetable together:

  | Skewer | Meat | Vegetable |
  |---|---|---|
  | Beef and carrot | beef | carrot |
  | Pork and potato | porkchop | potato |
  | Chicken and beetroot | chicken | beetroot |
  | Mutton and mushroom | mutton | brown mushroom |

- **Grilling:** each raw skewer grills at high heat for 100 ticks into a grilled skewer. A grilled skewer restores 10 hunger and 12 saturation.
- **Crafting:**
  - Prep counter: smooth stone slabs over planks.
  - Strainer: iron nuggets and string.
  - Grill: iron bars over a furnace.
  - Wok: iron ingots and a stick.
  - Æbleskive pan: iron ingots around a bucket.
  - A GameTest checks that none of these collide with a vanilla recipe.

## Cookbook

- **Stations and techniques:** each new station and technique has a cookbook entry. The techniques are skewering, straining, grilling, stir-frying, deep-frying, and cooking in wells.
- **Lists:** the cookbook lists the counter, strainer, grill, wok, and pan with their recipes.

## Verification

GameTests:

- **Format:**
  - The new recipe types load.
  - The validation rules reject water outside the pot, oil outside the pot and wok, and both liquids at once.
- **Pot oil:**
  - Oil fills from a tagged item, using a test-only tag entry.
  - A pot refuses water while it holds oil.
  - Oil recipes match only oil, and each batch uses its level.
  - A saved B-0045 pot loads as water.
- **Counter:**
  - Hand work makes raw skewers.
  - It pauses while a strainer stands on top.
  - A strainer can't be placed off a counter, and pops off when its counter is broken.
  - The cutting board still works off a counter.
- **Strainer:** a test recipe gives a liquid in its container and solids in the byproduct slot.
- **Grill:**
  - Fuel goes into its slot, and the grill lights only with something to cook.
  - Skewers cook each on their own.
  - A shovel puts the fire out.
  - Hoppers reach only the fuel slot.
- **Wok:** with test oil, a test recipe stir-fries at high heat and uses the oil.
- **Æbleskive pan:**
  - A well stops at half until turned.
  - Turning finishes it.
  - Unturned batter comes back out.
- **Saving:** every new station keeps its state when saved and loaded.
- **Kitchen:** a counter in the kitchen fills from storage.

Client GameTests capture the counter screen, the strainer, a grill with skewers, the wok, and the pan for `screenshots/cooking/`.

## Delivery

Each step is a commit with its tests green:

1. Recipe format, pot liquids and oil filling, skewers, and the new recipe types.
2. Prep counter, counter-top rule, and strainer, with their screens and recipe panels.
3. Grill and skewer recipes, then the wok.
4. Æbleskive pan with turning, and renderers.
5. Cookbook entries, screenshots, README, and backlog.

## Assumptions

- Placeholder models reuse vanilla textures until B-0034 adds art.
- Hand work at the counter takes 40 ticks per batch, and straining 60. B-0033 tunes them.
