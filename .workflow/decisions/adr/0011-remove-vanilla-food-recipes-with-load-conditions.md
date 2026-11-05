# ADR-0011: Remove vanilla food recipes with load conditions

Status: Accepted

## Context

[B-0045](../../features/B-0045/brief.md) requires food to be cooked only at cooking stations. Vanilla furnaces, smokers, campfires, and crafting tables make food through 35 vanilla recipes. Removing them must be visible in data, reversible by datapacks, and safe for existing items, trades, and loot.

## Decision

The mod ships an override for each removed vanilla recipe at its vanilla id, under `data/minecraft/recipe/`. Each override carries a Fabric `fabric:load_conditions` entry that never passes, so the recipe manager loads no recipe for that id. The removal list is exactly the recipes whose result is a food that cooking stations now make. Golden foods, suspicious stew, block unpacking, villager trades, and loot are unchanged.

## Rationale

Overrides keep the removal in data next to the replacement cooking recipes, where a datapack can restore a vanilla recipe by providing its own file. They change no vanilla code, so furnaces and campfires keep working for everything that is not food. Campfires refuse raw food without code changes, because vanilla only accepts items that a loaded recipe can cook.

## Scope

This decision applies to vanilla food recipes. Later features remove more vanilla recipes the same way when they replace them, and they restore nothing silently. If a future Minecraft version adds a food recipe, the removal list must be updated to match. A GameTest checks that every listed id is absent. If load conditions stop removing overridden recipes, the fallback is a recipe manager filter by the same id list.
