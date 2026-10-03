# The cabin has a greenhouse

Status: Ready
Feature ID: B-0019

## Goal

Give the household an editable garden for manual planting and harvesting.

## Stories and acceptance

### S1: Install the greenhouse

Story: An owner installs a greenhouse book and purchases the starting garden.

Acceptance:

- The room branches south from the west corridor, nearest the main room. Its entrance is centered on the north wall.
- One book reveals four purchases under B-0038. Sizes require the previous purchase; only the next cost is shown.
- Manual use requires neither central storage nor automation. Automation books and purchases are separate.
- Installation, traversal, permissions, and persistence follow B-0005.

| Purchase | Usable floor | Clear height above gardening surface | Glass | Iron ingots |
|---|---|---|---|---|
| Initial | 5×8 | 4 | 16 | 8 |
| Upgrade 1 | 7×11 | 5 | 32 | 12 |
| Upgrade 2 | 9×20 | 6 | 48 | 16 |
| Upgrade 3 | 11×29 | 7 | 64 | 24 |

Each purchase requires one of each listed plant. BOP means Biomes O' Plenty. Replacements are shared 1:1 fallbacks under PDR-0014.

| Purchase | Preferred ingredient | Intended biome | Vanilla fallback |
|---|---|---|---|
| Initial | Sunflower | Sunflower Plains | — |
| | BOP lavender | Lavender Field | Allium |
| | BOP barley | Pasture | Wheat seeds |
| Upgrade 1 | Sweet berries | Taiga | — |
| | Cactus | Desert | — |
| | Blue orchid | Swamp | — |
| | BOP clover | Orchard | Dandelion |
| Upgrade 2 | Cocoa beans | Jungle | — |
| | Pink petals | Cherry Grove | — |
| | BOP marigold | Seasonal Forest | Orange tulip |
| | Sea pickle | Warm Ocean | — |
| | BOP white petals | Snowblossom Grove | Lily of the valley |
| Upgrade 3 | Spore blossom | Lush Caves | — |
| | Kelp | Cold Ocean | — |
| | BOP blue hydrangea | Subtropics | Cornflower |
| | BOP icy iris | Auroral Garden | Azure bluet |
| | BOP glowflower | Mystic Grove | Oxeye daisy |
| | BOP toadstool | Fungal Jungle | Red mushroom |

- Purchases require three, four, five, and six new botanical types.
- Preferred ingredients target different biomes across all tiers. Fallback biomes may repeat.
- Ingredients encourage exploration without visit requirements. Collected and traded items count.
- Plants used for funding do not restrict what players can plant.

### S2: Expand the garden

Story: An owner purchases a larger greenhouse while keeping the existing garden intact.

Acceptance:

- Each upgrade increases width, length, and clear height within the room's reserved space.
- Growth widens both sides equally and extends southward. The gardening surface stays at the same height.
- Existing planting spots and paths retain their positions and remain planting spots and paths in the enlarged default pattern.
- Upgrades leave existing plants, soil, water, supports, fixtures, and player alterations in place. Plants keep their growth stages.
- Only added area receives default beds and paths. Existing alterations stay intact.
- There is no layout selection or plant relocation during greenhouse size upgrades.

| Default garden | Beds | Planting spots |
|---|---|---|
| 5×8 | Six crosswise 2×2 beds | 24 |
| 7×11 | Eight crosswise 3×2 beds | 48 |
| 9×20 | Fourteen crosswise 4×2 beds | 112 |
| 11×29 | Twenty crosswise 5×2 beds | 200 |

- A one-block central path runs southward from the entrance at every size.
- All defaults have two-row beds on either side, separated by one-block cross-paths. Beds reach the front and back walls; there are no end cross-paths.

### S3: Rearrange and use the garden

Story: An owner or resident plants, harvests, and changes beds to suit their garden.

Acceptance:

- The gardening surface is one editable block layer above a protected foundation.
- Owners and residents may remove and replace beds and paths, including placing water in that layer. The default arrangement need not be retained.
- Default paths are waterlogged bottom slabs. Their water occupies the same layer as the soil.
- Default beds start as tilled farmland, including newly added beds during expansion.
- Planting, growth conditions, hydration, and harvesting follow normal Minecraft behavior. The greenhouse supplies no yield bonus or automatic hydration.
- The room supplies crop lighting across default beds at every size, including at night. Player-built obstructions can shade crops normally.
- Automatic harvesting and replanting belong to B-0026.
- Intended Farmer's Delight support covers all four crops: cabbage, onions, tomatoes, and rice. Players supply their normal planting environments and supports.
- Rich soil, rich farmland, organic compost, and mushroom colonies retain their mod behavior. The room still supplies vanilla farmland by default.

### S4: Return to a packed garden

Story: A player returns to an empty packed cabin and finds that planted crops made progress.

Acceptance:

- Simulation pauses while the packed cabin is empty. On return, greenhouse plants catch up for elapsed server operating time under suitable growing conditions.
- Catch-up stops at each plant's normal mature state. It does not harvest or replant.
- Restart and repeated entry cannot apply the same elapsed progress twice.

## Scope

Installation, manual gardening, editable beds, supplied lighting, and size upgrades that preserve the existing garden.

## Non-goals

Automatic harvesting and replanting; garden reshaping or plant relocation; arboretum tree-growing behavior.

## Related records

- [Shared room rules](../B-0005/brief.md).
- [Room automation](../B-0026/brief.md).
- [Cabin books](../B-0038/brief.md).
- [Upgrade materials](../../direction.md#upgrade-materials-and-exploration).
- [Shared fallbacks](../../decisions/pdr/0014-use-preferred-upgrade-materials-with-shared-fallbacks.md).
- [Optional mod sources](../../references.md).
- [Compatibility verification](../B-0011/brief.md).
- [Greenhouse catch-up limit](../../direction.md#greenhouse-catch-up).

## Open questions and assumptions

- Slab material is deferred to Design as a presentation choice.
- Design verifies crop profiles and optional mod feasibility. B-0011 owns tested versions; Fabric 26.2 compatibility remains unverified.
