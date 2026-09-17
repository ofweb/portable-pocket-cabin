# Portable Pocket Cabin

A Fabric 26.2 mod for a travelling play style. Players can craft, deploy, expand, customize, pack, and relocate a persistent pocket cabin. The [roadmap](ROADMAP.md) links each milestone's design, implementation plan, and acceptance gates.

## Playing

Obtain a Block of Amethyst to reveal the three dimensional-core recipes. Combine all three cores into a Dimensional Foundation, then craft a palette-aware `Cabin Kit` with your chosen roof planks, wall wood, floor planks, and door.

Use the Kit on top of a solid terrain block. The first use previews the 5×5 footprint; use the same surface again within 30 seconds to deploy. The front stair appears above the selected block, and the door faces you. The first successful deployment permanently binds the cabin and creates its pocket interior.

Use the exterior door or lodestone to enter. To pack, sneak-use the exterior lodestone twice within 10 seconds. A five-second evacuation and packing countdown follows.

New cabins have a 4×4 usable interior. Use the protected interior lodestone beside the exit to open **Cabin Upgrades**. Allowed visitors may inspect upgrades; the owner and trusted players may deposit or withdraw required materials. Left-click deposits a carried stack or withdraws one, right-click moves one item or half of the oldest stored stack, and shift-click deposits ordinary matching stacks from the inventory. Each icon accepts only its exact requirement and preserves stack data. Creative mode follows the same rules.

Only the owner may install a fully funded upgrade, using the check-mark button twice to confirm the unchanged target and fund. A failed installation leaves its materials available for withdrawal. Expansion keeps the entrance wall fixed, grows one block per step up to the configured limit, and refuses obstructed space.

The Cabin category provides two independent windows for each side and rear wall. Windows grow through `1×2`, `2×2`, `3×3`, `5×4`, `7×6`, and `9×8` tiers, refuse obstructed footprints, and recenter after expansion. A wall's second window becomes available after its first is installed and both footprints fit.

Only the owner can downgrade or remove a window, with two-click confirmation. The change returns exact paid materials beside the interior controller and restores the wall. Grandfathered tier-one windows return no materials. Any newly invalid fund is named during confirmation and returned separately. Dropped items then follow ordinary Minecraft behavior.

The registry uses schema 7. Schema 6 cabins retain their windows, receipts, funds, and interrupted installations. Schemas 3–5 migrate and receive one grandfathered tier-one window on each side wall; schema 4 also retains its tracked fund. New cabins begin with solid walls. Schema 2 worlds cannot migrate safely: back them up and replace the development world with `just fresh-world`.

Cabins deploy in the Overworld, Nether, or End, and their protected interiors run only while deployed. Installed windows reflect the cabin's dimension, time, weather, or inactive state.

Normal player commands are:

```text
/cabin status
/cabin trust add <player>
/cabin trust remove <player>
/cabin access private|trusted
```

The lifecycle commands `/cabin preview`, `/cabin deploy`, and `/cabin pack` remain available only to operators for debugging and recovery. Material-pack authors can extend the Cabin Kit recipe through the [version 1 material-profile format](docs/roadmap/01-acquisition-relocation.md#material-profile-format).

## Development

Requires JDK 25 or newer.

```sh
./gradlew build
./gradlew runServer
```

The build runs server-side GameTests and headless dedicated-server startup and reload checks. Development-world operators also have these inspection and recovery commands:

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
