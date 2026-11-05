# PDR-0015: The cabin provides its own cooking system

Status: Accepted

## Context

The cabin planned to use Farmer's Delight Refabricated as an optional source of crops, meals, and cooking profiles. Its cooking is a set of single-step recipes. The cabin's goals for cooking go further: players learn real techniques, see how much work ordinary food takes, and automate familiar processes through the kitchen and central storage.

## Decision

Portable Pocket Cabin provides its own cooking and food production system. The [cooking concept](../../cooking/concept.md) describes it. It replaces Farmer's Delight as the intended food content. Farmer's Delight is no longer an intended source of upgrade materials, cooking profiles, or compatibility checks.

The greenhouse keeps its shipped Farmer's Delight growth profiles until cabin cooking supplies its own crops.

## Rationale

The cabin needs much deeper food chains than Farmer's Delight offers: real recipes, many intermediate items, heat set by fuel, butchery, fermentation, drying, and earned automation. Those depend on cabin systems: the kitchen room, central storage, product templates, and production jobs. An optional mod's recipes cannot supply that depth or teach the techniques behind it.

## Scope

This decision selects the cooking direction and retires Farmer's Delight as a planned integration. It does not select recipes, equipment, benefits, costs, or the order of delivery. Food changes apply throughout the world, not only in the cabin. The concept's remaining open questions stay open.

## Related records

- [Direction](../../direction.md#cooking-and-food-production).
- [Cooking concept](../../cooking/concept.md).
- [Production follows player progress](0013-gate-production-through-player-progress.md).
- [Manual kitchen](../../features/B-0041/brief.md).
- [Cooking and brewing jobs](../../features/B-0025/brief.md).
