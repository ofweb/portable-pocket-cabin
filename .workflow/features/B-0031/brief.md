# Owners request resources and offer surplus

Status: Draft
Feature ID: B-0031

## Goal

Connected cabins transfer resources after owner confirmation. Each cabin keeps its storage and resource ownership.

## Stories and acceptance

Shape must confirm these stories.

### S1: Create a request or surplus rule

Story: An owner creates a resource request for specified items or offers surplus up to a quantity limit to connected cabins.

Acceptance:

- Only the owner controls surplus rules and requests.
- A rule can keep a specified item quantity, keep inputs for owner restocks, or offer a specified quantity for a request.
- The source cabin checks hard reserves, committed jobs, committed loadout resources, owner rules, and permissions at the target cabin.
- When players withdraw items, automatic surplus rules do not apply.
- No interface or production job sees one inventory for the complete network.

### S2: Complete a transfer with source information

Story: The source cabin confirms resources that follow its rules. The target cabin receives them with source information.

Acceptance:

- Before items move, the source cabin commits items and the target cabin commits capacity.
- A transfer removes items from the source cabin and puts the same items and item data into the target cabin.
- If the transfer cannot complete, no items move. The target cabin gets ownership only when the transfer completes. Its jobs cannot use items before that time.
- Changed membership, permissions, owner rules, or capacity stop a transfer safely before it completes.
- Full storage or mailbox capacity cannot delete items or transfer the same items more than one time.
- Recovery completes a transfer one time or releases the source and target reservations. Recovery keeps all items.
- Each completed transfer records the source cabin, target cabin, rule, item, quantity, and request.

### S3: Inspect a request

Story: A player sees transfer status that follows their role at the target cabin.

Acceptance:

- Owners see complete surplus rule, hard reserve, storage, mailbox, and transfer status for their cabin.
- Residents see status for systems they can use, without owner surplus or loadout settings.
- Guests see failure information without item quantities, product lists, or owner settings.
- Inspection through the network gives no more information than local inspection for the same player.
- A failure identifies the cabin, request, or job and a blocking reason. Status shows requests for each cabin independently.

## Scope

This feature includes requests, surplus rules, hard reserves, transfer source information, recovery, and status that follows household roles.

## Non-goals

A cabin cannot inspect the inventory of a different cabin or use items in that inventory for jobs. Transfers always include source information.

## Related records

- [Direction](../../direction.md).
- [Hallways](../B-0009/brief.md).
- [Central storage](../B-0004/brief.md).
- [Crafting jobs and hard reserves](../B-0024/brief.md).
- [Owner loadouts](../B-0029/brief.md).
- [Mailbox automation](../B-0032/brief.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).

## Open questions and assumptions

- The time to keep a request, source cabin selection, transfer rates, surplus limits, and controls are open.
- The plan to keep inputs for more than one complete loadout restock is open. Shape must compare it with the group hard reserve for one restock in B-0029.
- The plan does not include item exceptions in inputs for the next loadout restocks. Shape must confirm this constraint.
