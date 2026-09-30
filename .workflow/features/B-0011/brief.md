# Optional content has explicit compatibility limits

Status: Draft
Feature ID: B-0011

## Goal

Players can use declared optional content without making the base cabin depend on missing mods.

## Stories and acceptance

The stories below are proposals for Shape.

### S1: Use a declared mod profile

Story: A player starts a world with a declared mod combination and uses its supported cabin content.

Acceptance:

- Compatibility claims name exact tested versions and combinations. A profile alone is not evidence of compatibility.
- Vanilla coverage includes every completed cabin feature.
- `Biomes O' Plenty` coverage includes declared palette, door, and World attunement profiles.
- Farmer's Delight Refabricated coverage includes crops, rich soil, kitchen and storage blocks, and declared cooking profiles.
- `Tom's Simple Storage` networks inside cabins stay independent of central storage.
- `Alex's Mobs Continued` coverage manages only profiled catalysts, stable residents, and livestock species.
- Alex's Mobs Continued Delight coverage includes only declared ingredients, meals, and processes.
- Placed blocks and block entities use Minecraft persistence. Managed entities, item data, and production need explicit profiles.

### S2: Missing optional content

Story: A player starts or reloads a copy of a world without an optional mod under its documented removal policy.

Acceptance:

- Missing mods or profiles stop only affected content. The base cabin starts and stays usable.
- Missing content does not reroll World attunement, replace saved materials silently, or corrupt saved state.
- Compatibility checks cover mods individually, together, and removed under each documented policy.
- Failure messages identify affected content and the reason it cannot operate.

## Scope

This feature includes the declared mod matrix, profile limits, absence behavior, and tested compatibility claims.

## Non-goals

Undocumented support for arbitrary internal mod behavior and inferred production or entity rules are not part of this feature.

## Related records

- [Direction](../../direction.md).
- [World attunement](../../context.md#world-attunement).
- [Optional production profiles](../B-0027/brief.md).
- [Release verification](../B-0035/brief.md).

## Open questions and assumptions

- Listed mods are compatibility candidates. Available versions, exact profiles, combinations, and removal policies need verification.
- The matrix does not claim that these mods currently operate with the project's release version.
