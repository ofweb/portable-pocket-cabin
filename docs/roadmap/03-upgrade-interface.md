# Milestone 3: Upgrade interface

**Depends on:** Milestone 2's world-attuned expansion services.

**Outcome:** Cabin upgrades are discoverable, understandable and purchased through a deliberate protected interface rather than provisional chat messages and sneak-use gestures.

**Status:** Draft. Each delivery requires alignment before implementation.

## Deliveries

### Delivery 3.1: Cabin upgrade interface — Draft

Replace the provisional Milestone 2 controller interaction with one player-facing interface for inspecting and purchasing cabin upgrades. The interaction model, information hierarchy, confirmation behavior, permissions and failure presentation must be aligned before an implementation plan is written.

## Existing behavior to replace

Normal-use of the interior Lodestone currently sends a chat message containing the cabin's current and maximum size, attuned wood and next expansion requirements. Sneak-use immediately attempts to purchase that expansion from the owner's inventory.

This behavior remains available only until this milestone is accepted. The world-attunement, requirement-resolution, validation and expansion services remain authoritative; this milestone replaces their presentation and player interaction, not their persistence model.

## Deferred decisions

- whether the upgrade interface opens from the existing interior controller or a separate protected block
- whether it shows only the next available upgrade or the broader upgrade path
- how requirements, missing materials, locked prerequisites and validation failures are presented
- whether purchasing uses immediate confirmation, a review step or a separate commit action
- which roles may inspect upgrades and which role may purchase them
