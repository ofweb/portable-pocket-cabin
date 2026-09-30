# Players get and install cabin books

Status: Draft
Feature ID: B-0038

## Goal

Players can get cabin books in Survival mode. A cabin owner installs each book through one control to reveal upgrades before purchase.

## Stories and acceptance

### S1: Purchase a cabin book

Story: A player goes to a book vendor and purchases one offered cabin book.

Acceptance:

- The game can randomly place a bookstall structure in small or large villages.
- Each bookstall is a villager job block. A villager without a job can use an open bookstall by the job rules. A player can place a bookstall.
- A book vendor offers one cabin book. The game selects the book at random when a player first opens the trade screen, then saves the offer.
- Each player sees the same book. Players in all cabins see the same offer.
- The book vendor keeps its offer. Restocking adds copies of that book but does not select a different book.
- More than one book vendor can select the same book type. A new vendor does not always have a new type.
- Each book type has a higher selection rate in some villager biomes. The villager's biome sets that rate when the game selects a trade. The rate stays the same when the player moves the villager or bookstall.
- Desert villagers have a higher selection rate for the enchanting room book and the enchanting automation book.
- The game can select all book types for all villager biomes.
- Other book vendors and the player's cabin have no effect on book selection.
- A purchase gives the player a book. Cabin state stays the same after purchase.
- Players can give the book to other players or trade the book with other players.

### S2: Install a cabin book

Story: A cabin owner installs a selected cabin book to reveal one or more upgrades.

Acceptance:

- The same cabin control is used to install each cabin book.
- The owner sees which upgrades the book will reveal before confirmation.
- Only the owner can install a book. A book in storage or inventory does not install until the owner selects it.
- When the owner installs a book, the cabin uses one book and reveals all listed upgrades for that cabin.
- Installation reveals upgrades only in the cabin where the owner installed the book.
- The book does not purchase an upgrade or give the cabin that upgrade. Each upgrade follows the usual fund and installation rules.
- If the cabin cannot install a book, the book stays in its source inventory and the cabin gives the reason.
- The cabin keeps installed book state and revealed upgrades through packing, restart, and redeployment.

## Scope

This feature includes the book vendor and one installation action for cabin books.

## Non-goals

Upgrade effects, costs, and steps are not part of this feature. Empty books and enchanted books are not part of this feature.

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

- The rate of bookstalls in villages, how the book vendor looks, and the trade cost are open.
- The book list and installation control are open questions.
- Selection rates and villager biomes for other book types are open.
- A player can trade with many vendors without seeing one book type.
