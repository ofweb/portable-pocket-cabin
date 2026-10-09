# ADR-0012: Keep recoverable forest records by chunk

Status: Accepted

## Context

An arboretum can contain more explored woodland than the server can keep
loaded. Tree growth, terrain repair, and reclamation must survive restart
without loading every explored area on entry. Cabin's greenhouse already uses
a saved clock and recovery journal for interrupted catch-up.

## Decision

Keep forest records partitioned by chunk in each arboretum's storage. They
record planting anchors, accounted growth time, original terrain changed by
players, and player placements with container protection ages. Minecraft
continues to own ordinary terrain, block-entity, and entity saves.

A compact world index owns occupancy transitions, reclamation cycle identity,
eligibility counts, and pending work. Compute reclamation quotas across the
arboretum; rounding separately in each chunk would change the agreed fraction.
Unloaded areas receive durable work records instead of forced terrain loads.
Give trees crossing chunk boundaries one planting owner to prevent duplicate
replanting or growth.

Use a persisted, monotonic server operating clock measured in ticks. Each
area accounts for its elapsed interval once. Recover pending changes and
apply catch-up when that area loads, before players can interact with it.
Bound work and neighbouring chunk loads so entry does not scan the forest.

Before automatic changes, durably record an operation identity and recovery
data. Persist resulting world changes and consumed time or cycle progress
before retiring the journal entry. Recovery must reconcile partial saves
without repeating elapsed time or reclamation. Failed recovery keeps the
affected area unavailable rather than treating unfinished work as complete.

## Rationale

Chunk records keep work proportional to areas in use. A durable index makes
whole-arboretum decisions possible without loading terrain. Journals bridge
the separate saves for Minecraft chunks and forest metadata.

## Scope

This decision owns forest metadata, time accounting, and recovery boundaries.
The brief owns growth limits, container protection, and reclamation behaviour.
Storage encoding and scheduling algorithms remain implementation choices.

## Related records

- [Arboretum contract](../../features/B-0023/brief.md).
- [Private worlds](0010-give-each-arboretum-a-private-world.md).
- [Ecology boundary](../../../src/main/java/dev/portablepocketcabin/CabinArboretumEcology.java).
- [Greenhouse recovery](../../../src/main/java/dev/portablepocketcabin/CabinGreenhouseGrowth.java).
