# A cabin learns and uses enchantments

Status: Draft
Feature ID: B-0008

## Goal

A cabin keeps known enchantments. Owners and residents use a dedicated table in an enchanting room to learn and apply them.

## Stories and acceptance

### S1: Install the enchanting room

Story: After the cabin installs the room book, the owner buys the enchanting room upgrade and gains access to its table.

Acceptance:

- The Enchanting tab stays hidden until the owner installs its book under B-0038. This book is distinct from the enchanting automation book.
- The installed book reveals the room upgrade. It does not install the room.
- Authorized visitors can inspect the revealed tab under the cabin upgrade rules.
- Only the owner installs the room and its upgrades. Residents can contribute to their upgrade funds.
- Installation adds the room and its table under the room purchase rules.
- The installed room can apply known enchantments up to level I.
- Before installation, the cabin cannot learn or manually apply known enchantments.

### S2: Learn one enchantment

Story: An owner or resident selects one enchantment from an item and learns it through a deliberate extraction.

Acceptance:

- Storage of an enchanted item alone teaches nothing.
- In Learn mode, the player selects an enchanted item and blank books from inventory or available central storage.
- The interface shows the selected enchantment, level, book cost, and warning that the source item and its other enchantments will be destroyed.
- Book cost is one blank book for each newly learned level. Level III costs three books from no knowledge or one book from level II.
- Success destroys the source item and consumes the books. No enchanted book returns.
- A failed check changes no item or cabin knowledge.
- The cabin keeps the highest learned level for each enchantment, which also permits lower levels.
- The cabin can learn a level above the enchanting room's current application limit.
- Extraction of a known enchantment at the same or a lower level uses no item.
- Learned knowledge survives packing, restart, and redeployment. The owner cannot remove learned knowledge.

### S3: Enchant an existing item

Story: An owner or resident selects an eligible item at the dedicated enchanting table and applies a known enchantment.

Acceptance:

- The interface shows eligible known enchantments, levels, lapis, amethyst, and other required materials before confirmation.
- The player can apply a level only up to the known level, the room's current limit, and that enchantment's own maximum.
- Manual enchanting completes on confirmation without a work timer.
- Amethyst cost is the selected level multiplied by one plus the item's current enchantment count.
- Lapis cost is two times the amethyst cost.
- Level III on an item with one enchantment uses six amethyst and twelve lapis.
- The player can select an item from player inventory or central storage when the cabin has central storage.
- Player inventory is the only source when the cabin has no central storage.
- The player selects lapis and amethyst sources independently of the item source. The interface shows each source and amount.
- The cabin checks the item, selected level, other enchantments, materials, and current role before it changes anything.
- The table rejects an enchantment type that the item already has, even when the selected level is higher.
- The enchanted item returns to its selected source inventory. The action checks for result space before it consumes materials.
- If a check fails, the item and materials stay in their source inventory and the interface shows the reason.
- Success applies only the selected enchantment and level. The item keeps its other permitted data.
- Owners and residents can start this action. Guests cannot start it or use its materials.

### S4: Raise the application limit

Story: A cabin owner installs an enchanting room upgrade and can then apply stronger known enchantments.

Acceptance:

- The enchanting room tab shows the current application limit and the next upgrade's effect.
- Only the owner can install the upgrade.
- Each level upgrade raises the application limit by one without changing any known enchantment or item.
- The limit remains with the cabin through packing, restart, and redeployment.

## Feature-wide constraints and acceptance

- Learning and manual enchanting require the dedicated enchanting room and its table. Ordinary placed enchanting tables and anvils keep normal Minecraft behavior.
- Manual enchanting uses amethyst in place of player experience. It has no experience-level requirement or experience deduction.
- Vanilla applicability and incompatible combinations remain in force. Optional enchantments need declared support.
- A curse requires an explicit request before the cabin applies it.

## Scope

This feature includes the dedicated enchanting room and table, learning, permanent knowledge, and manual enchanting of an existing item.

## Non-goals

Stored-item discovery, shared discovery, automation slot selection, equipment requisitions, and owner loadouts are outside this feature.

## Related records

- [Direction](../../direction.md).
- [Known enchantment](../../context.md#known-enchantment).
- [Room purchase and access brief](../B-0005/brief.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Portable home decision](../../decisions/pdr/0001-preserve-the-portable-home.md).
- [Manual and automated enchanting decision](../../decisions/pdr/0010-manual-enchanting-uses-all-known-enchantments.md).
- [Learned levels and room limits decision](../../decisions/pdr/0011-separate-learning-from-application-limits.md).
- [Cabin books brief](../B-0038/brief.md).
- [Book and upgrade decision](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
- [Equipment requisitions brief](../B-0028/brief.md).
- [Earlier milestone source](../../../docs/roadmap/08-enchanting-loadouts.md).

## Open questions and assumptions

- The optional enchantment profile catalogue and handling of unusual item data remain open.
- The maximum room level, room upgrade costs, and other room upgrades remain open.
