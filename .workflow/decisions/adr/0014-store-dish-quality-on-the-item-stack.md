# ADR-0014: Store dish quality on the item stack

Status: Accepted

## Context

B-0047 lets players season a dish for quality: plain, seasoned, or well balanced. Quality must show on the item, add a small saturation bonus, and keep dishes of different quality from stacking. B-0051 later adds benefits that read quality.

## Decision

A seasoned dish carries a `portable_pocket_cabin:dish_quality` data component with its level. The station that finishes the batch also gives the result stack its own food component, with saturation raised by the level's bonus. A plain dish has neither, so it stays identical to an unseasoned one. Seasoning is computed when a batch finishes, from one item taken from each seasoning slot. The recipe's `tastes` list and the `portable_pocket_cabin:tastes/*` item tags define it.

## Rationale

Data components are how Minecraft keeps per-stack state. They make different qualities stack separately for free, survive storage and hoppers, and sync to clients for tooltips. Overriding the stack's food component reuses vanilla eating, so no mixin into food consumption is needed. Tastes as tags and recipe fields keep seasoning data-driven for datapacks.

## Scope

This applies to dish recipes that list tastes, from the [phase 3 design](../../features/B-0046/design.md) onward. B-0051 food benefits read the same component and do not change how it is stored.
