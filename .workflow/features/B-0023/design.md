# B-0023 design, part 1: the enclosed grove

Status: Ready
Feature ID: B-0023

This design covers the first part of the [B-0023 brief](brief.md): the arboretum door, the three enclosed grove sizes, choosing and teaching tree types, planting and replanting, and catch-up growth. The woodlands (S3), return landmarks (S4) and woodland reclamation (S5, except grove catch-up) come in later parts. This design fixes where each cabin's grove lives so the woodlands can grow around it without moving it.

## Changes to the brief

The user agreed these changes on 2026-11-28:

- **Clear heights are 9, 12 and 15.** The brief gave 5, 10 and 15, but most trees are taller than five blocks. Usable floors stay 7×7, 19×19 and 29×29.
- **Tree types are taught with saplings.** A tree type can be selected only after the household teaches the grove that type with 15 of its saplings. The saplings are consumed. This applies to vanilla trees and to the mod's fruit and spice trees alike, so trees found by exploring stay exploration rewards. This replaces "core accepts any tree type" with "core accepts any taught tree type, regardless of biome or payment species".
- **Sizes also cost logs and glass.** Each purchase keeps its four saplings and adds wall logs and glass:

| Purchase | Saplings, one each | Wall logs | Glass |
|---|---|---|---|
| Grove 7×7 | Oak, birch, spruce, BOP flowering oak (cherry) | 16 | 32 |
| Grove 19×19 | Jungle, acacia, BOP red maple (cherry), BOP willow (mangrove propagule) | 32 | 48 |
| Grove 29×29 | Dark oak, cherry, BOP fir (spruce), BOP mahogany (jungle) | 48 | 64 |

Wall logs are the cabin palette's wall log, as for the enchanting room. Biomes O' Plenty saplings use the shared fallbacks of [PDR-0014](../../decisions/pdr/0014-use-preferred-upgrade-materials-with-shared-fallbacks.md).

## Shape of the change

The arboretum is the first room that is not part of the pocket dimension. It adds these parts:

- **State:** a new `CabinArboretumState` in `CabinUpgradeState`.
- **Book and purchases:** the `arboretum_book` and three `Target.arboretum(level)` purchases.
- **Dimension:** a new `arboretum` dimension holding every cabin's grove, each in its own region.
- **Door:** an arboretum door in the cabin's west corridor, and an exit door in the grove's north wall.
- **Planting board:** a block in the grove that opens the tree-type screen.
- **Planting:** a server service that plants and replants the grid and catches up growth.

## Where the grove lives

### The arboretum dimension

- `portable_pocket_cabin:arboretum` uses a flat generator with no layers, like the pocket, and a new biome, `portable_pocket_cabin:arboretum_grove`.
- The biome has plains colours and no mob spawns, so nothing spawns on the grove's grass.
- The dimension type has skylight and the overworld clock, so the grove has normal days and nights through its glass roof.
- Its height runs from y 0 to 384. The grove floor's grass is at y 64, which leaves room for woodland terrain below and tall trees above later.

### One region per cabin

Each cabin's grove has a fixed region, found from the cabin's cell index:

- Regions are 32,768 blocks apart in a grid of 1,800 columns, centred on the dimension origin.
- Cell index `n` maps to column `n % 1800` and row `n / 1800`. The region centre is `(-29,474,816 + 32,768 × column, 64, -29,474,816 + 32,768 × row)`. Every region stays inside the world border.
- The region centre is the grove's anchor: the inside of the exit door, on the grove floor.
- The woodlands in part 2 will fill the rest of the region, which gives each cabin about 16,000 blocks of woodland in every direction.
- A cabin with a cell index of 3,240,000 or more has no region and can't buy the arboretum. No real world reaches that number of cabins.
- `CabinArboretum.cabinAt(pos)` finds the cabin from a grove position by inverting the mapping.

Part 2 replaces the dimension's generator with the woodland generator. Chunks generated as empty in part 1 stay empty, so part 2 must regenerate the chunks around each grove that already exists before opening the woodlands. Part 2 owns that migration.

## The grove

### Coordinates

Grove coordinates are relative to the anchor: x runs east, z runs south, and y 0 is the grass floor.

| Size | Level | Usable floor x | Usable floor z | Clear height (y 1 to) |
|---|---|---|---|---|
| 7×7 | 1 | -3 to 3 | 0 to 6 | 9 |
| 19×19 | 2 | -9 to 9 | 0 to 18 | 12 |
| 29×29 | 3 | -14 to 14 | 0 to 28 | 15 |

The shell is one block outside the usable space:

- **Walls:** at x `±(half + 1)`, at z -1 (north), and at z `size` (south), from y -3 up to the roof. They use the palette's wall planks, with wall logs on the corners.
- **Roof:** glass at y `height + 1`.
- **Ground:**
  - Grass at y 0, dirt at y -1 and -2, and a foundation of the palette's floor planks at y -3.
  - The grass and dirt are editable.
  - The walls, roof, foundation and doors are protected.
- **Exit door:** at (0, 1, -1) and (0, 2, -1), centred in the north wall, facing south.
- **Lighting:** lanterns hang on the inside of the walls every four blocks at y 4. They light the edges at night without lighting the planting grid, so saplings grow by daylight through the roof.

### Planting grid

Grid spots are at x ∈ {-10, -6, -2, 2, 6, 10} and z ∈ {1, 5, 9, 13, 17, 21, 25}, inside the usable floor with at least one floor block before each wall:

- **7×7:** 2 × 2 = 4 spots.
- **19×19:** 4 × 5 = 20 spots.
- **29×29:** 6 × 7 = 42 spots.

Each size keeps every spot of the previous size. A spot is the block above the grass at that position.

### Planting board

The planting board is a protected block on the floor at (1, 1, 0), just east of the exit door. Using it opens the tree-type screen.

### Expansion

Each purchase grows the grove equally east and west and further south. The north wall and floor height stay where they are:

- The old side and south walls, and the old roof, become air.
- New floor gets grass and dirt over the foundation. Existing floor, trees and anything players placed stay as they are.
- The new walls, roof and lanterns are placed.
- The validation refuses if any new shell or floor block is neither air nor an old shell block. That can only happen if something was placed outside the grove by other means.

Each step is a plain block placement, so recovery repeats the same effect. As in the greenhouse, the upgrade service records the installation before the world effect runs.

## Doors

### The cabin's arboretum door

The door sits in the west corridor's south wall at relative x `-(s/2) - 29`, z 2, y 1 and 2, where `s` is the main room width.

- It is the `arboretum_door` block: a two-block door that can't be opened or broken, drawn as a wooden door with leaves.
- Installing the first grove size places it and installs the west corridor if needed, as the greenhouse does.
- The door is a structural block of the west corridor. Main room expansion moves it with the west wing, like the corridor walls.

### Walking through

**Using the arboretum door:**
- It takes the player to the grove anchor, (0, 1, 0), facing south.
- It works for owners, residents and guests while the cabin is deployed, no cabin operation is in progress, and no grove catch-up is running.
- The grove chunks are already loaded (see Simulation), so arrival doesn't wait for generation.

**Using the exit door:**
- It returns the player to the corridor block north of the arboretum door, facing north.
- If that block is obstructed, the player goes to the nearest safe block in the corridor. Failing that, they go to the main room.

**Players caught in the grove:**
- **Packing:** players in the grove are occupants of the cabin, so packing evacuates them outside the exterior as it does for the pocket. `CabinReconciliation.isOccupant` includes the grove.
- **Logging in after the cabin was packed or moved:** players in the grove are evacuated by the same rules as the pocket. Disconnecting in the grove records the cabin stay as it does in the pocket.
- **Main room expansion:** it doesn't wait for players in the grove, because the grove doesn't move. The exit door leads to the door's new position.

## Tree types

### Taught and selected

A tree type is a sapling: the vanilla oak, birch, spruce, jungle, acacia, dark oak, mangrove, cherry and pale oak saplings, and every fruit tree sapling of the mod. The elder is planted from its cutting.

- **Teaching:** the household teaches a type at the planting board by giving 15 of its saplings.
  - The saplings come from installed storage first, then the inventory, as other rooms fill ingredients. Named and customised stacks are skipped.
  - Teaching is permanent for the cabin.
- **Selecting:** owners and residents select up to 1, 2 or 3 taught types for sizes 7×7, 19×19 and 29×29. Guests can look but not change anything.
- **Changing the selection:**
  - Each sapling on a grid spot whose type is no longer selected is replaced with a sapling of a selected type.
  - Empty spots are planted at once.
  - Grown trees stay.
  - With nothing selected, nothing is planted.

### Planting

- A spot is planted when it is air and the grass or dirt below it can hold the sapling.
- The type is a random selected type.
- Dark oak and pale oak need four saplings. They are planted on the 2×2 square from the spot towards the south-east, and only when all four blocks are free.
- Spot saplings and the leaves of every tree in the grove drop no saplings. Other drops, such as logs, sticks, apples and fruit, stay. Saplings that players plant themselves away from the grid drop normally.
- Saplings that players carry in can't be planted on a grid spot. A grid spot only holds the grove's own planting.
- Trees grow only inside the grove. A mixin on `TreeFeature.validTreePos` treats every arboretum position outside a grove's floor and open space as blocked, so branching trees such as cherry stop at the walls and roof instead of growing through them into the empty dimension. Without it, a branch outside the walls would also block the next size.

### Replanting

When a spot becomes empty, the planting service waits 30 seconds of server time, then plants it.

- A spot that stays blocked, by a log or by something players placed, waits until it is clear.
- The waiting time is kept in memory, so a restart starts the 30 seconds again. This never plants early.
- The service checks each simulated grove once a second.

## Simulation and catch-up

- **While the cabin is deployed:** the grove's chunks hold a simulation ticket, like the pocket's rooms. Saplings, fruit and replanting carry on whether or not anyone is in the grove.
- **While the cabin is packed:** the ticket is removed and the grove sleeps, like the greenhouse.
- **When the cabin is deployed again:**
  1. The grove first replants every empty spot whose 30 seconds have passed.
  2. It then runs the greenhouse catch-up over the grove's volume for the elapsed server operating time. This grows saplings into trees and ripens fruit up to normal ripeness.
- **Recovery:** catch-up uses the same clock, sleep records and recovery receipt as the greenhouse. A grove that is catching up after a crash closes its doors until it finishes. The same elapsed time is never applied twice.

## Persistence

`CabinUpgradeState` gains an optional `arboretum` field with an empty default, so existing saves load unchanged. `CabinArboretumState` holds:

- `revealed`: whether the book has been installed.
- `level`: 0 to 3.
- `taught`: a set of sapling item ids.
- `selected`: a list of sapling item ids, at most the size's limit, all of them taught.

The grove's blocks live in the arboretum dimension, which never moves. Packing, travel and restart keep it as it is.

## Verification

### Dedicated-server checks

`./gradlew runArboretumReloadTest` builds every size in the real pocket and arboretum dimensions, then checks:
- Obstructions, protection and guest rights.
- Planting, including dark oak as a 2×2 square.
- That cherries and dark oaks grown right up to the walls leave nothing outside the grove.
- Drops, and a packed grove that sleeps.

After a restart, it checks that the grove and its state are kept, that the door moves with a main-room expansion, and that catch-up replants and grows trees exactly once.

### GameTests

- **Region mapping:** cell index to region and back. Every region is inside the world border.
- **Installation:**
  - The book reveals three purchases.
  - The first purchase builds the 7×7 grove with four grid spots, the planting board, both doors and the west corridor.
  - Each later purchase keeps the earlier floor, trees and grid spots, and builds the bigger shell at heights 12 and 15.
- **Teaching:**
  - It takes 15 saplings, from storage first.
  - It refuses with fewer and consumes nothing.
  - It skips named stacks.
- **Selection:**
  - It is limited to 1, 2 and 3 types.
  - Untaught types are refused.
  - Changing it swaps deselected saplings and keeps grown trees.
- **Replanting:**
  - A felled spot is replanted 30 seconds later.
  - A blocked spot waits.
  - Dark oak gets a full 2×2.
- **Drops:** grove leaves and spot saplings give no saplings, but leaves outside the grove still do.
- **Doors:**
  - Each door leads to the other side.
  - Guests can walk through.
  - Packing evacuates players from the grove.
  - A player logging back in to a grove whose cabin was packed is evacuated.
- **Expansion:** the arboretum door moves with the west wing, and the exit door then leads to its new position.
- **Catch-up:** a packed cabin's saplings grow into trees after time passes, and repeating the resume changes nothing.

### Client screenshots

- The grove at each size, by day and at night.
- The planting board screen.
- The arboretum door in the corridor.
