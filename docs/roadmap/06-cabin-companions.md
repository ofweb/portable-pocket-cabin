# Milestone 6: Cabin companions

**Depends on:** [Milestone 2](02-progression-space.md) general-space geometry and [Milestone 4](04-household-storage.md) household permissions.

**Outcome:** One previously tamed house cat can live safely in its owner's general cabin interior without becoming stored inventory, a production unit or an invulnerable outdoor combat pet.

**Status:** Draft. Delivery 6.1 requires final alignment before implementation.

## Scope

Delivery 6.1 adds one house cat to the general interior without requiring a stable. It covers registration, indoor behavior, protection, persistence and release of the same entity.

Other pets, resident-owned companions, breeding systems, packed-time production and functional-room animals remain out of scope.

## Registration

The cabin owner first tames a cat normally. While the cabin is deployed, a protected companion control may register one nearby cat that:

- is a normal tamed cat
- belongs to the cabin owner
- is not already homed to another cabin
- is not already managed by another system
- can be placed at a safe general-interior home position

Wild cats, another player's cats and a second house cat are rejected with distinct reasons.

Registration uses a recoverable transition. It records the cat UUID and intended cabin before moving that same entity from beside the exterior to its interior home. Reconciliation completes or reverses an interrupted move without creating another cat.

## Authority and persistence

The cat entity remains authoritative. The cabin registry stores only:

- its UUID
- its cabin home
- its safe interior home position
- any pending registration or release

The system never recreates the cat from a copied entity snapshot. Vanilla entity persistence retains its owner, appearance, custom name, health and other state while interior chunks unload.

Packing, restart, hallway access and owner logout leave the cat at home. If commands or save corruption remove the entity, reconciliation clears the stale binding and reports the loss instead of spawning a duplicate.

## Indoor behavior

A house cat remains in the general interior when its owner leaves. It cannot automatically cross the cabin exit, a hallway door or a functional-room boundary.

While standing, it may roam within safe interior bounds and favor beds, warm blocks, carpets, window perches and nearby players. The owner retains normal sit and stand interactions.

The cat may sleep near its sleeping owner and produce a vanilla cat gift only after a real completed sleep event while the interior is active. It gains no gifts, breeding progress or other output from inactive or packed time.

## Protection

The cabin prevents ordinary damage, boundary escape and unauthorized movement of the house cat. If pathfinding, vehicles or player construction move it outside safe interior bounds, the controller returns it to its recorded home position.

Only the cabin owner may change its sit state, registration or home assignment. Other players cannot release, lead, capture in a vehicle or otherwise remove it from the cabin.

Protection applies only while the cat is homed. Because it cannot leave automatically and loses protection on release, it cannot serve as an invulnerable outdoor combat pet.

## Release

The owner releases the cat through the companion control:

1. Require a deployed cabin and a loaded authoritative cat entity.
2. Find a bounded, collision-free and hazard-free position beside the exterior.
3. Persist a pending release.
4. Move the same entity UUID to the exterior destination.
5. Remove cabin protection and clear the home binding after the move succeeds.

Failure leaves the cat homed and reports the reason. A packed cabin cannot release its cat and explains that it must be redeployed first. Startup and live reconciliation complete an interrupted release without duplicating or losing the entity.

## Technical approach

A companion service owns registration, boundary enforcement, release and reconciliation. The registry binding is an association and transition journal, not an entity snapshot. Entity lookup and movement are server-authoritative, UUID-based and serialized per cabin.

The active general interior supplies the cat's ordinary ticking behavior. No packed-time simulation or companion-specific chunk ticket is added. Protection hooks reject damage and unauthorized transport only for the currently bound UUID inside its home cabin.

## Evergreen acceptance contract

Milestone 6 remains accepted only while automated tests and targeted manual checks establish that:

1. Only the cabin owner's eligible tamed cat can register, and each cabin accepts at most one.
2. Registration moves the same UUID into a safe interior position and recovers cleanly after interruption.
3. Packing, restart, redeployment and owner logout preserve the entity and home binding.
4. A standing cat roams safely, a sitting cat remains controllable by its owner, and neither crosses a cabin boundary automatically.
5. Ordinary damage, leads, vehicles and unauthorized interactions cannot remove a homed cat.
6. Gifts require a real active-interior sleep event; inactive time creates no gifts, breeding or other output.
7. Release requires a deployed exterior, preserves the UUID and state, and fails unchanged without a safe destination.
8. Reconciliation never creates a replacement for a missing cat or leaves one UUID bound to two cabins.
9. Releasing the cat removes cabin protection and restores ordinary exterior behavior.

Codec, companion-service, entity-transition, protection, GameTest and dedicated-server restart suites are the automated gates. Manual acceptance covers roaming, preferred resting places, sleep gifts, sit/stand interaction and visual behavior.

## Out of scope

- more than one house cat per cabin
- pets owned by residents or guests
- wolves, birds or other companion species
- stable, livestock or production behavior
- breeding or packed-time catch-up
- following players outside or between cabin spaces
- recreating a missing cat from saved state
- exact companion-control art or placement before delivery alignment
