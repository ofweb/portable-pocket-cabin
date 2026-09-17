# Milestone 10: Cooperative knowledge, mail and resource logistics

**Depends on:** [Milestone 7](07-production-automation.md) jobs and reserves, [Milestone 8](08-enchanting-loadouts.md) knowledge and loadouts, and [Milestone 9](09-connected-cabins.md) hallway networks.

**Outcome:** Connected cabins cooperate without becoming one inventory or exposing private household configuration.

**Status:** Draft. Each delivery requires alignment before implementation.

## Shared discoveries

Connection and active hallway membership propagate two kinds of permanent knowledge:

- installed automation capabilities become known to every member cabin but remain disabled until that cabin's owner enables them
- learned enchantments enter each member's known library at the highest shared level but never occupy an active slot automatically

Ordinary products, prepared meals and potion variants never propagate. Connecting temporarily may spread shared discoveries throughout a group; this is intended progression. Leaving retains every copied capability and enchantment.

Sharing knowledge transfers no book, item, material, active-slot selection, automation setting or private configuration.

## Requests and surplus

Every cabin retains its own storage and resource ownership. No interface or job sees a combined network inventory.

An owner may publish an exact request or expose surplus through rules such as:

- share an item above a fixed quantity
- retain enough inputs for a configured number of complete owner-loadout restocks
- transfer a configured amount after an explicit request

Donor evaluation includes [Milestone 7](07-production-automation.md) hard reserves, committed jobs and [Milestone 8](08-enchanting-loadouts.md) loadout commitments. Manual player withdrawals remain outside automatic sharing rules.

Remote automation never reads or consumes donor storage directly. One transfer performs:

1. Persist a recipient request with its destination and attribution.
2. Let each donor independently evaluate current sharing, reserve and permission rules.
3. Reserve one eligible amount at the donor and capacity at the recipient storage or mailbox.
4. Commit removal and deposit atomically.
5. Roll back both sides on failure or interruption.

The recipient owns the items only after commit. Its jobs cannot consume in-transit resources. Every success records source cabin, destination cabin, rule, item, amount and initiating request.

## Mail automation

An owner may enable a narrow mailbox capability to fulfil declared incoming requests or deliver eligible surplus. Each rule names its item, amount, destination and limit; no rule grants general remote storage access.

Delivery stops at missing capability, permission, donor surplus, reserve, recipient capacity or mailbox capacity. A full destination rejects the transfer without materializing partial ownership or deleting items.

## Status and privacy

Status remains grouped by cabin, request and job:

- owners see their complete storage, reserve, sharing, mailbox and transfer diagnostics
- residents see actionable status for facilities they may use, without owner sharing or loadout configuration
- guests see plain-language warnings without exact quantities, catalogues or private configuration
- remote inspection reveals no more than the same player could see at the destination cabin locally

The network never merges every cabin's shortages into a mandatory task list. Failures identify the cabin, request or job and one concrete blocking reason.

## Technical approach

A network-logistics service consumes [Milestone 9's](09-connected-cabins.md) stable membership generation but owns no hallway lifecycle. Discovery propagation is idempotent and monotonic. Transfer journals reference immutable request, donor, recipient and payload identities so restart recovery commits once or rolls back both reservations.

All donor policy and destination permission checks run server-side against current state immediately before commit. Stale membership, changed rules or lost capacity abort safely.

## Evergreen acceptance contract

Milestone 10 remains accepted only while automated tests and targeted manual checks establish that:

1. Automation capabilities and enchantments propagate permanently without enabling capabilities or active slots.
2. Product, meal and potion templates remain cabin-local.
3. Disconnecting retains copied knowledge but stops future propagation and transfers.
4. Donors expose only owner-approved surplus after reserves, jobs and loadout commitments.
5. Transfers commit equal item and component data at both cabins or change neither side.
6. Full storage, full mailbox, permission loss and stale membership cannot lose or duplicate items.
7. Recipient jobs cannot consume resources before transfer commit.
8. Every transfer remains attributable and visible under local role-based privacy rules.
9. Remote inspection never exposes exact data hidden from that player locally.

Discovery, policy, reservation, transfer-journal, permission, concurrency and dedicated-server restart tests are the automated gates. Manual acceptance covers publishing, attribution, status grouping and multiplayer visibility.

## Out of scope

- combined or remotely browsable storage
- propagation of ordinary product, meal or potion knowledge
- automatic activation of shared capabilities or enchantments
- direct remote consumption by recipient jobs
- anonymous or unattributed transfers
- final sharing limits, transfer rates and interface art before delivery alignment
