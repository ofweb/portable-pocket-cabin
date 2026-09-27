# Material profiles, version 1

Material profiles let a server datapack add exact wood families and doors for Cabin Kit crafting. A profile lives at:

```text
data/<namespace>/portable_pocket_cabin/material_profiles/<path>.json
```

The resulting profile ID is `<namespace>:<path>`. The format uses exact item and block identifiers. It does not infer families from item tags or names.

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

Both ingredient fields must name the item forms of their matching blocks. Every declared block and item must exist.

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

The `door` field must name a door block. `door_ingredient` must name that block's item form.

`required_mod` is optional for both types. When present, the loader skips the profile if the named Fabric mod is absent. Bundled resources define supported vanilla materials and conditional Biomes O' Plenty profiles.

Reload rejects an unsupported schema version or type, a missing field, an invalid registry entry, or ingredients that overlap another profile. A failed reload keeps the previous successful profile set. At least one wood family and one door profile must load. A mod that supplies blocks already saved in a cabin palette cannot be removed safely from that world.
