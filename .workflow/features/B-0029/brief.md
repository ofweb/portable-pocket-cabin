# A cabin maintains owner loadouts

Status: Draft
Feature ID: B-0029

## Goal

An owner sets equipment rules for cabin entry. The cabin moves available items and can request production for a remaining deficit.

## Stories and acceptance

### S1: Set a loadout rule

Story: An owner defines an item rule in a named group and sees its effect before the next cabin entry.

Acceptance:

- Only the owner can change groups and rules. Each rule has a safe item template, a minimum, and a maximum quantity.
- A rule can select preferred inventory slots, durability conditions, enchantments, and whether production is permitted.
- Active groups combine rules for the same template by their highest minimum and highest maximum.
- A zero minimum and zero maximum deposits every matching item. An item without a rule stays unchanged.
- Deposit of unlisted items is off by default and warns the owner while active.
- Equipped, protected, named, container, and unsupported custom-data items stay unchanged unless an exact rule includes them.

### S2: Reconcile on entry

Story: An owner enters the cabin and sees each completed transfer or the reason a rule could not complete.

Acceptance:

- Each item rule transfers as one action. A blocked rule changes no matching items, but other rules can complete.
- The cabin withdraws available central-storage items before it starts permitted production for a deficit.
- Excess items stay with the owner if central storage has no space.
- Repeat entry updates one outstanding restock request per item template. It counts inventory, storage, and committed output.
- The latest entry summary and pending restock state survive packing and restart.

### S3: Take an item temporarily

Story: An owner takes an item for one trip or reverses an available transfer from the latest entry summary.

Acceptance:

- A temporary exception prevents automatic deposit for that template until a later return or explicit removal.
- Take back succeeds only when the deposited item remains available and inventory can accept it.
- A failed Take back action moves no item and reports the reason.

## Scope

This feature includes owner loadout groups, entry transfers, restock requests, temporary exceptions, and entry status.

## Non-goals

Resident loadouts, guest loadouts, shared restock reserves, and automatic changes to unlisted items are outside this feature.

## Related records

- [Enchantment library brief](../B-0008/brief.md).
- [Equipment requisitions brief](../B-0028/brief.md).
- [Central storage brief](../B-0004/brief.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Earlier milestone source](../../../docs/roadmap/08-enchanting-loadouts.md).

## Open questions and assumptions

- The rule for a temporary exception after a departure without a later return needs agreement.
- Template matching, preferred slot conflicts, and the destination for completed restock output remain open.
