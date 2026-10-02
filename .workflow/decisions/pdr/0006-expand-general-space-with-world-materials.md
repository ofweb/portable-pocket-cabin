# PDR-0006: Expand general space with world materials

Status: Accepted

## Context

Expansion must keep player construction and cabin appearance. Fixed upgrade costs must use the wood types chosen for the cabin at first construction.

## Decision

Each new cabin has a protected interior with 3×3 blocks of usable [general space](../../context.md#general-space) and its saved palette. Each purchased expansion increases both dimensions by two blocks: 3×3, 5×5, 7×7, and so on. Growth adds one block on each of the four sides around a fixed center. Player blocks in the main room stay in place. The south entrance moves outward with its wall. Clear height starts at two blocks and increases by one per expansion, reaching its ten-block cap at 19×19. General space follows Minecraft behavior while the cabin is deployed.

Expansion wood requirements follow the saved [Cabin palette](../../context.md#cabin-palette), using the wood types selected at first construction. World attunement no longer selects expansion wood. Amethyst represents [Resonance](../../context.md#resonance), and obsidian anchors work between dimensions. An upgrade can require one or both materials. Other ingredients follow the [preferred-material and fallback rules](0014-use-preferred-upgrade-materials-with-shared-fallbacks.md). Ingredient fallbacks do not replace the saved palette.

The installed definition controls costs and maximum general size. The included progression has nine expansion purchases and reaches 21×21. At that maximum, the cabin has no general-space expansion offer. A blocked space prevents expansion before cabin changes or material consumption.

Passages to purpose-specific rooms become available at 5×5. The north, west, and east wall centers are reserved for passage openings, which are shown at that size. Windows cannot occupy those reserved centers. Rooms branch off corridor sides, leaving the ends available for extension. The [room brief](../../features/B-0005/brief.md) and individual room briefs own layout assignments and remaining placement questions.

## Rationale

Odd sizes give each wall one center block for a passage. Growth around a fixed center keeps the layout symmetric. The 21×21 cap provides substantial general space while dedicated rooms provide specialized space. Growth must preserve player construction. Using the original wood types connects expansion materials to the home's appearance. Specified costs let datapacks control progression independently of cabin geometry.

## Scope

This decision applies to general-space growth and upgrade materials. [PDR-0005](0005-keep-the-chosen-cabin-palette.md) states palette rules. [PDR-0007](0007-fund-and-install-cabin-upgrades.md) states funding and installation controls. Storage, rooms, and automation while packed are independent features.

## Related records

- [ADR-0008](../adr/0008-derive-expansion-geometry-from-saved-size.md) states geometry and safe expansion rules.
- [ADR-0009](../adr/0009-persist-versioned-world-attunement.md) records the existing attunement implementation. Design must revise it to follow the palette-based material decision.
