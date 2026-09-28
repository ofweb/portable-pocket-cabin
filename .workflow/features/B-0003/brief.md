# Receiving mailbox

Status: Draft
Feature ID: B-0003

## Goal

Let any player deliver items to a cabin without granting access to its private storage or revealing earlier deliveries.

## Stories and acceptance

### S1: Send a delivery

Story: As a sender, I can place items into a cabin mailbox without entering the cabin.

Acceptance:

- Each cabin starts with one receiving mailbox. Its manual interface is available on a deployed exterior.
- Any player can submit a delivery, regardless of household role or cabin entry access.
- A sender cannot see existing contents or withdraw a submitted item.
- A full mailbox rejects the whole attempted delivery. The sender keeps all rejected items.

### S2: Collect a delivery

Story: As an owner, I can inspect and collect the deliveries sent to my cabin.

Acceptance:

- Only the cabin owner can inspect or collect mailbox contents.
- Mailbox contents stay separate from central storage and automation until the owner accepts or collects them.
- A permission change or stale interaction cannot complete an unauthorized transfer.
- Each accepted transfer moves its full agreed amount once or leaves both inventories unchanged.

### S3: Retain deliveries during travel

Story: As an owner, I can move or recover the cabin without losing mailbox contents.

Acceptance:

- Exact mailbox stacks survive restart, packing, redeployment, and exterior loss.
- Packing removes the exterior interface, but it does not remove the mailbox or its contents.
- An unconnected packed cabin has no manual mailbox interface until redeployment.

## Feature-wide constraints and acceptance

- The mailbox has a fixed bound. A rejected delivery cannot delete or displace items.
- An open interface checks cabin identity, current role, and capacity before a transfer.
- Guest status shows no exact mailbox contents or private owner settings.

## Scope

This feature provides one public receiving point and owner collection for each cabin.

## Non-goals

- Automated fulfilment, surplus delivery, and transfers between cabins.
- A connected hallway interface or a manual interface for an unconnected packed cabin.
- Transfer of mailbox contents into central storage without an owner action.

## Related records

- [Household role decision](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [Household role brief](../B-0002/brief.md).

## Open questions and assumptions

- Draft assumption: The mailbox exists for every cabin, with no purchase step.
- Open: Set the initial capacity and the exact delivery and collection controls.
- Open: Define how an owner accepts mailbox contents after central storage exists.
