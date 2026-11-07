# Players mill, bake, and make dairy from scratch

Status: Implemented
Feature ID: B-0046

## Goal

Players turn grain and milk into the basics of a kitchen, the way people have for centuries:
- They mill wheat into flour, grow a sourdough starter, knead and proof dough, and bake bread, pastry, cakes, cookies, and pie.
- They settle milk and skim the cream, churn butter, culture yogurt, and set, press, brine, and age cheese.

Each step is a real process that the [cookbook](../../context.md#cookbook) explains. Together they show how much work lies behind a loaf of bread or a wedge of cheese.

B-0046 ships in phase 3 of the [cooking program](../../cooking/program.md), together with:
- [B-0047](../../backlog.md#b-0047-stocks-sauces-and-flavour), which adds salt.
- [B-0048](../../backlog.md#b-0048-fermentation-drying-and-preservation), which adds the fermentation crock, ageing shelf, drying rack, and brine barrel.

It uses the prep counter and strainer from [B-0053](../../backlog.md#b-0053-prep-counter-and-specialty-cooking-stations).

## Stories and acceptance

### S1: Mill grain

Story: A player mills wheat into flour.

Acceptance:

- The mill is a container station without heat, like a furnace without fuel. It grinds, crushes, cracks, and crumbles.
- Wheat grinds into wheat flour.
- **Shared station:** the mill also serves B-0047, which grinds rock salt, and later features that grind spices, grains, and meal.

### S2: Mix and shape on the prep counter

Story: A player places a mixing bowl on a prep counter, puts ingredients in it, and works them with a spoon.

Acceptance:

- **Mixing bowl:** sits on the B-0053 prep counter. Ingredients go in, and using the spoon on the bowl mixes, kneads, whisks, rubs in, or works them, as the recipe says.
- **Shaping:** the prep counter shapes dough into rolls and fills and closes pies.
- **Water:** recipes take water as water bottles, and return the empty bottles.

### S3: Leaven

Story: A player starts a sourdough starter, keeps it going, and makes yeast.

Acceptance:

- **Starter:** wheat flour and water become a sourdough starter after a day in the B-0048 fermentation crock.
- **Feeding:** feeding a starter with flour and water gives two starters, so one starter keeps a household baking.
- **Yeast:** a starter grown in sugar water in the crock, then dried on the drying rack.
- **Bread dough:** wheat flour, yeast or starter, water, and salt, kneaded in the mixing bowl, then proofed.
- **Proofing basket:** a time station. Recipes in it progress without heat over a set time.

### S4: Bake

Story: A player bakes bread, rolls, pastry, cakes, cookies, and pie in the oven.

Acceptance:

- These replace the B-0045 oven recipes that baked straight from wheat:
  - **Bread and bread rolls:** from proofed bread dough.
  - **Cookies:** from flour, butter, sugar, egg, and cocoa.
  - **Cake:** from flour, sugar, eggs, butter, and milk.
  - **Pumpkin pie:** from shortcrust pastry with pumpkin, egg, and sugar.
- **Shortcrust pastry:** flour, butter, water, and salt. The butter is rubbed in with the mixing bowl, then the pastry is chilled on the ageing shelf.
- **Sweet yeast dough:** for enriched bakes.
- Baked goods keep vanilla food values unless the design gives a reason to change them.

### S5: Separate cream and churn butter

Story: A player settles milk, skims the cream, and churns butter.

Acceptance:

- **Dairy pan:** a time station. Milk settles in it. Skimming with glass bottles then gives bottles of cream and leaves skimmed milk.
- **Butter churn:** churns cream into raw butter and buttermilk.
- **In the mixing bowl:** salt worked into raw butter makes salted butter, and cream whisked with sugar makes whipped cream.

### S6: Culture milk

Story: A player makes yogurt and crème fraîche.

Acceptance:

- **Yogurt:** milk warmed in the cooking pot at low heat, then cultured in the crock with a spoonful of yogurt for a day.
- **The first culture:** live buttermilk can start the very first batch of yogurt.
- **Crème fraîche:** cream cultured with buttermilk in the crock.

### S7: Make cheese

Story: A player makes rennet from a plant, sets curds, and presses, drains, brines, and ages cheese.

Acceptance:

- **Cardoon:** a new wild thistle in meadows and savannas. It can be replanted.
- **Vegetable rennet:** cardoon flowers steeped in salt water in the crock, as traditional Iberian cheeses still use.
- **Curds and whey:** milk warmed in the cooking pot at low heat with rennet, set, then cut.
- **Cheese press:** a container station that presses curds.
- **The cheeses:**
  - **Fresh cheese:** curds and salt, pressed lightly.
  - **Quark:** skimmed milk soured with buttermilk in the crock, then drained in the strainer.
  - **Cream cheese:** cream cultured in the crock, drained in the strainer, then salted.
  - **Aged hard cheese:** curds pressed hard, brined in the brine barrel, then aged on the ageing shelf.
  - **Aged soft cheese:** curds and salt, drained and ripened on the ageing shelf.

## Feature-wide constraints and acceptance

- New stations follow B-0045 rules: recipe types, counted matching, progress, saving, breaking, and household permissions inside a cabin. Inside the [B-0041](../B-0041/brief.md) kitchen they use central storage like other kitchen stations.
- Time stations follow the [cooking time stations catch-up](../../direction.md#cooking-time-stations-catch-up) direction. They count server time that passed while unloaded or packed, and finish a batch, but never collect, refill, or start one.
- Every new item and recipe appears in the cookbook, with its technique explained.
- Recipes are data, so datapacks can change times and add products.
- Products that need ingredients from later features ship with those features, under the program's [rule for shared chains](../../cooking/program.md#rule-for-shared-chains).

## Scope

- **Stations:** the mill, the mixing bowl with its spoon, the proofing basket, the dairy pan, the butter churn, and the cheese press.
- **Plant:** cardoon.
- **Products:** the wheat flour, leavening, dough, pastry, bake, butter, cultured milk, rennet, curd, and cheese chains in the stories.
- **The rest of the grains, flours, starches, doughs, and dairy products** in the [intermediate products](../../cooking/intermediates.md) list, as their ingredients arrive with B-0049, B-0050, and B-0052.

## Non-goals

- **B-0053:** the prep counter and strainer themselves.
- **B-0048:** the crock, ageing shelf, drying rack, and brine barrel themselves.
- **B-0047:** salt and sugars.
- **B-0049:** calf rennet. It later joins vegetable rennet as a second source.
- **B-0051:** food benefits beyond vanilla values.

## Related records

- [Cooking program](../../cooking/program.md).
- [Cooking stations and cookbook](../B-0045/brief.md).
- [Manual kitchen](../B-0041/brief.md).
- [Cooking stations list](../../cooking/stations.md).
- [Intermediate products](../../cooking/intermediates.md).
- [Raw ingredient sources](../../cooking/sources.md).
- [Cooking concept](../../cooking/concept.md).

## Open questions and assumptions

- **Time:** times are in game time. A "day" is one Minecraft day. B-0033 tunes them.
- **Containers:** milk stays in buckets. Cream comes in bottles, skimmed milk in buckets, and yogurt and crème fraîche in bowls. The design may adjust these.
- **Time station form:** the joint phase 3 design settles whether time stations hold several batches and how they show progress.
