# Cabin Network and Access

## Status and relationship to the MVP

This document specifies post-MVP roles, mailboxes, connection hallways, packed-cabin access and network-aware packing.

It deliberately supersedes these MVP restrictions once the relevant upgrades are present:

- packing may be initiated from inside rather than only outside
- a packed cabin may remain accessible through an upgraded connection
- occupants need evacuation only when packing would remove their final permitted exterior route

The authoritative UUID, lifecycle, crash-consistency and one-active-exterior invariants in [`SPEC.md`](../../SPEC.md) still apply.

Storage, sharing, loadouts and automation are specified in [Cabin Storage and Automation](cabin-storage-and-automation.md). Functional rooms are specified in [Cabin Progression and Functional Rooms](cabin-progression-and-rooms.md).

## Local roles

Roles belong to one cabin. A role in one connected cabin never grants the same role in another cabin. Every destination doorway rechecks that cabin's current permissions.

Each cabin has exactly one owner.

### Owner

The owner controls the cabin itself.

The owner may:

- change roles and permissions
- install structural, room and storage upgrades
- install and configure automation
- configure storage sharing
- create or leave hallway connections
- pack and deploy the cabin
- use personal loadouts, automatic restocking and saved requisition rules

### Resident

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

### Guest

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

## Receiving mailbox

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

## Connection upgrade

Cabin networking is a permanent paid upgrade for each participating cabin.

- Every cabin must purchase its own connection upgrade before joining a hallway.
- The upgrade remains installed if the cabin later leaves; its cost is not refunded.
- An upgraded cabin may later join another hallway without repurchasing the structural upgrade.
- Obsidian is a core mid-game connection material.
- Stronger connection tiers may require Nether and End materials.
- Exact recipes and progression gates are data-driven.

Connection is never implied by physical proximity. Owners mutually approve creation or membership, and a cabin belongs to at most one hallway network at a time.

Once joined, hallway access is independent of the distance or supported exterior dimension between member cabins. It is deliberately limited fast travel: the network connects one personal cabin per member and never creates arbitrary world waypoints.

## Shared connection hallway

The first accepted connection creates a persistent hallway allocation and lightweight network UUID. The record exists to preserve:

- membership
- hallway cell identity
- protected doorway allocations
- access checks
- packing reachability
- crash recovery

It does not own a combined storage, automation or resource ledger.

Every member cabin has one protected, labelled doorway in the hallway. Traversing it enters that cabin's interior after rechecking the traveller's local role and entry permission.

The hallway's protected shell and cabin doorways cannot be broken or moved by ordinary players. The remaining space may be furnished with ordinary Minecraft blocks.

Player-placed hallway blocks behave as they would in a normal shared world:

- they are not part of any cabin's central storage
- they are not automatically protected by cabin roles
- inventories are not consumed by cabin automation
- ordinary multiplayer trust and any external claim/protection mods govern their use

Removing a cabin from the hallway seals and removes only its protected doorway projection. It never deletes the cabin interior, hallway or other players' furnishings. Hallway records are not automatically destroyed merely because membership becomes small or temporarily inactive; dismantling and administrative recovery must be explicit and safe.

## Packed-cabin access

Packing removes a cabin's exterior anchor, not its interior or hallway doorway.

An upgraded connected cabin remains reachable through its hallway while `PACKED`. Entering loads the required room cell, applies bounded managed catch-up and rechecks the destination cabin's permissions.

Ordinary cabin blocks remain paused while the cell is unoccupied and otherwise inactive. Access through a hallway does not cause an empty packed cabin to remain permanently chunk-loaded.

A packed cabin's ordinary exterior exit is unavailable. Occupants leave through its hallway door and another permitted cabin with a deployed exterior.

## Packing from inside

The owner may initiate packing from a protected internal cabin interface after purchasing the connection upgrade.

The normal validation still applies:

- the cabin must be in a packable lifecycle state
- the owner must have authority
- delivery of the current bound packed item must be guaranteed
- the exterior projection must reconcile successfully
- every required evacuation destination must be safe

Packing from inside does not require evacuating the owner when another permitted deployed exterior remains reachable through the hallway.

## Transactional network-aware packing

Packing is evaluated as a transaction against the hallway graph. This prevents two owners from simultaneously packing what each initially believes is the other's remaining exit.

The operation:

1. locks the affected hallway membership and relevant cabin lifecycle transitions
2. rejects new entry through the exterior being removed
3. computes exterior reachability separately for every online occupant using that player's destination-cabin permissions
4. identifies only occupants who would lose their final permitted route to a deployed exterior
5. validates safe evacuation destinations for those occupants
6. rechecks the graph, roles, exteriors and cabin lifecycle immediately before commit
7. evacuates only the occupants who would otherwise be stranded
8. commits `PACKED`, removes the exterior projection and activates the bound packed item
9. retains the hallway doorway and internal access
10. releases all graph and lifecycle locks

If any required evacuation cannot be guaranteed, packing aborts without removing the exterior.

A route counts only when:

- every traversed connection remains active
- the destination cabin permits that player to enter
- the destination cabin has a valid `DEPLOYED` exterior
- the exterior safe-destination resolver succeeds

Offline players do not block movement. On login, occupancy and network generation are revalidated. If the player no longer has a permitted hallway route to a deployed exterior, the shared emergency destination fallback from the core spec is used.

## Cabin-specific status

Status remains separated by cabin even inside a hallway network.

- Owners see complete diagnostics for their cabin.
- Residents see actionable shared-facility status but not private owner configuration.
- Guests may see plain-language warnings and shortages without gaining storage access or exact inventory visibility.
- Inspecting another connected cabin applies that destination's local visibility rules.
- Owners may publish exact resource requests or surplus offers to the hallway.
- The default interface never combines every cabin's shortages into one mandatory task list.

## Shared discoveries

When cabins connect through a hallway, installed automation discoveries and learned enchantments propagate permanently according to the storage and automation specification.

Ordinary learned items, meals and potions do not propagate. Leaving the hallway never removes knowledge already copied.

## Balancing and interface decisions intentionally deferred

- exact connection-upgrade recipes and tiers
- hallway dimensions and visual variants
- maximum cabins per hallway, if a practical server limit is needed
- mailbox slot count
- hallway creation, invitation and departure screen layout
- explicit hallway dismantling and administrative-recovery commands
- whether residents may bind their respawn point to a cabin bed
