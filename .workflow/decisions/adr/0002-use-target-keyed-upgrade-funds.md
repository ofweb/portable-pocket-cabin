# ADR-0002: Use upgrade funds for each target

Status: Accepted

## Context

One tracked fund prevented contributions to different upgrades at the same time. Players also could not easily correct deposits. Multiple targets require saved contributions that cannot become general cabin storage.

## Decision

The cabin registry stores each non-empty fund under a stable upgrade target. A fund records resolved requirements and specified contributed stacks. Panel selection and installation confirmation stay in temporary menu state. The registry stores no tracked or selected upgrade.

Virtual menu slots show each requirement as a list of saved stacks in order. A total can exceed the stack limit without combining stacks that have different components. The server validates the cursor, inventory, and fund transaction before committing and saving a change. Fund revisions reject outdated actions and cancel pending confirmations. All viewers receive the resulting state after an accepted change.

The slots are not controller inventories, hopper targets, pipe endpoints, or general storage capabilities. Only available targets accept new deposits. Funds stay with their targets through packing, travel, and restart. Schema 4 migrates its tracked fund to the same target with unchanged requirements and stacks.

## Rationale

Funds for each target permit contributions at the same time. The registry keeps materials with the cabin independently of controller location. Each click completes as one transaction. A viewer cannot modify only one side of a transfer.

## Scope

This decision applies to fund ownership, stack components, and menu transactions. [PDR-0007](../pdr/0007-fund-and-install-cabin-upgrades.md) states contribution and permission rules. [ADR-0005](0005-authoritative-cabin-registry.md) states registry authority.

New targets require stable identities and saved requirement values. Cost changes cannot replace a non-empty fund without informing players. Storage and automation require transactions with owner confirmation.
