# The cabin has central storage

Status: Ready
Feature ID: B-0004

## Goal

Give each cabin one inventory for household items. The cabin does not move items from placed inventories.

## Stories and acceptance

### S1: Keep household items

Story: An owner or resident puts items into central storage and withdraws them through a cabin interface.

Acceptance:

- A cabin has one storage inventory after storage installation.
- A dedicated interior control opens storage. The Lodestone opens Cabin Upgrades.
- The protected control appears opposite the Lodestone after installation and stays there through expansion. Design settles its appearance.
- Owners and residents can put items in storage, see its contents, and withdraw items. Guests cannot use or inspect storage.
- Guests get a permission message without capacity, contents, or owner controls.
- The browser opens on Search.
- Capacity counts stacks at each item's Minecraft stack limit. Unstackable items each use one slot.
- Storage sorts deposited items and merges compatible partial stacks automatically.
- The browser follows Creative inventory: category tabs, Search, an item grid, scrolling, and tooltips.
- Category browsing follows Minecraft's Creative item order. Custom alphabetical and quantity sort controls are outside this feature.
- The hotbar stays visible while browsing. An Inventory tab gives access to the full player inventory.
- Items without a Creative category appear in Miscellaneous and search. Variants of the same item appear together, with tooltips distinguishing their data.
- Identical stacks share one entry showing total quantity. Different data, including names and enchantments, stays separate. Grouping does not increase capacity.
- Left-click on a stored entry takes up to one normal stack onto the cursor. Right-click takes one item.
- Shift-click on a stored entry moves at most one normal stack into player inventory.
- Shift-click on a player inventory stack deposits that stack and storage sorts it automatically.
- Clicking the storage grid with a held stack deposits it. Left-click deposits the stack; right-click deposits one item. Items that do not fit stay on the cursor.
- Deposits and withdrawals move as many items as fit, up to the action's stack limit. The remainder stays in its source.
- Storage keeps each stack and its data through restart, packing, redeployment, and a missing exterior.
- Chests and other placed inventories keep their contents. Central storage does not inspect or move those items.

### S2: Increase capacity

Story: A cabin owner installs a storage upgrade and gets more storage slots.

Acceptance:

- Only owner-installed storage upgrades add slots.
- One storage cabin book reveals initial storage installation and all capacity levels, following the shared book installation rules.
- Revealed storage upgrades have their own Storage tab in Cabin Upgrades.
- The Storage tab shows only the next available level and its cost.
- At maximum capacity, the Storage tab shows fully upgraded.
- Each level requires the previous level. The first purchase installs storage. Costs are:

| Stack slots | Materials for that purchase |
|---|---|
| 54 | 2 chests, 4 iron ingots, 4 amethyst shards |
| 108 | 8 copper ingots, 4 amethyst shards |
| 216 | 16 copper ingots, 4 amethyst shards, 4 redstone dust |
| 432 | 24 copper ingots, 4 amethyst shards, 8 redstone dust, 1 ender pearl |
| 864 | 32 copper ingots, 4 amethyst shards, 12 redstone dust, 1 crying obsidian, 1 Nether quartz |
| 1,728 | 48 copper ingots, 4 amethyst shards, 16 redstone dust, 2 shulker shells |

- If a capacity change does not complete, items in storage and the number of slots stay the same.
- Owners and residents see contents and available slots. Only owners control upgrades.
- A thin green capacity bar at the top resembles Minecraft's experience bar. It shows occupied stack slots relative to capacity.
- The bar's tooltip shows used and total slots. Search and category filters do not change capacity usage.

### S3: Fund one cabin upgrade

Story: A cabin owner selects one upgrade and moves its missing materials from central storage into its upgrade fund.

Acceptance:

- The owner starts the transfer for one target. Storage moves only items in that target's requirements that the fund does not have.
- Fill from Storage contributes available matching materials, even when storage cannot supply all missing requirements.
- Fill from Storage leaves named or customised stacks in storage, following the shared automatic funding rules.
- Storage does not fill funds without an owner action. Storage does not install upgrades.
- A changed target or requirement stops the transfer. No items move.
- Materials that the fund cannot accept stay in storage. If no eligible materials can move, the action makes no change and gives the reason.

## Feature-wide constraints and acceptance

- Each transfer checks cabin identity, cabin state, player role, capacity, and selected stacks before items move.
- Each transfer selects the quantity that fits before moving items. It moves that selected quantity once or moves no items.
- If a transfer stops before it completes, recovery completes the transfer one time or moves no items.
- An open storage interface checks the player's role again before each transfer.
- Players must stay near the control inside the cabin. Packing, leaving, or access removal closes the browser under normal container rules.

## Scope

The feature includes manual storage, automatic sorting, search and category browsing, capacity upgrades, storage status, and owner-approved transfers into one upgrade fund.

## Non-goals

- Automatic movement of items from placed inventories or between cabins.
- Production jobs that use storage items.
- Automatic upgrade funding or installation.

## Related records

- [Central storage](../../context.md#central-storage).
- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Upgrade funding decision](../../decisions/pdr/0007-fund-and-install-cabin-upgrades.md).
- [Household role brief](../B-0002/brief.md).
- [Mailbox brief](../B-0003/brief.md).
- [Cabin books brief](../B-0038/brief.md).
- [Books reveal upgrades decision](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
