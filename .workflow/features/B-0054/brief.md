# Players can grow every food they find

Status: Ready
Feature ID: B-0054

## Goal

Every plant food a player can find, they can also grow. Once they hold a seed, cutting, sapling, spore, or a piece of the plant itself, planting it gives more than they put in, in a field, in water, or in the cabin greenhouse. The same goes for shellfish: mussel and oyster beds spread, and prawn traps work in any water.

Finding the first seed can still take a long trip, a lucky drop, or a wandering trader's visit. This feature doesn't promise an easy source. It promises that no food is forage-only.

It follows the [cooking program](../../cooking/program.md). [B-0050](../B-0050/design.md) added the plants and [B-0049](../B-0049/design.md) the shellfish. An audit of both found these gaps:

- **Apples** only drop at random from oak and dark oak leaves. There is no tree to tend for them.
- **Sea plants and shellfish don't multiply.** Wakame, laver, and sea lettuce replant from what they give, but they never grow or spread. Planting one gives about one back. Mussel and oyster beds are the same.
- **Cardoon** gives back only the one plant it grew from, so a patch can't grow.
- **Prawn traps** only catch in warm and lukewarm oceans and mangrove swamps, so prawns can't be raised in a pond.
- **The greenhouse** can't grow some kinds:
  - Its catch-up only advances blocks with an `age`, so fruit-tree saplings don't grow while the cabin is packed or away.
  - Cinnamon trunks don't regrow bark, and palm trunks don't fill with sap.
  - Its glass roof counts as shade, so tea there only ever gives shaded leaves.

## Stories and acceptance

### S1: Grow apples

Story: A player plants an apple sapling and picks apples from the tree, season after season.

Acceptance:

- **Apple tree:** an apple tree species like the plum. A sapling grows into an oak-wood tree with fruiting leaves that flower, then bear apples. Picking them leaves the tree to fruit again.
- **Saplings:**
  - Breaking apple leaves sometimes drops a sapling, as other fruit trees do.
  - Oak and dark oak leaves rarely drop an apple sapling.
  - Wandering traders sometimes sell one.
- **Vanilla stays:** oak and dark oak leaves still drop apples as in vanilla.
- **Where it grows:** the tree also grows wild in forests, flower forests, and plains, like the plum tree.

### S2: Every plant multiplies

Story: A player plants any cooking plant they have found and harvests more than they planted.

Acceptance:

- **Every plant in the sources:** each plant food in the [raw ingredient sources](../../cooking/sources.md) can be planted from something it gives, and a grown plant gives back more than went in.
- **Sea plants:**
  - Wakame, laver, and sea lettuce grow taller or thicker over time.
  - A grown plant gives more than one when harvested.
  - They spread slowly to suitable water beside them.
  - Bone meal helps them grow, as it does for kelp.
- **Shellfish:** see S3.
- **Rosehips:** they stay picked from vanilla rose bushes. Rose bushes already multiply with bone meal.
- **Cardoon:** a grown cardoon gives back more than the one it grew from, so a patch can grow.

### S3: Farm shellfish

Story: A player sets mussels and oysters in water and raises prawns in a pond.

Acceptance:

- **Beds spread:** a mussel or oyster bed placed underwater on a solid floor grows over time. A grown bed gives more shellfish when gathered by hand, and slowly spreads to suitable floor beside it.
- **Where beds grow:** players' beds grow in any water, not only the biomes where they appear wild.
- **Prawn traps:** a trap catches prawns in any water.
  - In the waters where prawns live wild, warm and lukewarm oceans and mangrove swamps, it catches faster.
  - It still holds up to eight, and still catches up after time away.
- **Greenhouse and cabin water:** shellfish beds and prawn traps work in water inside the cabin too.

### S4: Grow everything in the greenhouse

Story: A player grows any food plant in the cabin greenhouse, and it keeps growing while the cabin is packed or away.

Acceptance:

- **Every kind:** field crops, trellis vines, bushes, paddy crops in water, fruit trees, cinnamon and palm trunks, sea plants and shellfish in water, and mushrooms on logs all grow in the greenhouse when their growing needs are met.
- **Catch-up:** greenhouse catch-up advances every kind as it does vanilla crops:
  - Saplings grow into trees.
  - Cinnamon bark grows back, and palm trunks fill with sap.
  - Sea plants and shellfish beds grow.
- **Room for trees:** a fruit tree grows only where it fits. A sapling with too little headroom waits, and the cookbook's Garden page says how much room each tree needs.
- **Tea under glass:** glass and other blocks that let light through don't count as shade. Tea under the greenhouse glass gives tea leaves. Under an opaque roof it gives shaded tea leaves.

### S5: Know how to grow it

Story: A player checks the cookbook to learn how to grow what they've found.

Acceptance:

- **Garden pages:** each plant's page says what it is planted from, what it needs, and that it multiplies.
- **Page coverage:** sea plants and shellfish beds have pages too.
- **Spreading plants:** the page says when a plant spreads on its own.
- **Getting started:** the page names where the first planting item comes from: wild plants, leaf drops, or wandering traders.

## Feature-wide constraints and acceptance

- **Growing needs as data:** new growth rules follow [B-0050](../B-0050/design.md). Needs and spread rates are data, so datapacks can change them.
- **Check against the sources:** a check lists every plant food in the sources and fails if one can't be planted from what it gives, or doesn't multiply. A future plant can't arrive forage-only unnoticed.
- **Getting the first item isn't guaranteed:** some planting items stay rare, far away, or trade-only.
- **No change to vanilla:** vanilla crops and vanilla apple drops behave as they do today.

## Scope

- **Apples:** the apple tree and its saplings.
- **Growth:** sea plant growth and spreading, and shellfish bed growth and spreading.
- **Prawn traps:** catching in any water, faster in warm waters.
- **Greenhouse:** catch-up for saplings, trunks, sea plants, and shellfish, and tea under glass.
- **Cookbook:** Garden pages for every growable food.
- **The check:** a check that every plant food in the sources can be grown.

## Non-goals

- **Animals and fish:** breeding animals and farming finned fish belong to the [livestock room](../B-0022/brief.md) and [aquatic berth](../B-0021/brief.md). Ducks already breed.
- **Easier first items:** making every planting item easy to obtain is out of scope. Exploring, rare drops, and trades stay as they are.
- **Arboretum trees:** they belong to [B-0023](../B-0023/brief.md).
- **Balance:** growth speeds and yields are first guesses for [B-0033](../../backlog.md#b-0033-survival-balance).

## Related records

- [Cooking crops and foraging design](../B-0050/design.md).
- [Butchery and seafood design](../B-0049/design.md).
- [Greenhouse brief](../B-0019/brief.md).
- [Raw ingredient sources](../../cooking/sources.md).

## Open questions and assumptions

- **Sea plant growth:** a growth stage and a harvest yield, as sweet berries have, is assumed. A tall plant, like kelp, is the alternative. The design picks one.
- **Spread rate:** spreading is assumed to be slow, so a bed or patch doubles over several in-game days, and to stop when no suitable floor is beside it.
- **Prawn traps:** away from prawn waters, a trap is assumed to catch at about half the rate.
