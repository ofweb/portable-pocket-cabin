# Milestone 11: Compatibility, balance and presentation

**Depends on:** Every completed system being tuned, integrated or presented.

**Outcome:** The completed feature set feels coherent in survival and remains reliable in the supported modpack.

**Status:** Draft. Compatibility claims require testing in the declared release versions.

## Scope

This milestone owns:

- the optional-mod compatibility matrix and release-version claims
- survival tuning of recipes, capacities, rates, limits, loot and costs
- higher-fidelity fake windows within the existing state-driven architecture
- consistent models, textures, sounds, tooltips, interfaces and status messages
- multiplayer soak, restart, reconciliation and administrative-recovery testing
- documentation of supported versions, extension points and optional-content failure behavior

Earlier milestones define integration contracts but cannot claim tested compatibility until their combinations pass here.

## Compatibility matrix

The release matrix must cover:

| Content | Required acceptance |
| --- | --- |
| Vanilla | Every completed milestone and baseline regression fixture |
| Biomes O' Plenty | Declared palette, door and attunement profiles; absence leaves vanilla usable |
| Farmer's Delight Refabricated | Ordinary crops, rich soil, kitchen and storage blocks; declared cooking profiles |
| Tom's Simple Storage | Ordinary in-cabin networks remain independent of central cabin storage |
| Alex's Mobs Continued | Only profiled catalysts, stable residents and livestock species are managed |
| Alex's Mobs Continued Delight | Only declared ingredients, meals and supported processes become reproducible |

Ordinary blocks and block entities should work through normal Minecraft persistence without knowledge of another mod's internals. Features that preserve entities, components or production semantics require explicit profiles.

Missing optional mods or profiles must disable only their content. They must not prevent base startup, reroll persisted attunement, substitute materials silently or corrupt saved state.

## Balance ownership

Milestone 11 settles data-driven values left open by accepted feature contracts.

### Progression and rooms

- upgrade recipes, quantities and attunement-pool weights
- maximum supported room dimensions and tier counts
- room costs, catch-up caps, growth, breeding and production rates
- storage and local-fixture capacities
- charcoal and coal-synthesis recipes and work
- supported material, crop, tree, mount and livestock profiles

### Storage, automation and enchanting

- storage capacity per tier and mailbox slots
- automation-book loot sources and rarity
- job depth, operation, work and catch-up limits
- hard-reserve defaults
- enchanting material formulas, work and active slots per tier
- supported crafting, cooking, brewing and enchanting profiles

### Connections

- connection recipes and tiers
- hallway dimensions, presentation variants and practical membership limits
- invitation, departure, dismantling and recovery interface details

Changing a value must preserve saved identities, paid receipts, committed jobs and current compatibility promises. Balance reloads must fail explicitly where live migration is unsupported.

## Presentation

Fake windows retain the lightweight exterior-condition projection from Milestones 0 and 3. Improvements may add better textures, transitions, biome cues and weather animation but not rendered cross-dimensional portals or terrain views.

Every protected interface must use consistent controls, role-filtered status and actionable failure text. Models, textures and sounds must distinguish interactive cabin systems from ordinary decoration without obscuring their Minecraft behavior.

## Release verification

The compatibility world includes every matrix entry in its supported release versions. Verification covers:

- fresh survival progression through every completed milestone without commands
- optional integrations present individually and together
- optional integrations removed from a copied test world under each documented support policy
- repeated packing, restart, schema migration and interrupted-transition recovery
- concurrent multiplayer entry, packing, storage, automation and network operations
- administrative diagnosis and recovery from damaged projections and stale items
- datapack and profile overrides with valid, missing and malformed content

Release documentation records exact supported versions, known incompatibilities, save-migration boundaries and extension schemas.

## Evergreen acceptance contract

Milestone 11 remains accepted only while release testing establishes that:

1. Every compatibility-matrix combination starts, plays and reloads under its documented policy.
2. Missing optional content fails locally without impossible base requirements or silent substitution.
3. A fresh survival world reaches every completed feature without operator commands.
4. Balance values remain finite, attainable and bounded under ordinary survival play.
5. Presentation exposes every important state and failure without weakening server authority.
6. Multiplayer soak and repeated restart tests preserve identity, ownership, resources and lifecycle invariants.
7. Administrative recovery repairs projections without deleting interiors, rooms, storage or managed residents.
8. Supported datapack and profile extensions validate early and fail with actionable diagnostics.
9. Public documentation matches the tested release versions, migrations and integration behavior.

Automated compatibility, GameTest and dedicated-server restart suites are release gates. Manual survival, GUI, audio, visual and multiplayer acceptance complete the matrix.

## Exploratory backlog

These ideas are not committed milestones:

- palette renovation
- automatic emergency fire-packing
- more exterior and room styles
- cabin naming and map integration
- cabin sleep affecting exterior night
- inferred arbitrary modded-material support

Each requires a separate scope, safety and compatibility alignment. Inferred integrations remain rejected until they have a safe contract.

The cabin carries the home, not the settlement. Outdoor defenses, paths, pens, mines and terrain adaptation remain local to each campsite.

## Out of scope

- rendered cross-dimensional windows or portals
- undocumented support for arbitrary mod internals
- silent fallback from missing persisted content
- balance changes that invalidate paid or committed state without migration
