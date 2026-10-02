# Backlog

## B-0002: Household roles

- Status: Ready for Shape
- Value: Give each cabin clear owner, resident, and guest capabilities.
- Direction: [Direction](direction.md).
- Feature Brief: [Household role brief](features/B-0002/brief.md).

## B-0003: Receiving mailbox

- Status: Ready for Shape
- Value: Let other players deliver items safely through one receiving mailbox that belongs to the cabin.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0002. Requires installed storage in B-0004 and connected-lounge access in B-0009.
- Feature Brief: [Receiving mailbox brief](features/B-0003/brief.md).

## B-0004: Central storage

- Status: Ready for Shape
- Value: Give each cabin one inventory for household use that stays independent of placed chests.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0002 and B-0038. Used by B-0003 and B-0007. Mailbox behavior belongs to B-0003.
- Feature Brief: [Central storage brief](features/B-0004/brief.md).

## B-0005: Room purchase and traversal

- Status: Ready for Shape
- Value: Connect installed rooms through complete interior corridors while preserving access and contents through cabin growth and travel.
- Direction: [Direction](direction.md).
- Feature Brief: [Room brief](features/B-0005/brief.md).
- Relationships: Uses B-0002. Shared room behavior for B-0008, B-0019 through B-0023, B-0039 through B-0041, and B-0043.

## B-0006: House cat

- Status: Ready for Shape
- Value: Let a tamed cat live safely in the general cabin interior as its home.
- Direction: [Direction](direction.md).
- Feature Brief: [House cat brief](features/B-0006/brief.md).
- Relationships: Uses household roles in B-0002.

## B-0007: Product knowledge and automation books

- Status: Ready for Shape
- Value: Let a cabin learn safe product templates and purchase automation capabilities after their books reveal the upgrades.
- Direction: [Direction](direction.md).
- Feature Brief: [Product knowledge brief](features/B-0007/brief.md).
- Relationships: Uses B-0004 and B-0038. Used by B-0024 through B-0027 and B-0036 through B-0037.

## B-0008: Enchantment library

- Status: Ready for Shape
- Value: Let an owner install and improve an enchanting room where players learn enchantments and apply them manually.
- Direction: [Direction](direction.md).
- Feature Brief: [Enchantment library brief](features/B-0008/brief.md).
- Relationships: Uses B-0005 and B-0038. Used by B-0028.

## B-0009: Connected cabin hallways

- Status: Ready for Shape
- Value: Let owners confirm cabin connections through a lasting hallway with doors that players cannot break.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0009/brief.md).
- Relationships: Uses B-0002. Used by B-0030.

## B-0011: Optional mod compatibility

- Status: Ready for Shape
- Value: Test optional mod profiles with their mods installed, removed, or changed.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0011/brief.md).
- Relationships: Applies to optional mod features. Used by B-0035.

## B-0012: Palette renovation

- Status: Needs Direction
- Value: Explore whether owners can select a different saved cabin palette.
- Direction: [Open question](direction.md#cabin-palette-changes).

## B-0013: Emergency fire packing

- Status: Needs Direction
- Value: Explore automatic packing during a fire near the cabin while players stay safe.
- Direction: [Open question](direction.md#automatic-packing-during-a-fire).

## B-0014: More cabin styles

- Status: Needs Direction
- Value: Explore more exterior and room designs that keep cabin identity clear.
- Direction: [Open question](direction.md#more-cabin-styles).

## B-0015: Cabin names and maps

- Status: Needs Direction
- Value: Explore cabin names and site displays on maps.
- Direction: [Open question](direction.md#cabin-names-and-maps).

## B-0016: Cabin sleep and exterior night

- Status: Needs Direction
- Value: Explore whether cabin sleep can control the time outside its deployed exterior.
- Direction: [Open question](direction.md#cabin-sleep-and-exterior-night).

## B-0018: Wider material support

- Status: Needs Direction
- Value: Explore more modded materials with safe profile rules.
- Direction: [Open question](direction.md#more-modded-materials).

## B-0019: Greenhouse

- Status: Ready for Shape
- Value: Let residents plant and harvest crops with profiles in a room where growth progresses without manual actions and has limits.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005. B-0026 defines automatic actions.
- Feature Brief: [Greenhouse brief](features/B-0019/brief.md).

## B-0020: Stable

- Status: Ready for Shape
- Value: Let residents house and release tamed animals with profiles that players can ride, while each animal keeps its identity.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005.
- Feature Brief: [Stable brief](features/B-0020/brief.md).

## B-0021: Aquatic berth

- Status: Ready for Shape
- Value: Add a stable berth with water for a tamed aquatic stable resident, such as a nautilus.
- Direction: [Direction](direction.md).
- Relationships: Extends the stable in B-0020.
- Feature Brief: [Aquatic berth brief](features/B-0021/brief.md).

## B-0022: Livestock room

- Status: Ready for Shape
- Value: Let residents keep livestock with profiles and population limits, and collect products manually.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005. B-0026 defines automatic actions.
- Feature Brief: [Livestock rooms brief](features/B-0022/brief.md).

## B-0023: Forestry room

- Status: Ready for Shape
- Value: Let residents grow, cut, and replant trees with profiles in the cabin's arboretum.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005. B-0026 defines automatic actions.
- Feature Brief: [Arboretum brief](features/B-0023/brief.md).

## B-0024: Bounded crafting jobs

- Status: Ready for Shape
- Value: Plan and operate local crafting jobs with recipes, hard reserves, work time, and safe outputs.
- Direction: [Direction](direction.md).
- Feature Brief: [Crafting jobs brief](features/B-0024/brief.md).
- Relationships: Uses B-0004, B-0007, and B-0039. Used by B-0008, B-0025, B-0028, B-0036, B-0037, and B-0042.

## B-0025: Cooking and brewing jobs

- Status: Ready for Shape
- Value: Add cooking and brewing processes with profiles to local jobs with limits.
- Direction: [Direction](direction.md).
- Feature Brief: [Cooking and brewing brief](features/B-0025/brief.md).
- Relationships: Uses B-0024. Cooking uses B-0041. Brewing uses B-0043.

## B-0026: Room automation

- Status: Ready for Shape
- Value: Add automatic actions that owners can enable independently in greenhouse, livestock, and forestry rooms. Manual use stays the same.
- Direction: [Direction](direction.md).
- Feature Brief: [Room automation brief](features/B-0026/brief.md).
- Relationships: Uses B-0007, B-0024, and room features B-0019, B-0022, or B-0023.

## B-0027: Optional production profiles

- Status: Ready for Shape
- Value: Add optional cooking and process profiles, including kiln work and later coal production.
- Direction: [Direction](direction.md).
- Feature Brief: [Optional production brief](features/B-0027/brief.md).
- Relationships: Uses B-0025. Optional mods must have integration profiles.

## B-0028: Equipment requisitions

- Status: Ready for Shape
- Value: Let an owner select enchantments for automation and request equipment through a material and work plan.
- Direction: [Direction](direction.md).
- Feature Brief: [Equipment requisitions brief](features/B-0028/brief.md).
- Relationships: Uses B-0008, B-0024, and B-0038.

## B-0029: Owner loadouts

- Status: Ready for Shape
- Value: Let an owner use loadout rules to move existing items between player inventory and central storage through separate book-unlocked upgrades.
- Direction: [Loadouts](direction.md#loadouts).
- Feature Brief: [Owner loadouts brief](features/B-0029/brief.md).
- Relationships: Uses B-0004 and B-0038. Independent of B-0028 and production rooms.

## B-0030: Safe packed cabin access

- Status: Needs Direction
- Value: Let players use a packed cabin through a connected hallway while they keep a safe exit.
- Direction: [Progress after vacancy](direction.md#progress-after-an-empty-cabin-becomes-occupied).
- Feature Brief: [Feature brief](features/B-0030/brief.md).
- Relationships: Uses B-0009 and the [portable-home decision](decisions/pdr/0001-preserve-the-portable-home.md).

## B-0031: Resource requests and surplus

- Status: Ready for Shape
- Value: Let connected cabins request resources and send surplus with owner confirmation. Transfers identify each cabin and keep storage independent.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0031/brief.md).
- Relationships: Uses B-0009 and B-0024. Used by B-0032.

## B-0032: Mailbox automation

- Status: Ready for Shape
- Value: Let an owner enable mailbox delivery with limits for requests and surplus.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0032/brief.md).
- Relationships: Uses B-0003 and B-0031.

## B-0033: Survival balance

- Status: Ready for Shape
- Value: Test costs, capacities, rates, limits, and loot for completed capabilities in Survival mode.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0033/brief.md).
- Relationships: Applies to completed features. Used by B-0035.

## B-0034: Cabin presentation

- Status: Ready for Shape
- Value: Make cabin controls, status, failures, Cabin windows, models, textures, and sounds clear and consistent.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0034/brief.md).
- Relationships: Applies to completed features that players can use. Used by B-0035.

## B-0035: Release verification and documentation

- Status: Ready for Shape
- Value: Verify progress in Survival mode, multiplayer recovery, and optional mod features. Documents state tested versions, migration limits, and extension formats.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0035/brief.md).
- Relationships: Uses completed features and B-0011, B-0033, and B-0034.

## B-0036: Local stock targets

- Status: Ready for Shape
- Value: Let owners select local stock targets that start production with limits through known recipes and installed capabilities.
- Direction: [Direction](direction.md).
- Feature Brief: [Stock targets brief](features/B-0036/brief.md).
- Relationships: Uses B-0024.

## B-0037: Upgrade funding plans

- Status: Ready for Shape
- Value: Let an owner confirm a production plan for one upgrade's missing materials. The plan does not install the upgrade automatically.
- Direction: [Direction](direction.md).
- Feature Brief: [Upgrade funding plan brief](features/B-0037/brief.md).
- Relationships: Uses B-0024 and upgrade funds.

## B-0038: Cabin book discovery and installation

- Status: Ready for Shape
- Value: Let players find book vendors in villages or create one through a bookstall. Players install cabin books to reveal upgrades.
- Direction: [Direction](direction.md).
- Feature Brief: [Cabin books brief](features/B-0038/brief.md).
- Relationships: Gives book sources for B-0004, B-0007, B-0008, B-0028, B-0029, B-0039 through B-0041, and B-0043.

## B-0039: Manual crafting room

- Status: Ready for Shape
- Value: Give the household a crafting room that works without storage or automation and uses installed storage automatically.
- Direction: [Manual production rooms](direction.md#manual-production-rooms).
- Relationships: Uses B-0005 and B-0038. Integrates B-0004. Used by B-0024.
- Feature Brief: [Manual crafting room brief](features/B-0039/brief.md).

## B-0040: Manual smelting room

- Status: Ready for Shape
- Value: Give the household a smelting room that works without storage or automation and uses installed storage automatically.
- Direction: [Manual production rooms](direction.md#manual-production-rooms).
- Relationships: Uses B-0005 and B-0038. Integrates B-0004. Used by B-0042.
- Feature Brief: [Manual smelting room brief](features/B-0040/brief.md).

## B-0041: Manual kitchen room

- Status: Ready for Shape
- Value: Give the household a kitchen that works without storage or automation and uses installed storage automatically.
- Direction: [Manual production rooms](direction.md#manual-production-rooms).
- Relationships: Uses B-0005 and B-0038. Integrates B-0004. Used by cooking in B-0025.
- Feature Brief: [Manual kitchen brief](features/B-0041/brief.md).

## B-0042: Smelting jobs

- Status: Ready for Shape
- Value: Let the household request automatic material processing through smelting capabilities unlocked separately from the manual room.
- Direction: [Manual production rooms](direction.md#manual-production-rooms).
- Relationships: Uses B-0004, B-0007, B-0024, and B-0040.

## B-0043: Manual potion room

- Status: Ready for Shape
- Value: Give the household a brewing room that works without storage or automation and uses installed storage automatically.
- Direction: [Manual production rooms](direction.md#manual-production-rooms).
- Relationships: Uses B-0005 and B-0038. Integrates B-0004. Used by brewing in B-0025.
- Feature Brief: [Manual potion room brief](features/B-0043/brief.md).
