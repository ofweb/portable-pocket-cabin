# ADR-0009: Persist versioned world attunement

Status: Accepted

## Context

Expansion costs can contain a variable plank requirement. Datapacks and optional mods can change the available woods after a save begins. A later lookup must not silently change an established requirement.

## Decision

The first progression lookup resolves one loaded wood profile from the definition pool. The world registry saves its exact profile ID and definition version for all players and cabins. Restart, reload, and later mod changes do not reroll it. An invalid saved version or unavailable profile stops upgrades with an actionable error.

One reloadable progression definition supplies the wood pool, maximum general size, and every one-block expansion cost. Each ingredient has a positive count and selects an exact item or the attuned planks slot. The loader requires each target size from five through the configured maximum. A pool without a loaded candidate fails closed. Optional profile absence does not prevent the base mod from loading.

## Rationale

The registry gives one stable answer across players and restarts. Version and profile checks expose incompatible data changes before a purchase changes cabin state. Exact profile resolution avoids a broad plank tag that could accept unintended materials.

## Scope

This decision covers world attunement and general-space cost definitions. [ADR-0007](0007-use-exact-material-profiles.md) owns profile validity. [PDR-0006](../pdr/0006-expand-general-space-with-world-materials.md) owns the visible material rule. The [progression format](../../../docs/reference/progression-definitions.md) states the current data contract.

## Consequences

Datapack authors must keep the saved definition version and profile valid, or restore compatible data before upgrades resume. A new attuned slot needs a separate decision and format change.
