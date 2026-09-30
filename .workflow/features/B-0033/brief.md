# Cabin progression has survival limits

Status: Draft
Feature ID: B-0033

## Goal

Completed cabin capabilities have attainable costs and bounded capacities and production rates in survival play.

## Stories and acceptance

The stories below are proposals for Shape.

### S1: Progress without operator commands

Story: A player acquires a cabin and reaches completed capabilities through normal survival play.

Acceptance:

- Progression covers upgrade recipes, material quantities, World attunement weights, room dimensions, and tier limits.
- Room costs, growth, breeding, production, and catch-up have finite limits.
- Storage, mailbox, and local fixture capacities have finite limits.
- Book sources and rarity support survival discovery under B-0038.
- Jobs have recipe depth, operation, work, catch-up, reserve, and rate limits.
- Enchanting uses the current learning, material, application-limit, and automation-slot rules in its workflow briefs.
- Kiln, charcoal, coal, and optional production use their declared process profiles.
- Connections have cost limits, hallway dimensions, and membership limits.

### S2: Keep paid value after a balance change

Story: An existing world receives a supported balance change without losing purchased value or committed work.

Acceptance:

- Changes keep saved identities, paid receipts, committed jobs, and compatibility promises.
- A change that needs unsupported live migration fails with a reason before it invalidates saved state.
- Balance changes do not replace agreed feature behavior without review in Shape.

## Scope

This feature includes survival tuning of completed costs, capacities, rates, limits, loot, and profile values.

## Non-goals

New capabilities and silent changes to paid or committed state are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Cabin books](../B-0038/brief.md).
- [Enchantment library](../B-0008/brief.md).
- [Equipment requisitions](../B-0028/brief.md).
- [Optional production](../B-0027/brief.md).
- [Release verification](../B-0035/brief.md).

## Open questions and assumptions

- Exact values and the evidence necessary to judge attainable survival progression are open.
- Features must settle their behavior before balance work can tune it.
