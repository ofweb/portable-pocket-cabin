# Vanilla crafting station references

Screenshots captured from Minecraft 26.2 through client GameTests at 1280×800
with GUI scale 3. These are the vanilla interfaces before cabin storage integration.
The fixture unlocks every recipe and supplies a limited inventory to show recipe
availability. It does not establish recipe-discovery rules for the cabin.

| Station | Normal or empty screen | Populated screen or recipe book |
|---|---|---|
| Crafting table | [Normal](screenshots/vanilla-01-crafting-table.png) | [All recipes](screenshots/vanilla-02-crafting-recipe-book-all.png), [Craftable recipes](screenshots/vanilla-03-crafting-recipe-book-craftable.png) |
| Loom | [Empty](screenshots/vanilla-04-loom-empty.png) | [Available patterns](screenshots/vanilla-05-loom-patterns.png) |
| Cartography table | [Empty](screenshots/vanilla-06-cartography-empty.png) | [Expand a map](screenshots/vanilla-07-cartography-expand-map.png) |
| Stonecutter | [Empty](screenshots/vanilla-08-stonecutter-empty.png) | [Available recipes](screenshots/vanilla-09-stonecutter-recipes.png) |
| Smithing table | [Empty](screenshots/vanilla-10-smithing-empty.png) | [Upgrade a named pickaxe](screenshots/vanilla-11-smithing-netherite-upgrade.png) |
| Furnace | — | [All recipes](screenshots/vanilla-12-furnace-recipe-book-all.png), [Craftable recipes](screenshots/vanilla-13-furnace-recipe-book-craftable.png) |

Crafting and furnace recipe books provide search, category tabs, and an
availability filter. Missing ingredients give recipe buttons red borders in
this version. Loom and stonecutter show choices after inputs are placed.
Cartography and smithing have no recipe-book panel.

To repeat the captures:

```sh
PPC_CAPTURE_STATIONS=1 ./gradlew runClientGameTest
```

The fixture is `VanillaStationScreenshots.java` in the GameTest source set.
New captures appear in `build/run/clientGameTest/screenshots/`.
The files in this directory preserve the comparison when later tests clear
the build output.
