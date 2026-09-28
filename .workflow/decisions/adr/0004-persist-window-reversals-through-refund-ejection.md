# ADR-0004: Persist window reversals through refund ejection

Status: Accepted

## Context

Window reversal changes saved cabin state, wall blocks, funds, and dropped items. Minecraft cannot commit these stores together. A crash must not duplicate or lose a paid-step refund.

## Decision

Schema 7 adds a window-reversal journal alongside the installation journal. The journals are mutually exclusive. Either active journal locks fund and upgrade actions until recovery completes. The reversal journal stores the action, target, expected and resulting tiers, exact refund stacks, invalidated funds, and phase.

Recovery applies an idempotent wall mutation and flushes resulting cabin state. It then ejects indexed, operation-tagged item entities beside the interior controller. It clears the journal only after ejection and another flush. Recovery loads the refund chunk before it checks persisted entities. A partial batch creates only missing indexed entities.

## Rationale

A separate reversal journal keeps refund phases out of the existing upward-installation path. Persisted operation tags let recovery identify the exact ejection batch. The controller gives one refund destination independent of owner location or inventory capacity.

## Scope

This decision covers downgrade and removal recovery. [ADR-0003](0003-persist-window-identity-and-derive-geometry.md) owns installation recovery. [PDR-0008](../pdr/0008-purchase-and-reverse-cabin-windows.md) owns refund behavior.

## Consequences

Recovery must finish before a new fund or upgrade mutation. Refund entities follow normal pickup, movement, fire, lava, and despawn rules after ejection. Minecraft chunk and entity-save durability still bounds hard-crash recovery.
