# The kitchen gets a prep counter and specialty stations

Status: Implemented
Feature ID: B-0053

## Goal

Real kitchens do most of their hand work on a counter: shaping dough, rolling wrappers, filling and skewering, assembling a plate. They also use a few special pans and grills. This feature adds:

- The **prep counter**, as the work surface that counter-top stations sit on.
- The **strainer**.
- The **grill**, **wok**, and **æbleskive pan**.
- **Oil in the cooking pot** for deep-frying.

It ships first in the [cooking program](../../cooking/program.md), because the counter and strainer are used by nearly every later cooking feature.

## Stories and acceptance

### S1: Work on the prep counter

Story: A player places a prep counter and does hand work on it, or sets counter-top stations on it.

Acceptance:

- **Prep counter:** a full block, crafted from planks and a stone slab. It sits on the floor like a cabinet.
- **Hand work:** with nothing on top, the counter is a station of its own. Ingredients go into its slots, and a recipe shapes, rolls, fills, wraps, skewers, layers, or assembles them. Hand work takes no heat and a short time.
- **Counter-top stations:** a station placed on top of a counter sits on it, as a pot sits on a stove. These are the B-0045 cutting board, the strainer, the B-0046 mixing bowl, and the B-0047 mortar and pestle. The counter's own hand work pauses while a station stands on it.
- **Placement rule:** counter-top stations from this feature and later ones need a counter under them. The B-0045 cutting board keeps working anywhere, so existing kitchens don't break.
- **Tools:** skewers are a tool, made from sticks. Skewering takes them from the player's inventory.

### S2: Strain and drain

Story: A player strains a liquid from its solids, or drains washed solids.

Acceptance:

- **Strainer:** a counter-top container station. It has an input, a liquid output, and a solid output.
- **Uses:** straining, draining, sieving, settling starch, and leaching lye. Recipes say which outputs each step gives.
- **Time:** short, with no heat.

### S3: Grill

Story: A player grills skewers, meat, and fish over a fire.

Acceptance:

- **Grill:** a surface station with its own fuel slot, like the oven. Fuel sets its heat under the B-0045 fuel tags.
- **Holding:** it holds up to four items or skewers, like the skillet, and cooks each on its own.
- **Phase 1 dishes:** meat and vegetable skewers, assembled on the counter from vanilla meats, potatoes, and carrots.
- **Fuel:** the B-0045 fuel rules apply. Lit fuel always burns down, a shovel puts it out, and hoppers add fuel from the side.

### S4: Stir-fry in the wok

Story: A player stir-fries over high heat.

Acceptance:

- **Wok:** a container station that sits on a stove at high heat.
- **Oil:** stir-frying needs a little oil, filled like the pot's water. Oil fills from oil bottles.
- **When it ships:** wok recipes need cooking oil, which B-0052 adds in phase 2. Under the program's [rule for shared chains](../../cooking/program.md#rule-for-shared-chains), they ship then.

### S5: Cook in round wells

Story: A player cooks batter in an æbleskive pan and turns each ball as it cooks.

Acceptance:

- **Æbleskive pan:** a surface station on a stove at medium heat, with seven wells.
- **Turning:** batter is poured into a well. Halfway through, the player turns it with a skewer, as Danes turn æbleskiver with a knitting needle. An unturned ball stops at half done and doesn't burn.
- **When it ships:** batters come from B-0046, so pan recipes ship in phase 3.

### S6: Deep-fry in the pot

Story: A player fills a cooking pot with oil and deep-fries.

Acceptance:

- **Oil level:** the pot holds water or oil, never both, up to three uses, like water. Oil bottles fill it and give back empty bottles.
- **Frying:** deep-frying recipes need oil and high heat. Each batch uses one level, as oil degrades with use.
- **Emptying:** a bucket or bottle can't take oil back out. The oil empties when it is used up, or when the pot is broken.
- **When it ships:** deep-frying recipes ship in phase 2 with the first oil.

## Feature-wide constraints and acceptance

- New stations follow B-0045 rules: recipe types, counted matching, progress, heat, saving, breaking, hoppers, and household permissions inside a cabin. Inside the [B-0041](../B-0041/brief.md) kitchen they use central storage like other kitchen stations, including oil and fuel filling from storage.
- Every new station, tool, and recipe appears in the cookbook, with its technique explained.
- Recipes are data, so datapacks can add counter, strainer, grill, wok, and pan recipes.

## Scope

- **Stations:** the prep counter, strainer, grill, wok, and æbleskive pan.
- **Tools and pot changes:** skewers, and oil in the cooking pot.
- **Phase 1 recipes:** grilled skewers from vanilla ingredients.
- **Later recipes:** all other recipes in the [recipe catalogue](../../cooking/recipe-catalogue.md) and [intermediate products](../../cooking/intermediates.md) that use these stations. They ship as their ingredients arrive.

## Non-goals

- **B-0052:** cooking oil itself.
- **B-0046 and B-0047:** the mixing bowl and the mortar and pestle.
- **B-0048:** the smoker.
- **Outdoors:** charcoal grills outside the kitchen beyond the grill station, and barbecue pits.

## Related records

- [Cooking program](../../cooking/program.md).
- [Cooking stations and cookbook](../B-0045/brief.md).
- [Manual kitchen](../B-0041/brief.md).
- [Cooking stations list](../../cooking/stations.md).

## Open questions and assumptions

- **Proposals for review:** the cutting board keeping working off the counter, the æbleskive turning step, and oil being used up per batch.
- **Times:** B-0033 tunes times and the oil use per batch.
