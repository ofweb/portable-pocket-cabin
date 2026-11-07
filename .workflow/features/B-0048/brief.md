# Players ferment, dry, cure, smoke, and age food

Status: Implemented
Feature ID: B-0048

## Goal

Before refrigeration, people kept food by fermenting, drying, salting, smoking, and ageing it. Those methods also created much of the world's flavour. Players learn them by doing them:

- Starting a sourdough culture.
- Turning apple juice into cider and then vinegar.
- Pickling beetroot, drying fruit and fish, curing salt cod and jerky, smoking salmon, and ageing cheese.

Time stations run for in-game days and keep working while players are away.

B-0048 ships in phase 3 of the [cooking program](../../cooking/program.md), together with:
- [B-0046](../B-0046/brief.md), which adds flour, milk products, and curds.
- [B-0047](../B-0047/brief.md), which adds salt.

## Stories and acceptance

### S1: Use time stations

Story: A player starts a long process and comes back when it is done.

Acceptance:

- **Fermentation crock:** a sealed crock for fermenting, culturing, souring, pickling, soaking, sprouting, steeping, and infusing.
- **Ageing shelf:** a cool shelf that holds several items and ages, ripens, cures, rests, sets, and chills them.
- **Drying rack:** dries and withers. Sun-drying recipes need open sky above the rack.
- **Brine barrel:** salts, brines, cures, and marinates.
- **Shared rules:**
  - Time stations need no heat. A recipe progresses over a set time measured in Minecraft days and hours.
  - They catch up as the [cooking time stations catch-up](../../direction.md#cooking-time-stations-catch-up) direction says. Batches can finish while their chunk is unloaded or their cabin is packed, but nothing is collected, refilled, or started.
- **Smoker:** the vanilla smoker becomes the smoking station. It burns low-heat fuel and smokes cured food slowly. Like other heated stations, it pauses with its fire.

### S2: Ferment and culture

Story: A player keeps cultures alive and turns juice into vinegar.

Acceptance:

- **Sourdough starter:** wheat flour and water, as [B-0046](../B-0046/brief.md) describes. Yeast is grown from it and dried.
- **Cider:** apple juice from [B-0052](../B-0052/brief.md) ferments into cider in the crock.
- **Cider vinegar:** cider soured for days with a little live vinegar becomes cider vinegar. The first vinegar comes from cider left open for longer.
- **Cultured dairy:** yogurt, crème fraîche, quark, cream cheese, and cardoon rennet use the crock as B-0046 describes.

### S3: Pickle

Story: A player pickles vegetables in vinegar.

Acceptance:

- **Pickled beetroot:** beetroot boiled, sliced, then pickled with cider vinegar and sugar.
- **Pickled carrots:** carrots sliced and pickled with cider vinegar and salt.

### S4: Dry

Story: A player dries fruit and kelp on the rack.

Acceptance:

- **Dried apple:** apples sliced, then dried.
- **Dried sweet berries:** sweet berries, dried.
- **Dried kelp:** kelp dries on the rack. This replaces the B-0045 oven recipe, since kelp is dried, not baked.

### S5: Cure and smoke

Story: A player salts, dries, and smokes meat and fish so they keep.

Acceptance:

- **Salt cod:** cod salted in the brine barrel, then dried on the rack.
- **Jerky:** beef cut into strips, cured with salt, then dried.
- **Smoked salmon:** salmon cured with salt and sugar, then cold-smoked in the smoker.
- **Smoked pork:** pork cured in brine, then smoked.
- **Keeping:** cured foods stack, keep indefinitely, and restore a fair amount of hunger, as preserved food did on long journeys.

### S6: Age cheese

Story: A player brines and ages cheese.

Acceptance:

- **Aged hard cheese:** pressed curds from B-0046, brined in the brine barrel, then aged on the ageing shelf for several days.
- **Aged soft cheese:** curds drained and ripened on the ageing shelf, with a white rind.

## Feature-wide constraints and acceptance

- New stations follow B-0045 rules: recipe types, counted matching, progress, saving, breaking, hoppers, and household permissions inside a cabin. Inside the [B-0041](../B-0041/brief.md) kitchen they use central storage like other kitchen stations.
- Catch-up never moves items between stations, never takes from storage, and never starts a batch.
- Every new item and recipe appears in the cookbook, with its technique explained, including why it preserves food.
- Recipes are data, so datapacks can change times and add ferments.

## Scope

- **Stations:** the fermentation crock, ageing shelf, drying rack, brine barrel, and the vanilla smoker as a smoking station.
- **Products:** the starter, yeast, cider, vinegar, pickles, dried fruit and kelp, cured and smoked fish and meat, and aged cheese in the stories.
- **Later:** the rest of the fermented, pickled, dried, and cured products in the [intermediate products](../../cooking/intermediates.md), as their ingredients arrive. These include sauerkraut, miso, soy sauce, koji, fish sauce, wine and rice vinegars, umeboshi, and sausages and hams from B-0049 cuts.

## Non-goals

- **Drinks:** alcoholic drinks as drinks. Cider exists only as the step to vinegar.
- **B-0051:** food benefits beyond hunger values.

## Related records

- [Cooking program](../../cooking/program.md).
- [Cooking time stations catch-up](../../direction.md#cooking-time-stations-catch-up).
- [Milling, baking, and dairy](../B-0046/brief.md).
- [Salt, sugars, and sauces](../B-0047/brief.md).
- [Pressing and drinks](../B-0052/brief.md).
- [Intermediate products](../../cooking/intermediates.md).

## Open questions and assumptions

- **Times:** times use Minecraft days. B-0033 tunes them.
- **Smoker:** whether the smoker stays the vanilla block with new recipes, or becomes a cabin station block, is a design question.
