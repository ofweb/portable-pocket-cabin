# Receiving mailbox

Status: Draft
Feature ID: B-0003

## Goal

Let players send items to a cabin without access to its private storage or earlier mail.

## Stories and acceptance

### S1: Send a delivery

Story: A player uses a deployed cabin mailbox to send items without entering the cabin.

Acceptance:

- Each cabin has one mailbox. Players can use the mailbox through the deployed exterior.
- All players can send mail, including guests.
- A sender cannot see earlier mail or take back items after a successful transfer.
- If the mailbox cannot hold the offered items, the mailbox rejects the transfer. The player keeps all offered items.

### S2: Collect a delivery

Story: A cabin owner checks the mailbox and collects its items.

Acceptance:

- Only the cabin owner can see or collect mailbox contents.
- Mail stays in the mailbox until the owner collects it. Mail does not enter central storage automatically.
- A change in permission stops an open interface from authorizing a later transfer.
- A successful transfer moves the full selected amount once. A failed transfer leaves both inventories unchanged.

### S3: Retain deliveries during travel

Story: A cabin owner moves or recovers the cabin and finds the same mail afterward.

Acceptance:

- Mailbox items and their data survive restart, packing, redeployment, and exterior loss.
- Packing removes the exterior access point. The cabin keeps its mailbox and mail.
- Players cannot use a packed cabin mailbox through its exterior until redeployment.

## Feature-wide constraints and acceptance

- The mailbox has a fixed capacity. A rejected transfer does not delete or displace items.
- Each transfer checks cabin identity, current role, and capacity when the player confirms it.
- Guest status does not show mailbox contents or private owner settings.

## Scope

This feature covers manual mail delivery and owner collection for one cabin mailbox.

## Non-goals

- Automatic delivery, surplus delivery, and transfers between cabins.
- Mailbox access through a connected hallway.
- Automatic transfer of mail into central storage.

## Related records

- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Household role brief](../B-0002/brief.md).
- [Central storage brief](../B-0004/brief.md).
- [Mailbox automation](../../backlog.md#b-0032-mailbox-automation).

## Open questions and assumptions

- Assumption: Each cabin gets its mailbox without a purchase.
- Open: What is the mailbox capacity, and how does the interface select an amount?
- Open: Where do collected items go if the owner's inventory has insufficient space?
- Open: Does the owner need a separate action to move mail into central storage?
