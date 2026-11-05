# ADR-0010: Define cooking as station recipe types

Status: Accepted

## Context

Cabin cooking replaces vanilla food cooking with many stations and long chains of intermediate items. Later features add stations such as drying racks, fermentation crocks, and mills, and datapacks must be able to add or change recipes. The cookbook and station panels run on the client, but since Minecraft 1.21.2 the server no longer sends recipes to clients.

## Decision

Each cooking station kind has its own Minecraft recipe type, all backed by one shared `CookingRecipe` format. A recipe names its ingredients, optional container and water, optional remainders, heat level, time, technique, and result. Stations match recipes from their inputs, like a furnace, and keep progress in block entity data. Heat is a property of the station, which reads it from fuel item tags and campfires. Recipes do not use the vanilla recipe book's placement, and stations fill their slots through mod payloads instead.

Fabric's recipe synchronization sends the cooking recipe serializers to clients, so the cookbook and panels read the same recipes the server uses.

## Rationale

Vanilla recipe types make cooking data-driven and reloadable with `/reload`, and they reuse the recipe manager, ingredient matching, and datapack overrides. One shared format keeps new stations cheap: a later feature registers a recipe type and a block entity, and its recipes are just data. Item tags for heat let datapacks retune fuels without code. Fabric's recipe sync avoids a custom recipe packet.

## Scope

This decision applies to cooking stations from [B-0045](../../features/B-0045/brief.md) onward. Time stations without heat, such as drying racks, use the same format and omit `heat`. Food benefits from B-0051 and automation from B-0025 consume these recipes and do not change their format. The [B-0045 design](../../features/B-0045/design.md) gives the schema and initial heat tags.
