# ADR-0002: Use target-keyed upgrade funds

Status: Accepted

## Context

One tracked fund forced players to stop one upgrade before players could fund another. The tracked fund also made correction of individual deposits difficult. Several upgrade targets need independent, persistent contributions without becoming general cabin storage.

## Decision

The authoritative cabin registry stores non-empty funds by stable upgrade target. Each fund captures resolved requirements and exact contributed stacks. Panel selection and installation confirmation remain temporary menu state. The registry stores no tracked or selected upgrade.

Virtual menu slots expose each requirement as an ordered collection of preserved stacks. An aggregate can exceed a normal stack limit without combining stacks with different components. The server validates the complete cursor, inventory, and fund transaction before it commits and persists a mutation. Fund revisions reject stale actions and invalidate armed confirmations. Shared viewers refresh after each accepted change.

The slots are not controller inventories, hopper targets, pipe endpoints, or general storage capabilities. Only available targets accept new deposits. Fund contents stay bound to their target through packing, relocation, and restart. The schema-4 tracked fund migrates to its stable target without changing requirements or stacks.

## Rationale

Target-keyed funds support concurrent contributions. Registry ownership keeps materials with the cabin when its controller projection moves. Whole-click transactions prevent a stale or concurrent viewer from changing only one side of a transfer.

## Scope

This decision covers fund ownership, exact stack preservation, and menu transactions. [PDR-0007](../pdr/0007-fund-and-install-cabin-upgrades.md) owns visible contribution and permission rules. [ADR-0005](0005-authoritative-cabin-registry.md) owns registry authority.

## Consequences

New upgrade targets need stable identities and exact requirement snapshots. A changed live cost cannot silently rewrite a non-empty fund. Future storage and automation must use explicit owner-authorized transactions.
