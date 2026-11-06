# External references

## Cooking texture sources

- Link: [16x16 Food](https://opengameart.org/content/16x16-food), [SuryaniD food icons](https://suryanid.itch.io/16x16-food-icons-variety-pack), [Unused Textures](https://github.com/malcolmriley/unused-textures), [Create: Food](https://github.com/AverageAnime/create-food-multiloader), [Foreck's Textures](https://github.com/Foreck1/foreck-textures), [OpenTextures](https://github.com/FreneticScribbler/OpenTextures), [TextureRepository](https://github.com/Futureazoo/TextureRepository), and [CleanRooster Assets](https://github.com/cleannrooster/forg-cleannrooster-assets).
- Relevance: Candidate sprites and models for the [cooking texture audit](cooking/texture-audit.md). The CC0 16x16 Food archive provides the broadest directly indexed food set. Unused Textures supplies 16 × 16 alternatives with attribution.
- Unverified: In-game fit of each candidate, the SuryaniD archive contents, Create: Food texture origin and reuse rights, and Foreck's conflicting license wording.

## Biomes O' Plenty

- Link: [Project repository](https://github.com/Glitchfiend/BiomesOPlenty), [26.2 biome registrations](https://github.com/Glitchfiend/BiomesOPlenty/blob/26.2/common/src/main/java/biomesoplenty/api/biome/BOPBiomes.java), [26.2 build targets](https://github.com/Glitchfiend/BiomesOPlenty/blob/26.2/gradle.properties).
- Relevance: Biomes, trees, plants, and flowers broaden exploration requirements and the arboretum's selected woodland palette. The 26.2 branch declares a Fabric target and registers woodland, maple, seasonal, coniferous, snowy coniferous, and redwood forests.
- Related: [Upgrade materials and exploration](direction.md#upgrade-materials-and-exploration), [Arboretum woodlands](direction.md#arboretum-woodlands).
- Unverified: Runtime compatibility, material and woodland profiles, and the fit of each forest's appearance and features.

## Alex's Mobs

- Link: [User-selected repository](https://github.com/AlexModGuy/AlexsMobs), [loader declaration](https://github.com/AlexModGuy/AlexsMobs/blob/1.20/src/main/resources/META-INF/mods.toml), [Alex's Mobs Continued listing](https://modrinth.com/mod/alexs-mobs-continued), [hummingbird behavior](https://github.com/AlexModGuy/AlexsMobs/blob/1.20/src/main/java/com/github/alexthe666/alexsmobs/entity/EntityHummingbird.java), [sugar glider behavior](https://github.com/AlexModGuy/AlexsMobs/blob/1.20/src/main/java/com/github/alexthe666/alexsmobs/entity/EntitySugarGlider.java).
- Relevance: Intended source of animal-related materials and non-dangerous arboretum wildlife. The original inspected branch targets Forge 1.20.1; the unofficial Continued listing advertises Fabric 26.2 builds. Upstream hummingbird goals suggest a passive candidate; sugar gliders can produce sapling rewards, which need checking against arboretum retention rules.
- Related: [Upgrade materials and exploration](direction.md#upgrade-materials-and-exploration), [Arboretum woodlands](direction.md#arboretum-woodlands), [Optional-mod compatibility](features/B-0011/brief.md).
- Unverified: Runtime compatibility, Continued's behavior against upstream, eligible species, spawn density, persistence, and removal policies. Published build availability does not establish compatibility or a woodland profile.

## Farmer's Delight Refabricated

- Link: [Project repository](https://github.com/MehVahdJukaar/FarmersDelightRefabricated).
- Relevance: No longer an intended integration. [PDR-0015](decisions/pdr/0015-build-cooking-into-the-cabin.md) replaces it with cabin cooking. The greenhouse still ships growth profiles for its crops.
- Related: [Cooking concept](cooking/concept.md), [greenhouse](features/B-0019/brief.md).
- Unverified: Compatible versions for this project's Minecraft version.

## Farmer's Delight crops and soils

- Link: [Upstream project](https://github.com/vectorwing/FarmersDelight), [crop registrations](https://github.com/vectorwing/FarmersDelight/blob/1.21/src/main/java/vectorwing/farmersdelight/common/registry/ModBlocks.java), [rich soil](https://github.com/vectorwing/FarmersDelight/blob/1.21/src/main/java/vectorwing/farmersdelight/common/block/RichSoilBlock.java), [rice](https://github.com/vectorwing/FarmersDelight/blob/1.21/src/main/java/vectorwing/farmersdelight/common/block/RiceBlock.java).
- Relevance: Adds cabbage, onions, tomatoes, rice, rich soil, compost conversion, and mushroom colonies. Flooded rice and climbing tomatoes affect garden customization and preservation during reshaping. Its crop behavior can inform cabin cooking crops.
- Related: [Greenhouse](features/B-0019/brief.md).
- Unverified: Inspected upstream targets NeoForge 1.21.1. Equivalent content and compatible Fabric 26.2 support through Refabricated remain unverified.

## Fruits Delight

- Link: [User-selected repository](https://github.com/Minecraft-LightLand/FruitsDelight), [build properties](https://github.com/Minecraft-LightLand/FruitsDelight/blob/1.20/gradle.properties), [mod declaration](https://github.com/Minecraft-LightLand/FruitsDelight/blob/1.20/src/main/resources/META-INF/mods.toml), [author's content and version listing](https://modrinth.com/mod/fruits-delight), [tree registrations](https://github.com/Minecraft-LightLand/FruitsDelight/blob/1.20/src/main/java/dev/xkmc/fruitsdelight/init/plants/FDTrees.java), [bush registrations](https://github.com/Minecraft-LightLand/FruitsDelight/blob/1.20/src/main/java/dev/xkmc/fruitsdelight/init/plants/FDBushes.java).
- Relevance: Intended source of fruit trees, bushes, and other fruit plants for woodland and orchard variety. The inspected source registers thirteen tree types, including fig and peach, and treats lemon as a bush. The author's listing describes fourteen trees.
- Food evidence: The inspected 1.20 and 1.21 sources give raw fruit no status effects. [Prepared foods](https://github.com/Minecraft-LightLand/FruitsDelight/blob/f9ec48a2e05399495e8b7a34fe35bd1861b2446b/src/main/java/dev/xkmc/fruitsdelight/init/food/FoodType.java) apply fruit effects; some dishes also give Farmer's Delight Nourishment. [Recipes](https://github.com/Minecraft-LightLand/FruitsDelight/blob/f9ec48a2e05399495e8b7a34fe35bd1861b2446b/src/main/java/dev/xkmc/fruitsdelight/init/data/RecipeGen.java) include muffins, tarts, baked fruit, drinks, jam, and meals. Neither branch's [configuration](https://github.com/Minecraft-LightLand/FruitsDelight/blob/f9ec48a2e05399495e8b7a34fe35bd1861b2446b/src/main/java/dev/xkmc/fruitsdelight/init/data/FDModConfig.java) exposes a general food-effects switch. This is source inspection, not runtime verification.
- Related: [Arboretum woodlands](direction.md#arboretum-woodlands), [Arboretum brief](features/B-0023/brief.md).
- Unverified: The inspected default branch targets Forge 1.20.1 and requires Farmer's Delight. Published versions list Forge/NeoForge through 1.21.1. A suitable Fabric 26.2 version or port and cabin profiles remain unresolved.

## Seamless portal prior art

- Link: [Immersive Portals repository](https://github.com/iPortalTeam/ImmersivePortalsMod), [implementation details](https://qouteall.fun/immptl/wiki/Implementation-Details.html).
- Relevance: Demonstrates seamless transfers through remote-world rendering, multiple client worlds, remote chunk tracking, and camera-crossing synchronization. Server preloading alone does not supply those behaviors.
- Related: [Arboretum woodlands](direction.md#arboretum-woodlands), [Arboretum brief](features/B-0023/brief.md).
- Unverified: Compatibility with Fabric 26.2 and cabin contents, entities, and cleared sightlines. The inspected repository is archived and reports that it is no longer maintained. This is prior art, not a selected dependency.
