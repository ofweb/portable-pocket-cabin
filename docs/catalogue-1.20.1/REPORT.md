# Minecraft 1.20.1 material catalogue

This catalogue records natural material sources in vanilla Minecraft, Biomes O' Plenty, and Alex's Mobs. It does not define recipes.

## Source versions

| Source | Version or commit |
| --- | --- |
| Minecraft | [Official 1.20.1 server jar](https://piston-data.mojang.com/v1/objects/84194a2f286ef7c14ed7ce0090dba59902951553/server.jar), inner jar SHA-256 `80db52b203ac5de6e5fc1c5082259df440fb2b5390b4c61d474e8fbc63cc41f5` |
| Biomes O' Plenty | [`BOP-1.20-18.x.x` at `678c9e5239d4e56562bfd865d3120fc96fd3d02c`](https://github.com/Glitchfiend/BiomesOPlenty/tree/678c9e5239d4e56562bfd865d3120fc96fd3d02c) |
| Alex's Mobs | [`1.20` at `09755dade2cfbdf14839e026d3af446f9d3ff843`](https://github.com/AlexModGuy/AlexsMobs/tree/09755dade2cfbdf14839e026d3af446f9d3ff843) |

The catalogue contains 127 biome records, 76 filtered material records, and 25 wood types. The raw extraction contains 312 material records. Each material retains concrete biome IDs and route level source files.

## Files

- `biomes.json` lists biomes, tags, mobs, and filtered candidate materials.
- `materials.json` lists filtered candidates and their acquisition routes.
- `woods.json` lists 40 wood items and groups them with bamboo into 25 wood types.
- `raw_extraction.json` retains excluded items with their source evidence and exclusion reasons.
- `build_catalogue.py` rebuilds all four JSON files from the pinned sources.

## Rebuild

Download the official 1.20.1 server jar from the Minecraft 1.20.1 release page. Extract `META-INF/versions/1.20.1/server-1.20.1.jar` as the inner jar. Check out the two mod commits above. Then run:

```sh
python3 docs/catalogue-1.20.1/build_catalogue.py \
  --server /path/to/server-1.20.1.jar \
  --bop /path/to/BiomesOPlenty \
  --alex /path/to/AlexsMobs
```

The script traces vanilla biome entries through placed and configured features. It reads block and entity loot tables. It traces BOP biome methods through placement and feature registrations. It resolves Alex's Mobs spawn predicates from `BiomeConfig` and `DefaultBiomes`, including explicit BOP biome IDs. It also records verified non-lethal acquisition in entity code.

Filtering runs after extraction. The raw file retains ores, generic blocks, common drops, and wood evidence. Candidate selection excludes these items and broad sources. A material found in more than 20 resolved biomes is treated as broadly available. This cutoff is a catalogue heuristic, not a game rule.

`eligible_biome_count` counts resolved biome IDs. `shared_family_tags` lists common source-defined tags. The family field remains conservative when no shared tag exists. The rarity label measures rough exploration effort from biome count and acquisition type. It does not measure item value or exact spawn probability. An `easy_outside_biomes` value of `null` means the source data did not establish the answer.

## Gaps and ambiguous mappings

- Forge base biome tags are absent from the three primary source sets. Fifty Alex's Mobs spawn mappings contain a Forge tag. The script uses verified BOP tag members but does not guess vanilla members. Three predicate tags remain unresolved: `alexsmobs:skreechers_can_spawn_wardens`, `forge:is_cold`, and `forge:no_default_monsters`. Thus, some vanilla mob ranges and their materials are incomplete.
- The script does not run Java world generation. Code-defined vanilla coral variants and some custom BOP feature outputs have no direct block state in the inspected configuration. Those materials are absent unless another traced route provides them. An empty biome material list does not prove that the biome lacks useful materials.
- Entity loot tables contain conditions, alternatives, and count functions. The catalogue records possible named items but does not evaluate every condition or spawn roll. A listed drop can need a special state or have a low chance.
- Vanilla non-lethal animal behavior is not fully traced from the obfuscated server classes. For example, the data extraction cannot reliably assign growth drops or horn events to concrete biome routes. These items need a separate code or runtime audit.
- Alex's Mobs entity behavior can create items outside loot tables. The script covers verified examples such as bear fur, moose antlers, roadrunner feathers, crocodile scutes, eggs, Komodo spit, and whale ambergris. Other behavior-derived items can be missing. In particular, the hammerhead shark tooth route lacks a verified biome mapping because its Forge ocean tag is not resolved.
- Wood groups reflect generated logs and loot-derived saplings. Some trees use vanilla logs with BOP leaves. A group without its own log or planks does not imply that the tree lacks wood in game.
- Structures, fishing, trades, player transport, and mod configuration changes can provide items outside these biome routes. The `biome_ids` field describes verified natural routes under the pinned defaults. It does not prove item possession came from one biome.

These gaps prevent the catalogue from serving as a final biome proof system. Recipe selection should wait for tag resolution and acquisition checks.
