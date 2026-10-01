# Direction

## Goal

Portable Pocket Cabin gives players a home that travels with them in Survival mode. Players can move its entrance and keep their home. Construction outside the cabin stays at each site.

## Intended end state

Players can get a [Cabin Kit](context.md#cabin-kit) through Survival mode and create one lasting pocket home. Players can deploy, pack, and move it between safe sites. The interior, its contents, and purchased upgrades stay with the cabin through travel, restart, and recovery. Players choose wood types for the cabin's appearance through its [Cabin palette](context.md#cabin-palette). The palette gives the home the same materials through each move.

The cabin becomes a useful household as the player explores. Its owner can give access to other players, receive deliveries, and keep resources for household use. Larger [general space](context.md#general-space) and different rooms give space for living, crops, animals, and trees. A house cat can have a home there. Players can learn from exploration to use production, enchanting, and owner loadouts. Players can connect cabins for safe visits and work together. Each cabin keeps its identity and resources. Each cabin must get and install cabin books locally to reveal upgrades. Learned enchantments stay local to each cabin. Connections do not give automation capabilities or learned enchantments.

Production at higher tiers uses advanced upgrade materials to place automation after the stage where gathering its resource should be routine. Previously collected and traded materials satisfy the gate. The owner does not need a separate personal achievement. [The production decision](decisions/pdr/0013-gate-production-through-player-progress.md) states the rule without selecting exact costs or tiers.

Players can use the complete cabin in Survival mode without operator commands. Cabin interfaces show available actions, costs, access, and failures. Optional mod features follow the same behavior and have stated compatibility limits.

### Upgrade materials and exploration

Upgrade costs are predefined for each upgrade type and level. Costs do not generate extra requirements from the world, deployment biome, inventory, or earlier purchases. Chosen ingredients can still encourage exploration of different biomes and dimensions and give overlooked items a use.

Exploration requirements use modest quantities. Progression encourages finding materials rather than stockpiling increasing amounts of unusual items. Previously collected and traded items satisfy requirements. Players do not need to revisit a source or prove that they visited it personally.

Costs can include manufactured items. A short ingredient list can represent substantial crafting and gathering through a few complex products. Cost balance must account for their underlying ingredients and work.

General-space expansion uses the wood types selected at first construction, amethyst, and obsidian. Its wood requirements follow the saved Cabin palette. The [expansion decision](decisions/pdr/0006-expand-general-space-with-world-materials.md) states this relationship. Greenhouse upgrades use glass and iron as base materials, with plants, seeds, and botanical finds as exploration requirements.

Players see costs only for the next available upgrade in each upgrade type. Later costs stay hidden so progression does not become a shopping list for future tiers.

Travel and changes to inventory do not alter costs.

Each upgrade specifies preferred materials. Each non-vanilla material has a shared fallback chain that can pass through other supported mods and ends with vanilla. The first available material in that chain supplies the requirement. Each fallback specifies a fixed replacement quantity to account for differences in material effort. The [material decision](decisions/pdr/0014-use-preferred-upgrade-materials-with-shared-fallbacks.md) states the rule. Ingredient fallbacks do not change the saved Cabin palette.

Biomes O' Plenty, Alex's Mobs, and Farmer's Delight Refabricated are intended optional sources for suitable upgrade materials. Support follows the project's profile and compatibility limits. Compatible versions and the Alex's Mobs variant remain unresolved. The [mod references](references.md) retain their sources and compatibility questions.

## Boundaries and tensions

The cabin carries the home. Paths, farms, mines, docks, and terrain stay at each site. Interior blocks follow Minecraft behavior while the cabin is deployed. They also operate when players are in a connected packed cabin. An empty packed cabin pauses simulation. When players return, the cabin calculates progress for the time it was empty. Only time while the server operates counts. Progress for that time includes rooms, jobs, and placed blocks such as furnaces and crops. Rooms can make progress up to a limit while the cabin has no active exterior.

Recovery must keep the home after a failed move or lost exterior. A cabin has one active exterior at most and one owner. Each player can have one cabin at most. Connections must keep each cabin's resources, access rules, and identity. Automation has limits. Interior blocks do not simulate while a packed cabin is empty. Optional mod features must have profiles. Compatibility does not include mods without profiles.

## Open questions

### Cabin palette changes

Can an owner select a different Cabin palette by supplying the new wood? The cabin must keep its identity and purchased upgrades. Until a renovation feature is agreed, wood types stay locked to the choices at first construction.

### Automatic packing during a fire

Can a fire near the cabin start automatic packing? A decision is necessary about the effect on players in the cabin.

### More cabin styles

Can the cabin offer different exterior and room designs? A decision is necessary about their effect on cabin identity.

### Cabin names and maps

Can players give cabins names and show their sites on maps? A decision is necessary about who can see these sites.

### Cabin sleep and exterior night

Can sleep in a cabin control the time at its deployed exterior? A decision is necessary about the effect on other players.

### More modded materials

Which additional materials should have profiles? Biomes O' Plenty, Alex's Mobs, and Farmer's Delight Refabricated are intended sources. Compatible versions must be established, including whether Alex's Mobs Continued can satisfy the Alex's Mobs goal.

### Fixed upgrade costs and optional materials

What happens to a displayed or partly funded cost if a mod is later added or removed? Shared material fallbacks must keep upgrades attainable while preserving contributions. The existing [funding decision](decisions/pdr/0007-fund-and-install-cabin-upgrades.md) prevents cost changes from applying to non-empty funds during operation.

### Progress after an empty cabin becomes occupied

Progress for the time a cabin was empty includes rooms, jobs, and placed blocks such as furnaces and crops. Only time while the server operates counts. Direction must confirm the progress limits.
