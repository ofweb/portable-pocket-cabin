# Direction

## Goal

Portable Pocket Cabin gives survival players a home that travels with them. A player can move its entrance between campsites without losing the home or the value of each campsite.

## Intended end state

A player can acquire a [Cabin Kit](context.md#cabin-kit) through survival play and establish one lasting pocket home. The player can deploy it at a suitable site, pack it, and take it to another site. The interior, its contents, and purchased improvements remain with the cabin through travel, restart, and recovery. The chosen [Cabin palette](context.md#cabin-palette) gives the home a consistent appearance. [World attunement](context.md#world-attunement) ties its growth to materials in the world.

The cabin becomes a useful household as the player explores. Its owner can share access, receive deliveries, and keep resources for household use. Larger [general space](context.md#general-space) and distinct rooms support ordinary living, crops, animals, and trees. A house cat can have a home there. Knowledge gained through exploration can support controlled production, enchanting, and owner loadouts. Players can connect their cabins for safe visits and cooperation while each cabin retains its own identity and resources. Each cabin must get and install its own cabin books to reveal upgrades. Learned enchantments stay local to each cabin. Connections do not give automation capabilities or learned enchantments.

Production at higher tiers follows player progress through Survival mode. It replaces resource work after that work becomes easy for the player. [The production decision](decisions/pdr/0013-gate-production-through-player-progress.md) keeps this rule separate from specific costs and tiers.

The complete experience works in survival without operator commands. Cabin interfaces make available actions, costs, access, and failures clear. Supported optional content fits the same play style and has explicit compatibility limits.

## Boundaries and tensions

The cabin carries the home. Paths, farms, mines, docks, and terrain remain at each campsite. Ordinary interior blocks keep normal Minecraft behavior while the cabin is deployed or a connected packed cabin is occupied. An empty packed cabin pauses normal simulation. When players return, the cabin calculates progress for the time it was empty. Only time while the server operates counts. Progress for that time includes rooms, jobs, and placed blocks such as furnaces and crops. Managed rooms may make bounded progress while the cabin is inactive.

The home must remain recoverable after a failed move or lost exterior. A cabin has at most one active exterior and one owner. The current model lets each player own at most one cabin. Cooperation must preserve each cabin's resources, access rules, and identity. Automation remains bounded. Ordinary interior blocks do not simulate while a packed cabin is empty. Optional integrations require declared support; arbitrary mod behavior is outside the compatibility promise.

## Open questions

### Cabin palette changes

Can an owner select a different Cabin palette for the same home? The cabin must keep its identity and purchased upgrades.

### Automatic packing during a fire

Can a fire near the cabin trigger automatic packing? A decision is necessary about the effect on cabin occupants.

### More cabin styles

Can the cabin offer different exterior and room designs? A decision is necessary about their effect on cabin identity.

### Cabin names and maps

Can players give cabins names and show their sites on maps? A decision is necessary about who can see these sites.

### Cabin sleep and exterior night

Can sleep in a cabin set the time at its deployed exterior? A decision is necessary about the effect on other players.

### More modded materials

Can the cabin use more modded materials with explicit profiles? The compatibility limits must stay in place.

### Progress after an empty cabin becomes occupied

Progress for the time a cabin was empty includes rooms, jobs, and placed blocks such as furnaces and crops. Only time while the server operates counts. Direction must confirm the progress limits.
