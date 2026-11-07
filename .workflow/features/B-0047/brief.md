# Players make salt, sugars, and sauces, and learn to season

Status: Implemented
Feature ID: B-0047

## Goal

Players make the seasonings every kitchen depends on. They mine and grind rock salt, boil seawater down to sea salt, and cook sugar cane into brown sugar and panela. Then they turn butter, flour, and fruit into sauces. They learn the five basic tastes, salty, sweet, sour, bitter, and umami, and that a dish is good when its tastes are in balance. Seasoning a dish well raises its quality.

B-0047 ships in phase 3 of the [cooking program](../../cooking/program.md), together with:
- [B-0046](../B-0046/brief.md), which adds the mill, mixing bowl, and butter.
- [B-0048](../../backlog.md#b-0048-fermentation-drying-and-preservation), which adds the drying rack, ageing shelf, and cider vinegar.

## Stories and acceptance

### S1: Make salt

Story: A player mines rock salt or boils seawater for salt.

Acceptance:

- **Rock salt:** a new ore in dripstone caves and badlands. It grinds into salt in the B-0046 mill.
- **Sea salt:**
  - A glass bottle filled in any ocean gives a bottle of seawater.
  - Seawater boiled down in the cooking pot at high heat leaves wet salt and nigari.
  - The salt crystals then dry on the drying rack.
- **Coarse salt:** salt dried slowly on the drying rack grows large crystals.
- **Nigari:** the bitter liquid left from sea salt. Later features use it to set tofu.

### S2: Make sugars and syrups

Story: A player cooks sugar cane into brown sugar and panela, and makes syrup and caramel.

Acceptance:

- **Brown sugar:** sugar cane juice boiled at high heat with its molasses kept, then dried.
- **Panela:** cane crushed in the mill, its juice boiled down, then set in blocks on the ageing shelf.
- **Simple syrup:** sugar dissolved in water at low heat.
- **Caramel:** sugar cooked on the skillet at medium heat until amber.

### S3: Pound and grind by hand

Story: A player places a mortar and pestle on a prep counter and pounds ingredients.

Acceptance:

- **Mortar and pestle:** a counter-top station on the [B-0053](../B-0053/brief.md) prep counter. Ingredients go in, and using the pestle pounds pastes, mashes, and grinds coarsely.

### S4: Make sauces

Story: A player makes sauces from what the kitchen already produces.

Acceptance:

- **Roux:** salted butter and wheat flour cooked together in the pot at low heat.
- **Sweet berry jam:** sweet berries cooked with sugar at low heat until it sets.
- **Caramel sauce:** caramel and cream.
- **Later sauces:** the stocks, pastes, and sauces in the [intermediate products](../../cooking/intermediates.md) ship as their ingredients arrive. This includes béchamel with nutmeg from B-0050, and stocks from B-0049 bones and B-0050 kombu.

### S5: Learn the five tastes

Story: A player reads in the cookbook which tastes each ingredient and dish has.

Acceptance:

- Every ingredient and dish lists its tastes in the cookbook: salty, sweet, sour, bitter, umami.
- The cookbook explains each taste with its real sources:
  - **Salty:** salt.
  - **Sweet:** sugar and fruit.
  - **Sour:** vinegar, cultured dairy, and fruit acids.
  - **Bitter:** nigari, toasted seeds, and greens.
  - **Umami:** aged cheese, dried kelp, mushrooms, and cured fish.

### S6: Season for quality

Story: A player seasons a dish and makes it better.

Acceptance:

- **Wanted tastes:** each savoury and sweet dish recipe names the tastes that balance it.
- **Seasoning slots:** a dish's last cooking step has optional seasoning slots. A seasoning whose taste the dish wants raises its quality by one level. A taste it doesn't want, or a second dose of the same taste, lowers it.
- **Levels:** plain, seasoned, and well balanced. A dish made without seasoning is plain and still good.
- **Effect now:** quality is shown on the item and adds a small, bounded saturation bonus. [B-0051](../../backlog.md#b-0051-food-benefits) later adds benefits that use quality.
- **Stacking:** dishes of different quality don't stack together.

## Feature-wide constraints and acceptance

- New stations follow B-0045 rules: recipe types, counted matching, progress, saving, breaking, hoppers, and household permissions inside a cabin. Inside the [B-0041](../B-0041/brief.md) kitchen they use central storage like other kitchen stations.
- **Filling:** recipe-panel filling never adds optional seasonings. Players add them deliberately.
- Every new item and recipe appears in the cookbook, with its technique and tastes explained.
- Recipes and wanted tastes are data, so datapacks can change them.

## Scope

- **Ore and station:** rock salt ore and the mortar and pestle.
- **Products:** salts, nigari, sugars and syrups, caramel, and the sauces in S4.
- **Systems:** taste data for every ingredient and dish, seasoning slots, and dish quality.
- **Later:** the remaining stocks, sauces, pastes, and condiments, as their ingredients arrive.

## Non-goals

- **B-0051:** food benefits beyond the small saturation bonus.
- **Spice and herb plants:** B-0050 owns them.

## Related records

- [Cooking program](../../cooking/program.md).
- [Milling, baking, and dairy](../B-0046/brief.md).
- [Prep counter and specialty stations](../B-0053/brief.md).
- [Intermediate products](../../cooking/intermediates.md).
- [Cooking techniques](../../cooking/techniques.md).

## Open questions and assumptions

- **Seasoning slots:** the joint phase 3 design settles how many slots a step has and which step takes them.
- **Tuning:** B-0033 tunes the saturation bonus and the ore's frequency.
