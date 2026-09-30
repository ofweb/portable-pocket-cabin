# An owner requests enchanted equipment

Status: Draft
Feature ID: B-0028

## Goal

An owner can request known equipment with enchantments selected for automation. The cabin shows the complete production plan before it commits resources.

## Stories and acceptance

### S1: Select enchantments for automation

Story: The cabin installs the enchanting automation book. The owner purchases slot upgrades and selects known enchantments for automation.

Acceptance:

- The enchanting automation book reveals the first slot upgrade in B-0038. The book gives no slot.
- The owner purchases the first slot upgrade through the enchanting room upgrades.
- Only the owner can install slot upgrades or select automation slots. A selection does not use up known enchantments.
- Each next slot upgrade adds slots. The cabin can keep known enchantments without a slot limit.
- Automation slot upgrades add slots. Room upgrades add to the application limit and do not add slots.
- A job cannot continue unless all enchantments for that job are in automation slots.
- Owners and residents can apply known enchantments without automation slots.

### S2: Plan a requisition

Story: An owner selects an equipment item, quantity, and enchantments. The cabin shows the resources and work time for the selected requisition.

Acceptance:

- Only the owner can save and commit a requisition. The item must be a known safe variant.
- The owner selects known enchantments from automation slots. The owner selects levels that the cabin knows.
- A selected level cannot be higher than the known level, the room application limit, or the highest level for that enchantment.
- Before it commits resources, the cabin checks that each enchantment can go on the item and that the enchantments can go together. The cabin also checks for all capabilities.
- The plan shows selected recipes, the quantity of each material, hard reserve values, output capacity, and work time.
- If a check blocks the request, the cabin gives the reason and commits no resources.

### S3: Complete a requisition

Story: A requisition gives the equipment when the job is complete. The cabin also gives the reason for each pause.

Acceptance:

- The requisition follows the same reservation, work, output, and recovery rules as bounded local production jobs.
- The job uses lapis and amethyst for each enchantment it applies. The job has a work time.
- If the owner removes an enchantment that a job uses from an automation slot, the job pauses. The job keeps committed resources and completed work.
- The job continues when its enchantments and other conditions are in place again.
- The request stays through packing and restart. The cabin creates output one time only.

## Feature-wide constraints and acceptance

- The cabin uses amethyst in place of player experience. Player experience is not a job input and does not go down.
- The cabin applies a curse only when the owner selects it.
- Residents can contribute learned enchantments through B-0008. Residents cannot save requisitions.

## Scope

This feature includes automation slot selection, owner equipment requests, and a local production plan for the owner.

## Non-goals

General equipment stock targets, resident requisitions, and production in a different cabin are not part of this feature.

## Related records

- [Enchantment learning brief](../B-0008/brief.md).
- [Known enchantment](../../context.md#known-enchantment).
- [Automation book](../../context.md#automation-book).
- [Cabin books brief](../B-0038/brief.md).
- [Book and upgrade decision](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
- [Crafting jobs brief](../B-0024/brief.md).
- [B-0007](../B-0007/brief.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [PDR-0010](../../decisions/pdr/0010-manual-enchanting-uses-all-known-enchantments.md).
- [PDR-0011](../../decisions/pdr/0011-separate-learning-from-application-limits.md).

## Open questions and assumptions

- The equipment variants the cabin can make, material costs, work rates, and output location are open.
- The number of automation slots and the upgrade steps that add them are open.
- The rule for manual material quantities also applies to automation costs. The effect of item rarity and equipment tier is open.
- The cost and sequence for more than one selected enchantment are open. An item with more enchantments has a higher cost for each added enchantment.
- The result of an enchantment profile change during a committed job is open.
