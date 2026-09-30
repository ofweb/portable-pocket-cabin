# A cabin learns and uses enchantments

Status: Draft
Feature ID: B-0008

## Goal

A cabin keeps known enchantments. Owners and residents use a dedicated table in an enchanting room to learn and apply them.

## Stories and acceptance

### S1: Install the enchanting room

Story: After the cabin installs the room book, the owner purchases the enchanting room upgrade and gains access to its table.

Acceptance:

- The cabin does not show Enchanting until the owner installs its book in B-0038. The room book and automation book reveal different upgrades.
- The installed book reveals the room upgrade. It does not install the room.
- Players with cabin access can inspect revealed upgrades, as the cabin upgrade rules state.
- Only the owner installs the room and its upgrades. Residents can contribute to their upgrade funds.
- Installation follows the room purchase rules and adds the room and table.
- The installed room can apply known enchantments up to the first level.
- Before installation, the cabin cannot learn or manually apply known enchantments.

### S2: Learn one enchantment

Story: An owner or resident selects one enchantment from an item and uses the Learn action to add it to known enchantments for the cabin.

Acceptance:

- An enchanted item in storage does not add a known enchantment to the cabin.
- In Learn mode, the player selects an enchanted item and empty books from inventory or available central storage.
- The interface shows the selected enchantment, level, and number of books. The interface tells the player that learning deletes the source item and its other enchantments.
- The cabin uses one empty book for each new level. Level III uses three books if the cabin knows no level, or one book if the cabin knows level II.
- Learning deletes the source item and uses the books. The cabin does not give back an enchanted book.
- If the cabin cannot complete the action, the item and known enchantments stay as before.
- The cabin keeps the highest learned level for each enchantment. It can use each lower level.
- The cabin can learn a level above the enchanting room application limit.
- The player uses no item to learn an enchantment that the cabin knows at that level or a higher level.
- Known enchantments stay with the cabin through packing, restart, and redeployment. The owner cannot remove them.

### S3: Enchant an item

Story: An owner or resident selects an item at the dedicated enchanting table and applies a known enchantment.

Acceptance:

- The interface shows known enchantments that can go on the item, their levels, lapis, amethyst, and other materials before the player confirms.
- The player can apply a level only up to the known level, the room's application limit, and the maximum level for that enchantment.
- The table applies an enchantment when the player confirms. Confirmation completes the action.
- The amethyst quantity is the selected level multiplied by one plus the number of enchantments on the item.
- The lapis quantity is two times the amethyst quantity.
- Level III on an item with one enchantment uses six amethyst and twelve lapis.
- The player can select an item from player inventory or central storage when the cabin has central storage.
- Player inventory is the only source when the cabin has no central storage.
- The player selects lapis and amethyst sources independently of the item source. The interface shows each source and number of items.
- The cabin checks the item, selected level, other enchantments, materials, and player role before it makes a change to the item.
- The table rejects an enchantment type that the item has, when the selected level is higher too.
- The enchanted item goes back to its selected source inventory. The action checks for result space before it uses materials.
- If the cabin cannot do the action, the item and materials stay in their source inventory and the interface gives the reason.
- The action applies only the selected enchantment and level. The item keeps its other profile data.
- Owners and residents can use this action. Guests cannot use this action or its materials.

### S4: Increase the application limit

Story: A cabin owner installs an enchanting room upgrade and can then apply stronger known enchantments.

Acceptance:

- The enchanting room shows the application limit and the effect of the next upgrade.
- Only the owner can install the upgrade.
- Each level upgrade adds one to the application limit. Known enchantments and items keep the same state.
- The limit stays with the cabin through packing, restart, and redeployment.

## Feature-wide constraints and acceptance

- The dedicated enchanting room and its table are necessary for learning and enchanting by a player. Placed enchanting tables and anvils follow Minecraft behavior.
- For enchanting by a player, amethyst replaces player experience. The action does not subtract player experience.
- Minecraft rules show which enchantments can go on each item and which enchantments can go together. The enchantment profile lists each optional enchantment.
- The owner selects a curse before the cabin applies it.

## Scope

This feature includes the dedicated enchanting room and table, learning, known enchantments, and enchanting by a player.

## Non-goals

The cabin does not learn from items in storage or use enchantments known by other cabins. Automation slot selection, equipment requisitions, and owner loadouts are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Known enchantment](../../context.md#known-enchantment).
- [Room purchase and access brief](../B-0005/brief.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [PDR-0001](../../decisions/pdr/0001-preserve-the-portable-home.md).
- [PDR-0010](../../decisions/pdr/0010-manual-enchanting-uses-all-known-enchantments.md).
- [PDR-0011](../../decisions/pdr/0011-separate-learning-from-application-limits.md).
- [Cabin books brief](../B-0038/brief.md).
- [Book and upgrade decision](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
- [Equipment requisitions brief](../B-0028/brief.md).
- [Earlier milestone source](../../../docs/roadmap/08-enchanting-loadouts.md).

## Open questions and assumptions

- Rules for optional enchantments and unusual item data are open.
- The highest room level and the cost and type of other room upgrades are open.
