# Connected cabins share discoveries

Status: Draft
Feature ID: B-0010

## Goal

Connected cabins share learned enchantments and automation capabilities without sharing items or private settings.

## Stories and acceptance

The stories below are proposals for Shape.

### S1: Share a discovery

Story: A cabin joins a hallway network and receives discoveries from its member cabins.

Acceptance:

- Active membership shares installed automation capabilities and the highest learned level of each enchantment.
- A received capability stays off until the receiving owner enables it.
- A received enchantment becomes known. It does not select an automation slot.
- The transfer includes no book, item, material, slot selection, loadout rule, or private setting.
- Product templates, meal variants, and potion variants stay local.
- Repeated sharing cannot reduce the known enchantment level or create a second capability.

### S2: Keep discoveries after departure

Story: A cabin leaves the network and keeps discoveries it already received.

Acceptance:

- Departure keeps received capabilities and enchantments but stops future sharing.
- A temporary connection can spread discoveries to other members. Departure does not remove those discoveries.
- Received discoveries stay through packing and restart.

## Scope

This feature includes permanent discovery sharing during active hallway membership.

## Non-goals

Shared inventories, product templates, automatic activation, and copied private settings are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Hallways](../B-0009/brief.md).
- [Product knowledge and capabilities](../B-0007/brief.md).
- [Enchantment library](../B-0008/brief.md).
- [Automation slots](../B-0028/brief.md).
- [Books reveal upgrades](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).

## Open questions and assumptions

- The effect of a shared capability on book discovery and local upgrade purchase is open. Sharing must not silently replace the book decision.
- Timing, player messages, and behavior when an optional profile is missing are open.
