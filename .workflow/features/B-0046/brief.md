# Players mill, bake, and make dairy from scratch

Status: Ready
Feature ID: B-0046

## Goal

Players turn grain, milk, and salt into the basics of a kitchen the way people have for centuries:
- They mill wheat into flour, grow a sourdough starter, knead and proof dough, and bake bread, pastry, cakes, cookies, and pie.
- They settle milk and skim the cream, churn butter, culture yogurt, and set, press, and age cheese.

Each step is a real process that the [cookbook](../../context.md#cookbook) explains. Together they show how much work lies behind a loaf of bread or a wedge of cheese. The four vanilla bakes that [B-0045](../B-0045/brief.md) made from whole wheat now need flour, butter, and pastry.

## Stories and acceptance

### S1: Mill grain and salt

Story: A player mills wheat into flour and rock salt into salt.

Acceptance:

- The mill is a container station without heat, like a furnace without fuel.
- Wheat grinds into wheat flour.
- Rock salt is a new ore in dripstone caves and badlands. It grinds into salt.

### S2: Mix and shape on the prep counter

Story: A player places a mixing bowl on a prep counter, puts ingredients in it, and works them with a spoon.

Acceptance:

- **Prep counter:** a work surface. Counter-top stations sit on it, as a pot sits on a stove. The cutting board can also sit on it.
- **Mixing bowl:** sits only on a prep counter. Ingredients go in, and using the spoon on the bowl mixes, kneads, whisks, rubs in, or works them, as the recipe says.
- **Shaping:** the prep counter itself shapes dough into rolls and fills and closes pies.
- **Water:** recipes take water as water bottles, and return the empty bottles.

### S3: Leaven with sourdough

Story: A player starts a sourdough starter and keeps it going.

Acceptance:

- The fermentation crock is a time station. Its recipes progress without heat over a set time, like a slow furnace.
- Wheat flour and water in the crock become a sourdough starter after a day.
- Feeding a starter with flour and water gives two starters, so one starter keeps a household baking.
- Bread dough is wheat flour, starter, water, and salt, kneaded in the mixing bowl, then proofed in a proofing basket.
- The proofing basket is a time station.

### S4: Bake

Story: A player bakes bread, rolls, pastry, cakes, cookies, and pie in the oven.

Acceptance:

- These replace the B-0045 oven recipes that baked straight from wheat:
  - **Bread and bread rolls:** from proofed bread dough.
  - **Cookies:** from flour, butter, sugar, egg, and cocoa.
  - **Cake:** from flour, sugar, eggs, butter, and milk.
  - **Pumpkin pie:** from shortcrust pastry with pumpkin, egg, and sugar.
- Shortcrust pastry is flour, butter, water, and salt. The butter is rubbed in with the mixing bowl, then the pastry is chilled on the ageing shelf.
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

Story: A player makes rennet from a plant, sets curds, and presses and ages cheese.

Acceptance:

- **Cardoon:** a new wild thistle in meadows and savannas. It can be replanted.
- **Vegetable rennet:** cardoon flowers steeped in salt water in the crock, as traditional cheeses still use.
- **Curds and whey:** milk warmed in the cooking pot at low heat with rennet, set, then cut.
- **Cheese press:** a container station that presses curds.
- **The cheeses:**
  - **Fresh cheese:** curds and salt, pressed lightly.
  - **Aged hard cheese:** curds dry-salted, pressed hard, then aged on the ageing shelf.
  - **Aged soft cheese:** curds and salt, drained and ripened on the ageing shelf.
- **Ageing shelf:** a time station. Its holding times are longer than the crock's.

## Feature-wide constraints and acceptance

- New stations follow B-0045 rules: recipe types, counted matching, progress, saving, breaking, and household permissions inside a cabin. Inside the [B-0041](../B-0041/brief.md) kitchen they use central storage like other kitchen stations.
- Time stations progress only while their chunk ticks, so they pause while an empty cabin is packed. Catch-up remains open under Direction.
- Every new item and recipe appears in the cookbook, with its technique explained.
- Recipes are data, so datapacks can change times and add products.

## Scope

- **Stations:**
  - Taken from B-0053: the prep counter.
  - Its own: the mixing bowl with its spoon, the mill, the proofing basket, the dairy pan, the butter churn, and the cheese press.
  - Taken from B-0048: the fermentation crock and the ageing shelf.
- **Ingredients:** rock salt, taken from B-0047, and cardoon, a new plant.
- **Products:** the flour, starter, dough, pastry, bake, butter, cultured milk, rennet, curd, and cheese chains in the stories.

## Non-goals

- **B-0053:** the strainer, grill, wok, and æbleskive pan, and the strained cheeses, quark and cream cheese, which need the strainer.
- **B-0048:** the drying rack, brine barrel, and smoker, dried yeast, and brined cheeses.
- **B-0050:** rye, corn, rice, and other grains and their flours.
- **B-0049:** calf rennet from butchery. It may later join vegetable rennet as a second source.
- **B-0047 and B-0052:** oils and oil-based doughs.
- **B-0051:** food benefits beyond vanilla values.

## Related records

- [Cooking stations and cookbook](../B-0045/brief.md).
- [Manual kitchen](../B-0041/brief.md).
- [Cooking stations list](../../cooking/stations.md).
- [Intermediate products](../../cooking/intermediates.md).
- [Raw ingredient sources](../../cooking/sources.md).
- [Cooking concept](../../cooking/concept.md).

## Open questions and assumptions

- **Time:** times are in game time. A "day" is one Minecraft day. B-0033 tunes them.
- **Containers:** milk stays in buckets. Cream comes in bottles, skimmed milk in buckets, and yogurt and crème fraîche in bowls. The design may adjust these.
- **Time station form:** the design settles whether time stations hold several batches and how they show progress.
