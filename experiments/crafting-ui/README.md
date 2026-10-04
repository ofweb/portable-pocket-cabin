# Vanilla crafting station references

Screenshots captured from Minecraft 26.2 through client GameTests at 1280×800
with GUI scale 3. These are the vanilla interfaces before cabin storage integration.
The fixture unlocks every recipe and supplies a limited inventory to show recipe
availability. It does not establish recipe-discovery rules for the cabin.

| Station | Normal or empty screen | Populated screen or recipe book |
|---|---|---|
| Crafting table | [Normal](../../screenshots/vanilla/crafting-stations/vanilla-01-crafting-table.png) | [All recipes](../../screenshots/vanilla/crafting-stations/vanilla-02-crafting-recipe-book-all.png), [Craftable recipes](../../screenshots/vanilla/crafting-stations/vanilla-03-crafting-recipe-book-craftable.png) |
| Loom | [Empty](../../screenshots/vanilla/crafting-stations/vanilla-04-loom-empty.png) | [Available patterns](../../screenshots/vanilla/crafting-stations/vanilla-05-loom-patterns.png) |
| Cartography table | [Empty](../../screenshots/vanilla/crafting-stations/vanilla-06-cartography-empty.png) | [Expand a map](../../screenshots/vanilla/crafting-stations/vanilla-07-cartography-expand-map.png) |
| Stonecutter | [Empty](../../screenshots/vanilla/crafting-stations/vanilla-08-stonecutter-empty.png) | [Available recipes](../../screenshots/vanilla/crafting-stations/vanilla-09-stonecutter-recipes.png) |
| Smithing table | [Empty](../../screenshots/vanilla/crafting-stations/vanilla-10-smithing-empty.png) | [Upgrade a named pickaxe](../../screenshots/vanilla/crafting-stations/vanilla-11-smithing-netherite-upgrade.png) |
| Furnace | — | [All recipes](../../screenshots/vanilla/crafting-stations/vanilla-12-furnace-recipe-book-all.png), [Craftable recipes](../../screenshots/vanilla/crafting-stations/vanilla-13-furnace-recipe-book-craftable.png) |

Crafting and furnace recipe books provide search, category tabs, and an
availability filter. Missing ingredients give recipe buttons red borders in
this version. Loom and stonecutter show choices after inputs are placed.
Cartography and smithing have no recipe-book panel.

To repeat the captures:

```sh
just screenshots
```

The fixture is `VanillaStationScreenshots.java` in the GameTest source set.
The command regenerates all captures and saves these [vanilla station screenshots](../../screenshots/vanilla/crafting-stations)
in the top-level `screenshots/` folder. They survive later tests that clear build output.
