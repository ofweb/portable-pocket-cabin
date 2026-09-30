# A cabin delivers mail automatically

Status: Draft
Feature ID: B-0032

## Goal

An owner can enable limited mailbox delivery for declared requests and eligible surplus.

## Stories and acceptance

The stories below are proposals for Shape.

### S1: Enable a delivery rule

Story: An owner enables mailbox automation and sets an item, quantity, destination, and delivery limit.

Acceptance:

- Only the owner enables the capability and controls its rules.
- Each rule specifies its item, quantity, destination, and limit.
- A rule grants no general access to remote storage.
- Automatic delivery follows resource request and surplus rules in B-0031.

### S2: Deliver or stop safely

Story: A delivery completes when the donor has eligible items and the destination has permission and capacity.

Acceptance:

- Missing capability, permission, surplus, reserves, storage capacity, or mailbox capacity stops delivery and gives a reason.
- A full destination rejects the transfer without item loss or partial ownership.
- A transfer keeps item data and completes one time after interruption or restart.
- Mailbox contents and collection follow B-0003's owner-only rules.
- Status follows B-0031's local and remote role limits.

## Scope

This feature includes owner-controlled mailbox automation for requests and surplus.

## Non-goals

General remote storage access and automatic movement from mail into central storage are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Receiving mailbox](../B-0003/brief.md).
- [Resource requests and surplus](../B-0031/brief.md).
- [Product capabilities](../B-0007/brief.md).

## Open questions and assumptions

- Capability discovery, upgrade costs, rule limits, delivery timing, and destination controls are open.
- Which requests use mailbox capacity or central storage capacity is open.
