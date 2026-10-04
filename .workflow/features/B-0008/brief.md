# A cabin learns and uses enchantments

Status: Ready
Feature ID: B-0008

## Goal

A cabin keeps known enchantments that owners and residents learn and apply at its dedicated enchanting table.

## Stories and acceptance

### S1: Install the enchanting room

Story: An owner installs the room book, then purchases the enchanting room and its table.

Acceptance:

- The room book reveals all five Enchanting purchases through B-0038. Automation uses a separate book.
- Only the owner purchases upgrades; residents can contribute under shared funding rules. Only the next cost appears.
- Installation follows B-0005 and requires 5×5 main space. The room branches west from the north corridor beyond crafting.
- Its usable floor stays 5×5 with three blocks of clear height. Upgrades preserve existing contents.
- The supplied anvil and grindstone use normal Minecraft rules, independently of library actions.
- Learning and application require installation.

Logs match saved wall wood. Collected and traded materials count without personal visits.

| Tier | Materials |
|---|---|
| I: install room | 1 enchanting table, 1 anvil, 1 grindstone, 16 wall logs, 4 amethyst blocks |
| II | 4 bookshelves, 2 prismarine crystals, 4 amethyst blocks |
| III | 4 bookshelves, 4 Nether quartz, 1 crying obsidian, 1 diamond, 4 amethyst blocks |
| IV | 4 bookshelves, 4 End stone, 1 ender pearl, 4 amethyst blocks |
| V | 4 bookshelves, 1 echo shard, 4 amethyst blocks |

### S2: Learn one enchantment

Story: An owner or resident sacrifices an enchanted item to teach the cabin one selected enchantment.

Acceptance:

- The player selects enchanted equipment or an enchanted book from inventory or installed storage to sacrifice. Ordinary books fill automatically.
- The preview identifies the enchantment, source level, book count, and destruction of the source with all its other enchantments.
- Confirmation consumes one source item and one ordinary book per newly learned level. Learning III costs three books from no knowledge, or one from II. It returns no enchanted book.
- The cabin keeps each enchantment's highest learned level, even above its room limit, and can use lower levels.
- Learning an already known or lower level consumes nothing.
- Knowledge persists through packing, restart, and redeployment and cannot be removed.

### S3: Enchant an item

Story: An owner or resident selects an item at the dedicated enchanting table and applies a known enchantment.

Acceptance:

- The player chooses a known enchantment and level; the preview shows compatibility and costs. There is no random roll.
- Levels respect cabin knowledge, the enchantment's normal maximum, and S4's room limits.
- Targets can be compatible equipment, ordinary books, or enchanted books. Ordinary books become enchanted books; existing books gain or improve compatible stored enchantments.
- Confirmation immediately changes one item. Other copies in its source stack remain unchanged.
- Amethyst shards cost the selected level multiplied by one plus the item's existing enchantment count. Lapis lazuli costs twice that amount. Stored book enchantments count too.
- Raising an existing enchantment pays the full selected-level cost under that same formula. Equal or lower levels consume nothing.
- The player selects the target from inventory or installed storage. Required amethyst and lapis fill automatically.
- The result returns to its selected source after checking space. Failure leaves inputs unchanged and gives the reason.
- Only the selected enchantment changes. Names, lore, durability, other enchantments, and unrelated vanilla item data stay intact.
- Created books work at ordinary anvils, can be traded, and can teach another cabin through sacrifice.

### S4: Increase the application limit

Story: An owner upgrades the room to apply stronger known enchantments.

Acceptance:

- The room shows its limit and the next upgrade's effect.
- Five tiers are purchased in order. I through IV apply up to the matching level. Final tier V removes the room cap.
- Upgrades leave known enchantments and existing items unchanged.
- The room tier stays with the cabin through packing, restart, and redeployment.

## Feature-wide constraints and acceptance

- Ingredient filling matches the crafting room: installed storage first, then player inventory for any shortfall. Without storage, use inventory only; no connection upgrade is required.
- Automatic filling skips named or custom ingredient stacks. The chosen target or sacrifice is always explicit. The preview shows ingredient sources and quantities; consumption requires confirmation.
- Unused pulled ingredients return to their source on closure. If unavailable, they return to player inventory, then drop normally if full.
- Owners and residents can learn, enchant, and use supplied stations. Guests cannot use household facilities, materials, or knowledge.
- Library actions require the installed table and presence near it inside the room. Player-placed stations follow Minecraft behavior.
- Each confirmation rechecks cabin identity, access, item state, quantities, compatibility, limits, and result space. Stale selections fail without consumption.
- Packing, leaving the station, or losing access closes the interface. Unconfirmed selections consume nothing. Interrupted actions complete once or leave everything unchanged; recovery cannot lose or duplicate items or knowledge.
- Amethyst replaces experience for library application; player experience stays unchanged.
- All vanilla enchantments, including treasure enchantments, are supported on vanilla items. Minecraft item eligibility and enchantment compatibility rules apply.
- Owners and residents may apply curses only through explicit selection. The preview clearly identifies the selected curse.

## Scope

The room, five tiers, supplied stations, learning, book creation, manual application, and optional central storage.

## Non-goals

Passive learning, automatic sharing of knowledge, automation slots, equipment requisitions, owner loadouts, and modded enchantment or item profiles.

## Related records

- [Direction](../../direction.md).
- [Known enchantment](../../context.md#known-enchantment).
- [Room purchase and access brief](../B-0005/brief.md).
- [Central storage](../B-0004/brief.md).
- [Household roles](../../decisions/pdr/0009-use-fixed-household-roles.md).
- [PDR-0001](../../decisions/pdr/0001-preserve-the-portable-home.md).
- [PDR-0010](../../decisions/pdr/0010-manual-enchanting-uses-all-known-enchantments.md).
- [PDR-0011](../../decisions/pdr/0011-separate-learning-from-application-limits.md).
- [Cabin books brief](../B-0038/brief.md).
- [Book and upgrade decision](../../decisions/pdr/0012-books-reveal-upgrades-before-purchase.md).
- [Equipment requisitions brief](../B-0028/brief.md).

## Open questions and assumptions

No material questions remain. Optional mod profiles are deferred; unsupported inputs are rejected without changes and with a reason.
