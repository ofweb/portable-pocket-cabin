# ADR-0009: Save versioned world attunement

Status: Accepted

## Context

Expansion costs can have a variable plank requirement. Datapacks and optional mods can modify available woods after a save begins. New lookups must not replace the saved requirement without informing players.

## Decision

The first progression lookup resolves one loaded wood profile from the definition pool. The world registry saves its specified profile ID and definition version for all players and cabins. Restart, reload, and mod changes do not select a different profile. An invalid saved version or unavailable profile stops upgrades with a reason and recovery information.

One reloadable progression definition gives the wood pool, maximum general size, and each one-block expansion cost. Each ingredient has a positive count and selects a specified item or the attuned planks slot. Definition validation requires each target size from five through the configured maximum size. A pool without a loaded candidate stops the action. Missing optional profiles do not prevent the mod from loading.

## Rationale

The registry gives all players the same stable answer through restarts. Version and profile validation reports incompatible data before purchase. Specified profile selection prevents item tags from accepting unintended materials.

## Scope

This decision applies to World attunement and general-space cost definitions. [ADR-0007](0007-use-exact-material-profiles.md) states profile validity. [PDR-0006](../pdr/0006-expand-general-space-with-world-materials.md) states material rules. The [definition code](../../../src/main/java/dev/portablepocketcabin/CabinUpgradeDefinitions.java) validates extension data. The [definition](../../../src/main/resources/data/portable_pocket_cabin/portable_pocket_cabin/progression/default.json) gives the costs.

Datapacks must keep the saved definition version and profile valid or restore compatible data before upgrades operate again. A new attuned slot requires a different decision and format.
