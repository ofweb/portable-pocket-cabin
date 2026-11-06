# ADR-0012: Confirm storage transfers with station receipts

Status: Accepted

## Context

From B-0041 onward, cooking stations move items to and from central storage. A single transfer can change three stores that Minecraft saves at different times:

- central storage, in the cabin registry
- the station's block entity, in a pocket chunk
- the player's inventory, in player data

A crash between those saves must not lose or duplicate items.

Existing storage custody solves this for the crafting room, whose inputs live only in an open menu, by recording the player's inventory and escrowed items in a session. Cooking stations keep their contents in the world, so the session alone can't say whether a station was saved before or after a transfer.

## Decision

A storage-backed station transfer runs in four steps:

1. **Plan:** compute the whole transfer without changing anything.
2. **Receipt:** write a receipt into the registry, in the same update as the new storage contents, and flush it. The receipt holds the before and after contents of the station and the player's inventory, and the items withdrawn from and deposited into storage.
3. **Apply:** set the station and the player's inventory. The station's block entity records the receipt's operation id in a saved field.
4. **Confirm:** save the station's chunk and the player, then remove the receipt.

Recovery decides from the station alone. If the station carries the operation id, the transfer completes. If not, it is undone from the receipt. Items that no longer fit anywhere are delivered as held items, as storage recovery already does.

## Rationale

The registry write is the single commit point, and the block entity's saved operation id tells recovery which side of that point the chunk is on. Both outcomes reconstruct every store from the receipt, so no item can be in two places or none. The protocol reuses existing pieces: registry flushing, `CabinPlayerSave.confirm`, join-time recovery, and held-item delivery.

## Scope

This applies to kitchen fills, fuel filling, and output overflow in [B-0041](../../features/B-0041/design.md), and to cooking automation in B-0025, which moves items between storage and stations without a player. Transfers that don't touch storage keep B-0045's single-store behaviour.
