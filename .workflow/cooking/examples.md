# Five recipes, start to finish

These examples show how finished cabin cooking should work, using the stations, heat levels, and intermediate items in the [end-state description](vision.md). Each one is made from scratch. Station names, times, and quantities are illustrative: the owning feature briefs settle them, and B-0033 balances them. A Minecraft day is 20 minutes.

Station kinds work like vanilla blocks:

- **Click:** like a stonecutter. Use a tool on the item.
- **Surface:** like a campfire. Items sit on it and cook.
- **Container:** like a furnace. Slots and a progress arrow.
- **Time:** progresses over time without heat.

## 1. Bread

A loaf of bread is the first real achievement. It shows milling, a living starter, waiting, and fuel choice.

| Step | Station | Inputs | Heat and time | Output |
|---|---|---|---|---|
| Mill | Mill (container) | 2 wheat | — 10 s | Wheat flour |
| Start a culture | Fermentation crock (time) | Wheat flour, water bottle | — 1 day | Sourdough starter |
| Feed the starter | Fermentation crock (time) | Sourdough starter, wheat flour, water bottle | — 5 min | 2 sourdough starter |
| Mix | Mixing bowl (container) | 3 wheat flour, sourdough starter, salt, water bottle | — 10 s | Bread dough |
| Proof | Proofing basket (time) | Bread dough | — 5 min | Risen dough |
| Bake | Oven (container) | Risen dough | Medium, 15 s | Bread |

- **Raw ingredients:** wheat, water, salt. A starter keeps for good if players feed it, so after the first day it is reusable: one starter in, two out.
- **It teaches:** flour comes from milling. Bread rises because of a culture, and that takes time. Baking needs steady medium heat, so coal or logs, not sticks.
- **Intermediate items:** flour, starter, dough, risen dough.
- **First automated:** flour, then dough. A mid-game kitchen keeps 16 flour and a fed starter on hand.

## 2. French onion soup

It looks like soup with bread on top. From scratch it needs a stock, caramelised onions, a loaf, and an aged cheese, and it rewards heat control at every stage.

| Step | Station | Inputs | Heat and time | Output |
|---|---|---|---|---|
| Butcher | Butcher block (click) | Cow carcass | Cleaver use | Beef cuts, 4 beef bones, beef fat |
| Roast bones | Oven (container) | 4 beef bones | High, 20 s | 4 roasted bones |
| Chop | Cutting board (click) | Onion, carrot, celery | Knife use | Mirepoix |
| Simmer stock | Cooking pot (container) | 4 roasted bones, mirepoix, water ×3 | Low, 2 min | 3 beef stock |
| Slice | Cutting board (click) | 4 onions | Knife use | 4 sliced onions |
| Caramelise | Skillet (surface) | Sliced onions, butter | Low, 90 s | Caramelised onions |
| Simmer soup | Cooking pot (container) | Caramelised onions ×4, 2 beef stock, bowl | Low, 1 min | Onion soup base |
| Slice | Cutting board (click) | Bread | Knife use | 4 bread slices |
| Gratinate | Oven (container) | Onion soup base, bread slice, grated aged cheese | High, 10 s | French onion soup |

- **Raw ingredients:** a cow, onions, carrots, celery, wheat for bread, milk for butter and cheese, and salt.
- **It teaches:** caramelising needs low heat for a long time, and high heat would only scorch. Roasting bones first gives a darker stock. Gratinating is a short blast of high heat.
- **Shared intermediates:** beef stock, butter, bread, and aged cheese are each separate chains from features B-0046, B-0047, and B-0049. The soup links to all of them in the cookbook.
- **Intermediate items:** about 12, from 7 raw ingredients.
- **First automated:** beef stock, from bones the kitchen collects once butchery is automated.

## 3. Bratwurst with sauerkraut

Two chains meet on one plate: butchery and sausage making on one side, days of fermentation on the other.

| Step | Station | Inputs | Heat and time | Output |
|---|---|---|---|---|
| Butcher | Butcher block (click) | Pig carcass | Cleaver use | Pork shoulder, pork belly, porkchops, pork fat, pork casing |
| Grind | Meat grinder (container) | Pork shoulder, pork fat | — 10 s | Minced pork |
| Season | Mixing bowl (container) | Minced pork, salt, black pepper, marjoram | — 5 s | Bratwurst filling |
| Stuff | Sausage stuffer (container) | Bratwurst filling, pork casing | — 10 s | 4 raw bratwurst |
| Shred | Cutting board (click) | Cabbage | Knife use | Shredded cabbage |
| Ferment | Fermentation crock (time) | 4 shredded cabbage, salt | — 2 days | 4 sauerkraut |
| Fry | Skillet (surface) | Raw bratwurst | Medium, 30 s | Bratwurst |
| Serve | Plate at the cutting board (click) | Bratwurst, sauerkraut, mustard | — | Bratwurst with sauerkraut |

- **Raw ingredients:** a pig, cabbage, salt, black pepper, marjoram, and mustard seed for mustard.
- **It teaches:**
  - Meat is not just meat. A shoulder suits mince, a belly suits bacon, and the casing comes from the same animal.
  - Salt draws water from cabbage, and the brine lets it ferment safely.
  - Sausage frying needs medium heat. High heat would split the casing.
- **Fermentation:** the crock runs for two Minecraft days, so players start sauerkraut early and do other things meanwhile. One batch serves many meals.
- **Intermediate items:** about 10.
- **First automated:** sauerkraut, a classic "start it and forget it" job. Butchery and sausages come later.

## 4. Miso soup

A light soup built on two of the most complex ingredients in the game: dashi and miso. It shows umami, drying, smoking, and long fermentation.

| Step | Station | Inputs | Heat and time | Output |
|---|---|---|---|---|
| Dry kelp | Drying rack (time) | Kelp | — 5 min | Kombu |
| Fillet | Butcher block (click) | Bonito | Knife use | 2 bonito fillets, fish bones |
| Smoke | Smoker (container) | Bonito fillet | Low, 10 min | Smoked bonito |
| Dry | Drying rack (time) | Smoked bonito | — 1 day | Katsuobushi |
| Shave | Cutting board (click) | Katsuobushi | Knife use | 8 bonito flakes |
| Steam rice | Cooking pot (container) | Rice, water | Low, 30 s | Steamed rice |
| Culture koji | Fermentation crock (time) | Steamed rice, koji spores | — 1 day | Koji rice, koji spores |
| Cook soybeans | Cooking pot (container) | 4 soybeans, water | Low, 1 min | Cooked soybeans |
| Mash | Mortar and pestle (click) | Cooked soybeans | Pestle use | Soybean mash |
| Age miso | Fermentation crock (time) | Soybean mash, koji rice, salt | — 5 days | 4 miso |
| Steep dashi | Cooking pot (container) | Kombu, bonito flakes, water ×3 | Low, 30 s | 3 dashi |
| Make tofu | Container chain in B-0046 | Soybeans, water, nigari | Low, then press | Tofu |
| Finish | Cooking pot (container) | Dashi, miso, tofu, spring onion, bowl | Low, 10 s | Miso soup |

- **Raw ingredients:** kelp, bonito, rice, soybeans, salt, spring onion, and koji spores.
- **It teaches:**
  - Umami comes from extraction and fermentation.
  - Dashi takes minutes once kombu and katsuobushi exist, but each of those takes a day.
  - Miso must never boil, so it is added last at low heat.
- **Koji:** a culture batch returns its koji spores, like the sourdough starter, so the first spores are the only hard part.
- **Intermediate items:** about 14. The finishing step is ten seconds, and everything before it is days.
- **First automated:** dashi and miso.

## 5. Æblemost

A short chain, for contrast. Pressed apple juice the way Danish farms make it.

| Step | Station | Inputs | Heat and time | Output |
|---|---|---|---|---|
| Wash and quarter | Cutting board (click) | 4 apples | Knife use | 4 apple quarters |
| Crush | Mill (container) | 4 apple quarters | — 10 s | Apple mash |
| Press | Fruit press (container) | Apple mash, 2 glass bottles | — 20 s | 2 æblemost, apple pomace |
| Settle | Time station: cellar shelf | Æblemost | — 5 min | Clear æblemost (optional) |

- **Raw ingredients:** apples and bottles.
- **It teaches:** juice is pressed, not cooked, and the cloudy and clear versions differ only by settling time.
- **By-product:** apple pomace feeds pigs or the compost bin. Nothing is wasted.
- **First automated:** pressing, once an orchard exists.

## What the five show together

- **Shared hubs matter most:** flour, salt, stock, butter, cheese, and starter cultures appear across these dishes. Building them first unlocks the most food.
- **Time is a resource:** fermentation and drying take Minecraft days, so players plan ahead and run several things at once. Stations keep progress while players are elsewhere.
- **Heat is a choice:** low for caramelising, steeping, and smoking; medium for baking and frying sausages; high for roasting bones and gratinating.
- **Complexity is hidden in "simple" food:** miso soup and French onion soup look easy and are the deepest chains here.
- **Automation follows naturally:** the steps players repeat most, such as flour, stock, sauerkraut, and dashi, are the first they want the cabin to take over.

## Gaps these examples expose

- **Salt:** almost every recipe needs it, but no feature owns it yet. Proposal: B-0047 owns salt, from seawater boiled down in a pot at high heat and from rock salt in caves.
- **Cultures:** sourdough starter and koji need a first source. Proposal: a starter begins from flour and water in a crock. Koji spores come from a villager trade or a rare find, and each batch returns some.
- **New animals and fish:** bonito needs to exist as a catchable fish. Proposal: B-0049 adds it as fishing loot in warm oceans.
- **Smoker:** this assumes the vanilla smoker becomes the smoking station, burning low-heat fuel for cured and smoked foods. Proposal: B-0048 owns this change.
- **Serving step:** plating at the cutting board is a light assembly step. It may be better as a crafting-free "plate" station, or as the cutting board's second use.
- **Durations:** these are first guesses. Real proofing takes hours and miso takes months. Minecraft needs shorter times that still feel like waiting.
