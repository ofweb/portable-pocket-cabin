# Room relocation feasibility

This isolated prototype investigates B-0005 room relocation on Minecraft 26.2,
Fabric Loader 0.19.5, and Fabric API 0.159.0+26.2. It does not change production
cabin behavior or implement expansion purchases.

The preserved runner passed all twelve custom GameTests and the Fabric reference
test in a fresh disposable project. Abrupt-restart recovery also passed. The
[results](results.json) retain success markers and hashes of the tested sources.

## Run

From the repository root:

```sh
python experiments/room-relocation/run.py
```

The runner copies the current project and its Gradle caches into a temporary
directory. It runs offline with Java 25 or newer. The dependency cache must
already contain this project's dependencies. Gradle and the local Minecraft
servers need permission to open local sockets. Logs stay in the temporary
project. `--output` selects a new output directory; `--cache-source` selects an
existing Gradle cache directory. Production sources and development worlds
remain untouched.

## Checks

The twelve custom GameTests cover:

- One-block overlapping moves east, west, north, and south, including chunk boundaries.
- Single and double chest contents, named items, furnace inventory and cooking progress.
- Bed and door halves, a wall torch, crop state, and contained water.
- Pending block and fluid updates, including their time, priority, and order in the saved snapshot.
- The same animal's UUID, name, health, and translated position.
- Recovery after clearing, partial placement, complete placement, and entity movement. Each check reloads the snapshot from a file and replays it twice before simulation resumes.
- Destination obstruction without changing the source inventory or blocking block.
- A moved furnace finishing its existing recipe through normal world ticks.
- An item frame retaining its support, item, and rotation through later survival checks.
- A vehicle retaining its passenger relationship after relocation.

The separate-process check runs in the actual pocket dimension. It saves a
partly relocated room and its move snapshot, then abruptly terminates the Java
process without normal server shutdown. A new process loads that partial world,
replays the snapshot twice, and checks the recovered blocks, inventory data,
furnace data, and original animal identity and position. This exercises one
persisted interruption state. It does not explore every possible ordering of
chunk, entity, registry, and player saves.

## Findings

Overlapping moves need a complete source snapshot before any source block is
changed. Block updates and container-removal side effects must be suppressed
while rebuilding. Block and fluid schedules need explicit relocation.

Chunk and entity loading must finish before capture. The pocket dimension needs
active simulation tickets while collecting its entities. Source readiness must
be checked explicitly before capture.

Ordinary entity teleporting dismounts passengers. The prototype records and
restores those relationships after moving the entities. A captured passenger
whose vehicle is outside the region is rejected. Item-frame attachment survives
the tested move.

Saved absolute destinations allow recovery to repeat the move while preserving
its tested contents. A saved snapshot must remain authoritative until recovery
finishes. Interactions and simulation must not change the affected contents
between capture and completion. Replaying an old snapshot after players resume
using the room would overwrite their later changes.

## Limits

This supports feasibility for the tested vanilla contents. It does not establish
compatibility with arbitrary modded blocks, external networks, or state stored
outside the moved region. Supported mod profiles need relocation checks.

The prototype does not handle online players, offline return positions, bed
respawn bindings, leash relationships, moving pistons, boundary-spanning
contraptions, or cabin-specific fixtures and connection records. Those cases
remain unverified. Maximum wing sizes and server-pause cost also remain open.

Production Design must settle move isolation, durable completion, metadata
updates, and recovery across independent saves. The prototype restores a move
snapshot; it does not implement funding, registry integration, or complete
transaction rollback. The behavioral brief remains Draft.

See [the room brief](../../.workflow/features/B-0005/brief.md) and
[the expansion geometry decision](../../.workflow/decisions/adr/0008-derive-expansion-geometry-from-saved-size.md).
