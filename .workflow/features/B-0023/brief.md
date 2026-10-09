# Arboretum opens into woodlands

Status: Designed
Feature ID: B-0023

## Goal

A household arboretum opens into woodlands.

## Stories and acceptance

### S1: Add the arboretum

Story: An owner installs an arboretum.

Acceptance:

- The arboretum connects to the south side of the west corridor.
- An interior block opens a planting interface controlling only the cultivated core.
- Installation and persistence follow B-0005; oak starts selected.
- Cabin entry never waits for woodland loading.
- Doors use normal transfers and loading with safe arrivals; errors only on failure, no preparation step. Entry always reaches the core; exit the cabin.

### S2: Expand the arboretum

Story: An owner expands and selects tree types.

Acceptance:

- Enclosed tiers (usable floor / clear height): 7×7 / 5, 19×19 / 10, 29×29 / 15 blocks; then woodlands around the same core.
- Keep at least one selected tree type; tier limits are 1, 2, 3, and 4.
- Fixed core grid: centered 2×2 in 7×7, four blocks apart. Extend with at least one floor block before enclosed walls: 4, 20, 42 positions.
- Selected species mix randomly across the grid.
- Selection replaces deselected core saplings and their transition counterparts, filling available spots. Grown trees and biome planting stay intact; new transition planting uses current selections.
- Fixed entrance centered on the north wall; expand equally east–west and southward, preserving floor height and contents.
- Core accepts any tree type regardless of biome or payment species.
- Purchases fund planting and replanting.
- No sapling drops; retained saplings replant trees without entering inventory or storage.

Purchases require one of each sapling. BOP means Biomes O' Plenty; parentheses give 1:1 vanilla fallbacks under [PDR-0014](../../decisions/pdr/0014-use-preferred-upgrade-materials-with-shared-fallbacks.md).

| Purchase | Required saplings |
|---|---|
| Initial | Oak, birch, spruce, BOP flowering oak (cherry) |
| 19×19 | Jungle, acacia, BOP red maple (cherry), BOP willow (mangrove propagule) |
| 29×29 | Dark oak, cherry, BOP fir (spruce), BOP mahogany (jungle) |
| Woodlands | Pale oak, BOP redwood (jungle), BOP snowblossom (cherry), BOP pine (spruce) |

### S3: Explore woodlands

Story: A player explores woodlands.

Acceptance:

- Effectively endless woodlands surround the freestanding entrance wall (29 blocks wide, 15 tall). Players can become lost and walk around either end into woodland behind it.
- No visible ceiling, including after clearing trees.
- Biomes use Overworld scale and gentle terrain; types follow [Direction](../../direction.md#arboretum-woodlands).
- First ring, with irregular boundaries: south oak, west birch, east flower forest, northwest taiga, northeast snowy taiga. BOP forests appear farther out.
- Two guaranteed second-ring orchards, one of each type, lie beyond western birch and eastern flower forest.
- First-ring ground: roughly ±3 blocks from cultivated-floor height.
- Occasional gentle freshwater forest ponds: roughly 5–12 blocks across, 1–3 deep, with gently sloping banks.
- Orchard biomes pair apple with pear, and orange with peach. Flat except ponds, with regularly spaced trees in rows and grassy gaps. Each pair replants independently of core selections.
- The 29×29 core blends through a 51×51 transition: grid alignment and spacing gradually loosen as selected types blend into biome species, without abrupt boundaries.
- Gradual markers near 39×39: tiny snow patches toward snowy woodland, or isolated undergrowth; near 45×45: critters and snow cover toward snowy woodland. Undergrowth stays light until normal biome undergrowth beyond 51×51.
- Transition trees remain harvestable; clearing alters its appearance.
- Fruit features require compatible Fruits Delight support: orchard and sparse natural fruit per [Direction](../../direction.md#arboretum-woodlands), retaining existing recipes.
- Sparse wildlife: cows, pigs, sheep, rabbits, bees, and suitable birds and critters. Alex's Mobs supplies optional species through [compatibility profiles](../B-0011/brief.md).
- No dangerous wildlife; bees may sting normally. Exclude goats, seagulls, crows, and monsters.
- Core, transition, and woodlands connect without transfers, visible loading, camera jumps, or warnings, including after clearing trees and looking back.

### S4: Return home

Story: A player uses a landmark to return home.

Acceptance:

- Protected, permanent doors and lamps: open and cross either side of a door to return beside the cabin's arboretum entrance; use a nearby safe spot if blocked. No item required.
- Landmarks scatter about 200–300 blocks apart from layer 4, inside biomes or at borders; some biomes have none. Layers count fewest regions from core; first-ring forests are layer 1.
- Small clearings have short, discreet paths revealing lamps. Regrowth keeps them open; no woodland-wide network.

### S5: Revisit reclaimed woodlands

Story: A player revisits reclaimed woodlands.

Acceptance:

- Packing, travel, and restart preserve woodland, replanting, and reclamation state.
- Occupied packed cabins operate normally, including trees, fruit, and animals; empty packed cabins pause. Animals are saved without catch-up breeding, aging, deaths, or drops.
- Felled trees replant after 30 seconds; blocked planting spots wait. Catch-up accounts for that delay before growth.
- Tree remnants without ground connection vanish without drops when the arboretum has no players.
- Cultivated builds remain permanent. Terrain repair restores original materials and pond water in clear spaces, waiting for player-placed blocks to be reclaimed.
- Becoming empty triggers one reclamation cycle, including last-player disconnect. Remove one third of eligible player-placed woodland blocks and repair one third of missing terrain blocks, each rounded up. Reclaimed blocks vanish without drops; manual breaking retains normal drops.
- Containers stay protected for four reclamation cycles after placement, then join removal. Removed containers and contents vanish without drops or recovery.
- Catch-up plants, replants, and grows trees toward maturity under suitable conditions using elapsed server operating time.
- Fruit catch-up stops at normal ripeness without extra accumulation; replanted trees grow before fruiting.
- Catch-up never harvests or counts time twice.

## Scope

Installation, cultivation, exploration, and landmarks.

## Non-goals

- Automatic felling and output collection (B-0026).
- Travel into external forests.
- Changes to Fruits Delight recipes or prepared-food effects.

## Related records

- [Backlog](../../backlog.md#b-0023-forestry-room).
- [Rooms](../B-0005/brief.md).
- [Automation](../B-0026/brief.md).
- [Entrance experiment](../../../experiments/arboretum-transfer/README.md).
- [Allocation](../../decisions/adr/0010-give-each-arboretum-a-private-world.md).
- [Runtime-world experiment](../../../experiments/arboretum-worlds/README.md).
- [Bundled fruit component](../../decisions/adr/0011-bundle-a-separate-lgpl-fruit-component.md).
- [Persistence](../../decisions/adr/0012-keep-recoverable-forest-records-by-chunk.md).

## Open questions and assumptions

- Allocation and transfer experiments passed. Fruit port compatibility remains unverified.
