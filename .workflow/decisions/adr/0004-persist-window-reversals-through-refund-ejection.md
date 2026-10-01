# ADR-0004: Save window reversals through refund delivery

Status: Accepted

## Context

Window reversal affects saved state, wall blocks, funds, and dropped items. Minecraft cannot commit these stores together. Recovery must not copy or lose a refund after a crash.

## Decision

Schema 7 adds a window-reversal journal. Only one reversal or installation journal can be active. An active journal locks fund and upgrade actions until recovery completes. The reversal journal records the action, target, expected and resulting tiers, specified refund stacks, invalidated funds, and phase.

Recovery applies the wall change so that repetition has no additional effects, then saves the resulting cabin state. It creates item entities with operation tags and indices beside the interior controller. It clears the journal only after item delivery and another write of saved state. Recovery loads the refund chunk before inspecting saved entities. If delivery stops before all entities exist, recovery creates only missing indices.

## Rationale

A different journal keeps refund phases independent of upgrade installation. Saved operation tags identify the refund batch. The controller gives one refund destination independently of owner location or inventory capacity.

## Scope

This decision applies to downgrade and removal recovery. [ADR-0003](0003-persist-window-identity-and-derive-geometry.md) states installation recovery. [PDR-0008](../pdr/0008-purchase-and-reverse-cabin-windows.md) states refund behavior.

Recovery must complete before new fund or upgrade changes. Delivered entities follow Minecraft pickup, movement, fire, lava, and despawn rules. Minecraft chunk and entity save durability limits recovery after a hard crash.
