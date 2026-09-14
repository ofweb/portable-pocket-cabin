# Milestone 9: Connected cabins and safe packed-cabin access

**Depends on:** Stable local permissions from Milestone 4 and lifecycle regression coverage from Milestone 1.

**Outcome:** Mutually consenting owners connect cabins through a persistent shared hallway, and a packed cabin remains reachable when the player still has a permitted route to another deployed exterior.

Major scope:

- connection upgrade, mutual consent and persistent hallway membership graph
- separately allocated shared hallways with labelled protected cabin doors
- ordinary, non-cabin-owned hallway furnishing behavior
- permission rechecks at every destination doorway
- hallway access to packed cabins without permanently loading empty rooms
- packing initiated from the protected interior interface
- graph-locked, per-occupant reachability checks and network-aware evacuation
- hallway mailbox projection backed by the same logical inbox
- explicit safe membership removal, doorway sealing and administrative recovery

**Red:** Add failing consent, permission-revocation, simultaneous-pack, last-exit, offline-login, stale-network-generation and hallway-removal tests.

**Green:** Connect two cabins first, then permit one to pack only when every affected player retains a safe route or can be evacuated.

**Refactor:** Centralise graph locking and route validation so exterior entry, interior packing and login recovery cannot disagree.

**Exit gate:** Concurrent packing cannot strand a player, expose an unauthorised destination, remove another cabin's doorway or delete hallway furnishings.


## Consolidated specification

_Source: connection, hallway, packed-access and network-safe packing sections of the former network specification._

### Connection upgrade

Cabin networking is a permanent paid upgrade for each participating cabin.

- Every cabin must purchase its own connection upgrade before joining a hallway.
- The upgrade remains installed if the cabin later leaves; its cost is not refunded.
- An upgraded cabin may later join another hallway without repurchasing the structural upgrade.
- Obsidian forms the dimensional anchor that keeps the hallway and cabin doorway fixed in the pocket dimension.
- Amethyst resonance identifies destinations and performs the magical routing between anchors.
- Stronger connection tiers may require Nether and End materials.
- Exact recipes and progression gates are data-driven and resolve their variable ingredients through the persistent world attunement in the progression specification.

Connection is never implied by physical proximity. Owners mutually approve creation or membership, and a cabin belongs to at most one hallway network at a time.

Once joined, hallway access is independent of the distance or supported exterior dimension between member cabins. It is deliberately limited fast travel: the network connects one personal cabin per member and never creates arbitrary world waypoints.

### Shared connection hallway

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

### Packed-cabin access

Packing removes a cabin's exterior anchor, not its interior or hallway doorway.

An upgraded connected cabin remains reachable through its hallway while `PACKED`. Entering loads the required room cell, applies bounded managed catch-up and rechecks the destination cabin's permissions.

Ordinary cabin blocks remain paused while the cell is unoccupied and otherwise inactive. Access through a hallway does not cause an empty packed cabin to remain permanently chunk-loaded.

A packed cabin's ordinary exterior exit is unavailable. Occupants leave through its hallway door and another permitted cabin with a deployed exterior.

### Packing from inside

The owner may initiate packing from a protected internal cabin interface after purchasing the connection upgrade.

The normal validation still applies:

- the cabin must be in a packable lifecycle state
- the owner must have authority
- delivery of the current bound packed item must be guaranteed
- the exterior projection must reconcile successfully
- every required evacuation destination must be safe

Packing from inside does not require evacuating the owner when another permitted deployed exterior remains reachable through the hallway.

### Transactional network-aware packing

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

### Cabin-specific status

Status remains separated by cabin even inside a hallway network.

- Owners see complete diagnostics for their cabin.
- Residents see actionable shared-facility status but not private owner configuration.
- Guests may see plain-language warnings and shortages without gaining storage access or exact inventory visibility.
- Inspecting another connected cabin applies that destination's local visibility rules.
- Owners may publish exact resource requests or surplus offers to the hallway.
- The default interface never combines every cabin's shortages into one mandatory task list.
