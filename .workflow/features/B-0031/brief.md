# Owners request resources and offer surplus

Status: Draft
Feature ID: B-0031

## Goal

Connected cabins exchange owner-approved resources while each cabin keeps its own storage and resource ownership.

## Stories and acceptance

The stories below are proposals for Shape.

### S1: Publish a request or surplus rule

Story: An owner publishes an exact resource request or offers a limited surplus to connected cabins.

Acceptance:

- Only the owner controls sharing rules and requests.
- A rule can retain a fixed item quantity, retain inputs for owner restocks, or offer a specified amount for a request.
- The donor checks hard reserves, committed jobs, loadout commitments, owner rules, and destination permissions.
- Player withdrawals do not follow automatic sharing rules.
- No interface or production job sees a combined network inventory.

### S2: Complete an attributed transfer

Story: A donor approves eligible resources and the recipient receives them with information about its source.

Acceptance:

- Before items move, the donor commits items and the destination commits capacity.
- A transfer moves the same items and item data at both cabins or changes neither inventory.
- The recipient owns the items only after completion. Its jobs cannot use items in transit.
- Changed membership, permissions, owner rules, or capacity stop a transfer safely before completion.
- Full storage or mailbox capacity cannot delete or duplicate items.
- Recovery completes a transfer one time or releases both reservations without item loss.
- Each success records source cabin, destination cabin, rule, item, quantity, and request.

### S3: Inspect a request

Story: A player sees transfer status allowed by that player's destination role.

Acceptance:

- Owners see complete sharing, reserve, storage, mailbox, and transfer status for their cabin.
- Residents see actionable facility status without owner sharing or loadout settings.
- Guests see warnings without exact quantities, product lists, or private settings.
- Remote inspection gives no more information than local inspection permits.
- A failure identifies the cabin, request, or job and a blocking reason. Status does not merge every cabin's shortages.

## Scope

This feature includes requests, surplus rules, protected reserves, attributed transfers, recovery, and role-based status.

## Non-goals

Combined storage, remote inventory browsing, direct consumption from donor storage, and anonymous transfers are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [Hallways](../B-0009/brief.md).
- [Central storage](../B-0004/brief.md).
- [Crafting jobs and reserves](../B-0024/brief.md).
- [Owner loadouts](../B-0029/brief.md).
- [Mailbox automation](../B-0032/brief.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).

## Open questions and assumptions

- Request lifetime, donor selection, transfer rates, sharing limits, and controls are open.
- The earlier proposal to retain inputs for several complete loadout restocks needs alignment with the one-restock group reserve in B-0029.
- Trip exceptions do not add future sharing reserves in the earlier proposal. Shape must confirm this boundary.
