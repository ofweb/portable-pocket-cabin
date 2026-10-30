# Players buy cabin books from book vendors

Status: Draft
Feature ID: B-0044

## Goal

Give players a survival source of cabin books through village book vendors and bookstalls.

## Stories and acceptance

### S1: Find a book vendor

Story: A player finds a book vendor in a village or creates one with a bookstall.

Acceptance:

- Villages can randomly include bookstall structures in small or large villages. Players can also place bookstalls.
- A bookstall is a villager job block. Villagers without jobs can claim open bookstalls under normal job rules.

### S2: Buy a cabin book

Story: A player trades with a book vendor for a cabin book.

Acceptance:

- A book vendor offers one cabin book, selected randomly when its trade screen first opens. The offer is saved and shared by all players and cabins.
- The vendor keeps its book type. Restocking adds copies without selecting a different type. Multiple vendors can select the same type.
- Each book type has higher selection rates in some villager biomes. The villager's biome sets the rate at selection; moving the villager or bookstall does not change it.
- Desert villagers favor enchanting room and enchanting automation books. All villager biomes can select all book types.
- Other vendors and the player's cabin do not affect selection.
- Purchasing a book does not change cabin state. Players can give or trade books to other players.

## Scope

This draft records the book acquisition rules retained from B-0038 Shape. Installing purchased books follows B-0038.

## Non-goals

Book installation, upgrade effects, costs, and purchase steps belong to B-0038 and the features that own each book.

## Related records

- [Cabin books](../B-0038/brief.md).
- [Book vendor](../../context.md#book-vendor).
- [Bookstall](../../context.md#bookstall).
- [Cabin book](../../context.md#cabin-book).
- [Books reveal upgrades decision](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
- [Direction](../../direction.md).

## Open questions and assumptions

- Bookstall frequency, vendor appearance, trade costs, the full book list, and remaining biome selection rates remain open.
- A player may trade with many vendors without finding a particular type.
