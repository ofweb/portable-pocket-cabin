# Cabin upgrades and production have limits

Status: Draft
Feature ID: B-0033

## Goal

Players can purchase completed cabin capabilities in Survival mode. Capacities and production rates have limits.

## Stories and acceptance

Shape must confirm these stories.

### S1: Progress without operator commands

Story: A player gets a cabin and completed capabilities through Survival mode.

Acceptance:

- Checks include upgrade recipes, material quantities, World attunement selection weights, room dimensions, and tier limits.
- Room costs, growth, breeding, production, and catch-up have limits.
- Storage, mailbox, and local fixture capacities have limits.
- Players can find cabin books in Survival mode through the sources and rarity rules in B-0038.
- Jobs have limits for recipe depth, operations, work, catch-up, hard reserves, and rates.
- Enchanting follows the learning, material, application limit, and automation slot rules in its workflow briefs.
- Kiln, charcoal, coal, and optional production use their process profiles.
- Connections have cost limits, hallway dimensions, and membership limits.

### S2: Keep purchased value after a balance change

Story: A world receives a balance change that follows its migration policy. Purchased value and committed work stay as before.

Acceptance:

- Changes keep saved identities, Upgrade refund receipt data, committed jobs, and documented compatibility.
- If a change during operation does not follow the migration policy, the action stops with a reason. Saved state stays as before.
- Shape must confirm each change to agreed feature behavior before that change applies.

## Scope

This feature includes changes to completed costs, capacities, rates, limits, loot, and profile values for Survival mode.

## Non-goals

New capabilities and changes to purchased or committed state without player information are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Cabin books](../B-0038/brief.md).
- [Known enchantments](../B-0008/brief.md).
- [Equipment requisitions](../B-0028/brief.md).
- [Optional production](../B-0027/brief.md).
- [Version checks](../B-0035/brief.md).

## Open questions and assumptions

- Values and the results necessary to confirm that players can purchase the upgrades in Survival mode are open.
- Shape must confirm feature behavior before balance changes apply.
