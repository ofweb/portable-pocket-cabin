# ADR-0011: Bundle a separate LGPL fruit component

Status: Accepted

## Context

B-0023 requires Fruits Delight trees, plants, recipes, and prepared-food
effects. Cabin uses MIT; the upstream mod uses LGPL-2.1-only. The project
targets Fabric Minecraft 26.2, so source reuse also requires a compatible port.

## Decision

Maintain the adapted fruit content as a separate LGPL-2.1-only component.
Build it as a distinct Fabric artifact and bundle it in the Cabin download.
Players receive the fruit content with Cabin without a separate installation.

The component owns its content registrations and upstream-derived behaviour.
Cabin owns its private worlds, planting controls, catch-up, and reclamation.
Keep Cabin state out of the fruit component; integration uses its public
content and interfaces. Independently written Cabin source retains MIT.

Distribute the fruit component's corresponding source and build materials,
preserve notices, and identify changed files and dates. Provide a documented
way to rebuild Cabin with a modified component. Check asset and dependency
licenses before copying them. A separate artifact alone does not establish
license compliance; the actual integration and distribution must satisfy LGPL.

## Rationale

Bundling provides the intended fruit experience in one download. A separate
component keeps upstream adaptation and licensing obligations visible without
spreading copied code throughout Cabin.

## Scope

This decision settles maintenance and distribution boundaries. It does not
confirm port compatibility, settle dependency substitutions, or change fruit
recipes and prepared-food effects.

## Consequences

Delivery requires runtime checks of growth, harvesting, ripening, recipes,
and effects on the target version. Release checks must also verify the
source distribution and rebuild path.

## Related records

- [Arboretum contract](../../features/B-0023/brief.md).
- [Compatibility work](../../features/B-0011/brief.md).
- [Fruit evidence](../../references.md#fruits-delight).
- [Upstream license](https://github.com/Minecraft-LightLand/FruitsDelight/blob/1.20/LICENSE).
- [LGPL-2.1-only text](https://spdx.org/licenses/LGPL-2.1-only.html).
