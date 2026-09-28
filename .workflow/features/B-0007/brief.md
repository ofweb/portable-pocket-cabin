# A cabin gets products and automation capabilities

Status: Draft
Feature ID: B-0007

## Goal

A cabin gets safe product templates from items in central storage. An owner can install automation books to give the cabin automation capabilities.

## Stories and acceptance

### S1: Get a product template

Story: An owner or resident puts an item with a safe profile into central storage. The cabin keeps a product template for that item.

Acceptance:

- The first stack of an item with a safe profile gives the cabin a product template.
- The cabin keeps the product template after storage has none of those items. Packing, restart, and redeployment do not remove the product template.
- A recipe or an item in a placed inventory does not give the cabin a product template. Seeing an item also does not give the cabin a product template.
- A product template keeps only the item identity and variant parts that the profile lists.
- The product template does not keep contents, names that players or mods add, lore, durability at the time of storage, or enchantments.
- The product template does not keep data in other item data unless the profile lists that data as safe.
- If the cabin cannot use an item variant, the item stays in storage. The cabin shows the reason.
- A product template cannot make an item or give the cabin a production capability.
- A product template stays with one cabin. Other cabins do not get that product template automatically.

### S2: Install an automation book

Story: A cabin owner finds an automation book and uses a cabin control to install its automation capability.

Acceptance:

- An automation book in storage does not install. Only the owner can use the control to install the automation capability.
- After installation is complete, the cabin gets one automation capability and removes one automation book.
- If the cabin cannot use the automation book or has its capability, installation stops and gives a reason. The book does not move.
- The cabin gets no new effect if it has that automation capability.
- Installed automation capabilities stay with the cabin through packing, restart, and redeployment.
- The owner can select each installed automation capability to operate or stop.
- A different automation book is necessary for each action: feed, collect, harvest, replant, fell trees, prepare meals, or brew.

## Feature-wide constraints and acceptance

- Automation capabilities and enchantments are different. Automation capabilities do not give enchantments. Enchantments do not install automation capabilities.
- The cabin gets a product template only from item parts that a profile lists as safe. An optional mod must have a profile for its item parts.
- Residents and guests cannot install automation capabilities or select when they operate.

## Scope

This feature includes product templates, automation books, installation, and an owner control for automation capabilities.

## Non-goals

- Jobs that make known items or do room actions.
- Product templates for item data that a profile does not list, or product templates for more than one cabin.
- Book locations, the number of books in loot, and the color and shape of the installation control.

## Related records

- [Product template](../../context.md#product-template).
- [Automation book](../../context.md#automation-book).
- [Automation capability](../../context.md#automation-capability).
- [Central storage brief](../B-0004/brief.md).
- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Crafting jobs brief](../B-0024/brief.md).
- [Production profiles](../B-0027/brief.md).

## Open questions and assumptions

- The item types and parts that each product profile accepts are open.
- The book list, sources for books, and installation control are open.
