# The cabin mailbox receives items

Status: Draft
Feature ID: B-0003

## Goal

Let players send items to a cabin without access to its storage or earlier mail.

## Stories and acceptance

### S1: Send a delivery

Story: A player uses a deployed cabin mailbox to send items without entering the cabin.

Acceptance:

- The mailbox appears once both central storage and connected-lounge access are installed. Either prerequisite alone provides no mailbox. Mailbox appearance belongs to this feature, not storage installation.
- Players can send deliveries through the deployed exterior and the cabin's mailbox access point in the shared lounge.
- Owners, residents, and guests can send mail.
- The delivery interface does not show earlier mail or let players withdraw items.
- If the mailbox cannot hold the selected items, the mailbox rejects the transfer. The player keeps all selected items.

### S2: Collect a delivery

Story: A cabin owner checks the mailbox and collects its items.

Acceptance:

- Only the cabin owner can see or collect mailbox contents.
- The owner can access the mailbox from the central storage interface and the cabin's mailbox access point in the shared lounge.
- Mail stays in the mailbox until the owner collects it. Mail does not enter central storage automatically.
- A transfer moves the selected number of items one time. A rejected transfer moves no items.

### S3: Keep deliveries during travel

Story: A cabin owner moves the cabin or finds its exterior missing and can collect the same mail.

Acceptance:

- Mailbox items and their data stay in the cabin through restart, packing, and redeployment.
- A missing exterior does not remove mailbox items or their data.
- Packing removes the exterior access point. The cabin keeps its mailbox and mail.
- Players cannot use a packed cabin mailbox through its exterior until redeployment.
- Shared-lounge access follows the connected-cabin access rules, including when the cabin is packed.

## Feature-wide constraints and acceptance

- The mailbox has a capacity limit. A rejected transfer does not move mailbox items or player items.
- Each transfer checks cabin identity, role, and capacity before items move.
- Guest status does not show mailbox contents or owner settings.

## Scope

The feature includes mailbox appearance after both prerequisites are installed, manual delivery, and owner access.

## Non-goals

- Automatic delivery, surplus delivery, and transfers between cabins.
- Automatic transfer of mail into central storage.

## Related records

- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Household role brief](../B-0002/brief.md).
- [Central storage brief](../B-0004/brief.md).
- [Shared-lounge connection](../B-0009/brief.md).
- [Mailbox automation](../../backlog.md#b-0032-mailbox-automation).

## Open questions and assumptions

- The mailbox capacity and the number of items in one transfer are open.
- The method to collect items when the owner's inventory is full is open.
- The scope of manual transfer into central storage is open.
- Whether leaving a hallway network changes mailbox availability remains open. Installed connection upgrades persist under B-0009.
