# Portable Pocket Cabin

A Fabric 26.2 mod for a travelling play style. Players can craft, deploy, expand, customize, pack, and relocate a persistent pocket cabin. [Direction](.workflow/direction.md) describes the intended end state. The [Backlog](.workflow/backlog.md) records possible work.

## Playing

Obtain a Block of Amethyst to reveal the three dimensional-core recipes. Combine all three cores into a Dimensional Foundation, then craft a palette-aware `Cabin Kit` with your chosen roof planks, wall wood, floor planks, and door. The [recipe data](src/main/resources/data/portable_pocket_cabin/recipe) gives the crafting ingredients.

Use the Kit on top of a solid terrain block. The first use previews the 5×5 footprint; use the same surface again within 30 seconds to deploy. The front stair appears above the selected block, and the door faces you. The first successful deployment permanently binds the cabin and creates its pocket interior. [Exterior screenshots](docs/cabin-exterior.md) show all four sides of the log cabin, with oak walls and a spruce roof.

Use the exterior door or lodestone to enter. To pack, sneak-use the exterior lodestone twice within 10 seconds. A five-second evacuation and packing countdown follows.

New cabins have a 3×3 usable interior with two blocks of clear height. Use the protected interior lodestone beside the exit to open **Cabin Upgrades**. Allowed visitors may inspect upgrades; the owner and trusted players may deposit or withdraw required materials. Left-click deposits a carried stack or withdraws one, right-click moves one item or half of the oldest stored stack, and shift-click deposits ordinary matching stacks from the inventory. Each icon accepts only its exact requirement and preserves stack data. Creative mode follows the same rules.

Only the owner may install a fully funded upgrade, using the check-mark button twice to confirm the unchanged target and fund. A failed installation leaves its materials available for withdrawal. Each expansion adds one block on all four sides around the fixed room center: 3×3, 5×5, 7×7, through 21×21 after nine purchases. Player blocks stay at their coordinates; the south entrance and adjacent lodestone move outward with their wall. Clear height increases with each expansion, reaching ten blocks at 19×19 and staying there at 21×21. Expansion refuses obstructed space.

Each expansion has fixed material quantities. Its plank total splits evenly across the cabin's saved floor, wall, and roof woods, combining matching types. Extra planks go to the floor, then the walls. World seed, travel, and inventory do not change costs. If an existing fund requires different wood, withdraw its materials before funding the new cost.

Datapack progression ingredients use `"palette_slot": "planks"` for this split. The old `"attuned_slot": "planks"` spelling remains accepted with palette-based behavior. `wood_pool` is optional and no longer selects upgrade materials.

The Cabin category provides two independent windows for each side and rear wall. Windows grow through `1×2`, `2×2`, `3×3`, `5×4`, `7×6`, and `9×8` tiers, refuse obstructed footprints, and move with the walls after expansion. Each window stays on its own side of the reserved wall center for future room passages. Base windows first fit at 5×5. A wall's second window becomes available after its first is installed and both footprints fit.

Only the owner can downgrade or remove a window, with two-click confirmation. The change returns exact paid materials beside the interior controller and restores the wall. Grandfathered tier-one windows return no materials. Any newly invalid fund is named during confirmation and returned separately. Dropped items then follow ordinary Minecraft behavior.

The registry uses schema 8 for centered, odd-size interiors. Earlier registries cannot migrate safely because their walls, entrances, and saved contents use different geometry. They are rejected without rewriting the save. Back up an earlier world and replace the development world with `just fresh-world`. New cabins begin with solid walls.

Cabins deploy in the Overworld, Nether, or End, and their protected interiors run only while deployed. Installed windows reflect the cabin's dimension, time, weather, or inactive state.

## Persistence and safety

Packing moves the entrance but keeps the cabin interior and its contents in place. Ordinary interior blocks pause while the cabin is packed. One cabin can have only one active exterior. Deployment rejects blocked or unsafe sites without clearing terrain. Packing removes only cabin-owned exterior blocks.

Packing stops if an occupant has no safe evacuation destination or the packed item cannot be delivered. A player who logged out inside an inactive cabin returns to a safe destination on login. If the exterior or packed item is lost, an operator can inspect and recover the cabin without deleting its interior.

Sleeping in a cabin bed makes it the owner's cabin home. If that bed is unavailable, respawning tries the current exterior, then a safe position near the death site for an inactive cabin. The last campsite and Overworld spawn remain fallbacks. A trusted visitor can sleep without changing their own home.

Normal player commands are:

```text
/cabin status
/cabin trust add <player>
/cabin trust remove <player>
/cabin access private|trusted
```

The lifecycle commands `/cabin preview`, `/cabin deploy`, and `/cabin pack` remain available only to operators for debugging and recovery. The [material profile code](src/main/java/dev/portablepocketcabin/CabinMaterialProfiles.java) validates version 1 profiles. [Profiles](src/main/resources/data/portable_pocket_cabin/portable_pocket_cabin/material_profiles) give datapack examples.

Central storage requires a Storage Cabin Book. The owner opens the interior Lodestone and clicks Book twice to reveal all six capacity levels. Fund and install the first level in the Storage tab. The protected bookshelf on the opposite wall opens storage for owners and residents. The browser provides Search, Creative categories, Miscellaneous, and Inventory tabs. Fill moves eligible missing materials from storage into the selected upgrade fund.

Book vendors belong to B-0038. Until that feature is available, operators can give the storage book with `/give @s portable_pocket_cabin:storage_book`.

## Development

Requires JDK 25 or newer.

```sh
./gradlew build
./gradlew runServer
```

The build runs server-side GameTests and headless dedicated-server startup and reload checks. The [GameTests](src/gametest/java/dev/portablepocketcabin/PortablePocketCabinGameTest.java) validate cabins, crafting, expansion, and upgrades. Development-world operators also have these inspection and recovery commands:

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
```

The [client test](src/gametest/java/dev/portablepocketcabin/PortablePocketCabinClientGameTest.java) creates a fresh world and cabin. It opens the server-synchronized upgrade screen and checks missing materials, partial funding, readiness, and installation confirmation. Screenshots use a 1280×800 window, GUI scale 3, a fixed cabin palette, and a fixed cursor position. They are saved under `build/run/clientGameTest/screenshots/`. The test leaves development saves untouched.

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
