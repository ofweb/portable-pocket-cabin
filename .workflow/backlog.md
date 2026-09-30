# Backlog

## B-0002: Household roles

- Status: Ready for Shape
- Value: Give each cabin clear owner, resident, and guest capabilities.
- Direction: [Direction](direction.md).
- Feature Brief: [Household role brief](features/B-0002/brief.md).

## B-0003: Receiving mailbox

- Status: Ready for Shape
- Value: Let other players deliver items to a cabin through one safe, cabin-owned receiving mailbox.
- Direction: [Direction](direction.md).
- Relationships: Uses the household roles in B-0002.
- Feature Brief: [Receiving mailbox brief](features/B-0003/brief.md).

## B-0004: Central storage

- Status: Ready for Shape
- Value: Give each cabin one authoritative inventory for household use while ordinary chests remain independent.
- Direction: [Direction](direction.md).
- Relationships: Uses the household roles in B-0002; supports B-0007.
- Feature Brief: [Central storage brief](features/B-0004/brief.md).

## B-0005: Room purchase and traversal

- Status: Ready for Shape
- Value: Let a cabin purchase a bounded room, keep its own space, and reach it through a safe interior door.
- Direction: [Direction](direction.md).
- Feature Brief: [Room brief](features/B-0005/brief.md).
- Relationships: Uses B-0002; supports B-0019 through B-0023.

## B-0006: House cat

- Status: Ready for Shape
- Value: Let a previously tamed cat live safely in its owner's general cabin interior.
- Direction: [Direction](direction.md).
- Feature Brief: [House cat brief](features/B-0006/brief.md).
- Relationships: Uses household roles in B-0002.

## B-0007: Product knowledge and automation books

- Status: Ready for Shape
- Value: Let a cabin learn safe product templates and buy narrow automation capabilities after their books reveal the upgrades.
- Direction: [Direction](direction.md).
- Feature Brief: [Product knowledge brief](features/B-0007/brief.md).
- Relationships: Uses B-0004 and B-0038; supports B-0024 through B-0027 and B-0036 through B-0037.

## B-0008: Enchantment library

- Status: Ready for Shape
- Value: Let an owner install and improve an enchanting room where players learn enchantments and apply known levels manually.
- Direction: [Direction](direction.md).
- Feature Brief: [Enchantment library brief](features/B-0008/brief.md).
- Relationships: Uses B-0005 and B-0038; supports B-0028.

## B-0009: Connected cabin hallways

- Status: Ready for Shape
- Value: Let consenting owners connect separate cabins through a persistent shared hallway with protected doors.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0009/brief.md).
- Relationships: Uses B-0002; supports B-0030.

## B-0011: Optional mod compatibility

- Status: Ready for Shape
- Value: Test declared optional mod profiles and their behavior when content is present, absent, or changed.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0011/brief.md).
- Relationships: Applies to features with optional integrations; supports B-0035.

## B-0012: Palette renovation

- Status: Needs Direction
- Value: Explore whether owners need a way to change a cabin's established material palette.
- Direction: [Open question](direction.md#cabin-palette-changes).

## B-0013: Emergency fire packing

- Status: Needs Direction
- Value: Explore whether a cabin should pack automatically during a nearby fire and how occupants remain safe.
- Direction: [Open question](direction.md#automatic-packing-during-a-fire).

## B-0014: More cabin styles

- Status: Needs Direction
- Value: Explore additional exterior and room styles that preserve clear cabin identity.
- Direction: [Open question](direction.md#more-cabin-styles).

## B-0015: Cabin names and maps

- Status: Needs Direction
- Value: Explore naming cabins and showing their current sites on maps.
- Direction: [Open question](direction.md#cabin-names-and-maps).

## B-0016: Cabin sleep and exterior night

- Status: Needs Direction
- Value: Explore whether sleep inside a cabin should change the time outside its deployed exterior.
- Direction: [Open question](direction.md#cabin-sleep-and-exterior-night).

## B-0018: Wider material support

- Status: Needs Direction
- Value: Investigate safe support for more modded materials without inferring arbitrary mod internals.
- Direction: [Open question](direction.md#more-modded-materials).

## B-0019: Greenhouse

- Status: Ready for Shape
- Value: Let residents plant and harvest supported crops in a managed room with bounded passive growth.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005; automatic actions belong to B-0026.

## B-0020: Stable

- Status: Ready for Shape
- Value: Let residents house and release eligible tamed mounts while each animal keeps its identity.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005.

## B-0021: Aquatic berth

- Status: Ready for Shape
- Value: Add a flooded stable berth for an eligible tamed aquatic resident such as a nautilus.
- Direction: [Direction](direction.md).
- Relationships: Extends the stable in B-0020.

## B-0022: Livestock room

- Status: Ready for Shape
- Value: Let residents tend a bounded population of supported livestock and collect products through manual actions.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005; automatic actions belong to B-0026.

## B-0023: Forestry room

- Status: Ready for Shape
- Value: Let residents grow, fell, and replant supported trees in a managed room.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005; automatic actions belong to B-0026.

## B-0024: Bounded crafting jobs

- Status: Ready for Shape
- Value: Plan and run local crafting jobs with explicit recipes, resource reserves, timed work, and safe output handling.
- Direction: [Direction](direction.md).
- Feature Brief: [Crafting jobs brief](features/B-0024/brief.md).
- Relationships: Uses B-0004 and B-0007; supports B-0008, B-0025, B-0028, B-0036, and B-0037.

## B-0025: Cooking and brewing jobs

- Status: Ready for Shape
- Value: Extend bounded local jobs to supported cooking and brewing processes.
- Direction: [Direction](direction.md).
- Feature Brief: [Cooking and brewing brief](features/B-0025/brief.md).
- Relationships: Uses B-0024.

## B-0026: Room automation

- Status: Ready for Shape
- Value: Add separately enabled actions for greenhouse, livestock, and forestry rooms without changing their manual use.
- Direction: [Direction](direction.md).
- Feature Brief: [Room automation brief](features/B-0026/brief.md).
- Relationships: Uses B-0007, B-0024, and the relevant room in B-0019, B-0022, or B-0023.

## B-0027: Optional production profiles

- Status: Ready for Shape
- Value: Support declared optional cooking and processing profiles, including kiln work and later coal synthesis.
- Direction: [Direction](direction.md).
- Feature Brief: [Optional production brief](features/B-0027/brief.md).
- Relationships: Uses B-0025; requires explicit integration profiles.

## B-0028: Equipment requisitions

- Status: Ready for Shape
- Value: Let an owner select enchantments for automation and request valid equipment through a visible material and work plan.
- Direction: [Direction](direction.md).
- Feature Brief: [Equipment requisitions brief](features/B-0028/brief.md).
- Relationships: Uses B-0008, B-0024, and B-0038; supports B-0029.

## B-0029: Owner loadouts

- Status: Ready for Shape
- Value: Let a cabin maintain explicit equipment rules for its owner when that owner enters.
- Direction: [Direction](direction.md).
- Feature Brief: [Owner loadouts brief](features/B-0029/brief.md).
- Relationships: Uses B-0028.

## B-0030: Safe packed cabin access

- Status: Needs Direction
- Value: Let players use an occupied packed cabin through a connected hallway while they keep a safe exit.
- Direction: [Progress after vacancy](direction.md#progress-after-an-empty-cabin-becomes-occupied).
- Feature Brief: [Feature brief](features/B-0030/brief.md).
- Relationships: Uses B-0009 and the [portable-home decision](decisions/pdr/0001-preserve-the-portable-home.md).

## B-0031: Resource requests and surplus

- Status: Ready for Shape
- Value: Let connected cabins request resources and offer owner-approved surplus through attributed transfers without merging storage.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0031/brief.md).
- Relationships: Uses B-0009 and B-0024; supports B-0032.

## B-0032: Mailbox automation

- Status: Ready for Shape
- Value: Let an owner enable bounded mailbox delivery for declared requests and eligible surplus.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0032/brief.md).
- Relationships: Uses B-0003 and B-0031.

## B-0033: Survival balance

- Status: Ready for Shape
- Value: Test and tune costs, capacities, rates, limits, and loot for completed capabilities in survival play.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0033/brief.md).
- Relationships: Applies to completed features; supports B-0035.

## B-0034: Cabin presentation

- Status: Ready for Shape
- Value: Make cabin controls, status, failures, windows, models, textures, and sounds clear and consistent.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0034/brief.md).
- Relationships: Applies to completed player-facing features; supports B-0035.

## B-0035: Release verification and documentation

- Status: Ready for Shape
- Value: Verify survival progression, multiplayer recovery, and supported integrations. Document tested versions, migration limits, and extension formats.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0035/brief.md).
- Relationships: Uses completed features and B-0011, B-0033, and B-0034.

## B-0036: Local stock targets

- Status: Ready for Shape
- Value: Let owners set local stock targets that trigger bounded production through known recipes and installed capabilities.
- Direction: [Direction](direction.md).
- Feature Brief: [Stock targets brief](features/B-0036/brief.md).
- Relationships: Uses B-0024.

## B-0037: Upgrade funding plans

- Status: Ready for Shape
- Value: Let an owner approve a production plan for one upgrade's missing materials without installing the upgrade automatically.
- Direction: [Direction](direction.md).
- Feature Brief: [Upgrade funding plan brief](features/B-0037/brief.md).
- Relationships: Uses B-0024 and the existing upgrade funds.

## B-0038: Cabin book discovery and installation

- Status: Ready for Shape
- Value: Let players find book vendors in villages or create one through a bookstall job site, then install cabin books to reveal upgrades.
- Direction: [Direction](direction.md).
- Feature Brief: [Cabin books brief](features/B-0038/brief.md).
- Relationships: Supplies book unlocks to B-0007, B-0008, and B-0028.
