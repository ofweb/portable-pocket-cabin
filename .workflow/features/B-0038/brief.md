# Players install cabin books through one shared flow

Status: Ready
Feature ID: B-0038

## Goal

Cabin owners install books through one Books tab to reveal upgrades before purchase. Cabin books have a consistent appearance, clear tooltips, and stacking by type.

## Stories and acceptance

### S1: Recognize and stack cabin books

Story: A player checks a cabin book before choosing which one to install.

Acceptance:

- Cabin books use the enchanted-book appearance and glint.
- Hovering over a cabin book identifies its type and the upgrades it reveals.
- Copies of the same cabin book type can stack. Different cabin book types cannot share a stack.

### S2: Install a cabin book

Story: A cabin owner installs a selected cabin book to reveal one or more upgrades.

Acceptance:

- Cabin Upgrades has a dedicated Books tab with one shared installation control.
- The Books tab remains available to the owner before any books are installed and after all known book types are installed.
- Every cabin book type follows this flow, including room, storage, loadout, and automation books.
- The owner manually places the chosen book into the dedicated book input slot in the Books tab.
- The slot accepts a stack of one cabin book type. Empty books, enchanted books, and other items cannot enter the slot.
- Installation uses the book in that slot. The control does not choose books from inventory or central storage.
- The Books tab shows the selected book's name and the upgrades it will reveal before confirmation.
- Placing a book in the slot shows the preview without consuming it. The owner confirms by clicking Install Book.
- Only the owner can install a book. A book in storage or inventory does not install until the owner selects it.
- When the owner installs a book, the cabin uses one book and reveals all listed upgrades for that cabin.
- Remaining copies stay in the input slot. An already installed book cannot be installed again or consumed.
- Installation reveals upgrades only in the cabin where the owner installed the book.
- The book does not purchase an upgrade or give the cabin that upgrade. Each upgrade follows the usual fund and installation rules.
- If the cabin cannot install a book, the book stays in the input slot and the interface gives the reason.
- Closing the interface returns unused books to the player's inventory. Any copies that do not fit drop beside the player.
- The cabin keeps installed book state and revealed upgrades through packing, restart, and redeployment.

## Scope

The Ready scope covers the existing storage, crafting, and greenhouse cabin books. It includes the Books tab, dedicated input slot, preview, confirmation, unused-book return, appearance, tooltips, and stacking by type.

The same installation rules apply to future cabin books, including room, loadout, and automation books. Adding those types belongs to their owning features.

## Non-goals

Book vendors, trades, village bookstalls, and new book types are deferred. Upgrade effects, costs, and purchase steps are outside this scope. Empty books and Minecraft enchanted books cannot be installed.

## Deferred book acquisition

This retained shaping belongs to later work within B-0038. It is outside the Ready installation contract and Design input.

- Villages can randomly include bookstall structures in small or large villages. Players can also place bookstalls.
- A bookstall is a villager job block. Villagers without jobs can claim open bookstalls under normal job rules.
- A book vendor offers one cabin book, selected randomly when its trade screen first opens. The offer is saved and shared by all players and cabins.
- The vendor keeps its book type. Restocking adds copies without selecting a different type. Multiple vendors can select the same type.
- Each book type has higher selection rates in some villager biomes. The villager's biome sets the rate at selection; moving the villager or bookstall does not change it.
- Desert villagers favor enchanting room and enchanting automation books. All villager biomes can select all book types.
- Other vendors and the player's cabin do not affect selection.
- Purchasing a book does not change cabin state. Players can give or trade books to other players.
- Later Shape must settle bookstall frequency, vendor appearance, trade costs, the full book list, and remaining biome selection rates. A player may trade with many vendors without finding a particular type.

## Related records

- [Direction](../../direction.md).
- [Books reveal upgrades decision](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
- [Upgrade purchase decision](../../decisions/pdr/0007-fund-and-install-cabin-upgrades.md).
- [Automation book](../../context.md#automation-book).
- [Cabin book](../../context.md#cabin-book).
- [Book vendor](../../context.md#book-vendor).
- [Bookstall](../../context.md#bookstall).
- [Revealed upgrade](../../context.md#revealed-upgrade).
- [Automation capabilities brief](../B-0007/brief.md).
- [Enchanting room brief](../B-0008/brief.md).
- [Equipment requisitions brief](../B-0028/brief.md).

## Open questions and assumptions

No material questions remain for the Ready installation scope. Deferred acquisition does not change the shared installation flow.
