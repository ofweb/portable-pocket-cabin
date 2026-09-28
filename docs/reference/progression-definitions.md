# Progression definition

One server datapack can replace the shared progression definition at:

```text
data/portable_pocket_cabin/portable_pocket_cabin/progression/default.json
```

The current format uses `schema_version: 2`. The [bundled definition](../../src/main/resources/data/portable_pocket_cabin/portable_pocket_cabin/progression/default.json) shows complete costs. An override must retain all fields required by this schema, including the window costs from [PDR-0008](../../.workflow/decisions/pdr/0008-purchase-and-reverse-cabin-windows.md).

`definition_version` is a positive integer saved with the [World attunement](../../.workflow/context.md#world-attunement). `maximum_general_size` must be greater than four and no more than 32. The bundled value is 21. `wood_pool` lists exact IDs from the [material profiles](material-profiles.md). At least one listed profile must load when the attunement resolves.

`general_expansions` supplies every target size from five through the maximum. Each entry has a `target_size` and nonempty `ingredients`. Each ingredient has a positive `count` and one of these selectors:

```json
{"item": "minecraft:obsidian", "count": 4}
{"attuned_slot": "planks", "count": 12}
```

The planks slot resolves to the exact planks item of the saved wood profile. Item tags, other attuned slots, biome groups, and prerequisite tiers are not part of this format. The current schema also requires `window_base` and `window_tiers`. The bundled definition shows their structure.
