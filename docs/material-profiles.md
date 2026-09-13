# Cabin material profiles

Cabin material profiles are server datapack resources stored at
`data/<namespace>/portable_pocket_cabin/material_profiles/<path>.json`. The resource path becomes the
stable profile ID. For example, `data/example/portable_pocket_cabin/material_profiles/wood/cedar.json`
defines `example:wood/cedar`.

Version 1 deliberately uses exact item and block identifiers. The loader does not infer a family from
tags or naming conventions.

## Wood family

```json
{
  "schema_version": 1,
  "type": "wood_family",
  "required_mod": "examplemod",
  "planks_ingredient": "examplemod:cedar_planks",
  "structural_wood_ingredient": "examplemod:cedar_log",
  "planks": "examplemod:cedar_planks",
  "structural_wood": "examplemod:cedar_log",
  "stairs": "examplemod:cedar_stairs",
  "slab": "examplemod:cedar_slab"
}
```

`required_mod` is optional. When present, the profile is ignored if that Fabric mod ID is absent.
`planks_ingredient` and `structural_wood_ingredient` must be the item forms of the corresponding
blocks. All four block forms must exist.

## Door

```json
{
  "schema_version": 1,
  "type": "door",
  "required_mod": "examplemod",
  "door_ingredient": "examplemod:cedar_door",
  "door": "examplemod:cedar_door"
}
```

The resolved block must be a door and `door_ingredient` must be that block's item form.

Reload fails with an actionable error when a profile has an unsupported version or type, omits a
required field, references a missing or unsuitable registry entry, or overlaps another loaded profile's
planks, structural wood, or door ingredient. A failed reload leaves the previous successfully loaded
profile set active.

The bundled resources define all vanilla families and door variants. Biomes O' Plenty 26.2 profiles
are conditional on the `biomesoplenty` mod ID and cover its complete wood-family set.
