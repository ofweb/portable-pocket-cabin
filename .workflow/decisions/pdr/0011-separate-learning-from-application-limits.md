# PDR-0011: Learn enchantments above application limits

Status: Accepted

## Context

Players can find enchantments above the enchanting room's application limit. Learning must keep the value of that discovery through room upgrades.

## Decision

The cabin can learn an item's full enchantment level before the room can apply it. It keeps the highest learned level in its known enchantment record. Learned enchantments stay local to each cabin. Connections do not transfer them.

An installed enchanting room applies up to level I. Each level upgrade raises the limit by one. Manual enchanting and automatic requisitions follow the room's application limit and each enchantment's maximum level.

## Rationale

The cabin keeps discoveries before the room is strong enough to use them. Room upgrades control when stronger enchanted items become available. Learning a higher level does not give an upgrade. Players must get enchantments for each cabin.

## Scope

This decision applies to learned levels and application limits. [B-0008](../../features/B-0008/brief.md) states learning, room upgrades, and manual use. [B-0028](../../features/B-0028/brief.md) applies the same limit to automatic requisitions. [Known enchantment](../../context.md#known-enchantment) gives the agreed term. This decision does not select the highest tier or costs.

The interface must show when a known level is above the application limit. Upgrades affect available actions without changing saved known enchantments.
