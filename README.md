# Portable Pocket Cabin

A Fabric 26.2 mod for a travelling play style. The full design and gated MVP deliveries live in [SPEC.md](SPEC.md).

## Development

Requires JDK 25 or newer.

```sh
./gradlew build
./gradlew runServer
```

The build runs both the server-side GameTests and the headless dedicated-server startup check. In a development world, grant yourself operator permission and use:

```text
/cabin status
/cabin visit-test
/cabin leave-test
```

`visit-test` creates a 7×7 smooth-stone safety platform in the otherwise empty pocket dimension. `leave-test` returns to the Overworld spawn.

For a headless dedicated-server smoke check:

```sh
./gradlew runStartupTest
```

The server stops itself after initialization and logs `DEDICATED_SERVER_STARTUP_TEST_PASSED` once the pocket dimension is available.

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
just server                 # start only the Docker server
just client                 # launch Prism and join localhost:25565
just client my-instance-id  # use a different Prism instance
just op YourMinecraftName   # enable the development OP commands
just logs
just stop
```

The server world persists under `run/server`. Export `PPC_PORT`, `PPC_MEMORY`, `PPC_SERVER`, `PPC_DNS`, `PRISM_ROOT`, `PRISM_INSTANCE_DIR`, or `PRISM_BIN` when local defaults differ. The container defaults to `PPC_DNS=1.1.1.1` to avoid host-local DNS stubs that are unreachable from Docker.

## Delivery 1 manual acceptance

1. Run `./gradlew runServer` and join with a Fabric 26.2 client carrying the same mod build.
2. Confirm `/cabin status` reports `pocket_dimension=ready`.
3. Run `/cabin visit-test`; confirm the world is empty except for the generated safety platform.
4. Run `/cabin leave-test`; confirm you return safely to the Overworld spawn.
5. Stop and restart the server, then repeat steps 2–4.
