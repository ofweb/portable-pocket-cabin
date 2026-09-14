# Portable Pocket Cabin

A Fabric 26.2 mod for a travelling play style. The safe MVP and Milestone 1 survival-acquisition flow are complete; Milestone 2's world-attuned expansion and Milestone 3.1's tracked upgrade interface are implemented. The [roadmap](ROADMAP.md) links each milestone's design, implementation plan, and acceptance gates.

## Playing

Obtain a Block of Amethyst to reveal the three dimensional-core recipes. Craft all three cores, combine them into a Dimensional Foundation, then use the Foundation with your chosen roof planks, structural wall wood, floor planks, and door to craft a palette-aware `Cabin Kit`.

Use the Kit on the top of a solid terrain block. The first use previews the 5×5 cabin footprint; use the same top surface again within 30 seconds to deploy it. The clicked surface puts the front stair directly above that block, and the door faces back toward you. Its first successful deployment permanently binds the cabin and creates a pocket interior using the selected materials.

Normal-use the exterior door or lodestone controller to enter. To pack, sneak-use the same exterior lodestone twice within 10 seconds. The existing five-second evacuation and packing countdown then runs without commands.

New cabins begin with a 4×4 usable interior. Normal-use or sneak-use the protected interior lodestone beside the exit to open **Cabin Upgrades**. Anyone currently allowed inside may inspect the next expansion and its world-attuned requirements. Only the owner may track, stop tracking, or install it; the owner and currently authorized trusted players may deliberately deposit held materials through the green control or shift-click them from their inventory. Only missing exact items are accepted, excess stays with the contributor, and creative mode follows the same rules.

A complete fund never installs automatically. The owner chooses **Install** after every requirement is funded; failed validation leaves the tracked upgrade and materials intact. **Stop tracking** requires a second confirmation and drops every contributed stack beside the interior controller like breaking a full chest. Expansion grows one block per step up to the configured limit, keeps the entrance wall fixed, and refuses obstructed horizontal or vertical space. Clear interior height starts at two blocks, grows by one block for every two size steps, and caps at ten blocks from size 20 onward.

The tracked upgrade fund uses cabin registry schema 4 and automatically migrates variable-height schema 3 saves with no tracked upgrade or fund. Worlds using the earlier fixed-height schema 2 must still be backed up and replaced with `just fresh-world`; those generated ceiling blocks cannot be distinguished safely from player construction.

Cabins can be deployed in the Overworld, Nether, or End. Their protected interiors keep running while deployed and pause while packed. Two fake-window panels show dawn, day, sunset, night, rain, thunder, Nether, End, or closed-shutter states.

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

The build runs both the server-side GameTests and the headless dedicated-server startup check. In a development world, operators additionally have these inspection and recovery commands:

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

The server stops itself after initialization and logs `DEDICATED_SERVER_STARTUP_TEST_PASSED` once the pocket dimension and registry are available. The full `./gradlew build` gate boots that saved world a second time, reconciles interrupted packing and deployment journals, verifies the exterior and interior survived, then packs and redeploys the cabin before checking monotonic cell allocation.

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

The current mod jar is rebuilt and copied into both environments before launch. Fabric API is downloaded by the server and copied into the Prism instance automatically. Useful commands:

```sh
just server                 # start the Docker server and make ofweb an OP
just client                 # launch Prism and join localhost:25566
just client my-instance-id  # use a different Prism instance
just op YourMinecraftName   # enable the development OP commands
just fresh-world            # archive the current world and start a clean one
just logs
just stop
```

The server world persists under `run/server`. `just fresh-world` stops the server, moves `run/server/world` into a timestamped directory under `run/world-backups`, and starts a newly generated world; it does not delete the previous world. The development server publishes on `localhost:25566` by default so it can coexist with a standard Minecraft server on port `25565`. The local development server uses peaceful difficulty and disables natural mob spawning so gameplay cannot interrupt cabin testing. `just server` provisions `ofweb` as an operator on every startup; set `PPC_OPS` to a comma-separated list of other usernames or UUIDs when needed. Export `PPC_PORT`, `PPC_MEMORY`, `PPC_SERVER`, `PPC_DNS`, `PRISM_ROOT`, `PRISM_INSTANCE_DIR`, or `PRISM_BIN` when local defaults differ. The container defaults to `PPC_DNS=1.1.1.1` to avoid host-local DNS stubs that are unreachable from Docker.
