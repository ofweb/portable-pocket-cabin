# ADR-0013: Catch up time stations from saved game time

Status: Accepted

## Context

Fermenting, ageing, drying, brining, proofing, and settling take Minecraft days. The [cooking time stations catch-up](../../direction.md#cooking-time-stations-catch-up) direction says these stations count server time that passed while their chunk was unloaded or their cabin was packed. They finish a batch but never collect, refill, or start one. The greenhouse catches up through a recoverable receipt, because it changes many blocks at once. A time station changes only its own block entity.

## Decision

Each time station saves the game time of its last tick. On every tick it advances its current batch by the game time that passed since then. A single-batch station advances its one batch, and a station with spots advances each spot's item. A tick finishes at most the current batch, or the current item in each spot. Any elapsed time beyond that is dropped, so catch-up never starts a following batch.

## Rationale

Game time advances only while the server runs, and it keeps advancing while a chunk is unloaded. A packed cabin keeps its block entity data, including the saved time. One subtraction on the next tick therefore covers the unloaded case, the packed case, and the server-time-only rule. The station's own save keeps the progress, so no receipt or recovery step is needed. Dropping the remaining time keeps catch-up from turning one batch into many.

## Scope

This applies to the proofing basket, dairy pan, fermentation crock, brine barrel, ageing shelf, and drying rack from the [phase 3 design](../../features/B-0046/design.md), and to later time stations. Heated stations keep the B-0045 rule and pause with their fire. Automation from B-0025 that collects or refills time stations is a separate decision.
