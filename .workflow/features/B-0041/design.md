# B-0041 design: manual kitchen room

Status: Ready
Feature ID: B-0041

This design implements the [B-0041 brief](brief.md). [ADR-0012](../../decisions/adr/0012-confirm-storage-transfers-with-station-receipts.md) records how kitchen transfers between central storage and station block entities stay safe across crashes.

## Shape of the change

The kitchen is a fourth room type, built like the crafting room but with nothing supplied except two lamps. Kitchen behaviour lives in a new `CabinKitchen` class. It adds four things:

- **Kitchen state:** `CabinKitchenState(revealed, level)` in `CabinUpgradeState`, with level 0 or 1.
- **Book and purchase:** the `kitchen_book` and the `Target.kitchen(1)` upgrade target.
- **Room:** its geometry and installation.
- **Storage-backed transfers:** kitchen stations fill from, and send their outputs to, central storage.

The cooking stations from [B-0045](../B-0045/design.md) don't change. Only the code that fills and empties them learns about the kitchen.

## Room

The [room layout reference](../../../experiments/room-layout/README.md) places the kitchen south of the east corridor, opposite the future potions room. Coordinates are relative to the cabin center, with `s` the main room width and `s / 2` rounded down:

| Part | Coordinates |
|---|---|
| Shell, the walls, floor, and ceiling | x `s/2+3` to `s/2+9`, y 0 to 4, z 2 to 8 |
| Floor, 5×5 | x `s/2+4` to `s/2+8`, z 3 to 7 |
| Entrance, 1×2 in the north wall | x `s/2+6`, y 1 to 2, z 2 |

The room is recorded as a `CabinRoom` with `CabinCorridor.EAST`, so `CabinRoomSpace.shifted` moves it east with each main room expansion, like the corridor. The north wall shares blocks with the corridor's south wall.

### Installation

- Kitchen installation follows the crafting room.
- Validation requires a 5×5 main room. It installs the east corridor through `CabinCorridors.validateInstall` if the corridor isn't there yet. It refuses a room volume that overlaps an installed room, and refuses any block that is neither air nor a shared corridor block.
- The upgrade service records the installation before the world effect runs.
- The world effect:
  - Places the east corridor if it's missing.
  - Builds the shell from the saved palette: floor planks at y 0, roof planks at y 4, and wall planks between.
  - Opens the entrance.
  - Hangs two lanterns from the ceiling at floor positions (1, 1) and (3, 3).
- Each of these is a plain block placement, so running the effect again during recovery gives the same room.
- The room volume is validated empty, so installation displaces nothing and needs no receipt.
- The registry commit adds the room and the east corridor, and sets the kitchen level to 1. It runs in the same transaction that completes the installation, like the crafting room.
- `CabinKitchen.paused` reports the room volume, and the corridor while it is being installed, to `CabinCorridors` for the duration of the installation.

### Protection and furnishing

- The shell is protected through the existing room-shell check in `CabinProtection`.
- Everything inside the room is ordinary player-placed content, lanterns included.
- Household members place and break blocks there under `CabinCorridors.mayChange`. Guests can't.

### Purchase

- The kitchen book reveals the purchase, and is installed through the Books tab like the other room books.
- The Kitchen group in Cabin Upgrades shows one offer:
  - **Title:** "Kitchen".
  - **Effect:** "Install a 5x5 kitchen room".
  - **Requirements:** 16 wall logs, 8 copper ingots and 4 amethyst blocks, where wall logs are `cabin.palette().walls().structuralWoodBlock()`.
  - **Gate:** "Expand the main room to 5x5 first" while the main room is smaller.
- Once installed, the offer reads "Installed".

## Kitchen stations

A kitchen station is any B-0045 cooking station block whose position is inside the kitchen's floor and clearance: `CabinRoomSpace.contains` and not `shell`. The rule doesn't care who placed the station or where it was before. A pot or skillet counts by its own position, and the stove or oven that heats it counts by the same rule.

A kitchen station uses central storage when all of these hold:

- The cabin is deployed.
- The player is household, by `CabinStorage.mayUse`.
- Storage is installed, at storage level at least 1.
- No cabin operation is in progress.

Otherwise the station behaves exactly as in B-0045.

### Sources

`CookingFill` already takes a list of ingredient sources. Kitchen fills pass `[storage, inventory]`:

- The storage source works on a copy of the storage stacks, so the planned transfer can read and shrink it.
- The allocation from B-0045 already prefers earlier stacks. After the station's own items, storage comes first, then the inventory.
- Named and customised stacks are skipped, as before.

### Displaced items

Items the fill removes from the station go to the player's main inventory first, then to storage. The planning check from B-0045 becomes: everything displaced must fit in the inventory once the planned ingredients leave it, plus the free storage capacity once the planned withdrawals leave storage. If it doesn't fit, nothing moves.

### Fuel

When the recipe needs heat, a fill also tops up fuel for one batch:

- **Which fuel slot:** the oven's own fuel slot, or the fuel slot of the stove directly below a pot or skillet. The stove must also be a kitchen station.
- **When:** only if that slot is empty.
- **Which fuel:** the first ordinary stack whose `CookingHeat.ofFuel` equals the recipe's heat, from storage first, then the inventory.
- **How much:** `ceil(time / burnDuration)` items, at least 1, capped at one stack, all of that one fuel item.
- Missing fuel doesn't block the ingredient fill. The panel shows the recipe as fillable, and the station's status line asks for heat.

The skillet has no recipe panel, so fuel filling applies to pots and ovens.

### Outputs

Shift-clicking the output or byproduct slot of a kitchen station moves the stack to the player's inventory first, then into storage, through `CabinStorageState.deposit`. Any remainder stays in the slot.

### Availability

- **Station recipe panel:** for a kitchen station, the reply counts storage, the inventory, and the station's own ingredients, in the order filling uses them.
- **Cookbook:** unchanged. It already counts storage for household members inside the cabin.

## Transfers and recovery

A kitchen fill or output changes up to three stores, and each is saved separately:

- central storage, in the cabin registry
- the station block entity, in the pocket chunk
- the player's inventory, in player data

[ADR-0012](../../decisions/adr/0012-confirm-storage-transfers-with-station-receipts.md) settles how these stay consistent. Transfers that don't touch storage keep the B-0045 behaviour.

1. **Plan:** the whole transfer is computed without changing anything, as in B-0045. Every check runs here: cabin, role, station position, recipe, contents, and space.
2. **Receipt:** one registry update writes the storage stacks after the transfer, together with a `KitchenTransfer` receipt in `CabinKitchenState.pending`. The update is then flushed. The receipt holds:
   - the operation id
   - the player
   - the station's position
   - the station's contents and fuel before and after
   - the player's main inventory before and after
   - the items withdrawn from and deposited into storage
3. **Apply:** the station and fuel slots are set, and the block entity records the operation id in a saved `last_transfer` field. Then the player's inventory is set.
4. **Confirm:** the pocket chunk holding the station is saved, and the player is saved with `CabinPlayerSave.confirm`. The receipt is then removed and the registry flushed.

Recovery runs at server start for each cabin, and when the receipt's player joins:

- **The station has `last_transfer` equal to the receipt:** the station was saved after the transfer. The player gets the receipt's after inventory if online, or at join through the existing join recovery. Then the receipt is removed.
- **The station doesn't have it, or is gone:** the transfer is undone. The withdrawals return to storage and the deposits are taken back from it. Anything that no longer fits is delivered as held items at the station, the way `CabinStorage.recover` delivers escrow. The player gets the receipt's before inventory. Then the receipt is removed.
- **A player with a pending receipt can't start another kitchen transfer** until it clears.

Both outcomes leave each item in exactly one place. A cabin operation that moves the room, such as packing or expansion, first clears pending receipts by this recovery.

## Persistence

- `CabinUpgradeState` gains an optional `kitchen` field with an empty default, so existing schema-10 saves load unchanged.
- `CabinKitchenState` holds `revealed`, `level`, and `pending` receipts.
- Station block entities gain `last_transfer`, an optional UUID that older saves don't have.
- The room itself is a `CabinRoom`, so B-0005 already preserves and relocates it with its contents. Kitchen stations pause in a packed cabin through standard block entity ticking, as in B-0045.

## Verification

GameTests:

- **Installation:**
  - The purchase is refused below a 5×5 main room, and when the room's space is obstructed.
  - Installing builds the shell, entrance, lanterns, and the east corridor.
  - The room moves east with an expansion.
  - The shell is protected, but stations and lanterns inside it can be broken by household members and not by guests.
- **Station membership:** a station inside the kitchen uses storage; the same station outside the room, or with storage not installed, doesn't.
- **Filling:**
  - Ingredients come from storage before the inventory, and named stacks are skipped.
  - Displaced items go to the inventory, then storage.
  - The fill refuses when displaced items don't fit in either.
  - Fuel at the recipe's heat fills an empty oven and an empty stove below a pot, and isn't added when fuel is present.
- **Outputs:** shift-clicking an output overflows into storage when the inventory is full.
- **Recovery:**
  - A receipt whose station shows `last_transfer` is completed.
  - A receipt whose station doesn't show it is undone, with storage, station, and inventory each matching their before state.
  - Undo that overflows storage delivers held items.

Server checks: a `KITCHEN_INSTALLATION` and a `KITCHEN_RECOVERY` check in the dedicated-server run, like the existing room checks. The recovery check interrupts a fill after the receipt is written and restarts the server.

Client GameTests capture the empty kitchen, a furnished kitchen, and the pot recipe panel showing stored ingredients, for `screenshots/cabin/kitchen/`.

## Delivery

Each step is a commit with its tests green:

1. Kitchen state, book, purchase, room geometry, installation, protection, and the installation server checks.
2. Kitchen station membership, storage sources, displaced items, fuel filling, panel availability, and output overflow, all through transfer receipts.
3. Recovery for receipts, the recovery server check, and join recovery.
4. Screenshots, README section, and backlog update.

## Assumptions

- Lanterns hanging from the ceiling give enough light; players add their own lighting otherwise.
- One stack of fuel per fill is enough. B-0033 tunes recipe times and may change the rule.
