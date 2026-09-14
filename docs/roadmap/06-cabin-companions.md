# Milestone 6: Cabin companions

**Depends on:** Milestones 2 and 4.

**Outcome:** A previously tamed house cat can live safely in its owner's general cabin interior without becoming a stored item, production unit or invulnerable outdoor combat pet.

**Status:** Draft. Each delivery requires alignment before implementation.

## Deliveries

### Delivery 6.1: House cat — Draft

Register, protect and later release one previously tamed cat while preserving its global identity and normal companion behavior inside the cabin. Alignment must settle the complete behavior and acceptance contract before implementation planning begins.

## Consolidated specification

_Source: house-cat section of the former progression specification._

### House cats

A house cat is a companion resident of the general cabin interior, not a stable mount or livestock population. The base progression cabin supports one house cat without requiring a stable upgrade.

The owner must first tame a cat normally in the exterior world. While the cabin is deployed, the owner can bring that same cat through the entrance and deliberately assign the cabin as its home at the interior controller. Wild cats, another player's cats and cats already homed to another cabin are rejected. Registration preserves the cat's global entity UUID, owner, appearance, custom name, health and other safe vanilla state.

A homed cat behaves like a house cat rather than continuously following its owner:

- it remains in the general interior when the owner leaves through the exterior or hallway
- while not ordered to sit, it roams within the safe interior and favours beds, warm blocks, carpets, window perches and nearby owners
- the owner can still tell it to sit or stand using normal pet interaction
- it may sleep near its sleeping owner and produce vanilla cat gifts only from a real completed sleep event while the room is active
- it never generates gifts, breeding progress or other outputs through packed-time catch-up
- it cannot automatically cross a cabin exit, hallway door or functional-room boundary

The cabin controller keeps the cat away from the protected exit and void boundary. A homed cat is protected from damage and is returned to its home position if pathfinding or an owner-built hazard leaves it outside the safe interior. Because it cannot accompany players outside, this protection cannot be used to create an invulnerable combat pet. Other players cannot move or release it.

The owner may explicitly release the cat from its cabin home while the exterior is deployed. Release requires a safe exterior destination and removes the cabin protections; it never creates a second copy. Packing, hallway access and owner logout leave the cat safely at home.

At minimum, status must distinguish a wild cat, a cat owned by somebody else and a cat already homed to another cabin.
