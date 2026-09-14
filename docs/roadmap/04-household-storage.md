# Milestone 4: Household roles, mailbox and central storage

**Depends on:** Milestone 2's controller foundation.

**Outcome:** A cabin behaves as a household with clear owner, resident and guest boundaries, a safe public receiving mailbox and one authoritative cabin-owned inventory.

Major scope:

- local owner, resident and guest roles with destination-specific permission checks
- the bounded exterior receiving mailbox with insert-only public access and owner collection
- atomic, slot-based central cabin storage with explicit protected interfaces
- role-aware deposit, withdrawal, facility use and configuration permissions
- storage capacity upgrades, hard reserves and transaction-safe mutations
- local status visibility that reveals only information appropriate to each role
- the first storage/status interface, while ordinary chests and Tom's Simple Storage remain independent

## Household and mailbox specification

_Source: local-role and receiving-mailbox sections of the former network specification._

### Status and relationship to the MVP

This document specifies post-MVP roles, mailboxes, connection hallways, packed-cabin access and network-aware packing.

It deliberately supersedes these MVP restrictions once the relevant upgrades are present:

- packing may be initiated from inside rather than only outside
- a packed cabin may remain accessible through an upgraded connection
- occupants need evacuation only when packing would remove their final permitted exterior route

The authoritative UUID, lifecycle, crash-consistency and one-active-exterior invariants in [Milestone 0](00-safe-mvp.md) still apply.

Storage, sharing, loadouts and automation are specified in [Cabin Storage and Automation](07-production-automation.md). Functional rooms are specified in [Cabin Progression and Functional Rooms](02-progression-space.md).

### Local roles

Roles belong to one cabin. A role in one connected cabin never grants the same role in another cabin. Every destination doorway rechecks that cabin's current permissions.

Each cabin has exactly one owner.

#### Owner

The owner controls the cabin itself.

The owner may:

- change roles and permissions
- install structural, room and storage upgrades
- install and configure automation
- configure storage sharing
- create or leave hallway connections
- pack and deploy the cabin
- use personal loadouts, automatic restocking and saved requisition rules

#### Resident

A resident lives in the cabin and uses its shared facilities.

Residents may:

- enter through the exterior or hallway
- deposit into and withdraw from main storage
- consume cabin resources through normal use
- use shared crafting, kitchen, brewing, enchanting, stable and functional-room systems
- store and retrieve their own eligible mounts

Residents may not:

- use the owner's personal loadouts, automatic restocking or saved requisition rules
- change cabin-wide configuration or roles
- install upgrades
- alter network membership or doorway configuration
- change storage-sharing rules
- pack or deploy the cabin
- dismantle managed livestock populations

#### Guest

A guest may visit without receiving access to the household's main resources.

Guests may:

- enter through permitted exterior and hallway doors
- inspect the physical cabin and shared hallway
- use ordinary non-resource-consuming facilities
- see plain-language operational warnings and shortages
- deposit items into the owner's protected receiving mailbox

Guests may not:

- browse, deposit into or withdraw from main storage
- consume cabin resources through crafting or automation
- harvest managed rooms or remove animals
- inspect exact storage quantities, private loadouts or mailbox contents
- change any cabin or network configuration

### Receiving mailbox

Every cabin owner has one protected receiving mailbox from the beginning. It is not a guest-specific compartment and is separate from central cabin storage.

The mailbox initially appears as an exterior interface:

- anyone may insert items
- senders cannot inspect existing contents
- senders cannot withdraw previously inserted items
- only the cabin owner may inspect or collect contents
- capacity is bounded and a full mailbox rejects additional delivery without deleting anything

To give another player an item manually, a player visits that player's deployed cabin and places the item into its mailbox. Manual delivery does not require cabin-entry or main-storage permission.

Once the cabin joins a connection hallway, the same logical inbox also appears beside its labelled hallway door. Both interfaces address one persisted mailbox inventory; they never copy or shuttle stacks between separate containers.

When packed, the exterior mailbox interface disappears with the exterior anchor while the hallway interface remains usable. An unconnected packed cabin has no manual mailbox interface until redeployed.

Mailbox contents do not become available to storage or automation until the owner collects or explicitly accepts them.

Later targeted mailbox automations may:

- fulfil explicitly configured incoming requests
- transfer configured resources
- deliver eligible shared surplus between connected cabin mailboxes

Every automated delivery is bounded, atomic, attributable and rejected when the receiving mailbox is full.

## Central-storage specification

_Source: central-storage and local-access sections of the former storage specification._

### Status and relationship to the MVP

This document specifies the post-MVP cabin-owned storage, knowledge and automation track.

It complements ordinary Minecraft inventories rather than replacing them. Chests, Tom's Simple Storage and other compatible blocks continue to behave normally inside active cabin rooms under the rules in [Milestone 0](00-safe-mvp.md).

Functional-room production is specified in [Cabin Progression and Functional Rooms](02-progression-space.md). Roles, mailboxes and cabin connections are specified in [Cabin Network and Access](09-connected-cabins.md).

Recipes for installing storage, library and automation upgrades use the persistent world attunement defined by the progression specification. The ordinary crafting, cooking and brewing recipes executed after those systems are installed do not vary merely because cabin upgrades do.

### Central cabin storage

Each cabin UUID owns one authoritative persistent virtual inventory.

- Capacity is measured in Minecraft-style slots.
- Every item obeys its normal maximum stack size.
- Non-stackable and unique stacks occupy one slot each.
- Capacity increases only through explicit storage upgrades.
- Packing, deployment, exterior loss and hallway membership never transfer ownership of this inventory.
- Storage mutations are journalled or otherwise atomic so interrupted automation and network transfers cannot duplicate or delete items.

Players deposit and retrieve items through protected cabin interfaces. Ordinary placed inventories are independent and are not scanned, merged or consumed automatically. Moving items between ordinary storage and cabin storage is always an explicit player action or a separately configured integration.

Greenhouses, kitchens, crafting systems, brewing, enchanting and other cabin automation consume from and deposit into this central inventory through server-side transactions.

### Storage access

The role matrix in the network specification applies.

- Owners and residents may deposit, withdraw and consume resources through shared facilities.
- Guests cannot browse, deposit into or withdraw from main storage.
- Guest exchange uses the mailbox rather than main storage.
- Only the owner may change capacity, automation, sharing, reserve and loadout configuration.
