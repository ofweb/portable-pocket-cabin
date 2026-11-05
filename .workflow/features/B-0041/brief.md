# The cabin has a manual kitchen

Status: Ready
Feature ID: B-0041

## Goal

Give the household a dedicated kitchen with supplied cooking stations. It works before storage or automation, and installed storage supplies fuel and ingredients without another upgrade. Cooking itself follows [B-0045](../B-0045/brief.md).

## Stories and acceptance

### S1: Install the kitchen

Story: An owner installs the kitchen book, then purchases the kitchen through Cabin Upgrades.

Acceptance:

- The kitchen has its own book and upgrade path under B-0038. One kitchen book reveals all three purchases. It reveals no automation upgrades.
- Each purchase requires the previous one, and only the next available purchase is shown. A dedicated Kitchen category shows the next purchase, then fully upgraded after the third.
- Only the owner purchases upgrades. Residents can contribute under shared funding rules.
- Installation follows B-0005 and requires 5×5 main space. The kitchen branches from the south side of the east corridor.
- Its usable floor stays 5×5 with three blocks of clear height through all purchases.
- Installing a station removes player blocks from only the space that station needs. Displaced blocks and their inventory contents drop into the room.
- A failed installation leaves the room and upgrade fund unchanged. Recovery cannot drop displaced items or install a station twice.
- Free floor remains for player-placed cooking stations, which work there as anywhere else.

Logs match saved wall wood. Collected and traded materials count without personal visits.

| Purchase | Supplied stations | Materials |
|---|---|---|
| I: Kitchen | Two stoves, one topped with a cooking pot and one with a skillet; a cutting board | 2 stoves, 1 cooking pot, 1 skillet, 1 cutting board, 16 wall logs, 8 copper ingots, 4 amethyst blocks |
| II: Oven | One oven | 1 oven, 8 copper ingots, 4 amethyst blocks |
| III: Second line | A second stove pair with a pot and a skillet, and a second cutting board | 2 stoves, 1 cooking pot, 1 skillet, 1 cutting board, 1 diamond, 4 amethyst blocks |

### S2: Cook in the kitchen

Story: An owner or resident cooks a recipe at the kitchen's supplied stations.

Acceptance:

- Supplied stations behave like placed B-0045 stations, including fuel, heat levels, water, and progress.
- Supplied stations are protected. It cannot be broken or moved, and it opens its interface directly.
- The supplied cutting board includes a fixed knife, so players need not carry one.
- Owners and residents can use the kitchen. Guests can enter but cannot use its stations, as B-0002 requires.
- Cooking in the kitchen requires neither central storage nor automation.

### S3: Use installed storage

Story: A household member cooks with ingredients and fuel kept in central storage.

Acceptance:

- Installed central storage becomes available to the kitchen without another connection upgrade.
- Recipe-book filling at a supplied station takes ingredients from storage first, then player inventory.
- Selecting a recipe also fills a supplied stove or oven with fuel at that recipe's heat level when it holds none, from storage first.
- Automatic filling skips named and customised stacks. Players can add those deliberately.
- Players take outputs from a station as usual. Shift-clicking an output moves it to player inventory, then storage when inventory is full.
- The cookbook counts storage toward recipe availability inside the cabin, as B-0045 states.

## Feature-wide constraints and acceptance

- Every fill, step, and output rechecks cabin identity, player role, recipe, contents, and space.
- An interrupted fill, step, or output completes once or changes nothing. Recovery cannot lose ingredients or duplicate food.
- Supplied stations, their contents, water, fuel, and progress persist through packing, restart, redeployment, and main-room relocation under B-0005.
- Kitchen stations pause while an empty cabin is packed. Catch-up remains open under Direction.
- Leaving, packing, or role removal closes kitchen interfaces. Station contents stay in place.

## Scope

This feature includes the kitchen book, its three purchases, supplied stations, protection, household access, and storage filling for fuel and ingredients.

## Non-goals

- Recipes, station behavior, heat levels, and prepared items belong to B-0045.
- Stations for milling, dairy, drying, fermentation, butchery, and drinks belong to B-0046 through B-0052. Those features may add kitchen purchases.
- Cooking automation belongs to B-0025. Material smelting belongs to B-0040.

## Related records

- [Cooking stations and cookbook](../B-0045/brief.md).
- [Cooking concept](../../cooking/concept.md).
- [Room purchase and traversal](../B-0005/brief.md).
- [Cabin books](../B-0038/brief.md).
- [Central storage](../B-0004/brief.md).
- [Household roles](../B-0002/brief.md).
- [Manual crafting room](../B-0039/brief.md).
- [Cooking and brewing automation](../B-0025/brief.md).
- [Manual production rooms](../../direction.md#manual-production-rooms).

## Open questions and assumptions

- B-0033 balances purchase costs.
- Later cooking features decide whether their stations join the kitchen through new purchases or stay player-placed only.
