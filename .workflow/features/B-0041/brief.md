# The cabin has a manual kitchen

Status: Ready
Feature ID: B-0041

## Goal

Give the household a furnished kitchen for [B-0045](../B-0045/brief.md) cooking. It works before storage or automation, and installed storage supplies ingredients and fuel.

## Stories and acceptance

### S1: Install the kitchen

Story: An owner installs the kitchen book, then purchases the kitchen through Cabin Upgrades.

Acceptance:

- Its B-0038 book reveals three sequential purchases, but no automation. The Kitchen category shows only the next purchase or fully upgraded.
- Only owners purchase upgrades; residents can contribute.
- Installation follows B-0005 and requires 5×5 main space. The kitchen branches from the south side of the east corridor.
- Its 5×5 floor and three-block clearance stay fixed.
- The first purchase also supplies a food barrel, a single tool chest, counters, hanging lamps, and a hood above a stove.
- Station installation displaces only blocks it needs, dropping them and their contents into the room. Failed installation leaves the room and fund unchanged; recovery neither drops items nor installs stations twice.
- Free floor remains for player-placed stations.

Logs match saved wall wood. Collected and traded materials count without personal visits.

| Purchase | Supplied stations | Materials |
|---|---|---|
| I: Kitchen | Two stoves, one topped with a cooking pot and one with a skillet; a cutting board | 2 stoves, 1 cooking pot, 1 skillet, 1 cutting board, 16 wall logs, 8 copper ingots, 4 amethyst blocks |
| II: Oven | One oven | 1 oven, 8 copper ingots, 4 amethyst blocks |
| III: Second line | A second stove pair with a pot and a skillet, and a second cutting board | 2 stoves, 1 cooking pot, 1 skillet, 1 cutting board, 1 diamond, 4 amethyst blocks |

### S2: Cook in the kitchen

Story: An owner or resident cooks a recipe at the kitchen's supplied stations.

Acceptance:

- Supplied stations follow B-0045 rules for fuel, heat, water, progress, and breaking. Players carry intermediate items and bring a knife for the cutting board.
- Owners and residents can cook without storage or automation. Guests can enter but cannot use stations under B-0002.
- Automation has its own book and upgrade.

### S3: Use installed storage

Story: A household member gets supplies and cooks with ingredients and fuel kept in central storage.

Acceptance:

- Installed central storage connects without another upgrade. Before installation, the barrel and single chest each have 27 local slots.
- Only these supplied containers become storage access points; extra containers stay local. Once connected, their local inventories disappear.
- The kitchen adds 54 central stack slots once, regardless of purchase order. First-level storage then has 108 slots. Capacity displays include the bonus; storage levels and costs stay unchanged.
- Installing storage transfers both containers' contents once. Installing the kitchen after storage adds capacity before connecting its containers. Removing access containers leaves central contents intact.
- The barrel initially filters food and cooking ingredients, including inedible ones; the chest filters tools and serving containers. Users can change filters, which each fixture remembers after moving. Filters affect browsing only; either accepts any storable item.
- Recipe-book filling at a supplied station takes ingredients from storage first, then player inventory.
- Selecting a recipe also fills a supplied stove or oven with fuel at that recipe's heat level when it holds none, from storage first.
- Automatic filling skips named and customised stacks. Players can add those deliberately.
- Shift-clicked outputs move to player inventory, then storage when full.
- The cookbook counts storage toward recipe availability inside the cabin, as B-0045 states.

### S4: Rearrange the kitchen

Story: An owner or resident rearranges kitchen furnishings to suit the household.

Acceptance:

- Owners and residents can rearrange stations, containers, and decorations. The room shell stays protected; guests cannot rearrange furnishings.
- Breaking a station drops its contents and restarts unfinished progress under B-0045.
- The two supplied storage fixtures retain their central connection when broken and placed again in the kitchen. Ordinary replacements remain local.
- Facilities work outside their original positions. B-0005 preserves furnishings and contents through restart, packing, redeployment, and relocation.

## Feature-wide constraints and acceptance

- Every fill, step, and output rechecks cabin identity, role, recipe, contents, and space.
- Interrupted operations complete once or change nothing. Recovery cannot lose or duplicate items, fixtures, or bonus capacity.
- B-0005 preserves stations, contents, water, fuel, and progress through packing, restart, redeployment, and relocation.
- Kitchen stations pause while an empty cabin is packed. Catch-up remains open under Direction.
- Leaving, packing, or role removal closes interfaces without moving contents.
- Storage access follows B-0004 permissions and transfer rules.

## Scope

Includes the three kitchen purchases, furnishings, household access, storage filling, bonus capacity, conversion, and filters.

## Non-goals

- B-0045 owns cooking behavior and recipes. B-0046 through B-0052 own later stations and may add purchases.
- B-0025 owns automation; B-0040 owns smelting. A custom fridge and outside storage access are excluded.

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

- B-0033 balances purchase costs.
- Design settles furnishings within the agreed dimensions.
- Later cooking features decide whether their stations join the kitchen through new purchases or stay player-placed only.
