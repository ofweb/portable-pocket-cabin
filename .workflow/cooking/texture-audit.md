# Cooking texture audit

The [candidate index](texture-audit.csv) covers every ingredient, intermediate product, dish, and station in the [cooking book](book/README.md). It also covers 30 items used by current cooking recipes that have no book page. A recipe needs an icon for its output item; the index does not assign an icon to each recipe step.

This is a source search, not an asset import. Each row records the current game ID when present, the best named candidate, its source and license, an archive path or repository link, and the next check. A candidate still needs a visual check in game. "No matching source found" means no suitable named sprite was found in the sources inspected here.

| Status | Entries |
| --- | ---: |
| Existing Minecraft art | 50 |
| CC0 icon candidate | 50 |
| CC0 icon needing adaptation | 34 |
| CC0 pack listing needing extraction | 1 |
| MIT icon candidate | 1 |
| Existing station model needing custom texture | 5 |
| Rights and origin review | 10 |
| License hold | 3 |
| No matching source found | 486 |
| **Total** | **640** |

## Sources and checks

- [16x16 Food](https://opengameart.org/content/16x16-food) is the main source. Its page marks the pack CC0. The downloaded archive contains 188 named PNGs, all 16 × 16. Eight sample sprites were previewed. The index matches 84 entries to its files; some are variants that need changes before use.
- [SuryaniD's food icons](https://suryanid.itch.io/16x16-food-icons-variety-pack) list a lime icon under CC0. The archive and its sprite have not been inspected.
- [Unused Textures](https://github.com/malcolmriley/unused-textures) has 106 `food_*.png` files and uses CC BY 4.0. Six matching 16 × 16 sprites were previewed. They remain alternate candidates where the CC0 pack covers the same food. Reuse needs credit.
- [Create: Food](https://github.com/AverageAnime/create-food-multiloader) has broad food coverage. Its GPL-3.0 repository credits textures based on Create and Farmer's Delight. The index keeps matching files as references until their origin and reuse rights are clear.
- [Foreck's Textures](https://github.com/Foreck1/foreck-textures) has conflicting license wording between its README and LICENSE.txt. [OpenTextures](https://github.com/FreneticScribbler/OpenTextures) has few cooking matches and uses CC BY-SA 4.0. [TextureRepository](https://github.com/Futureazoo/TextureRepository) uses CC BY-NC-SA 4.0. These sources are not selected for direct import.
- [CleanRooster Assets](https://github.com/cleannrooster/forg-cleannrooster-assets) uses MIT. Its mortar and pestle icon was previewed at 16 × 16; it is an item icon, not a station model.

## Playable cooking items

The ten custom food and preparation outputs already in the mod need their own sprites. The CC0 pack has a direct fried egg candidate. Create: Food has 16 × 16 sliced brown and red mushroom references, both pending rights review. No matching source was found for chopped carrot, chopped beetroot, chopped apple, diced potato, sautéed mushrooms, apple compote, or vegetable soup. Existing vanilla outputs already use Minecraft art.

The five implemented station blocks have models that currently point to vanilla textures. Campfire and smoker can use Minecraft art. Other planned stations need models as well as textures.
