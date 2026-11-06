# Portable Pocket Cabin

A Fabric 26.2 mod for a travelling play style. Players can craft, deploy, expand, customize, pack, and relocate a persistent pocket cabin. [Direction](.workflow/direction.md) describes the intended end state. The [Backlog](.workflow/backlog.md) records possible work.

## Playing

Obtain a Block of Amethyst to reveal the three dimensional-core recipes. Combine all three cores into a Dimensional Foundation, then craft a palette-aware `Cabin Kit` with your chosen roof planks, wall wood, floor planks, and door. The [recipe data](src/main/resources/data/portable_pocket_cabin/recipe) gives the crafting ingredients.

Use the Kit on top of a solid terrain block. The first use previews the 5×5 footprint; use the same surface again within 30 seconds to deploy. The front stair appears above the selected block, and the door faces you. The first successful deployment permanently binds the cabin and creates its pocket interior. [Exterior screenshots](screenshots/cabin/exterior) show all four sides of the log cabin, with oak walls and a spruce roof.

Use the exterior door or lodestone to enter. To pack, sneak-use the exterior lodestone twice within 10 seconds. A five-second evacuation and packing countdown follows.

New cabins have a 3×3 usable interior with two blocks of clear height. Use the protected interior lodestone beside the exit to open **Cabin Upgrades**. Guests may inspect upgrades; the owner and residents may deposit or withdraw required materials. Left-click deposits a carried stack or withdraws one, right-click moves one item or half of the oldest stored stack, and shift-click deposits ordinary matching stacks from the inventory. Each icon accepts only its exact requirement and preserves stack data. Creative mode follows the same rules.

Only the owner may install a fully funded upgrade, using the check-mark button twice to confirm the unchanged target and fund. A failed installation leaves its materials available for withdrawal. Each expansion adds one block on all four sides around the fixed room center: 3×3, 5×5, 7×7, through 21×21 after nine purchases. Player blocks stay at their coordinates; the south entrance and adjacent lodestone move outward with their wall. Clear height increases with each expansion, reaching ten blocks at 19×19 and staying there at 21×21. Expansion refuses obstructed space.

Each expansion has fixed material quantities. Its plank total splits evenly across the cabin's saved floor, wall, and roof woods, combining matching types. Extra planks go to the floor, then the walls. World seed, travel, and inventory do not change costs. If an existing fund requires different wood, withdraw its materials before funding the new cost.

Datapack progression ingredients use `"palette_slot": "planks"` for this split. The old `"attuned_slot": "planks"` spelling remains accepted with palette-based behavior. `wood_pool` is optional and no longer selects upgrade materials.

The Cabin category provides two independent windows for each side and rear wall. Windows grow through `1×2`, `2×2`, `3×3`, `5×4`, `7×6`, and `9×8` tiers, refuse obstructed footprints, and move with the walls after expansion. Each window stays on its own side of the reserved wall center for future room passages. Base windows first fit at 5×5. A wall's second window becomes available after its first is installed and both footprints fit.

Only the owner can downgrade or remove a window, with two-click confirmation. The change returns exact paid materials beside the interior controller and restores the wall. Grandfathered tier-one windows return no materials. Any newly invalid fund is named during confirmation and returned separately. Dropped items then follow ordinary Minecraft behavior.

Cabin books share the **Books** tab in Cabin Upgrades. The owner places a book or stack in its input slot to preview the upgrades, then clicks **Install Book**. Installation consumes one copy and reveals upgrades for this cabin. Each upgrade needs its own funding and purchase.

Cabin books look like enchanted books. Hover text identifies their upgrades. Copies of the same type stack up to 64; different types stay separate. Already installed books consume nothing. Unused copies return to inventory on close, or drop beside the player if inventory is full.

Install a Crafting Room Book through the **Books** tab in Cabin Upgrades, then fund the next purchase in the Crafting category. The first purchase requires 5×5 main space and adds a fixed 5×5 room beside the north corridor. It provides a crafting table, loom, and cartography table. The next purchases add stonecutting and smithing. One book reveals all three purchases. For development, obtain the book with `/give @s portable_pocket_cabin:crafting_book`.

Owners and residents can use these stations manually. Select a recipe or operation to fill inputs from central storage first, then player inventory. Automatic filling protects named and customised ingredients. The loom, cartography, and smithing panels let you select a specific target, including a named item. Click the result to craft one batch; shift-click crafts up to one output stack. The selected recipe refills after crafting. Closing returns unused inputs to their source. Storage becomes available as soon as it is installed.

Install an Enchanting Room Book through **Books**, then fund the next purchase in **Enchanting**. The first purchase requires 5×5 main space and adds a 5×5 room west of the north corridor, beyond crafting. It supplies an enchanting table, anvil, and grindstone. Five tiers apply up to levels I–IV, then remove the room cap. The normal enchantment maximum still applies. For development, use `/give @s portable_pocket_cabin:enchanting_book`.

Owners and residents open the supplied table to learn or apply enchantments. Select an item from inventory or installed storage, then select an enchantment and level. Hover the preview book for material sources and costs. **Confirm sacrifice** destroys one source item, including its other enchantments. Learning consumes one ordinary book per newly learned level. Knowledge can exceed the room limit and stays with the cabin through travel and restart.

**Confirm enchantment** applies one known enchantment to one compatible item or book. Amethyst shards cost the selected level multiplied by one plus the existing enchantment count; lapis costs twice that amount. Materials come from installed storage first, then inventory. Named or custom ingredients are skipped. The result returns to its selected source, and experience stays unchanged. Item eligibility and enchantment conflicts use vanilla rules. Curses require explicit selection and confirmation. The supplied anvil and grindstone follow normal Minecraft rules.

Install a Greenhouse Cabin Book through the **Books** tab, then fund the next purchase in the Greenhouse category. The room branches south from the west corridor and requires 5×5 main space. Its four gardens provide 24, 48, 112, and 200 default planting spots. Owners and residents can replace beds and waterlogged slab paths above a protected foundation. Expansion preserves existing plants, soil, water, and fixtures. Manual gardening needs no storage or automation.

For development, obtain the book with `/give @s portable_pocket_cabin:greenhouse_book`. Book vendors and trades remain deferred. [Greenhouse implementation notes](docs/greenhouse.md) describe packed growth, supported plants, optional ingredients, and verification.

### Cooking

Food is cooked only at cooking stations. The vanilla furnace, smoker, campfire, and crafting table no longer cook food, and a campfire no longer accepts raw food. Each station works like a furnace or campfire. [Cooking screenshots](screenshots/cooking) show the stations, their screens, and the cookbook.

- **Stove:** burns fuel to heat the cooking pot or skillet placed on it. The fuel sets the heat: planks, sticks, and other light wood give low heat; logs, coal, and charcoal give medium heat; coal blocks, blaze rods, and lava buckets give high heat. Other fuel gives medium heat. A lit campfire gives low heat to a pot or skillet on it.
- **Cooking pot:** six ingredient slots, a bowl slot, and water. Use a water bucket on it to fill it with three uses of water. Soups and stews simmer at low heat. The pot and wok can also hold up to three levels of cooking oil, but never water and oil together. Cooking oils arrive with fruit pressing.
- **Skillet:** holds up to four items, like a campfire, and cooks each one on its own. Use it with food to add it and with an empty hand to take food back. Meat sears at high heat, fish fries at medium heat, and eggs fry at low heat.
- **Oven:** has its own fuel slot and six ingredient slots. It bakes bread, cookies, pumpkin pie, baked potatoes, and cake, and dries kelp.
- **Cutting board:** place a stack of vegetables, fruit, or mushrooms on it, then use the kitchen knife to cut one item, or sneak-use to cut the whole stack.
- **Prep counter:** the kitchen work surface, with six ingredient slots for hand work such as threading skewers. Hand work needs no heat, and it pauses while another station stands on the counter. Cut a stick on the cutting board for four skewers, then put a skewer, a raw meat, and a vegetable on the counter for a raw skewer.
- **Strainer:** sits on a prep counter and pops off if the counter is broken. It separates a liquid, which comes out in its container, from the solids left behind.
- **Grill:** has its own fuel slot and cooks up to four items or skewers on its grate, each on its own. Use it with fuel to add fuel, with food to put it on the grate, and with an empty hand to take food back. Raw skewers grill at high heat.
- **Wok:** sits on a stove or campfire, like the pot, and stir-fries in oil at high heat.
- **Æbleskive pan:** sits on a stove or campfire and cooks seven balls of batter in its wells. Each ball stops halfway until you turn it: use a skewer on the pan to turn every ball that is ready.

A station lights new fuel only when it has something to cook, but lit fuel always burns down. Cooking advances only at the recipe's heat. When the wrong fuel is burning, use a shovel on the stove or oven to put it out. The next fuel then lights at its own heat. Hoppers work with the pot, stove, oven, counter, strainer, and wok as with a furnace. On the grill they only add fuel and take spent fuel.

The **cookbook**, crafted from a book and a bowl, lists every recipe by station. Each page shows the ingredients, heat, time, and water, and explains the technique behind the dish. An ingredient that another recipe makes links to that recipe. Recipes you have everything for show in green. Household members in a cabin with central storage also count stored items. The pot, oven, counter, strainer, and wok screens have a recipe button that fills the station from your inventory, skipping named and customised items. Inside a cabin, only the household can use the stations.

Install a Kitchen Book through **Books**, then fund the purchase in the **Kitchen** category. It needs a 5×5 main room and adds an empty 5×5 kitchen south of the east corridor, lit by two hanging lanterns. Owners and residents furnish it with their own stations. Once central storage is installed, every cooking station inside the kitchen draws on it:
- The recipe button fills the station from storage first, then your inventory.
- It also gives the stove or oven an empty-slot top-up of the longest-burning fuel at the recipe's heat.
- Displaced items and shift-clicked outputs go to your inventory, then storage.

Each transfer is recorded before it happens, so a crash completes it or undoes it, never both. For development, use `/give @s portable_pocket_cabin:kitchen_book`. [Kitchen screenshots](screenshots/cabin/kitchen) show the room, a furnished kitchen, and a fill from storage.

The [cooking book](.workflow/cooking/book) describes the full cooking design, including later stations and processes.

The registry uses schema 10 for household roles, centered interiors, and connected corridors. Earlier schemas are rejected without rewriting the save. Back up an older world and create a new one; for development, replace the world with `just fresh-world`. New cabins begin with solid walls.

Cabins deploy in the Overworld, Nether, or End, and their protected interiors run only while deployed. Installed windows reflect the cabin's dimension, time, weather, or inactive state.

## Persistence and safety

Packing moves the entrance but keeps the cabin interior and its contents in place. Ordinary interior blocks pause while the cabin is packed. One cabin can have only one active exterior. Deployment rejects blocked or unsafe sites without clearing terrain. Packing removes only cabin-owned exterior blocks.

Packing stops if an occupant has no safe evacuation destination or the packed item cannot be delivered. A player who logged out inside an inactive cabin returns to a safe destination on login. If the exterior or packed item is lost, an operator can inspect and recover the cabin without deleting its interior.

Sleeping in a cabin bed makes it the owner's cabin home. If that bed is unavailable, respawning tries the current exterior, then a safe position near the death site for an inactive cabin. The last campsite and Overworld spawn remain fallbacks. A resident can sleep without changing their own home.

Each player has one household role in each cabin: its owner, a resident, or a guest. Anyone can enter a deployed cabin. Residents use household facilities, storage, rooms, and upgrade funds. Only the owner installs or reverses upgrades and changes the household. Guests can use the exit, inspect upgrades, and eat or drink. They cannot change blocks, open interior inventories, interact with entities, use other items, or drop or collect items inside the cabin. A role in one cabin gives no access to another.

Normal player commands are:

```text
/cabin status
/cabin resident add <player>
/cabin resident remove <player>
/cabin household list
```

The owner can change residents while the cabin is deployed or packed. A new resident must be online or have joined this server before. A removed resident becomes a guest immediately.

The lifecycle commands `/cabin preview`, `/cabin deploy`, and `/cabin pack` remain available only to operators for debugging and recovery. The [material profile code](src/main/java/dev/portablepocketcabin/CabinMaterialProfiles.java) validates version 1 profiles. [Profiles](src/main/resources/data/portable_pocket_cabin/portable_pocket_cabin/material_profiles) give datapack examples.

Central storage requires a Storage Cabin Book. The owner opens the interior Lodestone, selects Books, places the book in the input slot, and clicks Install Book to reveal all six capacity levels. Fund and install the first level in the Storage tab. The protected bookshelf on the opposite wall opens storage for owners and residents. The browser provides Search, Creative categories, Miscellaneous, and Inventory tabs. Fill moves eligible missing materials from storage into the selected upgrade fund.

Book vendors and trades remain deferred. Operators can give the storage book with `/give @s portable_pocket_cabin:storage_book`.

## Development

Requires JDK 25 or newer.

Shared corridor support is available for room features. A first room installation adds its complete north, west, or east corridor for free. The west passage includes the livestock bend. Corridors have 3×3 clear interiors and 1×2 entrances. Their protected walls stay closed at uninstalled room positions. Room features own purchases, facilities, dimensions, and room upgrades; B-0005 adds no room offers.

Main-room expansion moves installed wings and their contents outward. It requires online occupants to return to the main room. Recovery snapshots preserve supported blocks, inventories, scheduled updates, animal identities, and cabin-home beds. Moving spaces stay closed and paused until recovery finishes. Ordinary containers, decorations, livestock, pets, boats, and basic minecarts are supported. Contents with unsupported external state, mod contents, leashed entities, extended pistons, split objects, and blocked destinations prevent expansion. Saved palette materials remain supported as generated structure.

```sh
./gradlew build
./gradlew runServer
```

The build runs server-side GameTests and headless dedicated-server startup and reload checks. The [GameTests](src/gametest/java/dev/portablepocketcabin/PortablePocketCabinGameTest.java) validate cabins, crafting, expansion, and upgrades. The corridor checks save interrupted and committed expansions, then verify recovery in a separate server process. Run them separately with `./gradlew runCorridorReloadTest`. Greenhouse checks verify interrupted room expansion and growth catch-up across separate server processes with `./gradlew runGreenhouseReloadTest`. Enchanting checks verify interrupted upgrades, knowledge, displaced contents, packing, and redeployment with `./gradlew runEnchantingReloadTest`. Development-world operators also have these inspection and recovery commands:

```text
/cabin create [player]
/cabin list
/cabin inspect <uuid>
/cabin visit <uuid>
/cabin reconcile <uuid>
/cabin recover-item <uuid> <player>
/cabin visit-test
/cabin leave-test
```

Run the Fabric client GameTests separately with a working graphical display:

```sh
just test-client
# Or: ./gradlew runClientGameTest
# Book installation and screenshots only:
PPC_TEST_BOOKS=1 ./gradlew runClientGameTest
# Regenerate every screenshot, including vanilla references:
just screenshots
```

The [client test](src/gametest/java/dev/portablepocketcabin/PortablePocketCabinClientGameTest.java) creates a fresh world and cabin. It opens the server-synchronized upgrade screen and checks missing materials, partial funding, readiness, and installation confirmation. Screenshots use a 1280×800 window, GUI scale 3, a fixed cabin palette, and a fixed cursor position. They are saved under `build/run/clientGameTest/screenshots/`. The test leaves development saves untouched.

`just screenshots` captures all cabin screens, exterior views, vanilla crafting stations, and vanilla creative inventory in one run. It overrides the individual capture filters and copies the results into the top-level [screenshots](screenshots/README.md) folder after the tests pass. Images are grouped under `screenshots/cabin/` and `screenshots/vanilla/`, with an index linking every capture. Keep screenshots there and link to them from experiment notes. Their paths stay stable across runs, and later tests can clear build output without removing them. The command requires Python 3 and automatically uses Xvfb when no display is configured.

On Linux without a display, install Xvfb and run `xvfb-run -a ./gradlew runClientGameTest`. Client tests are separate from `build`, so the existing server checks can still run without graphics. Screenshots are captures for visual review; no regression baselines have been accepted yet. Fabric supports fuzzy comparisons through `ClientGameTestContext.assertScreenshotEquals` when an inspected baseline is ready.

`visit-test` creates a 7×7 smooth-stone safety platform in the otherwise empty pocket dimension. `leave-test` returns to the Overworld spawn.

For a headless dedicated-server smoke check:

```sh
./gradlew runStartupTest
```

The server stops after initialization and logs `DEDICATED_SERVER_STARTUP_TEST_PASSED` once the pocket dimension and registry are available. The full build also reloads that world and checks recovery, persistence, packing, redeployment, and cell allocation.

Cabin near-death respawn distance defaults to 128–256 blocks. Operators can persist different bounds with the namespaced gamerules:

```text
/gamerule portable_pocket_cabin:respawn_min_distance 128
/gamerule portable_pocket_cabin:respawn_max_distance 256
```

If the maximum is configured below the minimum, the effective maximum is clamped to the minimum; `/cabin status` reports the effective range.

### Local server and Prism client

The development launcher uses `itzg/minecraft-server` for a persistent local server and Prism Launcher for the client. One-time setup:

1. In Prism, create a clean Minecraft 26.2 instance with Fabric Loader 0.19.5.
2. Give its instance folder the ID `portable-pocket-cabin-dev`, or set `PRISM_INSTANCE` to its existing folder ID.

Then launch both sides and connect directly:

```sh
just dev
```

The mod jar is rebuilt, mounted into the server, and copied into the Prism instance before launch. If the instance lacks Fabric API, `just dev` copies the server's downloaded jar. Useful commands:

```sh
just server                 # start the Docker server and make ofweb an OP
just client                 # launch Prism and join localhost:25566
just client my-instance-id  # use a different Prism instance
just op YourMinecraftName   # enable the development OP commands
just fresh-world            # archive the current world and start a clean one
just logs
just stop
```

The server world persists under `run/server`. `just fresh-world` archives it under `run/world-backups` before generating a replacement. The server defaults to `localhost:25566`, peaceful difficulty, and no natural mob spawning. It grants operator status to `ofweb`; set `PPC_OPS` to a comma-separated list of other usernames or UUIDs. Override local defaults with `PPC_PORT`, `PPC_MEMORY`, `PPC_SERVER`, `PPC_DNS`, `PRISM_ROOT`, `PRISM_INSTANCE_DIR`, or `PRISM_BIN`. The container defaults to public DNS at `1.1.1.1` because host-local DNS stubs may be unreachable from Docker.
