# An owner sets equipment rules for a cabin

Status: Draft
Feature ID: B-0029

## Goal

An owner sets equipment rules for cabin entry. The cabin moves available items and can request production to fill the missing quantity.

## Stories and acceptance

### S1: Create an equipment rule

Story: An owner sets an item rule in a group with a name and sees its effect before the next cabin entry.

Acceptance:

- Only the owner controls groups and rules. Each rule has a quantity range for a safe item template.
- A rule can select inventory slots, durability limits, enchantments, and a production setting.
- For the same template, active groups use the largest minimum quantity and largest maximum quantity from their rules.
- When the rule sets its minimum and maximum quantities to zero, the cabin moves all items with the same template to central storage. An item without a rule stays in place.
- The cabin keeps items without a rule in player inventory unless the owner selects automatic storage. The cabin tells the owner when automatic storage is active.
- Worn items, items with names, containers, and items with data that the rule does not list stay in place unless the rule lists each item.

### S2: Apply rules on entry

Story: An owner enters the cabin and sees each completed transfer or the reason a rule could not complete.

Acceptance:

- Each item rule moves its items as one action. A blocked rule does not move its items, but other rules can apply.
- The cabin withdraws available items from central storage before it starts production for the remaining quantity.
- If central storage has no space, items above the rule quantity stay in the owner inventory.
- Each item template has one active restock request after cabin entry. The request counts inventory, storage, and output in active jobs.
- The last entry report and restock state stay with the cabin through packing and restart.

### S3: Keep an item from cabin storage

Story: An owner keeps an item out of storage or reverses an available transfer from the last entry report.

Acceptance:

- An item exception stops automatic storage for that template until the owner places the item back in storage or removes the exception.
- The owner can move an item back to inventory only when the item is available and the owner has inventory space.
- If the cabin cannot move an item back, the cabin keeps the item in place and gives the reason.

## Scope

This feature includes owner equipment groups, entry transfers, restock requests, item exceptions, and entry reports.

## Non-goals

Residents and guests cannot use equipment groups. Each group has one restock hard reserve. Items without a rule keep the same state.

## Related records

- [Enchantment learning brief](../B-0008/brief.md).
- [Equipment requisitions brief](../B-0028/brief.md).
- [Central storage brief](../B-0004/brief.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Earlier milestone source](../../../docs/roadmap/08-enchanting-loadouts.md).

## Open questions and assumptions

- The rule for an item exception when the owner keeps an item out of storage is open.
- The item templates, slot selection rules, and restock output location are open questions.
