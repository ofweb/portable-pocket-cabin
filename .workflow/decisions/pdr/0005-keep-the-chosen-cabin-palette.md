# PDR-0005: Keep the cabin palette

Status: Accepted

## Context

Players select materials when they craft a Cabin Kit. The home must keep its appearance through packing, expansion, restart, and item recovery.

## Decision

Crafting selects independent materials for the floor, wall or frame, roof or ceiling, and door. The cabin keeps these four selections after first deployment. The same [Cabin palette](../../context.md#cabin-palette) controls the exterior and general interior. A packed item can show the palette but cannot modify its saved selection.

Blocks that belong to the cabin keep their protection. Wood blocks in the cabin structure resist fire. Player wood near the cabin follows Minecraft fire rules. Portal doors stay closed and resist redstone, oxidation, and other open-state changes. Doors, controllers, and Cabin windows use defined openings without replacing structural corner frames.

## Rationale

Saved materials keep the home's appearance through travel. A lost or copied item cannot modify those selections. Cabin protection applies to its structure independently of player construction near it.

## Scope

This decision applies to saved materials and their [generated cabin structures](../../context.md#generated-cabin-structure). It does not permit palette changes. [B-0012](../../backlog.md#b-0012-palette-renovation) holds that possible feature.

Removing a mod that supplies saved palette blocks is outside the compatibility policy. The cabin cannot replace a saved material without informing the player. Expansion and repair must keep the saved palette and protected frame.
