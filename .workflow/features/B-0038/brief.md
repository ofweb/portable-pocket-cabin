# Players obtain and install cabin books

Status: Draft
Feature ID: B-0038

## Goal

Players can obtain cabin books in survival. A cabin owner installs each book through one control to reveal upgrades for later purchase.

## Stories and acceptance

### S1: Buy a cabin book

Story: A player visits a villager with the book vendor profession and buys one offered cabin book.

Acceptance:

- Any generated village can select a rare bookstall building. No village-size threshold applies.
- The building contains a bookstall job site. An unemployed villager can claim any available bookstall, including one placed by a player, under normal villager job rules.
- A book vendor offers one cabin book. The game chooses it at random when a player first opens that vendor's trade screen, then saves the offer.
- The vendor shows the same book offer to every player. A buyer's cabin does not change the offered book.
- The vendor keeps its book offer. Restocking adds more copies of that book but does not select a different book.
- A new vendor can offer a book that another vendor already offers. Finding a new vendor does not guarantee a new book type.
- Each book type has preferred villager biome types that raise its chance. The villager's type, shown by its clothing, sets the odds when its offer is chosen. Moving the villager or bookstall does not change those odds.
- Desert villagers favor the enchanting room book and the enchanting automation book.
- Every book can still appear from every villager biome type.
- The selection does not depend on other vendors' offers or any buyer's cabin.
- A purchase gives the player a physical book. It does not change any cabin state.
- Players can give or trade a purchased book to another player.

### S2: Install a cabin book

Story: A cabin owner selects a cabin book and installs it to reveal one or more upgrades.

Acceptance:

- The same protected cabin control installs each cabin book.
- The owner sees which upgrades the book will reveal before confirmation.
- Only the owner can install a book. A book in storage or inventory does not install by itself.
- A successful installation consumes one book and reveals all its declared upgrades for that cabin.
- Installation reveals upgrades only in the cabin where the owner installed the book.
- The book does not purchase or enable an upgrade. Each target follows the normal upgrade fund and installation rules.
- Invalid, unavailable, or already installed books stay in their source inventory and give a reason.
- Installed book state and revealed upgrades survive packing, restart, and redeployment.

## Scope

This feature includes the book vendor and one installation action for cabin books.

## Non-goals

This feature does not define the effect, price, or upgrade path of a revealed target. Blank books and ordinary enchanted books are outside this feature.

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

- The chance for a generated village to select a bookstall building, the vendor's appearance, and trade price remain open.
- The book catalogue and installation control need agreement.
- The exact selection weights and the preferred villager biomes for other book types remain open.
- Random offers cannot guarantee a missing book within a fixed number of vendors.
- The effect of B-0010 shared discoveries on book-revealed upgrades needs review.
