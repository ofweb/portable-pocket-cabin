# PDR-0011: Learn enchantments above application limits

Status: Accepted

## Context

Players can find enchantments above the enchanting room's application limit. Learning must keep the value of that discovery through room upgrades.

## Decision

The cabin can learn an item's full enchantment level before the room can apply it. It keeps the highest learned level in its known enchantment record. Learned enchantments stay local to each cabin. Connections do not transfer them.

An installed enchanting room is tier I. Room tiers I through IV apply known enchantments up to the matching level. Tier V is the final room tier and removes the room's application limit. It can apply any learned level up to each enchantment's normal maximum. It does not grant unlearned levels or apply levels above that maximum. Manual enchanting and automatic requisitions follow these same limits.

## Rationale

The cabin keeps discoveries before the room is strong enough to use them. Room upgrades control when stronger enchanted items become available. Learning a higher level does not give an upgrade. Players must get enchantments for each cabin.

The final tier completes room progression without requiring more room upgrades for supported enchantments whose normal maximum exceeds level V.

## Scope

This decision applies to learned levels, the five room tiers, and application limits. [B-0008](../../features/B-0008/brief.md) states learning, room upgrades, and manual use. [B-0028](../../features/B-0028/brief.md) applies the same limits to automatic requisitions. [Known enchantment](../../context.md#known-enchantment) gives the agreed term. This decision does not select upgrade costs or optional enchantment support.

The interface must show when a known level is above the application limit. Upgrades affect available actions without changing saved known enchantments.
