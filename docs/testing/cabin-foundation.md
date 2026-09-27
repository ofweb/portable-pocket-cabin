# Cabin foundation regression checks

The cabin foundation remains accepted while automated tests and targeted manual checks establish these rules. [PDR-0001](../../.workflow/decisions/pdr/0001-preserve-the-portable-home.md), [PDR-0002](../../.workflow/decisions/pdr/0002-safe-cabin-travel.md), and [PDR-0003](../../.workflow/decisions/pdr/0003-cabin-home-respawning.md) state the product decisions.

## Automated checks

- Cabin identity, owner, cell index, palette, locations, and lifecycle survive restart. Cell allocation remains monotonic.
- Repeated and concurrent deployment or packing produce one transition, one exterior, and one current packed item.
- Interrupted lifecycle transitions recover one usable deployed or packed cabin without losing the interior.
- Placement, protection, and exact-mask removal leave adjacent player blocks unchanged in every rotation.
- Entry, exit, and evacuation choose safe loaded destinations in the Overworld, Nether, and End, including campsite and world-spawn fallbacks.
- Access checks remain effective during entry and the full packing countdown. Online occupants evacuate safely, and offline occupants recover on login.
- Deployed interiors have bounded simulation. Packed interiors do not tick ordinary blocks. Vanilla inventories, beds, water, farmland, crops, and block entities persist through packing and restart.
- Cabin-home respawning handles a valid bed, a missing or blocked bed, an inactive cabin, search timeout, and concurrent packing.
- Exterior loss and item recovery do not activate stale items, create duplicate entrances, or make the interior unreachable.
- Windows show the correct exterior condition or inactive signal.
- A dedicated server starts, reloads saved cabin state, and completes lifecycle recovery without client-only dependencies.

## Manual checks

Client interaction, presentation, and multiplayer timing receive targeted manual checks when automated tests cannot establish them reliably.
