# A cabin sends mail automatically

Status: Draft
Feature ID: B-0032

## Goal

An owner can select mailbox delivery to operate for requests and surplus that follow the cabin rules, up to a delivery limit.

## Stories and acceptance

Shape must confirm these stories.

### S1: Select a delivery rule

Story: An owner selects mailbox automation to operate and sets an item, quantity, target cabin, and delivery limit.

Acceptance:

- Only the owner selects the capability to operate and controls its rules.
- Each rule has an item, quantity, target cabin, and limit.
- A rule gives no general access to storage in other cabins.
- Automatic delivery follows resource request and surplus rules in B-0031.

### S2: Complete or stop a delivery safely

Story: A delivery completes when the source cabin has items that follow its rules. The target cabin must have permission and capacity.

Acceptance:

- Missing capability, permission, surplus, hard reserves, storage capacity, or mailbox capacity stops delivery and gives a reason.
- A full target inventory rejects the transfer. Item ownership stays with the source cabin, and all items stay in place.
- A transfer keeps item data and completes one time after interruption or restart.
- Mailbox contents and actions to withdraw items follow the owner-only rules in B-0003.
- Status follows the role limits for inspection in B-0031.

## Scope

This feature includes owner-controlled mailbox automation for requests and surplus.

## Non-goals

General access to storage in other cabins and automatic movement from mail into central storage are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Receiving mailbox](../B-0003/brief.md).
- [Resource requests and surplus](../B-0031/brief.md).
- [Product capabilities](../B-0007/brief.md).

## Open questions and assumptions

- Capability installation, upgrade costs, rule limits, delivery times, and target cabin controls are open.
- Which requests use mailbox capacity or central storage capacity is open.
