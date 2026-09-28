# An owner requests enchanted equipment

Status: Draft
Feature ID: B-0028

## Goal

An owner can request known equipment with enchantments selected for automation. The cabin shows the complete production plan before it commits resources.

## Stories and acceptance

### S1: Select enchantments for automation

Story: After the cabin installs the enchanting automation book, the owner buys slot upgrades and selects known enchantments for automation.

Acceptance:

- The enchanting automation book reveals the first slot upgrade under B-0038. The book alone grants no slot.
- The owner buys the first slot upgrade through the enchanting room upgrades.
- Only the owner can install slot upgrades or change the slot selection. A selection does not consume known enchantments.
- Later automation upgrades add slots. The cabin can keep known enchantments without a slot limit.
- Automation slot upgrades do not raise the room's enchantment level. Level upgrades do not add automation slots.
- A job cannot advance without each required enchantment in an automation slot.
- Manual enchanting can use known enchantments outside the automation slots.

### S2: Review a requisition

Story: An owner selects an equipment item, quantity, and enchantments, then reviews the required resources and work.

Acceptance:

- Only the owner can save and start a requisition. The item must be a known safe variant.
- The owner selects only known enchantments and levels that occupy automation slots.
- A selected level cannot exceed the known level, the enchanting room's current application limit, or the enchantment's own maximum.
- The cabin checks applicability, incompatible combinations, and required capabilities before commitment.
- The plan shows selected recipes, material quantities, hard reserves, output capacity, and work time.
- A failed check identifies the blocking condition and commits no resources.

### S3: Complete a requisition

Story: An owner starts a valid requisition and later receives the completed equipment or a clear pause reason.

Acceptance:

- The requisition follows the same reservation, work, output, and recovery rules as bounded local production jobs.
- Automated enchanting consumes lapis and amethyst for each applied enchantment and advances through timed work.
- If the owner removes a required enchantment from an automation slot, the job pauses and keeps committed resources and completed work.
- The job resumes only when the required enchantments and other conditions are valid again.
- Packing and restart do not remove the request or create the output twice.

## Feature-wide constraints and acceptance

- The cabin uses amethyst and timed work in place of player experience. It has no player experience-level requirement or deduction.
- A curse applies only when the owner explicitly selects it.
- Residents may contribute learned enchantments through the library. They cannot save or start requisitions.

## Scope

This feature includes automation slot selection, owner requests for equipment, and a visible local production plan.

## Non-goals

General equipment stock targets, resident requisitions, and production in a different cabin are outside this feature.

## Related records

- [Enchantment library brief](../B-0008/brief.md).
- [Known enchantment](../../context.md#known-enchantment).
- [Automation book](../../context.md#automation-book).
- [Cabin books brief](../B-0038/brief.md).
- [Book and upgrade decision](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
- [Crafting jobs brief](../B-0024/brief.md).
- [Product knowledge brief](../B-0007/brief.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Manual and automated enchanting decision](../../decisions/pdr/0010-manual-enchanting-uses-all-known-enchantments.md).
- [Learned levels and room limits decision](../../decisions/pdr/0011-separate-learning-from-application-limits.md).
- [Earlier milestone source](../../../docs/roadmap/08-enchanting-loadouts.md).

## Open questions and assumptions

- The supported equipment variants, exact material costs, work rates, and output destination need agreement.
- The number of automation slots and the upgrade steps that add them remain open.
- The proposed manual reagent formula also affects automated costs. The role of rarity and equipment tier remains open.
- The cost and order for several selected enchantments remain open. A per-application cost changes with each enchantment already on the item.
- The result of a change to an enchantment profile during a committed job remains open.
