# Milestone 9: Connected cabins and safe packed-cabin access

**Depends on:** [Milestone 0](00-safe-mvp.md) lifecycle safety and [Milestone 4](04-household-storage.md) household permissions.

**Outcome:** Mutually consenting owners connect cabins through a persistent shared hallway, and packed cabins remain reachable while each player retains a permitted route to a deployed exterior.

**Status:** Draft. Each delivery requires alignment before implementation.

## Scope

This milestone adds:

- a permanent connection upgrade and mutually approved hallway membership
- one separately allocated shared hallway with protected cabin doors
- permission-checked access to connected packed cabins
- interior packing with graph-locked reachability and evacuation
- hallway projections of existing mailboxes
- safe membership removal and administrative recovery

Hallways never combine cabin storage, automation, roles or resource ownership. [Milestone 10](10-cooperative-logistics.md) owns discovery sharing, requests, surplus offers and inter-cabin transfers.

## Connection upgrade

Each participating cabin must purchase its own permanent connection upgrade before joining a hallway. Leaving retains the installed upgrade without refund, so the cabin may join another hallway later.

Obsidian anchors the hallway and doorway; amethyst resonance identifies and routes destinations. Higher tiers may require Nether and End materials. Recipes and progression gates are data-driven and use the world's persisted attunement.

Physical proximity never creates a connection. Owners mutually approve creation and membership, and a cabin belongs to at most one hallway network. Once connected, supported exterior dimension and distance do not affect hallway access. Networks connect member cabins only, never arbitrary world waypoints.

## Shared hallway

The first accepted connection creates one permanent hallway cell and network UUID. Its persisted record contains membership, cell identity, doorway allocations, graph generation and recovery state.

Each member has one protected, labelled doorway. Traversal rechecks the destination cabin's role and entry policy before loading and entering its general interior.

The shell and cabin doorways resist ordinary breaking, explosions and pistons. All other hallway space accepts ordinary Minecraft blocks:

- furnishings do not belong to cabin storage
- cabin roles grant no automatic protection
- cabin automation never consumes hallway inventories
- ordinary multiplayer trust and external protection mods govern player blocks

Removing a member seals and removes only its protected doorway. It never deletes the cabin, hallway or player furnishings. A small or inactive network persists until owners explicitly dismantle it or an administrator performs recovery.

## Packed-cabin access

Packing removes a cabin's exterior anchor but retains its interior and hallway doorway. A connected upgraded cabin remains reachable while `PACKED`.

Entry loads only the required cabin or room cell, applies its bounded managed catch-up and rechecks destination permissions. Empty packed cabins receive no permanent chunk ticket, and ordinary blocks remain paused while inactive.

A packed cabin has no exterior exit. Its occupants leave through the hallway and another cabin whose entry policy permits them to reach a valid deployed exterior.

Hallway entry uses the same destination-local behavior as exterior entry. It projects [Milestone 4's](04-household-storage.md) single logical mailbox beside the cabin door and triggers [Milestone 8](08-enchanting-loadouts.md) owner-loadout reconciliation without copying either state.

## Packing from inside

The connection upgrade lets the owner initiate packing from a protected interior interface. Validation still requires:

- a packable lifecycle state and current owner authority
- guaranteed delivery of the bound packed item
- a reconciled exterior projection
- safe destinations for every occupant who would lose their final permitted exit

The owner may remain inside when another permitted route to a deployed exterior survives.

## Network-aware packing

Packing is one transaction against the hallway graph:

1. Lock affected membership and cabin lifecycle transitions.
2. Reject new entry through the exterior being removed.
3. Compute deployed-exterior reachability for each online occupant under that player's destination permissions.
4. Validate evacuation only for occupants losing their final permitted route.
5. Recheck graph generation, roles, exteriors and lifecycle.
6. Evacuate those occupants, commit `PACKED`, remove the exterior and activate the packed item.
7. Retain the hallway doorway and release all locks.

Failure to guarantee any required evacuation aborts before exterior removal. Concurrent packing cannot let two owners each rely on the other's disappearing exterior.

A usable route requires active membership, destination entry permission, a valid `DEPLOYED` exterior and a safe loaded position outside it.

Offline players do not block packing. Login revalidates occupancy, graph generation and permitted routes. A player without a valid exit uses [Milestone 0's](00-safe-mvp.md) emergency destination chain.

Membership removal and network dismantling use the same graph lock and per-player reachability checks. They seal only affected protected projections and fail before stranding an online occupant.

## Technical approach

One server-authoritative graph service owns network UUIDs, generations, membership and doorway allocations. Traversal, packing, login recovery and membership changes share its route resolver and lock order. Persisted transitions make doorway projection and graph mutation idempotent after interruption.

Status remains cabin-specific and follows [Milestone 4's](04-household-storage.md) visibility rules. Inspecting a connected cabin never grants more information than inspecting it locally.

## Evergreen acceptance contract

Milestone 9 remains accepted only while automated tests and targeted manual checks establish that:

1. Only upgraded cabins join through mutual owner consent, and each cabin belongs to at most one network.
2. Hallway allocation, membership and protected doorway identity survive restart without exposing another cell.
3. Destination roles and entry policy are rechecked on every traversal and permission change.
4. Packed access loads only required cells and never grants ordinary inactive blocks background simulation.
5. Mailbox and loadout projections use existing cabin state without copying it.
6. Concurrent packing preserves at least one permitted exit per occupant or evacuates that occupant safely.
7. Failed packing, membership removal or dismantling leaves exteriors, graph state and furnishings unchanged.
8. Login after graph or permission changes returns stranded players through the emergency destination chain.
9. Removing one member seals only its doorway and never deletes cabin or player-owned blocks.
10. Dedicated-server recovery completes interrupted graph and doorway transitions idempotently.

Graph, route, permission, lifecycle, concurrency, GameTest and dedicated-server restart suites are the automated gates. Manual acceptance covers labelled doors, hallway furnishing, traversal presentation and multiplayer timing.

## Out of scope

- combined storage, roles, automation or status ledgers
- arbitrary world waypoints
- resource requests, surplus offers or inter-cabin transfers
- automatic destruction of small or inactive networks
- cabin ownership transfer
- final hallway geometry, connection costs and interface art before delivery alignment
