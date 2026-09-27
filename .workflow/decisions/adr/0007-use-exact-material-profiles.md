# ADR-0007: Use exact versioned material profiles

Status: Accepted

## Context

The Cabin Kit accepts several wood and door families, including optional mod content. Names and tags cannot establish every block form, item form, or door behavior safely. Persisted palettes must resolve to known materials after reload.

## Decision

Server datapacks declare versioned wood-family and door profiles with exact item and block identifiers. The loader validates required fields, registry entries, matching item forms, door type, and duplicate ingredients before publishing a new profile set. An optional `required_mod` field skips a profile when its named mod is absent. A failed reload leaves the previous successful set active. At least one wood profile and one door profile must load.

The cabin stores the resolved profile choices in its palette at first binding. The loader does not infer arbitrary modded families from names or tags and does not substitute a missing saved material silently.

## Rationale

Exact declarations make recipe selection and structure projection predictable. Versioned validation reports unsupported content before it can affect a cabin. Optional profiles let the base mod load without an integration mod.

## Scope

This decision covers material discovery for Cabin Kit crafting and palette projection. The [version 1 format](../../../docs/reference/material-profiles.md) is the extension contract. [PDR-0005](../pdr/0005-keep-the-chosen-cabin-palette.md) owns the visible palette behavior.

## Consequences

New material families need explicit profiles. A missing optional mod disables only its profiles for new selections; removing blocks already saved in a cabin palette remains unsupported.
