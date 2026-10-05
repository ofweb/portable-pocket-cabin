# The cabin has a manual kitchen

Status: Ready
Feature ID: B-0041

## Goal

Give the household a furnished kitchen with individual workstations for manual food preparation before storage or automation.

## Stories and acceptance

### S1: Add the kitchen

Story: An owner installs the kitchen book and purchases the room.

Acceptance:

- The kitchen has its own book under B-0038 and one room purchase.
- It branches from the south side of the east corridor.
- Its usable floor is fixed at 5×5 with four blocks of clear interior height.
- The purchase supplies a smoker, a crafting counter, food and tool storage, counters, warm hanging lamps, and a chimney hood over the smoker.
- With a supported Farmer's Delight Refabricated version installed, that purchase also supplies a cutting board, a cooking pot heated by a stove, and a separate cooking surface for grilling or a skillet.
- Installation delivers the 24 contributed food items once to the local barrel before central storage, or to central storage when installed.
- Installation, traversal, permissions, and persistence follow B-0005.

Installation cost:

| Preferred item | Quantity | Without Farmer's Delight |
|---|---:|---|
| Raw beef | 4 | Same |
| Raw rabbit | 4 | Same |
| Raw salmon | 4 | Same |
| Carrot | 4 | Same |
| Potato | 4 | Same |
| Beetroot | 4 | Same |
| Stone | 16 | Same |
| Obsidian | 2 | Same |
| Amethyst block | 4 | Same |
| Smoker | 1 | Same |
| Crafting table | 1 | Same |
| Barrel | 1 | Same |
| Farmer's Delight cabinet | 1 | 1 chest |
| Cutting board | 1 | 2 sticks |
| Cooking pot | 1 | 4 iron ingots |
| Stove | 2 | 8 bricks |

Each version uses 16 requirement types. Construction materials cover lamps, counters, and the chimney hood.

### S2: Prepare food manually

Story: A household member uses the kitchen before automation is installed.

Acceptance:

- Owners and residents can use the stations without central storage or automation. Guests can inspect the room but cannot use its facilities.
- Players bring their own knives and skillets.
- Players use individual workstations; a combined meal-selection interface does not replace them.
- Players withdraw supplies, carry ingredients and intermediate outputs between stations, load inputs, and collect results.
- Loading a station starts its ordinary cooking behaviour. Automatic fetching and transfers belong to cooking automation.
- Station interfaces, recipes, fuel, and working inventories follow ordinary vanilla or supported-mod rules.
- Installed central storage becomes available without another connection upgrade.
- Automation requires its separate book and upgrade purchase.

### S3: Get kitchen supplies

Story: An owner or resident uses kitchen fixtures to get supplies for manual food preparation.

Acceptance:

- A vanilla barrel provides the initial food-storage fixture.
- Kitchen tools use a Farmer's Delight Refabricated cabinet when a supported version is installed. Otherwise, they use a single vanilla chest.
- Only the supplied food barrel and tool cabinet or chest become central storage access points.
- These two containers each have 27 local slots until central storage is installed. The supplied chest stays a single chest.
- An installed kitchen adds 54 central stack slots once central storage is installed, regardless of purchase order. First-level storage with a kitchen has 108 slots.
- This bonus belongs to the room, applies once, and survives moving or removing containers.
- Capacity displays include the kitchen bonus. Storage upgrade levels and costs stay unchanged.
- Installing central storage transfers both containers' contents once. The bonus covers conversion; installing the kitchen after storage adds capacity before delivering starter food.
- After installation, these two containers access only central storage. They no longer offer local inventories.
- The barrel initially shows food and cooking ingredients, including ingredients that cannot be eaten. The cupboard or chest initially shows kitchen tools and serving containers.
- Players can change filters. Each fixture remembers its last-used filter, including after breaking and placing it again.
- Filters affect browsing only. Either interface accepts any item central storage can hold.
- Extra kitchen containers remain local.
- Removing an access container leaves central storage contents intact.

### S4: Rearrange the kitchen

Story: An owner or resident rearranges kitchen furnishings to suit the household.

Acceptance:

- Owners and residents can rearrange kitchen workstations, storage containers, and decorations.
- The two supplied storage fixtures retain their central connection when broken and placed again in the kitchen. Ordinary replacements remain local.
- Kitchen facilities do not require their original installation positions.
- The room shell stays protected under the shared cabin rules. Guests cannot rearrange furnishings.
- Restart, packing, redeployment, and room relocation preserve the arranged furnishings and their contents under B-0005.

## Feature-wide constraints and acceptance

- Installation and storage conversion cannot duplicate or lose items, starter food, fixtures, or bonus capacity after interruption.
- Storage access follows B-0004 permissions and transfer rules.

## Scope

This feature includes the manual kitchen and owns its extensions to implemented B-0004: bonus capacity, container conversion, and filtered access. B-0025 and B-0027 own automation's workstation requirements.

## Non-goals

Cooking automation belongs to B-0025. Material smelting belongs to B-0040. Later kitchen tiers, a custom fridge, and central storage access outside the kitchen are outside this feature. Optional-mod addition and removal policies belong to B-0011.

## Related records

- [Room purchase and traversal](../B-0005/brief.md).
- [Cabin books](../B-0038/brief.md).
- [Central storage](../B-0004/brief.md).
- [Optional mod compatibility](../B-0011/brief.md).
- [Cooking and brewing automation](../B-0025/brief.md).
- [Optional production profiles](../B-0027/brief.md).
- [Preferred upgrade materials and shared fallbacks](../../decisions/pdr/0014-use-preferred-upgrade-materials-with-shared-fallbacks.md).
- [Upgrade materials and exploration](../../direction.md#upgrade-materials-and-exploration).
- [Manual production rooms](../../direction.md#manual-production-rooms).

## Open questions and assumptions

- Farmer's Delight Refabricated compatibility is unverified. Design must verify supported versions and station persistence under B-0011 before claiming support.
- Design settles cabinet appearance, initial furnishing placement, and the chimney hood's form within the agreed room dimensions.
