# The cabin has a manual kitchen

Status: Implemented
Feature ID: B-0041

## Goal

Give the household a kitchen room for [B-0045](../B-0045/brief.md) cooking. The household builds the kitchen by placing the cooking stations it wants. Stations inside the kitchen draw ingredients and fuel from installed central storage. Later automation in B-0025 works with the stations the household has placed there.

## Stories and acceptance

### S1: Install the kitchen

Story: An owner installs the kitchen book, then purchases the kitchen through Cabin Upgrades.

Acceptance:

- Its B-0038 book reveals one purchase, but no automation. The Kitchen category shows the purchase or that it is installed.
- Only owners purchase upgrades; residents can contribute.
- Installation follows B-0005 and requires 5×5 main space. The kitchen branches from the south side of the east corridor.
- Its 5×5 floor and three-block clearance stay fixed.
- The room comes empty apart from hanging lamps. The household places its own stations, counters, and decorations.
- Failed installation leaves the cabin and fund unchanged; recovery completes the installation once.

Logs match saved wall wood. Collected and traded materials count without personal visits.

| Purchase | Supplies | Materials |
|---|---|---|
| Kitchen | An empty 5×5 room with hanging lamps | 16 wall logs, 8 copper ingots, 4 amethyst blocks |

### S2: Cook in the kitchen

Story: An owner or resident places stations in the kitchen and cooks there.

Acceptance:

- Stations in the kitchen follow B-0045 rules for fuel, heat, water, progress, and breaking. Players craft their own stations and carry their own knife.
- Owners and residents can cook without storage or automation. Guests can enter but cannot use stations under B-0002.
- Automation has its own book and upgrade.

### S3: Use installed storage

Story: A household member cooks in the kitchen with ingredients and fuel kept in central storage.

Acceptance:

- Installed central storage serves every cooking station inside the kitchen without another upgrade. Stations elsewhere use only the player's inventory.
- Recipe filling at a kitchen station takes ingredients from storage first, then player inventory. Items it displaces go to the player's inventory, then storage when the inventory is full.
- Selecting a recipe also fills the stove or oven that heats it with fuel at that recipe's heat level when it holds none, from storage first.
- Automatic filling skips named and customised stacks. Players can add those deliberately.
- Shift-clicked outputs move to player inventory, then storage when full.
- The station recipe panel and the cookbook count storage toward recipe availability inside the kitchen and the cabin, as B-0045 states.

### S4: Rearrange the kitchen

Story: An owner or resident rearranges kitchen furnishings to suit the household.

Acceptance:

- Owners and residents can place, move, and break stations, counters, and decorations. The room shell stays protected; guests cannot change furnishings.
- Breaking a station drops its contents and restarts unfinished progress under B-0045.
- A station uses storage wherever it stands inside the kitchen. B-0005 preserves furnishings and contents through restart, packing, redeployment, and relocation.

## Feature-wide constraints and acceptance

- Every fill and output rechecks cabin identity, role, recipe, contents, and space.
- Interrupted transfers complete once or change nothing. Recovery cannot lose or duplicate items.
- B-0005 preserves stations, contents, water, fuel, and progress through packing, restart, redeployment, and relocation.
- Kitchen stations pause while an empty cabin is packed. Catch-up remains open under Direction.
- Leaving, packing, or role removal closes interfaces without moving contents.
- Storage access follows B-0004 permissions and transfer rules.

## Scope

Includes the kitchen purchase, the room, household access, and storage-backed filling and output at kitchen stations.

## Non-goals

- B-0045 owns cooking behavior and recipes. B-0046 through B-0052 own later stations.
- B-0025 owns automation and decides which stations each automation needs; B-0040 owns smelting.
- Supplied stations, kitchen storage containers, storage filters, and extra storage capacity. A custom fridge and outside storage access are excluded.

## Related records

- [Cooking stations and cookbook](../B-0045/brief.md).
- [Cooking concept](../../cooking/concept.md).
- [Room purchase and traversal](../B-0005/brief.md).
- [Cabin books](../B-0038/brief.md).
- [Central storage](../B-0004/brief.md).
- [Household roles](../B-0002/brief.md).
- [Cooking and brewing automation](../B-0025/brief.md).
- [Manual production rooms](../../direction.md#manual-production-rooms).

## Open questions and assumptions

- B-0033 balances the purchase cost.
- Later cooking features may add kitchen purchases, for example a larger room.
