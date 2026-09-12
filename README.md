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
/cabin create [player]
/cabin list
/cabin inspect <uuid>
/cabin visit <uuid>
/cabin preview
/cabin deploy
/cabin pack
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
just client                 # launch Prism and join localhost:25565
just client my-instance-id  # use a different Prism instance
just op YourMinecraftName   # enable the development OP commands
just logs
just stop
```

The server world persists under `run/server`. `just server` provisions `ofweb` as an operator on every startup; set `PPC_OPS` to a comma-separated list of other usernames or UUIDs when needed. Export `PPC_PORT`, `PPC_MEMORY`, `PPC_SERVER`, `PPC_DNS`, `PRISM_ROOT`, `PRISM_INSTANCE_DIR`, or `PRISM_BIN` when local defaults differ. The container defaults to `PPC_DNS=1.1.1.1` to avoid host-local DNS stubs that are unreachable from Docker.

## Delivery 1 manual acceptance

1. Run `./gradlew runServer` and join with a Fabric 26.2 client carrying the same mod build.
2. Confirm `/cabin status` reports `pocket_dimension=ready`.
3. Run `/cabin visit-test`; confirm the world is empty except for the generated safety platform.
4. Run `/cabin leave-test`; confirm you return safely to the Overworld spawn.
5. Stop and restart the server, then repeat steps 2–4.

## Delivery 2 manual acceptance

1. Start a fresh development world and run `/cabin create` as an operator.
2. Confirm `/cabin list` shows one `PACKED` record with cell `0`, then copy its UUID.
3. Confirm a second `/cabin create` is rejected because the player already owns a cabin.
4. Run `/cabin inspect <uuid>` and `/cabin visit <uuid>`; confirm the reported coordinates agree and the pocket dimension contains a lit debug marker.
5. Restart the server and confirm `/cabin list` still shows the same UUID, owner, cell and lifecycle.
6. Join as a second player (or run `/cabin create <player>`) and confirm the new cabin receives cell `1`, then visit its distinct marker.

## Delivery 3 manual acceptance

1. Start a fresh development world, run `/cabin create`, and find a flat, clear patch of Overworld ground.
2. Run `/cabin preview`; confirm particles trace the 5×5 footprint in front of you and the command reports whether it is clear.
3. Run `/cabin deploy` within 30 seconds; confirm a small stone cabin appears with an iron door and lodestone controller.
4. Interact with the exterior iron door or controller; confirm you enter a lit 21×21 pocket interior.
5. Interact with the interior iron door; confirm you return outside the exterior doorway.
6. In survival and creative mode, confirm the exterior and interior shell cannot be broken. Trigger an explosion and a piston beside the exterior and confirm its owned blocks remain intact.
7. Restart the server, run `/cabin list` and `/cabin inspect <uuid>`, and confirm the cabin remains `DEPLOYED` with the same Overworld position and facing.
8. Confirm `/cabin preview` refuses a second deployment and that a different player cannot enter the first player's cabin during this delivery.

## Delivery 4 manual acceptance

1. Start a fresh world and run `/cabin create`; confirm a bound `Packed Cabin` item appears and its tooltip shows the cabin UUID and generation `0`.
2. Preview and deploy the cabin. Confirm deployment consumes the bound item, then place a few blocks or items inside the pocket interior.
3. Fill every owner inventory slot, stand outside within 10 blocks of the controller, and run `/cabin pack`; confirm packing is rejected before the lifecycle changes.
4. Free one slot and run `/cabin pack` again. Confirm the entrance locks immediately, occupants see the five-second countdown, and online occupants are evacuated safely.
5. Confirm the exterior's exact owned mask disappears without changing adjacent player blocks, the cabin becomes `PACKED`, and the reserved item becomes a valid generation `1` `Packed Cabin`.
6. Redeploy from that item at another clear Overworld site. Confirm the same interior contents remain and the old item was consumed.
7. Run `/cabin reconcile <uuid>` on the valid deployment and confirm it remains unchanged. Confirm `/cabin recover-item <uuid> <owner>` refuses while that exterior is valid.
8. Remove the controller with an operator `/setblock` command, run `/cabin reconcile <uuid>`, and confirm the cabin becomes `ORPHANED`. Then run `/cabin recover-item <uuid> <owner>` and confirm it becomes `PACKED` with a newer item generation.
