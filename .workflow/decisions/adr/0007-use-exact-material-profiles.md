# ADR-0007: Use versioned material profiles

Status: Accepted

## Context

Cabin Kit crafting accepts wood and door families from optional mods. Names and tags cannot identify all block and item forms safely. Saved palettes must resolve to known materials after reload.

## Decision

Server datapacks declare versioned wood-family and door profiles with specified item and block identifiers. Profile validation checks necessary fields, registry entries, matching item forms, door type, and duplicate ingredients before applying a new profile set. The optional `required_mod` field skips a profile when its named mod is not installed. If reload fails, the earlier profile set stays active. At least one wood profile and one door profile must load.

The cabin saves resolved profile selections in its palette at first binding. Profiles cannot derive arbitrary modded families from names or tags. Missing saved materials cannot be replaced without informing players.

## Rationale

Specified identifiers keep recipe selection and [generated cabin structures](../../context.md#generated-cabin-structure) predictable. Validation reports incompatible profile data before use. Optional profiles let the mod load without their optional mods.

## Scope

This decision applies to Cabin Kit material discovery and structures from its palette. The [material profile code](../../../src/main/java/dev/portablepocketcabin/CabinMaterialProfiles.java) validates version 1 extension data. [PDR-0005](../pdr/0005-keep-the-chosen-cabin-palette.md) states palette behavior.

New material families require profiles. A missing optional mod disables only its profiles for new selections. Removing blocks saved in a cabin palette is outside the compatibility policy.
