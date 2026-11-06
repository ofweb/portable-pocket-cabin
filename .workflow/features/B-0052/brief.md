# Players press juice and oil and make drinks

Status: Ready
Feature ID: B-0052

## Goal

Players press fruit into juice and seeds into oil. They cook juice and sugar into syrups and cordials, and drink what they make. A press shows how much fruit goes into one bottle of juice, and how much seed goes into one bottle of oil. Cooking oil is the first fat for frying, so this feature opens the wok and deep-frying from [B-0053](../B-0053/brief.md).

It ships in phase 2 of the [cooking program](../../cooking/program.md).

## Stories and acceptance

### S1: Press

Story: A player presses prepared fruit or seeds into a liquid.

Acceptance:

- **Press:** a container station without heat. Prepared fruit or seeds go into its input. Glass bottles go into its container slot. Pressing gives bottles of juice or oil and leaves the solids behind.
- **Solids:** apple pomace, pulp from berries, melon, and carrots, and seed press cake. They compost like plant matter. Press cake also feeds farm animals the way wheat does.
- **Time:** short, with no heat.

### S2: Press juice

Story: A player presses fresh juice and drinks it.

Acceptance:

- **Apple juice:** apples quartered on the cutting board, then pressed. In Denmark this is æblemost.
- **Sweet berry juice:** sweet berries, pressed.
- **Melon juice:** melon slices, pressed.
- **Carrot juice:** carrots grated on the cutting board, then pressed.
- **Drinking:** juices are drinkable, return the glass bottle, and restore a little hunger.

### S3: Cook syrups and cordials

Story: A player cooks fruit with sugar into a cordial, or reduces juice into a syrup.

Acceptance:

- **Sweet berry cordial:** sweet berries, sugar, and water simmered in the cooking pot at low heat, strained in the B-0053 strainer, and bottled.
- **Apple syrup:** apple juice reduced slowly in the cooking pot at low heat.
- **Dilution:** cordials are diluted with water into drinks such as saftevand in the B-0046 mixing bowl, so diluted drinks ship in phase 3.

### S4: Press oil

Story: A player presses seeds into cooking oil.

Acceptance:

- **Sunflower oil:** the everyday frying oil.
  - Sunflowers dry on the plant like real seed heads. Threshing one on the cutting board gives sunflower seeds.
  - The seeds are pressed cold into sunflower oil.
- **Pumpkin seed oil:** the finishing oil.
  - Pumpkin seeds are toasted on the skillet at medium heat, then pressed, as Styrian pumpkin seed oil is.
  - It is dark and nutty, and the cookbook says it is for dressing, not frying.
- **Where oil goes:** oil bottles fill the B-0053 wok and cooking pot for stir-frying and deep-frying. Recipes say which oil they need. Any frying oil works where a recipe asks only for oil.

### S5: Ship the frying recipes

Story: With oil available, players stir-fry and deep-fry.

Acceptance:

- **Deep-frying:** recipes in the cooking pot with oil at high heat ship here, for those whose other ingredients exist, such as fried potatoes.
- **Stir-frying:** wok recipes ship here on the same condition.

## Feature-wide constraints and acceptance

- The press follows B-0045 rules: recipe types, counted matching, progress, saving, breaking, hoppers, and household permissions inside a cabin. Inside the [B-0041](../B-0041/brief.md) kitchen it uses central storage like other kitchen stations.
- Every new item and recipe appears in the cookbook, with its technique explained.
- Recipes are data, so datapacks can add juices and oils.

## Scope

- **Station:** the press.
- **Products:** juices, cordials, syrups, sunflower and pumpkin seed oil, and the pomace, pulp, and press cake left behind.
- **Phase 2 frying recipes:** the B-0053 frying recipes whose ingredients exist.
- **Later:** the rest of the drinks, syrups, infusions, and oils in the [recipe catalogue](../../cooking/recipe-catalogue.md) and [intermediate products](../../cooking/intermediates.md). They ship as their ingredients arrive. These include citrus juices, olive, sesame, and rapeseed oil, cocoa butter, mint and cassis syrups, teas, and chocolate drinks.

## Non-goals

- **Drink kinds:** alcoholic drinks, distilling, and carbonation. B-0048 ferments cider only as the step towards cider vinegar.
- **B-0051:** drink benefits beyond hunger.

## Related records

- [Cooking program](../../cooking/program.md).
- [Prep counter and specialty stations](../B-0053/brief.md).
- [Cooking stations and cookbook](../B-0045/brief.md).
- [Cooking vision](../../cooking/vision.md).

## Open questions and assumptions

- **Change from the program's first choice:** sunflower oil replaces pumpkin seed oil as the frying oil. Toasted pumpkin seed oil smokes at low heat, so real cooks use it for dressing. Sunflowers are vanilla, and sunflower oil is a real everyday frying oil.
- **Pressing ratios and hunger values:** B-0033 tunes them.
