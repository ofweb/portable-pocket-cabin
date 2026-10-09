# Private arboretum world experiment

This disposable experiment checks the allocation chosen for
[B-0023](../../.workflow/features/B-0023/brief.md) on Fabric Minecraft 26.2.
Production entrypoints and development worlds stay unchanged.

The [results](results.json) retain the checks and source hashes.
[Observer samples](observer-results.json) retain dimension and player tracking
through both private worlds and return to the cabin dimension.

Each test world uses a cabin UUID in its dimension key. A server accessor
supplies the level map, executor, and storage handle. The prototype creates
ordinary `ServerLevel` instances with the registered pocket dimension type
and a flat, empty generator. Test platforms provide the arrival floors.

## Checks

- Different terrain at identical coordinates in two worlds.
- Separate dimension save paths.
- Terrain and a chest with thirteen apples survive save, close, and reopen.
- Terrain and chest contents survive a separate server process.
- Two real clients transfer from the pocket dimension into each private world
  and back over localhost TCP. Worlds are created after the first client logs in.
- Players see each other after following through a normal dimension transfer.
  A player in another dimension is absent from the observer's entity list.
- The travelling player's UUID, seven held diamonds, and health survive transfers.
- Blocked arrivals use a nearby safe destination in the arrival dimension,
  in both directions, through the existing `SafeDestinationResolver`.

The runner reuses the two-client harness and hidden-window mixin from the
[entrance experiment](../arboretum-transfer/README.md). Its observer records
the current dimension as well as player visibility. The experiment does not
require continuous visibility across a door transfer.

## Run

```sh
python experiments/arboretum-worlds/run.py
```

`--output` selects a new disposable directory. The runner copies project
sources and the local Loom cache, then uses cached Gradle dependencies offline.
It runs seed and restart checks, followed by the two-client check. Local sockets
and a graphical display are required. Test windows stay hidden. Each process
has a deadline and its process group is stopped on interruption.

## Limits

The prototype transfers through the server API; automatic door crossing and
the planting interface remain unimplemented. It does not generate woodlands
or test wildlife, reclamation, growth catch-up, interrupted upgrades, saved
player login, packing, or connection-based access to packed cabins. It does
not benchmark cabin entry, world-loading latency, or many installed arboretums.
These checks establish the allocation and transfer path, not finished gameplay.
