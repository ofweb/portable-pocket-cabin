# Arboretum entrance transfer experiment

This isolated experiment tests matching bent passages in the current pocket
dimension on Minecraft 26.2, Fabric Loader 0.19.5, and Fabric API 0.159.0+26.2.
It investigates B-0023 feasibility. Production sources and development worlds
stay unchanged.

The runner completed eight client cases and a separate check with two actual
clients connected over localhost TCP to one offline dedicated server.
[Results](results.json) retain packet, frame, and movement measurements and
source hashes. [Observer samples](observer-results.json) retain player tracking
through entry, following, exit, and reunion.

## Findings

| Requirement | Result |
| --- | --- |
| Destination ready when crossing | Failed for distant transfers. The destination chunk was absent at the position packet. Its floor arrived before rendering, but the passage mesh was missing in the first frames. |
| Smooth movement | Relative translation preserved position, interpolation, velocity, yaw, and pitch within the test's tolerance in both directions. Absolute transfers corrected predicted position; default flags also zeroed velocity. |
| Multiplayer continuity | Failed. A traveller three blocks from the observer disappeared across the transfer and reappeared when the observer followed. Identity, seven held diamonds, and health survived entry and exit. |
| Loading screen | None in the sampled transfer frames. Missing world geometry still made the transfer visible. |

The [rendered control](evidence/control-frame.png) shows the passage normally.
The [first distant-transfer frame](evidence/transfer-first-frame.png) exposes
sky where its walls should be. The [settled frame](evidence/transfer-settled.png)
shows the passage after rendering catches up. Matching scenery and server
preloading alone therefore fail the seamless requirement.

The [observer before entry](evidence/observer-before.png) can see the traveller
in the straight section between bends. [After entry](evidence/observer-after.png),
that same view has no traveller. This failure occurs without trees concealing
either space.

A custom client handoff could address advance rendering and player visibility
across the threshold. Its feasibility remains unverified. The selected behavior
now uses the familiar entrance door to load the arboretum, with continuous walking
from its cultivated core into woodland. The
[arboretum brief](../../.workflow/features/B-0023/brief.md) owns that choice.

## Run

From the repository root:

```sh
python experiments/arboretum-transfer/run.py
```

The runner copies the project and cached dependencies to a new temporary
directory and runs Gradle offline. `--output` selects a new disposable directory;
`--cache-source` selects the Gradle cache. Local sockets and an existing graphical
display are required. Tests keep their windows hidden while using Minecraft's
real GPU renderer. Full logs, screenshots, and worlds remain in the temporary
project. The runner bounds execution and stops its process group on interruption.

## Coverage

- Identical passages at the same location, 64 blocks apart, and 4,096 blocks apart.
- A nearby loaded destination and a previously visited destination as controls.
- Distant entry and return with default, velocity-preserving, and fully relative
  position and movement flags.
- Actual held walking input before and after transfer, with packet-time checks,
  movement ticks, and checks for later server corrections.
- The first twelve rendered frames, with images of the first four. Destination
  block data and passage meshes are checked separately.
- A second client observing the traveller, following into the destination,
  observing the return transfer, and following back. Each stage retains at least
  fifteen client samples.
- Fixed passage walls with no trees, and a flat 51×51 clearing beyond the exit.

The test rejects missing frame or tick samples, a broken rendered control,
changed rotation, lost preserved velocity, and corrections during relative
translation. Completing the runner means the experiment ran successfully;
the results separately state whether its candidate meets the seamless criteria.

## Limits

Transfers are triggered by the harness while walking. Automatic threshold
detection, destination readiness gating, packet-delay handling, and remote player
rendering remain unimplemented. The test uses local networking, eight-chunk render
distance, disabled camera bobbing, and first-person walking. Sprinting, jumping,
third-person views, packet loss, slower hardware, and optional mods need further
verification for a seamless candidate.

This is an entrance experiment, not woodland generation or a cabin-entry
performance benchmark. Biomes, cultivation, the 29×29 core's persistence, packing,
and room layout migration are outside its checks. The
[arboretum brief](../../.workflow/features/B-0023/brief.md) owns those behaviors.
