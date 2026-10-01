# PDR-0014: Use preferred upgrade materials with shared fallbacks

Status: Accepted

## Context

Upgrade costs should be predictable while using materials from supported optional mods. Repeating substitution rules for each upgrade would make the same material behave differently across costs.

## Decision

Each upgrade type and level has predefined costs and specifies preferred materials. Each non-vanilla material has a shared ordered fallback chain. A fallback can come from another supported mod and have its own fallback. Every chain ends with a vanilla material. The first available material supplies the requirement for the installed supported mods and profiles.

Each fallback specifies a fixed replacement quantity. Substitution can change the required amount to reflect differences in rarity, gathering, and crafting effort. The same replacement rule applies wherever that material is used. Following several fallback steps applies their replacement quantities in order.

Travel, inventory, deployment biomes, and purchase history do not generate or select ingredients. [Expansion wood](0006-expand-general-space-with-world-materials.md) follows the saved Cabin palette. Ingredient fallbacks do not change the cabin's appearance or saved structural materials.

## Rationale

Preferred ingredients let upgrades express their purpose. Shared fallbacks keep a material's substitution consistent across upgrades and provide a vanilla endpoint when optional sources are absent. Fixed replacement quantities allow deliberate progression balance when materials require different effort.

## Scope

This decision applies to upgrade ingredients. It does not define exact recipes, quantities, fallback mappings, or behavior after installed mods change. [Direction](../../direction.md#fixed-upgrade-costs-and-optional-materials) retains the question about displayed costs and existing contributions. The [funding decision](0007-fund-and-install-cabin-upgrades.md) still applies. Saved palette replacement and removal of mods supplying placed blocks remain separate compatibility questions.
