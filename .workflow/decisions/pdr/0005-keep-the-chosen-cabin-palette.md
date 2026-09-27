# PDR-0005: Keep the chosen cabin palette

Status: Accepted

## Context

Players choose materials when they craft a Cabin Kit. A moving home needs a stable appearance that survives packing, expansion, restart, and item recovery.

## Decision

Crafting selects independent floor, wall or frame, roof or ceiling, and door materials. The cabin keeps those four selections after its first deployment. The same [Cabin palette](../../context.md#cabin-palette) controls the exterior and general interior. A packed item may show the palette but cannot change the cabin's saved selection.

Cabin-owned shell blocks retain their protection. Wooden shell blocks resist fire, while nearby player wood follows normal fire rules. Portal doors remain closed and resist redstone, oxidation, and ordinary open-state changes. Doors, controllers, and windows occupy declared openings without replacing structural corner frames.

## Rationale

The chosen materials make the cabin feel like one persistent home. A saved palette prevents a lost or copied item from changing that home. The protected shell separates cabin-owned structure from surrounding player construction.

## Scope

This decision covers the saved material selection and its protected projections. It does not authorize palette renovation. [B-0012](../../backlog.md#b-0012-palette-renovation) remains a separate possible feature.

## Consequences

Removing a mod that supplies saved palette blocks is unsupported. The cabin must not silently substitute another material. Expansion and repair must preserve the chosen palette and protected frame.
