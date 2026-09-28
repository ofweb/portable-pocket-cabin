# A house cat can live in a cabin

Status: Draft
Feature ID: B-0006

## Goal

A cabin owner can give one tamed cat a safe home in the general space. The cat keeps its identity and Minecraft cat behavior.

## Stories and acceptance

### S1: Give a cat a home

Story: An owner with a deployed cabin assigns a tamed cat near the cabin. The same cat enters the safe interior.

Acceptance:

- Only the cabin owner can assign a cat. The cat must have that player as its owner.
- The cat must have no other cabin home and no assignment to a different system.
- The cabin accepts one house cat at most. It gives a different reason for a cat without an owner, a cat with a different owner, or one more cat.
- If the cabin has no safe interior home position, the cat does not enter and the cabin has no house cat.
- The cabin saves the assigned cabin and safe interior home position as two values.
- The cat keeps its owner, name, health, and all other state when the cabin moves the cat.
- If the transfer stops, recovery completes the transfer or moves the cat back. Recovery cannot make a copy or assign the cat to two cabins.

### S2: Live in the cabin

Story: A house cat lives in the general space while its owner is in the cabin or in the world.

Acceptance:

- A standing cat can move in the safe area and stay near beds, warm blocks, carpets, perches near a cabin window, and players.
- Only the owner can make the cat sit or stand. Other players cannot make a change to its sit state or assigned cabin.
- The cat stays in the general space. The cat cannot use the cabin exit or enter a hallway or room.
- The cat is safe from damage while it has a cabin home. Other players cannot put the cat on a lead or in a vehicle. They cannot remove the cat.
- If the cat moves out of the safe area, the cabin moves the cat to its saved home position.
- If player blocks put the cat out of the safe area, the cabin moves the cat to its saved home position.
- The cat can sleep near its owner. It gives a cat gift only after the owner completes sleep while the interior is active.
- The cat makes no gifts, breeding progress, or other output while the interior is not active or the cabin is packed.

### S3: Keep the cat through cabin travel

Story: An owner enters the cabin after packing or restart and finds the same cat at home.

Acceptance:

- The cat keeps its assigned cabin, home position, and state through packing, restart, and redeployment. Hallway access does not remove these values.
- The cat stays in its home when the owner is not online. Packing does not make the cat an item.
- If the cat is missing, the cabin clears its assignment and home position and tells the owner that the cat is missing.
- The cabin does not make a replacement cat.

### S4: Release the cat

Story: An owner with a deployed cabin releases its house cat at a safe position near the cabin. The same cat has no cabin protection.

Acceptance:

- Only the owner can release the cat. The cat entity must be in a loaded chunk. A safe position near the cabin must be available.
- If the cabin is packed, it tells the owner to deploy the cabin before the owner releases the cat.
- If the cat entity is not available or there is no safe position, the cabin gives a reason and keeps the cat's home.
- After the owner releases the cat, the cat keeps its identity and state. The cabin clears its assigned cabin and home position. Cabin protection stops.
- If the action stops while the owner releases the cat, recovery completes the action or keeps the cat at home. The cabin cannot make a copy or delete saved home data.

## Feature-wide constraints and acceptance

- The cat entity keeps its Minecraft state. The cabin saves only the cat identity, assigned cabin, safe interior home position, and transfer status.
- A cat without a cabin home has no cabin protection. The cabin moves the cat to the exterior only when the owner releases the cat.

## Scope

This feature includes one house cat in the general space, assignment, protection, persistence, recovery, and the owner action to release the cat.

## Non-goals

- Other pets, cats with resident or guest owners, stable animals, and animals in rooms.
- Two cats in one cabin, breeding, or output while the interior is not active.
- The cat does not follow players into the world or other cabin spaces. The cabin does not make a replacement cat.

## Related records

- [Cat](../../context.md#house-cat).
- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [General space](../../decisions/adr/0008-derive-expansion-geometry-from-saved-size.md).
- [Cabin identity decision](../../decisions/adr/0005-authoritative-cabin-registry.md).

## Open questions and assumptions

- The control, its position, and the distance limit for cat assignment are open.
- The check for assignment to a different system is open.
