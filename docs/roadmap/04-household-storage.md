# Milestone 4: Household roles, mailbox and central storage

**Depends on:** [Milestone 0](00-safe-mvp.md) identity and access; [Milestone 3](03-upgrade-interface.md) upgrade funding.

**Outcome:** Each cabin has clear household roles, a safe public receiving mailbox and one authoritative cabin-owned inventory.

**Status:** Delivery 4.1 is aligned; implementation is pending. Later deliveries remain draft.

## Scope

This milestone adds:

- cabin-local owner, resident and guest roles with fixed capabilities
- a bounded public mailbox with owner-only collection
- slot-based central storage through protected interfaces
- storage capacity upgrades and atomic mutations
- role-filtered local storage and status information

Ordinary inventories, including chests and Tom's Simple Storage, remain independent. [Milestone 7](07-production-automation.md) owns reserves and automation, [Milestone 9](09-connected-cabins.md) owns connected and packed-cabin access, and [Milestone 10](10-cooperative-logistics.md) owns inter-cabin logistics.

## Household roles

Roles belong to one cabin. A role in one cabin grants nothing in another, and every action rechecks the destination cabin's current role. Role capabilities are fixed; Milestone 4 adds no entry policy or customizable permission matrix.

Each cabin has exactly one fixed owner. Explicit assignments make other players residents; every other player is a guest. Removing a resident returns that player to the default guest role. Ownership transfer is out of scope.

The owner manages residents through `/cabin resident add <player>` and `/cabin resident remove <player>`. `/cabin household list` shows the owner and explicit residents. Commands work while the cabin is deployed or packed, resolve online or previously known players and make no change for an unknown player or an already-satisfied request. The earlier `trust` and `access` commands are removed without aliases.

### Owner

The owner may:

- assign resident roles
- install cabin, room and storage upgrades
- deposit into, browse and withdraw from central storage
- inspect and collect mailbox contents
- configure storage capacity and later owner-only systems
- pack and deploy the cabin under [Milestone 0](00-safe-mvp.md)

Later milestones add network, automation, sharing and loadout controls without changing ownership.

### Resident

A resident may:

- enter the cabin
- deposit into, browse and withdraw from central storage
- consume cabin resources through permitted shared facilities
- use shared crafting, kitchen, brewing, enchanting, stable and functional-room systems
- store and retrieve their own eligible mounts

A resident may not:

- change roles or cabin-wide configuration
- install upgrades
- configure storage capacity, sharing, automation or owner loadouts
- pack or deploy the cabin
- dismantle managed livestock populations

### Guest

A guest may:

- enter and inspect the physical cabin
- inspect the cabin upgrade interface
- see plain-language operational warnings and shortages
- deposit items into the public mailbox

A guest may not:

- browse, deposit into or withdraw from central storage
- consume cabin resources through crafting or automation
- place or break blocks, open inventories, interact with entities or otherwise mutate the cabin interior
- harvest managed rooms or remove animals
- inspect exact storage quantities, private owner configuration or mailbox contents
- change cabin configuration

Public mailbox delivery does not require cabin entry or central-storage permission.

## Receiving mailbox

Every cabin begins with one protected receiving mailbox separate from central storage. Its initial interface appears on the deployed exterior.

- anyone may insert items
- senders cannot inspect contents or withdraw deposited items
- only the owner may inspect or collect contents
- capacity is bounded
- a full mailbox rejects the complete attempted delivery without deleting or displacing items
- contents persist through restart, packing, redeployment and exterior loss

Packing removes the exterior interface but not the mailbox or its contents. An unconnected packed cabin has no manual mailbox interface until redeployed. [Milestone 9](09-connected-cabins.md) may project the same logical mailbox beside a connected hallway door; multiple interfaces must never copy or shuttle stacks between inventories.

Mailbox contents remain unavailable to storage and automation until the owner collects or explicitly accepts them. [Milestone 10](10-cooperative-logistics.md) owns request fulfilment, surplus delivery and other automated transfers. Every later automated delivery must remain bounded, atomic, attributable and reject a full destination without loss.

## Central storage

After installation, each cabin UUID owns one authoritative persistent virtual inventory:

- capacity is measured in Minecraft-style slots
- each item obeys its normal maximum stack size
- non-stackable and unique stacks occupy one slot each
- only storage upgrades increase capacity
- packing, deployment, exterior loss and later network membership do not change ownership
- interrupted or concurrent mutations cannot duplicate, lose or partially move items

Players use protected cabin interfaces to deposit and retrieve items. Ordinary placed inventories are never scanned, merged or consumed automatically. Moving items between them and central storage requires an explicit player action or a later configured integration.

The owner may explicitly authorize one cabin upgrade to take its remaining exact requirements from central storage. Materials already committed to that target's fund are used first. Storage never fills funds or installs upgrades automatically; [Milestone 3](03-upgrade-interface.md) remains authoritative for funding and installation.

Later greenhouses, kitchens, crafting systems, brewing, enchanting and room automation use server-side storage transactions defined by their owning milestones.

## Access and status

- Owners and residents may deposit, browse, withdraw and consume resources through permitted shared facilities.
- Guests cannot browse, deposit into or withdraw from central storage.
- Guest exchange uses the mailbox.
- Only the owner may change capacity or later automation, sharing, reserve and loadout configuration.
- Owners see exact storage, mailbox and configuration status.
- Residents see exact shared-storage state and actionable failures for facilities they may use.
- Guests see plain-language warnings without exact quantities or private configuration.

Losing permission invalidates or refreshes an open interaction before another mutation. Every mutation rechecks cabin identity, lifecycle, role, capacity and offered stacks immediately before commit. Failure leaves every involved inventory unchanged and returns an actionable reason.

## Technical approach

Delivery 4.1 advances the registry to schema 8 and persists resident assignments. The owner remains part of the cabin identity, and the guest role is derived rather than stored. Because the mod is unreleased, schemas 3–7 are rejected with backup and fresh-world guidance instead of migrating trusted players or entry policies. Later deliveries will add mailbox contents, central-storage contents and capacity to the current schema at their implementation boundaries.

One server-authoritative permission policy resolves owner, resident and guest capabilities. Delivery 4.1 applies it to entry, upgrade funding, role commands, menu presentation and direct player mutation of the cabin interior. Role changes persist immediately; open upgrade interfaces refresh before accepting another action.

Mailbox and central storage are cabin-owned virtual inventories, not exposed block entities. Protected menus synchronize their state, but the server owns permission and mutation decisions. Each transfer validates its complete source and destination result before one atomic commit; multi-step or recoverable operations persist enough intent to complete without duplication or loss after interruption.

## Evergreen acceptance contract

Milestone 4 remains accepted only while automated tests and targeted manual checks establish that:

1. Schemas earlier than 8 fail closed with backup and fresh-world guidance.
2. Every player resolves to exactly one owner, resident or guest role with the fixed capability matrix.
3. Permission loss closes or invalidates open mailbox and storage interactions before another mutation.
4. Public mailbox insertion reveals no contents, permits no withdrawal and rejects full deliveries without item loss.
5. Only the owner can inspect or collect mailbox contents.
6. Central storage enforces slot capacity, normal stack limits and unique-stack occupancy.
7. Concurrent, stale and interrupted transfers commit completely once or leave source and destination unchanged.
8. Roles, capacity and exact mailbox and storage stacks survive restart, packing and redeployment.
9. Owner-authorized upgrade funding consumes existing fund contents first and never installs automatically.
10. Ordinary inventories remain independent, and no role exposes private quantities or configuration beyond its status rules.

Codec, service, menu-integration and dedicated-server restart tests are the automated gates. Manual acceptance covers the protected interfaces, permission changes, status visibility and multiplayer concurrency.

## Out of scope

- customizable capability matrices or per-player overrides
- configurable entry policies or ownership transfer
- connection hallways, packed-cabin access and network-aware packing
- storage sharing and inter-cabin resource transfer
- hard reserves, learned templates and production automation
- owner loadouts and automatic restocking
- automated mailbox fulfilment or surplus delivery
- automatic scanning or merging of ordinary inventories
- final interface art, dimensions and widgets
- exact initial mailbox and storage capacities or upgrade costs
