# PDR-0011: Separate learning from application limits

Status: Accepted

## Context

An owner can improve the enchanting room over time. A player may find an item with an enchantment level above the room's current limit.

## Decision

The cabin can learn the item's full enchantment level before the room can apply it. The highest learned level stays in the cabin's known enchantment record. Learned enchantments stay local to each cabin. Connections do not transfer them. An installed enchanting room applies up to level I. Each level upgrade raises that limit by one. Manual enchanting and automated requisitions follow the room limit and each enchantment's own maximum.

## Rationale

An early discovery keeps its value when the room is not yet strong enough to use it. Room upgrades still control when the cabin can produce stronger enchanted items. Learning a higher level does not grant an upgrade. Players must get enchantments for each cabin.

## Scope

This decision governs learned levels and the enchanting room's application limit. [B-0008](../../features/B-0008/brief.md) defines learning, room upgrades, and manual use. [B-0028](../../features/B-0028/brief.md) applies the same limit to automated requisitions. [Known enchantment](../../context.md#known-enchantment) defines the shared term. This decision does not set the highest tier or costs.

## Consequences

The interface must show when a known level exceeds the room's current limit. An upgrade changes available actions without changing saved knowledge.
