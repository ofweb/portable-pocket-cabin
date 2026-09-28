# Backlog

## B-0002: Household roles

- Maturity: Understood
- Possible value: Give each cabin clear owner, resident, and guest capabilities.
- Source: [Household role brief](features/B-0002/brief.md).

## B-0003: Receiving mailbox

- Maturity: Framed
- Possible value: Let other players deliver items to a cabin through one safe, cabin-owned receiving mailbox.
- Relationships: Uses the household roles in B-0002.
- Source: [Receiving mailbox brief](features/B-0003/brief.md).

## B-0004: Central storage

- Maturity: Framed
- Possible value: Give each cabin one authoritative inventory for household use while ordinary chests remain independent.
- Relationships: Uses the household roles in B-0002; supports B-0007 and B-0010.
- Source: [Central storage brief](features/B-0004/brief.md).

## B-0005: Room purchase and traversal

- Maturity: Framed
- Possible value: Let a cabin purchase a bounded room, keep its own space, and reach it through a safe interior door.
- Feature Brief: [Room brief](features/B-0005/brief.md).
- Relationships: Uses B-0002; supports B-0019 through B-0023.
- Source: [Functional rooms milestone](../docs/roadmap/05-functional-rooms.md).

## B-0006: House cat

- Maturity: Framed
- Possible value: Let a previously tamed cat live safely in its owner's general cabin interior.
- Feature Brief: [House cat brief](features/B-0006/brief.md).
- Relationships: Uses household roles in B-0002.

## B-0007: Product knowledge and automation books

- Maturity: Framed
- Possible value: Let a cabin learn safe product templates and buy narrow automation capabilities after their books reveal the upgrades.
- Feature Brief: [Product knowledge brief](features/B-0007/brief.md).
- Relationships: Uses B-0004 and B-0038; supports B-0024 through B-0027 and B-0036 through B-0037.

## B-0008: Enchantment library

- Maturity: Framed
- Possible value: Let an owner install and improve an enchanting room where players learn enchantments and apply known levels manually.
- Feature Brief: [Enchantment library brief](features/B-0008/brief.md).
- Relationships: Uses B-0005 and B-0038; supports B-0028 and B-0010.
- Source: [Enchanting milestone](../docs/roadmap/08-enchanting-loadouts.md).

## B-0009: Connected cabin hallways

- Maturity: Framed
- Possible value: Let consenting owners connect separate cabins through a persistent shared hallway with protected doors.
- Relationships: Uses B-0002; supports B-0030 and B-0010.
- Source: [Connections milestone](../docs/roadmap/09-connected-cabins.md).

## B-0010: Shared discoveries

- Maturity: Framed
- Possible value: Let connected cabins share installed capabilities and learned enchantments without sharing inventories or private configuration.
- Relationships: Uses B-0007, B-0008, and B-0009.
- Source: [Logistics milestone](../docs/roadmap/10-cooperative-logistics.md).

## B-0011: Optional mod compatibility

- Maturity: Framed
- Possible value: Test declared optional mod profiles and their behavior when content is present, absent, or changed.
- Relationships: Applies to features with optional integrations; supports B-0035.
- Source: [Compatibility milestone](../docs/roadmap/11-compatibility-polish.md).

## B-0012: Palette renovation

- Maturity: Unclear
- Possible value: Explore whether owners need a way to change a cabin's established material palette.
- Source: [Exploratory ideas](../docs/roadmap/11-compatibility-polish.md#exploratory-backlog).

## B-0013: Emergency fire packing

- Maturity: Unclear
- Possible value: Explore whether a cabin should pack automatically during a nearby fire and how occupants remain safe.
- Source: [Exploratory ideas](../docs/roadmap/11-compatibility-polish.md#exploratory-backlog).

## B-0014: More cabin styles

- Maturity: Unclear
- Possible value: Explore additional exterior and room styles that preserve clear cabin identity.
- Source: [Exploratory ideas](../docs/roadmap/11-compatibility-polish.md#exploratory-backlog).

## B-0015: Cabin names and maps

- Maturity: Unclear
- Possible value: Explore naming cabins and showing their current sites on maps.
- Source: [Exploratory ideas](../docs/roadmap/11-compatibility-polish.md#exploratory-backlog).

## B-0016: Cabin sleep and exterior night

- Maturity: Unclear
- Possible value: Explore whether sleep inside a cabin should change the time outside its deployed exterior.
- Source: [Exploratory ideas](../docs/roadmap/11-compatibility-polish.md#exploratory-backlog).

## B-0018: Wider material support

- Maturity: Unclear
- Possible value: Investigate safe support for more modded materials without inferring arbitrary mod internals.
- Source: [Exploratory ideas](../docs/roadmap/11-compatibility-polish.md#exploratory-backlog).

## B-0019: Greenhouse

- Maturity: Framed
- Possible value: Let residents plant and harvest supported crops in a managed room with bounded passive growth.
- Relationships: Uses B-0005; automatic actions belong to B-0026.
- Source: [Functional rooms milestone](../docs/roadmap/05-functional-rooms.md).

## B-0020: Stable

- Maturity: Framed
- Possible value: Let residents house and release eligible tamed mounts while each animal keeps its identity.
- Relationships: Uses B-0005.
- Source: [Functional rooms milestone](../docs/roadmap/05-functional-rooms.md).

## B-0021: Aquatic berth

- Maturity: Framed
- Possible value: Add a flooded stable berth for an eligible tamed aquatic resident such as a nautilus.
- Relationships: Extends the stable in B-0020.
- Source: [Functional rooms milestone](../docs/roadmap/05-functional-rooms.md).

## B-0022: Livestock room

- Maturity: Framed
- Possible value: Let residents tend a bounded population of supported livestock and collect products through manual actions.
- Relationships: Uses B-0005; automatic actions belong to B-0026.
- Source: [Functional rooms milestone](../docs/roadmap/05-functional-rooms.md).

## B-0023: Forestry room

- Maturity: Framed
- Possible value: Let residents grow, fell, and replant supported trees in a managed room.
- Relationships: Uses B-0005; automatic actions belong to B-0026.
- Source: [Functional rooms milestone](../docs/roadmap/05-functional-rooms.md).

## B-0024: Bounded crafting jobs

- Maturity: Framed
- Possible value: Plan and run local crafting jobs with explicit recipes, resource reserves, timed work, and safe output handling.
- Feature Brief: [Crafting jobs brief](features/B-0024/brief.md).
- Relationships: Uses B-0004 and B-0007; supports B-0008, B-0025, B-0028, B-0036, and B-0037.

## B-0025: Cooking and brewing jobs

- Maturity: Framed
- Possible value: Extend bounded local jobs to supported cooking and brewing processes.
- Feature Brief: [Cooking and brewing brief](features/B-0025/brief.md).
- Relationships: Uses B-0024.

## B-0026: Room automation

- Maturity: Framed
- Possible value: Add separately enabled actions for greenhouse, livestock, and forestry rooms without changing their manual use.
- Feature Brief: [Room automation brief](features/B-0026/brief.md).
- Relationships: Uses B-0007, B-0024, and the relevant room in B-0019, B-0022, or B-0023.

## B-0027: Optional production profiles

- Maturity: Framed
- Possible value: Support declared optional cooking and processing profiles, including kiln work and later coal synthesis.
- Feature Brief: [Optional production brief](features/B-0027/brief.md).
- Relationships: Uses B-0025; requires explicit integration profiles.

## B-0028: Equipment requisitions

- Maturity: Framed
- Possible value: Let an owner select enchantments for automation and request valid equipment through a visible material and work plan.
- Feature Brief: [Equipment requisitions brief](features/B-0028/brief.md).
- Relationships: Uses B-0008, B-0024, and B-0038; supports B-0029.
- Source: [Enchanting milestone](../docs/roadmap/08-enchanting-loadouts.md).

## B-0029: Owner loadouts

- Maturity: Framed
- Possible value: Let a cabin maintain explicit equipment rules for its owner when that owner enters.
- Feature Brief: [Owner loadouts brief](features/B-0029/brief.md).
- Relationships: Uses B-0028.
- Source: [Enchanting milestone](../docs/roadmap/08-enchanting-loadouts.md).

## B-0030: Safe packed cabin access

- Maturity: Framed
- Possible value: Let permitted players reach a packed cabin through a connected hallway while a safe exit route remains available.
- Relationships: Uses B-0009 and the [portable-home decision](decisions/pdr/0001-preserve-the-portable-home.md).
- Source: [Connections milestone](../docs/roadmap/09-connected-cabins.md).

## B-0031: Resource requests and surplus

- Maturity: Framed
- Possible value: Let connected cabins request resources and offer owner-approved surplus through attributed transfers without merging storage.
- Relationships: Uses B-0009 and B-0024; supports B-0032.
- Source: [Logistics milestone](../docs/roadmap/10-cooperative-logistics.md).

## B-0032: Mailbox automation

- Maturity: Framed
- Possible value: Let an owner enable bounded mailbox delivery for declared requests and eligible surplus.
- Relationships: Uses B-0003 and B-0031.
- Source: [Logistics milestone](../docs/roadmap/10-cooperative-logistics.md).

## B-0033: Survival balance

- Maturity: Framed
- Possible value: Test and tune costs, capacities, rates, limits, and loot for completed capabilities in survival play.
- Relationships: Applies to completed features; supports B-0035.
- Source: [Compatibility milestone](../docs/roadmap/11-compatibility-polish.md).

## B-0034: Cabin presentation

- Maturity: Framed
- Possible value: Make cabin controls, status, failures, windows, models, textures, and sounds clear and consistent.
- Relationships: Applies to completed player-facing features; supports B-0035.
- Source: [Compatibility milestone](../docs/roadmap/11-compatibility-polish.md).

## B-0035: Release verification and documentation

- Maturity: Framed
- Possible value: Verify survival progression, multiplayer recovery, and supported integrations. Document tested versions, migration limits, and extension formats.
- Relationships: Uses completed features and B-0011, B-0033, and B-0034.
- Source: [Compatibility milestone](../docs/roadmap/11-compatibility-polish.md).

## B-0036: Local stock targets

- Maturity: Framed
- Possible value: Let owners set local stock targets that trigger bounded production through known recipes and installed capabilities.
- Feature Brief: [Stock targets brief](features/B-0036/brief.md).
- Relationships: Uses B-0024.

## B-0037: Upgrade funding plans

- Maturity: Framed
- Possible value: Let an owner approve a production plan for one upgrade's missing materials without installing the upgrade automatically.
- Feature Brief: [Upgrade funding plan brief](features/B-0037/brief.md).
- Relationships: Uses B-0024 and the existing upgrade funds.

## B-0038: Cabin book discovery and installation

- Status: Ready for Shape
- Value: Let players find book vendors in villages or create one through a bookstall job site, then install cabin books to reveal upgrades.
- Direction: [Portable household goal](direction.md).
- Feature Brief: [Cabin books brief](features/B-0038/brief.md).
- Relationships: Supplies book unlocks to B-0007, B-0008, and B-0028.
