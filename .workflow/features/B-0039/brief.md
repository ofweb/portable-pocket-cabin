# A crafting room works manually before automation

Status: Draft
Feature ID: B-0039

## Goal

Give the household a dedicated crafting room that works without central storage or automation. Installed storage becomes available without another upgrade.

## Stories and acceptance

### S1: Install the crafting room

Story: An owner installs the crafting room book, then purchases the room through Cabin Upgrades.

Acceptance:

- The crafting room has its own book and upgrade path.
- The book reveals room upgrades under the shared book rules. It does not purchase the room or reveal automation upgrades.
- The first level provides a crafting table, loom, and cartography table. The second adds stonecutting. The third adds smithing.
- Each station keeps its familiar Minecraft interface and works manually without automation.
- Room-provided stations are protected and open their interface directly.
- The room has a fixed 5x5 usable floor area across all three levels.
- The room branches from the north corridor.
- Installing a workstation removes player blocks from only the space that station needs. Displaced blocks and their inventory contents drop into the room.
- A failed installation leaves the room and upgrade fund unchanged. Recovery cannot drop displaced items or install a station twice.
- Room installation, traversal, and persistence follow B-0005.
- One crafting room book reveals all three levels. Each level requires the previous one, and only the next available purchase is shown.
- A dedicated Crafting category in Cabin Upgrades shows the next purchase, then fully upgraded at the third level.

| Purchase | Materials |
|---|---|
| Crafting room | 1 crafting table, 1 loom, 1 cartography table, 16 logs matching the saved wall wood, 8 iron ingots, 4 amethyst blocks |
| Stonecutting | 1 stonecutter, 8 copper ingots, 4 amethyst blocks |
| Smithing | 1 smithing table, 16 copper ingots, 1 diamond, 4 amethyst blocks |

### S2: Craft manually

Story: An owner or resident uses the crafting room before the cabin has storage or automation.

Acceptance:

- Manual crafting works without central storage or an automation capability.
- Players can place inputs manually using normal station interactions or use automatic ingredient filling within the same station interface.
- Each room-provided station has a recipe-book panel for selecting its recipes or operations. Stations without a normal recipe book gain one alongside their familiar controls.
- When an operation matches several equipment items, maps, or banners, the recipe-book panel asks the player to choose the target. Ordinary ingredients fill automatically after that choice.
- Players can deliberately select named or customised targets in the panel. This selection permits using that target despite automatic-filling protection.
- Manual use follows the crafting-table interface and recipe book. The player clicks the result to craft.
- Normal Minecraft recipe rules apply. Manual use does not require a product template learned from storage.
- A normal result click crafts one recipe batch onto the cursor.
- Shift-click crafts up to one normal output stack into player inventory. It stops when ingredients or inventory space run out.
- After crafting, the selected recipe refills the grid using storage-first ingredients while they are available. Crafting occurs only when the player clicks the result.
- Without central storage, automatic filling uses player inventory alone.
- Guests cannot make a change to the room or its inventories.
- Installing central storage makes it available to the room without another connection upgrade.
- Selecting a recipe fills the crafting grid from storage first, then player inventory for any missing ingredients.
- All room-provided stations use the storage-first ingredient priority when storage is installed.
- Automatic ingredient filling skips named or customised stacks in storage and player inventory. Players can place those stacks into station inputs deliberately.
- Closing a station returns unused ingredients to their source. If that source cannot accept them, they go to player inventory, then any remainder drops normally.
- Intermediate ingredients require separate manual crafting steps until automation is installed.
- Crafting automation requires a separate automation book and upgrade purchase in B-0024 and B-0007.

## Feature-wide constraints and acceptance

- Every ingredient transfer and result action rechecks cabin identity, player role, recipe, input quantities, and result space.
- An interrupted transfer or craft completes once or makes no change. Recovery cannot lose ingredients or duplicate outputs.
- Players must stay near the station inside its room. Leaving, packing, or access removal closes the interface under the ingredient-return rules.

## Scope

This feature includes manual crafting, loom use, cartography, stonecutting, smithing, the room's upgrade path, and optional use of installed central storage.

## Non-goals

Smelting, kitchen use, and automated crafting jobs are separate features. Room-provided anvils and grindstones belong to enchanting. Loadouts have their own book and upgrade path.

## Related records

- [Vanilla station and recipe-book screenshots](../../../experiments/crafting-ui/README.md).
- [Manual production rooms](../../direction.md#manual-production-rooms).
- [Room purchase and traversal](../B-0005/brief.md).
- [Central storage](../B-0004/brief.md).
- [Crafting jobs](../B-0024/brief.md).
- [Automation capabilities](../B-0007/brief.md).
- [Enchanting room](../B-0008/brief.md).
- [Loadouts](../B-0029/brief.md).
- [Cabin books](../B-0038/brief.md).
- [Book installation rules](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).

## Open questions and assumptions

- Whether recipe-book panels show all supported recipes or only recipes the player has discovered remains open.
