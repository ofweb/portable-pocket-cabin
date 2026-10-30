# Greenhouse implementation

B-0019 installs four sequential gardens: 5×8, 7×11, 9×20, and 11×29.
Clear heights above the gardening surface are four through seven blocks.
The north entrance stays centered. Growth widens both sides and extends south.
The surface occupies one editable layer above the protected foundation.
It aligns with the corridor floor so the 1×2 entrance permits normal walking.
Default paths use waterlogged stone-brick top slabs. Their covered water hydrates
the vanilla farmland beds in the same layer.
A protected sea-lantern band in the walls lights default crops at every size.
Players can shade plants with their own construction.

Upgrades change old shell blocks and fill added space. They preserve existing
interior blocks, including altered surfaces, plants, inventories, and supports.
The shared corridor relocation system moves the greenhouse during main-room
expansion. Unsupported mod contents can prevent that move under B-0005.

## Packed growth

A saved clock accumulates random-tick selections while the server operates.
It respects `random_tick_speed` and excludes time while the server is stopped.
An empty packed cabin records the clock when its greenhouse suspends.
Redeployment applies plant random ticks at the expected vanilla selection rate.
Each return uses at most 8,192 rounds and stops ticking mature plants.
Only plants receive catch-up ticks; soil, compost, and machines do not.
Catch-up supplies no harvesting, replanting, hydration, or yield bonus.

A recovery journal saves original block states, elapsed selections, and a random
seed before catch-up changes the world. Recovery restores that starting point
and retries growth. Plants are saved before elapsed time is consumed; consumption
is saved before the journal is deleted. Pending recovery blocks garden edits,
packing, and main-room expansion.

Vanilla profiles cover wheat, carrots, potatoes, beetroot, torchflower, pitcher,
melon and pumpkin stems, berries, cocoa, nether wart, sugar cane, cactus, bamboo,
and growing vine or kelp heads. Normal light, soil, water, and support checks apply.
Stems stop at maturity before producing fruit. Spreading mushrooms and unknown
mod plants have no catch-up profile; their normal deployed behavior still applies.

## Optional mods

Botanical costs select registered Biomes O' Plenty ingredients, with shared 1:1
vanilla fallbacks from the brief. Existing funds retain their saved requirements.
A changed ingredient selection uses the existing stale-fund return flow.

Farmer's Delight profiles cover cabbage, onions, budding and mature tomatoes,
and both rice blocks. Rice needs its normal water and soil arrangement. Tomatoes
use their normal supports. The profiles call each block's own growth behavior.
The [tomato source](https://github.com/vectorwing/FarmersDelight/blob/1.21/src/main/java/vectorwing/farmersdelight/common/block/TomatoBlock.java)
and [rice source](https://github.com/vectorwing/FarmersDelight/blob/1.21/src/main/java/vectorwing/farmersdelight/common/block/RiceBlock.java)
informed these profiles. This source review does not verify Fabric 26.2 mod
compatibility. Rich soil, rich farmland, compost, and mushroom colonies keep
their mod behavior during normal simulation.

## Verification

`./gradlew build` runs cost, geometry, light-condition, maturity, and legacy-save
GameTests. Dedicated greenhouse checks cover every expansion, altered surfaces,
fixtures, permissions, lighting, main-room relocation, and interrupted operations
across a server restart. Run those checks with `./gradlew runGreenhouseReloadTest`.

`PPC_TEST_GREENHOUSE=1 ./gradlew runClientGameTest` checks book confirmation and
purchases through the real menu. It captures the book preview, missing materials,
initial and final purchase readiness, completed state, and garden under
`build/run/clientGameTest/screenshots/greenhouse-*.png`.
