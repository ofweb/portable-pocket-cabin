# Creative inventory comparison

Minecraft 26.2 screenshots captured through client GameTests at 1280×800,
GUI scale 3, with the default English language and resource pack.
Creative references hide the operator tab and use a fixed hotbar inventory.

| State | Creative inventory | Cabin storage |
|---|---|---|
| Category | [Building blocks](../../screenshots/vanilla/creative-inventory/vanilla-creative-01-building-blocks.png), [Colored blocks](../../screenshots/vanilla/creative-inventory/vanilla-creative-03-colored-blocks.png) | [Category](../../screenshots/cabin/storage/cabin-storage-category.png) |
| Scrolling | [Scrolled building blocks](../../screenshots/vanilla/creative-inventory/vanilla-creative-02-building-blocks-scrolled.png) | [Scrolled storage](../../screenshots/cabin/storage/cabin-storage-scrolled.png) |
| Search | [All items](../../screenshots/vanilla/creative-inventory/vanilla-creative-04-search-all.png) | [All stored items](../../screenshots/cabin/storage/cabin-storage-search.png) |
| Search results | [Diamond](../../screenshots/vanilla/creative-inventory/vanilla-creative-05-search-diamond.png) | [Diamond variants](../../screenshots/cabin/storage/cabin-storage-search-variants.png) |
| Item tooltip | [Creative item](../../screenshots/vanilla/creative-inventory/vanilla-creative-06-item-tooltip.png) | [Stored variant](../../screenshots/cabin/storage/cabin-storage-variant-tooltip.png) |
| Player inventory | [Creative inventory tab](../../screenshots/vanilla/creative-inventory/vanilla-creative-07-player-inventory.png) | [Storage inventory tab](../../screenshots/cabin/storage/cabin-storage-inventory.png) |

To repeat the captures:

```sh
just screenshots
```

The fixture is `VanillaCreativeScreenshots.java` in the GameTest source set.
The command regenerates all captures, including the current storage interface.
The images live under [vanilla creative inventory](../../screenshots/vanilla/creative-inventory)
and [cabin storage](../../screenshots/cabin/storage) in the top-level `screenshots/` folder.
They survive later tests that clear build output.

The current storage panel is taller than Creative inventory.
Its search field spans most of the panel and uses a black background.
Creative inventory places a short gray search field beside the title.
Storage also reorders the tabs, omits the scrollbar track, and arranges
equipment slots in a row without the player preview.
