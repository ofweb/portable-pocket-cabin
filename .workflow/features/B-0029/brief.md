# An owner sets equipment rules for a cabin

Status: Draft
Feature ID: B-0029

## Goal

Loadouts move existing items between central storage and player inventory. They have their own book and upgrade path, independent of production rooms.

## Stories and acceptance

Entry triggers, owner-only access, and detailed transfer rules below remain tentative for later Shape.

### S1: Create an equipment rule

Story: An owner sets an item rule in a group with a name and sees its effect before the next cabin entry.

Acceptance:

- Only the owner controls groups and rules. Each rule has a quantity range for a safe item template.
- A loadout book reveals loadout upgrades through the shared book rules. It does not enable production.
- A rule can select inventory slots, durability limits, and enchantments.
- For the same template, active groups use the largest minimum quantity and largest maximum quantity from their rules.
- When the rule sets its minimum and maximum quantities to zero, the cabin moves all items with the same template to central storage. An item without a rule stays in place.
- The cabin keeps items without a rule in player inventory unless the owner selects automatic storage. The cabin tells the owner when automatic storage is active.
- Worn items, items with names, containers, and items with data that the rule does not list stay in place unless the rule lists each item.

### S2: Apply rules on entry

Story: An owner enters the cabin and sees each completed transfer or the reason a rule could not complete.

Acceptance:

- Each item rule moves its items as one action. A blocked rule does not move its items, but other rules can apply.
- The cabin withdraws available matching items from central storage. Missing items do not start production or enchanting jobs.
- If central storage has no space, items above the rule quantity stay in the owner inventory.
- The last entry report stays with the cabin through packing and restart.

### S3: Keep an item from cabin storage

Story: An owner keeps an item out of storage or reverses an available transfer from the last entry report.

Acceptance:

- An item exception stops automatic storage for that template until the owner places the item back in storage or removes the exception.
- The owner can move an item back to inventory only when the item is available and the owner has inventory space.
- If the cabin cannot move an item back, the cabin keeps the item in place and gives the reason.

## Scope

This feature includes loadout upgrades, owner equipment groups, entry transfers, item exceptions, and entry reports.

## Non-goals

Crafting, enchanting, equipment production, and automatic production requests are outside this feature. Resident access remains open. Items without a rule keep the same state.

## Related records

- [Loadout direction](../../direction.md#loadouts).
- [Central storage brief](../B-0004/brief.md).
- [Cabin books](../B-0038/brief.md).
- [Book installation rules](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).

## Open questions and assumptions

- The loadout upgrade effects, costs, access for residents, item templates, slot rules, and triggers are open.
- Earlier proposals protect equipped items and protected slots and keep automatic storage off by default. Shape must confirm these boundaries.
- Earlier proposals let the owner request an item once and create an exception after a reversed transfer. These actions need Shape review.
- Persistence of groups, owner settings, and item exceptions is open. Entry reports have persistence coverage.
- The storage controls for group selection, rule previews, and item exceptions are open.
